package Client.login.component;

import Client.login.util.ColorTransition;
import Client.login.util.DragHandler;
import Client.login.util.Resources;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import javafx.stage.Stage; // 仅关闭当前窗口所需

public class Header extends BorderPane {
    public Header(int h,int w){
        setPrefSize(w,h); setMinSize(w,h); setMaxSize(w,h);
        Label close = new Label();
        close.setFont(Resources.ICON_FONT_MIN);
        close.setText("\ue900");
        close.setStyle("-fx-text-fill:#CDCDCD");
        close.setPrefSize(24,h);
        // 原为 Platform.exit()，会关闭整个应用；改为仅关闭当前窗口
        close.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            var scene = getScene();
            if (scene != null && scene.getWindow() != null) {
                if (scene.getWindow() instanceof Stage) {
                    ((Stage) scene.getWindow()).close();
                } else {
                    scene.getWindow().hide();
                }
            }
        });
        close.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { new ColorTransition(close, Duration.seconds(0.2), Paint.valueOf("#3d3d3d")).play(); setCursor(Cursor.HAND);} );
        close.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, e -> { new ColorTransition(close, Duration.seconds(0.2), Paint.valueOf("#cdcdcd")).play(); setCursor(Cursor.DEFAULT);} );
        setRight(close);
        setPadding(new Insets(2,6,0,0));
    }
    public void setTransparent(boolean t){ if(t){ setStyle("-fx-background-color:transparent"); } }
    public void setDragHandler(DragHandler d){ setOnMousePressed(d); setOnMouseDragged(d); }
}
