package Client.coursemgmt.admin;

import Server.model.Response;
import javafx.application.Platform;
import javafx.geometry.Insets;
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

    public static void showEditCourseDialog(CourseAdminPanel owner, Map<String, Object> course) {
        if (course == null) return;
        String courseId = String.valueOf(course.get("courseId"));
        String courseName = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
        Object credObj = course.get("courseCredit") != null ? course.get("courseCredit") : course.get("credit");
        Object schoolObj = course.get("college") != null ? course.get("college") : course.get("school");
        String courseCredit = credObj == null ? "" : String.valueOf(credObj);
        String college = schoolObj == null ? "" : String.valueOf(schoolObj);

        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("编辑课程");
        dialog.setHeaderText("编辑课程 " + courseId);

        TextField idField = new TextField(courseId);
        idField.setDisable(true);
        TextField nameField = new TextField(courseName);
        TextField creditField = new TextField(courseCredit);
        TextField collegeField = new TextField(college);

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
        ButtonType saveType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                String name = nameField.getText() == null ? "" : nameField.getText().trim();
                if (name.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "课程名称不能为空", ButtonType.OK);
                    a.showAndWait();
                    return null;
                }
                try {
                    Map<String, Object> m = new HashMap<>();
                    m.put("courseId", courseId);
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
                Response rr = CourseService.updateCourse(data);
                Platform.runLater(() -> {
                    if (rr.getCode() == 200) {
                        Alert a = new Alert(Alert.AlertType.INFORMATION, "更新课程成功", ButtonType.OK);
                        a.showAndWait();
                        owner.loadCourseData();
                    } else {
                        Alert a = new Alert(Alert.AlertType.ERROR, "更新课程失败: " + rr.getMessage(), ButtonType.OK);
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
