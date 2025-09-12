package Client.library.admin;

import Client.ClientNetworkHelper;
import Client.library.util.model.Book;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.time.LocalDate;
import java.util.*;

public class AddBookPanel extends VBox {
    private TextField isbnField;
    private TextField titleField;
    private TextField authorField;
    private TextField publisherField;
    private DatePicker publishDatePicker;
    private TextArea descriptionArea;
    private TextField categoryField;
    private TextField locationField;
    private TextField uuidField;
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
        setPadding(new Insets(15));
        setSpacing(15);

        Label titleLabel = new Label("添加书籍");
        titleLabel.setFont(Font.font(20));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        // 表单区域
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10));
        formGrid.setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 8;");

        // 书籍信息字段
        Label isbnLabel = new Label("ISBN:");
        isbnLabel.setStyle("-fx-font-weight: bold;");
        isbnField = new TextField();
        formGrid.add(isbnLabel, 0, 0);
        formGrid.add(isbnField, 1, 0);

        Label titleLabelForm = new Label("书名:");
        titleLabelForm.setStyle("-fx-font-weight: bold;");
        titleField = new TextField();
        formGrid.add(titleLabelForm, 0, 1);
        formGrid.add(titleField, 1, 1);

        Label authorLabel = new Label("作者:");
        authorLabel.setStyle("-fx-font-weight: bold;");
        authorField = new TextField();
        formGrid.add(authorLabel, 0, 2);
        formGrid.add(authorField, 1, 2);

        Label publisherLabel = new Label("出版社:");
        publisherLabel.setStyle("-fx-font-weight: bold;");
        publisherField = new TextField();
        formGrid.add(publisherLabel, 0, 3);
        formGrid.add(publisherField, 1, 3);

        Label publishDateLabel = new Label("出版日期:");
        publishDateLabel.setStyle("-fx-font-weight: bold;");
        publishDatePicker = new DatePicker();
        formGrid.add(publishDateLabel, 0, 4);
        formGrid.add(publishDatePicker, 1, 4);

        Label categoryLabel = new Label("类别:");
        categoryLabel.setStyle("-fx-font-weight: bold;");
        categoryField = new TextField();
        formGrid.add(categoryLabel, 0, 5);
        formGrid.add(categoryField, 1, 5);

        Label descriptionLabel = new Label("简介:");
        descriptionLabel.setStyle("-fx-font-weight: bold;");
        descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        formGrid.add(descriptionLabel, 0, 6);
        formGrid.add(descriptionArea, 1, 6);

        // 副本信息
        Label locationLabel = new Label("馆藏位置:");
        locationLabel.setStyle("-fx-font-weight: bold;");
        locationField = new TextField();
        formGrid.add(locationLabel, 0, 7);
        formGrid.add(locationField, 1, 7);

        Label uuidLabel = new Label("副本UUID:");
        uuidLabel.setStyle("-fx-font-weight: bold;");
        uuidField = new TextField();
        uuidField.setText(UUID.randomUUID().toString());
        uuidField.setEditable(false);
        Button generateUuidButton = new Button("生成新UUID");
        generateUuidButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
        generateUuidButton.setOnAction(e -> uuidField.setText(UUID.randomUUID().toString()));

        HBox uuidBox = new HBox(10, uuidField, generateUuidButton);
        uuidBox.setAlignment(Pos.CENTER_LEFT);

        formGrid.add(uuidLabel, 0, 8);
        formGrid.add(uuidBox, 1, 8);

        // 按钮区域
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button addButton = new Button("添加书籍");
        addButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        addButton.setPrefWidth(120);
        addButton.setPrefHeight(40);
        addButton.setOnAction(e -> addBook());

        Button clearButton = new Button("清空");
        clearButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        clearButton.setPrefWidth(80);
        clearButton.setPrefHeight(40);
        clearButton.setOnAction(e -> clearForm());

        buttonBox.getChildren().addAll(addButton, clearButton);

        // 状态标签
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        getChildren().addAll(titleLabel, formGrid, buttonBox, statusLabel);
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
        if (locationField.getText().trim().isEmpty()) {
            setStatus("请输入馆藏位置");
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("添加中..."));

                // 构建书籍对象
                Book book = new Book();
                book.setIsbn(isbnField.getText().trim());
                book.setTitle(titleField.getText().trim());
                book.setAuthor(authorField.getText().trim());
                book.setPublisher(publisherField.getText().trim());
                book.setPublishDate(publishDatePicker.getValue());
                book.setDescription(descriptionArea.getText().trim());
                book.setCategory(categoryField.getText().trim());
                book.setTotalCopies(1); // 初始库存为1
                book.setAvailableCopies(1);

                // 构建副本对象
                Book.BookCopy item = new Book.BookCopy();
                item.setCopyId(UUID.fromString(uuidField.getText().trim()));
                item.setLocation(locationField.getText().trim());
                item.setStatus("在馆");
                List<Book.BookCopy> copies = new ArrayList<>();
                copies.add(item);
                book.setCopies(copies);

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
                        setStatus("添加成功");
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
        descriptionArea.clear();
        categoryField.clear();
        locationField.clear();
        uuidField.setText(UUID.randomUUID().toString());
        setStatus("表单已清空");
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}