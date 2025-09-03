package Client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import Server.model.User;
import Server.model.Student;
import com.google.gson.Gson;

public class MainFrame {
    private Stage stage;
    private String cardNumber;
    private String userType; // "student", "teacher", "admin"
    private Gson gson = new Gson();

    public MainFrame(String cardNumber) {
        this.cardNumber = cardNumber;
        this.userType = getUserType(cardNumber);
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("智慧校园主界面");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox leftBar = new VBox(18);
        leftBar.setPadding(new Insets(10));
        leftBar.setAlignment(Pos.TOP_CENTER);
        leftBar.setStyle("-fx-background-color: #e3f0ff; -fx-background-radius: 8;");

        Button stuManageBtn = new Button("学籍管理");
        stuManageBtn.setPrefWidth(120);
        stuManageBtn.setStyle("-fx-font-size: 15px; -fx-background-radius: 8;");

        // 右侧面板初始为学籍管理
        StudentManagementPanel stuPanel = new StudentManagementPanel(cardNumber, userType);
        root.setCenter(stuPanel);

        // 权限控制
        if ("student".equals(userType) || "admin".equals(userType)) {
            leftBar.getChildren().add(stuManageBtn);
            stuManageBtn.setOnAction(e -> {
                root.setCenter(new StudentManagementPanel(cardNumber, userType));
            });
        }
        // 教师不显示学籍管理

        root.setLeft(leftBar);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    private String getUserType(String cardNumber) {
        if (cardNumber.length() == 9 && cardNumber.startsWith("2")) return "student";
        if (cardNumber.length() == 9 && cardNumber.startsWith("1")) return "teacher";
        try {
            int num = Integer.parseInt(cardNumber);
            if (num <= 1000) return "admin";
        } catch (Exception e) {}
        return "unknown";
    }
}
