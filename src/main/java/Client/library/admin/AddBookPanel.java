package Client.library.admin;

import Client.ClientNetworkHelper;
import Client.library.util.model.BookClass;
import Client.library.util.model.Category;
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
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddBookPanel extends VBox {
    private TextField isbnField, titleField, authorField, publisherField;
    private ComboBox<Category> categoryComboBox;
    private DatePicker publishDatePicker;
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
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setSpacing(20);
        setStyle("-fx-background-color: #f5f7fa;");

        // 标题
        Label titleLabel = new Label("添加书籍");
        titleLabel.setFont(Font.font(24));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 表单容器
        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setPadding(new Insets(25));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0.0, 0.0);");

        // 设置列约束，使第二列可以扩展
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(100);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(col1, col2);

        // 创建标签样式
        String labelStyle = "-fx-font-weight: bold; -fx-font-size: 14px;";

        // ISBN字段
        isbnField = createStyledTextField("ISBN号");
        form.add(createLabel("ISBN:", labelStyle), 0, 0);
        form.add(isbnField, 1, 0);

        // 书名字段
        titleField = createStyledTextField("书名");
        form.add(createLabel("书名:", labelStyle), 0, 1);
        form.add(titleField, 1, 1);

        // 作者字段
        authorField = createStyledTextField("作者");
        form.add(createLabel("作者:", labelStyle), 0, 2);
        form.add(authorField, 1, 2);

        // 出版社字段
        publisherField = createStyledTextField("出版社");
        form.add(createLabel("出版社:", labelStyle), 0, 3);
        form.add(publisherField, 1, 3);

        // 出版日期字段
        Label publishDateLabel = createLabel("出版日期:", labelStyle);
        publishDatePicker = new DatePicker();
        publishDatePicker.setStyle("-fx-font-size: 14px; -fx-pref-height: 35px;");
        form.add(publishDateLabel, 0, 4);
        form.add(publishDatePicker, 1, 4);

        // 类别字段
        Label categoryLabel = new Label("类别:");
        categoryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().addAll(Category.values());
        categoryComboBox.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category.getDescription();
            }

            @Override
            public Category fromString(String string) {
                return null; // 不需要从字符串转换
            }
        });
        categoryComboBox.setStyle("-fx-font-size: 14px; -fx-pref-height: 35px; -fx-background-radius: 5; -fx-border-radius: 5;");
        categoryComboBox.setButtonCell(new ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("选择类别");
                } else {
                    setText(item.getDescription());
                }
            }
        });
        // 设置默认选择第一个类别
        if (!categoryComboBox.getItems().isEmpty()) {
            categoryComboBox.getSelectionModel().selectFirst();
        }

        form.add(categoryLabel, 0, 5);
        form.add(categoryComboBox, 1, 5);

        // 描述字段
        Label descriptionLabel = createLabel("描述:", labelStyle);
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("书籍描述");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; -fx-border-radius: 5;");
        form.add(descriptionLabel, 0, 6);
        form.add(descriptionArea, 1, 6);

        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        Button submitButton = new Button("添加书籍");
        submitButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-pref-width: 120px; -fx-pref-height: 35px;");
        submitButton.setOnAction(e -> addBook());

        Button clearButton = new Button("清空");
        clearButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-pref-width: 120px; -fx-pref-height: 35px;");
        clearButton.setOnAction(e -> clearForm());

        buttonBox.getChildren().addAll(clearButton, submitButton);

        // 状态标签
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));

        // 添加到主容器
        getChildren().addAll(titleLabel, form, buttonBox, statusLabel);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 14px; -fx-pref-height: 35px; -fx-background-radius: 5; -fx-border-radius: 5;");
        return field;
    }

    private Label createLabel(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        return label;
    }

    private void addBook() {
        // 验证表单
        if (isbnField.getText().trim().isEmpty()) {
            setStatus("请输入ISBN号");
            return;
        }
        if (titleField.getText().trim().isEmpty()) {
            setStatus("请输入书名");
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("添加中..."));

                String isbn = isbnField.getText().trim();
                String name = titleField.getText().trim();
                String author = authorField.getText().trim();
                String publisher = publisherField.getText().trim();
                LocalDate publishDate = publishDatePicker.getValue();
                String category = categoryComboBox.getValue().name(); // 获取枚举名称(如"SCIENCE")
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
                } else if (code == 409) { // 假设409表示书籍已存在
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("添加失败");
                        alert.setHeaderText("书籍已存在");
                        alert.setContentText("该ISBN的书籍已存在，请使用修改功能添加副本。");
                        alert.showAndWait();
                        setStatus("添加失败: 书籍已存在");
                    });
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
    }

    private void clearForm() {
        isbnField.clear();
        titleField.clear();
        authorField.clear();
        publisherField.clear();
        publishDatePicker.setValue(null);
        categoryComboBox.getSelectionModel().selectFirst(); // 重置为默认选择
        descriptionArea.clear();
        setStatus("表单已清空");
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}