package Client.panel.course.coursemgmt.dialog;

import Client.panel.course.coursemgmt.CourseAdminPanel;
import Client.panel.course.coursemgmt.service.CourseService;
import Client.model.Response;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * 将"添加学生"对话框逻辑从 TeachingClassCard 中抽离出来，便于复用和测试。
 * 使用与AddCourseDialog相同的样式进行美化
 */
public class AddStudentDialog {
    public static void showAndHandle(CourseAdminPanel owner, Client.model.course.TeachingClass tc) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.setTitle("添加学生到教学班");
        dialog.setHeaderText("请填写学生信息");
        dialog.getDialogPane().getStylesheets().add("/styles/dialog.css");

        // 创建输入字段
        TextField cardField = createTextField("请输入学生一卡通号");

        // 创建标签
        Label cardLabel = new Label("一卡通号:");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 25, 15, 25));
        grid.setAlignment(Pos.CENTER);

        // 添加标签和输入框
        grid.add(cardLabel, 0, 0);
        grid.add(cardField, 1, 0);

        // 设置列约束
        GridPane.setHgrow(cardField, Priority.ALWAYS);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(grid);

        dialog.getDialogPane().setContent(content);

        ButtonType addType = new ButtonType("添加", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        // 设置结果转换器
        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                String cardNum = cardField.getText() == null ? "" : cardField.getText().trim();

                // 验证输入
                if (cardNum.isEmpty()) {
                    showErrorAlert("验证失败", "一卡通号不能为空");
                    return null;
                }
                if (!cardNum.matches("\\d+")) {
                    showErrorAlert("输入错误", "一卡通号必须为纯数字");
                    return null;
                }
                return cardNum;
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cardNumber -> {
            new Thread(() -> {
                try {
                    long cardLong = Long.parseLong(cardNumber);
                    Response rr = CourseService.sendSelectCourse(cardLong, tc.getUuid());
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            showSuccessAlert("操作成功", "学生已成功添加到教学班");
                            if (owner != null) owner.loadCourseData();
                        } else {
                            showErrorAlert("操作失败", "添加失败: " + rr.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        showErrorAlert("网络异常", "网络连接异常: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }

    private static TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(250);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}