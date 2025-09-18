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

import java.util.*;

/**
 * 从 CourseAdminPanel 中拆分出来的“新增教学班”对话框
 */
public class AddTeachingClassDialog {
    public static void showForCourse(CourseAdminPanel owner, String courseId) {
        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.setTitle("新增教学班");
        dialog.setHeaderText("为课程 " + courseId + " 添加教学班");

        TextField teacherField = owner.createStyledTextField("");
        javafx.scene.control.ComboBox<String> dayChoice = new javafx.scene.control.ComboBox<>(FXCollections.observableArrayList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        dayChoice.setEditable(true);
        dayChoice.setPrefWidth(140);
        TextField timeInput = owner.createStyledTextField("例如: 9-11节");
        Button addScheduleBtn = new Button("添加到列表");
        ListView<String> scheduleList = new ListView<>(FXCollections.observableArrayList());
        scheduleList.setPrefHeight(120);
        Button removeScheduleBtn = new Button("移除所选");
        TextField placeField = owner.createStyledTextField("");
        TextField capacityField = owner.createStyledTextField("");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("教师姓名:"), 0, 0);
        grid.add(teacherField, 1, 0);
        grid.add(new Label("时间安排 (可添加多条):"), 0, 1);
        HBox schedInputRow = new HBox(8, dayChoice, timeInput, addScheduleBtn, removeScheduleBtn);
        schedInputRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(schedInputRow, 1, 1);
        grid.add(scheduleList, 1, 2);
        grid.add(new Label("地点:"), 0, 3);
        grid.add(placeField, 1, 3);
        grid.add(new Label("容量:"), 0, 4);
        grid.add(capacityField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        ButtonType addType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        addScheduleBtn.setOnAction(e -> {
            String day = dayChoice.getEditor().getText();
            if (day == null || day.trim().isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR, "请填写上课日（如 周一）", ButtonType.OK);
                a.showAndWait();
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
                    TeachingClass tc = new TeachingClass();
                    tc.setUuid(UUID.randomUUID().toString());
                    tc.setCourseId(courseId);
                    tc.setTeacherName(teacherField.getText());
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
                    tc.setPlace(placeField.getText());
                    tc.setCapacity(Integer.parseInt(capacityField.getText()));
                    tc.setSelectedCount(0);
                    return tc;
                } catch (NumberFormatException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "容量必须为整数", ButtonType.OK);
                    a.showAndWait();
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
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "新增成功", ButtonType.OK);
                            a.showAndWait();
                            owner.loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "新增失败: " + rr.getMessage(), ButtonType.OK);
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

