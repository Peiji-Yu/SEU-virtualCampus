package Client.panel.store.student;

import Client.ClientNetworkHelper;
import Client.model.store.Order;
import Client.model.store.OrderItem;
import Client.util.adapter.UUIDAdapter;
import Client.model.Request;
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
import java.util.*;

public class MyOrderPanel extends BorderPane {
    private final String cardNumber;
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private VBox ordersContainer;
    private TextField searchField;
    private Label resultsLabel;

    private Gson gson;

    public MyOrderPanel(String cardNumber) {
        this.cardNumber = cardNumber;

        // 初始化Gson适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        loadOrders();
        displayOrders(orders);
    }

    private void initializeUI() {
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("我的订单");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查看和管理您的订单");
        subtitleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        VBox headtitleBox = new VBox(5, titleLabel, subtitleLabel);
        headtitleBox.setAlignment(Pos.CENTER_LEFT);
        headtitleBox.setPadding(new Insets(0, 0, 20, 0));

        // 搜索区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(0, 0, 5, 0));
        searchBox.setStyle("-fx-background-color: white;");

        // 搜索框和按钮在同一行
        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        searchField = createStyledTextField("输入订单ID进行搜索");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("搜索");
        searchButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> searchOrders());

        searchRow.getChildren().addAll(searchField, searchButton);

        // 结果标签
        resultsLabel = new Label("找到 0 个订单");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(headtitleBox, searchRow, resultsLabel);
        topContainer.getChildren().addAll(searchBox);
        setTop(topContainer);

        // 中心订单展示区域
        ordersContainer = new VBox(15);
        ordersContainer.setPadding(new Insets(0, 28, 5, 28));

        ScrollPane scrollPane = new ScrollPane(ordersContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: white;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scrollPane);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 16px; -fx-pref-height: 45px; " +
                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent;" +
                "-fx-padding: 0 10px;");

        // 监听输入，限制只能输入UUID格式字符
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[0-9-]*")) {
                field.setText(newValue.replaceAll("[^0-9-]", ""));
            }
        });

        return field;
    }

    private void loadOrders() {
        try {
            // 构建获取订单请求
            Map<String, Object> data = new HashMap<>();
            data.put("cardNumber", Integer.parseInt(cardNumber));
            Request request = new Request("getUserOrders", data);

            // 使用ClientNetworkHelper发送请求
            String response = ClientNetworkHelper.send(request);

            // 解析响应
            Map<String, Object> responseMap = gson.fromJson(response, Map.class);
            int code = ((Double) responseMap.get("code")).intValue();

            if (code == 200) {
                // 提取订单列表
                Type orderListType = new TypeToken<List<Order>>(){}.getType();
                List<Order> orderList = gson.fromJson(gson.toJson(responseMap.get("data")), orderListType);

                orders.setAll(orderList);

            } else {
                System.err.println("加载失败: " + responseMap.get("message"));
            }
        } catch (Exception e) {
            System.err.println("通信错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void searchOrders() {
        loadOrders();

        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            displayOrders(orders);
            return;
        }

        List<Order> filteredOrders = new ArrayList<>();
        for (Order order : orders) {
            // 使用ID搜索
            if (order.getUuid().toLowerCase().contains(keyword)) {
                filteredOrders.add(order);
                continue;
            }
        }
        displayOrders(filteredOrders);
    }

    private void displayOrders(List<Order> orderList) {
        Platform.runLater(() -> {
            ordersContainer.getChildren().clear();

            if (orderList.isEmpty()) {
                resultsLabel.setText("找到 0 个订单");
                return;
            }

            for (Order order : orderList) {
                OrderCard card = new OrderCard(order);
                ordersContainer.getChildren().add(card);
            }
            resultsLabel.setText("找到 " + orderList.size() + " 个订单");
        });
    }

    private void payOrder(String orderId) {
        new Thread(() -> {
            try {
                // 构建支付订单请求
                Map<String, Object> data = new HashMap<>();
                data.put("orderId", orderId);
                Request request = new Request("payOrder", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        showAlert("成功", "订单支付成功");
                        // 刷新订单列表
                        loadOrders();
                        displayOrders(orders);
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("错误", "支付失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                System.err.println("支付过程中发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void cancelOrder(String orderId) {
        // 创建取消确认对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("取消订单");
        alert.setHeaderText("确认要取消订单 " + orderId + " 吗？");
        alert.setContentText("此操作不可撤销");

        // 设置对话框样式
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 14px;");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    // 构建取消订单请求
                    Map<String, Object> data = new HashMap<>();
                    data.put("orderId", orderId);
                    Request request = new Request("cancelOrder", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Platform.runLater(() -> {
                            showAlert("成功", "订单取消成功");
                            // 刷新订单列表
                            loadOrders();
                            displayOrders(orders);
                        });
                    } else {
                        Platform.runLater(() -> {
                            showAlert("错误", "取消失败: " + responseMap.get("message"));
                        });
                    }
                } catch (Exception e) {
                    System.err.println("取消过程中发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 设置对话框样式
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 14px;");

        alert.showAndWait();
    }

    // 内部类：订单卡片
    private class OrderCard extends VBox {
        private final Order order;

        public OrderCard(Order order) {
            this.order = order;
            initializeUI();
        }

        private void initializeUI() {
            setPadding(new Insets(15));
            setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
            setSpacing(10);

            // 订单基本信息区域
            HBox headerBox = new HBox();
            headerBox.setAlignment(Pos.CENTER_LEFT);
            headerBox.setSpacing(15);

            // 左侧订单信息
            VBox leftBox = new VBox(5);
            leftBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(leftBox, Priority.ALWAYS);

            Label idLabel = new Label("订单号: " + order.getUuid());
            idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label timeLabel = new Label("时间: " + order.getTime().replace("T", " "));
            timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

            leftBox.getChildren().addAll(idLabel, timeLabel);

            // 右侧价格和状态信息
            VBox rightBox = new VBox(5);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            Label totalLabel = new Label("¥" + order.getTotalYuan());
            totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label statusLabel = new Label(order.getStatus());
            statusLabel.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus()) +
                    "; -fx-font-weight: bold; -fx-font-size: 14px;");

            rightBox.getChildren().addAll(totalLabel, statusLabel);

            headerBox.getChildren().addAll(leftBox, rightBox);

            // 添加分割线
            Region separator = createSeparator();

            // 订单项容器
            VBox itemsContainer = new VBox(10);
            itemsContainer.setPadding(new Insets(10, 0, 0, 0));

            // 添加订单项
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                for (int i = 0; i < order.getItems().size(); i++) {
                    OrderItem item = order.getItems().get(i);
                    HBox itemBox = createItemBox(item);
                    itemsContainer.getChildren().add(itemBox);

                    // 在每两个商品之间添加分割线
                    if (i < order.getItems().size() - 1) {
                        Region itemSeparator = createSeparator();
                        itemsContainer.getChildren().add(itemSeparator);
                    }
                }
            } else {
                // 加载订单项详情
                loadOrderItems();

                // 显示加载中的占位符
                Label loadingLabel = new Label("加载商品信息中...");
                loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
                itemsContainer.getChildren().add(loadingLabel);
            }

            // 底部操作按钮区域
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            buttonBox.setPadding(new Insets(10, 0, 0, 0));

            // 根据订单状态显示不同按钮
            if ("待支付".equals(order.getStatus())) {
                Button payBtn = new Button("支付");
                payBtn.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-padding: 8 16; -fx-background-radius: 5;");
                payBtn.setOnAction(e -> payOrder(order.getUuid()));

                Button cancelBtn = new Button("取消");
                cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-padding: 8 16; -fx-background-radius: 5;");
                cancelBtn.setOnAction(e -> cancelOrder(order.getUuid()));

                buttonBox.getChildren().addAll(payBtn, cancelBtn);
            }

            getChildren().addAll(headerBox, separator, itemsContainer, buttonBox);
        }

        private HBox createItemBox(OrderItem item) {
            HBox itemBox = new HBox(15);
            itemBox.setAlignment(Pos.CENTER_LEFT);

            // 商品图片
            ImageView imageView = new ImageView();
            imageView.setFitWidth(60);
            imageView.setFitHeight(60);
            imageView.setPreserveRatio(true);

            String image_url = item.getItem().getPictureLink();
            if (image_url != null && !image_url.isEmpty()) {
                try {
                    Image image = new Image(image_url, true);
                    imageView.setImage(image);
                } catch (Exception e) {
                    // 使用默认图片
                    try {
                        Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                        imageView.setImage(defaultImage);
                    } catch (Exception ex) {
                        // 如果默认图片加载失败，使用纯色背景
                        imageView.setStyle("-fx-background-color: #e0e0e0;");
                    }
                }
            } else {
                // 使用默认图片
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                    imageView.setImage(defaultImage);
                } catch (Exception e) {
                    imageView.setStyle("-fx-background-color: #e0e0e0;");
                }
            }

            // 商品信息
            VBox itemInfo = new VBox(5);
            itemInfo.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(itemInfo, Priority.ALWAYS);

            Label nameLabel = new Label(item.getItem().getItemName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label descLabel = new Label(item.getItem().getDescription() != null ?
                    item.getItem().getDescription() : "暂无描述");
            descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            itemInfo.getChildren().addAll(nameLabel, descLabel);

            // 价格和数量
            VBox priceBox = new VBox(5);
            priceBox.setAlignment(Pos.CENTER_RIGHT);

            Label priceLabel = new Label(String.format("¥%.2f", item.getItemPrice() / 100.0));
            priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #000000;");

            Label quantityLabel = new Label("x" + item.getAmount());
            quantityLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            priceBox.getChildren().addAll(priceLabel, quantityLabel);

            itemBox.getChildren().addAll(imageView, itemInfo, priceBox);

            return itemBox;
        }

        private void loadOrderItems() {
            new Thread(() -> {
                try {
                    // 构建获取订单详细信息请求
                    Map<String, Object> data = new HashMap<>();
                    data.put("orderId", order.getUuid());
                    Request request = new Request("getOrder", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        // 提取订单详情
                        Order detail_order = gson.fromJson(gson.toJson(responseMap.get("data")), Order.class);
                        order.setItems(detail_order.getItems());

                        // 更新UI
                        Platform.runLater(() -> {
                            // 重新创建订单卡片
                            OrderCard newCard = new OrderCard(order);
                            int index = ordersContainer.getChildren().indexOf(OrderCard.this);
                            if (index >= 0) {
                                ordersContainer.getChildren().set(index, newCard);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // 创建分割线辅助方法
    private Region createSeparator() {
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator.setMaxWidth(Double.MAX_VALUE);
        return separator;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "待支付": return "#f39c12";
            case "已支付": return "#27ae60";
            case "已取消": return "#e74c3c";
            case "已退款": return "#95a5a6";
            default: return "#34495e";
        }
    }
}