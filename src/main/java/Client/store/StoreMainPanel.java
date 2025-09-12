package Client.store;

import Client.store.admin.AdminManageOrderPanel;
import Client.store.admin.AdminManageProductPanel;
import Client.store.student.CustomerOrderPanel;
import Client.store.student.CustomerProductPanel;
import  Client.store.admin.AdminAddProductPanel;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class StoreMainPanel extends BorderPane {
    private final String cardNumber;
    private final boolean isAdmin;

    // 各个功能面板
    private CustomerProductPanel customerProductPanel;
    private CustomerOrderPanel customerOrderPanel;
    private AdminAddProductPanel adminAddProductPanel;
    private AdminManageProductPanel adminManageProductPanel;
    private AdminManageOrderPanel adminManageOrderPanel;

    private Button currentSelectedButton;

    public StoreMainPanel(String cardNumber, String userType) {
        this.cardNumber = cardNumber;
        this.isAdmin = "admin".equalsIgnoreCase(userType);
        initializeUI();
    }

    private void initializeUI() {
        // 左侧导航栏
        VBox leftBar = new VBox(10);
        leftBar.setPadding(new Insets(15));
        leftBar.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 12;");
        leftBar.setPrefWidth(150);

        if (isAdmin) {
            // 管理员功能按钮
            Button addProductBtn = createNavButton("添加商品");
            addProductBtn.setOnAction(e -> switchToPanel("addProduct"));

            Button manageProductBtn = createNavButton("管理商品");
            manageProductBtn.setOnAction(e -> switchToPanel("manageProduct"));

            Button manageOrderBtn = createNavButton("管理订单");
            manageOrderBtn.setOnAction(e -> switchToPanel("manageOrder"));

            leftBar.getChildren().addAll(addProductBtn, manageProductBtn, manageOrderBtn);
            setLeft(leftBar);

            // 默认显示添加商品面板
            switchToPanel("addProduct");
        } else {
            // 用户功能按钮
            Button productBtn = createNavButton("商品浏览");
            productBtn.setOnAction(e -> switchToPanel("product"));

            Button orderBtn = createNavButton("我的订单");
            orderBtn.setOnAction(e -> switchToPanel("order"));

            leftBar.getChildren().addAll(productBtn, orderBtn);
            setLeft(leftBar);

            // 默认显示商品浏览面板
            switchToPanel("product");
        }

        // 顶部标题
        Label titleLabel = new Label("校园超市系统" + (isAdmin ? " (管理员)" : ""));
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-padding: 15;");
        setTop(titleLabel);
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(120);
        btn.setPrefHeight(40);
        resetButtonStyle(btn);
        return btn;
    }

    private void switchToPanel(String panelType) {
        if (currentSelectedButton != null) {
            resetButtonStyle(currentSelectedButton);
        }

        switch (panelType) {
            case "product":
                if (customerProductPanel == null) {
                    customerProductPanel = new CustomerProductPanel(cardNumber);
                }
                setCenter(customerProductPanel);
                break;
            case "order":
                if (customerOrderPanel == null) {
                    customerOrderPanel = new CustomerOrderPanel(cardNumber);
                }
                setCenter(customerOrderPanel);
                break;
            case "addProduct":
                if (adminAddProductPanel == null) {
                    adminAddProductPanel = new AdminAddProductPanel();
                }
                setCenter(adminAddProductPanel);
                break;
            case "manageProduct":
                if (adminManageProductPanel == null) {
                    adminManageProductPanel = new AdminManageProductPanel();
                }
                setCenter(adminManageProductPanel);
                break;
            case "manageOrder":
                if (adminManageOrderPanel == null) {
                    adminManageOrderPanel = new AdminManageOrderPanel();
                }
                setCenter(adminManageOrderPanel);
                break;
        }
    }

    private void setSelectedButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-border-color: #4e8cff; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-background-color: #f8fbff; -fx-text-fill: #4e8cff; " +
                "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8, 0, 0, 2);");
    }

    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 14px; -fx-background-radius: 10; " +
                "-fx-background-color: white; -fx-text-fill: #2a4d7b; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");
    }
}