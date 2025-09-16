package Client.courseselect;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import Client.timetable.TimetablePanel  ;

public class CourseSelectMainPanel extends BorderPane {
    private CourseSelectPanel courseSelectPanel;
    private SelectedCoursesPanel selectedCoursesPanel;
    private TimetablePanel timetablePanel;
    private Button currentSelectedButton;
    private final int studentId;

    public CourseSelectMainPanel(int studentId) {
        this.studentId = studentId;
        initializeUI();
    }

    private void initializeUI() {
        // 左侧导航栏
        VBox leftBar = new VBox();
        leftBar.setStyle("-fx-background-color: #f4f4f4;"
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.2), 10, 0, 1, 0);");
        leftBar.setPrefWidth(210);

        // 设置说明标签
        Label leftLabel = new Label("课程选课");
        leftLabel.setStyle("-fx-text-fill: #303030; -fx-font-family: 'Microsoft YaHei UI'; " +
                "-fx-font-size: 12px; -fx-alignment: center-left; -fx-padding: 10 0 10 15;");

        // 添加分割线
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator.setMaxWidth(Double.MAX_VALUE);

        // 全部课程按钮
        Button allCoursesButton = new Button("全部课程");
        allCoursesButton.setPrefWidth(210);
        allCoursesButton.setPrefHeight(56);
        setSelectedButtonStyle(allCoursesButton);
        currentSelectedButton = allCoursesButton;

        allCoursesButton.setOnAction(e -> {
            if (currentSelectedButton != allCoursesButton) {
                resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(allCoursesButton);
                currentSelectedButton = allCoursesButton;

                // 初始化全部课程页面
                if (courseSelectPanel == null) {
                    courseSelectPanel = new CourseSelectPanel(studentId);
                }
                courseSelectPanel.refreshData();
                setCenter(courseSelectPanel);
            }
        });

        // 添加分割线
        Region separator1 = new Region();
        separator1.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator1.setMaxWidth(Double.MAX_VALUE);

        // 已选课程按钮
        Button selectedCoursesButton = new Button("已选课程");
        selectedCoursesButton.setPrefWidth(210);
        selectedCoursesButton.setPrefHeight(56);
        resetButtonStyle(selectedCoursesButton);

        selectedCoursesButton.setOnAction(e -> {
            if (currentSelectedButton != selectedCoursesButton) {
                resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(selectedCoursesButton);
                currentSelectedButton = selectedCoursesButton;

                // 初始化已选课程页面
                if (selectedCoursesPanel == null) {
                    selectedCoursesPanel = new SelectedCoursesPanel(studentId);
                }
                selectedCoursesPanel.refreshData();
                setCenter(selectedCoursesPanel);
            }
        });

        // 添加分割线
        Region separator2 = new Region();
        separator2.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator2.setMaxWidth(Double.MAX_VALUE);

        // 我的课表课程按钮
        Button timetableButton = new Button("我的课表");
        timetableButton.setPrefWidth(210);
        timetableButton.setPrefHeight(56);
        resetButtonStyle(timetableButton);

        timetableButton.setOnAction(e -> {
            if (currentSelectedButton != timetableButton) {
                resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(timetableButton);
                currentSelectedButton = timetableButton;

                // 初始化已选课程页面
                if (timetablePanel == null) {
                    timetablePanel = new TimetablePanel(Integer.toString(studentId));
                }
                setCenter(timetablePanel);
            }
        });

        // 添加分割线
        Region separator3 = new Region();
        separator3.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator3.setMaxWidth(Double.MAX_VALUE);


        leftBar.getChildren().addAll(leftLabel, separator,
                allCoursesButton, separator1,
                selectedCoursesButton, separator2,
                timetableButton, separator3);
        setLeft(leftBar);

        // 初始化默认面板并刷新
        courseSelectPanel = new CourseSelectPanel(studentId);
        setCenter(courseSelectPanel);
    }

    private void setSelectedButtonStyle(Button button) {
        button.setStyle("-fx-font-family: 'Microsoft YaHei UI'; -fx-font-size: 16px; " +
                "-fx-background-color: #176B3A; -fx-text-fill: white; " +
                "-fx-alignment: center-left; -fx-padding: 0 0 0 56;");
    }

    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-family: 'Microsoft YaHei UI'; -fx-font-size: 16px; " +
                "-fx-background-color: #f4f4f4; -fx-text-fill: black; " +
                "-fx-alignment: center-left;  -fx-padding: 0 0 0 60;");
    }
}