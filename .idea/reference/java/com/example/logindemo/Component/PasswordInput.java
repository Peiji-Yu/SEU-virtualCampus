package com.example.logindemo.Component;

import com.example.logindemo.Util.InputAnimation;
import com.example.logindemo.Util.Resources;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class PasswordInput extends UsernameInput {

    private boolean isVisible = false;
    private TextField visibleTextField = new TextField();
    private PasswordField unVisibleField;

    private String password;
    public PasswordInput(String placeholder) {
        super(placeholder,new PasswordField());
        this.icon.setText("\ue902");
        unVisibleField = (PasswordField) textField;
        visibleTextField.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;-fx-font-size: 14px;-fx-text-fill: #5BA3E7");
        visibleTextField.setFont(Resources.ROBOTO_REGULAR);
        visibleTextField.setPrefSize(220,28);
        visibleTextField.setFocusTraversable(false);
        visibleTextField.setTranslateX(-10);
        textField.textProperty().addListener(((observable, oldValue, newValue) -> {
            password = newValue;
        }));
        visibleTextField.textProperty().addListener(((observable, oldValue, newValue) -> {
            password = newValue;
        }));
        this.icon.setOnMouseClicked(event -> {
            isChanging = true;
            if(!isVisible) {
                this.icon.setText("\ue901");
                int cursorPosition = textField.getCaretPosition();
                visibleTextField.setText(password);
                pane.getChildren().set(1,visibleTextField);
                visibleTextField.requestFocus();
                visibleTextField.positionCaret(cursorPosition);
                visibleTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    if(newValue) {
                        InputAnimation.forwardAnimationPlay(this, Duration.seconds(0.2));
                    } else {
                        if(!isChanging){
                            InputAnimation.reverseAnimationPlay(this, Duration.seconds(0.2));
                        }
                    }
                });
                isVisible = true;
                isChanging = false;
            } else {
                isChanging = true;
                this.icon.setText("\ue902");
                int cursorPosition = visibleTextField.getCaretPosition();
                textField.setText(password);
                this.pane.getChildren().set(1,textField);
                textField.requestFocus();
                textField.positionCaret(cursorPosition);
                isVisible = false;
                isChanging = false;
            }
        });
    }
}
