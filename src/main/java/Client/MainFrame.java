package Client;

import Client.login.LogoutHandler;
import Client.studentmgmt.admin.StudentAdminPanel;
import Client.studentmgmt.student.StudentSelfPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * 应用主界面：左侧功能导航 + 右侧内容区（当前只包含学籍管理模块）。
 * 根据 userType(student/admin/teacher) 动态加载不同面板。
 * 注意：仅调整结构与注释，不修改原有UI与行为。
 * @author Msgo-srAm
 */
public class MainFrame {
    // -------------------- 常量样式定义 --------------------
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String DANGER_COLOR = "#ff6b6b";
    private static final String DANGER_HOVER_COLOR = "#ff5252";
    private static final String BACKGROUND_COLOR = "#f8fbff";
    private static final String SIDEBAR_COLOR = "#e8f2ff";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final String SECONDARY_TEXT_COLOR = "#666666";

    // -------------------- 实例字段 --------------------
    private Stage stage;
    private final String cardNumber;

    // 用户类型 student / teacher / admin / unknown
    private final String userType;

    // 当前选中导航按钮（用于复位样式）
    private Button currentSelectedButton;

    public MainFrame(String cardNumber) {
        this.cardNumber = cardNumber;
        this.userType = getUserType(cardNumber);
    }

    /**
     * 显示主界面窗口。
     */
    public void show() {
        stage = new Stage();
        stage.setTitle("智慧校园主界面");
        // 调整窗口尺寸以适应更多列
        stage.setMinWidth(1300);
        stage.setMinHeight(750);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 左侧导航栏
        VBox leftBar = new VBox(18);
        leftBar.setPadding(new Insets(15));
        leftBar.setAlignment(Pos.TOP_CENTER);
        leftBar.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Button stuManageBtn = new Button("学籍管理");
        stuManageBtn.setPrefWidth(130);
        stuManageBtn.setPrefHeight(45);
        setSelectedButtonStyle(stuManageBtn);

        Button logoutBtn = buildLogoutButton();

        // 右侧内容初始载入
        if ("student".equals(userType)) {
            root.setCenter(new StudentSelfPanel(cardNumber));
        } else if ("admin".equals(userType)) {
            root.setCenter(new StudentAdminPanel());
        }

        // 学生与管理员显示“学籍管理”按钮
        if ("student".equals(userType) || "admin".equals(userType)) {
            leftBar.getChildren().add(stuManageBtn);

            // 初次默认选中
            currentSelectedButton = stuManageBtn;

            stuManageBtn.setOnAction(e -> {
                if (currentSelectedButton == stuManageBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton);
                }
                setSelectedButtonStyle(stuManageBtn);
                currentSelectedButton = stuManageBtn;
                if ("student".equals(userType)) {
                    root.setCenter(new StudentSelfPanel(cardNumber));
                } else {
                    root.setCenter(new StudentAdminPanel());
                }
            });
        } else {
            // 教师暂无功能，显示提示
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

        // 底部退出按钮
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        leftBar.getChildren().addAll(spacer, logoutBtn);
        root.setLeft(leftBar);

        Scene scene = new Scene(root, 1500, 780);
        stage.setScene(scene);
        stage.show();
    }

    // -------------------- 构建控件 / 样式方法 --------------------
    private Button buildLogoutButton() {
        Button logoutBtn = new Button("退出登录");
        logoutBtn.setPrefWidth(130);
        logoutBtn.setPrefHeight(45);
        setDangerButtonStyle(logoutBtn);
        logoutBtn.setOnMouseEntered(e -> setDangerButtonHoverStyle(logoutBtn));
        logoutBtn.setOnMouseExited(e -> setDangerButtonStyle(logoutBtn));
        logoutBtn.setOnAction(e -> LogoutHandler.handleLogout(stage));
        return logoutBtn;
    }

    private void setSelectedButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-border-color: " + PRIMARY_COLOR + "; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-background-color: " + BACKGROUND_COLOR + "; -fx-text-fill: " + PRIMARY_COLOR + "; " +
                "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8, 0, 0, 2);");
    }

    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-background-radius: 10; " +
                "-fx-background-color: white; -fx-text-fill: " + TEXT_COLOR + "; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");
    }

    private void setDangerButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,107,107,0.3), 8, 0, 0, 2);");
    }

    private void setDangerButtonHoverStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-background-color: " + DANGER_HOVER_COLOR + "; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,82,82,0.4), 10, 0, 0, 3);");
    }

    // -------------------- 工具方法 --------------------
    private String getUserType(String cardNumber) {
        if (cardNumber.length() == 9 && cardNumber.startsWith("2")) {
            return "student";
        }
        if (cardNumber.length() == 9 && cardNumber.startsWith("1")) {
            return "teacher";
        }
        try {
            int num = Integer.parseInt(cardNumber);
            if (num <= 1000) {
                return "admin";
            }
        } catch (Exception e) {
            // 忽略异常，返回未知类型
        }
        return "unknown";
    }
}
