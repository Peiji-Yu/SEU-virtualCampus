package Client.login.component;

import Client.login.util.Resources;
import javafx.animation.FillTransition;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/** 可配置文案/宽度/颜色的实心按钮，与 LoginButton 同风格
 *  @author Msgo-srAm
 */
public class FilledButton extends Pane {
    private final Rectangle background; private final Label label;
    private final Color normal; private final Color hover; private final Color textColor;

    public FilledButton(String text, double width, Color normal, Color hover, Color textColor){
        this.normal = normal==null? Resources.PRIMARY : normal;
        this.hover = hover==null? Resources.SECONDARY : hover;
        this.textColor = textColor==null? Resources.WHITE : textColor;
        background = new Rectangle(width,40);
        background.setFill(this.normal);
        background.setArcWidth(6); background.setArcHeight(6);
        getChildren().add(background);
        label = new Label(text);
        label.setFont(Resources.ROBOTO_REGULAR);
        label.setTextFill(this.textColor);
        BorderPane bp = new BorderPane(); bp.setCenter(label); bp.setMinSize(width,40); getChildren().add(bp);
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { FillTransition ft = new FillTransition(Duration.seconds(0.2), background, this.normal, this.hover); ft.play(); setCursor(Cursor.HAND); });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> { FillTransition ft = new FillTransition(Duration.seconds(0.2), background, this.hover, this.normal); ft.play(); setCursor(Cursor.DEFAULT); });
    }
    public void setOnAction(Runnable r){
        // 使用 setOnMouseClicked 覆盖旧的点击监听，避免重复触发多个历史监听
        this.setOnMouseClicked(e -> r.run());
    }
    public void setText(String t){ label.setText(t); }
    public void setButtonWidth(double w){ background.setWidth(w); ((BorderPane)getChildren().get(1)).setMinWidth(w); }
    public void setBusy(boolean busy){ setDisable(busy); setOpacity(busy?0.85:1.0); }
}
