package Client.panel.login.util;

import Client.panel.login.component.UsernameInput;
import javafx.animation.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class InputAnimation {
    private static boolean approx(double a, double b){ return Math.abs(a - b) < 0.05; }
    public static void reverse(UsernameInput ui, Duration d){
        // 幂等：若已经在初始状态则不执行
        if(approx(ui.getPlaceHolder().getTranslateY(), 0) && approx(ui.getPlaceHolder().getScaleX(), 1) && approx(ui.getPlaceHolder().getScaleY(), 1)){
            return;
        }
        if(ui.getPane().getChildren().get(1) instanceof TextField tf){
            String txt = tf.getText();
            if(txt == null || txt.isEmpty()){
                StrokeTransition st = new StrokeTransition(d, ui.getBackgroundShape(), Resources.SECONDARY, Resources.DISABLED); st.play();
                TranslateTransition tt = new TranslateTransition(d, ui.getPlaceHolder()); tt.setToY(0); tt.play();
                ScaleTransition sc = new ScaleTransition(d, ui.getPlaceHolder()); sc.setToY(1); sc.setToX(1); sc.play();
                new Timeline(new KeyFrame(d,new KeyValue(ui.getPlaceHolder().textFillProperty(),Resources.DISABLED))).play();
                new Timeline(new KeyFrame(d,new KeyValue(ui.getIcon().textFillProperty(),Resources.DISABLED))).play();
                ScaleTransition sc2 = new ScaleTransition(d, ui.getRectangle()); sc2.setFromX(1); sc2.setToX(0); sc2.play();
            } else {
                StrokeTransition st = new StrokeTransition(d, ui.getBackgroundShape(), Resources.SECONDARY, Resources.DISABLED); st.play();
                new Timeline(new KeyFrame(d,new KeyValue(ui.getPlaceHolder().textFillProperty(),Resources.DISABLED))).play();
                new Timeline(new KeyFrame(d,new KeyValue(ui.getIcon().textFillProperty(),Resources.DISABLED))).play();
                Label mirror = new Label(tf.getText()); mirror.setMouseTransparent(true); mirror.setStyle("-fx-font-size:14px;-fx-text-fill:#B4C0C7;-fx-background-color:white"); mirror.setFont(Resources.ROBOTO_REGULAR); mirror.setPrefHeight(15);
                ui.getChildren().add(mirror); AnchorPane.setLeftAnchor(mirror,5.0); AnchorPane.setTopAnchor(mirror,6.0);
                Timeline fade = new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(mirror.textFillProperty(),Resources.SECONDARY)),new KeyFrame(Duration.seconds(0.2),new KeyValue(mirror.textFillProperty(),Resources.DISABLED)));
                fade.play(); tf.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:14px;-fx-text-fill:#B4C0C7");
                fade.setOnFinished(e-> ui.getChildren().remove(mirror));
            }
        }
    }
    public static void forward(UsernameInput ui, Duration d){
        // 幂等：若已在上浮状态则不执行
        if(approx(ui.getPlaceHolder().getTranslateY(), -14) && approx(ui.getPlaceHolder().getScaleX(), 0.9) && approx(ui.getPlaceHolder().getScaleY(), 0.9)){
            return;
        }
        StrokeTransition st = new StrokeTransition(d, ui.getBackgroundShape(), Resources.DISABLED, Resources.SECONDARY); st.play();
        ScaleTransition scLine = new ScaleTransition(d, ui.getRectangle()); scLine.setFromX(0); scLine.setToX(1);
        if(ui.getPane().getChildren().get(1) instanceof TextField tf){
            String txt = tf.getText();
            // 无论是否有文本，占位符都需要上浮到相同位置
            TranslateTransition tt = new TranslateTransition(d, ui.getPlaceHolder()); tt.setToY(-14); tt.play();
            if(txt == null || txt.isEmpty()){
                scLine.play();
            } else {
                Label temp = new Label(); temp.setMouseTransparent(true); temp.setStyle("-fx-font-size:14px;-fx-text-fill:#B4C0C7;-fx-background-color:white"); temp.setFont(Resources.ROBOTO_REGULAR); temp.setPrefHeight(15);
                ui.getChildren().add(temp); AnchorPane.setLeftAnchor(temp,5.0); AnchorPane.setTopAnchor(temp,6.0);
                Timeline color = new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(temp.textFillProperty(),Resources.DISABLED)),new KeyFrame(Duration.seconds(0.2),new KeyValue(temp.textFillProperty(),Resources.SECONDARY)));
                color.play(); tf.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:14px;-fx-text-fill:#429866");
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
