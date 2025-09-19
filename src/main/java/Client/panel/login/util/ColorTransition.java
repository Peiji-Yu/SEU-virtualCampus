package Client.panel.login.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Labeled;
import javafx.scene.control.Label;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

public class ColorTransition {
    private final Timeline animation;
    public ColorTransition(Labeled label, Duration duration, Paint toColor){
        animation = new Timeline(new KeyFrame(duration,new KeyValue(label.textFillProperty(),toColor)));
    }
    // 兼容某些编译环境直接传入 Label 的重载
    public ColorTransition(Label label, Duration duration, Paint toColor){
        this((Labeled)label,duration,toColor);
    }
    // 静态工厂（可在出错时改用）
    public static ColorTransition of(Label label, Duration duration, Paint toColor){
        return new ColorTransition(label,duration,toColor);
    }
    public void play(){ animation.play(); }
}

