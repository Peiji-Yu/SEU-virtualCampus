package Client.studentmgmt.student;

import Client.studentmgmt.service.StudentClientService;
import Server.model.student.Student;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.text.SimpleDateFormat;

/**
 * 学生个人学籍查看面板（只读 self 包）。
 * 作者: @Msgo-srAm
 */
public class StudentSelfPanel extends VBox {
    private final String cardNumber;
    private final StudentClientService service = new StudentClientService();
    // 将标题栏提升为类字段，使其它方法（如刷新回调）可访问
    private HBox titleBox;

    public StudentSelfPanel(String cardNumber) {
        this.cardNumber = cardNumber;
        setPadding(new Insets(0));
        setSpacing(0);
        setStyle("-fx-background-color: transparent;");
        init();
    }

    private void init() {
        // 使用滚动面板确保内容适配
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // 不显示滚动条（隐藏水平与垂直滚动条），但允许平移/拖动视图
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);

        VBox contentContainer = new VBox(15);
        contentContainer.setPadding(new Insets(20));
        contentContainer.setStyle("-fx-background-color: #f8fafc;");

        // 标题区域
        titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 10, 0));

        Label title = new Label("我的学籍信息");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        titleBox.getChildren().add(title);
        contentContainer.getChildren().add(titleBox);

        Label loadingLabel = new Label("正在加载学籍信息...");
        loadingLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-font-style: italic;");
        contentContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            try {
                Student stu = service.getSelf(Integer.parseInt(cardNumber));
                Platform.runLater(() -> {
                    contentContainer.getChildren().remove(loadingLabel);
                    if (stu != null) {
                        display(stu, contentContainer);
                    } else {
                        showError("学籍信息获取失败，请稍后重试", contentContainer);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    contentContainer.getChildren().remove(loadingLabel);
                    showError("网络连接失败: " + ex.getMessage(), contentContainer);
                });
            }
        }).start();

        scrollPane.setContent(contentContainer);
        getChildren().add(scrollPane);
    }

    private void showError(String msg, VBox container) {
        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px; -fx-background-color: #fef2f2; " +
                "-fx-padding: 12px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        container.getChildren().add(err);
    }

    private void display(Student s, VBox container) {
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; " +
                "-fx-padding: 25px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);");

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        // 基本信息卡片
        VBox baseInfoCard = createInfoCard("基本信息", "#3b82f6");
        GridPane baseGrid = new GridPane();
        baseGrid.setHgap(20);
        baseGrid.setVgap(10);
        baseGrid.setPadding(new Insets(15, 0, 15, 0));

        // 设置列约束
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(100);
        col1.setPrefWidth(100);

        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setMinWidth(200);

        baseGrid.getColumnConstraints().addAll(col1, col2);

        int r = 0;
        addStyledRow(baseGrid, r++, "姓名:", s.getName());
        addStyledRow(baseGrid, r++, "一卡通号:", String.valueOf(s.getCardNumber()));
        addStyledRow(baseGrid, r++, "学号:", s.getStudentNumber());
        addStyledRow(baseGrid, r++, "身份证号:", s.getIdentity());

        if (s.getGender() != null) {
            addStyledRow(baseGrid, r++, "性别:", s.getGender().getDescription());
        }

        if (s.getBirth() != null) {
            addStyledRow(baseGrid, r++, "出生日期:", df.format(s.getBirth()));
        }

        addStyledRow(baseGrid, r++, "籍贯:", s.getBirthPlace());

        if (s.getPoliticalStat() != null) {
            addStyledRow(baseGrid, r++, "政治面貌:", s.getPoliticalStat().getDescription());
        }

        baseInfoCard.getChildren().add(baseGrid);

        // 分隔线
        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));

        // 学籍信息卡片
        VBox studyInfoCard = createInfoCard("学籍信息", "#10b981");
        GridPane studyGrid = new GridPane();
        studyGrid.setHgap(20);
        studyGrid.setVgap(10);
        studyGrid.setPadding(new Insets(15, 0, 15, 0));

        // 设置相同的列约束
        studyGrid.getColumnConstraints().addAll(col1, col2);

        r = 0;
        addStyledRow(studyGrid, r++, "学院：", s.getSchool());
        addStyledRow(studyGrid, r++, "专业：", s.getMajor());

        if (s.getStatus() != null) {
            addStyledRow(studyGrid, r++, "学籍状态：", s.getStatus().getDescription());
        }

        if (s.getEnrollment() != null) {
            addStyledRow(studyGrid, r++, "入学日期：", df.format(s.getEnrollment()));
        }

        studyInfoCard.getChildren().add(studyGrid);

        // 刷新按钮
        Button refresh = new Button("刷新信息");
        refresh.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed); " +
                "-fx-text-fill: white; -fx-background-radius: 8px; " +
                "-fx-padding: 10px 24px; -fx-cursor: hand;");

        refresh.setOnMouseEntered(e -> refresh.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-background-color: linear-gradient(to right, #4338ca, #6d28d9); " +
                        "-fx-text-fill: white; -fx-background-radius: 8px; " +
                        "-fx-padding: 10px 24px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(79,70,229,0.3), 8, 0, 0, 2);"
        ));

        refresh.setOnMouseExited(e -> refresh.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed); " +
                        "-fx-text-fill: white; -fx-background-radius: 8px; " +
                        "-fx-padding: 10px 24px; -fx-cursor: hand;"
        ));

        refresh.setOnAction(e -> {
            container.getChildren().clear();
            container.getChildren().add(titleBox);
            Label reloadLabel = new Label("正在重新加载学籍信息...");
            reloadLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-font-style: italic;");
            container.getChildren().add(reloadLabel);

            new Thread(() -> {
                try {
                    Student stu = service.getSelf(Integer.parseInt(cardNumber));
                    Platform.runLater(() -> {
                        container.getChildren().remove(reloadLabel);
                        if (stu != null) {
                            display(stu, container);
                        } else {
                            showError("学籍信息获取失败，请稍后重试", container);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        container.getChildren().remove(reloadLabel);
                        showError("网络连接失败: " + ex.getMessage(), container);
                    });
                }
            }).start();
        });

        HBox buttonContainer = new HBox(refresh);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(20, 0, 0, 0));

        mainContainer.getChildren().addAll(baseInfoCard, separator, studyInfoCard, buttonContainer);
        container.getChildren().add(mainContainer);
    }

    private VBox createInfoCard(String titleText, String color) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #ffffff; -fx-padding: 0;");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + "; " +
                "-fx-padding: 0 0 5px 0; -fx-border-color: " + color + "20; -fx-border-width: 0 0 2px 0;");

        card.getChildren().add(title);
        return card;
    }

    private void addStyledRow(GridPane grid, int row, String label, String value) {
        if (value == null) {
            value = "未设置";
        }

        Label labelField = new Label(label);
        labelField.setStyle("-fx-font-weight: 600; -fx-text-fill: #374151; " +
                "-fx-font-size: 14px; -fx-padding: 2px 0;");

        Label valueField = new Label(value);
        valueField.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 14px; -fx-padding: 2px 0; " +
                "-fx-wrap-text: true;");

        grid.add(labelField, 0, row);
        grid.add(valueField, 1, row);
    }
}