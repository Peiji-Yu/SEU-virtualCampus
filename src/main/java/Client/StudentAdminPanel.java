package Client;

import Server.model.student.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.*;

/**
 * 管理员学籍管理面板 (增删改查)
 */
public class StudentAdminPanel extends VBox {
    private final StudentClientService service = new StudentClientService();

    // 样式常量
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String PRIMARY_HOVER_COLOR = "#3d7bff";
    private static final String CARD_BACKGROUND = "#ffffff";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final double BUTTON_WIDTH = 110; // 新增：统一按钮宽度

    private TableView<Student> table;

    public StudentAdminPanel() {
        setPadding(new Insets(18));
        setSpacing(10);
        init();
    }

    private void init() {
        Label title = new Label("学籍管理");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");
        getChildren().add(title);

        HBox searchBox = createSearchBox();
        getChildren().add(searchBox);

        table = createTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);

        HBox opBox = createOpBox();
        getChildren().add(opBox);

        loadAll();
    }

    private HBox createSearchBox() {
        HBox box = new HBox(12);
        box.setPadding(new Insets(15));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: " + CARD_BACKGROUND + "; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8,0,0,2);");

        Label l = new Label("搜索条件:");
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 14px;");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("按姓名", "按学号", "按一卡通号");
        typeCombo.setValue("按姓名");
        typeCombo.setPrefWidth(120);
        typeCombo.setPrefHeight(40);
        typeCombo.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 1;");

        TextField field = new TextField();
        field.setPromptText("请输入搜索内容");
        field.setPrefHeight(40);
        field.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 1; -fx-font-size: 14px;");
        HBox.setHgrow(field, Priority.ALWAYS);

        CheckBox fuzzy = new CheckBox("模糊搜索");
        fuzzy.setSelected(true);
        fuzzy.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 14px;");

        HBox btnArea = new HBox(8); btnArea.setAlignment(Pos.CENTER_RIGHT);
        Button searchBtn = new Button("搜索");
        setPrimaryButtonStyle(searchBtn);
        uniformButtonWidth(searchBtn);
        Button clearBtn = new Button("清空");
        setPrimaryButtonStyle(clearBtn);
        uniformButtonWidth(clearBtn);
        btnArea.getChildren().addAll(searchBtn, clearBtn);

        typeCombo.valueProperty().addListener((o, ov, nv) -> {
            if ("按姓名".equals(nv)) {
                if (!box.getChildren().contains(fuzzy)) {
                    int idx = box.getChildren().indexOf(btnArea);
                    box.getChildren().add(idx, fuzzy);
                }
                fuzzy.setSelected(true);
            } else {
                box.getChildren().remove(fuzzy);
            }
        });

        searchBtn.setOnAction(e -> {
            String v = field.getText().trim();
            if (v.isEmpty()) { showAlert(Alert.AlertType.WARNING, "输入提示", "请输入搜索内容"); return; }
            String searchType = mapSearchType(typeCombo.getValue());
            boolean fz = "按姓名".equals(typeCombo.getValue()) && fuzzy.isSelected();
            doSearch(searchType, v, fz);
        });
        clearBtn.setOnAction(e -> { field.clear(); loadAll(); });

        box.getChildren().addAll(l, typeCombo, field, fuzzy, btnArea);
        return box;
    }

    private TableView<Student> createTable() {
        TableView<Student> t = new TableView<>();
        t.setPrefHeight(420);
        t.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        t.setTableMenuButtonVisible(true); // 允许右上角菜单显示/隐藏列
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");

        TableColumn<Student, String> cCard = new TableColumn<>("一卡通号");
        cCard.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCardNumber())));
        cCard.setPrefWidth(90);

        TableColumn<Student, String> cName = new TableColumn<>("姓名");
        cName.setCellValueFactory(d -> new SimpleStringProperty(val(d.getValue().getName())));
        cName.setPrefWidth(90);

        TableColumn<Student, String> cId = new TableColumn<>("身份证号");
        cId.setCellValueFactory(d -> new SimpleStringProperty(val(d.getValue().getIdentity())));
        cId.setPrefWidth(170);

        TableColumn<Student, String> cStuNo = new TableColumn<>("学号");
        cStuNo.setCellValueFactory(d -> new SimpleStringProperty(val(d.getValue().getStudentNumber())));
        cStuNo.setPrefWidth(110);

        TableColumn<Student, String> cGender = new TableColumn<>("性别");
        cGender.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGender()!=null? d.getValue().getGender().getDescription():"未设置"));
        cGender.setPrefWidth(55); // 调小

        TableColumn<Student, String> cBirth = new TableColumn<>("出生日期");
        cBirth.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBirth()!=null? df.format(d.getValue().getBirth()):"未设置"));
        cBirth.setPrefWidth(110);

        TableColumn<Student, String> cEnroll = new TableColumn<>("入学日期");
        cEnroll.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEnrollment()!=null? df.format(d.getValue().getEnrollment()):"未设置"));
        cEnroll.setPrefWidth(110);

        TableColumn<Student, String> cBirthPlace = new TableColumn<>("籍贯");
        cBirthPlace.setCellValueFactory(d -> new SimpleStringProperty(val(d.getValue().getBirthPlace())));
        cBirthPlace.setPrefWidth(120);

        TableColumn<Student, String> cPolitical = new TableColumn<>("政治面貌");
        cPolitical.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPoliticalStat()!=null? d.getValue().getPoliticalStat().getDescription():"未设置"));
        cPolitical.setPrefWidth(110);

        TableColumn<Student, String> cSchool = new TableColumn<>("学院");
        cSchool.setCellValueFactory(d -> new SimpleStringProperty(val(d.getValue().getSchool())));
        cSchool.setPrefWidth(110);

        TableColumn<Student, String> cMajor = new TableColumn<>("专业");
        cMajor.setCellValueFactory(d -> new SimpleStringProperty(val(d.getValue().getMajor())));
        cMajor.setPrefWidth(140);

        TableColumn<Student, String> cStatus = new TableColumn<>("学籍状态");
        cStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()!=null? d.getValue().getStatus().getDescription():"未设置"));
        cStatus.setPrefWidth(100);

        t.getColumns().addAll(cCard, cName, cId, cStuNo, cGender, cBirth, cEnroll, cBirthPlace, cPolitical, cSchool, cMajor, cStatus);

        // 允许横向滚动：放入 ScrollPane（调用处已直接加入 VBox，可在这里返回前包装）
        return t;
    }

    private String val(String s){ return (s==null||s.isEmpty())?"未设置":s; }

    private HBox createOpBox() {
        HBox box = new HBox(20);
        box.setPadding(new Insets(15,0,0,0));
        box.setAlignment(Pos.CENTER);
        Button add = new Button("添加学生"); setPrimaryButtonStyle(add); uniformButtonWidth(add);
        Button edit = new Button("修改选中"); setPrimaryButtonStyle(edit); uniformButtonWidth(edit);
        Button del = new Button("删除选中"); setPrimaryButtonStyle(del); uniformButtonWidth(del);
        Button ref = new Button("刷新"); setPrimaryButtonStyle(ref); uniformButtonWidth(ref);
        Region s1=new Region(),s2=new Region(),s3=new Region(),l=new Region(),r=new Region();
        HBox.setHgrow(l,Priority.ALWAYS); HBox.setHgrow(r,Priority.ALWAYS);
        HBox.setHgrow(s1,Priority.ALWAYS); HBox.setHgrow(s2,Priority.ALWAYS); HBox.setHgrow(s3,Priority.ALWAYS);
        add.setOnAction(e-> openEditDialog(null));
        edit.setOnAction(e-> { Student sel = table.getSelectionModel().getSelectedItem(); if (sel!=null) openEditDialog(sel); else showAlert(Alert.AlertType.WARNING,"选择提示","请先选择要修改的学生");});
        del.setOnAction(e-> { Student sel = table.getSelectionModel().getSelectedItem(); if (sel!=null) confirmDelete(sel); else showAlert(Alert.AlertType.WARNING,"选择提示","请先选择要删除的学生");});
        ref.setOnAction(e-> loadAll());
        box.getChildren().addAll(l, add, s1, edit, s2, del, s3, ref, r);
        return box;
    }

    // ================= 业务逻辑 =================
    private void loadAll() { doSearch("byName", "", true); }

    private void doSearch(String type, String value, boolean fuzzy) {
        new Thread(()->{
            try {
                java.util.List<Student> list = service.searchStudents(type,value,fuzzy);
                Platform.runLater(()-> { table.getItems().clear(); table.getItems().addAll(list); });
            } catch (Exception e) {
                Platform.runLater(()-> showAlert(Alert.AlertType.ERROR, "搜索失败", e.getMessage()));
            }
        }).start();
    }

    private void openEditDialog(Student student) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(student==null?"添加学生":"修改学生信息");
        VBox content = new VBox(10); content.setPadding(new Insets(20));

        TextField cardField = new TextField(student==null?"":String.valueOf(student.getCardNumber()));
        cardField.setPromptText(student==null?"系统自动生成":"一卡通号");
        cardField.setDisable(true); setDisabledStyle(cardField);

        TextField nameField = new TextField(student==null?"": student.getName());
        nameField.setPromptText("姓名");
        if (student!=null) { nameField.setDisable(true); setDisabledStyle(nameField);} // 修改不可

        TextField idField = new TextField(student==null?"": student.getIdentity());
        idField.setPromptText("身份证号");
        if (student!=null) { idField.setDisable(true); setDisabledStyle(idField);} // 修改不可

        TextField stuNumField = new TextField(student==null?"": student.getStudentNumber());
        stuNumField.setPromptText("学号");

        TextField majorField = new TextField(student==null?"": student.getMajor()); majorField.setPromptText("专业");
        TextField schoolField = new TextField(student==null?"": student.getSchool()); schoolField.setPromptText("学院");
        TextField birthPlaceField = new TextField(student==null?"": student.getBirthPlace()); birthPlaceField.setPromptText("籍贯");

        ComboBox<Gender> genderCombo = new ComboBox<>(); genderCombo.getItems().addAll(Gender.values());
        if (student!=null && student.getGender()!=null) { genderCombo.setValue(student.getGender()); genderCombo.setDisable(true); }

        ComboBox<StudentStatus> statusCombo = new ComboBox<>(); statusCombo.getItems().addAll(StudentStatus.values());
        if (student!=null && student.getStatus()!=null) statusCombo.setValue(student.getStatus());

        ComboBox<PoliticalStatus> politicalCombo = new ComboBox<>(); politicalCombo.getItems().addAll(PoliticalStatus.values());
        if (student!=null && student.getPoliticalStat()!=null) politicalCombo.setValue(student.getPoliticalStat());

        DatePicker birthPicker = new DatePicker(); birthPicker.setPromptText("出生日期");
        if (student!=null && student.getBirth()!=null) { birthPicker.setValue(student.getBirth().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()); birthPicker.setDisable(true); }

        DatePicker enrollPicker = new DatePicker(); enrollPicker.setPromptText("入学时间");
        if (student!=null && student.getEnrollment()!=null) { enrollPicker.setValue(student.getEnrollment().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()); }

        // 保存 / 取消 按钮
        HBox btnBox = new HBox(10); btnBox.setAlignment(Pos.CENTER_RIGHT);
        Button save = new Button("保存"); save.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 6;");
        Button cancel = new Button("取消"); cancel.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 6;");
        btnBox.getChildren().addAll(save,cancel);
        cancel.setOnAction(e-> dialog.close());

        save.setOnAction(e-> {
            try {
                if (nameField.getText().trim().isEmpty()) { showAlert(Alert.AlertType.WARNING,"输入错误","姓名不能为空"); return; }
                if (student==null && idField.getText().trim().isEmpty()) { showAlert(Alert.AlertType.WARNING,"输入错误","身份证号不能为空"); return; }
                if (stuNumField.getText().trim().isEmpty()) { showAlert(Alert.AlertType.WARNING,"输入错误","学号不能为空"); return; }
                if (student==null && birthPicker.getValue()==null) { showAlert(Alert.AlertType.WARNING,"输入错误","请选择出生日期"); return; }
                if (enrollPicker.getValue()==null) { showAlert(Alert.AlertType.WARNING,"输入错误","请选择入学时间"); return; }
                if (student==null && genderCombo.getValue()==null) { showAlert(Alert.AlertType.WARNING,"输入错误","请选择性别"); return; }
                if (statusCombo.getValue()==null) { showAlert(Alert.AlertType.WARNING,"输入错误","请选择学籍状态"); return; }
                if (politicalCombo.getValue()==null) { showAlert(Alert.AlertType.WARNING,"输入错误","请选择政治面貌"); return; }

                // 新学生或更新实体
                Student ns = new Student();
                if (student==null) {
                    ns.setIdentity(idField.getText().trim());
                    ns.setGender(genderCombo.getValue());
                    ns.setBirth(java.util.Date.from(birthPicker.getValue().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
                } else {
                    ns.setCardNumber(student.getCardNumber());
                    ns.setIdentity(student.getIdentity());
                    ns.setGender(student.getGender());
                    ns.setBirth(student.getBirth());
                }
                ns.setName(nameField.getText().trim());
                ns.setStudentNumber(stuNumField.getText().trim());
                ns.setMajor(majorField.getText().trim());
                ns.setSchool(schoolField.getText().trim());
                ns.setBirthPlace(birthPlaceField.getText().trim());
                ns.setStatus(statusCombo.getValue());
                ns.setPoliticalStat(politicalCombo.getValue());
                ns.setEnrollment(java.util.Date.from(enrollPicker.getValue().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

                new Thread(() -> {
                    try {
                        boolean ok = (student==null)? service.addStudent(ns) : service.updateStudent(ns);
                        Platform.runLater(() -> {
                            if (ok) { showAlert(Alert.AlertType.INFORMATION, "成功", student==null?"学生添加成功":"学生信息更新成功"); dialog.close(); loadAll(); }
                            else showAlert(Alert.AlertType.ERROR, "失败", student==null?"学生添加失败":"学生信息更新失败");
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "操作失败", ex.getMessage()));
                    }
                }).start();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "操作失败", ex.getMessage());
            }
        });

        content.getChildren().addAll(
                new Label("一卡通号:"), cardField,
                new Label("姓名:"), nameField,
                new Label("身份证号:"), idField,
                new Label("学号:"), stuNumField,
                new Label("专业:"), majorField,
                new Label("学院:"), schoolField,
                new Label("籍贯:"), birthPlaceField,
                new Label("性别:"), genderCombo,
                new Label("学籍状态:"), statusCombo,
                new Label("政治面貌:"), politicalCombo,
                new Label("出生日期:"), birthPicker,
                new Label("入学时间:"), enrollPicker,
                btnBox
        );
        dialog.setScene(new Scene(new ScrollPane(content), 400, 600));
        dialog.showAndWait();
    }

    private void confirmDelete(Student s) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("确认删除"); a.setHeaderText("删除学生"); a.setContentText("确定要删除学生 " + s.getName() + " 吗？此操作不可撤销。");
        a.showAndWait().ifPresent(r -> { if (r==ButtonType.OK) doDelete(s); });
    }

    private void doDelete(Student s) {
        new Thread(() -> {
            try {
                boolean ok = service.deleteStudent(s.getCardNumber());
                Platform.runLater(() -> {
                    if (ok) { showAlert(Alert.AlertType.INFORMATION, "成功", "学生删除成功"); loadAll(); }
                    else showAlert(Alert.AlertType.ERROR, "失败", "学生删除失败");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "删除失败", e.getMessage()));
            }
        }).start();
    }

    private String mapSearchType(String display) {
        switch (display) {
            case "按姓名": return "byName";
            case "按学号": return "byStudentNumber";
            case "按一卡通号": return "byCardNumber";
            default: return "byName";
        }
    }

    private void setPrimaryButtonStyle(Button b) {
        b.setPrefHeight(40);
        // 不设置宽度（统一由 uniformButtonWidth 控制）
        b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);");
        b.setOnMouseEntered(e -> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: " + PRIMARY_HOVER_COLOR + "; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(61,123,255,0.4), 10,0,0,3);"));
        b.setOnMouseExited(e -> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);"));
    }

    private void uniformButtonWidth(Button b) {
        b.setPrefWidth(BUTTON_WIDTH);
        b.setMinWidth(BUTTON_WIDTH);
        b.setMaxWidth(BUTTON_WIDTH);
    }

    private void setDisabledStyle(Control c) {
        c.setStyle("-fx-opacity: 0.7; -fx-background-color: #f2f2f2; -fx-text-fill: #555;");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg); alert.showAndWait();
    }
}
