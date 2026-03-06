# QuantumLauncher - Отчёт об ошибках и доработках

> Создано: 2026
> Версия проекта: 1.0.0-SNAPSHOT

---

## 🔴 КРИТИЧЕСКИЕ ПРОБЛЕМЫ (Блокируют сборку)

### 1. Несовместимость JDK и Gradle

**Проблема:**
- Установлен JDK 25 (class file version 69)
- Gradle 8.11 не поддерживает JDK 25
- Ошибка: `Unsupported class file major version 69`

**Решение:**
```
Вариант А: Установить JDK 17 или JDK 21 LTS
  - Скачать с https://adoptium.net/ или https://aws.amazon.com/corretto/
  - Установить и настроить JAVA_HOME

Вариант Б: Обновить Gradle до версии 9.x
  - Изменить gradle/wrapper/gradle-wrapper.properties:
    distributionUrl=https\://services.gradle.org/distributions/gradle-9.0.0-milestone-1-bin.zip
```

---

## 🔴 ОШИБКИ КОНФИГУРАЦИИ

### 2. Неправильная конфигурация JavaFX в ui/build.gradle

**Файл:** `modules/ui/build.gradle`

**Текущий код (ОШИБКА):**
```groovy
javafx '21.0.1'
```

**Исправить на:**
```groovy
implementation 'org.openjfx:javafx-controls:21.0.1:win'
implementation 'org.openjfx:javafx-graphics:21.0.1:win'
implementation 'org.openjfx:javafx-fxml:21.0.1:win'
```

---

### 3. ComponentScan не включает вложенные пакеты

**Файл:** `src/main/java/com/quantumlauncher/QuantumLauncherApplication.java`

**Проблема:** Указан пакет `com.quantumlauncher.monitoring`, но сервисы находятся в `com.quantumlauncher.monitoring.service`

**Исправить:**
```java
@ComponentScan(basePackages = {
    "com.quantumlauncher",
    "com.quantumlauncher.core",
    // ... существующие пакеты ...
    "com.quantumlauncher.monitoring.service"  // Добавить .service
})
```

---

### 4. FXML обработчики событий используют старый синтаксис

**Файл:** `src/main/resources/fxml/main.fxml`

**Проблема:** Используется старый синтаксис `#{...}` для onMouseEntered/onMouseExited:
```xml
onMouseEntered="#{btnInstances.setStyle(...)}"
```

**Решение:** Использовать методы контроллера или CSS :hover псевдокласс

---

## 🟡 ОШИБКИ КОМПИЛЯЦИИ

### 5. MainController - приватные методы вызываются из FXML

**Файл:** `src/main/java/com/quantumlauncher/ui/controller/MainController.java`

**Проблема:** Методы `showInstances()`, `showMods()` и т.д. являются приватными, но вызываются из FXML

**Исправить:** Добавить @FXML аннотацию или сделать методы публичными:
```java
// Вариант 1: Добавить @FXML
@FXML
private void showInstances() { ... }

// Вариант 2: Сделать публичными
public void showInstances() { ... }
```

---

### 6. Модель Instance - отсутствуют геттеры/сеттеры

**Файл:** `modules/core/src/main/java/com/quantumlauncher/core/model/Instance.java`

**Проблема:** Модель используется в контроллерах, но геттеры/сеттеры не проверены

**Проверить наличие:**
- `getId()`, `setId()`
- `getName()`, `setName()`
- `getVersion()`, `setVersion()`
- `getModloader()`, `setModloader()`
- `getMaxRam()`, `setMaxRam()`
- `getLastPlayed()`, `setLastPlayed()`

---

### 7. AIChatAssistant - отсутствует реализация

**Файл:** `modules/ai/src/main/java/com/quantumlauncher/ai/service/AIChatAssistant.java`

**Проблема:** Класс существует, но метод `getResponse()` требует реализации

**Проверить и реализовать метод:**
```java
public String getResponse(String userId, String message) {
    // Требует реализации
    throw new UnsupportedOperationException("Требует реализации");
}
```

---

## 🟡 ОТСУТСТВУЮЩИЕ РЕСУРСЫ

### 8. Отсутствуют изображения

**Отсутствует:**
- `src/main/resources/images/icon.png`
- `src/main/resources/images/default-avatar.png`

**Решение:** Создать placeholder изображения или удалить ссылки из FXML

---

### 9. Отсутствует FXML файл

**Файл:** `src/main/resources/fxml/ai_assistant.fxml`

**Проблема:** Файл существует, но не проверено содержимое

---

## 🟢 ПРЕДУПРЕЖДЕНИЯ И РЕКОМЕНДАЦИИ

### 10. Нестандартный запуск приложения

**Файл:** `src/main/java/com/quantumlauncher/QuantumLauncherApplication.java`

**Проблема:** Spring Boot запускает JavaFX из main метода

**Рекомендация:** Использовать отдельный entry point или перейти на Spring Boot 3.x с JavaFX модулем

---

### 11. SQLite диалект для Hibernate 6.x

**Файл:** `src/main/resources/application.properties`

**Проблема:** `spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect`

**Проверить:** Нужен `hibernate-community-dialects` для Hibernate 6.x (уже добавлен в core/build.gradle)

---

### 12. Модульные зависимости - потенциальные циклы

**Проверить зависимости:**
- `ui` зависит от `ai`, `content`, `forks`, `security`, `skins`, `monitoring`
- `ai` зависит от `core`
- `content` зависит от `core`
- `instances` зависит от `core`, `content`, `security`

**Рекомендация:** Проверить нет ли циклических зависимостей

---

### 13. Неиспользуемые импорты и переменные

**Проверить в файлах:**
- `InstancesController.java` - неиспользуемые импорты
- `ModsController.java` - переменная `Path` может быть не нужна

---

## 📋 СПИСОК ФАЙЛОВ ДЛЯ ПРОВЕРКИ

### Java контроллеры (UI)
- [x] `MainController.java` - требует исправления @FXML
- [x] `InstancesController.java` - OK
- [x] `ModsController.java` - OK
- [x] `AIController.java` - требует AIChatAssistant
- [ ] `ForksController.java` - не проверен
- [ ] `SkinsController.java` - не проверен
- [ ] `SettingsController.java` - OK

### Модули (Core)
- [x] `Instance.java` - проверить геттеры/сеттеры
- [x] `Mod.java` - OK
- [x] `Fork.java` - не проверен
- [x] `Skin.java` - не проверен
- [x] `AppSettings.java` - не проверен

### Сервисы
- [x] `InstanceManager.java` - OK
- [x] `ModManagementService.java` - не проверен
- [x] `ConflictAnalysisService.java` - OK
- [ ] `AIChatAssistant.java` - требует реализации
- [ ] `SecurityScanner.java` - не проверен
- [ ] `CloudStorageService.java` - не проверен
- [ ] `ForkManager.java` - не проверен
- [ ] `SkinManager.java` - не проверен

### API Controllers
- [ ] `InstanceController.java` - не проверен
- [ ] `ModController.java` - не проверен
- [ ] `ForkController.java` - не проверен
- [ ] `SkinController.java` - не проверен
- [ ] `SettingsController.java` (api) - не проверен

### FXML
- [x] `main.fxml` - требует исправления обработчиков
- [x] `instances.fxml` - не проверен
- [x] `mods.fxml` - не проверен
- [x] `forks.fxml` - не проверен
- [x] `skins.fxml` - не проверен
- [x] `ai.fxml` - не проверен
- [x] `settings.fxml` - не проверен

---

## ✅ ЧТО УЖЕ РАБОТАЕТ

1. Структура проекта (11 модулей)
2. Core модели (JPA сущности)
3. JpaConfig
4. Gradle конфигурация (кроме ui/build.gradle)
5. Основная архитектура Spring + JavaFX
6. PerformanceMonitor
7. ConflictAnalysisService
8. InstanceManager (базовый функционал)
9. UI контроллеры (базовая структура)

---

## 🎯 ПЛАН ИСПРАВЛЕНИЙ

### Приоритет 1 (Критично):
1. ✅ Проблема 1: JDK/Gradle - требуется JDK 17/21
2. ✅ Проблема 2: ui/build.gradle - исправить JavaFX зависимости
3. ✅ Проблема 3: ComponentScan - добавить .service пакеты

### Приоритет 2 (Компиляция):
4. ✅ Проблема 5: MainController @FXML
5. ✅ Проблема 6: Instance геттеры/сеттеры
6. ✅ Проблема 7: AIChatAssistant реализация

### Приоритет 3 (Ресурсы):
7. ✅ Проблема 8: Создать placeholder изображения
8. ✅ Проблема 4: Исправить FXML обработчики

---

*Обновлено: 2026*
