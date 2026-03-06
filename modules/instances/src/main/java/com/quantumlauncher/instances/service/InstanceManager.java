package com.quantumlauncher.instances.service;

import com.quantumlauncher.auth.model.MinecraftAccount;
import com.quantumlauncher.auth.service.MicrosoftAuthService;
import com.quantumlauncher.core.model.Instance;
import com.quantumlauncher.core.model.Mod;
import com.quantumlauncher.core.repository.InstanceRepository;
import com.quantumlauncher.core.repository.ModRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Сервис управления инстансами и запуска Minecraft
 */
@Service
public class InstanceManager {
    
    private static final Logger log = LoggerFactory.getLogger(InstanceManager.class);
    
    @Autowired
    private InstanceRepository instanceRepository;
    
    @Autowired
    private ModRepository modRepository;
    
    @Autowired
    private MicrosoftAuthService authService;
    
    @Autowired
    private MinecraftInstaller installer;
    
    @Autowired
    private ModloaderInstaller modloaderInstaller;
    
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Создание нового инстанса
     */
    public Instance createInstance(String name, String mcVersion, String modloader, String javaPath) {
        log.info("Создание инстанса: {} ({}, {})", name, mcVersion, modloader);
        
        // Проверка уникальности имени
        if (instanceRepository.existsByName(name)) {
            throw new IllegalArgumentException("Инстанс с таким именем уже существует");
        }
        
        // Создание директории инстанса
        Path instancesDir = Paths.get("instances");
        Path instanceDir = instancesDir.resolve(name);
        
        try {
            Files.createDirectories(instanceDir);
            Files.createDirectories(instanceDir.resolve("mods"));
            Files.createDirectories(instanceDir.resolve("config"));
            Files.createDirectories(instanceDir.resolve("logs"));
            
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию инстанса", e);
        }
        
        // Создание инстанса
        Instance instance = new Instance();
        instance.setName(name);
        instance.setVersion(mcVersion);
        instance.setModloader(modloader);
        instance.setPath(instanceDir.toString());
        instance.setJavaPath(javaPath != null ? javaPath : findJava());
        instance.setMinRam(2048);
        instance.setMaxRam(4096);
        
        return instanceRepository.save(instance);
    }
    
    /**
     * Создание инстанса с привязкой к аккаунту
     */
    public Instance createInstance(String name, String mcVersion, String modloader, String accountId, String javaPath) {
        Instance instance = createInstance(name, mcVersion, modloader, javaPath);
        instance.setMinecraftAccountId(accountId);
        return instanceRepository.save(instance);
    }
    
    /**
     * Запуск Minecraft
     */
    public CompletableFuture<Process> launchInstance(String instanceId) {
        log.info("Запуск инстанса: {}", instanceId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Instance> instanceOpt = instanceRepository.findById(instanceId);
                if (instanceOpt.isEmpty()) {
                    throw new IllegalArgumentException("Инстанс не найден");
                }
                
                Instance instance = instanceOpt.get();
                
                // Обновление времени последней игры
                instance.setLastPlayed(Instant.now().getEpochSecond());
                instanceRepository.save(instance);
                
                // Получение аккаунта
                MinecraftAccount account = null;
                String accessToken = "null"; // Для офлайн режима
                String uuid = UUID.randomUUID().toString(); // Случайный UUID для офлайн
                String username = "Player";
                
                if (instance.getMinecraftAccountId() != null) {
                    account = authService.getAccountById(instance.getMinecraftAccountId());
                    if (account != null) {
                        // Получаем валидный токен (автоматически обновляет если нужно)
                        accessToken = authService.getValidAccessToken(account);
                        uuid = account.getUuid();
                        username = account.getUsername();
                        
                        // Обновляем время использования
                        authService.updateLastUsed(account.getId());
                        
                        log.info("Используется аккаунт: {} ({})", username, uuid);
                    }
                }
                
                // Подготовка команды запуска
                ProcessBuilder processBuilder = buildLaunchCommand(instance, accessToken, uuid, username);
                
                // Настройка процесса
                processBuilder.directory(new File(instance.getPath()));
                processBuilder.redirectErrorStream(true);
                
                // Запуск
                Process process = processBuilder.start();
                runningProcesses.put(instanceId, process);
                
                // Логирование вывода
                logProcessOutput(process, instance.getName());
                
                // Ожидание завершения
                int exitCode = process.waitFor();
                runningProcesses.remove(instanceId);
                
                log.info("Инстанс {} завершился с кодом: {}", instance.getName(), exitCode);
                return process;
                
            } catch (Exception e) {
                log.error("Ошибка запуска инстанса", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Построение команды запуска
     */
    private ProcessBuilder buildLaunchCommand(Instance instance, String accessToken, String uuid, String username) throws Exception {
        List<String> command = new ArrayList<>();
        
        // Java
        String javaPath = instance.getJavaPath();
        if (javaPath == null || javaPath.isEmpty()) {
            javaPath = findJava();
        }
        command.add(javaPath);
        
        // JVM аргументы
        command.addAll(parseJvmArgs(instance));
        
        // Добавляем нативы в путь
        Path nativesPath = installer.getNativesDir(instance.getVersion());
        String nativesPathStr = nativesPath.toAbsolutePath().toString();
        
        // Для Windows добавляем путь к нативам
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            command.add("-Djava.library.path=" + nativesPathStr);
        }
        
        // Основной класс
        String mainClass = getMainClass(instance);
        
        // Собираем classpath
        String classpath = buildClasspath(instance);
        if (!classpath.isEmpty()) {
            command.add("-cp");
            command.add(classpath);
        }
        
        command.add(mainClass);
        
        // Аргументы игры
        command.add("--username");
        command.add(username);
        
        command.add("--uuid");
        command.add(uuid);
        
        command.add("--accessToken");
        command.add(accessToken);
        
        command.add("--version");
        // Используем версию с модлоадером если есть
        String versionId = getVersionId(instance);
        command.add(versionId);
        
        command.add("--gameDir");
        command.add(instance.getPath());
        
        command.add("--assetsDir");
        command.add(installer.getAssetsDir().toAbsolutePath().toString());
        
        command.add("--assetIndex");
        command.add(installer.getAssetIndex(instance.getVersion()));
        
        command.add("--userType");
        command.add("mojang");
        
        // Дополнительные аргументы для Forge
        if ("forge".equalsIgnoreCase(instance.getModloader())) {
            command.add("--fml.forgeLicense");
            command.add("fml");
            command.add("--fml.ignorePatchDiscrepancies");
            command.add("true");
            command.add("--fml.ignoreInvalidMinecraftCertificates");
            command.add("true");
        }
        
        log.info("Команда запуска: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(System.getenv());
        
        // Для Windows добавляем путь к нативам в PATH
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            String pathEnv = pb.environment().get("PATH");
            pb.environment().put("PATH", nativesPathStr + File.pathSeparator + pathEnv);
        }
        
        return pb;
    }
    
    /**
     * Сборка classpath
     */
    private String buildClasspath(Instance instance) throws IOException {
        List<Path> classpath = new ArrayList<>();
        
        // Добавляем libraries
        classpath.addAll(installer.getClasspath(instance.getVersion()));
        
        // Добавляем JAR инстанса (версия с модлоадером)
        String versionId = getVersionId(instance);
        Path versionJar = installer.getVersionDir(instance.getVersion()).resolve(versionId + ".jar");
        
        // Если нет отдельного JAR с модлоадером, используем vanilla
        if (!Files.exists(versionJar)) {
            versionJar = installer.getVersionJarPath(instance.getVersion());
        }
        
        if (Files.exists(versionJar)) {
            classpath.add(versionJar);
        }
        
        // Собираем строку classpath
        String separator = System.getProperty("os.name").toLowerCase().contains("windows") ? ";" : ":";
        return classpath.stream()
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.joining(separator));
    }
    
    /**
     * Получение ID версии (с модлоадером)
     */
    private String getVersionId(Instance instance) {
        if (instance.getModloader() != null && !instance.getModloader().isEmpty()) {
            return instance.getVersion() + "-" + instance.getModloader().toLowerCase();
        }
        return instance.getVersion();
    }
    
    /**
     * Парсинг JVM аргументов
     */
    private List<String> parseJvmArgs(Instance instance) {
        List<String> args = new ArrayList<>();
        
        // Базовые аргументы для Minecraft
        args.add("-Xms" + instance.getMinRam() + "m");
        args.add("-Xmx" + instance.getMaxRam() + "m");
        
        // Оптимизация
        args.add("-XX:+UseG1GC");
        args.add("-XX:+ParallelRefProcEnabled");
        args.add("-XX:MaxGCPauseMillis=200");
        args.add("-XX:+UnlockExperimentalVMOptions");
        args.add("-XX:+AlwaysPreTouch");
        
        // Дополнительные аргументы из настроек
        if (instance.getJvmArgs() != null && !instance.getJvmArgs().isEmpty()) {
            args.addAll(Arrays.asList(instance.getJvmArgs().split("\\s+")));
        }
        
        return args;
    }
    
    /**
     * Получение главного класса
     */
    private String getMainClass(Instance instance) {
        if (instance.getModloader() != null) {
            return modloaderInstaller.getMainClass(instance.getVersion(), instance.getModloader());
        }
        return "net.minecraft.client.main.Main";
    }
    
    /**
     * Поиск Java
     */
    private String findJava() {
        String javaHome = System.getProperty("java.home");
        Path javaPath = Paths.get(javaHome, "bin", "java.exe");
        
        if (Files.exists(javaPath)) {
            return javaPath.toString();
        }
        
        javaPath = Paths.get(javaHome, "bin", "java");
        return javaPath.toString();
    }
    
    /**
     * Логирование вывода процесса
     */
    private void logProcessOutput(Process process, String instanceName) {
        executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[{}] {}", instanceName, line);
                }
            } catch (IOException e) {
                log.error("Ошибка чтения вывода процесса", e);
            }
        });
    }
    
    /**
     * Остановка запущенного инстанса
     */
    public void stopInstance(String instanceId) {
        Process process = runningProcesses.get(instanceId);
        if (process != null && process.isAlive()) {
            log.info("Остановка инстанса: {}", instanceId);
            process.destroy();
            
            // Принудительная остановка через 5 секунд
            executor.submit(() -> {
                try {
                    Thread.sleep(5000);
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    /**
     * Проверка запущен ли инстанс
     */
    public boolean isRunning(String instanceId) {
        Process process = runningProcesses.get(instanceId);
        return process != null && process.isAlive();
    }
    
    /**
     * Получение всех инстансов
     */
    public List<Instance> getAllInstances() {
        return instanceRepository.findAll();
    }
    
    /**
     * Удаление инстанса
     */
    public void deleteInstance(String instanceId) {
        // Остановка если запущен
        stopInstance(instanceId);
        
        Optional<Instance> instanceOpt = instanceRepository.findById(instanceId);
        if (instanceOpt.isPresent()) {
            Instance instance = instanceOpt.get();
            
            // Удаление файлов
            try {
                Path instancePath = Paths.get(instance.getPath());
                if (Files.exists(instancePath)) {
                    deleteDirectory(instancePath);
                }
            } catch (IOException e) {
                log.error("Не удалось удалить файлы инстанса", e);
            }
            
            // Удаление из БД
            instanceRepository.delete(instance);
        }
    }
    
    /**
     * Рекурсивное удаление директории
     */
    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }
    
    /**
     * Обновление настроек инстанса
     */
    public Instance updateInstance(String instanceId, Instance updated) {
        Optional<Instance> instanceOpt = instanceRepository.findById(instanceId);
        
        if (instanceOpt.isEmpty()) {
            throw new IllegalArgumentException("Инстанс не найден");
        }
        
        Instance instance = instanceOpt.get();
        
        if (updated.getName() != null) {
            instance.setName(updated.getName());
        }
        if (updated.getJvmArgs() != null) {
            instance.setJvmArgs(updated.getJvmArgs());
        }
        if (updated.getMinRam() != null) {
            instance.setMinRam(updated.getMinRam());
        }
        if (updated.getMaxRam() != null) {
            instance.setMaxRam(updated.getMaxRam());
        }
        if (updated.getJavaPath() != null) {
            instance.setJavaPath(updated.getJavaPath());
        }
        if (updated.getMinecraftAccountId() != null) {
            instance.setMinecraftAccountId(updated.getMinecraftAccountId());
        }
        if (updated.getModloaderVersion() != null) {
            instance.setModloaderVersion(updated.getModloaderVersion());
        }
        
        return instanceRepository.save(instance);
    }
    
    /**
     * Привязка аккаунта к инстансу
     */
    public Instance setAccount(String instanceId, String accountId) {
        Optional<Instance> instanceOpt = instanceRepository.findById(instanceId);
        
        if (instanceOpt.isEmpty()) {
            throw new IllegalArgumentException("Инстанс не найден");
        }
        
        Instance instance = instanceOpt.get();
        instance.setMinecraftAccountId(accountId);
        return instanceRepository.save(instance);
    }
    
    /**
     * Получение аккаунта инстанса
     */
    public MinecraftAccount getInstanceAccount(String instanceId) {
        Optional<Instance> instanceOpt = instanceRepository.findById(instanceId);
        
        if (instanceOpt.isEmpty()) {
            return null;
        }
        
        Instance instance = instanceOpt.get();
        if (instance.getMinecraftAccountId() == null) {
            return null;
        }
        
        return authService.getAccountById(instance.getMinecraftAccountId());
    }
}
