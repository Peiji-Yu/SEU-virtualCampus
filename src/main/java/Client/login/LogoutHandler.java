package Client.login;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import java.util.Optional;

/**
 * 退出登录处理工具（迁移至 login 子包）。
 * 职责：
 * 1. 确认对话框 -> 关闭主界面 -> 回到登录界面。
 * 2. 隔离退出流程，便于后续扩展清理/日志等。
 * 线程模型：阻塞式 showAndWait，桌面应用场景可接受。
 * 扩展点：异步注销、会话失效通知、操作审计。
 * 作者：@Msgo-srAm
 */
public class LogoutHandler {
    public static boolean handleLogout(Stage currentStage) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("退出确认");
        confirmAlert.setHeaderText("退出登录");
        confirmAlert.setContentText("您确定要退出当前用户并返回登录界面吗？");
        ButtonType confirmButton = new ButtonType("确认退出");
        ButtonType cancelButton = new ButtonType("取消");
        confirmAlert.getButtonTypes().setAll(confirmButton, cancelButton);
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            return performLogout(currentStage);
        }
        return false;
    }
    private static boolean performLogout(Stage currentStage) {
        try {
            if (currentStage != null) {currentStage.close();}
            LoginClientFX loginClient = new LoginClientFX();
            Stage loginStage = new Stage();
            loginClient.start(loginStage);
            return true;
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("退出失败");
            errorAlert.setHeaderText("退出登录时发生错误");
            errorAlert.setContentText("无法返回登录界面，请重新启动应用程序。\n错误信息：" + e.getMessage());
            errorAlert.showAndWait();
            return false;
        }
    }
}

