package Client.panel.course.coursemgmt.card;

import Client.panel.course.coursemgmt.CourseAdminPanel;
import Client.panel.course.coursemgmt.service.CourseCrud;
import Client.model.course.TeachingClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 课程卡片（包含课程头部与对应教学班的 FlowPane 列表）
 */
public class CourseCard extends VBox {
    public CourseCard(Map<String, Object> course, List<TeachingClass> tcs, CourseAdminPanel owner) {
        super(10);
        initUI(course, tcs, owner);
    }

    private void initUI(Map<String, Object> course, List<TeachingClass> tcs, CourseAdminPanel owner) {
        String courseId = String.valueOf(course.get("courseId"));
        String courseName = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
        Object credObj = course.get("courseCredit") != null ? course.get("courseCredit") : course.get("credit");
        Object schoolObj = course.get("college") != null ? course.get("college") : course.get("school");
        String courseCredit = credObj == null ? "" : String.valueOf(credObj);
        String college = schoolObj == null ? "" : String.valueOf(schoolObj);

        this.setPadding(new Insets(12));
        this.setPrefWidth(980);
        this.setMaxWidth(Double.MAX_VALUE);
        this.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 10;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        String titleText = courseId + "  " + courseName;
        if (!courseCredit.isEmpty() || !college.isEmpty()) {
            titleText += "  (" + (courseCredit.isEmpty() ? "?" : courseCredit) + "学分";
            if (!college.isEmpty()) {
                titleText += ", " + college;
            }
            titleText += ")";
        }
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 添加教学班按钮
        Button addClassBtn = new Button();
        Image addImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/增加.png")));
        ImageView addView = new ImageView(addImg);
        addView.setFitWidth(18);
        addView.setFitHeight(18);
        addView.setPreserveRatio(true);
        addView.setSmooth(true);
        addClassBtn.setGraphic(addView);
        addClassBtn.setTooltip(new Tooltip("添加教学班"));
        addClassBtn.setStyle("-fx-background-color: transparent;");
        addClassBtn.setPrefSize(26, 26);
        addClassBtn.setOnAction(e -> owner.showAddClassForCourse(courseId));
        addClassBtn.setOnMouseEntered(e ->addClassBtn.setStyle("-fx-background-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, #1E1F22, 20, 0, 0, 0);"));
        addClassBtn.setOnMouseExited(e ->addClassBtn.setStyle("-fx-background-color: transparent;"));

        // 编辑课程按钮
        Button editCourseBtn = new Button();
        Image editImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/编辑.png")));
        ImageView editView = new ImageView(editImg);
        editView.setFitWidth(18);
        editView.setFitHeight(18);
        editView.setPreserveRatio(true);
        editView.setSmooth(true);
        editCourseBtn.setGraphic(editView);
        editCourseBtn.setTooltip(new Tooltip("编辑课程"));
        editCourseBtn.setStyle("-fx-background-color: transparent;");
        editCourseBtn.setPrefSize(26, 26);
        editCourseBtn.setOnAction(e -> CourseCrud.showEditCourseDialog(owner, course));
        editCourseBtn.setOnMouseEntered(e ->editCourseBtn.setStyle("-fx-background-color: transparent;" +
                "-fx-effect: dropshadow(gaussian, #1E1F22, 20, 0, 0, 0);"));
        editCourseBtn.setOnMouseExited(e ->editCourseBtn.setStyle("-fx-background-color: transparent;"));

        // 删除课程按钮
        Button delCourseBtn = new Button();
        Image delImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/删除.png")));
        ImageView delView = new ImageView(delImg);
        delView.setFitWidth(18);
        delView.setFitHeight(18);
        delView.setPreserveRatio(true);
        delView.setSmooth(true);
        delCourseBtn.setGraphic(delView);
        delCourseBtn.setTooltip(new Tooltip("删除课程"));
        delCourseBtn.setStyle("-fx-background-color: transparent;");
        delCourseBtn.setPrefSize(26, 26);
        delCourseBtn.setOnAction(e -> CourseCrud.deleteCourseConfirmed(owner, course));
        delCourseBtn.setOnMouseEntered(e ->delCourseBtn.setStyle("-fx-background-color: transparent;" +
                "-fx-effect: dropshadow(gaussian, #1E1F22, 20, 0, 0, 0);"));
        delCourseBtn.setOnMouseExited(e ->delCourseBtn.setStyle("-fx-background-color: transparent;"));

        header.getChildren().addAll(title, spacer, addClassBtn, editCourseBtn, delCourseBtn);

        FlowPane details = new FlowPane();
        details.setHgap(12);
        details.setVgap(12);
        details.setPadding(new Insets(10, 0, 0, 0));
        details.prefWrapLengthProperty().bind(this.widthProperty().subtract(32));
        details.setVisible(false);
        details.setManaged(false);

        delCourseBtn.setPrefSize(26, 26);
        if (tcs != null) {
            for (TeachingClass tc : tcs) {
                details.getChildren().add(new TeachingClassCard(tc, owner, details));
            }
        }

        if (tcs == null || tcs.isEmpty()) {
            Label no = new Label("该课程当前无教学班");
            no.setStyle("-fx-text-fill: #888888;");
            details.getChildren().add(no);
        }

        header.setOnMouseClicked(e -> {
            boolean showing = details.isVisible();
            details.setVisible(!showing);
            details.setManaged(!showing);
            try {
                details.applyCss();
                details.layout();
                this.applyCss();
                this.layout();
            } catch (Exception ex) {
                details.requestLayout();
                this.requestLayout();
            }
        });

        this.getChildren().addAll(header, details);
    }
}
