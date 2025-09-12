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

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

public class AdminManageOrderPanel extends BorderPane {
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private VBox ordersContainer;
    private TextField orderIdField, userCardField;
    private Label statusLabel;

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
        setPadding(new Insets(15));

        // 顶部搜索区域
        VBox topBox = new VBox(10);

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        orderIdField = new TextField();
        orderIdField.setPromptText("订单UUID...");
        orderIdField.setPrefWidth(200);

        Button orderSearchBtn = new Button("查询订单");
        orderSearchBtn.setOnAction(e -> searchOrderById());

        userCardField = new TextField();
        userCardField.setPromptText("用户卡号...");
        userCardField.setPrefWidth(150);
        userCardField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                userCardField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        Button userOrderBtn = new Button("查询用户订单");
        userOrderBtn.setOnAction(e -> searchOrdersByUser());

        Button refreshBtn = new Button("刷新所有");
        refreshBtn.setOnAction(e -> loadAllOrders());

        searchBar.getChildren().addAll(
                new Label("订单ID:"), orderIdField, orderSearchBtn,
                new Label("用户卡号:"), userCardField, userOrderBtn,
                refreshBtn
        );

        // 状态标签
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        topBox.getChildren().addAll(searchBar, statusLabel);
        setTop(topBox);

        // 中心订单展示区域
        ordersContainer = new VBox(15);
        ordersContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(ordersContainer);
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);
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

    private void searchOrderById() {
        String orderId = orderIdField.getText().trim();

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

    private void searchOrdersByUser() {
        String cardNumber = userCardField.getText().trim();

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
            ordersContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Order order : orderList) {
            HBox orderCard = createOrderCard(order);
            ordersContainer.getChildren().add(orderCard);
        }
    }

    private HBox createOrderCard(Order order) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #ddd; -fx-border-radius: 8; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.CENTER_LEFT);

        // 订单基本信息
        VBox infoBox = new VBox(5);
        Label idLabel = new Label("订单号: " + order.getUuid());
        idLabel.setStyle("-fx-font-weight: bold;");

        Label userLabel = new Label("用户: " + order.getCardNumber());
        Label timeLabel = new Label("时间: " + order.getTime());
        Label totalLabel = new Label("金额: " + order.getTotalYuan() + "元");
        Label statusLabel = new Label("状态: " + order.getStatus());
        statusLabel.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus()) + ";");

        infoBox.getChildren().addAll(idLabel, userLabel, timeLabel, totalLabel, statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 操作按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button detailBtn = new Button("详情");
        detailBtn.setOnAction(e -> showOrderDetail(order));

        // 管理员特有操作
        if ("已支付".equals(order.getStatus())) {
            Button refundBtn = new Button("退款");
            refundBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
            refundBtn.setOnAction(e -> refundOrder(order.getUuid()));

            buttonBox.getChildren().addAll(detailBtn, refundBtn);
        } else {
            buttonBox.getChildren().add(detailBtn);
        }

        card.getChildren().addAll(infoBox, spacer, buttonBox);

        return card;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "待支付": return "#ff9800";
            case "已支付": return "#4CAF50";
            case "已取消": return "#f44336";
            case "已退款": return "#9e9e9e";
            default: return "#000000";
        }
    }

    private void showOrderDetail(Order order) {
        // 创建详情对话框
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("订单详情");
        dialog.setHeaderText("订单号: " + order.getUuid());

        // 创建内容区域
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // 基本信息
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(5);

        infoGrid.add(new Label("用户卡号:"), 0, 0);
        infoGrid.add(new Label(String.valueOf(order.getCardNumber())), 1, 0);

        infoGrid.add(new Label("订单时间:"), 0, 1);
        infoGrid.add(new Label(order.getTime()), 1, 1);

        infoGrid.add(new Label("订单状态:"), 0, 2);
        Label statusLabel = new Label(order.getStatus());
        statusLabel.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus()) + ";");
        infoGrid.add(statusLabel, 1, 2);

        infoGrid.add(new Label("订单金额:"), 0, 3);
        infoGrid.add(new Label(order.getTotalYuan() + "元"), 1, 3);

        infoGrid.add(new Label("备注:"), 0, 4);
        infoGrid.add(new Label(order.getRemark() != null ? order.getRemark() : "无"), 1, 4);

        content.getChildren().add(infoGrid);

        // 订单项列表
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Label itemsLabel = new Label("订单项:");
            itemsLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
            content.getChildren().add(itemsLabel);

            for (OrderItem item : order.getItems()) {
                HBox itemBox = new HBox(10);
                itemBox.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 10; -fx-background-radius: 5;");

                VBox itemInfo = new VBox(5);
                itemInfo.getChildren().addAll(
                        new Label("商品: " + item.getItem().getItemName()),
                        new Label("价格: " + item.getPrice() / 100.0 + "元"),
                        new Label("数量: " + item.getAmount()),
                        new Label("条形码: " + item.getItem().getBarcode())
                );

                itemBox.getChildren().add(itemInfo);
                content.getChildren().add(itemBox);
            }
        }

        // 设置对话框按钮
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void refundOrder(String orderId) {
        // 创建退款确认对话框
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("订单退款");
        dialog.setHeaderText("确认要对订单 " + orderId + " 执行退款操作吗？");
        dialog.setContentText("退款原因(可选):");

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
                    statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 14px;");
                    break;
                case "success":
                    statusLabel.setStyle("-fx-text-fill: #388e3c; -fx-font-size: 14px;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
                    break;
            }
        });
    }
}