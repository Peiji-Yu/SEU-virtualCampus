package com.example.logindemo.Component;

import com.example.logindemo.Util.ColorTransition;
import com.example.logindemo.Util.DragHandler;
import com.example.logindemo.Util.Resources;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

public class Header extends BorderPane {
    int height;
    int width;

    public Header(int height, int width) {
        this.height = height;
        this.width = width;
        this.setPrefSize(width,height);
        this.setMinSize(width,height);
        this.setMaxSize(width,height);
        Label label = new Label();
        label.setFont(Resources.ICON_FONT_MIN);
        label.setText("\ue900");
        label.setStyle("-fx-text-fill: #CDCDCD");
        label.setPrefSize(20,height);
        label.addEventHandler(MouseEvent.MOUSE_CLICKED,event -> {
            Platform.exit();
        });

        label.addEventHandler(MouseEvent.MOUSE_ENTERED,event -> {
            ColorTransition colorTransition = new ColorTransition(label, Duration.seconds(0.2),Paint.valueOf("#3d3d3d"));
            colorTransition.play();
            this.setCursor(Cursor.HAND);
        });

        label.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET,event -> {
            ColorTransition colorTransition = new ColorTransition(label,Duration.seconds(0.2),Paint.valueOf("#cdcdcd"));
            colorTransition.play();
        });
        this.setRight(label);
    }

    public void setTransparent(boolean transparent) {
        if(transparent) {
            this.setStyle("-fx-background-color: transparent");
        }
    }

    public void setDragHandler(DragHandler dragHandler) {
        this.setOnMousePressed(dragHandler);
        this.setOnMouseDragged(dragHandler);
    }
}
