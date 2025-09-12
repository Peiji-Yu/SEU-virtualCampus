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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminAddProductPanel extends VBox {
    private TextField nameField, priceField, categoryField, stockField,
            barcodeField, pictureField, descriptionField, uuidField;
    private Button submitButton;
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
        setPadding(new Insets(15));
        setSpacing(15);

        Label titleLabel = new Label("添加商品");
        titleLabel.setFont(Font.font(20));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 8;");

        // 商品UUID字段
        Label uuidLabel = new Label("商品UUID:");
        uuidLabel.setStyle("-fx-font-weight: bold;");
        uuidField = new TextField();
        uuidField.setText(UUID.randomUUID().toString());
        uuidField.setEditable(false);
        Button generateUuidButton = new Button("生成新UUID");
        generateUuidButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
        generateUuidButton.setOnAction(e -> uuidField.setText(UUID.randomUUID().toString()));

        form.add(uuidLabel, 0, 0);
        form.add(uuidField, 1, 0);
        form.add(generateUuidButton, 2, 0);

        nameField = new TextField();
        nameField.setPromptText("商品名称");

        priceField = new TextField();
        priceField.setPromptText("价格（元）");

        categoryField = new TextField();
        categoryField.setPromptText("类别");

        stockField = new TextField();
        stockField.setPromptText("库存数量");

        barcodeField = new TextField();
        barcodeField.setPromptText("条形码");

        pictureField = new TextField();
        pictureField.setPromptText("图片URL");

        descriptionField = new TextField();
        descriptionField.setPromptText("商品描述");

        form.add(new Label("商品名称:"), 0, 1);
        form.add(nameField, 1, 1);
        form.add(new Label("价格:"), 0, 2);
        form.add(priceField, 1, 2);
        form.add(new Label("类别:"), 0, 3);
        form.add(categoryField, 1, 3);
        form.add(new Label("库存:"), 0, 4);
        form.add(stockField, 1, 4);
        form.add(new Label("条形码:"), 0, 5);
        form.add(barcodeField, 1, 5);
        form.add(new Label("图片URL:"), 0, 6);
        form.add(pictureField, 1, 6);
        form.add(new Label("描述:"), 0, 7);
        form.add(descriptionField, 1, 7);

        // 按钮区域
        submitButton = new Button("添加商品");
        submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        submitButton.setPrefWidth(120);
        submitButton.setPrefHeight(40);
        submitButton.setOnAction(e -> addProduct());

        Button clearButton = new Button("清空");
        clearButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        clearButton.setPrefWidth(80);
        clearButton.setPrefHeight(40);
        clearButton.setOnAction(e -> clearForm());

        // 按钮容器
        VBox buttonBox = new VBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(submitButton, clearButton);

        // 状态标签
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        getChildren().addAll(titleLabel, form, buttonBox, statusLabel);
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