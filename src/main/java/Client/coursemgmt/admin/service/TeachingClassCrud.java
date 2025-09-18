package Client.coursemgmt.admin.service;

import Client.coursemgmt.admin.CourseAdminPanel;
import Client.coursemgmt.admin.dialog.AddTeachingClassDialog;
import Server.model.Response;
import Server.model.course.TeachingClass;
import javafx.application.Platform;
import javafx.scene.control.*;

import java.util.*;

/**
 * 教学班的增删改对话框与交互逻辑封装
 */
public class TeachingClassCrud {
    public static void showAddTeachingClassDialog(CourseAdminPanel owner, String courseId) {
        // 委托到拆分后的对话框
        AddTeachingClassDialog.showForCourse(owner, courseId);
    }

    public static void showEditTeachingClassDialog(CourseAdminPanel owner, TeachingClass selected) {
        // 复用新增对话框的编辑接口，保持样式和行为一致
        AddTeachingClassDialog.showEdit(owner, selected);
    }

    public static void deleteTeachingClassConfirmed(CourseAdminPanel owner, TeachingClass tc) {
        if (tc == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定删除该教学班吗？", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    Response rr = CourseService.deleteTeachingClass(tc.getUuid());
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "删除成功", ButtonType.OK);
                            a.showAndWait();
                            owner.loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "删除失败: " + rr.getMessage(), ButtonType.OK);
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
        }
    }
}
