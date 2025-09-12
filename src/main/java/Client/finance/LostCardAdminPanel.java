package Client.finance;

import Client.ClientNetworkHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.beans.property.SimpleStringProperty;
import java.util.List;
import java.util.Map;

/**
 * 管理员挂失管理面板：展示所有挂失一卡通账号，支持解除挂失。
 * @author GitHub Copilot
 */
public class LostCardAdminPanel extends VBox {
    private final TableView<Map<String, Object>> tableView;
    private final Label statusLabel;

    public LostCardAdminPanel() {
        setSpacing(18);
        setPadding(new Insets(32, 32, 32, 32));
        setStyle("-fx-background-color: #fff; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 12, 0, 0, 2);");

        Label title = new Label("挂失管理");
        title.setFont(Font.font(22));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        getChildren().add(title);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        getChildren().add(statusLabel);

        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY); // 替换弃用API
        tableView.setPrefHeight(480);

        TableColumn<Map<String, Object>, String> cardCol = new TableColumn<>("一卡通号");
        cardCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("cardNumber");
            if (value instanceof Number) {
                // 使用不带科学计数法的字符串显示
                return new SimpleStringProperty(String.format("%d", ((Number) value).longValue()));
            }
            return new SimpleStringProperty(String.valueOf(value));
        });
        cardCol.setPrefWidth(120);
        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("name"))));
        nameCol.setPrefWidth(80);
        TableColumn<Map<String, Object>, String> balanceCol = new TableColumn<>("余额");
        balanceCol.setCellValueFactory(data -> {
            Object value = data.getValue().get("balance");
            if (value instanceof Number) {
                double yuan = ((Number) value).doubleValue() / 100.0;
                return new SimpleStringProperty(String.format("%.2f元", yuan));
            }
            return new SimpleStringProperty("0.00元");
        });
        balanceCol.setPrefWidth(80);
        TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("status"))));
        statusCol.setPrefWidth(80);

        TableColumn<Map<String, Object>, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(100);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("解除挂失");
            {
                btn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14px;");
                btn.setOnAction(e -> {
                    Map<String, Object> item = getTableView().getItems().get(getIndex());
                    Object cardObj = item.get("cardNumber");
                    long cardNumber;
                    if (cardObj instanceof Number) {
                        cardNumber = ((Number) cardObj).longValue();
                    } else {
                        cardNumber = Long.parseLong(cardObj.toString());
                    }
                    cancelReportLoss(cardNumber);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });

        tableView.getColumns().addAll(cardCol, nameCol, balanceCol, statusCol, actionCol);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        getChildren().add(tableView);

        // 底部操作区
        HBox opBox = new HBox(20);
        opBox.setPadding(new Insets(15,0,0,0));
        opBox.setAlignment(Pos.CENTER);
        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 15px; -fx-font-weight: bold;");
        refreshBtn.setPrefWidth(110);
        refreshBtn.setOnAction(e -> refreshList());
        opBox.getChildren().add(refreshBtn);
        Region left = new Region();
        Region right = new Region();
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        opBox.getChildren().add(0, left);
        opBox.getChildren().add(right);
        getChildren().add(opBox);

        refreshList();
    }

    @SuppressWarnings("unchecked")
    private void refreshList() {
        statusLabel.setText("正在加载挂失账号...");
        new Thread(() -> {
            var result = ClientNetworkHelper.findAllLostCards();
            Platform.runLater(() -> {
                Number code = (Number) result.get("code");
                if (result == null || code == null || code.intValue() != 200) {
                    statusLabel.setText("加载失败，请重试。");
                    tableView.getItems().clear();
                    return;
                }
                List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                if (data == null || data.isEmpty()) {
                    statusLabel.setText("当前无挂失账号。");
                    tableView.getItems().clear();
                } else {
                    statusLabel.setText("共 " + data.size() + " 个挂失账号。");
                    tableView.getItems().setAll(data);
                }
            });
        }).start();
    }

    private void cancelReportLoss(long cardNumber) {
        statusLabel.setText("正在解除挂失...");
        new Thread(() -> {
            try {
                var resp = ClientNetworkHelper.cancelReportLoss(cardNumber);
                Platform.runLater(() -> {
                    Alert alert;
                    Object codeObj = resp != null ? resp.get("code") : null;
                    int code = (codeObj instanceof Number) ? ((Number) codeObj).intValue() : -1;
                    if (code == 200) {
                        alert = new Alert(Alert.AlertType.INFORMATION, "解除挂失成功", ButtonType.OK);
                    } else {
                        alert = new Alert(Alert.AlertType.ERROR, "解除挂失失败", ButtonType.OK);
                    }
                    alert.showAndWait();
                    refreshList();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "网络异常，解除挂失失败", ButtonType.OK);
                    alert.showAndWait();
                    refreshList();
                });
            }
        }).start();
    }
}
