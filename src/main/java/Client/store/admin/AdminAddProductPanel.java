package Client.store.admin;

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

public class AdminAddProductPanel extends VBox {
    private TextField nameField, priceField, categoryField, stockField,
            barcodeField, pictureField, uuidField;
    private TextArea descriptionField; // 更改为TextArea以支持多行文本
    private Button submitButton, clearButton;
    private Label statusLabel;

    private Gson gson;

    public AdminAddProductPanel() {
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
        Label titleLabel = new Label("添加商品");
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
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPrefWidth(120);
        form.getColumnConstraints().addAll(col1, col2, col3);

        // 商品UUID字段
        Label uuidLabel = new Label("商品UUID:");
        uuidLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        uuidField = new TextField();
        uuidField.setText(UUID.randomUUID().toString());
        uuidField.setEditable(false);
        uuidField.setStyle("-fx-font-size: 14px; -fx-pref-height: 35px;");

        Button generateUuidButton = new Button("生成新UUID");
        generateUuidButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-pref-height: 30px;");
        generateUuidButton.setOnAction(e -> uuidField.setText(UUID.randomUUID().toString()));

        form.add(uuidLabel, 0, 0);
        form.add(uuidField, 1, 0);
        form.add(generateUuidButton, 2, 0);

        // 创建标签样式
        String labelStyle = "-fx-font-weight: bold; -fx-font-size: 14px;";

        // 名称字段
        nameField = createStyledTextField("商品名称");
        form.add(createLabel("商品名称:", labelStyle), 0, 1);
        form.add(nameField, 1, 1, 2, 1);

        // 价格字段
        priceField = createStyledTextField("价格（元）");
        form.add(createLabel("价格:", labelStyle), 0, 2);
        form.add(priceField, 1, 2, 2, 1);

        // 类别字段
        categoryField = createStyledTextField("类别");
        form.add(createLabel("类别:", labelStyle), 0, 3);
        form.add(categoryField, 1, 3, 2, 1);

        // 库存字段
        stockField = createStyledTextField("库存数量");
        form.add(createLabel("库存:", labelStyle), 0, 4);
        form.add(stockField, 1, 4, 2, 1);

        // 条形码字段
        barcodeField = createStyledTextField("条形码");
        form.add(createLabel("条形码:", labelStyle), 0, 5);
        form.add(barcodeField, 1, 5, 2, 1);

        // 图片URL字段
        pictureField = createStyledTextField("图片URL");
        form.add(createLabel("图片URL:", labelStyle), 0, 6);
        form.add(pictureField, 1, 6, 2, 1);

        // 描述字段 - 改为TextArea
        Label descriptionLabel = createLabel("描述:", labelStyle);
        descriptionField = new TextArea();
        descriptionField.setPromptText("商品描述");
        descriptionField.setWrapText(true);
        descriptionField.setPrefRowCount(4);
        descriptionField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; -fx-border-radius: 5;");

        form.add(descriptionLabel, 0, 7);
        form.add(descriptionField, 1, 7, 2, 1);

        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        submitButton = new Button("添加商品");
        submitButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-pref-width: 120px; -fx-pref-height: 35px;");
        submitButton.setOnAction(e -> addProduct());

        clearButton = new Button("清空");
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

    private void addProduct() {
        // 验证表单
        if (nameField.getText().trim().isEmpty()) {
            setStatus("请输入商品名称");
            return;
        }
        if (priceField.getText().trim().isEmpty()) {
            setStatus("请输入商品价格");
            return;
        }
        if (categoryField.getText().trim().isEmpty()) {
            setStatus("请输入商品类别");
            return;
        }
        if (stockField.getText().trim().isEmpty()) {
            setStatus("请输入库存数量");
            return;
        }
        if (barcodeField.getText().trim().isEmpty()) {
            setStatus("请输入条形码");
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("添加中..."));

                String uuid = uuidField.getText().trim();
                String name = nameField.getText().trim();
                double price = Double.parseDouble(priceField.getText().trim());
                int priceFen = (int) Math.round(price * 100);
                String category = categoryField.getText().trim();
                int stock = Integer.parseInt(stockField.getText().trim());
                String barcode = barcodeField.getText().trim();
                String picture = pictureField.getText().trim();
                String description = descriptionField.getText().trim();

                // 构建商品对象
                Map<String, Object> item = new HashMap<>();
                item.put("uuid", uuid);
                item.put("itemName", name);
                item.put("price", priceFen);
                item.put("category", category);
                item.put("stock", stock);
                item.put("barcode", barcode);
                item.put("pictureLink", picture);
                item.put("description", description);

                // 构建添加请求
                Map<String, Object> data = new HashMap<>();
                data.put("item", item);
                Request request = new Request("addItem", data);

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
                } else {
                    Platform.runLater(() ->
                            setStatus("添加失败: " + responseMap.get("message")));
                }
            } catch (NumberFormatException e) {
                Platform.runLater(() ->
                        setStatus("输入错误: 价格和库存必须是数字"));
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("添加错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void clearForm() {
        nameField.clear();
        priceField.clear();
        categoryField.clear();
        stockField.clear();
        barcodeField.clear();
        pictureField.clear();
        descriptionField.clear();
        uuidField.setText(UUID.randomUUID().toString());
        setStatus("表单已清空");
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}