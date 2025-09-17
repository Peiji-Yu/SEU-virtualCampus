package Client.coursemgmt.admin;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CourseAdminMainPanel extends BorderPane {
    private CourseAdminPanel courseAdminPanel;
    private TimetableSearchPanel timetablePanel;
    private Button currentSelectedButton;

    public CourseAdminMainPanel() {
        initializeUI();
    }

    private void initializeUI() {
        // 左侧导航栏
        VBox leftBar = new VBox();
        leftBar.setStyle("-fx-background-color: #f4f4f4;"
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.2), 10, 0, 1, 0);");
        leftBar.setPrefWidth(210);

        // 设置说明标签
        Label leftLabel = new Label("课程管理");
        leftLabel.setStyle("-fx-text-fill: #303030; -fx-font-family: 'Microsoft YaHei UI'; " +
                "-fx-font-size: 12px; -fx-alignment: center-left; -fx-padding: 10 0 10 15;");

        // 添加分割线
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator.setMaxWidth(Double.MAX_VALUE);

        // 全部课程按钮
        Button allCoursesButton = new Button("课程管理");
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
                if (courseAdminPanel == null) {
                    courseAdminPanel = new CourseAdminPanel();
                }
                setCenter(courseAdminPanel);
            }
        });

        // 添加分割线
        Region separator1 = new Region();
        separator1.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator1.setMaxWidth(Double.MAX_VALUE);

        // 学生课表按钮
        Button timetableButton = new Button("学生课表");
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
                    timetablePanel = new TimetableSearchPanel();
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
                timetableButton, separator3);
        setLeft(leftBar);

        // 初始化默认面板并刷新
        courseAdminPanel = new CourseAdminPanel();
        setCenter(courseAdminPanel);
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

