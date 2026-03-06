# QuantumLauncher

**Версия:** 1.0.0-SNAPSHOT  
**Технологии:** Java 21, JavaFX 21, Spring Boot 3.2

---

## Описание

QuantumLauncher — следующее поколение лаунчера для Minecraft с AI-возможностями.

### Особенности

- 🤖 AI Ассистент и анализ конфликтов модов
- 🎮 Управление инстансами (Fabric, Forge, Quilt, Vanilla)
- 🧩 Установка и управление модами
- 🍴 Кастомные сборки (Forks)
- 👤 Управление скинами и плащами
- ⚡ Мониторинг производительности
- 🔒 Встроенный сканер безопасности
- ☁️ Облачная синхронизация

---

## Требования

- **JDK 21** (LTS)
- **Gradle 8.5** (для сборки)

---

## Сборка

### 1. Установка Gradle

```powershell
# Windows (через winget)
winget install Gradle.Gradle

# Или скачать вручную с https://gradle.org/install/
```

### 2. Клонирование репозитория

```bash
git clone <repo-url>
cd QuantumLauncher
```

### 3. Сборка проекта

```bash
gradle build
```

### 4. Запуск

```bash
gradle run
```

---

## Структура проекта

```
QuantumLauncher/
├── build.gradle              # Конфигурация сборки
├── settings.gradle           # Настройки модулей
├── src/main/
│   ├── java/                 # Основной код
│   └── resources/            # Ресурсы (FXML, CSS)
├── modules/                  # Подмодули
│   ├── core/                 # Модели и репозитории
│   ├── api/                  # REST API контроллеры
│   ├── ai/                   # AI сервисы
│   ├── content/              # Управление модами
│   ├── instances/            # Управление инстансами
│   ├── forks/                # Кастомные сборки
│   ├── skins/                # Управление скинами
│   ├── security/             # Сканер безопасности
│   ├── cloud/                # Облачное хранилище
│   ├── monitoring/           # Мониторинг FPS
│   └── ui/                   # JavaFX UI модуль
└── gradle/                   # Gradle Wrapper
```

---

## Модули

| Модуль | Описание |
|--------|----------|
| **core** | Модели JPA (Instance, Mod, Fork, Skin, AppSettings) |
| **api** | REST контроллеры |
| **ai** | AIChatAssistant, ConflictAnalysisService |
| **content** | ModManagementService |
| **instances** | InstanceManager |
| **forks** | ForkManager |
| **skins** | SkinManager |
| **security** | SecurityScanner |
| **cloud** | CloudStorageService |
| **monitoring** | PerformanceMonitor |
| **ui** | JavaFX контроллеры |

---

## Конфигурация

Настройки в `src/main/resources/application.properties`:

```properties
# База данных SQLite
spring.datasource.url=jdbc:sqlite:data/quantumlauncher.db

# Порт сервера
server.port=8080

# AI настройки
ai.chat.api-key=
ai.chat.model=gpt-3.5-turbo
```

---

## Разработка

### Добавление нового модуля

1. Создать директорию `modules/<module-name>/`
2. Создать `build.gradle` в модуле
3. Добавить в `settings.gradle`: `include '<module-name>'`
4. Добавить зависимость в корневой `build.gradle`

### Добавление нового UI экрана

1. Создать FXML в `src/main/resources/fxml/`
2. Создать Controller в `src/main/java/com/quantumlauncher/ui/controller/`
3. Добавить навигацию в `MainController.java`

---

## Лицензия

MIT License

---

*Создано с использованием Java 21 + JavaFX 21*
