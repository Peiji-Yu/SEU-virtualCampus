package Client.panel.library.admin;

import Client.ClientNetworkHelper;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Client.model.Request;
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
import java.util.regex.Pattern;

public class BorrowBookPanel extends BorderPane {
    private TextField userIdField;
    private TextField uuidField;
    private Label statusLabel;
    private Gson gson;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public BorrowBookPanel() {
        // 创建配置了LocalDate和UUID适配器的Gson实例
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
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
        Label titleLabel = new Label("办理借书");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("办理书籍借阅");
        subtitleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        VBox headtitleBox = new VBox(5, titleLabel, subtitleLabel);
        headtitleBox.setAlignment(Pos.CENTER_LEFT);
        headtitleBox.setPadding(new Insets(0, 0, 20, 0));

        // 创建GridPane来放置表单字段
        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(5);
        formGrid.setAlignment(Pos.CENTER_LEFT);

        // 一卡通号字段
        VBox userIdBox = createFieldWithLabel("一卡通号");
        userIdField = createStyledTextField("请输入一卡通号");
        userIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                userIdField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        userIdBox.getChildren().add(userIdField);
        formGrid.add(userIdBox, 0, 0);

        // UUID字段
        VBox uuidBox = createFieldWithLabel("书籍副本ID");
        uuidField = createStyledTextField("请输入书籍副本UUID");
        uuidBox.getChildren().add(uuidField);
        formGrid.add(uuidBox, 0, 1);

        // 设置列约束：第一列宽度随窗口扩展
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().add(col1);


        // 按钮区域
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        Button submitButton = new Button("办理借阅");
        submitButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        submitButton.setOnAction(e -> processBorrow());

        buttonBox.getChildren().addAll(submitButton);

        // 添加透明占位部件
        // 每个带标签输入框高度大约为: 标签高度(约20px) + 输入框高度(45px) + 间距(8px) = 73px
        Region spacer = new Region();
        spacer.setMinHeight(219);
        spacer.setPrefHeight(219);
        spacer.setStyle("-fx-background-color: transparent;");

        // 将所有组件添加到容器
        container.getChildren().addAll(headtitleBox, formGrid, buttonBox, spacer);

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

    private void processBorrow() {
        String userIdStr = userIdField.getText().trim();
        String uuid = uuidField.getText().trim();

        // 验证表单
        if (userIdStr.isEmpty()) {
            setStatus("请输入一卡通号");
            highlightField(userIdField);
            return;
        }

        if (uuid.isEmpty()) {
            setStatus("请输入书籍副本UUID");
            highlightField(uuidField);
            return;
        }

        if (!UUID_PATTERN.matcher(uuid).matches()) {
            setStatus("请输入有效的UUID格式");
            highlightField(uuidField);
            return;
        }

        try {
            int userId = Integer.parseInt(userIdStr);

            setStatus("办理中...");

            new Thread(() -> {
                try {
                    // 构建借阅请求
                    Map<String, Object> data = new HashMap<>();
                    data.put("uuid", uuid);
                    data.put("userId", userId);

                    Request request = new Request("borrowBook", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);
                    System.out.println("借阅响应: " + response);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Platform.runLater(() -> {
                            setStatus("借阅成功");
                            clearForm();
                        });
                    } else {
                        Platform.runLater(() -> {
                            setStatus("借阅失败: " + responseMap.get("message"));
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        setStatus("通信错误: " + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }).start();
        } catch (NumberFormatException e) {
            setStatus("一卡通号必须是数字");
            highlightField(userIdField);
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
        userIdField.clear();
        uuidField.clear();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}