package Client.studentmgmt.admin;

import Client.studentmgmt.service.StudentClientService;
import Server.model.student.Student;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * 学生删除确认与执行工具类 (admin 包)。
 * 封装确认弹窗 + 异步删除。
 * 作者: @Msgo-srAm
 */
public final class StudentDeleteHelper {
    private StudentDeleteHelper() {}
    public interface DeleteCallback {void onSuccess();}
    public static void confirmAndDelete(Student student, StudentClientService service, DeleteCallback cb) {
        if (student == null) {return;}
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要删除学生 " + student.getName() + " 吗？此操作不可撤销。", ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("删除确认");
        alert.setTitle("确认删除");
        alert.showAndWait().ifPresent(type -> {if (type == ButtonType.OK) {doDelete(student, service, cb);}});
    }
    private static void doDelete(Student student, StudentClientService service, DeleteCallback cb) {
        new Thread(() -> {
            try {
                boolean ok = service.deleteStudent(student.getCardNumber());
                Platform.runLater(() -> {
                    if (ok) {showInfo("成功", "学生删除成功"); if (cb != null) {cb.onSuccess();}}
                    else {showError("失败", "学生删除失败");}
                });
            } catch (Exception e) {Platform.runLater(() -> showError("删除失败", e.getMessage()));}
        }).start();
    }
    private static void showInfo(String title, String msg) {Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);a.setHeaderText(title);a.setTitle(title);a.showAndWait();}
    private static void showError(String title, String msg) {Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);a.setHeaderText(title);a.setTitle(title);a.showAndWait();}
}

