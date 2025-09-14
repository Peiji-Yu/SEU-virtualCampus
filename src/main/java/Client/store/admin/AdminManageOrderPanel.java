package Client.store.admin;

import Client.ClientNetworkHelper;
import Client.store.util.model.Order;
import Client.store.util.model.OrderItem;
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
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

public class AdminManageOrderPanel extends BorderPane {
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private VBox ordersContainer;
    private TextField searchField;
    private Label statusLabel;
    private Map<String, Boolean> expandedOrders = new HashMap<>();

    private Gson gson;

    public AdminManageOrderPanel() {
        // 初始化Gson适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        loadAllOrders();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(20);
        topContainer.setPadding(new Insets(0, 0, 20, 0));

        // 标题
        Label titleLabel = new Label("管理订单");
        titleLabel.setFont(Font.font(24));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 顶部搜索区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(20));
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // 统一搜索框
        searchField = new TextField();
        searchField.setPromptText("输入订单UUID或用户卡号...");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-font-size: 16px; -fx-padding: 0 10px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // 监听输入，限制只能输入数字或UUID格式
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[0-9a-zA-Z-]*")) {
                searchField.setText(newValue.replaceAll("[^0-9a-zA-Z-]", ""));
            }
        });

        Button searchBtn = new Button("搜索");
        searchBtn.setPrefSize(100, 40);
        searchBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white;");
        searchBtn.setOnAction(e -> handleSearch());

        Button refreshBtn = new Button("刷新");
        refreshBtn.setPrefSize(100, 40);
        refreshBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #2ecc71; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadAllOrders());

        searchBar.getChildren().addAll(searchField, searchBtn, refreshBtn);

        searchBox.getChildren().addAll(searchBar);
        topContainer.getChildren().addAll(titleLabel, searchBox);
        setTop(topContainer);

        // 中心订单展示区域
        ordersContainer = new VBox(15);
        ordersContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(ordersContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        setCenter(scrollPane);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    private void handleSearch() {
        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            loadAllOrders();
            return;
        }

        // 判断搜索内容类型：纯数字可能是卡号，否则是订单ID
        if (searchText.matches("\\d+")) {
            searchOrdersByUser(searchText);
        } else {
            searchOrderById(searchText);
        }
    }

    private void loadAllOrders() {
        setStatus("加载所有订单中...", "info");

        new Thread(() -> {
            try {
                // 构建请求
                Request request = new Request("getAllOrders", null);

                // 发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取订单列表
                    Type orderListType = new TypeToken<List<Order>>(){}.getType();
                    List<Order> orderList = gson.fromJson(gson.toJson(responseMap.get("data")), orderListType);

                    Platform.runLater(() -> {
                        displayOrders(orderList);
                        setStatus("成功加载 " + orderList.size() + " 个订单", "success");
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("加载失败: " + responseMap.get("message"), "error");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void searchOrderById(String orderId) {
        if (orderId.isEmpty()) {
            setStatus("请输入订单ID", "error");
            return;
        }

        setStatus("查询订单中...", "info");

        new Thread(() -> {
            try {
                // 构建请求
                Map<String, Object> data = new HashMap<>();
                data.put("orderId", orderId);

                Request request = new Request("getOrder", data);

                // 发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取订单详情
                    Order order = gson.fromJson(gson.toJson(responseMap.get("data")), Order.class);

                    Platform.runLater(() -> {
                        List<Order> orderList = new ArrayList<>();
                        orderList.add(order);
                        displayOrders(orderList);
                        setStatus("订单查询成功", "success");
                    });
                } else {
                    Platform.runLater(() -> {
                        ordersContainer.getChildren().clear();
                        Label emptyLabel = new Label("没有找到订单");
                        emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
                        ordersContainer.getChildren().add(emptyLabel);
                        setStatus("查询失败: " + responseMap.get("message"), "error");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void searchOrdersByUser(String cardNumber) {
        if (cardNumber.isEmpty()) {
            setStatus("请输入用户卡号", "error");
            return;
        }

        setStatus("查询用户订单中...", "info");

        new Thread(() -> {
            try {
                // 构建请求
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", Integer.parseInt(cardNumber));

                Request request = new Request("getUserOrders", data);

                // 发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取订单列表
                    Type orderListType = new TypeToken<List<Order>>(){}.getType();
                    List<Order> orderList = gson.fromJson(gson.toJson(responseMap.get("data")), orderListType);

                    Platform.runLater(() -> {
                        displayOrders(orderList);
                        setStatus("找到 " + orderList.size() + " 个订单", "success");
                    });
                } else {
                    Platform.runLater(() -> {
                        ordersContainer.getChildren().clear();
                        Label emptyLabel = new Label("没有找到订单");
                        emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
                        ordersContainer.getChildren().add(emptyLabel);
                        setStatus("查询失败: " + responseMap.get("message"), "error");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void displayOrders(List<Order> orderList) {
        ordersContainer.getChildren().clear();

        if (orderList.isEmpty()) {
            Label emptyLabel = new Label("没有找到订单");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 20;");
            ordersContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Order order : orderList) {
            VBox orderCard = createOrderCard(order);
            ordersContainer.getChildren().add(orderCard);
        }
    }

    private VBox createOrderCard(Order order) {
        VBox card = new VBox();
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        card.setSpacing(10);

        // 存储订单ID和展开状态
        String orderId = order.getUuid().toString();
        boolean isExpanded = expandedOrders.getOrDefault(orderId, false);

        // 订单基本信息区域（始终显示）
        HBox summaryBox = new HBox();
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setSpacing(15);

        // 订单状态指示器
        Region statusIndicator = new Region();
        statusIndicator.setPrefSize(8, 40);
        statusIndicator.setStyle("-fx-background-color: " + getStatusColor(order.getStatus()) + "; -fx-background-radius: 4;");

        // 订单基本信息
        VBox infoBox = new VBox(5);
        Label idLabel = new Label("订单号: " + order.getUuid());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label userLabel = new Label("用户: " + order.getCardNumber());
        userLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        Label timeLabel = new Label("时间: " + order.getTime());
        timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(idLabel, userLabel, timeLabel);

        // 金额和状态信息
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        Label totalLabel = new Label(order.getTotalYuan() + "元");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #e74c3c;");

        Label statusLabel = new Label(order.getStatus());
        statusLabel.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus()) + "; -fx-font-weight: bold; -fx-font-size: 14px;");

        statusBox.getChildren().addAll(totalLabel, statusLabel);

        summaryBox.getChildren().addAll(statusIndicator, infoBox, statusBox);

        // 详细信息区域（默认折叠）
        VBox detailBox = new VBox(10);
        detailBox.setVisible(isExpanded);
        detailBox.setManaged(isExpanded);

        if (isExpanded) {
            // 添加详细信息
            addOrderDetails(detailBox, order);
        }

        // 操作按钮区域（仅在展开时显示）
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setVisible(isExpanded);
        buttonBox.setManaged(isExpanded);

        if (isExpanded && "已支付".equals(order.getStatus())) {
            Button refundBtn = new Button("退款");
            refundBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 8 16; -fx-background-radius: 5;");
            refundBtn.setOnAction(e -> refundOrder(order.getUuid().toString()));
            buttonBox.getChildren().add(refundBtn);
        }

        card.getChildren().addAll(summaryBox, detailBox, buttonBox);

        // 点击卡片切换展开状态
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                boolean newExpandedState = !expandedOrders.getOrDefault(orderId, false);
                expandedOrders.put(orderId, newExpandedState);

                detailBox.setVisible(newExpandedState);
                detailBox.setManaged(newExpandedState);
                buttonBox.setVisible(newExpandedState);
                buttonBox.setManaged(newExpandedState);

                if (newExpandedState) {
                    addOrderDetails(detailBox, order);

                    // 如果已支付，添加退款按钮
                    if ("已支付".equals(order.getStatus())) {
                        Button refundBtn = new Button("退款");
                        refundBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; " +
                                "-fx-padding: 8 16; -fx-background-radius: 5;");
                        refundBtn.setOnAction(event -> refundOrder(order.getUuid().toString()));
                        buttonBox.getChildren().setAll(refundBtn);
                    } else {
                        buttonBox.getChildren().clear();
                    }
                }
            }
        });

        return card;
    }

    private void addOrderDetails(VBox detailBox, Order order) {
        detailBox.getChildren().clear();

        // 创建详细信息网格
        GridPane detailGrid = new GridPane();
        detailGrid.setHgap(15);
        detailGrid.setVgap(10);
        detailGrid.setPadding(new Insets(10, 0, 0, 0));

        // 添加详细信息
        detailGrid.add(new Label("订单号:"), 0, 0);
        detailGrid.add(new Label(order.getUuid().toString()), 1, 0);

        detailGrid.add(new Label("用户卡号:"), 0, 1);
        detailGrid.add(new Label(String.valueOf(order.getCardNumber())), 1, 1);

        detailGrid.add(new Label("订单时间:"), 0, 2);
        detailGrid.add(new Label(order.getTime()), 1, 2);

        detailGrid.add(new Label("订单状态:"), 0, 3);
        Label statusDetailLabel = new Label(order.getStatus());
        statusDetailLabel.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus()) + "; -fx-font-weight: bold;");
        detailGrid.add(statusDetailLabel, 1, 3);

        detailGrid.add(new Label("订单金额:"), 0, 4);
        detailGrid.add(new Label(order.getTotalYuan() + "元"), 1, 4);

        detailGrid.add(new Label("备注:"), 0, 5);
        detailGrid.add(new Label(order.getRemark() != null ? order.getRemark() : "无"), 1, 5);

        detailBox.getChildren().add(detailGrid);

        try {
            // 构建获取订单详细信息请求
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", order.getUuid().toString());
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
            } else {
                Platform.runLater(() -> {
                    setStatus("加载失败: " + responseMap.get("message"), "error");
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                setStatus("通信错误: " + e.getMessage(), "error");
            });
            e.printStackTrace();
        }

        // 添加订单项列表
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsLabel = new Label("订单项:");
            itemsLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
            detailBox.getChildren().add(itemsLabel);

            for (OrderItem item : order.getItems()) {
                HBox itemBox = new HBox(15);
                itemBox.setPadding(new Insets(10));
                itemBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
                itemBox.setAlignment(Pos.CENTER_LEFT);

                VBox itemInfo = new VBox(5);
                itemInfo.getChildren().addAll(
                        new Label("商品: " + item.getItem().getItemName()),
                        new Label("价格: " + (item.getItemPrice() / 100.0) + "元"),
                        new Label("数量: " + item.getAmount()),
                        new Label("条形码: " + item.getItem().getBarcode())
                );

                itemBox.getChildren().add(itemInfo);
                detailBox.getChildren().add(itemBox);
            }
        }
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

    private void refundOrder(String orderId) {
        // 创建退款确认对话框
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("订单退款");
        dialog.setHeaderText("确认要对订单 " + orderId + " 执行退款操作吗？");
        dialog.setContentText("退款原因(可选):");

        // 设置对话框样式
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 14px;");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            setStatus("处理退款中...", "info");

            new Thread(() -> {
                try {
                    // 构建退款请求
                    Map<String, Object> data = new HashMap<>();
                    data.put("orderId", orderId);

                    if (!reason.trim().isEmpty()) {
                        data.put("reason", reason.trim());
                    }

                    Request request = new Request("refundOrder", data);

                    // 发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Platform.runLater(() -> {
                            setStatus("退款成功", "success");
                            // 刷新订单列表
                            loadAllOrders();
                        });
                    } else {
                        Platform.runLater(() -> {
                            setStatus("退款失败: " + responseMap.get("message"), "error");
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        setStatus("通信错误: " + e.getMessage(), "error");
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private void setStatus(String message, String type) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            switch (type) {
                case "error":
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    break;
                case "success":
                    statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 14px;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
                    break;
            }
        });
    }
}