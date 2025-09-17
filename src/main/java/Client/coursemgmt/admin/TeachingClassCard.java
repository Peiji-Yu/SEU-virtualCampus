package Client.coursemgmt.admin;

import Server.model.Response;
import Server.model.course.TeachingClass;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;


/**
 * 将原来 CourseAdminPanel 中 createTeachingClassCard 的逻辑独立成组件，行为通过 CourseAdminPanel 回调
 */
public class TeachingClassCard extends VBox {
    private final TeachingClass tc;
    private final CourseAdminPanel owner;

    public TeachingClassCard(TeachingClass tc, CourseAdminPanel owner, FlowPane parent) {
        super(8);
        this.tc = tc;
        this.owner = owner;
        initUI(parent);
    }

    private void initUI(FlowPane parent) {
        this.setPadding(new Insets(10));
        String normalStyle = "-fx-background-radius: 6; -fx-background-color: #F5F7FA;";
        this.setStyle(normalStyle);
        this.setPrefHeight(Region.USE_COMPUTED_SIZE);
        this.setMinHeight(Region.USE_COMPUTED_SIZE);

        try {
            this.prefWidthProperty().bind(parent.widthProperty()
                    .subtract(32)
                    .subtract(parent.getHgap() * 3)
                    .divide(4));
            this.minWidthProperty().bind(this.prefWidthProperty().multiply(0.75));
            this.maxWidthProperty().bind(this.prefWidthProperty().multiply(1.05));
        } catch (Exception ignored) {
            this.setPrefWidth(240);
        }

        Label teacher = new Label("教师: " + (tc.getTeacherName() == null ? "未知" : tc.getTeacherName()));
        teacher.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        teacher.setWrapText(true);

        // 将 schedule 字符串解析为 Map<String,String> 并格式化显示，行为与学生端一致
        VBox scheduleBox = new VBox(2);
        scheduleBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        Label timeTitle = new Label("时间:");
        timeTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-weight: bold;");
        scheduleBox.getChildren().add(timeTitle);

        String scheduleJson = tc.getSchedule();
        if (scheduleJson != null && !scheduleJson.trim().isEmpty()) {
            try {
                java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
                java.util.Map<String, String> scheduleMap = new com.google.gson.Gson().fromJson(scheduleJson, mapType);
                if (scheduleMap != null && !scheduleMap.isEmpty()) {
                    for (java.util.Map.Entry<String, String> e : scheduleMap.entrySet()) {
                        String formatted = formatScheduleValue(e.getValue());
                        Label dayLine = new Label(e.getKey() + ": " + formatted);
                        dayLine.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                        dayLine.setWrapText(true);
                        scheduleBox.getChildren().add(dayLine);
                    }
                } else {
                    Label raw = new Label(scheduleJson);
                    raw.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                    raw.setWrapText(true);
                    scheduleBox.getChildren().add(raw);
                }
            } catch (Exception ex) {
                Label raw = new Label(scheduleJson);
                raw.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                raw.setWrapText(true);
                scheduleBox.getChildren().add(raw);
            }
        } else {
            Label unset = new Label("未设置");
            unset.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
            scheduleBox.getChildren().add(unset);
        }

        Label place = new Label("地点: " + (tc.getPlace() == null ? "未设置" : tc.getPlace()));
        place.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        place.setWrapText(true);

        int sel = tc.getSelectedCount() == null ? 0 : tc.getSelectedCount();
        int cap = tc.getCapacity() == null ? 0 : tc.getCapacity();
        Label capacity = new Label("容量: " + sel + "/" + cap);
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        capacity.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewListBtn = new Button("查看名单");
        viewListBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4e8cff;");
        viewListBtn.setOnAction(e -> owner.showStudentListDialog(tc.getUuid(), tc.getCourseId() + " " + (tc.getCourse() == null ? "" : tc.getCourse().getCourseName())));

        Button addStudentBtn = new Button("添加学生");
        addStudentBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addStudentBtn.setOnAction(e -> {
            TextInputDialog inputDialog = new TextInputDialog();
            inputDialog.setTitle("添加学生到教学班");
            inputDialog.setHeaderText("请输入学生一卡通号");
            inputDialog.setContentText("一卡通号:");
            inputDialog.showAndWait().ifPresent(cardNumber -> {
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
                                owner.loadCourseData();
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
        });

        Button editBtn = new Button("编辑");
        editBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");
        editBtn.setOnAction(e -> TeachingClassCrud.showEditTeachingClassDialog(owner, tc));

        Button delBtn = new Button("删除");
        delBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        delBtn.setOnAction(e -> TeachingClassCrud.deleteTeachingClassConfirmed(owner, tc));

        HBox btnRow = new HBox(8, spacer, viewListBtn, addStudentBtn, editBtn, delBtn);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        this.getChildren().addAll(teacher, scheduleBox, place, capacity, btnRow);
        this.requestLayout();
    }

    // 与学生端保持一致的时间格式化逻辑（将节次等简单表达转换为具体时间段）
    private String formatScheduleValue(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        if (s.contains(":")) return s;

        String[] periodStart = new String[]{"", "08:00", "08:50", "9:50", "10:40","11:30",
                "14:00", "14:50", "15:50", "16:40",
                "17:30", "19:00", "19:50", "20:40"};
        String[] periodEnd = new String[]{"", "08:45", "09:35", "10:35", "11:25", "12:15",
                "14:45", "15:35", "16:35", "17:25",
                "18:15", "19:45", "20:35", "21:25"};

        java.util.regex.Pattern rangePat = java.util.regex.Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)\\s*节?$");
        java.util.regex.Matcher m = rangePat.matcher(s);
        if (m.find()) {
            try {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                if (a < 1) a = 1;
                if (b < 1) b = 1;
                if (a >= periodStart.length) a = periodStart.length - 1;
                if (b >= periodEnd.length) b = periodEnd.length - 1;
                if (a > b) { int tmp=a; a=b; b=tmp; }
                String start = periodStart[a];
                String end = periodEnd[b];
                if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                    return s + " (" + start + "-" + end + ")";
                }
            } catch (Exception ignored) {}
        }

        java.util.regex.Pattern singlePat = java.util.regex.Pattern.compile("^(\\d+)\\s*节?$");
        m = singlePat.matcher(s);
        if (m.find()) {
            try {
                int p = Integer.parseInt(m.group(1));
                if (p < 1) p = 1;
                if (p >= periodStart.length) p = periodStart.length - 1;
                String start = periodStart[p];
                String end = periodEnd[p];
                if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                    return s + " (" + start + "-" + end + ")";
                }
            } catch (Exception ignored) {}
        }

        return s;
    }
}
