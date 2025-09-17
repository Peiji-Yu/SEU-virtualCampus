package Client.login.component;

import Client.login.util.InputAnimation;
import Client.login.util.Resources;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * 用户名/通用文本输入组件。
 * @author Msgo-srAm
 */
public class UsernameInput extends AnchorPane {
    protected Label placeHolder; protected Rectangle background; protected Label icon; protected Rectangle rectangle; protected Pane pane; protected TextField textField; boolean first = true; protected boolean isChanging;
    public UsernameInput(String placeholder, boolean numeric){ this(placeholder, createField(numeric)); }
    public UsernameInput(String placeholder, TextField tf){
        setStyle("-fx-background-color:transparent;");
        background = new Rectangle(250, 40); // 高度改为40
        background.setFill(Color.TRANSPARENT);
        background.setStroke(Resources.DISABLED);
        background.setStrokeWidth(1.5);
        background.setArcHeight(8);
        background.setArcWidth(8);
        getChildren().add(background);

        setMaxSize(250, 40); // 高度改为40

        icon = new Label();
        icon.setFont(Resources.ICON_FONT);
        icon.setText("\ue903");
        icon.setPrefSize(25, 37); // 高度改为37以保持居中
        icon.setTextFill(Resources.DISABLED);
        getChildren().add(icon);
        AnchorPane.setRightAnchor(icon, 8.0);

        rectangle = new Rectangle(95, 18);
        rectangle.setFill(Resources.WHITE);
        rectangle.setTranslateY(-10);

        textField = tf;
        textField.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:14px;-fx-text-fill:#5BA3E7");
        textField.setFont(Resources.ROBOTO_REGULAR);
        textField.setPrefSize(220, 38); // 高度改为38
        textField.setFocusTraversable(false);
        textField.setTranslateX(-10);

        placeHolder = new Label(placeholder);
        placeHolder.setFont(Resources.ROBOTO_REGULAR);
        placeHolder.setTextFill(Resources.DISABLED);
        placeHolder.setPrefHeight(38); // 高度改为38

        placeHolder.setMouseTransparent(true);

        pane = new Pane(placeHolder, textField);
        setPadding(new Insets(0, 0, 0, 5));
        getChildren().add(pane);
        AnchorPane.setLeftAnchor(pane, 5.0);

        textField.focusedProperty().addListener((o, ov, nv) -> {
            if(nv){
                InputAnimation.forward(this, Duration.seconds(0.2));
            } else {
                if(!isChanging){
                    InputAnimation.reverse(this, Duration.seconds(0.2));
                }
            }
        });
    }
    private static TextField createField(boolean numeric){ TextField f = new TextField(); if(numeric){ f.textProperty().addListener((o,ov,nv)-> { if(!nv.matches("\\d*")){ f.setText(nv.replaceAll("\\D","")); } }); } return f; }
    public String getText(){ return textField.getText(); }
    public void setOnAction(Runnable r){ textField.setOnAction(e-> r.run()); }
    public Label getPlaceHolder(){return placeHolder;} public Rectangle getBackgroundShape(){return background;} public Label getIcon(){return icon;} public Rectangle getRectangle(){return rectangle;} public Pane getPane(){return pane;} public boolean isFirst(){return first;} public void setFirst(boolean f){first=f;}
    public void clear(){
        textField.setText("");
        // 触发占位符回落（幂等，若已在初始位置则无动作）
        Client.login.util.InputAnimation.reverse(this, javafx.util.Duration.seconds(0.15));
    }
    // 新增：请求将焦点聚焦到内部文本框
    public void requestInnerFocus(){
        textField.requestFocus();
    }
    // 新增：设置文本并在非空时上移占位符
    public void setText(String text){
        boolean nonEmpty = text != null && !text.isEmpty();
        isChanging = true;
        textField.setText(text == null ? "" : text);
        if (nonEmpty) {
            Client.login.util.InputAnimation.forward(this, javafx.util.Duration.seconds(0.12));
        } else {
            Client.login.util.InputAnimation.reverse(this, javafx.util.Duration.seconds(0.12));
        }
        isChanging = false;
    }
    // 新增：只读/可编辑切换（不改变整体禁用样式）
    public void setEditable(boolean editable){
        textField.setEditable(editable);
        textField.setFocusTraversable(editable);
    }
}
