package com.example.logindemo.Util;

import com.example.logindemo.Component.UsernameInput;
import javafx.animation.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class InputAnimation {
    public static void reverseAnimationPlay(UsernameInput usernameInput, Duration duration){
        if(usernameInput.getPane().getChildren().get(1) instanceof TextField textField){
            if(textField.getText().length() == 0) {
                StrokeTransition strokeTransition = new StrokeTransition(duration, usernameInput.getBackgroundShape(),Resources.SECONDARY,Resources.DISABLED);
                strokeTransition.play();
                TranslateTransition translateTransition= new TranslateTransition(duration, usernameInput.getPlaceHolder());
                translateTransition.setByY(14);
                translateTransition.play();
                ScaleTransition scaleTransition = new ScaleTransition(duration, usernameInput.getPlaceHolder());
                scaleTransition.setToY(1);
                scaleTransition.setToX(1);
                scaleTransition.play();
                Timeline colorTransition = new Timeline(new KeyFrame(duration,new KeyValue(usernameInput.getPlaceHolder().textFillProperty(),Resources.DISABLED)));
                Timeline colorTransition2 = new Timeline(new KeyFrame(duration,new KeyValue(usernameInput.getIcon().textFillProperty(),Resources.DISABLED)));
                colorTransition.play();
                colorTransition2.play();
                ScaleTransition scaleTransition1 = new ScaleTransition(duration, usernameInput.getRectangle());
                scaleTransition1.setFromX(1);
                scaleTransition1.setToX(0);
                scaleTransition1.play();
            }else {
                StrokeTransition strokeTransition = new StrokeTransition(duration, usernameInput.getBackgroundShape(),Resources.SECONDARY,Resources.DISABLED);
                strokeTransition.play();
                Timeline colorTransition = new Timeline(new KeyFrame(duration,new KeyValue(usernameInput.getPlaceHolder().textFillProperty(),Resources.DISABLED)));
                Timeline colorTransition2 = new Timeline(new KeyFrame(duration,new KeyValue(usernameInput.getIcon().textFillProperty(),Resources.DISABLED)));
                colorTransition.play();
                colorTransition2.play();
                Label label = new Label();
                label.setText(textField.getText());
                label.setStyle("-fx-font-size: 14px;-fx-text-fill: #B4C0C7;-fx-background-color: white");
                label.setFont(Resources.ROBOTO_REGULAR);
                label.setPrefHeight(15);
                usernameInput.getChildren().add(label);
                AnchorPane.setLeftAnchor(label,5.0);
                AnchorPane.setTopAnchor(label,6.0);
                Timeline colorTransition1 = new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(label.textFillProperty(),Resources.SECONDARY)),
                        new KeyFrame(Duration.seconds(0.2),new KeyValue(label.textFillProperty(),Resources.DISABLED)));
                colorTransition1.play();
                textField.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;-fx-font-size: 14px;-fx-text-fill: #B4C0C7;");
                colorTransition1.setOnFinished(event -> {
                    usernameInput.getChildren().remove(4);
                });
            }
        }

    }

    public static void forwardAnimationPlay(UsernameInput usernameInput, Duration duration) {
        StrokeTransition strokeTransition = new StrokeTransition(duration, usernameInput.getBackgroundShape(),Resources.DISABLED,Resources.SECONDARY);
        strokeTransition.play();
        AnchorPane.setLeftAnchor(usernameInput.getPane(),5.0);
        ScaleTransition scaleTransition1 = new ScaleTransition(duration, usernameInput.getRectangle());
        scaleTransition1.setFromX(0);
        scaleTransition1.setToX(1);
        if(usernameInput.getPane().getChildren().get(1) instanceof TextField textField) {
            if(textField.getText().length() == 0){
                TranslateTransition translateTransition= new TranslateTransition(duration, usernameInput.getPlaceHolder());
                translateTransition.setByY(-14);
                translateTransition.play();
                scaleTransition1.play();
            } else {
                Label label = new Label();
                label.setStyle("-fx-font-size: 14px;-fx-text-fill: #B4C0C7;-fx-background-color: white");
                label.setFont(Resources.ROBOTO_REGULAR);
                label.setPrefHeight(15);
                usernameInput.getChildren().add(label);
                AnchorPane.setLeftAnchor(label,5.0);
                AnchorPane.setTopAnchor(label,6.0);
                Timeline colorTransition = new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(label.textFillProperty(),Resources.DISABLED)),
                new KeyFrame(Duration.seconds(0.2),new KeyValue(label.textFillProperty(),Resources.SECONDARY)));
                colorTransition.play();
                textField.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;-fx-font-size: 14px;-fx-text-fill: #5BA3E7");
                colorTransition.setOnFinished(event -> {
                    usernameInput.getChildren().remove(4);
                });
            }
        }
        ScaleTransition scaleTransition = new ScaleTransition(duration, usernameInput.getPlaceHolder());
        scaleTransition.setToY(0.9);
        scaleTransition.setToX(0.9);
        scaleTransition.play();
        Timeline colorTransition = new Timeline(new KeyFrame(duration,new KeyValue(usernameInput.getPlaceHolder().textFillProperty(),Resources.SECONDARY)));
        Timeline colorTransition2 = new Timeline(new KeyFrame(duration,new KeyValue(usernameInput.getIcon().textFillProperty(),Resources.SECONDARY)));
        colorTransition.play();
        colorTransition2.play();
        if(usernameInput.isFirstPlay()){
            usernameInput.getChildren().set(2, usernameInput.getRectangle());
            AnchorPane.setLeftAnchor(usernameInput.getRectangle(),5.0);
            usernameInput.getChildren().add(usernameInput.getPane());
            usernameInput.setFirstPlay(false);
            scaleTransition1.play();
        }
    }
}
