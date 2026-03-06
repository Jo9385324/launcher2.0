package com.quantumlauncher.ui.controller;

import com.quantumlauncher.auth.model.MinecraftAccount;
import com.quantumlauncher.auth.service.MicrosoftAuthService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для управления аккаунтами Microsoft
 */
@Component
public class AccountsController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(AccountsController.class);
    
    @Autowired
    private MicrosoftAuthService authService;
    
    // UI Elements
    @FXML
    private ListView<AccountListItem> accountListView;
    
    @FXML
    private Button btnAddMicrosoft;
    
    @FXML
    private Button btnRemove;
    
    @FXML
    private Button btnSetDefault;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private ProgressIndicator progressIndicator;
    
    private ObservableList<AccountListItem> accounts = FXCollections.observableArrayList();
    
    // Device Flow
    private CompletableFuture<MinecraftAccount> currentAuthFuture;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Инициализация AccountsController");
        
        // Настройка ListView
        accountListView.setItems(accounts);
        accountListView.setCellFactory(listView -> new AccountListCell());
        
        // Обработчики кнопок
        btnAddMicrosoft.setOnAction(e -> startMicrosoftAuth());
        btnRemove.setOnAction(e -> removeSelectedAccount());
        btnSetDefault.setOnAction(e -> setAsDefault());
        
        // Загрузка аккаунтов
        loadAccounts();
    }
    
    /**
     * Загрузка списка аккаунтов
     */
    private void loadAccounts() {
        try {
            List<MinecraftAccount> accountList = authService.getAllAccounts();
            accounts.clear();
            
            for (MinecraftAccount account : accountList) {
                accounts.add(new AccountListItem(account));
            }
            
            updateStatus("Загружено аккаунтов: " + accounts.size());
        } catch (Exception e) {
            log.error("Ошибка загрузки аккаунтов", e);
            updateStatus("Ошибка загрузки аккаунтов");
        }
    }
    
    /**
     * Начало авторизации через Microsoft
     */
    private void startMicrosoftAuth() {
        try {
            updateStatus("Начало авторизации Microsoft...");
            progressIndicator.setVisible(true);
            btnAddMicrosoft.setDisable(true);
            
            // Запускаем Device Flow
            MicrosoftAuthService.DeviceCodeInfo deviceCode = authService.startDeviceFlow();
            
            // Показываем диалог с кодом
            showDeviceCodeDialog(deviceCode);
            
            // Ожидаем завершения авторизации
            currentAuthFuture = authService.waitForDeviceCodeAuth(
                    deviceCode.deviceCode, 
                    deviceCode.interval, 
                    deviceCode.expiresIn
            );
            
            currentAuthFuture.thenAccept(account -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnAddMicrosoft.setDisable(false);
                    
                    if (account != null) {
                        accounts.add(new AccountListItem(account));
                        updateStatus("Аккаунт добавлен: " + account.getUsername());
                        
                        // Обновляем профиль в MainController
                        // (через event или обновление)
                    } else {
                        updateStatus("Авторизация отменена");
                    }
                });
            }).exceptionally(e -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnAddMicrosoft.setDisable(false);
                    updateStatus("Ошибка: " + e.getMessage());
                    showErrorDialog("Ошибка авторизации", e.getMessage());
                });
                return null;
            });
            
        } catch (Exception e) {
            log.error("Ошибка начала авторизации", e);
            progressIndicator.setVisible(false);
            btnAddMicrosoft.setDisable(false);
            updateStatus("Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Показ диалога с кодом устройства
     */
    private void showDeviceCodeDialog(MicrosoftAuthService.DeviceCodeInfo deviceCode) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Авторизация Microsoft");
            alert.setHeaderText("Вход в аккаунт Minecraft");
            alert.setContentText(
                    "1. Откройте браузер и перейдите по адресу:\n" +
                    deviceCode.verificationUri + "\n\n" +
                    "2. Введите код: " + deviceCode.userCode + "\n\n" +
                    "3. Войдите в свой аккаунт Microsoft\n\n" +
                    "Ожидание авторизации..."
            );
            alert.initModality(Modality.APPLICATION_MODAL);
            
            // Кнопка для открытия браузера
            ButtonType openBrowserBtn = new ButtonType("Открыть в браузере");
            ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(openBrowserBtn, cancelBtn);
            
            // Добавляем обработчик для открытия браузера
            alert.showAndWait().ifPresent(result -> {
                if (result == openBrowserBtn) {
                    try {
                        // Открываем браузер
                        java.awt.Desktop.getDesktop().browse(new URI(deviceCode.verificationUri));
                    } catch (Exception e) {
                        log.warn("Не удалось открыть браузер", e);
                    }
                } else {
                    // Отмена
                    if (currentAuthFuture != null) {
                        currentAuthFuture.cancel(true);
                    }
                }
            });
        });
    }
    
    /**
     * Удаление выбранного аккаунта
     */
    private void removeSelectedAccount() {
        AccountListItem selected = accountListView.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showErrorDialog("Ошибка", "Выберите аккаунт для удаления");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удаление аккаунта");
        confirm.setContentText("Вы уверены, что хотите удалить аккаунт " + selected.getUsername() + "?");
        
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    authService.deleteAccount(selected.getId());
                    accounts.remove(selected);
                    updateStatus("Аккаунт удалён");
                } catch (Exception e) {
                    log.error("Ошибка удаления аккаунта", e);
                    showErrorDialog("Ошибка", "Не удалось удалить аккаунт: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Установка аккаунта по умолчанию
     */
    private void setAsDefault() {
        AccountListItem selected = accountListView.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showErrorDialog("Ошибка", "Выберите аккаунт");
            return;
        }
        
        try {
            authService.activateAccount(selected.getId());
            
            // Деактивируем остальные
            for (AccountListItem item : accounts) {
                if (!item.getId().equals(selected.getId())) {
                    authService.deactivateAccount(item.getId());
                }
            }
            
            loadAccounts();
            updateStatus("Аккаунт " + selected.getUsername() + " установлен по умолчанию");
        } catch (Exception e) {
            log.error("Ошибка установки аккаунта по умолчанию", e);
            showErrorDialog("Ошибка", e.getMessage());
        }
    }
    
    /**
     * Обновление статуса
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }
    
    /**
     * Показ диалога ошибки
     */
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Элемент списка аккаунтов
     */
    public static class AccountListItem {
        private final String id;
        private final String username;
        private final String uuid;
        private final boolean active;
        private final String skinUrl;
        
        public AccountListItem(MinecraftAccount account) {
            this.id = account.getId();
            this.username = account.getUsername();
            this.uuid = account.getUuid();
            this.active = account.isActive();
            this.skinUrl = account.getSkinUrl();
        }
        
        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getUuid() { return uuid; }
        public boolean isActive() { return active; }
        public String getSkinUrl() { return skinUrl; }
        
        public String getDisplayText() {
            return username + (active ? " (по умолчанию)" : "");
        }
    }
    
    /**
     * Ячейка списка аккаунтов
     */
    private static class AccountListCell extends ListCell<AccountListItem> {
        private final HBox content;
        private final ImageView avatarView;
        private final Label usernameLabel;
        private final Label statusLabel;
        
        public AccountListCell() {
            content = new HBox(15);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(10));
            
            avatarView = new ImageView();
            avatarView.setFitWidth(40);
            avatarView.setFitHeight(40);
            
            VBox textBox = new VBox(5);
            usernameLabel = new Label();
            usernameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
            
            statusLabel = new Label();
            statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #888888;");
            
            textBox.getChildren().addAll(usernameLabel, statusLabel);
            content.getChildren().addAll(avatarView, textBox);
            
            // Загружаем дефолтный аватар
            try {
                Image defaultAvatar = new Image(
                        AccountsController.class.getResourceAsStream("/images/default-avatar.png")
                );
                avatarView.setImage(defaultAvatar);
            } catch (Exception e) {
                // Игнорируем
            }
        }
        
        @Override
        protected void updateItem(AccountListItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
            } else {
                usernameLabel.setText(item.getUsername());
                statusLabel.setText(item.isActive() ? "По умолчанию" : "Активен");
                
                // Загрузка скина
                if (item.getSkinUrl() != null && !item.getSkinUrl().isEmpty()) {
                    // Асинхронная загрузка скина
                    new Thread(() -> {
                        try {
                            Image skin = new Image(item.getSkinUrl(), 40, 40, false, true);
                            Platform.runLater(() -> avatarView.setImage(skin));
                        } catch (Exception e) {
                            // Игнорируем
                        }
                    }).start();
                }
                
                setGraphic(content);
            }
        }
    }
}
