package Client.login.component;

import Client.login.util.InputAnimation;
import Client.login.util.Resources;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class PasswordInput extends UsernameInput {
    private boolean visible = false; private final TextField plainField = new TextField(); private final PasswordField pwdField; private String cache;
    public PasswordInput(String placeholder){
        super(placeholder, new PasswordField());
        rectangle = new Rectangle(75,18); rectangle.setFill(Resources.WHITE); rectangle.setTranslateY(-10);
        this.getIcon().setText("\ue902");
        pwdField = (PasswordField) this.textField;
        plainField.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:14px;-fx-text-fill:#5BA3E7");
        plainField.setFont(Resources.ROBOTO_REGULAR);
        plainField.setPrefSize(220,28);
        plainField.setFocusTraversable(false);
        plainField.setTranslateX(-10);
        this.textField.textProperty().addListener((o,ov,nv)-> cache = nv);
        plainField.textProperty().addListener((o,ov,nv)-> cache = nv);
        this.getIcon().setOnMouseClicked(e -> {
            isChanging = true;
            if(!visible){
                this.getIcon().setText("\ue901");
                int pos = textField.getCaretPosition();
                plainField.setText(cache);
                getPane().getChildren().set(1, plainField);
                plainField.requestFocus();
                plainField.positionCaret(pos);
                plainField.focusedProperty().addListener((o,ov,nv)-> {
                    if(nv){ InputAnimation.forward(this, Duration.seconds(0.2)); }
                    else { if(!isChanging){ InputAnimation.reverse(this, Duration.seconds(0.2)); } }
                });
                visible = true; isChanging = false;
            } else {
                this.getIcon().setText("\ue902");
                int pos = plainField.getCaretPosition();
                textField.setText(cache);
                getPane().getChildren().set(1, textField);
                textField.requestFocus();
                textField.positionCaret(pos);
                visible = false; isChanging = false;
            }
        });
    }
    public String getPassword(){ return cache == null ? "" : cache; }
    public void setOnAction(Runnable r){ this.textField.setOnAction(e-> r.run()); plainField.setOnAction(e-> r.run()); }
}

