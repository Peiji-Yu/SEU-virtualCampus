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
 * 学生个人学籍查看面板（只读 self 包）。
 * 作者: @Msgo-srAm
 */
public class StudentSelfPanel extends VBox {
    private final String cardNumber;
    private final StudentClientService service = new StudentClientService();
    // 将标题栏提升为类字段，使其它方法（如刷新回调）可访问
    private HBox titleBox;
    // 可访问的内容容器，用于刷新时重建/替换内容
    private VBox contentContainer;
    // 刷新按钮（类字段以便在刷新过程中禁用/启用）
    private Button refreshButton;

    public StudentSelfPanel(String cardNumber) {
        this.cardNumber = cardNumber;
        // 让子节点水平方向填充
        this.setFillWidth(true);
        // 告诉父容器可以垂直方向拉伸
        VBox.setVgrow(this, Priority.ALWAYS);
        setPadding(new Insets(0));
        setSpacing(0);
        setStyle("-fx-background-color: #F4F4F4;");
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

        // 将 contentContainer 提升为类字段以便刷新使用
        contentContainer = new VBox(20);
        contentContainer.setPadding(new Insets(25));
        contentContainer.setStyle("-fx-background-color: transparent;");

        // 标题区域
        titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 15, 0));

        // 刷新按钮（放在标题左侧）
        refreshButton = createRefreshButton();

        Label title = new Label("我的学籍信息");
        title.setStyle("-fx-font-size: 40px; -fx-font-weight: 800; -fx-text-fill: #1e293b; " +
                "-fx-font-family: 'Microsoft YaHei UI', 'Segoe UI', 'PingFang SC', sans-serif;");

        titleBox.getChildren().addAll(title, refreshButton);
        contentContainer.getChildren().add(titleBox);

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
        errIcon.setStyle("-fx-font-size: 48px;");

        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 18px; -fx-background-color: #fef2f2; " +
                "-fx-padding: 16px; -fx-background-radius: 12px; -fx-border-radius: 12px; " +
                "-fx-border-color: #fecaca; -fx-border-width: 1px; " +
                "-fx-font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;");

        errorContainer.getChildren().addAll(errIcon, err);
        container.getChildren().add(errorContainer);
    }

    private void display(Student s, VBox container) {
        VBox mainContainer = new VBox(25);
        mainContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16px; " +
                "-fx-padding: 30px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 20, 0, 0, 4);");

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        // 创建左右布局的容器
        HBox infoLayout = new HBox(30);
        infoLayout.setAlignment(Pos.TOP_LEFT);
        infoLayout.setStyle("-fx-background-color: transparent;");

        // 基本信息卡片（左侧）
        VBox baseInfoCard = createInfoCard("基本信息", "#3b82f6");
        GridPane baseGrid = new GridPane();
        baseGrid.setHgap(28);
        baseGrid.setVgap(12);
        baseGrid.setPadding(new Insets(20, 0, 20, 0));

        // 设置列约束
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(120);
        col1.setPrefWidth(120);

        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setMinWidth(250);

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

        // 学籍信息卡片（右侧）
        VBox studyInfoCard = createInfoCard("学籍信息", "#10b981");
        GridPane studyGrid = new GridPane();
        studyGrid.setHgap(28);
        studyGrid.setVgap(12);
        studyGrid.setPadding(new Insets(20, 0, 20, 0));

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

        // 将两个卡片添加到水平布局
        infoLayout.getChildren().addAll(baseInfoCard, studyInfoCard);

        // 已移除刷新按钮，界面为只读展示

        mainContainer.getChildren().add(infoLayout);
        container.getChildren().add(mainContainer);
    }

    private VBox createInfoCard(String titleText, String color) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: #ffffff; -fx-padding: 0;");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: " + color + "; " +
                "-fx-padding: 0 0 8px 0; -fx-border-color: " + color + "30; -fx-border-width: 0 0 3px 0; " +
                "-fx-font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;");

        card.getChildren().add(title);
        return card;
    }

    private void addStyledRow(GridPane grid, int row, String label, String value) {
        if (value == null) {
            value = "未设置";
        }

        Label labelField = new Label(label);
        labelField.setStyle("-fx-font-weight: 600; -fx-text-fill: #374151; " +
                "-fx-font-size: 18px; -fx-padding: 6px 0; " +
                "-fx-font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;");

        Label valueField = new Label(value);
        valueField.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 18px; -fx-padding: 6px 0; " +
                "-fx-wrap-text: true; -fx-font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;");

        grid.add(labelField, 0, row);
        grid.add(valueField, 1, row);
    }

    // 新增：创建加载占位节点（供初次加载和刷新时使用）
    private VBox createLoadingContainer() {
        VBox loadingContainer = new VBox(10);
        loadingContainer.setAlignment(Pos.CENTER);
        loadingContainer.setPadding(new Insets(40, 0, 40, 0));

        Label loadingLabel = new Label("正在加载学籍信息...");
        loadingLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 20px; -fx-font-style: italic; " +
                "-fx-font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;");

        HBox loadingDots = new HBox(6);
        loadingDots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 28px;");
            loadingDots.getChildren().add(dot);
        }

        loadingContainer.getChildren().addAll(loadingLabel, loadingDots);
        return loadingContainer;
    }

    // 新增：刷新逻辑（控制按钮启用/禁用并重新拉取数据）
    private void doRefresh() {
        if (contentContainer == null) {
            return;
        }
        // 防止重复点击
        refreshButton.setDisable(true);

        // 清除标题之后的所有内容，展示加载占位
        // titleBox 为 contentContainer 的首个子节点
        Platform.runLater(() -> {
            // 移除除标题以外的所有子节点
            contentContainer.getChildren().removeIf(node -> node != titleBox);
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

    // 新增：创建带图标的刷新按钮，若资源加载失败回退到文本图标
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