package Client.studentmgmt.admin;

import Client.studentmgmt.service.StudentClientService;
import Client.util.UIUtil;
import Server.model.student.Student;
import Server.model.student.Gender;
import Server.model.student.StudentStatus;
import Server.model.student.PoliticalStatus;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;

/**
 * 学生新增/编辑对话框 (admin 包)。固定尺寸 720x460。
 * 作者: @Msgo-srAm
 */
public final class StudentEditDialog {
    private StudentEditDialog() {}
    private static final int WIDTH = 720;
    private static final int HEIGHT = 460;
    private static final String BACKGROUND_STYLE = "-fx-background-color: linear-gradient(to bottom right, #e3f0ff, #f8fbff);";
    private static final String FIELD_STYLE = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #b3c6e7; -fx-padding: 4 8 4 8; -fx-font-size: 13px;";
    private static final String LABEL_STYLE = "-fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-font-size: 13px;";
    private static final String BTN_OK_STYLE = "-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 26 8 26;";
    private static final String BTN_CANCEL_STYLE = "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 26 8 26;";
    private static final String ID_PATTERN = "\\d{17}[0-9Xx]";
    private static final String STU_NUM_PATTERN = "\\d{8}";

    public static void open(Student existing, Runnable onSuccess, StudentClientService service) {
        if (service == null) {alert(Alert.AlertType.ERROR, "服务错误", "无法提交数据");return;}
        Stage dialog = new Stage();dialog.initModality(Modality.APPLICATION_MODAL);
        UIUtil.applyLogoToStage(dialog);
        dialog.setTitle(existing == null ? "添加学生" : "修改学生信息");
        TextField cardField = new TextField(existing == null ? "" : String.valueOf(existing.getCardNumber()));cardField.setDisable(true);
        TextField stuNumField = new TextField(existing == null ? "" : existing.getStudentNumber());
        TextField nameField = new TextField(existing == null ? "" : existing.getName()); if(existing!=null){nameField.setDisable(true);}
        TextField idField = new TextField(existing == null ? "" : existing.getIdentity()); if(existing!=null){idField.setDisable(true);}
        ComboBox<Gender> genderCombo = new ComboBox<>(); genderCombo.getItems().addAll(Gender.values());
        if(existing!=null && existing.getGender()!=null){genderCombo.setValue(existing.getGender());genderCombo.setDisable(true);} else {genderCombo.setValue(Gender.MALE);}
        DatePicker birthPicker = new DatePicker(); if(existing!=null && existing.getBirth()!=null){birthPicker.setValue(existing.getBirth().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());birthPicker.setDisable(true);}
        ComboBox<StudentStatus> statusCombo = new ComboBox<>(); statusCombo.getItems().addAll(StudentStatus.values()); statusCombo.setValue(existing!=null && existing.getStatus()!=null? existing.getStatus(): StudentStatus.ENROLLED);
        DatePicker enrollPicker = new DatePicker(); if(existing!=null && existing.getEnrollment()!=null){enrollPicker.setValue(existing.getEnrollment().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());}
        ComboBox<PoliticalStatus> politicalCombo = new ComboBox<>(); politicalCombo.getItems().addAll(PoliticalStatus.values()); politicalCombo.setValue(existing!=null && existing.getPoliticalStat()!=null? existing.getPoliticalStat(): PoliticalStatus.MASSES);
        TextField birthPlaceField = new TextField(existing==null?"":existing.getBirthPlace());
        TextField majorField = new TextField(existing==null?"":existing.getMajor());
        TextField schoolField = new TextField(existing==null?"":existing.getSchool());
        List<Control> styleTargets = Arrays.asList(stuNumField,nameField,idField,genderCombo,birthPicker,statusCombo,enrollPicker,politicalCombo,birthPlaceField,majorField,schoolField);
        for(Control c: styleTargets){if(!c.isDisabled()){c.setStyle(FIELD_STYLE);}}
        GridPane grid = new GridPane();grid.setHgap(18);grid.setVgap(14);grid.setAlignment(Pos.CENTER);grid.setStyle("-fx-padding: 10 0 10 0;");
        Function<String, Label> lab = t->{Label lb=new Label(t);lb.setStyle(LABEL_STYLE);return lb;};int r=0;
        addRow(grid,r++,lab.apply("一卡通号:"),cardField,lab.apply("学号:"),stuNumField);
        addRow(grid,r++,lab.apply("姓名:"),nameField,lab.apply("身份证号:"),idField);
        addRow(grid,r++,lab.apply("性别:"),genderCombo,lab.apply("出生日期:"),birthPicker);
        addRow(grid,r++,lab.apply("学籍状态:"),statusCombo,lab.apply("入学时间:"),enrollPicker);
        addRow(grid,r++,lab.apply("政治面貌:"),politicalCombo,lab.apply("籍贯:"),birthPlaceField);
        addRow(grid,r++,lab.apply("专业:"),majorField,lab.apply("学院:"),schoolField);
        Button save = new Button("保存"); save.setStyle(BTN_OK_STYLE);
        Button cancel = new Button("取消"); cancel.setStyle(BTN_CANCEL_STYLE);
        HBox btnBox = new HBox(30, save, cancel); btnBox.setAlignment(Pos.CENTER); btnBox.setStyle("-fx-padding: 15 0 0 0;");
        cancel.setOnAction(e->dialog.close());
        save.setOnAction(e->{if(!validate(existing,nameField,idField,stuNumField,genderCombo,birthPicker,statusCombo,enrollPicker,politicalCombo)){return;} Student ns = buildStudent(existing,nameField,idField,stuNumField,genderCombo,birthPicker,statusCombo,enrollPicker,politicalCombo,birthPlaceField,majorField,schoolField); new Thread(()->doSubmit(existing,ns,service,dialog,onSuccess)).start();});
        VBox container = new VBox(18, grid, btnBox);container.setAlignment(Pos.TOP_CENTER);container.setStyle("-fx-padding: 26 34 28 34;");
        ScrollPane sp = new ScrollPane(container);sp.setFitToWidth(true);sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);sp.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        StackPane root = new StackPane(sp);root.setStyle(BACKGROUND_STYLE);
        Scene scene = new Scene(root, WIDTH, HEIGHT); dialog.setScene(scene); dialog.setResizable(false);
        dialog.setMinWidth(WIDTH);dialog.setMaxWidth(WIDTH);dialog.setMinHeight(HEIGHT);dialog.setMaxHeight(HEIGHT);dialog.showAndWait();
    }
    private static void doSubmit(Student existing, Student ns, StudentClientService service, Stage dialog, Runnable onSuccess){
        try {boolean ok = (existing==null)? service.addStudent(ns): service.updateStudent(ns); Platform.runLater(()->{if(ok){alert(Alert.AlertType.INFORMATION,"成功", existing==null?"学生添加成功":"学生信息更新成功");dialog.close(); if(onSuccess!=null){onSuccess.run();}} else {alert(Alert.AlertType.ERROR,"失败", existing==null?"学生添加失败":"学生信息更新失败");}});} catch (Exception ex){Platform.runLater(()->alert(Alert.AlertType.ERROR,"操作失败", ex.getMessage()));}
    }
    private static boolean validate(Student existing, TextField nameField, TextField idField, TextField stuNumField, ComboBox<Gender> genderCombo, DatePicker birthPicker, ComboBox<StudentStatus> statusCombo, DatePicker enrollPicker, ComboBox<PoliticalStatus> politicalCombo){
        if(nameField.getText().trim().isEmpty()){alert(Alert.AlertType.WARNING,"输入错误","姓名不能为空");return false;}
        if(existing==null && idField.getText().trim().isEmpty()){alert(Alert.AlertType.WARNING,"输入错误","身份证号不能为空");return false;}
        if(stuNumField.getText().trim().isEmpty()){alert(Alert.AlertType.WARNING,"输入错误","学号不能为空");return false;}
        if(existing==null && birthPicker.getValue()==null){alert(Alert.AlertType.WARNING,"输入错误","请选择出生日期");return false;}
        if(enrollPicker.getValue()==null){alert(Alert.AlertType.WARNING,"输入错误","请选择入学时间");return false;}
        if(existing==null && genderCombo.getValue()==null){alert(Alert.AlertType.WARNING,"输入错误","请选择性别");return false;}
        if(statusCombo.getValue()==null){alert(Alert.AlertType.WARNING,"输入错误","请选择学籍状态");return false;}
        if(politicalCombo.getValue()==null){alert(Alert.AlertType.WARNING,"输入错误","请选择政治面貌");return false;}
        String idVal=idField.getText().trim(); if(!idVal.isEmpty() && !idVal.matches(ID_PATTERN)){alert(Alert.AlertType.WARNING,"格式错误","身份证号需18位：前17位数字最后一位数字或X");return false;}
        if(!stuNumField.getText().trim().matches(STU_NUM_PATTERN)){alert(Alert.AlertType.WARNING,"格式错误","学号必须为8位数字");return false;}
        return true;
    }
    private static Student buildStudent(Student existing, TextField nameField, TextField idField, TextField stuNumField, ComboBox<Gender> genderCombo, DatePicker birthPicker, ComboBox<StudentStatus> statusCombo, DatePicker enrollPicker, ComboBox<PoliticalStatus> politicalCombo, TextField birthPlaceField, TextField majorField, TextField schoolField){
        Student ns = new Student();
        if(existing==null){ns.setIdentity(idField.getText().trim());ns.setGender(genderCombo.getValue());ns.setBirth(toUtcDate(birthPicker.getValue()));} else {ns.setCardNumber(existing.getCardNumber());ns.setIdentity(existing.getIdentity());ns.setGender(existing.getGender());ns.setBirth(existing.getBirth());}
        ns.setName(nameField.getText().trim());ns.setStudentNumber(stuNumField.getText().trim());ns.setMajor(majorField.getText().trim());ns.setSchool(schoolField.getText().trim());ns.setBirthPlace(birthPlaceField.getText().trim());ns.setStatus(statusCombo.getValue());ns.setPoliticalStat(politicalCombo.getValue());ns.setEnrollment(toUtcDate(enrollPicker.getValue()));
        return ns;
    }
    private static java.util.Date toUtcDate(java.time.LocalDate ld){if(ld==null){return null;} return java.util.Date.from(ld.atStartOfDay(ZoneOffset.UTC).toInstant());}
    private static void addRow(GridPane g,int row,Label l1,Control c1,Label l2,Control c2){g.add(l1,0,row);g.add(c1,1,row);g.add(l2,2,row);g.add(c2,3,row);GridPane.setHgrow(c1, Priority.ALWAYS);GridPane.setHgrow(c2, Priority.ALWAYS);c1.setMaxWidth(Double.MAX_VALUE);c2.setMaxWidth(Double.MAX_VALUE);}
    private static void alert(Alert.AlertType type,String title,String msg){
        Alert a=new Alert(type,msg,ButtonType.OK);
        a.setHeaderText(title);
        UIUtil.applyLogoToAlert(a);
        a.showAndWait();
    }
}
