package Client.coursemgmt.admin.dialog;

import Client.coursemgmt.admin.CourseAdminPanel;
import Client.coursemgmt.admin.service.CourseService;
import Server.model.Response;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/**
 * 将“添加学生”对话框逻辑从 TeachingClassCard 中抽离出来，便于复用和测试。
 */
public class AddStudentDialog {
    public static void showAndHandle(CourseAdminPanel owner, Server.model.course.TeachingClass tc) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setTitle("添加学生到教学班");
        inputDialog.setHeaderText("请输入学生一卡通号");
        inputDialog.setContentText("一卡通号:");

        Optional<String> opt = inputDialog.showAndWait();
        opt.ifPresent(cardNumber -> {
            String cardNum = cardNumber == null ? "" : cardNumber.trim();
            if (cardNum.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR, "一卡通号不能为空", ButtonType.OK);
                a.showAndWait();
                return;
            }
            if (!cardNum.matches("\\d+")) {
                Alert a = new Alert(Alert.AlertType.ERROR, "一卡通号必须为纯数字", ButtonType.OK);
                a.showAndWait();
                return;
            }

            new Thread(() -> {
                try {
                    long cardLong = Long.parseLong(cardNum);
                    Response rr = CourseService.sendSelectCourse(cardLong, tc.getUuid());
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "添加成功", ButtonType.OK);
                            a.showAndWait();
                            if (owner != null) owner.loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "添加失败: " + rr.getMessage(), ButtonType.OK);
                            a.showAndWait();
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.ERROR, "网络异常: " + ex.getMessage(), ButtonType.OK);
                        a.showAndWait();
                    });
                }
            }).start();
        });
    }
}

