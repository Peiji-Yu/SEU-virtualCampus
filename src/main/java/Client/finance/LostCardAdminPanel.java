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
import java.util.ArrayList;
import java.util.Map;

/**
 * 管理员挂失管理面板：展示所有挂失一卡通账号，支持解除挂失。
 */
public class LostCardAdminPanel extends VBox {
    private final TableView<Map<String, Object>> tableView;
    private final Label statusLabel;
    // promote columns to fields so they can be resized later
    private final TableColumn<Map<String, Object>, String> cardCol;
    private final TableColumn<Map<String, Object>, String> nameCol;
    private final TableColumn<Map<String, Object>, String> balanceCol;
    private final TableColumn<Map<String, Object>, String> statusCol;
    private final TableColumn<Map<String, Object>, Void> actionCol;

    public LostCardAdminPanel() {
        setSpacing(18);
        setPadding(new Insets(32, 32, 32, 32));
        setStyle("-fx-background-color: #fff; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 12, 0, 0, 2);");

        Label title = new Label("挂失管理");
        title.setFont(Font.font(22));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        getChildren().add(title);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        getChildren().add(statusLabel);

        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.setPrefHeight(480);
        // 基础字体样式
        tableView.setStyle("-fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;");
        java.net.URL styleRes = getClass().getResource("/styles/table-styles.css");
        if (styleRes != null) {
            tableView.getStylesheets().add(styleRes.toExternalForm());
        }

        // 列定义（赋值到字段）
        cardCol = new TableColumn<>("一卡通号");
        cardCol.setCellValueFactory(data -> {
            if (data == null || data.getValue() == null) {
                return new SimpleStringProperty("");
            }
            Object value = data.getValue().get("cardNumber");
            if (value == null) {
                return new SimpleStringProperty("");
            }
            if (value instanceof Number) {
                return new SimpleStringProperty(String.valueOf(((Number) value).longValue()));
            }
            return new SimpleStringProperty(String.valueOf(value));
        });
        cardCol.setPrefWidth(120);

        nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(data -> {
            if (data == null || data.getValue() == null) {
                return new SimpleStringProperty("");
            }
            Object v = data.getValue().get("name");
            return new SimpleStringProperty(v == null ? "" : String.valueOf(v));
        });
        nameCol.setPrefWidth(100);

        balanceCol = new TableColumn<>("余额");
        balanceCol.setCellValueFactory(data -> {
            if (data == null || data.getValue() == null) {
                return new SimpleStringProperty("0.00元");
            }
            Object value = data.getValue().get("balance");
            if (value instanceof Number) {
                double yuan = ((Number) value).doubleValue() / 100.0;
                return new SimpleStringProperty(String.format("%.2f元", yuan));
            }
            return new SimpleStringProperty("0.00元");
        });
        balanceCol.setPrefWidth(90);

        statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(data -> {
            if (data == null || data.getValue() == null) {
                return new SimpleStringProperty("");
            }
            Object v = data.getValue().get("status");
            return new SimpleStringProperty(v == null ? "" : String.valueOf(v));
        });
        statusCol.setPrefWidth(90);

        actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("解除挂失");
            {
                btn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14px;");
                btn.setOnAction(e -> {
                    Map<String, Object> item = getTableView().getItems().get(getIndex());
                    if (item == null) {
                        return;
                    }
                    Object cardObj = item.get("cardNumber");
                    long cardNumber;
                    try {
                        if (cardObj instanceof Number) {
                            cardNumber = ((Number) cardObj).longValue();
                        } else {
                            cardNumber = Long.parseLong(String.valueOf(cardObj));
                        }
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "卡号解析错误，无法解除挂失", ButtonType.OK);
                            alert.showAndWait();
                        });
                        return;
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

        // 防止 varargs 导致的未检查泛型警告，先用 List 再 setAll
        List<TableColumn<Map<String, Object>, ?>> cols = new ArrayList<>();
        cols.add(cardCol);
        cols.add(nameCol);
        cols.add(balanceCol);
        cols.add(statusCol);
        cols.add(actionCol);
        tableView.getColumns().setAll(cols);
        // 列样式统一
        for (TableColumn<?, ?> col : tableView.getColumns()) {
            col.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 8 10 8 10;");
        }

        // 在表格宽度变化时自动调整列宽
        tableView.widthProperty().addListener((obs, oldW, newW) -> adjustColumnWidths());

        VBox.setVgrow(tableView, Priority.ALWAYS);
        getChildren().add(tableView);

        // 底部操作区
        HBox opBox = new HBox(20);
        opBox.setPadding(new Insets(15, 0, 0, 0));
        opBox.setAlignment(Pos.CENTER);
        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle("-fx-background-color: #1D8C4F; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 15px; -fx-font-weight: bold;");
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
            Map<String, Object> result = ClientNetworkHelper.findAllLostCards();
            Platform.runLater(() -> {
                if (result == null) {
                    statusLabel.setText("加载失败，请重试。");
                    tableView.getItems().clear();
                    return;
                }
                Object codeObj = result.get("code");
                int code = (codeObj instanceof Number) ? ((Number) codeObj).intValue() : -1;
                if (code != 200) {
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
                    // 延迟调整列宽以等待表格渲染
                    Platform.runLater(this::adjustColumnWidths);
                }
            });
        }).start();
    }

    private void cancelReportLoss(long cardNumber) {
        statusLabel.setText("正在解除挂失...");
        new Thread(() -> {
            try {
                Map<String, Object> resp = ClientNetworkHelper.cancelReportLoss(cardNumber);
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

    // 根据 tableView 当前宽度按百分比分配列宽
    private void adjustColumnWidths() {
        if (tableView == null || cardCol == null) {
            return;
        }
        double total = tableView.getWidth();
        if (total <= 0) {
            return;
        }

        // 预留滚动条与内边距空间
        double reserve = 40;
        double avail = Math.max(200, total - reserve);

        // 分配比例（可根据需求调整）
        // 一卡通号比例
        double pCard = 0.20;
        // 姓名比例
        double pName = 0.22;
        // 余额比例
        double pBalance = 0.14;
        // 状态比例
        double pStatus = 0.14;
        // 操作列比例
        double pAction = 0.20;

        double wCard = Math.max(80, avail * pCard);
        double wName = Math.max(80, avail * pName);
        double wBalance = Math.max(60, avail * pBalance);
        double wStatus = Math.max(60, avail * pStatus);
        double wAction = Math.max(80, avail * pAction);

        // 防止总和超过可用宽度，按比例缩放
        double sum = wCard + wName + wBalance + wStatus + wAction;
        if (sum > avail) {
            double scale = avail / sum;
            wCard *= scale;
            wName *= scale;
            wBalance *= scale;
            wStatus *= scale;
            wAction *= scale;
        } else if (sum < avail) {
            // 将多余空间分配给最后一列，避免右侧留白
            double extra = avail - sum;
            wAction += extra;
        }

        cardCol.setPrefWidth(wCard);
        nameCol.setPrefWidth(wName);
        balanceCol.setPrefWidth(wBalance);
        statusCol.setPrefWidth(wStatus);
        actionCol.setPrefWidth(wAction);
    }
    public void refreshData() {
        refreshList();
    }
}
