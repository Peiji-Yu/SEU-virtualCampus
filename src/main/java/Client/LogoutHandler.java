package Client;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * 退出登录处理类
 * 负责处理用户退出登录的相关逻辑
 */
public class LogoutHandler {

    /**
     * 处理用户退出登录
     * @param currentStage 当前主界面窗口
     * @return true表示用户确认退出，false表示用户取消退出
     */
    public static boolean handleLogout(Stage currentStage) {
        // 创建确认退出对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("退出确认");
        confirmAlert.setHeaderText("退出登录");
        confirmAlert.setContentText("您确定要退出当前用户并返回登录界面吗？");

        // 自定义按钮文本
        ButtonType confirmButton = new ButtonType("确认退出");
        ButtonType cancelButton = new ButtonType("取消");
        confirmAlert.getButtonTypes().setAll(confirmButton, cancelButton);

        // 显示对话框并等待用户选择
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == confirmButton) {
            // 用户确认退出
            return performLogout(currentStage);
        }

        // 用户取消退出
        return false;
    }

    /**
     * 执行退出登录操作
     * @param currentStage 当前主界面窗口
     * @return true表示成功退出
     */
    private static boolean performLogout(Stage currentStage) {
        try {
            // 关闭当前主界面窗口
            if (currentStage != null) {
                currentStage.close();
            }

            // 返回登录界面
            LoginClientFX loginClient = new LoginClientFX();
            Stage loginStage = new Stage();
            loginClient.start(loginStage);

            return true;
        } catch (Exception e) {
            // 处理异常，显示错误信息
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("退出失败");
            errorAlert.setHeaderText("退出登录时发生错误");
            errorAlert.setContentText("无法返回登录界面，请重新启动应用程序。\n错误信息：" + e.getMessage());
            errorAlert.showAndWait();

            return false;
        }
    }

    /**
     * 显示退出成功信息（可选使用）
     */
    public static void showLogoutSuccessMessage() {
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("退出成功");
        infoAlert.setHeaderText(null);
        infoAlert.setContentText("已成功退出登录，正在返回登录界面...");
        infoAlert.show();
    }
}
