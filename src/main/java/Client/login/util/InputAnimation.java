package Client.login.util;

import Client.login.component.UsernameInput;
import javafx.animation.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class InputAnimation {
    public static void reverse(UsernameInput ui, Duration d){
        if(ui.getPane().getChildren().get(1) instanceof TextField tf){
            String txt = tf.getText();
            if(txt == null || txt.isEmpty()){
                StrokeTransition st = new StrokeTransition(d, ui.getBackgroundShape(), Resources.SECONDARY, Resources.DISABLED); st.play();
                TranslateTransition tt = new TranslateTransition(d, ui.getPlaceHolder()); tt.setByY(14); tt.play();
                ScaleTransition sc = new ScaleTransition(d, ui.getPlaceHolder()); sc.setToY(1); sc.setToX(1); sc.play();
                new Timeline(new KeyFrame(d,new KeyValue(ui.getPlaceHolder().textFillProperty(),Resources.DISABLED))).play();
                new Timeline(new KeyFrame(d,new KeyValue(ui.getIcon().textFillProperty(),Resources.DISABLED))).play();
                ScaleTransition sc2 = new ScaleTransition(d, ui.getRectangle()); sc2.setFromX(1); sc2.setToX(0); sc2.play();
            } else {
                StrokeTransition st = new StrokeTransition(d, ui.getBackgroundShape(), Resources.SECONDARY, Resources.DISABLED); st.play();
                new Timeline(new KeyFrame(d,new KeyValue(ui.getPlaceHolder().textFillProperty(),Resources.DISABLED))).play();
                new Timeline(new KeyFrame(d,new KeyValue(ui.getIcon().textFillProperty(),Resources.DISABLED))).play();
                Label mirror = new Label(tf.getText()); mirror.setStyle("-fx-font-size:14px;-fx-text-fill:#B4C0C7;-fx-background-color:white"); mirror.setFont(Resources.ROBOTO_REGULAR); mirror.setPrefHeight(15);
                ui.getChildren().add(mirror); AnchorPane.setLeftAnchor(mirror,5.0); AnchorPane.setTopAnchor(mirror,6.0);
                Timeline fade = new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(mirror.textFillProperty(),Resources.SECONDARY)),new KeyFrame(Duration.seconds(0.2),new KeyValue(mirror.textFillProperty(),Resources.DISABLED)));
                fade.play(); tf.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:14px;-fx-text-fill:#B4C0C7");
                fade.setOnFinished(e-> ui.getChildren().remove(mirror));
            }
        }
    }
    public static void forward(UsernameInput ui, Duration d){
        StrokeTransition st = new StrokeTransition(d, ui.getBackgroundShape(), Resources.DISABLED, Resources.SECONDARY); st.play();
        ScaleTransition scLine = new ScaleTransition(d, ui.getRectangle()); scLine.setFromX(0); scLine.setToX(1);
        if(ui.getPane().getChildren().get(1) instanceof TextField tf){
            if(tf.getText().isEmpty()){
                TranslateTransition tt = new TranslateTransition(d, ui.getPlaceHolder()); tt.setByY(-14); tt.play(); scLine.play();
            } else {
                Label temp = new Label(); temp.setStyle("-fx-font-size:14px;-fx-text-fill:#B4C0C7;-fx-background-color:white"); temp.setFont(Resources.ROBOTO_REGULAR); temp.setPrefHeight(15);
                ui.getChildren().add(temp); AnchorPane.setLeftAnchor(temp,5.0); AnchorPane.setTopAnchor(temp,6.0);
                Timeline color = new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(temp.textFillProperty(),Resources.DISABLED)),new KeyFrame(Duration.seconds(0.2),new KeyValue(temp.textFillProperty(),Resources.SECONDARY)));
                color.play(); tf.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:14px;-fx-text-fill:#5BA3E7");
                color.setOnFinished(e-> ui.getChildren().remove(temp));
            }
        }
        ScaleTransition scale = new ScaleTransition(d, ui.getPlaceHolder()); scale.setToY(0.9); scale.setToX(0.9); scale.play();
        new Timeline(new KeyFrame(d,new KeyValue(ui.getPlaceHolder().textFillProperty(),Resources.SECONDARY))).play();
        new Timeline(new KeyFrame(d,new KeyValue(ui.getIcon().textFillProperty(),Resources.SECONDARY))).play();
        if(ui.isFirst()){
            ui.getChildren().set(2, ui.getRectangle()); AnchorPane.setLeftAnchor(ui.getRectangle(),5.0); ui.getChildren().add(ui.getPane()); ui.setFirst(false); scLine.play();
        }
    }
}

