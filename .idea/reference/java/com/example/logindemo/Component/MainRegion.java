package com.example.logindemo.Component;

import com.example.logindemo.Util.ColorTransition;
import com.example.logindemo.Util.Resources;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MainRegion extends AnchorPane {

    private LoginButton loginButton;
    public MainRegion() {
        Label title = new Label("W e l c o m e");
        title.setFont(Resources.ROBOTO_BOLD);
        title.setTextFill(Resources.FONT_COLOR);
        Label subTitle1 = new Label("Or ");
        Label subTitle2 = new Label("Sign up");
        subTitle1.setFont(Resources.ROBOTO_REGULAR);
        subTitle1.setStyle("-fx-font-size: 12px;");
        subTitle1.setTextFill(Resources.DISABLED);
        subTitle2.setFont(Resources.ROBOTO_REGULAR);
        subTitle2.setStyle("-fx-font-size: 12px;");
        subTitle2.setTextFill(Resources.PRIMARY);
        subTitle2.setOnMouseEntered(event -> {
            ColorTransition colorTransition = new ColorTransition(subTitle2, Duration.seconds(0.2),Resources.SECONDARY);
            colorTransition.play();
            subTitle2.setCursor(Cursor.HAND);
        });
        subTitle2.setOnMouseExited(event -> {
            ColorTransition colorTransition = new ColorTransition(subTitle2, Duration.seconds(0.2),Resources.PRIMARY);
            colorTransition.play();
            subTitle2.setCursor(Cursor.DEFAULT);
        });
        HBox hBox = new HBox(subTitle1,subTitle2);
        VBox top = new VBox(title,hBox);
        this.getChildren().add(top);
        UsernameInput usernameInput = new UsernameInput("Username",new TextField());
        PasswordInput passwordInput = new PasswordInput("Password");
        VBox input = new VBox(usernameInput,passwordInput);
        input.setSpacing(23);
        Label forgetPassword = new Label("                                                     Forget Password?");
        forgetPassword.setTextFill(Resources.DISABLED);
        forgetPassword.setFont(Resources.ROBOTO_LIGHT);
        forgetPassword.setOnMouseEntered(event -> {
            ColorTransition colorTransition = new ColorTransition(forgetPassword, Duration.seconds(0.2),Resources.SECONDARY);
            colorTransition.play();
            forgetPassword.setCursor(Cursor.HAND);
        });
        forgetPassword.setOnMouseExited(event -> {
            ColorTransition colorTransition = new ColorTransition(forgetPassword, Duration.seconds(0.2),Resources.DISABLED);
            colorTransition.play();
            forgetPassword.setCursor(Cursor.DEFAULT);
        });
        VBox inputArea = new VBox(input,forgetPassword);
        inputArea.setSpacing(5);
        VBox vBox2 = new VBox(top,inputArea);
        vBox2.setSpacing(28);
        loginButton = new LoginButton();
        VBox main = new VBox(vBox2,loginButton);
        main.setSpacing(34);
        this.getChildren().addAll(main);
    }

    public LoginButton getLoginButton() {
        return loginButton;
    }
}
