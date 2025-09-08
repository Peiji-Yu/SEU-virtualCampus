package Client.util;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * UI 工具：统一为窗口与对话框设置应用 Logo 图标。
 */
public final class UIUtil {
    private UIUtil() {}
    private static Image LOGO;

    private static Image getLogo() {
        if (LOGO == null) {
            try {
                var url = UIUtil.class.getResource("/Image/Logo.png");
                if (url != null) {
                    LOGO = new Image(url.toExternalForm());
                }
            } catch (Exception ignore) {}
        }
        return LOGO;
    }

    public static void applyLogoToStage(Stage stage) {
        if (stage == null) return;
        Image img = getLogo();
        if (img != null) {
            stage.getIcons().add(img);
        }
    }

    public static void applyLogoToAlert(Alert alert) {
        if (alert == null) return;
        try {
            Window w = alert.getDialogPane().getScene().getWindow();
            if (w instanceof Stage) {
                applyLogoToStage((Stage) w);
            }
        } catch (Exception ignore) {}
    }
}

