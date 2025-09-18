package Client.coursemgmt.admin.dialog;

import Client.coursemgmt.admin.CourseAdminPanel;
import Client.coursemgmt.admin.service.CourseService;
import Server.model.Response;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 美化后的添加课程对话框组件
 */
public class AddCourseDialog {
    public static void show(CourseAdminPanel owner) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.setTitle("新增课程");
        dialog.setHeaderText("请填写课程信息");
        dialog.getDialogPane().getStylesheets().add("/styles/dialog.css");

        // 创建表单字段
        TextField idField = createTextField("请输入课程编号");
        TextField nameField = createTextField("请输入课程名称");
        TextField creditField = createTextField("请输入学分（数字）");
        TextField collegeField = createTextField("请输入开设学院");

        // 创建带星号的必填标签
        Label idLabel = new Label("课程编号:");
        Label nameLabel = new Label("课程名称:");
        Label creditLabel = new Label("学分:");
        Label collegeLabel = new Label("开设学院:");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 25, 15, 25));
        grid.setAlignment(Pos.CENTER);

        // 添加标签和输入框
        grid.add(idLabel, 0, 0);
        grid.add(idField, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(creditLabel, 0, 2);
        grid.add(creditField, 1, 2);
        grid.add(collegeLabel, 0, 3);
        grid.add(collegeField, 1, 3);

        // 设置列约束
        GridPane.setHgrow(idField, Priority.ALWAYS);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(creditField, Priority.ALWAYS);
        GridPane.setHgrow(collegeField, Priority.ALWAYS);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(grid);

        dialog.getDialogPane().setContent(content);

        ButtonType addType = new ButtonType("添加", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        // 设置结果转换器
        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                String id = idField.getText() == null ? "" : idField.getText().trim();
                String name = nameField.getText() == null ? "" : nameField.getText().trim();

                // 验证必填字段
                if (id.isEmpty() || name.isEmpty()) {
                    showErrorAlert("验证失败", "课程编号和课程名称为必填项");
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
                    showErrorAlert("输入错误", "学分必须为有效的数字");
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
                        showSuccessAlert("操作成功", "新增课程成功");
                        owner.loadCourseData();
                    } else {
                        showErrorAlert("操作失败", "新增课程失败: " + rr.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showErrorAlert("网络异常", "网络连接异常: " + ex.getMessage());
                });
            }
        }).start());
    }

    private static TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(250);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}