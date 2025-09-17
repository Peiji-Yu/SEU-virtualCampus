package Client.coursemgmt.admin;

import Server.model.Response;
import Server.model.course.TeachingClass;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;


/**
 * 将原来 CourseAdminPanel 中 createTeachingClassCard 的逻辑独立成组件，行为通过 CourseAdminPanel 回调
 */
public class TeachingClassCard extends VBox {
    private final TeachingClass tc;
    private final CourseAdminPanel owner;

    public TeachingClassCard(TeachingClass tc, CourseAdminPanel owner, FlowPane parent) {
        super(8);
        this.tc = tc;
        this.owner = owner;
        initUI(parent);
    }

    private void initUI(FlowPane parent) {
        this.setPadding(new Insets(10));
        String normalStyle = "-fx-background-radius: 6; -fx-border-color: #cfe4ff; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: linear-gradient(to bottom, #eaf4ff, #dceeff);";
        String hoverStyle = "-fx-background-radius: 6; -fx-border-color: #9fd0ff; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: linear-gradient(to bottom, #d6eeff, #c0e6ff); -fx-effect: dropshadow(gaussian, rgba(30,80,150,0.12), 8,0,0,2);";
        this.setStyle(normalStyle);
        this.setOnMouseEntered(e -> this.setStyle(hoverStyle));
        this.setOnMouseExited(e -> this.setStyle(normalStyle));
        this.setPrefHeight(Region.USE_COMPUTED_SIZE);
        this.setMinHeight(Region.USE_COMPUTED_SIZE);

        try {
            this.prefWidthProperty().bind(parent.widthProperty()
                    .subtract(32)
                    .subtract(parent.getHgap() * 3)
                    .divide(4));
            this.minWidthProperty().bind(this.prefWidthProperty().multiply(0.75));
            this.maxWidthProperty().bind(this.prefWidthProperty().multiply(1.05));
        } catch (Exception ignored) {
            this.setPrefWidth(240);
        }

        Label teacher = new Label("教师: " + (tc.getTeacherName() == null ? "未知" : tc.getTeacherName()));
        teacher.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        teacher.setWrapText(true);

        Label schedule = new Label("时间: " + (tc.getSchedule() == null ? "未设置" : tc.getSchedule()));
        schedule.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        schedule.setWrapText(true);

        Label place = new Label("地点: " + (tc.getPlace() == null ? "未设置" : tc.getPlace()));
        place.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        place.setWrapText(true);

        int sel = tc.getSelectedCount() == null ? 0 : tc.getSelectedCount();
        int cap = tc.getCapacity() == null ? 0 : tc.getCapacity();
        Label capacity = new Label("容量: " + sel + "/" + cap);
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        capacity.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewListBtn = new Button("查看名单");
        viewListBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4e8cff;");
        viewListBtn.setOnAction(e -> owner.showStudentListDialog(tc.getUuid(), tc.getCourseId() + " " + (tc.getCourse() == null ? "" : tc.getCourse().getCourseName())));

        Button addStudentBtn = new Button("添加学生");
        addStudentBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addStudentBtn.setOnAction(e -> {
            TextInputDialog inputDialog = new TextInputDialog();
            inputDialog.setTitle("添加学生到教学班");
            inputDialog.setHeaderText("请输入学生一卡通号");
            inputDialog.setContentText("一卡通号:");
            inputDialog.showAndWait().ifPresent(cardNumber -> {
                String cardNum = cardNumber == null ? "" : cardNumber.trim();
                if (cardNum.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "一卡通号不能为空", ButtonType.OK);
                    a.showAndWait();
                    return;
                }
                if (!cardNum.matches("\\d+")) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "一卡通号必须为纯数字", ButtonType.OK);
                    a.showAndWait();
                    return;
                }
                new Thread(() -> {
                    try {
                        long cardLong = Long.parseLong(cardNum);
                        Response rr = CourseService.sendSelectCourse(cardLong, tc.getUuid());
                        Platform.runLater(() -> {
                            if (rr.getCode() == 200) {
                                Alert a = new Alert(Alert.AlertType.INFORMATION, "添加成功", ButtonType.OK);
                                a.showAndWait();
                                owner.loadCourseData();
                            } else {
                                Alert a = new Alert(Alert.AlertType.ERROR, "添加失败: " + rr.getMessage(), ButtonType.OK);
                                a.showAndWait();
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            Alert a = new Alert(Alert.AlertType.ERROR, "网络异常: " + ex.getMessage(), ButtonType.OK);
                            a.showAndWait();
                        });
                    }
                }).start();
            });
        });

        Button editBtn = new Button("编辑");
        editBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");
        editBtn.setOnAction(e -> TeachingClassCrud.showEditTeachingClassDialog(owner, tc));

        Button delBtn = new Button("删除");
        delBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        delBtn.setOnAction(e -> TeachingClassCrud.deleteTeachingClassConfirmed(owner, tc));

        HBox btnRow = new HBox(8, spacer, viewListBtn, addStudentBtn, editBtn, delBtn);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        this.getChildren().addAll(teacher, schedule, place, capacity, btnRow);
        this.requestLayout();
    }
}
