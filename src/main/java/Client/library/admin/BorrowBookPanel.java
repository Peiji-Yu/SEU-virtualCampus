package Client.library.admin;

import Client.ClientNetworkHelper;
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
import java.util.regex.Pattern;

public class BorrowBookPanel extends VBox {
    private TextField userIdField;
    private TextField uuidField;
    private Button borrowButton;
    private Label statusLabel;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private Gson gson;

    public BorrowBookPanel() {
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
        Label titleLabel = new Label("借书办理");
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

        // 一卡通号字段
        userIdField = createStyledTextField("一卡通号");
        userIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                userIdField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        form.add(createLabel("一卡通号:", labelStyle), 0, 0);
        form.add(userIdField, 1, 0);

        // UUID字段
        uuidField = createStyledTextField("书籍副本UUID");
        form.add(createLabel("书籍UUID:", labelStyle), 0, 1);
        form.add(uuidField, 1, 1);

        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        borrowButton = new Button("办理借阅");
        borrowButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-pref-width: 120px; -fx-pref-height: 35px;");
        borrowButton.setOnAction(e -> processBorrow());

        buttonBox.getChildren().addAll(borrowButton);

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

    private void processBorrow() {
        String userIdStr = userIdField.getText().trim();
        String uuid = uuidField.getText().trim();

        // 验证表单
        if (userIdStr.isEmpty()) {
            setStatus("请输入一卡通号", "error");
            return;
        }

        if (uuid.isEmpty()) {
            setStatus("请输入书籍副本UUID", "error");
            return;
        }

        if (!UUID_PATTERN.matcher(uuid).matches()) {
            setStatus("请输入有效的UUID格式", "error");
            return;
        }

        try {
            int userId = Integer.parseInt(userIdStr);

            setStatus("办理中...", "info");

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
                            setStatus("借阅成功", "success");
                            clearForm();
                        });
                    } else {
                        Platform.runLater(() -> {
                            setStatus("借阅失败: " + responseMap.get("message"), "error");
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        setStatus("通信错误: " + e.getMessage(), "error");
                    });
                    e.printStackTrace();
                }
            }).start();
        } catch (NumberFormatException e) {
            setStatus("一卡通号必须是数字", "error");
        }
    }

    private void clearForm() {
        userIdField.clear();
        uuidField.clear();
    }

    private void setStatus(String message, String type) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            switch (type) {
                case "error":
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    break;
                case "success":
                    statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 14px;");
                    break;
            }
        });
    }
}