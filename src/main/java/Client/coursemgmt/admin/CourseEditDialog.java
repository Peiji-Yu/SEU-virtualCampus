package Client.coursemgmt.admin;

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
            id.setText(nv(origin.id));
            name.setText(nv(origin.name));
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

        Button ok = new Button("保存");
        Button cancel = new Button("取消");
        ok.setDefaultButton(true);
        cancel.setCancelButton(true);
        gp.add(ok, 0, 8);
        gp.add(cancel, 1, 8);

        ok.setOnAction(e -> {
            try {
                int cap = Integer.parseInt(capacity.getText().trim());
                if (origin == null){
                    CourseAdminPanel.Course c = new CourseAdminPanel.Course();
                    c.id = id.getText().trim();
                    c.name = name.getText().trim();
                    c.teacher = teacher.getText().trim();
                    c.clazz = clazz.getText().trim();
                    c.room = room.getText().trim();
                    c.capacity = cap;
                    c.schedule = schedule.getText().trim();
                    if (!students.getText().trim().isEmpty()){
                        c.students = Arrays.asList(students.getText().trim().split(","));
                    }
                    onSave.accept(c);
                } else {
                    origin.id = id.getText().trim();
                    origin.name = name.getText().trim();
                    origin.teacher = teacher.getText().trim();
                    origin.clazz = clazz.getText().trim();
                    origin.room = room.getText().trim();
                    origin.capacity = cap;
                    origin.schedule = schedule.getText().trim();
                    if (!students.getText().trim().isEmpty()){
                        origin.students = Arrays.asList(students.getText().trim().split(","));
                    } else {
                        origin.students = new java.util.ArrayList<>();
                    }
                    onSave.accept(origin);
                }
                stage.close();
            } catch (Exception ex){
                // 简化：容量解析失败等异常直接忽略或可弹窗
                stage.close();
            }
        });
        cancel.setOnAction(e -> stage.close());

        stage.setScene(new Scene(gp));
        stage.showAndWait();
    }

    private static String nv(String s){ return s==null?"":s; }
}
