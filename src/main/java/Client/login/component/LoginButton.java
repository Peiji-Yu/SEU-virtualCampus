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

public class LoginButton extends Pane {
    private final Rectangle background; private final Label label; private boolean busy = false;
    public LoginButton(){
        background = new Rectangle(250,34);
        background.setFill(Resources.PRIMARY);
        background.setArcWidth(6); background.setArcHeight(6);
        getChildren().add(background);
        label = new Label("Login");
        label.setFont(Resources.ROBOTO_REGULAR);
        label.setTextFill(Resources.WHITE);
        BorderPane bp = new BorderPane(); bp.setCenter(label); bp.setMinSize(250,34); getChildren().add(bp);
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { if(!busy){ FillTransition ft = new FillTransition(Duration.seconds(0.2), background, Resources.PRIMARY, Resources.SECONDARY); ft.play(); setCursor(Cursor.HAND);} });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> { if(!busy){ FillTransition ft = new FillTransition(Duration.seconds(0.2), background, Resources.SECONDARY, Resources.PRIMARY); ft.play(); setCursor(Cursor.DEFAULT);} });
    }
    public void setOnLogin(Runnable r){ this.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> { if(!busy){ r.run(); } }); }
    public void setBusy(boolean b){ this.busy = b; if(b){ label.setText("Loading..."); setOpacity(0.85); setDisable(true);} else { label.setText("Login"); setOpacity(1); setDisable(false);} }
}

