package Client.coursemgmt.admin.dialog;

import Client.coursemgmt.admin.CourseAdminPanel;
import Client.coursemgmt.admin.service.CourseService;
import Server.model.Response;
import Server.model.course.TeachingClass;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.*;

/**
 * 从 CourseAdminPanel 中拆分出来的“新增教学班”对话框
 */
public class AddTeachingClassDialog {
    public static void showForCourse(CourseAdminPanel owner, String courseId) {
        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.setTitle("新增教学班");
        dialog.setHeaderText("为课程 " + courseId + " 添加教学班");
        dialog.getDialogPane().getStylesheets().add("/styles/dialog.css");

        // 创建表单字段
        TextField teacherField = createTextField("请输入教师姓名");
        ComboBox<String> dayChoice = new ComboBox<>(FXCollections.observableArrayList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        dayChoice.setEditable(true);
        dayChoice.setPrefWidth(120);
        dayChoice.setPromptText("选择上课日");
        TextField timeInput = createTextField("例如: 9-11节");
        Button addScheduleBtn = createButton("添加到列表");
        ListView<String> scheduleList = new ListView<>(FXCollections.observableArrayList());
        scheduleList.setPrefHeight(120);
        Button removeScheduleBtn = createButton("移除所选");
        TextField placeField = createTextField("请输入上课地点");
        TextField capacityField = createTextField("请输入容量（数字）");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 25, 15, 25));
        grid.setAlignment(Pos.CENTER);

        // 添加标签和输入框
        grid.add(createLabel("教师姓名:"), 0, 0);
        grid.add(teacherField, 1, 0);
        grid.add(createLabel("时间安排:"), 0, 1);

        HBox schedInputRow = new HBox(10, dayChoice, timeInput, addScheduleBtn, removeScheduleBtn);
        schedInputRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(schedInputRow, 1, 1);

        grid.add(new Label(), 0, 2); // 空标签占位
        grid.add(scheduleList, 1, 2);

        grid.add(createLabel("地点:"), 0, 3);
        grid.add(placeField, 1, 3);

        grid.add(createLabel("容量:"), 0, 4);
        grid.add(capacityField, 1, 4);

        // 设置列约束
        GridPane.setHgrow(teacherField, Priority.ALWAYS);
        GridPane.setHgrow(timeInput, Priority.ALWAYS);
        GridPane.setHgrow(placeField, Priority.ALWAYS);
        GridPane.setHgrow(capacityField, Priority.ALWAYS);
        GridPane.setHgrow(scheduleList, Priority.ALWAYS);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(grid);

        dialog.getDialogPane().setContent(content);
        ButtonType addType = new ButtonType("添加", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        addScheduleBtn.setOnAction(e -> {
            String day = dayChoice.getEditor().getText();
            if (day == null || day.trim().isEmpty()) {
                showErrorAlert("验证失败", "请填写上课日（如 周一）");
                return;
            }
            String time = timeInput.getText() == null ? "" : timeInput.getText().trim();
            scheduleList.getItems().add(day.trim() + "||" + time);
            scheduleList.getSelectionModel().clearSelection();
            timeInput.clear();
        });

        removeScheduleBtn.setOnAction(e -> {
            int sel = scheduleList.getSelectionModel().getSelectedIndex();
            if (sel >= 0) scheduleList.getItems().remove(sel);
        });

        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                try {
                    // 验证必填字段
                    String teacherName = teacherField.getText() == null ? "" : teacherField.getText().trim();
                    String place = placeField.getText() == null ? "" : placeField.getText().trim();
                    String capacityText = capacityField.getText() == null ? "" : capacityField.getText().trim();

                    if (teacherName.isEmpty() || place.isEmpty() || capacityText.isEmpty()) {
                        showErrorAlert("验证失败", "教师姓名、地点和容量为必填项");
                        return null;
                    }

                    if (scheduleList.getItems().isEmpty()) {
                        showErrorAlert("验证失败", "请至少添加一条时间安排");
                        return null;
                    }

                    TeachingClass tc = new TeachingClass();
                    tc.setUuid(UUID.randomUUID().toString());
                    tc.setCourseId(courseId);
                    tc.setTeacherName(teacherName);

                    Map<String, String> schedMap = new LinkedHashMap<>();
                    for (String item : scheduleList.getItems()) {
                        if (item == null) continue;
                        String[] parts = item.split("\\|\\|", 2);
                        String d = parts.length >= 1 ? parts[0].trim() : "";
                        String t = parts.length >= 2 ? parts[1].trim() : "";
                        if (d.isEmpty()) continue;
                        String prev = schedMap.get(d);
                        if (prev == null || prev.trim().isEmpty()) {
                            schedMap.put(d, t);
                        } else {
                            List<String> list = new ArrayList<>(Arrays.asList(prev.split(",")));
                            if (!t.isEmpty() && !list.contains(t)) {
                                list.add(t);
                                schedMap.put(d, String.join(",", list));
                            }
                        }
                    }
                    tc.setSchedule(new Gson().toJson(schedMap));
                    tc.setPlace(place);
                    tc.setCapacity(Integer.parseInt(capacityText));
                    tc.setSelectedCount(0);
                    return tc;
                } catch (NumberFormatException ex) {
                    showErrorAlert("输入错误", "容量必须为有效的整数");
                    return null;
                }
            }
            return null;
        });

        Optional<TeachingClass> res = dialog.showAndWait();
        res.ifPresent(tc -> {
            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("uuid", tc.getUuid());
                    data.put("courseId", tc.getCourseId());
                    data.put("teacherName", tc.getTeacherName());
                    try {
                        Map<String, Object> scheduleObj = new Gson().fromJson(tc.getSchedule(), Map.class);
                        data.put("schedule", scheduleObj == null ? new HashMap<>() : scheduleObj);
                    } catch (Exception ignore) {
                        data.put("schedule", tc.getSchedule());
                    }
                    data.put("place", tc.getPlace());
                    data.put("capacity", tc.getCapacity());
                    Response rr = CourseService.addTeachingClass(data);
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            showSuccessAlert("操作成功", "新增教学班成功");
                            owner.loadCourseData();
                        } else {
                            showErrorAlert("操作失败", "新增教学班失败: " + rr.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        showErrorAlert("网络异常", "网络连接异常: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }

    private static TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(250);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static Button createButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(100);
        return button;
    }

    private static Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #495057;");
        return label;
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