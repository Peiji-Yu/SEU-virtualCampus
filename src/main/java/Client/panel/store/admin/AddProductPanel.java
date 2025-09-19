package Client.panel.store.admin;

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

public class AddProductPanel extends BorderPane {
    private TextField nameField, priceField, stockField, barcodeField, pictureField;
    private TextArea descriptionField;
    private ToggleGroup categoryToggleGroup;
    private Label statusLabel;

    private static final String[] CATEGORY_OPTIONS = {"书籍", "文具", "食品", "日用品", "电子产品", "其他"};

    private Gson gson;

    public AddProductPanel() {
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
        Label titleLabel = new Label("添加商品");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("添加新的商品");
        subtitleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        VBox headtitleBox = new VBox(5, titleLabel, subtitleLabel);
        headtitleBox.setAlignment(Pos.CENTER_LEFT);
        headtitleBox.setPadding(new Insets(0, 0, 20, 0));

        // 创建GridPane来放置表单字段
        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(5);
        formGrid.setAlignment(Pos.CENTER_LEFT);

        // 商品名称 - 第一行
        VBox nameBox = createFieldWithLabel("商品名称");
        nameField = createStyledTextField("");
        nameBox.getChildren().add(nameField);
        formGrid.add(nameBox, 0, 0, 2, 1);

        // 价格和库存 - 第二行
        VBox priceBox = createFieldWithLabel("价格（元）");
        priceField = createStyledTextField("");
        priceBox.getChildren().add(priceField);
        formGrid.add(priceBox, 0, 1);

        VBox stockBox = createFieldWithLabel("库存数量");
        stockField = createStyledTextField("");
        stockBox.getChildren().add(stockField);
        formGrid.add(stockBox, 1, 1);

        // 设置GridPane列约束，使两列等宽
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        formGrid.getColumnConstraints().addAll(col1, col2);

        // 类别选择 - 使用单选框 - 第三行
        VBox categoryBox = createFieldWithLabel("类别");
        categoryToggleGroup = new ToggleGroup();

        // 创建单选框容器
        FlowPane categoryRadioContainer = new FlowPane();
        categoryRadioContainer.setHgap(15);
        categoryRadioContainer.setVgap(10);
        categoryRadioContainer.setAlignment(Pos.TOP_LEFT);

        // 为每个类别创建单选框
        for (String category : CATEGORY_OPTIONS) {
            RadioButton radioButton = new RadioButton(category);
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

        // 条形码 - 第四行
        VBox barcodeBox = createFieldWithLabel("条形码");
        barcodeField = createStyledTextField("");
        barcodeBox.getChildren().add(barcodeField);
        formGrid.add(barcodeBox, 0, 3, 2, 1);

        // 图片链接 - 第五行
        VBox pictureBox = createFieldWithLabel("图片URL");
        pictureField = createStyledTextField("");
        pictureBox.getChildren().add(pictureField);
        formGrid.add(pictureBox, 0, 4, 2, 1);

        // 描述字段 - 最下方
        VBox descriptionBox = createFieldWithLabel("描述");
        descriptionField = new TextArea();
        descriptionField.setPromptText("请输入商品描述");
        descriptionField.setWrapText(true);
        descriptionField.setPrefRowCount(6);

        descriptionField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                "-fx-border-radius: 5;");
        VBox.setVgrow(descriptionField, Priority.ALWAYS);
        descriptionBox.getChildren().add(descriptionField);

        // 按钮区域
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        Button submitButton = new Button("添加");
        submitButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        submitButton.setOnAction(e -> addProduct());

        buttonBox.getChildren().addAll(submitButton);

        // 将所有组件添加到容器
        container.getChildren().addAll(headtitleBox, formGrid, categoryBox, descriptionBox, buttonBox);

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

    private void addProduct() {
        // 验证表单
        if (nameField.getText().trim().isEmpty()) {
            setStatus("请输入商品名称");
            highlightField(nameField);
            return;
        }
        if (priceField.getText().trim().isEmpty()) {
            setStatus("请输入价格");
            highlightField(priceField);
            return;
        }
        if (stockField.getText().trim().isEmpty()) {
            setStatus("请输入库存数量");
            highlightField(stockField);
            return;
        }
        if (barcodeField.getText().trim().isEmpty()) {
            setStatus("请输入条形码");
            highlightField(barcodeField);
            return;
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            int stock = Integer.parseInt(stockField.getText().trim());

            // 验证价格和库存的合理性
            if (price <= 0) {
                setStatus("价格必须大于0");
                highlightField(priceField);
                return;
            }
            if (stock < 0) {
                setStatus("库存不能为负数");
                highlightField(stockField);
                return;
            }

            // 所有验证通过，继续添加商品
            new Thread(() -> {
                try {
                    Platform.runLater(() -> setStatus("添加中..."));

                    String name = nameField.getText().trim();
                    int priceFen = (int) Math.round(price * 100);
                    int stockValue = stock;
                    String barcode = barcodeField.getText().trim();
                    String picture = pictureField.getText().trim();
                    String description = descriptionField.getText().trim();

                    // 获取选中的类别
                    RadioButton selectedRadio = (RadioButton) categoryToggleGroup.getSelectedToggle();
                    String category = (String) selectedRadio.getUserData();

                    // 构建商品对象
                    Map<String, Object> item = new HashMap<>();
                    item.put("uuid", UUID.randomUUID().toString()); // 系统自动分配UUID
                    item.put("itemName", name);
                    item.put("price", priceFen);
                    item.put("category", category);
                    item.put("stock", stockValue);
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
                            setStatus("商品添加成功");
                            clearForm();
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

        } catch (NumberFormatException e) {
            setStatus("价格和库存必须是数字");
            if (!priceField.getText().trim().matches("\\d+(\\.\\d+)?")) highlightField(priceField);
            if (!stockField.getText().trim().matches("\\d+")) highlightField(stockField);
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
        nameField.clear();
        priceField.clear();
        stockField.clear();
        barcodeField.clear();
        pictureField.clear();
        descriptionField.clear();

        // 重置类别选择为第一个选项
        if (!categoryToggleGroup.getToggles().isEmpty()) {
            categoryToggleGroup.selectToggle(categoryToggleGroup.getToggles().get(0));
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}