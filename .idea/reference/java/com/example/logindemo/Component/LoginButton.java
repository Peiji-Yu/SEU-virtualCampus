package com.example.logindemo.Component;

import com.example.logindemo.Util.Resources;
import javafx.animation.FillTransition;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class LoginButton extends Pane {
    public LoginButton() {
        Rectangle background = new Rectangle(250,34);
        background.setFill(Resources.PRIMARY);
        background.setArcWidth(6);
        background.setArcHeight(6);
        this.getChildren().add(background);
        Label label = new Label("Login");
        label.setFont(Resources.ROBOTO_REGULAR);
        label.setTextFill(Resources.WHITE);
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(label);
        borderPane.setMinSize(250,34);
        this.getChildren().add(borderPane);
        this.addEventHandler(MouseEvent.MOUSE_ENTERED,event -> {
            FillTransition fillTransition = new FillTransition(Duration.seconds(0.2),background,Resources.PRIMARY,Resources.SECONDARY);
            fillTransition.play();
            this.setCursor(Cursor.HAND);
        });
        this.addEventHandler(MouseEvent.MOUSE_EXITED,event -> {
            FillTransition fillTransition = new FillTransition(Duration.seconds(0.2),background,Resources.SECONDARY,Resources.PRIMARY);
            fillTransition.play();
            this.setCursor(Cursor.DEFAULT);
        });
    }
}
