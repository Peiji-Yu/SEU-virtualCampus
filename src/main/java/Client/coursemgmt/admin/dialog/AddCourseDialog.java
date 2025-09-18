package Client.coursemgmt.admin.dialog;

import Client.coursemgmt.admin.CourseAdminPanel;
import Client.coursemgmt.admin.service.CourseService;
import Server.model.Response;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 从 CourseCrud 中拆分出来的“添加课程”对话框组件
 */
public class AddCourseDialog {
    public static void show(CourseAdminPanel owner) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("新增课程");
        dialog.setHeaderText("添加新课程");

        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField creditField = new TextField();
        TextField collegeField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("课程编号:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("课程名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("学分:"), 0, 2);
        grid.add(creditField, 1, 2);
        grid.add(new Label("开设学院:"), 0, 3);
        grid.add(collegeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        ButtonType addType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                String id = idField.getText() == null ? "" : idField.getText().trim();
                String name = nameField.getText() == null ? "" : nameField.getText().trim();
                if (id.isEmpty() || name.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "课程编号和课程名称为必填项", ButtonType.OK);
                    a.showAndWait();
                    return null;
                }
                try {
                    Map<String, Object> m = new HashMap<>();
                    m.put("courseId", id);
                    m.put("courseName", name);
                    String creditText = creditField.getText();
                    if (creditText != null && !creditText.trim().isEmpty()) {
                        double c = Double.parseDouble(creditText.trim());
                        m.put("courseCredit", c);
                        m.put("credit", c);
                    } else {
                        m.put("courseCredit", null);
                        m.put("credit", null);
                    }
                    String school = collegeField.getText() == null ? "" : collegeField.getText().trim();
                    m.put("college", school);
                    m.put("school", school);
                    return m;
                } catch (NumberFormatException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "学分必须为数字", ButtonType.OK);
                    a.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<Map<String, Object>> res = dialog.showAndWait();
        res.ifPresent(data -> new Thread(() -> {
            try {
                Response rr = CourseService.addCourse(data);
                Platform.runLater(() -> {
                    if (rr.getCode() == 200) {
                        Alert a = new Alert(Alert.AlertType.INFORMATION, "新增课程成功", ButtonType.OK);
                        a.showAndWait();
                        owner.loadCourseData();
                    } else {
                        Alert a = new Alert(Alert.AlertType.ERROR, "新增课程失败: " + rr.getMessage(), ButtonType.OK);
                        a.showAndWait();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "网络异常: " + ex.getMessage(), ButtonType.OK);
                    a.showAndWait();
                });
            }
        }).start());
    }
}

