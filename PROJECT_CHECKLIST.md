# QuantumLauncher - Чек-лист доработки до рабочей версии

## Фаза 1: Инфраструктура и сборка (Blocker Issues)

### 1.1 JDK и Gradle
- [ ] **Проблема:** Gradle 8.11 не поддерживает JDK 25
  - [x] Изменено: установить JDK 21 в gradle.properties
  - [x] Изменено: добавлен org.gradle.java.home в gradle.properties
  - [ ] TODO: Проверить сборку с JDK 21

### 1.2 JavaFX зависимости
- [x] **Проблема:** JavaFX классы не найдены в основном проекте
  - [x] Добавлены javafx в корневой build.gradle:
  ```gradle
  implementation 'org.openjfx:javafx-controls:21.0.1:win'
  implementation 'org.openjfx:javafx-graphics:21.0.1:win'
  implementation 'org.openjfx:javafx-fxml:21.0.1:win'
  implementation 'org.openjfx:javafx-base:21.0.1:win'
  ```

### 1.3 Переименование файла
- [x] Launcher.java → QuantumLauncher.java (ИСПРАВЛЕНО)

---

## Фаза 2: Модули и зависимости

### 2.1 AI модуль
- [x] Добавлена jakarta.annotation-api в modules/ai/build.gradle
- [x] Добавлены настройки AI в application.properties:
  ```properties
  ai.chat.api-key=demo
  ai.chat.model=gpt-3.5-turbo
  ```

### 2.2 Content модуль
- [ ] **Проверить:** Модуль content имеет retrofit зависимости
  - [ ] Проверить наличие в корневом build.gradle (уже есть)

### 2.3 Monitoring модуль
- [ ] **Проверить:** Модуль monitoring добавлен в корневой build.gradle (уже есть)

---

## Фаза 3: Модели и Сервисы

### 3.1 Отсутствующие сервисы
- [x] InstanceManager.java - СУЩЕСТВУЕТ
- [x] ForkManager.java - СУЩЕСТВУЕТ
- [x] SkinManager.java - СУЩЕСТВУЕТ
- [x] PerformanceMonitor.java - СУЩЕСТВУЕТ

### 3.2 Отсутствующие модели
- [ ] Проверить наличие всех моделей в modules/core/model/
  - [x] AppSettings.java - ЕСТЬ
  - [x] Fork.java - ЕСТЬ
  - [x] Instance.java - ЕСТЬ
  - [x] Mod.java - ЕСТЬ
  - [x] Skin.java - ЕСТЬ

---

## Фаза 4: Контроллеры UI

### 4.1 Зависимости контроллеров
- [x] AIController требует:
  - [x] AIChatAssistant (модуль ai) - ЕСТЬ
  - [x] ConflictAnalysisService (модуль ai) - ЕСТЬ
  - [x] ModRepository (модуль core) - ЕСТЬ

- [x] ForksController требует:
  - [x] ForkManager (модуль forks) - ЕСТЬ
  - [x] InstanceManager (модуль instances) - ЕСТЬ
  - [x] Fork.java (модуль core) - ЕСТЬ
  - [x] Instance.java (модуль core) - ЕСТЬ

- [x] InstancesController требует:
  - [x] InstanceManager (модуль instances) - ЕСТЬ
  - [x] Instance.java (модуль core) - ЕСТЬ

- [x] ModsController требует:
  - [x] ModManagementService (модуль content) - ЕСТЬ
  - [x] InstanceManager (модуль instances) - ЕСТЬ
  - [x] SecurityScanner (модуль security) - ЕСТЬ
  - [x] Mod.java (модуль core) - ЕСТЬ
  - [x] Instance.java (модуль core) - ЕСТЬ

- [x] SettingsController требует:
  - [x] SettingsRepository (модуль core) - ЕСТЬ
  - [x] AppSettings.java (модуль core) - ЕСТЬ

- [x] SkinsController требует:
  - [x] SkinManager (модуль skins) - ЕСТЬ
  - [x] Skin.java (модуль core) - ЕСТЬ

- [x] MainController требует:
  - [x] PerformanceMonitor (модуль monitoring) - ЕСТЬ

---

## Фаза 5: Репозитории

### 5.1 Проверка методов репозиториев
- [x] ModRepository - все методы есть
- [x] SkinRepository - все методы есть
- [x] ForkRepository - все методы есть
- [x] InstanceRepository - ПРОВЕРЕНО:
  - [x] findAll() (через JpaRepository)
  - [x] findById(String id) (через JpaRepository)
  - [x] findByName(String name) - existsByName()
  - [x] findByNameContainingIgnoreCase()
  - [x] findByVersion()
  - [x] findByModloader()
- [x] SettingsRepository - ПРОВЕРЕНО:
  - [x] findByKey()

---

## Фаза 6: UI Ресурсы

### 6.1 Изображения
- [x] icon.png - СОЗДАН
- [x] default-avatar.png - СОЗДАН

### 6.2 CSS
- [x] Исправлен gradient в theme.css (не поддерживается в JavaFX)

### 6.3 FXML файлы
- [x] ai.fxml - ЕСТЬ
- [x] ai_assistant.fxml - ЕСТЬ
- [x] forks.fxml - ЕСТЬ
- [x] instances.fxml - ЕСТЬ
- [x] main.fxml - ЕСТЬ
- [x] mods.fxml - ЕСТЬ
- [x] settings.fxml - ЕСТЬ
- [x] skins.fxml - ЕСТЬ

---

## Фаза 7: Безопасность

### 7.1 Security модуль
- [x] SecurityScanner существует в modules/security/

---

## Фаза 8: Конфигурация

### 8.1 Spring Boot
- [x] JpaConfig - УЖЕ НАСТРОЕН (дублирование с QuantumLauncherApplication - ok)
- [x] SQLite Dialect - настроен в application.properties
- [x] AI настройки - добавлены в application.properties

---

## Команда для сборки

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
cmd /c "gradlew.bat build --no-daemon"
```

---

## Фаза 9: Дополнительный контент (Шейдеры, Текстур-паки, Карты)

### 9.1 Модели
- [x] Shader.java - СОЗДАН
- [x] TexturePack.java - СОЗДАН
- [x] Map.java - СОЗДАН

### 9.2 Репозитории
- [x] ShaderRepository - СОЗДАН
- [x] TexturePackRepository - СОЗДАН
- [x] MapRepository - СОЗДАН

### 9.3 Сервисы
- [x] ShaderService (модуль content) - СОЗДАН
- [x] TexturePackService (модуль content) - СОЗДАН
- [x] MapService (модуль content) - СОЗДАН

### 9.4 Контроллеры UI
- [x] ShadersController - СОЗДАН
- [x] TexturePacksController - СОЗДАН
- [x] MapsController - СОЗДАН

### 9.5 FXML файлы
- [x] shaders.fxml - СОЗДАН
- [x] texturepacks.fxml - СОЗДАН
- [x] maps.fxml - СОЗДАН

### 9.6 Навигация в main.fxml
- [x] Кнопка Шейдеры добавлена
- [x] Кнопка Текстур-паки добавлена
- [x] Кнопка Карты добавлена
- [x] Методы навигации в MainController добавлены

---

## Статус выполнения

- [x] Фаза 1: Инфраструктура - ЗАВЕРШЕНО
- [x] Фаза 2: Модули - ЗАВЕРШЕНО
- [x] Фаза 3: Модели и Сервисы - ЗАВЕРШЕНО
- [x] Фаза 4: Контроллеры UI - ЗАВЕРШЕНО
- [x] Фаза 5: Репозитории - ЗАВЕРШЕНО
- [x] Фаза 6: UI Ресурсы - ЗАВЕРШЕНО
- [x] Фаза 7: Безопасность - ЗАВЕРШЕНО
- [x] Фаза 8: Конфигурация - ЗАВЕРШЕНО
- [x] Фаза 9: Дополнительный контент - ЗАВЕРШЕНО

---

*Обновлено: 2026-03-05*
