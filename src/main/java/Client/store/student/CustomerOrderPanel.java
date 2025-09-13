package Client.store.student;

import Client.ClientNetworkHelper;
import Client.store.util.StoreUtils;
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

public class CustomerOrderPanel extends BorderPane {
    private final String cardNumber;
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private VBox ordersContainer;
    private TextField searchField;
    private Label statusLabel;
    private Label resultsLabel;
    private Button refreshBtn;

    private Gson gson;

    public CustomerOrderPanel(String cardNumber) {
        this.cardNumber = cardNumber;

        // 初始化Gson适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        loadOrders();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(20);
        topContainer.setPadding(new Insets(0, 0, 20, 0));

        // 标题
        Label titleLabel = new Label("我的订单");
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
        searchField.setPromptText("输入订单UUID搜索...");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-font-size: 16px; -fx-padding: 0 10px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // 监听输入，限制只能输入UUID格式字符
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[0-9a-zA-Z-]*")) {
                searchField.setText(newValue.replaceAll("[^0-9a-zA-Z-]", ""));
            }
        });

        Button searchBtn = new Button("搜索");
        searchBtn.setPrefSize(100, 40);
        searchBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white;");
        searchBtn.setOnAction(e -> searchOrders());

        refreshBtn = new Button("刷新");
        refreshBtn.setPrefSize(100, 40);
        refreshBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #2ecc71; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadOrders());

        searchBar.getChildren().addAll(searchField, searchBtn, refreshBtn);

        searchBox.getChildren().addAll(searchBar);
        topContainer.getChildren().addAll(titleLabel, searchBox);
        setTop(topContainer);

        // 结果数量标签
        resultsLabel = new Label("加载中...");
        resultsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 0 0 10 0;");

        // 中心订单展示区域
        ordersContainer = new VBox(15);
        ordersContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(ordersContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox centerBox = new VBox(10);
        centerBox.getChildren().addAll(resultsLabel, scrollPane);
        setCenter(centerBox);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    private void loadOrders() {
        setStatus("加载订单中...", "info");
        refreshBtn.setDisable(true);

        new Thread(() -> {
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

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        displayOrders(orders);
                        setStatus("成功加载 " + orderList.size() + " 个订单", "success");
                        refreshBtn.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("加载失败: " + responseMap.get("message"), "error");
                        resultsLabel.setText("加载失败");
                        refreshBtn.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage(), "error");
                    resultsLabel.setText("通信错误");
                    refreshBtn.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void searchOrders() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            displayOrders(orders);
            resultsLabel.setText("显示全部 " + orders.size() + " 个订单");
            return;
        }

        List<Order> filteredOrders = new ArrayList<>();
        for (Order order : orders) {
            if (order.getUuid().toString().toLowerCase().contains(keyword)) {
                filteredOrders.add(order);
            }
        }

        displayOrders(filteredOrders);
        resultsLabel.setText("找到 " + filteredOrders.size() + " 个订单");
    }

    private void displayOrders(List<Order> orderList) {
        Platform.runLater(() -> {
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
        });
    }

    private VBox createOrderCard(Order order) {
        VBox card = new VBox();
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        card.setSpacing(10);

        // 订单基本信息区域
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

        Label timeLabel = new Label("时间: " + order.getTime());
        timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(idLabel, timeLabel);

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

        // 操作按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button detailBtn = new Button("详情");
        detailBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-padding: 8 16; -fx-background-radius: 5;");
        detailBtn.setOnAction(e -> showOrderDetail(order));

        // 根据订单状态显示不同按钮
        if ("待支付".equals(order.getStatus())) {
            Button payBtn = new Button("支付");
            payBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 8 16; -fx-background-radius: 5;");
            payBtn.setOnAction(e -> payOrder(order.getUuid().toString()));

            Button cancelBtn = new Button("取消");
            cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 8 16; -fx-background-radius: 5;");
            cancelBtn.setOnAction(e -> cancelOrder(order.getUuid().toString()));

            buttonBox.getChildren().addAll(detailBtn, payBtn, cancelBtn);
        } else if ("已支付".equals(order.getStatus())) {
            Button refundBtn = new Button("申请退款");
            refundBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 8 16; -fx-background-radius: 5;");
            refundBtn.setOnAction(e -> refundOrder(order.getUuid().toString()));

            buttonBox.getChildren().addAll(detailBtn, refundBtn);
        } else {
            buttonBox.getChildren().add(detailBtn);
        }

        card.getChildren().addAll(summaryBox, buttonBox);

        return card;
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

    private void showOrderDetail(Order order) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("加载订单详情中...", "info"));

                // 构建获取订单详情请求
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
                    Order detailedOrder = gson.fromJson(gson.toJson(responseMap.get("data")), Order.class);

                    Platform.runLater(() -> {
                        // 创建并显示订单详情对话框
                        Dialog<Void> dialog = new Dialog<>();
                        dialog.setTitle("订单详情");
                        dialog.setHeaderText("订单号: " + detailedOrder.getUuid());

                        // 设置对话框样式
                        DialogPane dialogPane = dialog.getDialogPane();
                        dialogPane.setStyle("-fx-font-size: 14px;");

                        // 创建详情内容
                        VBox content = new VBox(10);
                        content.setPadding(new Insets(10));

                        GridPane detailGrid = new GridPane();
                        detailGrid.setHgap(15);
                        detailGrid.setVgap(10);
                        detailGrid.setPadding(new Insets(10, 0, 0, 0));

                        // 添加详细信息
                        detailGrid.add(new Label("订单号:"), 0, 0);
                        detailGrid.add(new Label(detailedOrder.getUuid().toString()), 1, 0);

                        detailGrid.add(new Label("订单时间:"), 0, 1);
                        detailGrid.add(new Label(detailedOrder.getTime()), 1, 1);

                        detailGrid.add(new Label("订单状态:"), 0, 2);
                        Label statusDetailLabel = new Label(detailedOrder.getStatus());
                        statusDetailLabel.setStyle("-fx-text-fill: " + getStatusColor(detailedOrder.getStatus()) + "; -fx-font-weight: bold;");
                        detailGrid.add(statusDetailLabel, 1, 2);

                        detailGrid.add(new Label("订单金额:"), 0, 3);
                        detailGrid.add(new Label(detailedOrder.getTotalYuan() + "元"), 1, 3);

                        if (detailedOrder.getRemark() != null && !detailedOrder.getRemark().isEmpty()) {
                            detailGrid.add(new Label("备注:"), 0, 4);
                            detailGrid.add(new Label(detailedOrder.getRemark()), 1, 4);
                        }

                        content.getChildren().add(detailGrid);

                        // 添加商品列表
                        if (detailedOrder.getItems() != null && !detailedOrder.getItems().isEmpty()) {
                            Label itemsLabel = new Label("商品列表:");
                            itemsLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
                            content.getChildren().add(itemsLabel);

                            for (OrderItem item : detailedOrder.getItems()) {
                                HBox itemBox = new HBox(15);
                                itemBox.setPadding(new Insets(10));
                                itemBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
                                itemBox.setAlignment(Pos.CENTER_LEFT);

                                VBox itemInfo = new VBox(5);
                                itemInfo.getChildren().addAll(
                                        new Label("商品: " + item.getItem().getItemName()),
                                        new Label("价格: " + (item.getPrice() / 100.0) + "元"),
                                        new Label("数量: " + item.getAmount()),
                                        new Label("条形码: " + item.getItem().getBarcode())
                                );

                                itemBox.getChildren().add(itemInfo);
                                content.getChildren().add(itemBox);
                            }
                        }

                        dialog.getDialogPane().setContent(content);
                        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        dialog.showAndWait();

                        setStatus("订单详情加载完成", "success");
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("加载订单详情失败: " + responseMap.get("message"), "error");
                        showAlert("错误", "加载订单详情失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("加载订单详情错误: " + e.getMessage(), "error");
                    showAlert("错误", "加载订单详情时发生错误: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void payOrder(String orderId) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("支付中...", "info"));

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
                        setStatus("支付成功", "success");
                        showAlert("成功", "订单支付成功");
                        // 刷新订单列表
                        loadOrders();
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("支付失败: " + responseMap.get("message"), "error");
                        showAlert("错误", "支付失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("支付错误: " + e.getMessage(), "error");
                    showAlert("错误", "支付过程中发生错误: " + e.getMessage());
                });
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
                    Platform.runLater(() -> setStatus("取消中...", "info"));

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
                            setStatus("取消成功", "success");
                            showAlert("成功", "订单取消成功");
                            // 刷新订单列表
                            loadOrders();
                        });
                    } else {
                        Platform.runLater(() -> {
                            setStatus("取消失败: " + responseMap.get("message"), "error");
                            showAlert("错误", "取消失败: " + responseMap.get("message"));
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        setStatus("取消错误: " + e.getMessage(), "error");
                        showAlert("错误", "取消过程中发生错误: " + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void refundOrder(String orderId) {
        // 创建退款确认对话框
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("申请退款");
        dialog.setHeaderText("确认要对订单 " + orderId + " 申请退款吗？");
        dialog.setContentText("退款原因(可选):");

        // 设置对话框样式
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 14px;");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> setStatus("申请退款中...", "info"));

                    // 构建退款请求
                    Map<String, Object> data = new HashMap<>();
                    data.put("orderId", orderId);

                    if (!reason.trim().isEmpty()) {
                        data.put("reason", reason.trim());
                    }

                    Request request = new Request("refundOrder", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Platform.runLater(() -> {
                            setStatus("退款申请已提交", "success");
                            showAlert("成功", "退款申请已提交，等待处理");
                            // 刷新订单列表
                            loadOrders();
                        });
                    } else {
                        Platform.runLater(() -> {
                            setStatus("退款申请失败: " + responseMap.get("message"), "error");
                            showAlert("错误", "退款申请失败: " + responseMap.get("message"));
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        setStatus("退款申请错误: " + e.getMessage(), "error");
                        showAlert("错误", "退款申请过程中发生错误: " + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }).start();
        });
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