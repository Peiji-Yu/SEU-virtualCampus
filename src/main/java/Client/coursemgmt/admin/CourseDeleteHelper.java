package Client.coursemgmt.admin;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.List;
import java.util.Optional;

/**
 * 删除确认帮助类（前端演示）
 * @author GitHub Copilot
 */
public class CourseDeleteHelper {
    public static void confirmAndDelete(CourseAdminPanel.Course sel, List<CourseAdminPanel.Course> all, Runnable onDone){
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "确认删除课程：" + (sel==null?"":sel.name) + " ?");
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK){
            all.remove(sel);
            if (onDone != null) { onDone.run(); }
        }
    }
}
