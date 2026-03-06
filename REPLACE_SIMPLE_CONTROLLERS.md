# Промт: Замена упрощённых контроллеров на полноценные + Удаление CLI

## Задача

1. Заменить упрощённые контроллеры в `modules/ui/src/main/java/com/quantumlauncher/ui/controller/` на полноценные версии из `src/main/java/com/quantumlauncher/ui/controller/`.

2. **УБРАТЬ ВЕСЬ ИНТЕРФЕЙС КОМАНДНОЙ СТРОКИ (CLI)** — приложение должно работать ТОЛЬКО через графический интерфейс JavaFX:
   - Удалить или закомментировать любые `System.out.println()` которые не являются debug-логами
   - Удалить все `Scanner`, `BufferedReader` для ввода с консоли
   - Убрать все `args` из main методов (где они не нужны для запуска)
   - Убрать вывод в консоль при запуске (ASCII-art логотип можно оставить как визуальный элемент в UI)
   - Оставить только логирование через SLF4J (logger.info, logger.error и т.д.)

## Файлы для замены

### 1. InstancesController
- **Упрощённая версия:** `modules/ui/src/main/java/com/quantumlauncher/ui/controller/InstancesController.java`
- **Полноценная версия:** `src/main/java/com/quantumlauncher/ui/controller/InstancesController.java`
- **Действие:** Удалить упрощённую версию, использовать полноценную

### 2. SettingsController  
- **Упрощённая версия:** `modules/ui/src/main/java/com/quantumlauncher/ui/controller/SettingsController.java`
- **Полноценная версия:** `src/main/java/com/quantumlauncher/ui/controller/SettingsController.java`
- **Действие:** Удалить упрощённую версию, использовать полноценную

## Что нужно сделать

### Часть 1: Замена контроллеров

1. Удалить файл `modules/ui/src/main/java/com/quantumlauncher/ui/controller/InstancesController.java`
2. Удалить файл `modules/ui/src/main/java/com/quantumlauncher/ui/controller/SettingsController.java`
3. Проверить, что в `settings.gradle` модуль `ui` указывает на правильную директорию
4. Убедиться, что полноценные контроллеры в `src/main/java/...` используют:
   - `@Component` аннотацию для Spring
   - `@Autowired` для внедрения сервисов
   - Реализуют `javafx.fxml.Initializable`

### Часть 2: Удаление CLI (только графика!)

1. **Удалить/закомментировать System.out.println** в файлах:
   - `src/main/java/com/quantumlauncher/QuantumLauncher.java` — убрать ASCII-art вывод в консоли при запуске
   - Оставить только logger.info() для логирования

2. **Убрать Scanner и консольный ввод** — никаких запросов к пользователю через консоль

3. **Main методы** — должны просто запускать приложение без ожидания ввода:
   ```java
   public static void main(String[] args) {
       // Только запуск, без обработки аргументов
       Application.launch(QuantumLauncherApp.class, args);
   }
   ```

4. **Оставить только:**
   - Графический интерфейс JavaFX (окна, кнопки, меню)
   - Логирование через SLF4J (logger.info, logger.warn, logger.error)
   - Диалоговые окна JavaFX (Alert, Dialog) для взаимодействия с пользователем

5. **Удалить или переделать** классы, которые зависят от консольного ввода/вывода

## Ожидаемый результат

1. Проект должен использовать только полноценные контроллеры из основной директории `src/main/java/`, а дубликаты из `modules/ui/` должны быть удалены.

2. **Приложение работает ТОЛЬКО через графический интерфейс:**
   - Запуск происходит автоматически без аргументов командной строки
   - Нет никаких запросов в консоли
   - Все взаимодействие с пользователем через JavaFX UI (окна, кнопки, диалоги)
   - Единственный вывод в консоль — это логи (через SLF4J logger)

## Примечание

В `modules/ui/` могут остаться другие файлы, если они не являются дубликатами. Проверить содержимое директории перед удалением.

## Проверка после изменений

После выполнения всех изменений запустить приложение и убедиться:
1. Приложение запускается с графическим окном
2. В консоли только логи (нет приглашений ввода, нет лишнего вывода)
3. Все кнопки и меню работают
4. Нет ошибок при запуске
