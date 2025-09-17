package Client.finance;

import com.google.gson.Gson;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

/**
 * 一卡通交易管理主面板：采用类似选课界面的左右布局结构
 * 左侧导航栏：一卡通信息、交易记录
 * 右侧内容区：显示对应的功能面板
 */
public class FinancePanel extends BorderPane {
    private final String selfCardNumber;
    private final boolean admin;
    private CardInfoPanel cardInfoPanel;
    private TransactionPanel transactionPanel;
    private LostCardAdminPanel lostCardPanel;
    private Button currentSelectedButton;

    public FinancePanel(String cardNumber, String userType) {
        this.selfCardNumber = cardNumber;
        this.admin = "admin".equals(userType);
        initializeUI();
    }

    private void initializeUI() {
        // 左侧导航栏
        VBox leftBar = new VBox();
        leftBar.setStyle("-fx-background-color: #f4f4f4;"
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.2), 10, 0, 1, 0);");
        leftBar.setPrefWidth(210);

        // 设置说明标签
        Label leftLabel = new Label("一卡通管理");
        leftLabel.setStyle("-fx-text-fill: #303030; -fx-font-family: 'Microsoft YaHei UI'; " +
                "-fx-font-size: 12px; -fx-alignment: center-left; -fx-padding: 10 0 10 15;");

        // 添加分割线
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator.setMaxWidth(Double.MAX_VALUE);

        // 一卡通信息按钮
        Button cardInfoButton = new Button("一卡通信息");
        cardInfoButton.setPrefWidth(210);
        cardInfoButton.setPrefHeight(56);
        setSelectedButtonStyle(cardInfoButton);
        currentSelectedButton = cardInfoButton;

        cardInfoButton.setOnAction(e -> {
            if (currentSelectedButton != cardInfoButton) {
                resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(cardInfoButton);
                currentSelectedButton = cardInfoButton;

                if (cardInfoPanel == null) {
                    cardInfoPanel = new CardInfoPanel(selfCardNumber, admin);
                }
                cardInfoPanel.refreshData();
                setCenter(cardInfoPanel);
            }
        });

        // 添加分割线
        Region separator1 = new Region();
        separator1.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator1.setMaxWidth(Double.MAX_VALUE);

        // 交易记录按钮
        Button transactionButton = new Button("交易记录");
        transactionButton.setPrefWidth(210);
        transactionButton.setPrefHeight(56);
        resetButtonStyle(transactionButton);

        transactionButton.setOnAction(e -> {
            if (currentSelectedButton != transactionButton) {
                resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(transactionButton);
                currentSelectedButton = transactionButton;

                if (transactionPanel == null) {
                    transactionPanel = new TransactionPanel(selfCardNumber, admin);
                }
                transactionPanel.refreshData();
                setCenter(transactionPanel);
            }
        });

        // 添加分割线
        Region separator2 = new Region();
        separator2.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator2.setMaxWidth(Double.MAX_VALUE);

        // 挂失管理按钮
        Button lostCardButton = new Button("挂失管理");
        lostCardButton.setPrefWidth(210);
        lostCardButton.setPrefHeight(56);
        resetButtonStyle(lostCardButton);

        lostCardButton.setOnAction(e -> {
            if (currentSelectedButton != lostCardButton) {
                resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(lostCardButton);
                currentSelectedButton = lostCardButton;

                if (lostCardPanel == null) {
                    lostCardPanel = new LostCardAdminPanel();
                }
                lostCardPanel.refreshData();
                setCenter(lostCardPanel);
            }
        });

        // 添加分割线
        Region separator3 = new Region();
        separator3.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator3.setMaxWidth(Double.MAX_VALUE);

        // NOTE: 不要在此处将组件添加到 leftBar，稍后会在创建所有按钮后一次性添加，避免重复添加导致异常
        leftBar.getChildren().addAll(leftLabel, separator,
                cardInfoButton, separator1,
                transactionButton, separator2, lostCardButton, separator3);
        setLeft(leftBar);

        // 初始化默认面板
        cardInfoPanel = new CardInfoPanel(selfCardNumber, admin);
        setCenter(cardInfoPanel);
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