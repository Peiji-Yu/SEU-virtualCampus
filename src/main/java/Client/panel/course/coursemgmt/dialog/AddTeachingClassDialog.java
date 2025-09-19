package Client.panel.course.coursemgmt.dialog;

import Client.panel.course.coursemgmt.CourseAdminPanel;
import Client.panel.course.coursemgmt.service.CourseService;
import Client.model.Response;
import Client.model.course.TeachingClass;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 从 CourseAdminPanel 中拆分出来的“新增教学班”对话框
 */
public class AddTeachingClassDialog {
    public static void showForCourse(CourseAdminPanel owner, String courseId) {
        // 预取所有教学班以便在提交时进行冲突检测，避免在 UI 线程阻塞网络请求
        AtomicReference<List<TeachingClass>> allRef = new AtomicReference<>();
        new Thread(() -> {
            try {
                List<TeachingClass> fetched = CourseService.fetchAllTeachingClasses();
                allRef.set(fetched == null ? new ArrayList<>() : fetched);
            } catch (Exception ignored) {
                allRef.set(null); // 标记无法获取
            }
        }).start();

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

        // 占位标签
        // 空标签占位
        grid.add(new Label(), 0, 2);
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
            if (sel >= 0) {
                scheduleList.getItems().remove(sel);
            }
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
                        if (item == null) {
                            continue;
                        }
                        String[] parts = item.split("\\|\\|", 2);
                        String d = parts.length >= 1 ? parts[0].trim() : "";
                        String t = parts.length >= 2 ? parts[1].trim() : "";
                        if (d.isEmpty()) {
                            continue;
                        }
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
                    // 前端冲突检测（使用预取的数据进行冲突检测）
                    List<TeachingClass> all = allRef.get();
                    if (all == null) {
                        showErrorAlert("网络异常", "无法验证时间冲突（未能获取教学班列表）");
                        return null;
                    }
                    String conflict = detectConflict(tc, place, all);
                    if (conflict != null) {
                        showErrorAlert("时间冲突", conflict);
                        return null;
                    }

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
                    // 最终同步冲突检查，避免预取数据过时导致漏检
                    try {
                        List<TeachingClass> latest = CourseService.fetchAllTeachingClasses();
                        String conflictNow = detectConflict(tc, tc.getPlace(), latest);
                        if (conflictNow != null) {
                            Platform.runLater(() -> showErrorAlert("时间冲突", "提交前检测到冲突: " + conflictNow));
                            return;
                        }
                    } catch (Exception ex) {
                        Platform.runLater(() -> showErrorAlert("网络异常", "无法完成提交前的冲突校验: " + ex.getMessage()));
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("uuid", tc.getUuid());
                    data.put("courseId", tc.getCourseId());
                    data.put("teacherName", tc.getTeacherName());
                    try {
                        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                        Map<String, Object> scheduleObj = new Gson().fromJson(tc.getSchedule(), mapType);
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
                    Platform.runLater(() -> showErrorAlert("网络异常", "网络连接异常: " + ex.getMessage()));
                }
            }).start();
        });
    }

    // 新增：复用新增对话框用于编辑教学班，预填并保存到 updateTeachingClass
    public static void showEdit(CourseAdminPanel owner, TeachingClass selected) {
        if (selected == null) {
            return;
        }
        // 预取所有教学班以便在提交时进行冲突检测，避免在 UI 线程阻塞网络请求
        AtomicReference<List<TeachingClass>> allRef = new AtomicReference<>();
        new Thread(() -> {
            try {
                List<TeachingClass> fetched = CourseService.fetchAllTeachingClasses();
                allRef.set(fetched == null ? new ArrayList<>() : fetched);
            } catch (Exception ignored) {
                allRef.set(null);
            }
        }).start();

        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.setTitle("编辑教学班");
        dialog.setHeaderText("编辑教学班信息");
        dialog.getDialogPane().getStylesheets().add("/styles/dialog.css");

        TextField teacherField = createTextField("请输入教师姓名");
        teacherField.setText(selected.getTeacherName());
        ComboBox<String> dayChoice = new ComboBox<>(FXCollections.observableArrayList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        dayChoice.setEditable(true);
        dayChoice.setPrefWidth(120);
        TextField timeInput = createTextField("例如: 9-11节");
        Button addScheduleBtn = createButton("添加到列表");
        ListView<String> scheduleList = new ListView<>(FXCollections.observableArrayList());
        scheduleList.setPrefHeight(120);
        Button removeScheduleBtn = createButton("移除所选");
        TextField placeField = createTextField("请输入上课地点");
        placeField.setText(selected.getPlace() == null ? "" : selected.getPlace());
        TextField capacityField = createTextField("请输入容量（数字）");
        capacityField.setText(selected.getCapacity() == null ? "" : String.valueOf(selected.getCapacity()));

        // 解析已有 schedule（兼容 map/array/plain 格式）
        try {
            String raw = selected.getSchedule();
            if (raw != null && !raw.trim().isEmpty()) {
                Gson g = new Gson();
                String t = raw.trim();
                if (t.startsWith("{")) {
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> m = g.fromJson(t, mapType);
                    if (m != null) {
                        for (Map.Entry<String, Object> en : m.entrySet()) {
                            String d = en.getKey() == null ? "" : String.valueOf(en.getKey()).trim();
                            Object val = en.getValue();
                            if (d.isEmpty() || val == null) {
                                continue;
                            }
                            if (val instanceof java.util.List) {
                                for (Object it : (java.util.List<?>) val) {
                                    if (it == null) {
                                        continue;
                                    }
                                    String ti = String.valueOf(it).trim();
                                    if (!ti.isEmpty()) {
                                        scheduleList.getItems().add(d + "||" + ti);
                                    }
                                }
                            } else {
                                String ti = String.valueOf(val).trim();
                                if (ti.contains(",")) {
                                    String[] parts = ti.split("\\s*,\\s*");
                                    for (String p : parts) {
                                        if (!p.trim().isEmpty()) {
                                            scheduleList.getItems().add(d + "||" + p.trim());
                                        }
                                    }
                                } else {
                                    scheduleList.getItems().add(d + "||" + ti);
                                }
                            }
                        }
                    }
                } else if (t.startsWith("[")) {
                    Type listMapType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    java.util.List<Map<String, Object>> arr = g.fromJson(t, listMapType);
                    if (arr != null) {
                        for (Map<String, Object> it : arr) {
                            if (it == null) {
                                continue;
                            }
                            Object od = it.get("day");
                            Object ot = it.get("time");
                            String d = od == null ? "" : String.valueOf(od).trim();
                            String ti = ot == null ? "" : String.valueOf(ot).trim();
                            if (d.isEmpty()) {
                                continue;
                            }
                            if (ti.contains(",")) {
                                String[] parts = ti.split("\\s*,\\s*");
                                for (String p : parts) {
                                    if (!p.trim().isEmpty()) {
                                        scheduleList.getItems().add(d + "||" + p.trim());
                                    }
                                }
                            } else {
                                scheduleList.getItems().add(d + "||" + ti);
                            }
                        }
                    }
                } else {
                    scheduleList.getItems().add(t + "||");
                }
            }
        } catch (Exception ignored) {
        }

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
            if (sel >= 0) {
                scheduleList.getItems().remove(sel);
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 25, 15, 25));
        grid.setAlignment(Pos.CENTER);

        grid.add(createLabel("教师姓名:"), 0, 0);
        grid.add(teacherField, 1, 0);
        grid.add(createLabel("时间安排:"), 0, 1);
        HBox schedInputRow = new HBox(10, dayChoice, timeInput, addScheduleBtn, removeScheduleBtn);
        schedInputRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(schedInputRow, 1, 1);
        // 空标签占位
        grid.add(new Label(), 0, 2);
        grid.add(scheduleList, 1, 2);
        grid.add(createLabel("地点:"), 0, 3);
        grid.add(placeField, 1, 3);
        grid.add(createLabel("容量:"), 0, 4);
        grid.add(capacityField, 1, 4);

        GridPane.setHgrow(teacherField, Priority.ALWAYS);
        GridPane.setHgrow(timeInput, Priority.ALWAYS);
        GridPane.setHgrow(placeField, Priority.ALWAYS);
        GridPane.setHgrow(capacityField, Priority.ALWAYS);
        GridPane.setHgrow(scheduleList, Priority.ALWAYS);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(grid);

        dialog.getDialogPane().setContent(content);
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
                        if (item == null) {
                            continue;
                        }
                        String[] parts = item.split("\\|\\|", 2);
                        String d = parts.length >= 1 ? parts[0].trim() : "";
                        String t = parts.length >= 2 ? parts[1].trim() : "";
                        if (d.isEmpty()) {
                            continue;
                        }
                        if (t == null) {
                            t = "";
                        }
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
                    tc.setSelectedCount(selected.getSelectedCount());
                    // 前端冲突检测
                    List<TeachingClass> all = allRef.get();
                    if (all == null) {
                        showErrorAlert("网络异常", "无法验证时间冲突（未能获取教学班列表）");
                        return null;
                    }
                    String conflict = detectConflict(tc, tc.getPlace(), all);
                    if (conflict != null) {
                        showErrorAlert("时间冲突", conflict);
                        return null;
                    }
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
                    // 最终同步冲突检查（与新增流程一致）
                    try {
                        List<TeachingClass> latest = CourseService.fetchAllTeachingClasses();
                        String conflictNow = detectConflict(tc, tc.getPlace(), latest);
                        if (conflictNow != null) {
                            Platform.runLater(() -> showErrorAlert("时间冲突", "提交前检测到冲突: " + conflictNow));
                            return;
                        }
                    } catch (Exception ex) {
                        Platform.runLater(() -> showErrorAlert("网络异常", "无法完成提交前的冲突校验: " + ex.getMessage()));
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("uuid", tc.getUuid());
                    data.put("teacherName", tc.getTeacherName());
                    try {
                        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                        Map<String, Object> scheduleObj = new Gson().fromJson(tc.getSchedule(), mapType);
                        data.put("schedule", scheduleObj == null ? new HashMap<>() : scheduleObj);
                    } catch (Exception ignore) {
                        data.put("schedule", tc.getSchedule());
                    }
                    data.put("place", tc.getPlace());
                    data.put("capacity", tc.getCapacity());
                    Response rr = CourseService.updateTeachingClass(data);
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            showSuccessAlert("操作成功", "更新教学班成功");
                            owner.loadCourseData();
                        } else {
                            showErrorAlert("操作失败", "更新教学班失败: " + rr.getMessage());
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

    // 冲突检测主逻辑，返回冲突描述或 null
    private static String detectConflict(TeachingClass candidate, String place, List<TeachingClass> all) {
        if (candidate == null || all == null) {
            return null;
        }
        String candTeacher = candidate.getTeacherName() == null ? "" : candidate.getTeacherName().trim();

        // 将候选与已有教学班的 schedule 统一解析为 Map<day, List<range>> 格式
        Map<String, List<int[]>> candMap = buildScheduleRangesMapFromJson(candidate.getSchedule());
        if (candMap == null || candMap.isEmpty()) {
            return null;
        }

        for (TeachingClass ex : all) {
            if (ex == null) {
                continue;
            }
            // skip self when editing
            if (candidate.getUuid() != null && candidate.getUuid().equals(ex.getUuid())) {
                continue;
            }

            Map<String, List<int[]>> exMap = buildScheduleRangesMapFromJson(ex.getSchedule());
            if (exMap == null || exMap.isEmpty()) {
                continue;
            }

            // teacher conflict
            String exTeacher = ex.getTeacherName() == null ? "" : ex.getTeacherName().trim();
            boolean sameTeacher = !candTeacher.isEmpty() && !exTeacher.isEmpty() && candTeacher.equalsIgnoreCase(exTeacher);

            // place conflict
            String exPlace = ex.getPlace() == null ? "" : ex.getPlace().trim();
            boolean samePlace = place != null && !place.trim().isEmpty() && exPlace.equalsIgnoreCase(place.trim());

            if (!sameTeacher && !samePlace) {
                continue;
            }

            // compare per day using normalized maps
            for (Map.Entry<String, List<int[]>> ce : candMap.entrySet()) {
                String day = ce.getKey();
                if (day == null) {
                    continue;
                }
                List<int[]> candRanges = ce.getValue();
                List<int[]> exRanges = exMap.get(day);
                if (exRanges == null || exRanges.isEmpty()) {
                    continue;
                }
                for (int[] a : candRanges) {
                    for (int[] b : exRanges) {
                        if (rangesOverlap(a, b)) {
                            // 构建更友好的冲突描述，包含课程与教师信息（若可用）
                            String exCourseInfo = ex.getCourse() != null && ex.getCourse().getCourseName() != null
                                    ? (ex.getCourseId() + " " + ex.getCourse().getCourseName())
                                    : ex.getCourseId();
                            String exTeacherName = ex.getTeacherName() == null ? "未知" : ex.getTeacherName();
                            if (sameTeacher) {
                                return String.format("教师 %s 在 %s 的时间 %s 与课程 %s 的教学班（教师：%s）冲突", candTeacher, day, formatRange(a), exCourseInfo, exTeacherName);
                            }
                            if (samePlace) {
                                return String.format("教室 %s 在 %s 的时间 %s 与课程 %s 的教学班（教师：%s）冲突", place, day, formatRange(a), exCourseInfo, exTeacherName);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    // 将任意 JSON schedule 字符串解析为 Map<day, List<range>>，兼容值为字符串/数组/对象等
    private static Map<String, List<int[]>> buildScheduleRangesMapFromJson(String scheduleJson) {
        Map<String, List<int[]>> out = new LinkedHashMap<>();
        if (scheduleJson == null || scheduleJson.trim().isEmpty()) {
            return out;
        }
        try {
            Gson g = new Gson();
            Type token = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> raw = g.fromJson(scheduleJson, token);
            if (raw == null) {
                return out;
            }
            for (Map.Entry<String, Object> en : raw.entrySet()) {
                String day = en.getKey() == null ? null : en.getKey().trim();
                if (day == null || day.isEmpty()) {
                    continue;
                }
                Object val = en.getValue();
                List<int[]> ranges = parseRangesFromObject(val);
                if (ranges != null && !ranges.isEmpty()) {
                    out.put(day, ranges);
                }
            }
        } catch (Exception ex) {
            // 解析失败返回空 map
        }
        return out;
    }

    // 解析任意类型的 schedule 值（String / List / Map），返回分钟区间列表
    private static List<int[]> parseRangesFromObject(Object val) {
        List<int[]> out = new ArrayList<>();
        if (val == null) {
            return out;
        }
        if (val instanceof String) {
            return parseRangesFromValue((String) val);
        }
        if (val instanceof Number) {
            // treat as single period number
            String s = String.valueOf(((Number) val).intValue());
            return parseRangesFromValue(s);
        }
        if (val instanceof List) {
            for (Object it : (List<?>) val) {
                if (it == null) {
                    continue;
                }
                if (it instanceof String) {
                    out.addAll(parseRangesFromValue((String) it));
                } else if (it instanceof Number) {
                    out.addAll(parseRangesFromValue(String.valueOf(((Number) it).intValue())));
                } else {
                    // fallback to toString
                    out.addAll(parseRangesFromValue(String.valueOf(it)));
                }
            }
            return out;
        }
        // fallback: use toString
        return parseRangesFromValue(String.valueOf(val));
    }

    // 解析单个 schedule value（可能包含逗号分隔多个 token），返回分钟范围列表
    private static List<int[]> parseRangesFromValue(String val) {
        List<int[]> out = new ArrayList<>();
        if (val == null) return out;
        // 支持中英文逗号分隔
        String normalized = val.replace('，', ',');
        String[] parts = normalized.split("\\s*,\\s*");
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (t.isEmpty()) continue;
            int[] r = parseSingleTokenToRange(t);
            if (r != null) out.add(r);
        }
        return out;
    }

    // 将单个 token 转换为 [startMin,endMin]
    private static int[] parseSingleTokenToRange(String t) {
        if (t == null || t.trim().isEmpty()) return null;
        t = t.trim();
        // 移除常见中文修饰，如“第”“节”等，便于匹配数字或时间格式
        t = t.replaceAll("[第节\u3000]", "").trim();
        // 如果包含 ':'，视为具体时间段 hh:mm-hh:mm
        if (t.contains(":")) {
            String[] parts = t.split("-", 2);
            try {
                int s = parseTimeToMinutes(parts[0].trim());
                int e = parts.length > 1 ? parseTimeToMinutes(parts[1].trim()) : s + 45;
                return new int[]{s, e};
            } catch (Exception ex) { return null; }
        }
        // 如果是数字范围 9-11 或 单个数字 9
        java.util.regex.Pattern rangePat = java.util.regex.Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)$");
        java.util.regex.Matcher m = rangePat.matcher(t);
        if (m.find()) {
            try {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                return periodIndexRangeToMinutes(Math.min(a,b), Math.max(a,b));
            } catch (Exception ex) { return null; }
        }
        java.util.regex.Pattern singlePat = java.util.regex.Pattern.compile("^(\\d+)$");
        m = singlePat.matcher(t);
        if (m.find()) {
            try {
                int p = Integer.parseInt(m.group(1));
                return periodIndexRangeToMinutes(p, p);
            } catch (Exception ex) { return null; }
        }
        return null;
    }

    private static int parseTimeToMinutes(String hhmm) {
        String[] a = hhmm.split(":");
        int hh = Integer.parseInt(a[0]);
        int mm = a.length > 1 ? Integer.parseInt(a[1]) : 0;
        return hh * 60 + mm;
    }

    private static int[] periodIndexRangeToMinutes(int a, int b) {
        String[] periodStart = new String[]{"", "08:00", "08:50", "09:50", "10:40","11:30",
                "14:00", "14:50", "15:50", "16:40",
                "17:30", "19:00", "19:50", "20:40"};
        String[] periodEnd = new String[]{"", "08:45", "09:35", "10:35", "11:25", "12:15",
                "14:45", "15:35", "16:35", "17:25",
                "18:15", "19:45", "20:35", "21:25"};
        if (a < 1) a = 1; if (b < 1) b = 1;
        if (a >= periodStart.length) a = periodStart.length - 1;
        if (b >= periodEnd.length) b = periodEnd.length - 1;
        int s = parseTimeToMinutes(periodStart[a]);
        int e = parseTimeToMinutes(periodEnd[b]);
        return new int[]{s, e};
    }

    private static boolean rangesOverlap(int[] a, int[] b) {
        if (a == null || b == null) return false;
        return a[0] < b[1] && b[0] < a[1];
    }

    private static String formatRange(int[] r) {
        if (r == null) return "";
        int sh = r[0] / 60, sm = r[0] % 60, eh = r[1] / 60, em = r[1] % 60;
        return String.format("%02d:%02d-%02d:%02d", sh, sm, eh, em);
    }
}

