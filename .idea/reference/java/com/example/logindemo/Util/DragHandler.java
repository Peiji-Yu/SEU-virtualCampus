package com.example.logindemo.Util;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class DragHandler implements EventHandler<MouseEvent> {
    private Stage primaryStage;
    private double oldX;
    private double oldY;
    private double screenX;
    private double screenY;

    public DragHandler(Stage primaryStage){
        this.primaryStage = primaryStage;
    }

    @Override
    public void handle(MouseEvent event) {
        if(event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            this.oldX = event.getX();
            this.oldY = event.getY();
            this.screenX = event.getScreenX();
            this.screenY = event.getScreenY();
        } else if(event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            this.primaryStage.setX(event.getScreenX() - oldX );
            this.primaryStage.setY(event.getScreenY() - oldY );
        }
    }
}
