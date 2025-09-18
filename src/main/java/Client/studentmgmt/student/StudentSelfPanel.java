package Client.studentmgmt.student;

import Client.studentmgmt.service.StudentClientService;
import Server.model.student.Student;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.text.SimpleDateFormat;
import java.io.InputStream;

/**
 * 现代化学生学籍查看面板（只读 self 包）
 * 采用主题色 #176B3A 和简洁现代设计
 */
public class StudentSelfPanel extends VBox {
    private final String cardNumber;
    private final StudentClientService service = new StudentClientService();
    private VBox contentContainer;
    private Button refreshButton;
    private VBox titleContainer; // 添加标题容器的引用

    public StudentSelfPanel(String cardNumber) {
        this.cardNumber = cardNumber;
        this.setFillWidth(true);
        VBox.setVgrow(this, Priority.ALWAYS);
        setPadding(new Insets(0));
        setSpacing(0);
        setStyle("-fx-background-color: #f4f4f4;");
        init();
    }

    private void init() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);

        contentContainer = new VBox();
        contentContainer.setPadding(new Insets(25, 25, 25, 25));
        contentContainer.setStyle("-fx-background-color: transparent;");
        contentContainer.setAlignment(Pos.TOP_CENTER); // 设置内容容器居中

        // 标题区域
        titleContainer = new VBox(8); // 初始化标题容器
        titleContainer.setAlignment(Pos.CENTER_LEFT);
        titleContainer.setPadding(new Insets(0, 0, 40, 0)); // 增加标题与卡片之间的距离
        titleContainer.setMaxWidth(1325); // 设置标题最大宽度与卡片一致

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("我的学籍信息");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        refreshButton = createRefreshButton();

        titleBox.getChildren().addAll(title, refreshButton);

        // 添加副标题
        Label subtitle = new Label("详细介绍个人基本信息与学籍信息");
        subtitle.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        titleContainer.getChildren().addAll(titleBox, subtitle);
        contentContainer.getChildren().add(titleContainer);

        // 创建加载动画容器
        VBox loadingContainer = createLoadingContainer();
        contentContainer.getChildren().add(loadingContainer);

        new Thread(() -> {
            try {
                Student stu = service.getSelf(Integer.parseInt(cardNumber));
                Platform.runLater(() -> {
                    contentContainer.getChildren().remove(loadingContainer);
                    if (stu != null) {
                        display(stu, contentContainer);
                    } else {
                        showError("学籍信息获取失败，请稍后重试", contentContainer);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    contentContainer.getChildren().remove(loadingContainer);
                    showError("网络连接失败: " + ex.getMessage(), contentContainer);
                });
            }
        }).start();

        scrollPane.setContent(contentContainer);
        getChildren().add(scrollPane);
    }

    private void showError(String msg, VBox container) {
        VBox errorContainer = new VBox(10);
        errorContainer.setAlignment(Pos.CENTER);
        errorContainer.setPadding(new Insets(40, 0, 40, 0));

        Label errIcon = new Label("⚠️");
        errIcon.setStyle("-fx-font-size: 36px;");

        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: #721c24; -fx-font-size: 16px; -fx-background-color: #f8d7da; " +
                "-fx-padding: 12px; -fx-background-radius: 5px; -fx-border-radius: 5px; " +
                "-fx-border-color: #f5c6cb; -fx-border-width: 1px;");

        errorContainer.getChildren().addAll(errIcon, err);
        container.getChildren().add(errorContainer);
    }

    private void display(Student s, VBox container) {
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8px; " +
                "-fx-padding: 25px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        mainContainer.setMaxWidth(1325); // 增大卡片宽度，从900调整到1100

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        // 创建左右布局的容器
        HBox infoLayout = new HBox(30);
        infoLayout.setAlignment(Pos.TOP_LEFT);

        // 基本信息卡片（左侧）
        VBox baseInfoCard = createInfoCard("基本信息");
        GridPane baseGrid = new GridPane();
        baseGrid.setHgap(20);
        baseGrid.setVgap(10);
        baseGrid.setPadding(new Insets(15, 0, 15, 0));

        // 设置列约束
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(100);

        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setMinWidth(200);

        baseGrid.getColumnConstraints().addAll(col1, col2);

        int r = 0;
        addStyledRow(baseGrid, r, "姓名:", s.getName()); r += 1;
        addStyledRow(baseGrid, r, "一卡通号:", String.valueOf(s.getCardNumber())); r += 1;
        addStyledRow(baseGrid, r, "学号:", s.getStudentNumber()); r += 1;
        addStyledRow(baseGrid, r, "身份证号:", s.getIdentity()); r += 1;

        if (s.getGender() != null) {
            addStyledRow(baseGrid, r, "性别:", s.getGender().getDescription()); r += 1;
        }

        if (s.getBirth() != null) {
            addStyledRow(baseGrid, r, "出生日期:", df.format(s.getBirth())); r += 1;
        }

        addStyledRow(baseGrid, r, "籍贯:", s.getBirthPlace()); r += 1;

        if (s.getPoliticalStat() != null) {
            addStyledRow(baseGrid, r, "政治面貌:", s.getPoliticalStat().getDescription());
        }

        baseInfoCard.getChildren().add(baseGrid);
        HBox.setHgrow(baseInfoCard, Priority.ALWAYS);

        // 添加分隔线
        VBox separator = new VBox();
        separator.setStyle("-fx-background-color: #e9ecef; -fx-min-width: 1px; -fx-max-width: 1px; -fx-pref-height: 180px;");
        separator.setAlignment(Pos.CENTER);

        // 学籍信息卡片（右侧）
        VBox studyInfoCard = createInfoCard("学籍信息");
        GridPane studyGrid = new GridPane();
        studyGrid.setHgap(20);
        studyGrid.setVgap(10);
        studyGrid.setPadding(new Insets(15, 0, 15, 0));

        // 设置相同的列约束
        studyGrid.getColumnConstraints().addAll(col1, col2);

        r = 0;
        addStyledRow(studyGrid, r, "学院：", s.getSchool()); r += 1;
        addStyledRow(studyGrid, r, "专业：", s.getMajor()); r += 1;

        if (s.getStatus() != null) {
            addStyledRow(studyGrid, r, "学籍状态：", s.getStatus().getDescription()); r += 1;
        }

        if (s.getEnrollment() != null) {
            addStyledRow(studyGrid, r, "入学日期：", df.format(s.getEnrollment()));
        }

        studyInfoCard.getChildren().add(studyGrid);
        HBox.setHgrow(studyInfoCard, Priority.ALWAYS);

        // 将两个卡片和分隔线添加到水平布局
        infoLayout.getChildren().addAll(baseInfoCard, separator, studyInfoCard);
        mainContainer.getChildren().add(infoLayout);
        container.getChildren().add(mainContainer);
        baseInfoCard.setPrefHeight(450);
        studyInfoCard.setPrefHeight(450);
    }

    private VBox createInfoCard(String titleText) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #ffffff; -fx-padding: 0;");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #176B3A; " +
                "-fx-padding: 0 0 8px 0; -fx-border-color: #176B3A; -fx-border-width: 0 0 2px 0;");

        card.getChildren().add(title);
        return card;
    }

    private void addStyledRow(GridPane grid, int row, String label, String value) {
        if (value == null) {
            value = "未设置";
        }

        Label labelField = new Label(label);
        labelField.setStyle("-fx-font-weight: 600; -fx-text-fill: #495057; " +
                "-fx-font-size: 16px; -fx-padding: 4px 0;");

        Label valueField = new Label(value);
        valueField.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 16px; -fx-padding: 4px 0; " +
                "-fx-wrap-text: true;");

        grid.add(labelField, 0, row);
        grid.add(valueField, 1, row);
    }

    private VBox createLoadingContainer() {
        VBox loadingContainer = new VBox(10);
        loadingContainer.setAlignment(Pos.CENTER);
        loadingContainer.setPadding(new Insets(40, 0, 40, 0));

        Label loadingLabel = new Label("正在加载学籍信息...");
        loadingLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 16px;");

        HBox loadingDots = new HBox(6);
        loadingDots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #176B3A; -fx-font-size: 24px;");
            loadingDots.getChildren().add(dot);
        }

        loadingContainer.getChildren().addAll(loadingLabel, loadingDots);
        return loadingContainer;
    }

    private void doRefresh() {
        if (contentContainer == null) {
            return;
        }

        refreshButton.setDisable(true);

        Platform.runLater(() -> {
            // 移除除标题容器以外的所有子节点
            contentContainer.getChildren().removeIf(node -> node != titleContainer);

            VBox loading = createLoadingContainer();
            contentContainer.getChildren().add(loading);

            new Thread(() -> {
                try {
                    Student stu = service.getSelf(Integer.parseInt(cardNumber));
                    Platform.runLater(() -> {
                        contentContainer.getChildren().remove(loading);
                        if (stu != null) {
                            display(stu, contentContainer);
                        } else {
                            showError("学籍信息获取失败，请稍后重试", contentContainer);
                        }
                        refreshButton.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        contentContainer.getChildren().remove(loading);
                        showError("网络连接失败: " + ex.getMessage(), contentContainer);
                        refreshButton.setDisable(false);
                    });
                }
            }).start();
        });
    }

    private Button createRefreshButton() {
        Button btn;
        Image img = null;
        try {
            InputStream is = getClass().getResourceAsStream("/Image/刷新.png");
            if (is != null) {
                img = new Image(is);
            }
        } catch (Exception ignored) {
        }

        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(22);
            iv.setFitHeight(22);
            iv.setPreserveRatio(true);
            btn = new Button();
            btn.setGraphic(iv);
        } else {
            // 资源未找到时回退到原文字图标
            btn = new Button("🔄");
        }

        btn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-text-fill: #3b82f6; " +
                "-fx-cursor: hand; -fx-padding: 6 10 6 10; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        btn.setTooltip(new Tooltip("刷新学籍信息"));
        btn.setOnAction(evt -> doRefresh());
        return btn;
    }
}