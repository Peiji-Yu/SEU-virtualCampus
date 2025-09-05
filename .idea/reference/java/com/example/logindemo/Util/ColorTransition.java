package com.example.logindemo.Util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

public class ColorTransition {
    private Timeline animation;

    public ColorTransition(Label label, Duration duration, Paint toColor) {
        animation = new Timeline(new KeyFrame(duration,new KeyValue(label.textFillProperty(),toColor)));
    }

    public void play(){
        animation.play();
    }
}
