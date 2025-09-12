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

        Button save = new Button("保存");
        Button cancel = new Button("取消");
        save.setDefaultButton(true);
        cancel.setCancelButton(true);
        gp.add(save, 0, 8, 2, 1);
        gp.add(cancel, 1, 8);

        save.setOnAction(e -> {
            final String courseId = id.getText().trim();
            final String courseName = name.getText().trim();
            final String school = "计算机学院"; // 可扩展为输入项
            final double credit;
            try { credit = Double.parseDouble(capacity.getText().trim()); } catch (Exception ex) { return; }
            // 构造课程对象
            java.util.Map<String, Object> course = new java.util.HashMap<>();
            course.put("courseId", courseId);
            course.put("courseName", courseName);
            course.put("school", school);
            course.put("credit", credit);
            new Thread(() -> {
                try {
                    String resp;
                    if (origin == null) {
                        // 新增课程
                        resp = ClientNetworkHelper.addCourse(course);
                    } else {
                        // 编辑课程
                        java.util.Map<String, Object> updates = new java.util.HashMap<>();
                        updates.put("courseName", courseName);
                        updates.put("credit", credit);
                        resp = ClientNetworkHelper.updateCourse(courseId, updates);
                    }
                    java.util.Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, java.util.Map.class);
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(result.get("success").equals(Boolean.TRUE) ? javafx.scene.control.Alert.AlertType.INFORMATION : javafx.scene.control.Alert.AlertType.ERROR,
                                (String) result.get("message"));
                        alert.showAndWait();
                        if (result.get("success").equals(Boolean.TRUE)) {
                            stage.close();
                            if (onSave != null) onSave.accept(origin);
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "操作失败: " + ex.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        });
        cancel.setOnAction(e -> stage.close());

        stage.setScene(new Scene(gp));
        stage.showAndWait();
    }

    private static String nv(String s){ return s==null?"":s; }
}
