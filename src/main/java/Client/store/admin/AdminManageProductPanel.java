package Client.store.admin;

import Client.ClientNetworkHelper;
import Client.store.util.model.Item;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

public class AdminManageProductPanel extends BorderPane {
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private VBox productsContainer;
    private ComboBox<String> categoryCombo;
    private TextField searchField, idSearchField;
    private Label statusLabel;
    private Gson gson;

    public AdminManageProductPanel() {
        // 创建配置了LocalDate和UUID适配器的Gson实例
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        loadCategories();
        loadAllItems();
    }

    private void initializeUI() {
        setPadding(new Insets(15));

        // 顶部标题
        Label titleLabel = new Label("商品管理");
        titleLabel.setFont(Font.font(20));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        // 顶部搜索区域
        VBox topBox = new VBox(15);
        topBox.getChildren().add(titleLabel);

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("选择类别");
        categoryCombo.setPrefWidth(150);

        searchField = new TextField();
        searchField.setPromptText("搜索商品...");
        searchField.setPrefWidth(200);

        Button searchBtn = new Button("搜索");
        searchBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
        searchBtn.setOnAction(e -> performSearch());

        idSearchField = new TextField();
        idSearchField.setPromptText("按UUID搜索...");
        idSearchField.setPrefWidth(200);

        Button idSearchBtn = new Button("UUID搜索");
        idSearchBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
        idSearchBtn.setOnAction(e -> searchById());

        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadAllItems());

        Button addProductBtn = new Button("添加商品");
        addProductBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        addProductBtn.setOnAction(e -> addNewProduct());

        searchBar.getChildren().addAll(
                new Label("类别:"), categoryCombo,
                new Label("关键词:"), searchField, searchBtn,
                new Label("UUID:"), idSearchField, idSearchBtn,
                refreshBtn, addProductBtn
        );

        topBox.getChildren().add(searchBar);
        setTop(topBox);

        // 中心商品展示区域
        productsContainer = new VBox(15);
        productsContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(productsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        setCenter(scrollPane);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    private void loadCategories() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("加载类别中..."));

                // 构建获取类别请求
                Request request = new Request("getAllCategories", new HashMap<>());

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取类别列表
                    Type categoryListType = new TypeToken<List<String>>(){}.getType();
                    List<String> categories = gson.fromJson(gson.toJson(responseMap.get("data")), categoryListType);

                    // 在UI线程中更新下拉框
                    Platform.runLater(() -> {
                        categoryCombo.getItems().clear();
                        categoryCombo.getItems().add("所有类别");
                        categoryCombo.getItems().addAll(categories);
                        categoryCombo.getSelectionModel().selectFirst();
                        setStatus("类别加载完成");
                    });
                } else {
                    Platform.runLater(() ->
                            setStatus("加载类别失败: " + responseMap.get("message")));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("通信错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void loadAllItems() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("加载商品中..."));

                // 构建获取所有商品请求
                Request request = new Request("getAllItems", new HashMap<>());

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取商品列表
                    Type itemListType = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        displayProducts(items);
                        setStatus("加载完成，共 " + items.size() + " 个商品");
                    });
                } else {
                    Platform.runLater(() ->
                            setStatus("加载商品失败: " + responseMap.get("message")));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("通信错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        String category = categoryCombo.getValue();

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                // 构建搜索请求
                Map<String, Object> data = new HashMap<>();

                if ("所有类别".equals(category) || category == null) {
                    // 搜索所有类别
                    data.put("keyword", keyword);
                    Request request = new Request("searchItems", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Type itemListType = new TypeToken<List<Item>>(){}.getType();
                        List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);

                        Platform.runLater(() -> {
                            displayProducts(items);
                            setStatus("搜索完成，找到 " + items.size() + " 个商品");
                        });
                    } else {
                        Platform.runLater(() ->
                                setStatus("搜索失败: " + responseMap.get("message")));
                    }
                } else {
                    // 按特定类别搜索
                    data.put("category", category);
                    data.put("keyword", keyword);
                    Request request = new Request("searchItemsByCategory", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Type itemListType = new TypeToken<List<Item>>(){}.getType();
                        List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);

                        Platform.runLater(() -> {
                            displayProducts(items);
                            setStatus("搜索完成，找到 " + items.size() + " 个商品");
                        });
                    } else {
                        Platform.runLater(() ->
                                setStatus("搜索失败: " + responseMap.get("message")));
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("通信错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void searchById() {
        String uuid = idSearchField.getText().trim();

        if (uuid.isEmpty()) {
            setStatus("请输入UUID");
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                // 构建按UUID搜索请求
                Map<String, Object> data = new HashMap<>();
                data.put("itemId", uuid);
                Request request = new Request("getItemById", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取单个商品
                    Item item = gson.fromJson(gson.toJson(responseMap.get("data")), Item.class);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        List<Item> singleItemList = new ArrayList<>();
                        singleItemList.add(item);
                        displayProducts(singleItemList);
                        setStatus("搜索完成");
                    });
                } else {
                    Platform.runLater(() -> {
                        displayProducts(new ArrayList<>());
                        setStatus("未找到商品: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("通信错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void displayProducts(List<Item> productList) {
        productsContainer.getChildren().clear();

        if (productList.isEmpty()) {
            Label emptyLabel = new Label("没有找到商品");
            emptyLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 16px; -fx-padding: 20;");
            productsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Item product : productList) {
            HBox productCard = createProductCard(product);
            productsContainer.getChildren().add(productCard);
        }
    }

    private HBox createProductCard(Item product) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #ddd; -fx-border-radius: 8; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.CENTER_LEFT);

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setStyle("-fx-background-color: #f0f0f0;");

        if (product.getPictureLink() != null && !product.getPictureLink().isEmpty()) {
            try {
                Image image = new Image(product.getPictureLink(), true);
                imageView.setImage(image);
            } catch (Exception e) {
                // 使用默认图片
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-product.png"));
                if (defaultImage.isError()) {
                    // 如果默认图片加载失败，使用纯色背景
                    imageView.setStyle("-fx-background-color: #e0e0e0;");
                } else {
                    imageView.setImage(defaultImage);
                }
            }
        } else {
            // 使用默认图片
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-product.png"));
            if (defaultImage.isError()) {
                // 如果默认图片加载失败，使用纯色背景
                imageView.setStyle("-fx-background-color: #e0e0e0;");
            } else {
                imageView.setImage(defaultImage);
            }
        }

        // 商品信息
        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(product.getItemName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label priceLabel = new Label("价格: " + product.getPriceYuan() + "元");
        Label stockLabel = new Label("库存: " + product.getStock());
        Label categoryLabel = new Label("类别: " + product.getCategory());
        Label idLabel = new Label("ID: " + product.getUuid());
        idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Label salesLabel = new Label("销量: " + product.getSalesVolume());
        salesLabel.setStyle("-fx-text-fill: #666;");

        infoBox.getChildren().addAll(nameLabel, priceLabel, stockLabel, salesLabel, categoryLabel, idLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 操作按钮
        Button editBtn = new Button("修改");
        editBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
        editBtn.setOnAction(e -> editProduct(product));

        Button deleteBtn = new Button("删除");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> deleteProduct(product.getUuid()));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(imageView, infoBox, spacer, buttonBox);

        return card;
    }

    private void addNewProduct() {
        // 创建添加商品对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("添加商品");
        dialog.setHeaderText("添加新商品");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("商品名称");

        ComboBox<String> categoryField = new ComboBox<>();
        categoryField.setItems(categoryCombo.getItems());
        categoryField.getSelectionModel().selectFirst();

        TextField priceField = new TextField();
        priceField.setPromptText("价格(分)");

        TextField stockField = new TextField();
        stockField.setPromptText("库存数量");

        TextField imageField = new TextField();
        imageField.setPromptText("图片链接");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("商品描述");
        descriptionArea.setPrefRowCount(3);

        TextField barcodeField = new TextField();
        barcodeField.setPromptText("条形码");

        grid.add(new Label("名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("类别:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("价格(分):"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("库存:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("图片链接:"), 0, 4);
        grid.add(imageField, 1, 4);
        grid.add(new Label("描述:"), 0, 5);
        grid.add(descriptionArea, 1, 5);
        grid.add(new Label("条形码:"), 0, 6);
        grid.add(barcodeField, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // 创建新商品
                Item newItem = new Item();
                newItem.setUuid(UUID.randomUUID().toString());
                newItem.setItemName(nameField.getText());
                newItem.setCategory(categoryField.getValue());
                newItem.setPrice(Integer.parseInt(priceField.getText()));
                newItem.setStock(Integer.parseInt(stockField.getText()));
                newItem.setPictureLink(imageField.getText());
                newItem.setDescription(descriptionArea.getText());
                newItem.setBarcode(barcodeField.getText());
                newItem.setSalesVolume(0); // 新商品销量为0

                // 发送添加请求
                addProduct(newItem);
            }
        });
    }

    private void addProduct(Item item) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("添加商品中..."));

                // 构建添加商品请求
                Map<String, Object> data = new HashMap<>();
                data.put("item", item);
                Request request = new Request("addItem", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("添加商品成功");
                        // 刷新商品列表
                        loadAllItems();
                    });
                } else {
                    Platform.runLater(() ->
                            setStatus("添加商品失败: " + responseMap.get("message")));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("添加商品错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void editProduct(Item product) {
        // 创建编辑商品对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("修改商品");
        dialog.setHeaderText("修改《" + product.getItemName() + "》的信息");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(product.getItemName());

        ComboBox<String> categoryField = new ComboBox<>();
        categoryField.setItems(categoryCombo.getItems());
        categoryField.getSelectionModel().select(product.getCategory());

        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        priceField.setPromptText("价格(分)");

        TextField stockField = new TextField(String.valueOf(product.getStock()));

        TextField imageField = new TextField(product.getPictureLink());
        imageField.setPromptText("图片链接");

        TextArea descriptionArea = new TextArea(product.getDescription());
        descriptionArea.setPrefRowCount(3);

        TextField barcodeField = new TextField(product.getBarcode());
        barcodeField.setPromptText("条形码");

        grid.add(new Label("名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("类别:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("价格(分):"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("库存:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("图片链接:"), 0, 4);
        grid.add(imageField, 1, 4);
        grid.add(new Label("描述:"), 0, 5);
        grid.add(descriptionArea, 1, 5);
        grid.add(new Label("条形码:"), 0, 6);
        grid.add(barcodeField, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // 更新商品信息
                product.setItemName(nameField.getText());
                product.setCategory(categoryField.getValue());
                product.setPrice(Integer.parseInt(priceField.getText()));
                product.setStock(Integer.parseInt(stockField.getText()));
                product.setPictureLink(imageField.getText());
                product.setDescription(descriptionArea.getText());
                product.setBarcode(barcodeField.getText());

                // 发送更新请求
                updateProduct(product);
            }
        });
    }

    private void updateProduct(Item product) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("更新商品中..."));

                // 构建更新商品请求
                Map<String, Object> data = new HashMap<>();
                data.put("item", product);
                Request request = new Request("updateItem", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("更新商品成功");
                        // 刷新商品列表
                        loadAllItems();
                    });
                } else {
                    Platform.runLater(() ->
                            setStatus("更新商品失败: " + responseMap.get("message")));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("更新商品错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteProduct(String productId) {
        // 确认删除对话框
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认删除");
        confirmDialog.setHeaderText("确认删除商品");
        confirmDialog.setContentText("确定要删除这个商品吗？此操作不可恢复。");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        Platform.runLater(() -> setStatus("删除商品中..."));

                        // 构建删除商品请求
                        Map<String, Object> data = new HashMap<>();
                        data.put("itemId", productId);
                        Request request = new Request("deleteItem", data);

                        // 使用ClientNetworkHelper发送请求
                        String netResponse = ClientNetworkHelper.send(request);

                        // 解析响应
                        Map<String, Object> responseMap = gson.fromJson(netResponse, Map.class);
                        int code = ((Double) responseMap.get("code")).intValue();

                        if (code == 200) {
                            Platform.runLater(() -> {
                                setStatus("删除商品成功");
                                // 刷新商品列表
                                loadAllItems();
                            });
                        } else {
                            Platform.runLater(() ->
                                    setStatus("删除商品失败: " + responseMap.get("message")));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                setStatus("删除商品错误: " + e.getMessage()));
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}