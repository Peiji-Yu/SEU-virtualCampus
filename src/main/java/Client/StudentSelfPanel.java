package Client;

import Server.model.student.Student;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;

/**
 * 学生个人学籍查看面板（只读）
 */
public class StudentSelfPanel extends VBox {
    private final String cardNumber;
    private final StudentClientService service = new StudentClientService();

    public StudentSelfPanel(String cardNumber) {
        this.cardNumber = cardNumber;
        setPadding(new Insets(18));
        setSpacing(10);
        init();
    }

    private void init() {
        Label title = new Label("我的学籍信息");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        getChildren().add(title);

        Label loadingLabel = new Label("正在加载学籍信息...");
        loadingLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        getChildren().add(loadingLabel);

        new Thread(() -> {
            try {
                Student stu = service.getSelf(Integer.parseInt(cardNumber));
                Platform.runLater(() -> {
                    getChildren().remove(loadingLabel);
                    if (stu != null) display(stu); else showError("学籍信息获取失败，请稍后重试");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    getChildren().remove(loadingLabel);
                    showError("网络连接失败: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void showError(String msg) {
        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        getChildren().add(err);
    }

    private void display(Student s) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 8; -fx-padding: 15;");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        Label baseTitle = new Label("基本信息");
        baseTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        box.getChildren().add(baseTitle);

        GridPane baseGrid = new GridPane();
        baseGrid.setHgap(20);
        baseGrid.setVgap(8);
        baseGrid.setStyle("-fx-padding: 10 0 10 0;");
        int r = 0;
        addRow(baseGrid, r++, "姓名:", s.getName());
        addRow(baseGrid, r++, "一卡通号:", String.valueOf(s.getCardNumber()));
        addRow(baseGrid, r++, "学号:", s.getStudentNumber());
        addRow(baseGrid, r++, "身份证号:", s.getIdentity());
        if (s.getGender()!=null) addRow(baseGrid, r++, "性别:", s.getGender().getDescription());
        if (s.getBirth()!=null) addRow(baseGrid, r++, "出生日期:", df.format(s.getBirth()));
        addRow(baseGrid, r++, "籍贯:", s.getBirthPlace());
        if (s.getPoliticalStat()!=null) addRow(baseGrid, r++, "政治面貌:", s.getPoliticalStat().getDescription());
        box.getChildren().add(baseGrid);

        Label studyTitle = new Label("学籍信息");
        studyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-padding: 10 0 0 0;");
        box.getChildren().add(studyTitle);

        GridPane studyGrid = new GridPane();
        studyGrid.setHgap(20);
        studyGrid.setVgap(8);
        studyGrid.setStyle("-fx-padding: 10 0 10 0;");
        r = 0;
        addRow(studyGrid, r++, "学院:", s.getSchool());
        addRow(studyGrid, r++, "专业:", s.getMajor());
        if (s.getStatus()!=null) addRow(studyGrid, r++, "学籍状态:", s.getStatus().getDescription());
        if (s.getEnrollment()!=null) addRow(studyGrid, r++, "入学日期:", df.format(s.getEnrollment()));
        box.getChildren().add(studyGrid);

        Button refresh = new Button("刷新信息");
        refresh.setStyle("-fx-font-size: 14px; -fx-background-color: #4e8cff; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16 8 16;");
        refresh.setOnAction(e -> { getChildren().clear(); init(); });
        VBox refreshBox = new VBox(refresh);
        refreshBox.setStyle("-fx-padding: 10 0 0 0;");
        box.getChildren().add(refreshBox);

        getChildren().add(box);
    }

    private void addRow(GridPane grid, int row, String label, String value) {
        if (value == null) value = "未设置";
        javafx.scene.control.Label l1 = new javafx.scene.control.Label(label);
        l1.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-min-width: 80;");
        javafx.scene.control.Label l2 = new javafx.scene.control.Label(value);
        l2.setStyle("-fx-text-fill: #333; -fx-font-size: 14px;");
        grid.add(l1,0,row); grid.add(l2,1,row);
    }
}

