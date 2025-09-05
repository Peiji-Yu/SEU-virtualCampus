package com.example.logindemo.Component;

import com.example.logindemo.Util.InputAnimation;
import com.example.logindemo.Util.Resources;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
public class UsernameInput extends AnchorPane {
     Label placeHolder;
     Rectangle background;
     Label icon;

     Rectangle rectangle;

     Pane pane;

     TextField textField;
     boolean isFirstPlay = true;
     boolean isChanging;

    public UsernameInput(String placeholder,TextField textField) {
        this.setStyle("-fx-background-color: transparent;");
        background = new Rectangle(250,30);
        background.setFill(Paint.valueOf("#ffffff00"));
        background.setStroke(Resources.DISABLED);
        background.setStrokeWidth(1.5);
        background.setArcHeight(8);
        background.setArcWidth(8);
        this.getChildren().add(background);
        this.setMaxSize(250,30);
        icon = new Label();
        icon.setFont(Resources.ICON_FONT);
        icon.setText("\ue903");
        icon.setPrefSize(25,27);
        icon.setTextFill(Resources.DISABLED);
        this.getChildren().add(icon);
        AnchorPane.setRightAnchor(icon,8.0);
        rectangle = new Rectangle(75,18);
        rectangle.setFill(Resources.WHITE);
        rectangle.setTranslateY(-10);
        this.textField = textField;
        textField.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;-fx-font-size: 14px;-fx-text-fill: #5BA3E7");
        textField.setFont(Resources.ROBOTO_REGULAR);
        textField.setPrefSize(220,28);
        textField.setFocusTraversable(false);
        textField.setTranslateX(-10);
        placeHolder = new Label(placeholder);
        placeHolder.setFont(Resources.ROBOTO_REGULAR);
        placeHolder.setTextFill(Resources.DISABLED);
        placeHolder.setPrefHeight(28);
        pane = new Pane(placeHolder,textField);
        this.setPadding(new Insets(0,0,0,5));
        this.getChildren().add(pane);
        AnchorPane.setLeftAnchor(pane,5.0);
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                InputAnimation.forwardAnimationPlay(this, Duration.seconds(0.2));
            } else {
                if(!isChanging) {
                    InputAnimation.reverseAnimationPlay(this, Duration.seconds(0.2));
                }

            }
        });
    }

    public Label getPlaceHolder() {
        return placeHolder;
    }

    public Rectangle getBackgroundShape() {
        return background;
    }

    public Label getIcon() {
        return icon;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public Pane getPane() {
        return pane;
    }

    public boolean isFirstPlay() {
        return isFirstPlay;
    }

    public void setFirstPlay(boolean firstPlay) {
        isFirstPlay = firstPlay;
    }
}
