package Client.courseselect;

import Client.ClientNetworkHelper;
import Client.util.EventBus;
import Server.model.Request;
import Server.model.Response;
import Server.model.course.TeachingClass;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelectedCoursesPanel extends BorderPane {
    private final int studentId;
    private VBox courseListContainer;
    private Label statusLabel;
    private final Set<String> pendingSelections = Collections.synchronizedSet(new HashSet<>());

    public SelectedCoursesPanel(int studentId) {
        this.studentId = studentId;
        initializeUI();
        loadSelectedCourses();
    }

    private void initializeUI() {
        // 顶部标题
        Label titleLabel = new Label("已选课程");
        titleLabel.setStyle("-fx-font-family: 'Microsoft YaHei UI';-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setPadding(new Insets(20));
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // 状态标签
        statusLabel = new Label("正在加载已选课程...");
        statusLabel.setStyle("-fx-font-family: 'Microsoft YaHei UI';-fx-font-size: 14px; -fx-text-fill: #666666;");
        HBox statusBox = new HBox(statusLabel);
        statusBox.setPadding(new Insets(0, 20, 10, 20));

        // 课程列表容器
        courseListContainer = new VBox(15);
        courseListContainer.setPadding(new Insets(16, 24, 24, 24));
        courseListContainer.setPrefWidth(800);

        ScrollPane scrollPane = new ScrollPane(courseListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setPrefViewportHeight(600);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        // 不显示滚动条（隐藏水平与垂直滚动条），但允许平移/拖动视图
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        // 刷新按钮
        Button refreshButton = new Button("刷新列表");
        refreshButton.setStyle("-fx-font-family: 'Microsoft YaHei UI';-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> loadSelectedCourses());

        HBox buttonBox = new HBox(refreshButton);
        buttonBox.setPadding(new Insets(10, 20, 20, 20));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox mainContainer = new VBox(titleBox, statusBox, scrollPane, buttonBox);
        mainContainer.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12;");

        // 使 mainContainer 填满 CourseSelectPanel 的中心区域
        mainContainer.setFillWidth(true);
        // 让 scrollPane 随 mainContainer 垂直增长，填充剩余空间
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        // 绑定 mainContainer 大小到当前面板，减去外边距（this.setPadding 的 20*2）保证占满
        mainContainer.prefWidthProperty().bind(this.widthProperty().subtract(40));
        mainContainer.prefHeightProperty().bind(this.heightProperty().subtract(40));

        this.setCenter(mainContainer);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #FFFFFF;");
    }

    private void loadSelectedCourses() {
        statusLabel.setText("正在加载已选课程...");
        courseListContainer.getChildren().clear();

        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", studentId);
                Request request = new Request("getStudentSelectedCourses", data);
                String responseStr = ClientNetworkHelper.send(request);
                Response response = new Gson().fromJson(responseStr, Response.class);

                if (response.getCode() == 200) {
                    Object d = response.getData();
                    List<TeachingClass> list = new ArrayList<>();
                    if (d instanceof List) {
                        String json = new Gson().toJson(d);
                        list = new Gson().fromJson(json, new com.google.gson.reflect.TypeToken<List<TeachingClass>>(){}.getType());
                    }

                    final List<TeachingClass> finalList = list;
                    Platform.runLater(() -> {
                        displaySelectedCoursesList(finalList);
                        statusLabel.setText("已选课程: " + finalList.size());
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("加载已选课程失败: " + response.getMessage());
                        showAlert("错误", "加载已选课程失败: " + response.getMessage(), Alert.AlertType.ERROR);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("网络错误: " + e.getMessage());
                    showAlert("错误", "网络连接失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void displaySelectedCoursesList(List<TeachingClass> list) {
        courseListContainer.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label no = new Label("您尚未选任何课程");
            no.setStyle("-fx-font-size: 16px; -fx-text-fill: #666666;");
            courseListContainer.getChildren().add(no);
            return;
        }

        for (TeachingClass tc : list) {
            courseListContainer.getChildren().add(createSelectedCourseCard(tc));
        }
    }

    private Node createSelectedCourseCard(TeachingClass tc) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #e6f6ec; -fx-background-radius: 6; -fx-border-color: #cfe8d8; -fx-border-width: 1; -fx-border-radius: 6;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(6);
        Label title = new Label(tc.getCourseId() + "  " + (tc.getCourse() != null ? tc.getCourse().getCourseName() : ""));
        title.setStyle("-fx-font-family: 'Microsoft YaHei UI';-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        Label teacher = new Label("教师: " + (tc.getTeacherName() == null ? "" : tc.getTeacherName()));
        teacher.setStyle("-fx-font-family: 'Microsoft YaHei UI';-fx-font-size: 13px; -fx-text-fill: #333333;");
        
        VBox scheduleBox = new VBox(2);
        try {
            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
            Map<String, String> scheduleMap = new Gson().fromJson(tc.getSchedule(), mapType);
            if (scheduleMap != null) {
                for (Map.Entry<String, String> e : scheduleMap.entrySet()) {
                    String formatted = formatScheduleValue(e.getValue());
                    Label line = new Label(e.getKey() + ": " + formatted);
                    line.setStyle("-fx-font-family: 'Microsoft YaHei UI';-fx-font-size: 12px; -fx-text-fill: #666666;");
                    scheduleBox.getChildren().add(line);
                }
            }
        } catch (Exception ignored) {}

        Label place = new Label("地点: " + (tc.getPlace() == null ? "" : tc.getPlace()));
        place.setStyle("-fx-font-family: 'Microsoft YaHei UI';-fx-font-size: 12px; -fx-text-fill: #666666;");
        info.getChildren().addAll(title, teacher, scheduleBox, place);

        Region spacer = new Region(); 
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button dropBtn = new Button("退课");
        dropBtn.setStyle("-fx-font-family: 'Microsoft YaHei UI';-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        dropBtn.setOnAction(e -> {
            dropBtn.setDisable(true);
            pendingSelections.add(tc.getUuid());
            dropCourse(tc, dropBtn);
        });
        dropBtn.setPrefWidth(90);

        card.getChildren().addAll(info, spacer, dropBtn);
        return card;
    }

    private void dropCourse(TeachingClass tc, Button dropBtn) {
        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", studentId);
                data.put("teachingClassUuid", tc.getUuid());

                Request request = new Request("dropCourse", data);
                String responseStr = ClientNetworkHelper.send(request);
                Response response = new Gson().fromJson(responseStr, Response.class);

                Platform.runLater(() -> {
                    pendingSelections.remove(tc.getUuid());
                    if (response.getCode() == 200) {
                        showAlert("成功", "退课成功！", Alert.AlertType.INFORMATION);
                        loadSelectedCourses();
                        EventBus.post("student:courseChanged");
                    } else {
                        dropBtn.setDisable(false);
                        showAlert("退课失败", response.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    pendingSelections.remove(tc.getUuid());
                    dropBtn.setDisable(false);
                    showAlert("错误", "网络连接失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private String formatScheduleValue(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        if (s.contains(":")) return s;

        String[] periodStart = new String[]{"", "08:00", "08:50", "10:00", "10:50", "14:00", "14:50", "15:50", "16:40", "19:00", "19:50", "20:10", "20:55"};
        String[] periodEnd = new String[]{"", "08:45", "09:35", "10:45", "11:30", "14:45", "15:35", "16:35", "17:25", "19:45", "20:35", "20:50", "21:40"};

        Pattern rangePat = Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)\\s*节?$");
        Matcher m = rangePat.matcher(s);
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

        Pattern singlePat = Pattern.compile("^(\\d+)\\s*节?$");
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

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refreshData() {
        loadSelectedCourses(); // 每次进入都刷新已选课程
    }
}