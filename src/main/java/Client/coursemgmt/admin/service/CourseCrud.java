package Client.coursemgmt.admin.service;

import Client.coursemgmt.admin.CourseAdminPanel;
import Client.coursemgmt.admin.dialog.AddCourseDialog;
import Server.model.Response;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 课程的增删改查对话框与交互逻辑封装
 */
public class CourseCrud {
    public static void showAddCourseDialog(CourseAdminPanel owner) {
        // 委托给拆分后的 AddCourseDialog
        AddCourseDialog.show(owner);
    }

    public static void showEditCourseDialog(CourseAdminPanel owner, Map<String, Object> course) {
        // 复用 AddCourseDialog 的编辑接口以保持统一样式与逻辑
        AddCourseDialog.showEdit(owner, course);
    }

    public static void deleteCourseConfirmed(CourseAdminPanel owner, Map<String, Object> course) {
        if (course == null) return;
        String courseId = String.valueOf(course.get("courseId"));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定删除课程 " + courseId + " 吗？这会同时删除该课程下的教学班。", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    Response rr = CourseService.deleteCourse(courseId);
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "删除课程成功", ButtonType.OK);
                            a.showAndWait();
                            owner.loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "删除课程失败: " + rr.getMessage(), ButtonType.OK);
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
