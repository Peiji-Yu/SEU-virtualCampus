package Client.store;

import Client.store.admin.AddProductPanel;
import Client.store.admin.ManageOrderPanel;
import Client.store.admin.ManageProductPanel;
import Client.store.student.MyOrderPanel;
import Client.store.student.ProductSearchPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class StoreMainPanel extends BorderPane {
    private ProductSearchPanel productSearchPanel;
    private MyOrderPanel myOrderPanel;
    private AddProductPanel addProductPanel;
    private ManageProductPanel manageProductPanel;
    private ManageOrderPanel manageOrderPanel;
    private Button currentSelectedButton;
    private final String cardNumber;
    private final boolean isAdmin;

    public StoreMainPanel(String cardNumber, String userType) {
        this.cardNumber = cardNumber;
        this.isAdmin = "admin".equalsIgnoreCase(userType);
        initializeUI();
    }

    private void initializeUI() {
        // 左侧导航栏
        VBox leftBar = new VBox();

        // 设置样式，添加向内阴影效果
        leftBar.setStyle("-fx-background-color: #f4f4f4;"
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.2), 10, 0, 1, 0);");
        leftBar.setPrefWidth(210);

        //设置说明标签
        Label leftLabel = new Label("校园超市");
        leftLabel.setStyle("-fx-text-fill: #303030; -fx-font-family: 'Microsoft YaHei UI'; " +
                "-fx-font-size: 12px; -fx-alignment: center-left; -fx-padding: 10 0 10 15;");

        // 添加分割线
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator.setMaxWidth(Double.MAX_VALUE);

        // 如果是管理员，添加管理功能按钮
        if (isAdmin) {
            Button addProductButton = new Button("添加商品");
            addProductButton.setPrefWidth(210);
            addProductButton.setPrefHeight(56);
            setSelectedButtonStyle(addProductButton);
            currentSelectedButton = addProductButton;

            addProductButton.setOnAction(e -> {
                if (currentSelectedButton != addProductButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(addProductButton);
                    currentSelectedButton = addProductButton;

                    // 初始化添加商品页面
                    if (addProductPanel == null) {
                        addProductPanel = new AddProductPanel();
                    }
                    setCenter(addProductPanel);
                }
            });

            // 添加分割线
            Region separator1 = new Region();
            separator1.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator1.setMaxWidth(Double.MAX_VALUE);

            Button manageProductButton = new Button("管理商品");
            manageProductButton.setPrefWidth(210);
            manageProductButton.setPrefHeight(56);
            resetButtonStyle(manageProductButton);

            manageProductButton.setOnAction(e -> {
                if (currentSelectedButton != manageProductButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(manageProductButton);
                    currentSelectedButton = manageProductButton;

                    // 初始化管理商品页面
                    if (manageProductPanel == null) {
                        manageProductPanel = new ManageProductPanel();
                    }
                    setCenter(manageProductPanel);
                }
            });

            // 添加分割线
            Region separator2 = new Region();
            separator2.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator2.setMaxWidth(Double.MAX_VALUE);

            // 添加管理订单按钮
            Button manageOrderButton = new Button("管理订单");
            manageOrderButton.setPrefWidth(210);
            manageOrderButton.setPrefHeight(56);
            resetButtonStyle(manageOrderButton);

            manageOrderButton.setOnAction(e -> {
                if (currentSelectedButton != manageOrderButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(manageOrderButton);
                    currentSelectedButton = manageOrderButton;

                    // 初始化管理订单页面
                    if (manageOrderPanel == null) {
                        manageOrderPanel = new ManageOrderPanel();
                    }
                    setCenter(manageOrderPanel);
                }
            });

            // 添加分割线
            Region separator3 = new Region();
            separator3.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator3.setMaxWidth(Double.MAX_VALUE);

            leftBar.getChildren().addAll(leftLabel, separator,
                    addProductButton, separator1,
                    manageProductButton, separator2,
                    manageOrderButton, separator3);
            setLeft(leftBar);

            // 初始化默认面板
            addProductPanel = new AddProductPanel();
            setCenter(addProductPanel);
        }
        else {
            // 浏览商品按钮
            Button productButton = new Button("浏览商品");
            productButton.setPrefWidth(210);
            productButton.setPrefHeight(56);
            setSelectedButtonStyle(productButton);
            currentSelectedButton = productButton;

            productButton.setOnAction(e -> {
                if (currentSelectedButton != productButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(productButton);
                    currentSelectedButton = productButton;

                    // 初始化浏览商品页面
                    if (productSearchPanel == null) {
                        productSearchPanel = new ProductSearchPanel(cardNumber);
                    }
                    setCenter(productSearchPanel);
                }
            });

            // 添加分割线
            Region separator1 = new Region();
            separator1.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator1.setMaxWidth(Double.MAX_VALUE);

            // 我的订单按钮
            Button orderButton = new Button("我的订单");
            orderButton.setPrefWidth(210);
            orderButton.setPrefHeight(56);
            resetButtonStyle(orderButton);

            orderButton.setOnAction(e -> {
                if (currentSelectedButton != orderButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(orderButton);
                    currentSelectedButton = orderButton;

                    // 初始化我的订单页面
                    if (myOrderPanel == null) {
                        myOrderPanel = new MyOrderPanel(cardNumber);
                    }
                    setCenter(myOrderPanel);
                }
            });

            // 添加分割线
            Region separator2 = new Region();
            separator2.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator2.setMaxWidth(Double.MAX_VALUE);

            leftBar.getChildren().addAll(leftLabel, separator,
                    productButton, separator1,
                    orderButton, separator2);
            setLeft(leftBar);

            // 初始化面板
            productSearchPanel = new ProductSearchPanel(cardNumber);
            setCenter(productSearchPanel);
        }
    }

    private void setSelectedButtonStyle(Button button) {
        button.setStyle("-fx-font-family: 'Microsoft YaHei UI'; -fx-font-size: 16px; " +
                "-fx-background-color: #176B3A; -fx-text-fill: white; " +
                "-fx-alignment: center-left; -fx-padding: 0 0 0 56;");
    }

    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-family: 'Microsoft YaHei UI'; -fx-font-size: 16px; " +
                "-fx-background-color: #f4f4f4; -fx-text-fill: black; " +
                "-fx-alignment: center-left;  -fx-padding: 0 0 0 60;");
    }
}