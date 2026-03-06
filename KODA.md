# KODA.md — Инструкции для работы с проектом QuantumLauncher

## Обзор проекта

**QuantumLauncher** — это лаунчер для Minecraft нового поколения с поддержкой AI-функций. Проект представляет собой десктонное приложение, построенное на стеке технологий **Java 21 + JavaFX 21 + Spring Boot 3.2**.

### Основные возможности

- AI-ассистент для анализа совместимости модов
- Управление игровыми инстансами (Fabric, Forge, Quilt, Vanilla)
- Установка и управление модами
- Кастомные сборки (Forks)
- Управление скинами и плащами
- Мониторинг производительности
- Встроенный сканер безопасности
- Облачная синхронизация

---

## Архитектура проекта

### Структура директорий

```
QuantumLauncher/
├── build.gradle              # Корневой файл конфигурации сборки
├── settings.gradle           # Настройки подмодулей
├── gradle.properties         # Свойства Gradle
├── src/main/
│   ├── java/                 # JavaFX контроллеры UI
│   │   └── com/quantumlauncher/
│   │       ├── QuantumLauncherApplication.java  # Главный класс приложения
│   │       ├── QuantumLauncher.java
│   │       └── ui/
│   │           ├── QuantumLauncherApp.java      # JavaFX Application
│   │           └── controller/                  # UI контроллеры
│   └── resources/
│       ├── application.properties               # Конфигурация приложения
│       ├── fxml/                                # FXML макеты UI
│       ├── css/theme.css                        # Стили
│       └── images/                              # Изображения
└── modules/                 # Подмодули проекта
    ├── core/                # Модели JPA и репозитории
    ├── api/                 # REST контроллеры
    ├── ai/                  # AI сервисы
    ├── content/             # Управление модами
    ├── instances/           # Управление инстансами
    ├── forks/               # Кастомные сборки
    ├── skins/               # Управление скинами
    ├── security/            # Сканер безопасности
    ├── cloud/               # Облачное хранилище
    ├── monitoring/          # Мониторинг FPS
    └── ui/                  # JavaFX UI модуль
```

### Технологический стек

| Компонент | Версия |
|-----------|--------|
| JDK | 21 (LTS) |
| JavaFX | 21.0.1 |
| Spring Boot | 3.2.0 |
| Gradle | 8.5 (рекомендуется) |
| SQLite | 3.44.1.0 |
| Hibernate | 6.4.1.Final |

---

## Сборка и запуск

### Требования

- **JDK 21** — требуется для сборки проекта
- **Gradle 8.5+** — для управления сборкой

### Команды для сборки

```powershell
# Windows: установка переменной JAVA_HOME и сборка
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
cmd /c "gradlew.bat build --no-daemon"
```

```bash
# Linux/Mac
export JAVA_HOME=/path/to/jdk-21
./gradlew build --no-daemon
```

### Запуск приложения

```bash
./gradlew run
```

### Режим разработки

```bash
./gradlew bootRun
```

---

## Модульная структура

### Зависимости между модулями

```
Корневой проект (build.gradle)
    │
    ├── :core          — базовый модуль (модели, репозитории)
    ├── :api           — REST API
    ├── :ai            — AI сервисы (зависит от :core)
    ├── :content       — управление модами (зависит от :core)
    ├── :instances     — управление инстансами (зависит от :core)
    ├── :forks         — кастомные сборки (зависит от :core)
    ├── :skins         — управление скинами (зависит от :core)
    ├── :security      — сканер безопасности (зависит от :core)
    ├── :cloud         — облачное хранилище (зависит от :core)
    ├── :monitoring    — мониторинг производительности
    └── :ui            — JavaFX UI модуль
```

### Описание модулей

#### :core
Содержит JPA-сущности и репозитории:
- `Instance.java` — модель игрового инстанса
- `Mod.java` — модель мода
- `Fork.java` — модель кастомной сборки
- `Skin.java` — модель скина
- `AppSettings.java` — модель настроек приложения

#### :ai
AI-сервисы:
- `AIChatAssistant` — AI-ассистент
- `ConflictAnalysisService` — анализ конфликтов модов

#### :content
- `ModManagementService` — управление модами

#### :instances
- `InstanceManager` — менеджер инстансов

#### :forks
- `ForkManager` — менеджер сборок

#### :skins
- `SkinManager` — менеджер скинов

#### :security
- `SecurityScanner` — сканер безопасности модов

#### :monitoring
- `PerformanceMonitor` — мониторинг производительности

#### :ui
JavaFX контроллеры:
- `MainController` — главный контроллер
- `AIController` — UI для AI-ассистента
- `AIAssistantController`
- `InstancesController` — управление инстансами
- `ModsController` — управление модами
- `ForksController` — управление сборками
- `SkinsController` — управление скинами
- `SettingsController` — настройки

---

## Конфигурация

### Основные настройки (application.properties)

```properties
# База данных SQLite
spring.datasource.url=jdbc:sqlite:data/quantumlauncher.db
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update

# Сервер
server.port=8080

# AI
ai.chat.api-key=demo
ai.chat.model=gpt-3.5-turbo
```

### Настройки Gradle (gradle.properties)

```properties
org.gradle.jvmargs=-Xmx4g -XX:+EnableDynamicAgentLoading
org.gradle.java.home=C\:\\Program Files\\Java\\jdk-21
org.gradle.java.installations.auto-detect=false
```

---

## Правила разработки

### Стиль кодирования

1. **Язык комментариев и документации** — русский
2. **Именование пакетов** — английский, lowerCamelCase
3. **Именование классов** — UpperCamelCase
4. **Именование методов** — lowerCamelCase
5. **Форматирование** — стандарт Java (4 пробела)

### Структура Java-пакета

```
com.quantumlauncher.<module>
├── model/           # JPA-сущности
├── repository/      # Spring Data репозитории
├── service/         # Бизнес-логика
├── controller/      # REST контроллеры (для API модуля)
└── config/          # Конфигурация
```

### Работа с UI (JavaFX)

1. FXML-файлы располагаются в `src/main/resources/fxml/`
2. Контроллеры — в `src/main/java/com/quantumlauncher/ui/controller/`
3. CSS-стили — в `src/main/resources/css/theme.css`
4. Изображения — в `src/main/resources/images/`

### Добавление нового модуля

1. Создать директорию `modules/<module-name>/src/main/java/com/quantumlauncher/<module-name>/`
2. Создать `build.gradle` в модуле с зависимостями
3. Добавить в `settings.gradle`: `include '<module-name>'` и `project(':...').projectDir = ...`
4. Добавить зависимость в корневой `build.gradle`: `implementation project(':<module-name>')`

### Добавление нового UI экрана

1. Создать FXML-файл в `src/main/resources/fxml/`
2. Создать Controller-класс в `src/main/java/com/quantumlauncher/ui/controller/`
3. Добавить навигацию в `MainController.java`

---

## Текущее состояние (по PROJECT_CHECKLIST.md)

### Выполнено

- [x] JDK 21 настроен в gradle.properties
- [x] AI модуль настроен (jakarta.annotation-api)
- [x] Все модели созданы (Instance, Mod, Fork, Skin, AppSettings)
- [x] Все сервисы созданы (InstanceManager, ForkManager, SkinManager, PerformanceMonitor)
- [x] Все репозитории созданы
- [x] FXML-файлы созданы
- [x] Изображения созданы (icon.png, default-avatar.png)
- [x] CSS исправлен (убраны gradient, не поддерживаемые в JavaFX)

### Требует проверки/доработки

- [ ] Сборка с JDK 21 (требуется верифицировать)
- [ ] JavaFX зависимости в корневом build.gradle
- [ ] Интеграция контроллеров с сервисами

---

## Типичные задачи

### Сборка проекта

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
./gradlew build
```

### Очистка и пересборка

```powershell
./gradlew clean build
```

### Запуск тестов

```powershell
./gradlew test
```

### Просмотр зависимостей

```powershell
./gradlew dependencies
```

---

## Известные ограничения

1. **JavaFX CSS** — градиенты (`linear-gradient`) не поддерживаются, использовать только сплошные цвета
2. **SQLite** — ограниченная поддержка некоторых SQL-функций в Hibernate
3. **JavaFX на Linux/Mac** — требуется установка нативных библиотек JavaFX для соответствующей ОС

---

## Контакты и поддержка

Проект разработан с использованием Java 21 + JavaFX 21 + Spring Boot 3.2

*Обновлено: 2026-03-05*
