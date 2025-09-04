package Client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.google.gson.Gson;

public class MainFrame {
    private Stage stage;
    private String cardNumber;
    private String userType; // "student", "teacher", "admin"
    private Gson gson = new Gson();
    private Button currentSelectedButton; // 跟踪当前选中的按钮

    // 统一的样式常量
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String PRIMARY_HOVER_COLOR = "#3d7bff";
    private static final String DANGER_COLOR = "#ff6b6b";
    private static final String DANGER_HOVER_COLOR = "#ff5252";
    private static final String BACKGROUND_COLOR = "#f8fbff";
    private static final String SIDEBAR_COLOR = "#e8f2ff";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final String SECONDARY_TEXT_COLOR = "#666666";

    public MainFrame(String cardNumber) {
        this.cardNumber = cardNumber;
        this.userType = getUserType(cardNumber);
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("智慧校园主界面");

        // 设置窗口最小尺寸，确保表格能完整显示
        stage.setMinWidth(1000);
        stage.setMinHeight(700);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox leftBar = new VBox(18);
        leftBar.setPadding(new Insets(15));
        leftBar.setAlignment(Pos.TOP_CENTER);
        leftBar.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Button stuManageBtn = new Button("学籍管理");
        stuManageBtn.setPrefWidth(130);
        stuManageBtn.setPrefHeight(45);
        // 设置默认选中状态的样式（蓝色边框）
        setSelectedButtonStyle(stuManageBtn);

        // 创建退出按钮
        Button logoutBtn = new Button("退出登录");
        logoutBtn.setPrefWidth(130);
        logoutBtn.setPrefHeight(45);
        setDangerButtonStyle(logoutBtn);

        // 退出按钮悬停效果
        logoutBtn.setOnMouseEntered(e -> setDangerButtonHoverStyle(logoutBtn));
        logoutBtn.setOnMouseExited(e -> setDangerButtonStyle(logoutBtn));

        // 退出按钮点击事件
        logoutBtn.setOnAction(e -> {
            LogoutHandler.handleLogout(stage);
        });

        // 右侧面板初始为学籍管理
        StudentManagementPanel stuPanel = new StudentManagementPanel(cardNumber, userType);
        root.setCenter(stuPanel);

        // 权限控制
        if ("student".equals(userType) || "admin".equals(userType)) {
            leftBar.getChildren().add(stuManageBtn);

            // 设置学籍管理按钮为默认选中状态
            currentSelectedButton = stuManageBtn;

            stuManageBtn.setOnAction(e -> {
                // 如果点击的是当前已选中的按钮，不执行任何操作
                if (currentSelectedButton == stuManageBtn) {
                    return;
                }

                // 重置之前选中按钮的样式
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton);
                }

                // 设置新选中按钮的样式
                setSelectedButtonStyle(stuManageBtn);
                currentSelectedButton = stuManageBtn;

                // 切换到学籍管理面板
                root.setCenter(new StudentManagementPanel(cardNumber, userType));
            });
        } else {
            // 教师用户显示暂无功能的提示
            VBox noFunctionBox = new VBox(20);
            noFunctionBox.setAlignment(Pos.CENTER);
            noFunctionBox.setStyle("-fx-background-color: " + BACKGROUND_COLOR + "; -fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2); -fx-padding: 40;");

            Label noFunctionTitle = new Label("欢迎使用智慧校园系统");
            noFunctionTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");

            Label noFunctionDesc = new Label("您当前的账户类型暂无可用功能模块");
            noFunctionDesc.setStyle("-fx-font-size: 16px; -fx-text-fill: " + SECONDARY_TEXT_COLOR + ";");

            noFunctionBox.getChildren().addAll(noFunctionTitle, noFunctionDesc);
            root.setCenter(noFunctionBox);
        }

        // 添加退出按钮到左侧栏（所有用户类型都显示）
        // 在左侧栏底部添加一个弹性空间，然后添加退出按钮
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        leftBar.getChildren().addAll(spacer, logoutBtn);

        root.setLeft(leftBar);

        // 增加初始窗口尺寸以更好地显示表格
        Scene scene = new Scene(root, 1100, 750);
        stage.setScene(scene);
        stage.show();
    }

    // 设置按钮选中状态的样式
    private void setSelectedButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-border-color: " + PRIMARY_COLOR + "; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-background-color: " + BACKGROUND_COLOR + "; -fx-text-fill: " + PRIMARY_COLOR + "; " +
                "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8, 0, 0, 2);");
    }

    // 重置按钮为未选中状态的样式
    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-background-radius: 10; " +
                "-fx-background-color: white; -fx-text-fill: " + TEXT_COLOR + "; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");
    }

    private String getUserType(String cardNumber) {
        if (cardNumber.length() == 9 && cardNumber.startsWith("2")) return "student";
        if (cardNumber.length() == 9 && cardNumber.startsWith("1")) return "teacher";
        try {
            int num = Integer.parseInt(cardNumber);
            if (num <= 1000) return "admin";
        } catch (Exception e) {}
        return "unknown";
    }

    // 设置危险按钮（如退出按钮）的样式
    private void setDangerButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,107,107,0.3), 8, 0, 0, 2);");
    }

    // 设置危险按钮的悬停样式
    private void setDangerButtonHoverStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-background-color: " + DANGER_HOVER_COLOR + "; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,82,82,0.4), 10, 0, 0, 3);");
    }
}
