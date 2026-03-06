package com.quantumlauncher.ui.component;

import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

/**
 * 3D Preview персонажа для отображения скинов
 */
public class SkinPreview3D extends Group {
    
    private final Group character = new Group();
    private double lastMouseX, lastMouseY;
    private boolean isDragging = false;
    
    // Цвета по умолчанию
    private Color skinColor = new Color(0.8, 0.7, 0.6, 1.0);
    private Color outfitColor = new Color(0.2, 0.3, 0.5, 1.0);
    
    public SkinPreview3D() {
        setupScene();
        setupMouseHandlers();
    }
    
    private void setupScene() {
        // Создание частей тела
        createHead();
        createBody();
        createArms();
        createLegs();
        
        character.setTranslateX(0);
        character.setTranslateY(50);
        
        getChildren().add(character);
        
        // Начальный поворот
        character.setRotationAxis(new Point3D(0, 1, 0));
        character.setRotate(30);
    }
    
    private void createHead() {
        // Голова - куб
        Box head = new Box(40, 40, 40);
        head.setTranslateY(-70);
        
        PhongMaterial headMaterial = new PhongMaterial();
        headMaterial.setDiffuseColor(skinColor);
        headMaterial.setSpecularColor(new Color(0.3, 0.3, 0.3, 1.0));
        head.setMaterial(headMaterial);
        
        character.getChildren().add(head);
        
        // Лицо (упрощённо)
        createFace();
    }
    
    private void createFace() {
        // Глаза
        Box leftEye = new Box(8, 8, 2);
        leftEye.setTranslateX(-10);
        leftEye.setTranslateY(-75);
        leftEye.setTranslateZ(20);
        leftEye.setMaterial(new PhongMaterial(Color.BLACK));
        
        Box rightEye = new Box(8, 8, 2);
        rightEye.setTranslateX(10);
        rightEye.setTranslateY(-75);
        rightEye.setTranslateZ(20);
        rightEye.setMaterial(new PhongMaterial(Color.BLACK));
        
        // Рот
        Box mouth = new Box(16, 4, 2);
        mouth.setTranslateY(-62);
        mouth.setTranslateZ(20);
        mouth.setMaterial(new PhongMaterial(Color.BLACK));
        
        character.getChildren().addAll(leftEye, rightEye, mouth);
    }
    
    private void createBody() {
        // Тело
        Box torso = new Box(40, 60, 20);
        torso.setTranslateY(-30);
        
        PhongMaterial bodyMaterial = new PhongMaterial();
        bodyMaterial.setDiffuseColor(outfitColor);
        torso.setMaterial(bodyMaterial);
        
        character.getChildren().add(torso);
        
        // Куртка/броня
        Box jacket = new Box(42, 62, 22);
        jacket.setTranslateY(-30);
        jacket.setTranslateZ(-1);
        
        PhongMaterial jacketMaterial = new PhongMaterial();
        jacketMaterial.setDiffuseColor(outfitColor.brighter());
        jacketMaterial.setSpecularColor(new Color(0.2, 0.2, 0.2, 1.0));
        jacket.setMaterial(jacketMaterial);
        
        character.getChildren().add(jacket);
    }
    
    private void createArms() {
        // Левая рука
        Box leftArm = new Box(15, 60, 15);
        leftArm.setTranslateX(-32);
        leftArm.setTranslateY(-20);
        
        PhongMaterial armMaterial = new PhongMaterial();
        armMaterial.setDiffuseColor(skinColor);
        leftArm.setMaterial(armMaterial);
        
        // Правая рука
        Box rightArm = new Box(15, 60, 15);
        rightArm.setTranslateX(32);
        rightArm.setTranslateY(-20);
        
        PhongMaterial armMaterial2 = new PhongMaterial();
        armMaterial2.setDiffuseColor(skinColor);
        rightArm.setMaterial(armMaterial2);
        
        character.getChildren().addAll(leftArm, rightArm);
    }
    
    private void createLegs() {
        // Левая нога
        Box leftLeg = new Box(18, 60, 18);
        leftLeg.setTranslateX(-12);
        leftLeg.setTranslateY(40);
        
        PhongMaterial legMaterial = new PhongMaterial();
        legMaterial.setDiffuseColor(outfitColor.darker());
        leftLeg.setMaterial(legMaterial);
        
        // Правая нога
        Box rightLeg = new Box(18, 60, 18);
        rightLeg.setTranslateX(12);
        rightLeg.setTranslateY(40);
        
        PhongMaterial legMaterial2 = new PhongMaterial();
        legMaterial2.setDiffuseColor(outfitColor.darker());
        rightLeg.setMaterial(legMaterial2);
        
        character.getChildren().addAll(leftLeg, rightLeg);
    }
    
    private void setupMouseHandlers() {
        setOnMousePressed((MouseEvent e) -> {
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
            isDragging = true;
            e.consume();
        });
        
        setOnMouseDragged((MouseEvent e) -> {
            if (isDragging) {
                double deltaX = e.getSceneX() - lastMouseX;
                double deltaY = e.getSceneY() - lastMouseY;
                
                character.setRotate(character.getRotate() + deltaX * 0.5);
                character.setRotationAxis(new Point3D(0, 1, 0));
                
                // Ограничение наклона
                double currentTilt = character.getRotate();
                // Простой tilt через rotate X
                
                lastMouseX = e.getSceneX();
                lastMouseY = e.getSceneY();
                e.consume();
            }
        });
        
        setOnMouseReleased((MouseEvent e) -> {
            isDragging = false;
            e.consume();
        });
    }
    
    /**
     * Автоматическое вращение
     */
    public void startAutoRotate() {
        RotateTransition rotate = new RotateTransition(Duration.seconds(8), character);
        rotate.setFromAngle(0);
        rotate.setToAngle(360);
        rotate.setAxis(new Point3D(0, 1, 0));
        rotate.setCycleCount(Timeline.INDEFINITE);
        rotate.play();
    }
    
    /**
     * Остановка вращения
     */
    public void stopAutoRotate() {
        // Остановка анимаций - очищаем все дочерние анимации
        character.getChildren().clear();
    }
    
    /**
     * Установка скина из изображения
     */
    public void setSkinTexture(Image image) {
        // В реальной реализации - наложение текстуры на 3D модель
        // Пока заглушка
    }
    
    /**
     * Установка цвета кожи
     */
    public void setSkinColor(Color color) {
        this.skinColor = color;
        // Обновление материалов
    }
    
    /**
     * Установка цвета одежды
     */
    public void setOutfitColor(Color color) {
        this.outfitColor = color;
        // Обновление материалов
    }
}
