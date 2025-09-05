package com.example.logindemo.Util;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class FadeAnimation {
    public static void fadeIn(Duration duration, Node node,Duration delay) {
        FadeTransition fadeTransition = new FadeTransition(duration,node);
        fadeTransition.setToValue(1);
        fadeTransition.setDelay(delay);
        fadeTransition.play();
        node.setDisable(false);
    }

    public static void fadeOut(Duration duration,Node node){
        FadeTransition fadeTransition = new FadeTransition(duration,node);
        fadeTransition.setToValue(0);
        fadeTransition.play();
        node.setDisable(true);
    }
}
