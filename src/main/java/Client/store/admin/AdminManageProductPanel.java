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
    private TextField searchField;
    private Label statusLabel;
    private Gson gson;
    private ToggleGroup viewToggleGroup;
    private Map<String, Accordion> productAccordions = new HashMap<>();

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
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(20);
        topContainer.setPadding(new Insets(0, 0, 20, 0));

        // 标题
        Label titleLabel = new Label("商品管理");
        titleLabel.setFont(Font.font(24));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 搜索区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(20));
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        // 搜索框
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("搜索商品名称或UUID...");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("搜索");
        searchBtn.setPrefSize(100, 40);
        searchBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        searchBtn.setOnAction(e -> performSearch());

        Button refreshBtn = new Button("刷新");
        refreshBtn.setPrefSize(100, 40);
        refreshBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        refreshBtn.setOnAction(e -> loadAllItems());

        searchBar.getChildren().addAll(searchField, searchBtn, refreshBtn);

        // 类别下拉框
        HBox categoryBox = new HBox(10);
        categoryBox.setAlignment(Pos.CENTER_LEFT);

        Label categoryLabel = new Label("筛选类别:");
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("所有类别");
        categoryCombo.setPrefWidth(200);
        categoryCombo.setPrefHeight(35);
        categoryCombo.setStyle("-fx-font-size: 14px; -fx-background-radius: 5;");

        categoryBox.getChildren().addAll(categoryLabel, categoryCombo);

        searchBox.getChildren().addAll(searchBar, categoryBox);
        topContainer.getChildren().addAll(titleLabel, searchBox);
        setTop(topContainer);

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
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
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

        // 判断是否是UUID搜索
        boolean isUuidSearch = false;
        try {
            UUID.fromString(keyword);
            isUuidSearch = true;
        } catch (IllegalArgumentException e) {
            // 不是有效的UUID，按普通搜索处理
        }

        if (isUuidSearch) {
            searchById(keyword);
        } else {
            searchByKeywordAndCategory(keyword, category);
        }
    }

    private void searchByKeywordAndCategory(String keyword, String category) {
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

    private void searchById(String uuid) {
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
        productAccordions.clear();

        if (productList.isEmpty()) {
            Label emptyLabel = new Label("没有找到商品");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 16px; -fx-padding: 40;");
            emptyLabel.setAlignment(Pos.CENTER);
            productsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Item product : productList) {
            TitledPane productPane = createProductPane(product);
            productsContainer.getChildren().add(productPane);
        }
    }

    private TitledPane createProductPane(Item product) {
        // 创建折叠面板
        TitledPane pane = new TitledPane();
        pane.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1;");
        pane.setExpanded(false);

        // 标题部分 - 只显示商品名和库存
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(10));

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        imageView.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");

        if (product.getPictureLink() != null && !product.getPictureLink().isEmpty()) {
            try {
                Image image = new Image(product.getPictureLink(), true);
                imageView.setImage(image);
            } catch (Exception e) {
                // 使用默认图片
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                    imageView.setImage(defaultImage);
                } catch (Exception ex) {
                    // 如果默认图片加载失败，使用纯色背景
                    imageView.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;");
                }
            }
        } else {
            // 使用默认图片
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                imageView.setImage(defaultImage);
            } catch (Exception e) {
                // 如果默认图片加载失败，使用纯色背景
                imageView.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;");
            }
        }

        Label nameLabel = new Label(product.getItemName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label stockLabel = new Label("库存: " + product.getStock());
        stockLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        titleBox.getChildren().addAll(imageView, nameLabel, spacer, stockLabel);
        pane.setGraphic(titleBox);

        // 内容部分 - 显示详细信息
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(15));
        contentBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 0 0 8 8;");

        // 商品详细信息
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(10);
        infoGrid.setPadding(new Insets(0, 0, 15, 0));

        Label priceLabel = new Label("价格: " + product.getPriceYuan() + "元");
        priceLabel.setStyle("-fx-font-size: 14px;");

        Label salesLabel = new Label("销量: " + product.getSalesVolume());
        salesLabel.setStyle("-fx-font-size: 14px;");

        Label categoryLabel = new Label("类别: " + product.getCategory());
        categoryLabel.setStyle("-fx-font-size: 14px;");

        Label idLabel = new Label("ID: " + product.getUuid());
        idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");

        infoGrid.add(priceLabel, 0, 0);
        infoGrid.add(salesLabel, 1, 0);
        infoGrid.add(categoryLabel, 0, 1);
        infoGrid.add(idLabel, 1, 1);

        // 商品描述
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            Label descLabel = new Label("描述: " + product.getDescription());
            descLabel.setStyle("-fx-font-size: 14px; -fx-wrap-text: true;");
            descLabel.setMaxWidth(Double.MAX_VALUE);
            infoGrid.add(descLabel, 0, 2, 2, 1);
        }

        // 操作按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("修改");
        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-size: 14px;");
        editBtn.setPrefSize(80, 35);
        editBtn.setOnAction(e -> editProduct(product));

        Button deleteBtn = new Button("删除");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-size: 14px;");
        deleteBtn.setPrefSize(80, 35);
        deleteBtn.setOnAction(e -> deleteProduct(product.getUuid()));

        buttonBox.getChildren().addAll(editBtn, deleteBtn);

        contentBox.getChildren().addAll(infoGrid, buttonBox);
        pane.setContent(contentBox);

        return pane;
    }

    private void editProduct(Item product) {
        // 创建编辑商品对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("修改商品");
        dialog.setHeaderText("修改《" + product.getItemName() + "》的信息");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(product.getItemName());
        nameField.setPromptText("商品名称");

        TextField categoryField = new TextField(product.getCategory());
        categoryField.setPromptText("商品类别");

        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        priceField.setPromptText("价格(分)");

        TextField stockField = new TextField(String.valueOf(product.getStock()));
        stockField.setPromptText("库存数量");

        TextField imageField = new TextField(product.getPictureLink());
        imageField.setPromptText("图片链接");

        TextArea descriptionArea = new TextArea(product.getDescription());
        descriptionArea.setPromptText("商品描述");
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
                product.setCategory(categoryField.getText());
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
        statusLabel.setText("状态: " + message);
    }
}