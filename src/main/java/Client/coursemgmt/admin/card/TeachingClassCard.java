package Client.coursemgmt.admin.card;

import Client.coursemgmt.admin.CourseAdminPanel;
import Client.coursemgmt.admin.dialog.AddStudentDialog;
import Client.coursemgmt.admin.service.TeachingClassCrud;
import Server.model.course.TeachingClass;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.Objects;


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
        HBox scheduleHeader = new HBox(2);
        VBox timeBox = new VBox(2);
        scheduleBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        scheduleHeader.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        timeBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        Label timeTitle = new Label("时间:");
        timeTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-weight: bold;");
        scheduleHeader.getChildren().add(timeTitle);

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
                        timeBox.getChildren().add(dayLine);
                    }
                } else {
                    Label raw = new Label(scheduleJson);
                    raw.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                    raw.setWrapText(true);
                    timeBox.getChildren().add(raw);
                }
            } catch (Exception ex) {
                Label raw = new Label(scheduleJson);
                raw.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                raw.setWrapText(true);
                timeBox.getChildren().add(raw);
            }
        } else {
            Label unset = new Label("未设置");
            unset.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
            timeBox.getChildren().add(unset);
        }
        scheduleHeader.getChildren().add(timeBox);
        scheduleBox.getChildren().add(scheduleHeader);
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

        // 顶部行：左侧为教师信息，右侧为编辑/删除按钮
        HBox topRow = new HBox();
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox editDelBox = new HBox(6);
        editDelBox.setAlignment(javafx.geometry.Pos.TOP_RIGHT);

        Button viewListBtn = new Button("查看名单");
        viewListBtn.setStyle("-fx-background-color: #1D8C4F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
        viewListBtn.setPrefHeight(30);
        viewListBtn.setOnMouseEntered(e -> viewListBtn.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;"));
        viewListBtn.setOnMouseExited(e -> viewListBtn.setStyle("-fx-background-color: #1D8C4F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 13;"));
        viewListBtn.setOnAction(e -> owner.showStudentListDialog(tc.getUuid(), tc.getCourseId() + " " + (tc.getCourse() == null ? "" : tc.getCourse().getCourseName())));



        Button addStudentBtn = new Button("添加学生");
        addStudentBtn.setStyle("-fx-background-color: #1D8C4F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
        addStudentBtn.setPrefHeight(30);
        addStudentBtn.setOnMouseEntered(e -> addStudentBtn.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;"));
        addStudentBtn.setOnMouseExited(e -> addStudentBtn.setStyle("-fx-background-color: #1D8C4F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;"));
        addStudentBtn.setOnAction(e -> AddStudentDialog.showAndHandle(owner, tc));


        Button editBtn = new Button();
        Image editImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/编辑.png")));
        ImageView editView = new ImageView(editImg);
        editView.setFitWidth(20);
        editView.setFitHeight(20);
        editView.setPreserveRatio(true);
        editView.setSmooth(true);
        editBtn.setGraphic(editView);
        editBtn.setTooltip(new Tooltip("编辑"));
        editBtn.setStyle("-fx-background-color: transparent;");
        editBtn.setPrefSize(26, 26);
        editBtn.setOnAction(e -> TeachingClassCrud.showEditTeachingClassDialog(owner, tc));
        editBtn.setOnMouseEntered(e ->editBtn.setStyle("-fx-background-color: transparent;" +
                "-fx-effect: dropshadow(gaussian, #1E1F22, 20, 0, 0, 0);"));
        editBtn.setOnMouseExited(e ->editBtn.setStyle("-fx-background-color: transparent;"));


        Button delBtn = new Button();
        Image delImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/删除.png")));
        ImageView delView = new ImageView(delImg);
        delView.setFitWidth(20);
        delView.setFitHeight(20);
        delView.setPreserveRatio(true);
        delView.setSmooth(true);
        delBtn.setGraphic(delView);
        delBtn.setTooltip(new Tooltip("删除"));
        delBtn.setStyle("-fx-background-color: transparent;");
        delBtn.setPrefSize(26, 26);
        delBtn.setOnAction(e -> TeachingClassCrud.deleteTeachingClassConfirmed(owner, tc));
        delBtn.setOnMouseEntered(e ->delBtn.setStyle("-fx-background-color: transparent;" +
                "-fx-effect: dropshadow(gaussian, #1E1F22, 20, 0, 0, 0);"));
        delBtn.setOnMouseExited(e ->delBtn.setStyle("-fx-background-color: transparent;"));

        // 将编辑和删除按钮移动到右上角容器中
        editDelBox.getChildren().addAll(editBtn, delBtn);

        HBox btnRow = new HBox();
        double btnSpacing = 8;
        btnRow.setSpacing(btnSpacing);
        btnRow.setPadding(new Insets(0, btnSpacing, 0, btnSpacing));
        btnRow.setAlignment(javafx.geometry.Pos.CENTER);

        // 创建三个“弹簧”
        Region spring1 = new Region();
        Region spring2 = new Region();
        Region spring3 = new Region();

        // 让弹簧可以水平拉伸
        HBox.setHgrow(spring1, Priority.ALWAYS);
        HBox.setHgrow(spring2, Priority.ALWAYS);
        HBox.setHgrow(spring3, Priority.ALWAYS);

        // 把弹簧和按钮按顺序加入
        btnRow.getChildren().addAll(spring1, viewListBtn, spring2, addStudentBtn, spring3);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER);

        // 顶部放置教师和右侧编辑/删除按钮，底部保留查看/添加按钮
        topRow.getChildren().addAll(teacher, topSpacer, editDelBox);
        this.getChildren().addAll(topRow, scheduleBox, place, capacity, btnRow);
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
