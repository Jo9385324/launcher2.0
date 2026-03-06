# QuantumLauncher - Ошибки и необходимые доработки

## 🔴 Критические проблемы (Блокируют сборку)

### 1. Несовместимость JDK 25 и Gradle 8.11
**Проблема:** Gradle 8.11 не поддерживает JDK 25 (class file version 69)
**Ошибка:**
```
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
Unsupported class file major version 69
```
**Решение:**
- [ ] **Вариант А:** Установить JDK 21 и использовать его (рекомендуется)
- [ ] **Вариант Б:** Обновить Gradle до версии 9.x в `gradle/wrapper/gradle-wrapper.properties`:
  ```properties
  distributionUrl=https\://services.gradle.org/distributions/gradle-9.0.0-milestone-1-bin.zip
  ```

---

## 🟠 Ошибки компиляции (после исправления JDK)

### 2. Отсутствующие Java файлы в модулях

#### 2.1 API контроллеры - проблемы с импортами
**Файлы:**
- `modules/api/src/main/java/com/quantumlauncher/api/controller/*.java`

**Проблемы:**
- [ ] Возможные конфликты пакетов (ui.controller vs api.controller)
- [ ] Проверить импорты `com.quantumlauncher.ui.controller.*`

#### 2.2 AIChatAssistant - зависимость jakarta.annotation
**Файл:** `modules/ai/src/main/java/com/quantumlauncher/ai/service/AIChatAssistant.java:15`
**Проблема:** Использует `jakarta.annotation.PostConstruct` но модуль ai не имеет зависимости jakarta
**Решение:** Добавить в `modules/ai/build.gradle`:
```gradle
implementation 'jakarta.annotation:jakarta.annotation-api:2.1.1'
```

#### 2.3 ModsController - проблема с enabled полем
**Файл:** `src/main/java/com/quantumlauncher/ui/controller/ModsController.java:109`
**Проблема:** `mod.getEnabled()` может вернуть null
**Решение:**
```java
boolean enabled = mod.getEnabled() != null && mod.getEnabled();
```

### 3. Проблемы с зависимостями модулей

#### 3.1 Модуль monitoring - отсутствует в основном build.gradle
**Проблема:** В `build.gradle` (корневой) нет зависимости `implementation project(':monitoring')`
**Решение:** Добавить в корневой `build.gradle` в секцию dependencies:
```gradle
implementation project(':monitoring')
```

#### 3.2 Модуль content - конфликт зависимостей
**Проблема:** Модуль content использует retrofit, но он не добавлен в корневой build.gradle
**Решение:** Добавить в корневой `build.gradle`:
```gradle
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
```

#### 3.3 Модуль instances - конфликт имён с ui.controller
**Файл:** `modules/instances/src/main/java/com/quantumlauncher/instances/service/InstanceManager.java`
**Проблема:** Пакет сервисов InstanceManager может конфликтовать с контроллером
**Решение:** Убедиться что инжектится правильный бин

---

## 🟡 Проблемы конфигурации

### 4. JpaConfig - конфликт с QuantumLauncherApplication
**Проблема:** JpaConfig дублирует конфигурацию из QuantumLauncherApplication
**Файлы:**
- `modules/core/src/main/java/com/quantumlauncher/core/config/JpaConfig.java`
- `src/main/java/com/quantumlauncher/QuantumLauncherApplication.java`

**Решение:** Удалить дублирующиеся аннотации из JpaConfig или объединить конфигурации

### 5. SQLite Dialect проблемы
**Проблема:** `org.hibernate.community.dialect.SQLiteDialect` может отсутствовать
**Файл:** `src/main/resources/application.properties:17`
**Решение:** Проверить наличие класса или использовать кастомный диалект

### 6. Отсутствующие ресурсы

#### 6.1 Изображения
**Проблема:** FXML ссылаются на несуществующие изображения:
- `/images/icon.png`
- `/images/default-avatar.png`

**Решение:** [ ] Создать placeholder изображения или обновить FXML

#### 6.2 CSS файл
**Файл:** `src/main/resources/css/theme.css`
**Проблема:** Не проверен на наличие ошибок CSS
**Решение:** [ ] Проверить и исправить синтаксис CSS

---

## 🟢 Проблемы бизнес-логики

### 7. AIChatAssistant - работает в демо-режиме
**Файл:** `modules/ai/src/main/java/com/quantumlauncher/ai/service/AIChatAssistant.java`
**Проблема:** AI активируется только при наличии API ключа в конфигурации
**Решение:** Добавить в `application.properties`:
```properties
ai.chat.api-key=demo
ai.chat.model=gpt-3.5-turbo
```

### 8. ModRepository - findByInstanceId может отсутствовать
**Проблема:** Не проверено наличие метода в репозитории
**Файл:** `modules/core/src/main/java/com/quantumlauncher/core/repository/ModRepository.java`
**Решение:** Добавить если отсутствует:
```java
List<Mod> findByInstanceId(String instanceId);
```

### 9. SkinRepository - findByPlayerUuid может отсутствовать
**Проблема:** Не проверено наличие метода в репозитории
**Файл:** `modules/core/src/main/java/com/quantumlauncher/core/repository/SkinRepository.java`
**Решение:** Добавить если отсутствует:
```java
List<Skin> findByPlayerUuid(String playerUuid);
Optional<Skin> findByPlayerUuidAndActive(String playerUuid, boolean active);
```

### 10. ForkRepository - методы могут отсутствовать
**Проблема:** ForkManager использует методы которых может не быть
**Файл:** `modules/core/src/main/java/com/quantumlauncher/core/repository/ForkRepository.java`
**Решение:** Добавить:
```java
List<Fork> findByOfficial(boolean official);
List<Fork> findByMcVersion(String mcVersion);
List<Fork> findByModloader(String modloader);
```

---

## 🔵 UI/UX проблемы

### 11. FXML - проблемы с синтаксисом

#### 11.1 main.fxml - некорректные event handlers
**Файл:** `src/main/resources/fxml/main.fxml:42-52`
**Проблема:** Использование `#{}` вместо `${}` для inline обработчиков
**Текущий код:**
```xml
onMouseEntered="#{btnInstances.setStyle(...)}"
```
**Должно быть:** Исправлено в контроллере через setOnAction/setOnMouseEntered

#### 11.2 AIController - TabPane может отсутствовать в FXML
**Проблема:** AIController использует fx:id для TabPane и Tab, но они могут отсутствовать
**Файл:** `src/main/resources/fxml/ai.fxml`
**Решение:** [ ] Проверить/создать ai.fxml с необходимыми элементами

### 12. Контроллеры - отсутствующие FXML

#### 12.1 ForksController
**Проблема:** ForksController существует но не проверен fxml/forks.fxml
**Решение:** [ ] Проверить соответствие fx:id в ForksController и fxml

#### 12.2 SettingsController
**Проблема:** SettingsController существует но не проверен fxml/settings.fxml
**Решение:** [ ] Проверить соответствие fx:id

#### 12.3 SkinsController
**Проблема:** SkinsController существует но не проверен fxml/skins.fxml
**Решение:** [ ] Проверить соответствие fx:id

### 13. SkinPreview3D - 3D preview может не работать
**Файл:** `src/main/java/com/quantumlauncher/ui/component/SkinPreview3D.java`
**Проблема:** JavaFX 3D требует дополнительной настройки
**Решение:** [ ] Реализовать упрощённую версию или использовать 2D preview

---

## ⚪ Рекомендуемые улучшения

### 14. Безопасность

#### 14.1 API ключи в коде
**Проблема:** API ключи могут быть захардкожены
**Решение:** [ ] Вынести все ключи в application.properties или environment variables

#### 14.2 CloudStorageService - отсутствует валидация
**Решение:** [ ] Добавить проверку credentials перед использованием

### 15. Обработка ошибок

#### 15.1 Глобальный exception handler
**Решение:** [ ] Добавить @ControllerAdvice для REST контроллеров

#### 15.2 UI feedback
**Решение:** [ ] Добавить toast/notification для пользователя при ошибках

### 16. Тестирование

**Решение:** [ ] Добавить unit тесты для:
- ConflictAnalysisService
- ModManagementService
- SecurityScanner
- InstanceManager

### 17. Логирование

**Проблема:** Некоторые классы используют System.out вместо логгера
**Решение:** [ ] Заменить все System.out на log

---

## 📋 Чеклист для сборки

### Перед сборкой:
- [ ] Исправить проблему с JDK (установить JDK 21 или Gradle 9)
- [ ] Проверить/добавить все зависимости модулей в корневой build.gradle
- [ ] Создать placeholder изображения (icon.png, default-avatar.png)
- [ ] Проверить все fx:id в FXML и контроллерах

### После успешной сборки:
- [ ] Протестировать навигацию между экранами
- [ ] Протестировать создание инстанса
- [ ] Протестировать AI модуль
- [ ] Протестировать безопасное сканирование

---

## 📝 Версии ПО

| Компонент | Текущая версия | Рекомендуемая |
|-----------|---------------|---------------|
| JDK | 25 | 21 LTS |
| Gradle | 8.11 | 8.11 + JDK 21 ИЛИ 9.0 |
| JavaFX | 21.0.1 | 21.0.1 |
| Spring Boot | 3.2.0 | 3.2.0 |
| Hibernate | 6.4.1 | 6.4.1 |

---

*Обновлено: 2026*
