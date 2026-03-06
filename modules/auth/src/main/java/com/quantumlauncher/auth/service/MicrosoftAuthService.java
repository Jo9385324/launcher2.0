package com.quantumlauncher.auth.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.quantumlauncher.auth.model.MinecraftAccount;
import com.quantumlauncher.auth.repository.AccountRepository;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Сервис Microsoft OAuth авторизации для Minecraft
 */
@Service
public class MicrosoftAuthService {

    private static final Logger log = LoggerFactory.getLogger(MicrosoftAuthService.class);

    // Microsoft OAuth endpoints
    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBL_AUTH_URL = "https://auth.xboxlive.com/xboxlive/users/authenticate";
    private static final String XSTS_AUTH_URL = "https://auth.xboxlive.com/xboxlive/users/authorize";
    private static final String YGGDRASIL_URL = "https://authserver.mojang.com/authenticate";
    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    // Scope для Minecraft
    private static final String MINECRAFT_SCOPE = "XboxLive.signin offline_access";

    // Client ID для Minecraft (публичный)
    private static final String CLIENT_ID = "00000000441cc96b";

    // Параметры шифрования
    private static final String ENCRYPTION_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final String ENCRYPTED_PREFIX = "enc:";

    // Retry
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 500;
    private static final long MAX_BACKOFF_MS = 4000;

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final SecureRandom secureRandom = new SecureRandom();

    // Хранилище pending авторизаций (deviceCode -> future)
    private final Map<String, CompletableFuture<MinecraftAccount>> pendingAuths = new ConcurrentHashMap<>();

    @Autowired
    private AccountRepository accountRepository;

    @Value("${auth.token-encryption-key:quantumlauncher-default-key-change-me}")
    private String encryptionKey;

    public MicrosoftAuthService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Начало авторизации через Device Flow
     */
    public DeviceCodeInfo startDeviceFlow() throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", MINECRAFT_SCOPE)
                .build();

        Request request = new Request.Builder()
                .url(DEVICE_CODE_URL)
                .post(body)
                .build();

        String responseBody = executeWithRetry("DeviceFlow: device_code", () -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (isRetryableHttpCode(response.code())) {
                        throw new IOException("Временная ошибка получения device code: HTTP " + response.code());
                    }
                    throw new IOException("Ошибка получения device code: HTTP " + response.code());
                }
                return response.body().string();
            }
        });

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);

        DeviceCodeInfo info = new DeviceCodeInfo();
        info.deviceCode = json.get("device_code").getAsString();
        info.userCode = json.get("user_code").getAsString();
        info.verificationUri = json.get("verification_uri").getAsString();
        info.message = json.get("message").getAsString();
        info.expiresIn = json.get("expires_in").getAsInt();
        info.interval = json.get("interval").getAsInt();

        log.info("Device Flow начат. Код: {}, URL: {}", info.userCode, info.verificationUri);
        return info;
    }

    /**
     * Ожидание завершения авторизации (poll)
     */
    public CompletableFuture<MinecraftAccount> waitForDeviceCodeAuth(String deviceCode, int interval, int expiresIn) {
        CompletableFuture<MinecraftAccount> future = new CompletableFuture<>();
        pendingAuths.put(deviceCode, future);

        Thread pollingThread = new Thread(() -> {
            long expiresAt = System.currentTimeMillis() + (expiresIn * 1000L);

            while (System.currentTimeMillis() < expiresAt) {
                try {
                    TokenResponse tokenResponse = requestToken(deviceCode);

                    if (tokenResponse != null) {
                        MinecraftAccount account = authenticateWithMinecraft(tokenResponse);
                        future.complete(account);
                        pendingAuths.remove(deviceCode);
                        return;
                    }
                } catch (IOException e) {
                    log.warn("Ошибка при опросе токена: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("Ошибка авторизации в Minecraft", e);
                    future.completeExceptionally(e);
                    pendingAuths.remove(deviceCode);
                    return;
                }

                try {
                    Thread.sleep(interval * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    future.cancel(false);
                    pendingAuths.remove(deviceCode);
                    return;
                }
            }

            future.completeExceptionally(new IOException("Время авторизации истекло"));
            pendingAuths.remove(deviceCode);
        });

        pollingThread.setDaemon(true);
        pollingThread.start();
        return future;
    }

    /**
     * Запрос токена по device code
     */
    private TokenResponse requestToken(String deviceCode) throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .add("client_id", CLIENT_ID)
                .add("device_code", deviceCode)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build();

        String responseBody = executeWithRetry("DeviceFlow: token", () -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String raw = response.body().string();

                if (response.code() == 400) {
                    JsonObject json = gson.fromJson(raw, JsonObject.class);
                    if (json.has("error")) {
                        String error = json.get("error").getAsString();
                        if ("authorization_pending".equals(error)) {
                            return null;
                        }
                        if ("slow_down".equals(error)) {
                            throw new IOException("Сервер просит замедлить опрос (slow_down)");
                        }
                        if ("expired_token".equals(error)) {
                            throw new IOException("Device code истёк");
                        }
                    }
                    throw new IOException("Ошибка получения токена: HTTP400");
                }

                if (!response.isSuccessful()) {
                    if (isRetryableHttpCode(response.code())) {
                        throw new IOException("Временная ошибка получения токена: HTTP " + response.code());
                    }
                    throw new IOException("Ошибка получения токена: HTTP " + response.code());
                }
                return raw;
            }
        });

        if (responseBody == null) {
            return null;
        }

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.accessToken = json.get("access_token").getAsString();
        tokenResponse.refreshToken = json.get("refresh_token").getAsString();
        tokenResponse.expiresIn = json.get("expires_in").getAsInt();
        tokenResponse.tokenType = json.get("token_type").getAsString();
        if (json.has("scope")) {
            tokenResponse.scope = json.get("scope").getAsString();
        }
        return tokenResponse;
    }

    /**
     * Обновление токена через refresh_token
     */
    public TokenResponse refreshToken(String refreshToken) throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("refresh_token", refreshToken)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build();

        String responseBody = executeWithRetry("OAuth: refresh_token", () -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (isRetryableHttpCode(response.code())) {
                        throw new IOException("Временная ошибка обновления токена: HTTP " + response.code());
                    }
                    throw new IOException("Ошибка обновления токена: HTTP " + response.code());
                }
                return response.body().string();
            }
        });

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.accessToken = json.get("access_token").getAsString();
        tokenResponse.refreshToken = json.has("refresh_token")
                ? json.get("refresh_token").getAsString()
                : refreshToken;
        tokenResponse.expiresIn = json.get("expires_in").getAsInt();
        tokenResponse.tokenType = json.get("token_type").getAsString();
        return tokenResponse;
    }

    /**
     * Аутентификация в Xbox Live (получение XBL токена)
     */
    public String authenticateXboxLive(String accessToken) throws IOException {
        JsonObject requestJson = new JsonObject();
        requestJson.add("Properties", new JsonObject());
        requestJson.getAsJsonObject("Properties").addProperty("AuthMethod", "RPS");
        requestJson.getAsJsonObject("Properties").addProperty("SiteName", "user.auth.xboxlive.com");
        requestJson.getAsJsonObject("Properties").addProperty("RpsTicket", "d=" + accessToken);
        requestJson.addProperty("RelyingParty", "http://auth.xboxlive.com");
        requestJson.addProperty("TokenType", "JWT");

        RequestBody body = RequestBody.create(requestJson.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(XBL_AUTH_URL).post(body).build();

        String responseBody = executeWithRetry("XBL authenticate", () -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (isRetryableHttpCode(response.code())) {
                        throw new IOException("Временная ошибка XBL: HTTP " + response.code());
                    }
                    throw new IOException("Ошибка XBL аутентификации: HTTP " + response.code());
                }
                return response.body().string();
            }
        });

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        return json.get("Token").getAsString();
    }

    /**
     * Получение XSTS токена
     */
    public String authenticateXSTS(String xblToken) throws IOException {
        JsonObject requestJson = new JsonObject();
        requestJson.add("Properties", new JsonObject());
        requestJson.getAsJsonObject("Properties").addProperty("SandboxId", "RETAIL");
        JsonArray userTokens = new JsonArray();
        userTokens.add(xblToken);
        requestJson.getAsJsonObject("Properties").add("UserTokens", userTokens);
        requestJson.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        requestJson.addProperty("TokenType", "JWT");

        RequestBody body = RequestBody.create(requestJson.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(XSTS_AUTH_URL).post(body).build();

        String responseBody = executeWithRetry("XSTS authorize", () -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (isRetryableHttpCode(response.code())) {
                        throw new IOException("Временная ошибка XSTS: HTTP " + response.code());
                    }
                    throw new IOException("Ошибка XSTS аутентификации: HTTP " + response.code());
                }
                return response.body().string();
            }
        });

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        return json.get("Token").getAsString();
    }

    /**
     * Аутентификация в Yggdrasil (Minecraft)
     */
    public String authenticateYggdrasil(String xstsToken) throws IOException {
        String[] parts = xstsToken.split("\\.");
        String claimsJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonObject claims = gson.fromJson(claimsJson, JsonObject.class);
        String uhs = claims.get("xid").getAsString();

        JsonObject requestJson = new JsonObject();
        requestJson.add("agent", new JsonObject());
        requestJson.getAsJsonObject("agent").addProperty("name", "Minecraft");
        requestJson.getAsJsonObject("agent").addProperty("version", 1);
        requestJson.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);

        RequestBody body = RequestBody.create(requestJson.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(YGGDRASIL_URL).post(body).build();

        String responseBody = executeWithRetry("Yggdrasil authenticate", () -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (isRetryableHttpCode(response.code())) {
                        throw new IOException("Временная ошибка Yggdrasil: HTTP " + response.code());
                    }
                    throw new IOException("Ошибка Yggdrasil аутентификации: HTTP " + response.code());
                }
                return response.body().string();
            }
        });

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        return json.get("accessToken").getAsString();
    }

    /**
     * Получение профиля игрока
     */
    public MinecraftProfile getMinecraftProfile(String minecraftToken) throws IOException {
        Request request = new Request.Builder()
                .url(MINECRAFT_PROFILE_URL)
                .get()
                .addHeader("Authorization", "Bearer " + minecraftToken)
                .build();

        String responseBody = executeWithRetry("Minecraft profile", () -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.code() == 404) {
                    throw new IOException("Аккаунт не имеет Minecraft");
                }
                if (!response.isSuccessful()) {
                    if (isRetryableHttpCode(response.code())) {
                        throw new IOException("Временная ошибка профиля Minecraft: HTTP " + response.code());
                    }
                    throw new IOException("Ошибка получения профиля: HTTP " + response.code());
                }
                return response.body().string();
            }
        });

        JsonObject json = gson.fromJson(responseBody, JsonObject.class);

        MinecraftProfile profile = new MinecraftProfile();
        profile.id = json.get("id").getAsString();
        profile.name = json.get("name").getAsString();

        if (json.has("skins")) {
            JsonArray skins = json.getAsJsonArray("skins");
            for (JsonElement skin : skins) {
                JsonObject skinObj = skin.getAsJsonObject();
                if ("CURRENT".equals(skinObj.get("state").getAsString())) {
                    profile.skinUrl = skinObj.get("url").getAsString();
                    profile.skinVariant = skinObj.has("variant")
                            ? skinObj.get("variant").getAsString()
                            : "CLASSIC";
                }
            }
        }

        if (json.has("capes")) {
            JsonArray capes = json.getAsJsonArray("capes");
            for (JsonElement cape : capes) {
                JsonObject capeObj = cape.getAsJsonObject();
                if ("CURRENT".equals(capeObj.get("state").getAsString())) {
                    profile.capeUrl = capeObj.get("url").getAsString();
                }
            }
        }

        return profile;
    }

    /**
     * Полная аутентификация через Microsoft OAuth
     */
    public MinecraftAccount authenticateWithMinecraft(TokenResponse tokenResponse) throws IOException {
        log.info("Начало полной авторизации Minecraft");

        String xblToken = authenticateXboxLive(tokenResponse.accessToken);
        String xstsToken = authenticateXSTS(xblToken);
        String minecraftToken = authenticateYggdrasil(xstsToken);
        MinecraftProfile profile = getMinecraftProfile(minecraftToken);

        MinecraftAccount account = accountRepository.findByUuid(profile.id)
                .orElseGet(() -> new MinecraftAccount(profile.id, profile.name));

        account.setUsername(profile.name);
        account.setAccessToken(encrypt(minecraftToken));
        account.setRefreshToken(encrypt(tokenResponse.refreshToken));
        account.setRefreshTokenExpiry(System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L));
        account.setXblToken(encrypt(xblToken));
        account.setXstsToken(encrypt(xstsToken));
        account.setSkinUrl(profile.skinUrl);
        account.setCapeUrl(profile.capeUrl);
        account.setLastUsed(Instant.now().getEpochSecond());

        return accountRepository.save(account);
    }

    /**
     * Обновление токенов аккаунта
     */
    public MinecraftAccount refreshAccountTokens(MinecraftAccount account) throws IOException {
        log.info("Обновление токенов для аккаунта: {}", account.getUsername());

        String decryptedRefreshToken = decryptIfNeeded(account.getRefreshToken());
        TokenResponse tokenResponse = refreshToken(decryptedRefreshToken);
        return authenticateWithMinecraft(tokenResponse);
    }

    /**
     * Получение или обновление валидного токена
     */
    public String getValidAccessToken(MinecraftAccount account) throws IOException {
        if (account.isRefreshTokenExpired()) {
            log.info("Refresh токен истёк, обновляем...");
            MinecraftAccount updated = refreshAccountTokens(account);
            return decryptIfNeeded(updated.getAccessToken());
        }

        return decryptIfNeeded(account.getAccessToken());
    }

    public List<MinecraftAccount> getAllAccounts() {
        return accountRepository.findAll().stream().map(this::decryptTokensSafely).toList();
    }

    public List<MinecraftAccount> getActiveAccounts() {
        return accountRepository.findByActiveTrue().stream().map(this::decryptTokensSafely).toList();
    }

    public MinecraftAccount getAccountById(String id) {
        return decryptTokensSafely(accountRepository.findById(id).orElse(null));
    }

    public MinecraftAccount getAccountByUuid(String uuid) {
        return decryptTokensSafely(accountRepository.findByUuid(uuid).orElse(null));
    }

    public void deleteAccount(String id) {
        accountRepository.deleteById(id);
    }

    public void deactivateAccount(String id) {
        accountRepository.findById(id).ifPresent(account -> {
            account.setActive(false);
            accountRepository.save(account);
        });
    }

    public void activateAccount(String id) {
        accountRepository.findById(id).ifPresent(account -> {
            account.setActive(true);
            accountRepository.save(account);
        });
    }

    public void updateLastUsed(String id) {
        accountRepository.findById(id).ifPresent(account -> {
            account.setLastUsed(Instant.now().getEpochSecond());
            accountRepository.save(account);
        });
    }

    private <T> T executeWithRetry(String operation, IOCallable<T> action) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return action.call();
            } catch (IOException e) {
                lastException = e;

                boolean canRetry = attempt < MAX_RETRY_ATTEMPTS && isRetryableException(e);
                if (!canRetry) {
                    throw e;
                }

                long delayMs = Math.min(INITIAL_BACKOFF_MS * (1L << (attempt - 1)), MAX_BACKOFF_MS);
                long jitter = secureRandom.nextInt(150);
                long finalDelay = delayMs + jitter;

                log.warn("{}: попытка {}/{} неуспешна: {}. Повтор через {} мс",
                        operation, attempt, MAX_RETRY_ATTEMPTS, e.getMessage(), finalDelay);

                try {
                    Thread.sleep(finalDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Повтор запроса прерван", ie);
                }
            }
        }

        throw lastException != null ? lastException : new IOException("Неизвестная ошибка выполнения операции: " + operation);
    }

    private boolean isRetryableException(IOException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return true;
        }

        String lower = msg.toLowerCase();
        return lower.contains("timeout")
                || lower.contains("temporar")
                || lower.contains("429")
                || lower.contains("500")
                || lower.contains("502")
                || lower.contains("503")
                || lower.contains("504")
                || lower.contains("slow_down")
                || lower.contains("временная ошибка");
    }

    private boolean isRetryableHttpCode(int code) {
        return code == 408 || code == 429 || (code >= 500 && code <= 599);
    }

    private MinecraftAccount decryptTokensSafely(MinecraftAccount source) {
        if (source == null) {
            return null;
        }

        try {
            return decryptedCopy(source);
        } catch (IOException e) {
            log.warn("Не удалось расшифровать токены аккаунта {}: {}", source.getUsername(), e.getMessage());
            return source;
        }
    }

    private MinecraftAccount decryptedCopy(MinecraftAccount source) throws IOException {
        MinecraftAccount copy = new MinecraftAccount();
        copy.setId(source.getId());
        copy.setUuid(source.getUuid());
        copy.setUsername(source.getUsername());
        copy.setAccessToken(decryptIfNeeded(source.getAccessToken()));
        copy.setRefreshToken(decryptIfNeeded(source.getRefreshToken()));
        copy.setRefreshTokenExpiry(source.getRefreshTokenExpiry());
        copy.setXblToken(decryptIfNeeded(source.getXblToken()));
        copy.setXstsToken(decryptIfNeeded(source.getXstsToken()));
        copy.setSkinUrl(source.getSkinUrl());
        copy.setCapeUrl(source.getCapeUrl());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setLastUsed(source.getLastUsed());
        copy.setActive(source.isActive());
        return copy;
    }

    private String encrypt(String plainText) throws IOException {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }

        if (plainText.startsWith(ENCRYPTED_PREFIX)) {
            return plainText;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IOException("Ошибка шифрования токена", e);
        }
    }

    private String decryptIfNeeded(String encryptedOrPlainText) throws IOException {
        if (encryptedOrPlainText == null || encryptedOrPlainText.isBlank()) {
            return encryptedOrPlainText;
        }

        if (!encryptedOrPlainText.startsWith(ENCRYPTED_PREFIX)) {
            return encryptedOrPlainText;
        }

        String payloadBase64 = encryptedOrPlainText.substring(ENCRYPTED_PREFIX.length());
        try {
            byte[] payload = Base64.getDecoder().decode(payloadBase64);
            if (payload.length <= GCM_IV_LENGTH_BYTES) {
                throw new IOException("Некорректный формат зашифрованного токена");
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(payload, GCM_IV_LENGTH_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IOException("Ошибка дешифрования токена", e);
        }
    }

    private SecretKeySpec buildSecretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hashed, ENCRYPTION_ALGORITHM);
    }

    @FunctionalInterface
    private interface IOCallable<T> {

        T call() throws IOException;
    }

    public static class DeviceCodeInfo {

        public String deviceCode;
        public String userCode;
        public String verificationUri;
        public String message;
        public int expiresIn;
        public int interval;
    }

    public static class TokenResponse {

        public String accessToken;
        public String refreshToken;
        public int expiresIn;
        public String tokenType;
        public String scope;
    }

    public static class MinecraftProfile {

        public String id;
        public String name;
        public String skinUrl;
        public String capeUrl;
        public String skinVariant;
    }
}
