package Client.coursemgmt.admin;

import Server.model.course.TeachingClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

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
            if (!college.isEmpty()) titleText += ", " + college;
            titleText += ")";
        }
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addClassBtn = new Button("新增教学班");
        addClassBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
        addClassBtn.setOnAction(e -> owner.showAddClassForCourse(courseId));

        Button editCourseBtn = new Button("编辑课程");
        editCourseBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");
                                                                                                                           editCourseBtn.setOnAction(e -> CourseCrud.showEditCourseDialog(owner, course));

        Button delCourseBtn = new Button("删除课程");
        delCourseBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        delCourseBtn.setOnAction(e -> CourseCrud.deleteCourseConfirmed(owner, course));

        header.getChildren().addAll(title, spacer, addClassBtn, editCourseBtn, delCourseBtn);

        FlowPane details = new FlowPane();
        details.setHgap(12);
        details.setVgap(12);
        details.setPadding(new Insets(10, 0, 0, 0));
        details.prefWrapLengthProperty().bind(this.widthProperty().subtract(32));
        details.setVisible(false);
        details.setManaged(false);

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
