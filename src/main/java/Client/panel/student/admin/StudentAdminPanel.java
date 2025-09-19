package Client.panel.student.admin;

import Client.panel.student.service.StudentClientService;
import Client.util.UIUtil;
import Client.model.student.Student;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

/**
 * 管理员学籍管理面板 (admin 包)
 * 采用现代化卡片式设计，参考书籍管理界面风格
 * 作者: @Msgo-srAm
 */
public class StudentAdminPanel extends BorderPane {
    private final StudentClientService service = new StudentClientService();
    private static final String PRIMARY_COLOR = "#176b3a";
    private static final String PRIMARY_HOVER_COLOR = "#1e7d46";
    private static final String TEXT_COLOR = "#2c3e50";

    private TextField searchField;
    private ComboBox<String> searchTypeCombo;
    private CheckBox fuzzyCheckBox;
    private VBox studentsContainer;
    private Label resultsLabel;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public StudentAdminPanel() {
        initializeUI();
        performSearch();
    }

    private void initializeUI() {
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("学籍管理");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查找和管理学生信息");
        subtitleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        VBox headtitleBox = new VBox(5, titleLabel, subtitleLabel);
        headtitleBox.setAlignment(Pos.CENTER_LEFT);
        headtitleBox.setPadding(new Insets(0, 0, 20, 0));

        // 搜索区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(0, 0, 5, 0));
        searchBox.setStyle("-fx-background-color: white;");

        // 搜索框和按钮在同一行
        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        // 搜索类型选择
        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("姓名", "学号", "一卡通号");
        searchTypeCombo.setValue("姓名");
        searchTypeCombo.setStyle("-fx-font-size: 14px; -fx-pref-width: 100px; -fx-pref-height: 45px; " +
                        "-fx-background-radius: 4; -fx-border-radius: 4;"+
                        " -fx-border-color: #1D8C4F; -fx-border-width: 1; -fx-background-color: #ffffff;"+
                        " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");

        searchField = createStyledTextField("输入搜索关键词");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("搜索");
        searchButton.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> performSearch());

        searchRow.getChildren().addAll(searchTypeCombo, searchField, searchButton);

        // 搜索选项行
        HBox optionsRow = new HBox(10);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        fuzzyCheckBox = new CheckBox("模糊搜索");
        fuzzyCheckBox.setStyle("-fx-font-size: 14px; -fx-text-fill: #3b3e45;" +
                "-fx-focus-color: " + PRIMARY_COLOR + "; -fx-faint-focus-color: transparent;");

        optionsRow.getChildren().addAll(fuzzyCheckBox);

        // 结果标签
        resultsLabel = new Label("找到 0 名学生");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(headtitleBox, searchRow, optionsRow, resultsLabel);
        topContainer.getChildren().addAll(searchBox);
        setTop(topContainer);

        // 中心学生展示区域
        studentsContainer = new VBox(8); // 进一步减少垂直间距
        studentsContainer.setPadding(new Insets(0, 28, 5, 28));

        ScrollPane scrollPane = new ScrollPane(studentsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: white;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // 添加学生按钮（固定在右下角）
        Button addButton = new Button("+");
        addButton.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px; " +
                "-fx-min-width: 50px; -fx-min-height: 50px; -fx-background-radius: 25;");
        addButton.setOnAction(e -> StudentEditDialog.open(null, this::performSearch, service));

        StackPane.setMargin(addButton, new Insets(0, 20, 20, 0));
        StackPane.setAlignment(addButton, Pos.BOTTOM_RIGHT);

        StackPane contentPane = new StackPane(scrollPane, addButton);
        setCenter(contentPane);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 16px; -fx-pref-height: 45px; " +
                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-focus-color: " + PRIMARY_COLOR + "; -fx-faint-focus-color: transparent;" +
                "-fx-padding: 0 10px;");
        return field;
    }

    private void performSearch() {
        String searchText = searchField.getText().trim();
        boolean fuzzy = fuzzyCheckBox.isSelected();
        String searchType = searchTypeCombo.getValue();

        // 将中文搜索类型转换为服务层需要的参数
        String searchParam;
        switch (searchType) {
            case "学号":
                searchParam = "byStudentNumber";
                break;
            case "一卡通号":
                searchParam = "byCardNumber";
                break;
            default: // 姓名
                searchParam = "byName";
                break;
        }

        new Thread(() -> {
            try {
                List<Student> students = service.searchStudents(searchParam, searchText, fuzzy);
                Platform.runLater(() -> displayStudents(students));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "搜索失败", e.getMessage()));
            }
        }).start();
    }

    private void displayStudents(List<Student> students) {
        studentsContainer.getChildren().clear();

        if (students.isEmpty()) {
            resultsLabel.setText("找到 0 名学生");
            Label noResultsLabel = new Label("没有找到符合条件的学生");
            noResultsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-padding: 40px;");
            noResultsLabel.setAlignment(Pos.CENTER);
            studentsContainer.getChildren().add(noResultsLabel);
            return;
        }

        for (Student student : students) {
            StudentCard card = new StudentCard(student);
            studentsContainer.getChildren().add(card);
        }

        resultsLabel.setText("找到 " + students.size() + " 名学生");
    }

    // 学生信息卡片
    private class StudentCard extends VBox {
        private final Student student;

        public StudentCard(Student student) {
            this.student = student;
            initializeUI();
        }

        private void initializeUI() {
            setPadding(new Insets(10)); // 进一步减少内边距
            setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
            setSpacing(6); // 进一步减少组件间距

            // 学生基本信息区域
            HBox summaryBox = new HBox();
            summaryBox.setAlignment(Pos.CENTER_LEFT);
            summaryBox.setSpacing(10); // 进一步减少间距

            // 学生基本信息
            VBox infoBox = new VBox(3); // 进一步减少垂直间距
            infoBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label nameLabel = new Label(student.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;"); // 增大姓名字体

            Label idLabel = new Label("学号: " + student.getStudentNumber());
            idLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

            infoBox.getChildren().addAll(nameLabel, idLabel);

            // 学生状态信息
            VBox statusBox = new VBox(3); // 进一步减少垂直间距
            statusBox.setAlignment(Pos.CENTER_RIGHT);

            Label cardLabel = new Label("一卡通: " + student.getCardNumber());
            cardLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

            String statusText = student.getStatus() != null ?
                    student.getStatus().getDescription() : "未设置";
            Label statusLabel = new Label("状态: " + statusText);
            statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

            statusBox.getChildren().addAll(cardLabel, statusLabel);

            summaryBox.getChildren().addAll(infoBox, statusBox);

            // 详细信息区域
            VBox detailBox = new VBox(6); // 进一步减少垂直间距
            detailBox.setPadding(new Insets(6, 0, 0, 0)); // 进一步减少内边距

            // 使用两列网格布局展示详细信息
            GridPane detailsGrid = new GridPane();
            detailsGrid.setHgap(10); // 进一步减少水平间距
            detailsGrid.setVgap(4);  // 进一步减少垂直间距

            // 设置列约束
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPrefWidth(110);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPrefWidth(160);
            ColumnConstraints col3 = new ColumnConstraints();
            col3.setPrefWidth(110);
            ColumnConstraints col4 = new ColumnConstraints();
            col4.setPrefWidth(160);
            detailsGrid.getColumnConstraints().addAll(col1, col2, col3, col4);

            // 添加详细信息
            int row = 0;
            addDetailRow(detailsGrid, row++, "身份证号", student.getIdentity());
            addDetailRow(detailsGrid, row++, "性别", student.getGender() != null ? student.getGender().getDescription() : "未设置");

            // 修复日期显示问题
            String birthDate = student.getBirth() != null ? dateFormat.format(student.getBirth()) : "未设置";
            addDetailRow(detailsGrid, row++, "出生日期", birthDate);

            String enrollDate = student.getEnrollment() != null ? dateFormat.format(student.getEnrollment()) : "未设置";
            addDetailRow(detailsGrid, row++, "入学日期", enrollDate);

            addDetailRow(detailsGrid, row++, "籍贯", student.getBirthPlace());
            addDetailRow(detailsGrid, row++, "政治面貌", student.getPoliticalStat() != null ? student.getPoliticalStat().getDescription() : "未设置");
            addDetailRow(detailsGrid, row++, "学院", student.getSchool());
            addDetailRow(detailsGrid, row++, "专业", student.getMajor());

            detailBox.getChildren().add(detailsGrid);

            // 操作按钮区域
            HBox actionButtons = new HBox(6); // 进一步减少按钮间距
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            actionButtons.setPadding(new Insets(6, 0, 0, 0)); // 进一步减少内边距

            // 修改按钮
            Button editButton = new Button("修改");
            editButton.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-pref-width: 70px;");
            editButton.setOnAction(e -> StudentEditDialog.open(student, StudentAdminPanel.this::performSearch, service));

            // 删除按钮
            Button deleteButton = new Button("删除");
            deleteButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 13px; -fx-pref-width: 70px;");
            deleteButton.setOnAction(e -> confirmAndDelete(student));

            actionButtons.getChildren().addAll(editButton, deleteButton);

            getChildren().addAll(summaryBox, detailBox, actionButtons);
        }

        private void addDetailRow(GridPane grid, int row, String label, String value) {
            if (value == null || value.isEmpty()) {
                value = "未设置";
            }

            Label labelText = new Label(label + ":");
            labelText.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label valueText = new Label(value);
            valueText.setStyle("-fx-font-size: 13px;");

            int col = (row % 2) * 2; // 每行两个字段，交替位置

            grid.add(labelText, col, row);
            grid.add(valueText, col + 1, row);
        }

        private void confirmAndDelete(Student student) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("确认删除");
            confirmDialog.setHeaderText("确认删除学生");
            confirmDialog.setContentText("确定要删除学生「" + student.getName() + "」吗？此操作不可撤销。");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        service.deleteStudent(student.getCardNumber());
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "删除成功", "学生信息已成功删除");
                            performSearch();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                showAlert(Alert.AlertType.ERROR, "删除失败", e.getMessage()));
                    }
                }).start();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        UIUtil.applyLogoToAlert(alert);
        alert.showAndWait();
    }
}