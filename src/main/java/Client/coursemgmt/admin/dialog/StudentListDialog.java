package Client.coursemgmt.admin.dialog;

import Client.coursemgmt.admin.CourseAdminPanel;
import Client.coursemgmt.admin.service.CourseService;
import Server.model.Response;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 美化后的"查看已选学生"模态窗口
 */
public class StudentListDialog {
    public static void show(CourseAdminPanel owner, String teachingClassUuid, String title) {
        if (teachingClassUuid == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "教学班 UUID 为空，无法查看名单");
            a.showAndWait();
            return;
        }

        StageWrapper stage = new StageWrapper();
        javafx.stage.Stage dialog = stage.createModal("已选学生 - " + (title == null ? teachingClassUuid : title));

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f8f9fa;");

        // 标题栏
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));

        Label titleLabel = new Label("已选学生列表");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #176B3A;");
        headerBox.getChildren().add(titleLabel);

        Label status = new Label("正在加载名单...");
        status.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d; -fx-padding: 5px 0;");

        TableView<StudentRow> tv = new TableView<>();
        tv.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 4px;");

        TableColumn<StudentRow, String> c1 = new TableColumn<>("学号");
        c1.setCellValueFactory(d -> d.getValue().sidProperty());
        c1.setPrefWidth(140);
        c1.setStyle("-fx-alignment: CENTER_LEFT;");

        TableColumn<StudentRow, String> c2 = new TableColumn<>("姓名");
        c2.setCellValueFactory(d -> d.getValue().nameProperty());
        c2.setPrefWidth(120);
        c2.setStyle("-fx-alignment: CENTER_LEFT;");

        TableColumn<StudentRow, String> c3 = new TableColumn<>("专业");
        c3.setCellValueFactory(d -> d.getValue().majorProperty());
        c3.setPrefWidth(160);
        c3.setStyle("-fx-alignment: CENTER_LEFT;");

        TableColumn<StudentRow, String> c4 = new TableColumn<>("学院");
        c4.setCellValueFactory(d -> d.getValue().schoolProperty());
        c4.setPrefWidth(140);
        c4.setStyle("-fx-alignment: CENTER_LEFT;");

        TableColumn<StudentRow, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(100);
        actionCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button dropBtn = new Button("退选");
            {
                dropBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-padding: 6px 12px; -fx-border-radius: 4px; -fx-background-radius: 4px;");
                dropBtn.setOnAction(e -> {
                    StudentRow sr = getTableRow() == null ? null : (StudentRow) getTableRow().getItem();
                    if (sr == null) return;

                    Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
                    conf.setTitle("确认退选");
                    conf.setHeaderText(null);
                    conf.setContentText("确认将该学生从本教学班退选吗？");
                    conf.getDialogPane().getStylesheets().add("/styles/dialog.css");

                    Optional<ButtonType> r = conf.showAndWait();
                    if (!(r.isPresent() && r.get() == ButtonType.OK)) return;

                    dropBtn.setDisable(true);
                    new Thread(() -> {
                        try {
                            String cardStr = sr.cardNumberProperty().get();
                            if (cardStr == null || cardStr.trim().isEmpty()) {
                                Platform.runLater(() -> {
                                    Alert a = new Alert(Alert.AlertType.ERROR, "无法解析一卡通号（为空），已取消退选操作", ButtonType.OK);
                                    a.getDialogPane().getStylesheets().add("/styles/dialog.css");
                                    a.showAndWait();
                                    dropBtn.setDisable(false);
                                });
                                return;
                            }
                            Double cardDouble;
                            try {
                                java.math.BigDecimal bd = new java.math.BigDecimal(cardStr.trim());
                                cardDouble = bd.doubleValue();
                            } catch (Exception parseEx) {
                                try {
                                    long lv = Long.parseLong(cardStr.trim());
                                    cardDouble = (double) lv;
                                } catch (Exception ignore) {
                                    Platform.runLater(() -> {
                                        Alert a = new Alert(Alert.AlertType.ERROR, "无法解析一卡通号: " + cardStr + "，已取消退选操作", ButtonType.OK);
                                        a.getDialogPane().getStylesheets().add("/styles/dialog.css");
                                        a.showAndWait();
                                        dropBtn.setDisable(false);
                                    });
                                    return;
                                }
                            }
                            Response rr = CourseService.sendDropCourse(cardDouble, teachingClassUuid);
                            Platform.runLater(() -> {
                                if (rr.getCode() == 200) {
                                    tv.getItems().remove(sr);
                                    status.setText("共 " + tv.getItems().size() + " 名学生");
                                    owner.loadCourseData();

                                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                                    success.setTitle("操作成功");
                                    success.setHeaderText(null);
                                    success.setContentText("退选成功");
                                    success.getDialogPane().getStylesheets().add("/styles/dialog.css");
                                    success.showAndWait();
                                } else {
                                    Alert a = new Alert(Alert.AlertType.ERROR, "退选失败: " + rr.getMessage(), ButtonType.OK);
                                    a.getDialogPane().getStylesheets().add("/styles/dialog.css");
                                    a.showAndWait();
                                    dropBtn.setDisable(false);
                                }
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> {
                                Alert a = new Alert(Alert.AlertType.ERROR, "网络错误: " + ex.getMessage(), ButtonType.OK);
                                a.getDialogPane().getStylesheets().add("/styles/dialog.css");
                                a.showAndWait();
                                dropBtn.setDisable(false);
                            });
                        }
                    }).start();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(dropBtn);
            }
        });

        tv.getColumns().addAll(c1, c2, c3, c4, actionCol);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox contentBox = new VBox(10);
        contentBox.getChildren().addAll(headerBox, status, tv);
        VBox.setVgrow(tv, Priority.ALWAYS);

        root.getChildren().add(contentBox);

        Scene scene = new Scene(root, 700, 450);
        scene.getStylesheets().add("/styles/dialog.css");
        dialog.setScene(scene);
        dialog.show();

        new Thread(() -> {
            try {
                Map<String, Object> result = CourseService.getTeachingClassStudentsRaw(teachingClassUuid);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("data");
                    ObservableList<StudentRow> rows = FXCollections.observableArrayList();
                    if (students != null) {
                        for (Object stuObj : students) {
                            if (!(stuObj instanceof Map)) continue;
                            Map<?, ?> stu = (Map<?, ?>) stuObj;
                            Object cardObj = stu.get("cardNumber");
                            String cardNum;
                            if (cardObj == null) {
                                cardNum = "";
                            } else if (cardObj instanceof String) {
                                cardNum = (String) cardObj;
                            } else if (cardObj instanceof Number) {
                                cardNum = new java.math.BigDecimal(cardObj.toString()).toPlainString();
                                if (cardNum.indexOf('.') >= 0) { cardNum = cardNum.replaceAll("\\.?0+$", ""); }
                            } else {
                                cardNum = String.valueOf(cardObj);
                            }

                            Object stuNumObj = stu.get("studentNumber");
                            String sid;
                            if (stuNumObj == null) {
                                sid = "";
                            } else if (stuNumObj instanceof String) {
                                sid = (String) stuNumObj;
                            } else if (stuNumObj instanceof Number) {
                                sid = new java.math.BigDecimal(stuNumObj.toString()).toPlainString();
                                if (sid.indexOf('.') >= 0) { sid = sid.replaceAll("\\.?0+$", ""); }
                            } else {
                                sid = String.valueOf(stuNumObj);
                            }

                            String sname = stu.get("name") == null ? "" : String.valueOf(stu.get("name"));
                            String major = stu.get("major") == null ? "" : String.valueOf(stu.get("major"));
                            String school = stu.get("school") == null ? "" : String.valueOf(stu.get("school"));
                            rows.add(new StudentRow(cardNum, sid, sname, major, school));
                        }
                    }
                    Platform.runLater(() -> {
                        status.setText("共 " + rows.size() + " 名学生");
                        tv.setItems(rows);
                    });
                } else {
                    Platform.runLater(() -> status.setText("加载失败: " + String.valueOf(result.get("message"))));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> status.setText("网络错误: " + ex.getMessage()));
            }
        }).start();
    }

    // 内部简单行模型
    private static class StudentRow {
        private final javafx.beans.property.SimpleStringProperty cardNumber;
        private final javafx.beans.property.SimpleStringProperty sid;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty major;
        private final javafx.beans.property.SimpleStringProperty school;

        StudentRow(String cardNumber, String sid, String name, String major, String school) {
            this.cardNumber = new javafx.beans.property.SimpleStringProperty(cardNumber == null ? "" : cardNumber);
            this.sid = new javafx.beans.property.SimpleStringProperty(sid == null ? "" : sid);
            this.name = new javafx.beans.property.SimpleStringProperty(name == null ? "" : name);
            this.major = new javafx.beans.property.SimpleStringProperty(major == null ? "" : major);
            this.school = new javafx.beans.property.SimpleStringProperty(school == null ? "" : school);
        }

        public javafx.beans.property.SimpleStringProperty cardNumberProperty() { return cardNumber; }
        public javafx.beans.property.SimpleStringProperty sidProperty() { return sid; }
        public javafx.beans.property.SimpleStringProperty nameProperty() { return name; }
        public javafx.beans.property.SimpleStringProperty majorProperty() { return major; }
        public javafx.beans.property.SimpleStringProperty schoolProperty() { return school; }
    }

    // 小的辅助封装用于创建模态 Stage（避免在同一文件重复导入 Stage/Modality）
    private static class StageWrapper {
        public javafx.stage.Stage createModal(String title) {
            javafx.stage.Stage s = new javafx.stage.Stage();
            s.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            s.setTitle(title);
            return s;
        }
    }
}
