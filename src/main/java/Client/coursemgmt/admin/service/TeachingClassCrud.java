package Client.coursemgmt.admin.service;

import Client.coursemgmt.admin.CourseAdminPanel;
import Server.model.Response;
import Server.model.course.TeachingClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.*;

/**
 * 教学班的增删改对话框与交互逻辑封装
 */
public class TeachingClassCrud {
    public static void showAddTeachingClassDialog(CourseAdminPanel owner, String courseId) {
        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.setTitle("新增教学班");
        dialog.setHeaderText("为课程 " + courseId + " 添加教学班");

        TextField teacherField = new TextField();
        javafx.scene.control.ComboBox<String> dayChoice = new javafx.scene.control.ComboBox<>(FXCollections.observableArrayList("周一","周二","周三","周四","周五","周六","周日"));
        dayChoice.setEditable(true);
        dayChoice.setPrefWidth(140);
        TextField timeInput = new TextField();
        timeInput.setPromptText("例如: 9-11节 或 09:00-10:40");
        Button addScheduleBtn = new Button("添加到列表");
        ListView<String> scheduleList = new ListView<>(FXCollections.observableArrayList());
        scheduleList.setPrefHeight(120);
        Button removeScheduleBtn = new Button("移除所选");
        TextField placeField = new TextField();
        TextField capacityField = new TextField();

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
                        if (t == null) t = "";
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
                    tc.setSchedule(new com.google.gson.Gson().toJson(schedMap));
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
        res.ifPresent(tc -> new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", tc.getUuid());
                data.put("courseId", tc.getCourseId());
                data.put("teacherName", tc.getTeacherName());
                try {
                    Map<String, Object> scheduleObj = new com.google.gson.Gson().fromJson(tc.getSchedule(), Map.class);
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
        }).start());
    }

    public static void showEditTeachingClassDialog(CourseAdminPanel owner, TeachingClass selected) {
        if (selected == null) return;
        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.setTitle("编辑教学班");
        dialog.setHeaderText("编辑教学班信息");

        TextField teacherField = new TextField(selected.getTeacherName());
        javafx.scene.control.ComboBox<String> dayChoice = new javafx.scene.control.ComboBox<>(FXCollections.observableArrayList("周一","周二","周三","周四","周五","周六","周日"));
        dayChoice.setEditable(true);
        TextField timeInput = new TextField();
        timeInput.setPromptText("例如: 9-11节 或 09:00-10:40");
        Button addScheduleBtn = new Button("添加到列表");
        ListView<String> scheduleList = new ListView<>(FXCollections.observableArrayList());
        scheduleList.setPrefHeight(120);
        Button removeScheduleBtn = new Button("移除所选");
        TextField placeField = new TextField(selected.getPlace());
        TextField capacityField = new TextField(String.valueOf(selected.getCapacity()));

        // 解析已有 schedule
        try {
            String raw = selected.getSchedule();
            if (raw != null && !raw.trim().isEmpty()) {
                com.google.gson.Gson g = new com.google.gson.Gson();
                String t = raw.trim();
                if (t.startsWith("{")) {
                    Map<?, ?> m = g.fromJson(t, Map.class);
                    if (m != null) {
                        for (Map.Entry<?, ?> en : m.entrySet()) {
                            String d = en.getKey() == null ? "" : String.valueOf(en.getKey()).trim();
                            Object val = en.getValue();
                            if (d.isEmpty() || val == null) continue;
                            if (val instanceof java.util.List) {
                                for (Object it : (java.util.List<?>) val) {
                                    if (it == null) continue;
                                    String ti = String.valueOf(it).trim();
                                    if (!ti.isEmpty()) scheduleList.getItems().add(d + "||" + ti);
                                }
                            } else {
                                String ti = String.valueOf(val).trim();
                                if (ti.contains(",")) {
                                    String[] parts = ti.split("\\\s*,\\\s*");
                                    for (String p : parts) if (!p.trim().isEmpty()) scheduleList.getItems().add(d + "||" + p.trim());
                                } else {
                                    scheduleList.getItems().add(d + "||" + ti);
                                }
                            }
                        }
                    }
                } else if (t.startsWith("[")) {
                    java.util.List<Map> arr = g.fromJson(t, java.util.List.class);
                    if (arr != null) {
                        for (Map it : arr) {
                            if (it == null) continue;
                            Object od = it.get("day");
                            Object ot = it.get("time");
                            String d = od == null ? "" : String.valueOf(od).trim();
                            String ti = ot == null ? "" : String.valueOf(ot).trim();
                            if (d.isEmpty()) continue;
                            if (ti.contains(",")) {
                                String[] parts = ti.split("\\\s*,\\\s*");
                                for (String p : parts) if (!p.trim().isEmpty()) scheduleList.getItems().add(d + "||" + p.trim());
                            } else {
                                scheduleList.getItems().add(d + "||" + ti);
                            }
                        }
                    }
                } else {
                    scheduleList.getItems().add(t + "||");
                }
            }
        } catch (Exception ignored) {}

        addScheduleBtn.setOnAction(e -> {
            String day = dayChoice.getEditor().getText();
            if (day == null || day.trim().isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR, "请填写上课日（如 周一）", ButtonType.OK);
                a.showAndWait();
                return;
            }
            String time = timeInput.getText() == null ? "" : timeInput.getText().trim();
            scheduleList.getItems().add(day.trim() + "||" + time);
            timeInput.clear();
        });
        removeScheduleBtn.setOnAction(e -> {
            int sel = scheduleList.getSelectionModel().getSelectedIndex();
            if (sel >= 0) scheduleList.getItems().remove(sel);
        });

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
        ButtonType saveType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                try {
                    TeachingClass tc = new TeachingClass();
                    tc.setUuid(selected.getUuid());
                    tc.setCourseId(selected.getCourseId());
                    tc.setTeacherName(teacherField.getText());
                    Map<String, String> schedMap = new LinkedHashMap<>();
                    for (String item : scheduleList.getItems()) {
                        if (item == null) continue;
                        String[] parts = item.split("\\|\\|", 2);
                        String d = parts.length >= 1 ? parts[0].trim() : "";
                        String t = parts.length >= 2 ? parts[1].trim() : "";
                        if (d.isEmpty()) continue;
                        if (t == null) t = "";
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
                    tc.setSchedule(new com.google.gson.Gson().toJson(schedMap));
                    tc.setPlace(placeField.getText());
                    tc.setCapacity(Integer.parseInt(capacityField.getText()));
                    tc.setSelectedCount(selected.getSelectedCount());
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
        res.ifPresent(tc -> new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", tc.getUuid());
                data.put("teacherName", tc.getTeacherName());
                try {
                    Map<String, Object> scheduleObj = new com.google.gson.Gson().fromJson(tc.getSchedule(), Map.class);
                    data.put("schedule", scheduleObj == null ? new HashMap<>() : scheduleObj);
                } catch (Exception ignore) {
                    data.put("schedule", tc.getSchedule());
                }
                data.put("place", tc.getPlace());
                data.put("capacity", tc.getCapacity());
                Response rr = CourseService.updateTeachingClass(data);
                Platform.runLater(() -> {
                    if (rr.getCode() == 200) {
                        Alert a = new Alert(Alert.AlertType.INFORMATION, "更新成功", ButtonType.OK);
                        a.showAndWait();
                        owner.loadCourseData();
                    } else {
                        Alert a = new Alert(Alert.AlertType.ERROR, "更新失败: " + rr.getMessage(), ButtonType.OK);
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

