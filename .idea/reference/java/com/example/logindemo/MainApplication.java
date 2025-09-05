package com.example.logindemo;

import com.example.logindemo.Component.Header;
import com.example.logindemo.Component.MainRegion;
import com.example.logindemo.Util.DragHandler;
import com.example.logindemo.Util.FadeAnimation;
import com.example.logindemo.Util.Resources;
import javafx.animation.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        boolean debug = false;
        if(!debug){
            stage.initStyle(StageStyle.TRANSPARENT);
            AnchorPane root = new AnchorPane();
            root.setStyle("-fx-background-radius: 10px;-fx-background-color: #fff;");
            Scene scene = new Scene(root,375,575);
            scene.setFill(Paint.valueOf("#FFFFFF00"));
            DragHandler dragHandler = new DragHandler(stage);
            Header header = new Header(24,375);
            header.setDragHandler(dragHandler);
            header.setTransparent(true);
            ImageView backgroundItem = new ImageView(getClass().getResource("/Image/BGI.png").toExternalForm());
            ImageView logo = new ImageView(getClass().getResource("/Image/Logo.png").toExternalForm());
            MainRegion mainRegion = new MainRegion();
            root.getChildren().add(logo);
            AnchorPane.setTopAnchor(logo,55.0);
            AnchorPane.setLeftAnchor(logo,156.0);
            root.getChildren().add(backgroundItem);
            Rectangle focusRequester = new Rectangle(375,575, Color.TRANSPARENT);
            ProgressBar progressBar = new ProgressBar();
            progressBar.getStylesheets().add(getClass().getResource("/Css/ProgressBar.css").toExternalForm());
            root.getChildren().add(focusRequester);
            focusRequester.addEventHandler(MouseEvent.MOUSE_CLICKED,event -> {
                focusRequester.requestFocus();
            });
            root.getChildren().add(header);
            root.getChildren().add(mainRegion);
            AnchorPane.setLeftAnchor(mainRegion,63.0);
            AnchorPane.setTopAnchor(mainRegion,161.0);
            mainRegion.getLoginButton().setOnMouseClicked(event -> {
                FadeAnimation.fadeOut(Duration.seconds(0.2),mainRegion);
                root.getChildren().add(progressBar);
                progressBar.setOpacity(0.0);
                AnchorPane.setLeftAnchor(progressBar,112.0);
                AnchorPane.setTopAnchor(progressBar,291.0);
                FadeAnimation.fadeIn(Duration.seconds(0.2),progressBar,Duration.seconds(0.4));
                TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.6),logo);
                translateTransition.setByY(105);
                translateTransition.setDelay(Duration.seconds(0.2));
                translateTransition.play();
            });
            stage.setScene(scene);
            stage.show();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}