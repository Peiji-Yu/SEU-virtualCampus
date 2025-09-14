package Client.coursemgmt.admin;

import Client.ClientNetworkHelper;
import Client.util.UIUtil;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 课程编辑对话框：新增/修改课程。
 * 回调 onSave 接收被保存的 Course（新增时为新对象，编辑时为原对象）。
 */
public class CourseEditDialog {
    public static void open(CourseAdminPanel.Course origin, Consumer<CourseAdminPanel.Course> onSave){
        Stage stage = new Stage();
        UIUtil.applyLogoToStage(stage);
        stage.setTitle(origin==null?"添加课程":"修改课程");
        stage.initModality(Modality.APPLICATION_MODAL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(16));

        TextField id = new TextField(); id.setPromptText("课程号");
        TextField name = new TextField(); name.setPromptText("课程名");
        TextField teacher = new TextField(); teacher.setPromptText("任课教师");
        TextField clazz = new TextField(); clazz.setPromptText("教学班");
        TextField room = new TextField(); room.setPromptText("教室");
        TextField capacity = new TextField(); capacity.setPromptText("容量");
        TextField schedule = new TextField(); schedule.setPromptText("上课时间，如 周二(3-5节)");
        TextArea students = new TextArea(); students.setPromptText("已选学生（以英文逗号分隔）"); students.setPrefRowCount(3);

        if (origin != null){
            id.setText(nv(origin.courseId));
            name.setText(nv(origin.courseName));
            teacher.setText(nv(origin.teacher));
            clazz.setText(nv(origin.clazz));
            room.setText(nv(origin.room));
            capacity.setText(String.valueOf(origin.capacity));
            schedule.setText(nv(origin.schedule));
            if (origin.students!=null && !origin.students.isEmpty()){
                students.setText(String.join(",", origin.students));
            }
        }

        gp.addRow(0, new Label("课程号"), id);
        gp.addRow(1, new Label("课程名"), name);
        gp.addRow(2, new Label("任课教师"), teacher);
        gp.addRow(3, new Label("教学班"), clazz);
        gp.addRow(4, new Label("教室"), room);
        gp.addRow(5, new Label("容量"), capacity);
        gp.addRow(6, new Label("时间"), schedule);
        gp.addRow(7, new Label("已选学生"), students);

        Button save = new Button("保存");
        Button cancel = new Button("取消");
        save.setDefaultButton(true);
        cancel.setCancelButton(true);
        gp.add(save, 0, 8, 2, 1);
        gp.add(cancel, 1, 8);

        save.setOnAction(e -> {
            final String courseId = id.getText().trim();
            final String courseName = name.getText().trim();
            final String teacherName = teacher.getText().trim();
            final String clazzName = clazz.getText().trim();
            final String roomName = room.getText().trim();
            final int courseCapacity;
            try { courseCapacity = Integer.parseInt(capacity.getText().trim()); } catch (Exception ex) { return; }
            final String courseSchedule = schedule.getText().trim();
            final List<String> studentList = Arrays.stream(students.getText().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            final String school = "计算机学院"; // 可扩展为输入项
            final float credit = 1.0f; // 可扩展为输入项
            CourseAdminPanel.Course courseObj = origin == null ? new CourseAdminPanel.Course() : origin;
            courseObj.courseId = courseId;
            courseObj.courseName = courseName;
            courseObj.teacher = teacherName;
            courseObj.clazz = clazzName;
            courseObj.room = roomName;
            courseObj.capacity = courseCapacity;
            courseObj.schedule = courseSchedule;
            courseObj.students = new java.util.ArrayList<>(studentList);
            courseObj.school = school;
            courseObj.credit = credit;
            if (onSave != null) onSave.accept(courseObj);
            stage.close();
        });
        cancel.setOnAction(e -> stage.close());

        stage.setScene(new Scene(gp));
        stage.showAndWait();
    }

    private static String nv(String s){ return s==null?"":s; }
}
