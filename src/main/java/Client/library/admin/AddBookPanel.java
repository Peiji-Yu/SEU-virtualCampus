package Client.library.admin;

import Client.ClientNetworkHelper;
import Client.library.model.BookClass;
import Client.library.model.Category;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddBookPanel extends BorderPane {
    private TextField isbnField, titleField, authorField, publisherField;
    private TextField yearField, monthField, dayField;
    private ToggleGroup categoryToggleGroup;
    private TextArea descriptionArea;
    private Label statusLabel;

    private Gson gson;

    public AddBookPanel() {
        // 创建配置了LocalDate和UUID适配器的Gson实例
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        clearForm();
    }

    private void initializeUI() {
        // 设置背景和边距
        setPadding(new Insets(20, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 表单容器 - 放在中心
        VBox formContainer = createFormContainer();
        setCenter(formContainer);

        // 状态标签 - 放在底部
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");
        BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
        BorderPane.setMargin(statusLabel, new Insets(0, 35, 0, 35));
        setBottom(statusLabel);
    }

    private VBox createFormContainer() {
        VBox container = new VBox(5);
        container.setPadding(new Insets(30));
        container.setStyle("-fx-background-color: white;");
        container.setAlignment(Pos.CENTER);

        // 添加标题和说明
        Label titleLabel = new Label("添加书籍");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("添加新的书籍");
        subtitleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        VBox headtitleBox = new VBox(5, titleLabel, subtitleLabel);
        headtitleBox.setAlignment(Pos.CENTER_LEFT);
        headtitleBox.setPadding(new Insets(0, 0, 20, 0));

        // ISBN字段
        VBox isbnBox = createFieldWithLabel("ISBN号");
        isbnField = createStyledTextField("");
        isbnBox.getChildren().add(isbnField);

        // 创建GridPane来放置表单字段
        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(5);
        formGrid.setAlignment(Pos.CENTER_LEFT);

        // 书名和作者
        VBox titleBox = createFieldWithLabel("书名");
        titleField = createStyledTextField("");
        titleBox.getChildren().add(titleField);
        formGrid.add(titleBox, 0, 0);

        VBox authorBox = createFieldWithLabel("作者");
        authorField = createStyledTextField("");
        authorBox.getChildren().add(authorField);
        formGrid.add(authorBox, 1, 0);

        // 出版社和出版日期
        VBox publisherBox = createFieldWithLabel("出版社");
        publisherField = createStyledTextField("");
        publisherBox.getChildren().add(publisherField);
        formGrid.add(publisherBox, 0, 1);

        VBox dateBox = createFieldWithLabel("出版日期");
        HBox dateInputBox = new HBox(10);
        dateInputBox.setAlignment(Pos.CENTER_LEFT);

        yearField = createStyledTextField("年");

        monthField = createStyledTextField("月");

        dayField = createStyledTextField("日");

        dateInputBox.getChildren().addAll(yearField, new Label("-"), monthField, new Label("-"), dayField);
        dateBox.getChildren().add(dateInputBox);
        formGrid.add(dateBox, 1, 1);

        // 设置GridPane列约束，使两列等宽
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        formGrid.getColumnConstraints().addAll(col1, col2);

        // 类别选择 - 使用单选框
        VBox categoryBox = createFieldWithLabel("类别");
        categoryToggleGroup = new ToggleGroup();

        // 创建单选框容器
        FlowPane categoryRadioContainer = new FlowPane();
        categoryRadioContainer.setHgap(15);
        categoryRadioContainer.setVgap(10);
        categoryRadioContainer.setAlignment(Pos.TOP_LEFT);

        // 为每个类别创建单选框
        for (Category category : Category.values()) {
            RadioButton radioButton = new RadioButton(category.getDescription());
            radioButton.setToggleGroup(categoryToggleGroup);
            radioButton.setUserData(category);
            radioButton.setStyle("-fx-font-size: 14px; -fx-text-fill: #495057; " +
                    "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; -fx-padding: 5 10 5 5;");
            categoryRadioContainer.getChildren().add(radioButton);
        }

        // 默认选择第一个类别
        if (!categoryRadioContainer.getChildren().isEmpty()) {
            ((RadioButton) categoryRadioContainer.getChildren().get(0)).setSelected(true);
        }

        // 为类别选择添加边框
        VBox categoryBorderBox = new VBox(categoryRadioContainer);
        categoryBorderBox.setStyle("-fx-border-color: #ced4da; -fx-border-radius: 5; -fx-padding: 10;");
        categoryBox.getChildren().add(categoryBorderBox);

        // 描述字段
        VBox descriptionBox = createFieldWithLabel("描述");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("请输入书籍描述");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(6);

        descriptionArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                "-fx-border-radius: 5;");
        VBox.setVgrow(descriptionArea, Priority.ALWAYS);
        descriptionBox.getChildren().add(descriptionArea);

        // 按钮区域
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        Button submitButton = new Button("添加");
        submitButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        submitButton.setOnAction(e -> addBook());

        buttonBox.getChildren().addAll(submitButton);

        // 将所有组件添加到容器
        container.getChildren().addAll(headtitleBox, isbnBox, formGrid, categoryBox, descriptionBox, buttonBox);

        return container;
    }

    private VBox createFieldWithLabel(String labelText) {
        VBox container = new VBox(8);

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #495057; -fx-font-weight: bold;");

        container.getChildren().add(label);
        return container;
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 16px; -fx-pref-height: 45px; " +
                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent;" +
                "-fx-padding: 0 10px;"
                );
        return field;
    }

    private void addBook() {
        // 验证表单
        if (isbnField.getText().trim().isEmpty()) {
            setStatus("请输入ISBN号");
            highlightField(isbnField);
            return;
        }
        if (titleField.getText().trim().isEmpty()) {
            setStatus("请输入书名");
            highlightField(titleField);
            return;
        }

        // 验证日期
        String yearText = yearField.getText().trim();
        String monthText = monthField.getText().trim();
        String dayText = dayField.getText().trim();

        if (yearText.isEmpty() || monthText.isEmpty() || dayText.isEmpty()) {
            setStatus("请输入完整的出版日期");
            if (yearText.isEmpty()) highlightField(yearField);
            if (monthText.isEmpty()) highlightField(monthField);
            if (dayText.isEmpty()) highlightField(dayField);
            return;
        }

        try {
            int year = Integer.parseInt(yearText);
            int month = Integer.parseInt(monthText);
            int day = Integer.parseInt(dayText);

            // 验证月份和日期的有效性
            if (month < 1 || month > 12) {
                setStatus("月份必须在1-12之间");
                highlightField(monthField);
                return;
            }

            if (day < 1 || day > 31) {
                setStatus("日期必须在1-31之间");
                highlightField(dayField);
                return;
            }

            // 验证特定月份的天数
            if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) {
                setStatus("该月份最多只有30天");
                highlightField(dayField);
                return;
            }

            // 验证闰年二月
            if (month == 2) {
                boolean isLeapYear = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
                if (isLeapYear && day > 29) {
                    setStatus("闰年二月最多只有29天");
                    highlightField(dayField);
                    return;
                } else if (!isLeapYear && day > 28) {
                    setStatus("平年二月最多只有28天");
                    highlightField(dayField);
                    return;
                }
            }

            // 验证年份合理性（假设书籍出版年份不能早于1000年）
            if (year < 1000 || year > LocalDate.now().getYear()) {
                setStatus("年份不合理，请检查");
                highlightField(yearField);
                return;
            }

            // 所有验证通过，继续添加书籍
            final LocalDate publishDate = LocalDate.of(year, month, day);

            new Thread(() -> {
                try {
                    Platform.runLater(() -> setStatus("添加中..."));

                    String isbn = isbnField.getText().trim();
                    String name = titleField.getText().trim();
                    String author = authorField.getText().trim();
                    String publisher = publisherField.getText().trim();

                    // 获取选中的类别
                    RadioButton selectedRadio = (RadioButton) categoryToggleGroup.getSelectedToggle();
                    Category selectedCategory = (Category) selectedRadio.getUserData();
                    String category = selectedCategory.name();

                    String description = descriptionArea.getText().trim();

                    // 构建书籍对象
                    BookClass book = new BookClass();
                    book.setIsbn(isbn);
                    book.setName(name);
                    book.setAuthor(author);
                    book.setPublisher(publisher);
                    book.setPublishDate(publishDate);
                    book.setCategory(category);
                    book.setInventory(0); // 默认库存量为0
                    book.setDescription(description);

                    // 构建添加请求
                    Map<String, Object> data = new HashMap<>();
                    data.put("book", book);
                    Request request = new Request("addBook", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);
                    System.out.println("添加响应: " + response);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Platform.runLater(() -> {
                            setStatus("书籍添加成功");
                            clearForm();
                        });
//                    } else if (code == 400) { // 假设400表示书籍已存在
//                        Platform.runLater(() -> {
//                            Alert alert = new Alert(Alert.AlertType.WARNING);
//                            alert.setTitle("添加失败");
//                            alert.setHeaderText("书籍已存在");
//                            alert.setContentText("该ISBN的书籍已存在，请使用修改功能添加副本。");
//                            alert.showAndWait();
//                            setStatus("添加失败: 书籍已存在");
//                        });
                    } else {
                        Platform.runLater(() ->
                                setStatus("添加失败: " + responseMap.get("message")));
                    }
                } catch (Exception e) {
                    Platform.runLater(() ->
                            setStatus("添加错误: " + e.getMessage()));
                    e.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException e) {
            setStatus("出版日期必须为数字");
            if (!yearText.matches("\\d+")) highlightField(yearField);
            if (!monthText.matches("\\d+")) highlightField(monthField);
            if (!dayText.matches("\\d+")) highlightField(dayField);
        }
    }

    private void highlightField(TextField field) {
        field.setStyle(field.getStyle() + " -fx-border-color: #dc3545; -fx-border-width: 2px; " +
                "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; ");
        // 5秒后移除高亮
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    field.setStyle("-fx-font-size: 16px; -fx-pref-height: 45px; " +
                            "-fx-background-radius: 5; -fx-border-radius: 5; " +
                            "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent; " +
                            "-fx-padding: 0 10px;");
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void clearForm() {
        isbnField.clear();
        titleField.clear();
        authorField.clear();
        publisherField.clear();
        yearField.clear();
        monthField.clear();
        dayField.clear();

        // 重置类别选择为第一个选项
        if (!categoryToggleGroup.getToggles().isEmpty()) {
            categoryToggleGroup.selectToggle(categoryToggleGroup.getToggles().get(0));
        }

        descriptionArea.clear();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}