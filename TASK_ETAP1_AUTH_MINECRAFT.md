# ЭТАП 1: Авторизация + Установка Minecraft + Запуск

## Задачи

Реализовать полноценную систему авторизации и запуска игры без заглушек и костылей.

### 1. Microsoft OAuth авторизация

Создать сервис `MicrosoftAuthService` в `modules/auth/src/main/java/com/quantumlauncher/auth/service/`:

**Функции:**
- Получение OAuth code через Device Flow (открытие браузера, ввод кода)
- Обмен code на access_token и refresh_token
- Получение XBL токена (Xbox Live)
- Получение XSTS токена
- Получение Minecraft токена (Yggdrasil)
- Получение профиля игрока (UUID, username)
- Сохранение токенов в БД с шифрованием
- Автообновление токенов через refresh_token

**Эндпоинты OAuth Microsoft:**
- Device code: POST https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode
- Token: POST https://login.microsoftonline.com/consumers/oauth2/v2.0/token
- XBL: POST https://auth.xboxlive.com/xboxlive/users/authenticate
- XSTS: POST https://auth.xboxlive.com/xboxlive/users/authorize
- Yggdrasil: POST https://authserver.mojang.com/authenticate

**Модели:**
- `MinecraftAccount.java` — UUID, username, accessToken, refreshToken, refreshTokenExpiry, skinUrl, capeUrl
- `AccountRepository.java` — findByUuid, findByUsername, save, delete

### 2. Скачивание Minecraft (Version Manager)

Создать сервис `MinecraftInstaller` в `modules/instances/src/main/java/com/quantumlauncher/instances/service/`:

**Функции:**
- Получение списка версий с Mojang (versions.json)
- Скачивание version manifest
- Скачивание client.jar
- Скачивание assets (через assets index)
- Скачивание libraries (все необходимые)
- Скачивание нативов (для Windows)
- Проверка целостности файлов (SHA-1)
- Версионирование (хранить все версии отдельно)

**Структура директорий:**
```
versions/
├── 1.20.4/
│   ├── 1.20.4.jar
│   ├── 1.20.4.json
│   └── natives/
│       └── windows-x64/
├── 1.20.3/
│   └── ...
assets/
├── indexes/
│   ├── 1.20.json
│   └── ...
├── objects/
│   └── ...
libraries/
├── net/
├── com/
└── ...
```

**Методы:**
- `List<String> getAvailableVersions()` — получить список версий
- `void installVersion(String version)` — установить версию
- `void installAssets(String version)` — установить assets
- `Path getVersionJar(String version)` — путь к client.jar
- `Path getNativesDir(String version)` — путь к нативам
- `List<String> getClasspath(String version)` — полный classpath

### 3. Установка Fabric/Forge/Quilt

Создать сервис `ModloaderInstaller` в `modules/instances/src/main/java/com/quantumlauncher/instances/service/`:

**Fabric:**
- Скачивание fabric-installer с GitHub/Pojoac
- Запуск инсталлера с параметрами
- Установка в директорию версии
- Создание fabric-loader-meta.json

**Forge:**
- Скачивание forge-installer с files.minecraftforge.net
- Запуск инсталлера
- Установка universal jar и libraries
-特别注意: Forge 1.17+ использует новый инсталлер

**Quilt:**
- Аналогично Fabric, но с quilt-meta

**Методы:**
- `void installFabric(String mcVersion, String loaderVersion)`
- `void installForge(String mcVersion, String forgeVersion)`
- `void installQuilt(String mcVersion, String quiltVersion)`
- `List<ModloaderVersion> getAvailableFabricVersions(String mcVersion)`
- `List<ModloaderVersion> getAvailableForgeVersions(String mcVersion)`
- `boolean isInstalled(String mcVersion, String modloader)`

### 4. Правильный запуск игры

Обновить `InstanceManager` в `modules/instances/`:

**Изменения:**
- Использовать реальные токены из MicrosoftAuthService
- Генерировать правильный UUID (не случайный!)
- Передавать accessToken в аргументы запуска
- Загружать скин и плащ через --skinUrl (Fabric) или через assets
- Подключать все libraries и нативы к classpath

**Команда запуска (полный пример):**
```
java
-Xmx4g -Xms2g -XX:+UseG1GC ...
-Dfml.ignorePatchDiscrepancies=true
-Dfml.ignoreInvalidMinecraftCertificates=true
-Djava.library.path=natives/windows-x64
-cp "libraries/*;versions/1.20.4-forge/1.20.4-forge.jar"
net.minecraft.client.main.Main
--username Player
--uuid <real-uuid-from-microsoft>
--accessToken <access-token>
--version 1.20.4-forge
--gameDir instances/TestModpack
--assetsDir assets
--assetIndex 1.20
--userType mojang
```

**Добавить в Instance:**
- Поле `minecraftAccountId` — ссылка на аккаунт
- Поле `modloaderVersion` — версия модлоадера

### 5. Управление аккаунтами в UI

Создать контроллер `AccountsController` в `src/main/java/com/quantumlauncher/ui/controller/`:

**FXML:** `accounts.fxml`
- Список аккаунтов (ListView)
- Кнопки: Добавить Microsoft, Удалить, Выбрать
- Отображение: аватар, никнейм, статус

**Функции:**
- Клик "Добавить" → открытие браузера → Device Flow
- Сохранение аккаунта в БД
- Выбор аккаунта для инстанса
- Удаление аккаунта

### 6. Обновление MainController

Добавить в sidebar кнопку "Аккаунты" и соответствующий функционал.

## Требования к коду

1. **Без заглушек** — полная реализация всех HTTP запросов
2. **Обработка ошибок** — retry логика, таймауты, понятные сообщения
3. **Логирование** — всё через SLF4J logger
4. **Потокобезопасность** — использовать CompletableFuture для загрузок
5. **Прогресс** — показывать ProgressBar при загрузке

## Новые модули

Создать модуль `auth`:
- Добавить в settings.gradle: `include 'auth'`
- dependencies: okHttp3, gson
- Структура:
```
modules/auth/
├── build.gradle
└── src/main/java/com/quantumlauncher/auth/
    ├── service/
    │   └── MicrosoftAuthService.java
    └── model/
        └── MinecraftAccount.java
```

## Проверка

После реализации:
1. Можно добавить Microsoft аккаунт через UI
2. Можно установить любую версию Minecraft
3. Можно установить Fabric/Forge
4. Игра запускается с реальным скином и UUID
