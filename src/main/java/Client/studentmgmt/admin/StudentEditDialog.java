package Client.studentmgmt.admin;

import Client.studentmgmt.service.StudentClientService;
import Client.util.UIUtil;
import Server.model.student.Student;
import Server.model.student.Gender;
import Server.model.student.StudentStatus;
import Server.model.student.PoliticalStatus;
import javafx.application.Platform;
import javafx.geometry.Insets;
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
 * 学生新增/编辑对话框 (admin 包)
 * 现代化简约设计，使用主题色#176b3a
 * 作者: @Msgo-srAm
 */
public final class StudentEditDialog {
    private StudentEditDialog() {}

    // 主题色和尺寸
    private static final String PRIMARY_COLOR = "#176b3a";
    private static final String PRIMARY_HOVER_COLOR = "#1e7d46";
    private static final String BACKGROUND_COLOR = "#f8f9fa";
    private static final String BORDER_COLOR = "#dee2e6";
    private static final String TEXT_COLOR = "#212529";
    private static final String DISABLED_COLOR = "#6c757d";

    private static final int WIDTH = 720;
    private static final int HEIGHT = 530;

    // 样式
    private static final String FIELD_STYLE = "-fx-background-color: white; -fx-background-radius: 4; " +
            "-fx-border-radius: 4; -fx-border-color: " + BORDER_COLOR + "; " +
            "-fx-padding: 8 12 8 12; -fx-font-size: 14px; -fx-text-fill: " + TEXT_COLOR + ";";
    private static final String DISABLED_FIELD_STYLE = FIELD_STYLE + " -fx-background-color: #f1f3f5; -fx-text-fill: " + DISABLED_COLOR + ";";
    private static final String LABEL_STYLE = "-fx-font-weight: 600; -fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 14px;";
    private static final String BTN_PRIMARY_STYLE = "-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; " +
            "-fx-background-radius: 4; -fx-font-weight: 600; -fx-font-size: 14px; -fx-padding: 8 20 8 20;";
    private static final String BTN_PRIMARY_HOVER_STYLE = "-fx-background-color: " + PRIMARY_HOVER_COLOR + "; -fx-text-fill: white; " +
            "-fx-background-radius: 4; -fx-font-weight: 600; -fx-font-size: 14px; -fx-padding: 8 20 8 20;";
    private static final String BTN_SECONDARY_STYLE = "-fx-background-color: #6c757d; -fx-text-fill: white; " +
            "-fx-background-radius: 4; -fx-font-weight: 600; -fx-font-size: 14px; -fx-padding: 8 20 8 20;";

    // 验证规则
    private static final String ID_PATTERN = "\\d{17}[0-9Xx]";
    private static final String STU_NUM_PATTERN = "\\d{8}";

    public static void open(Student existing, Runnable onSuccess, StudentClientService service) {
        if (service == null) {
            showAlert(Alert.AlertType.ERROR, "服务错误", "无法提交数据");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        UIUtil.applyLogoToStage(dialog);
        dialog.setTitle(existing == null ? "添加学生" : "修改学生信息");

        // 创建表单字段
        TextField cardField = createTextField(existing == null ? "" : String.valueOf(existing.getCardNumber()), true);
        TextField stuNumField = createTextField(existing == null ? "" : existing.getStudentNumber(), false);
        TextField nameField = createTextField(existing == null ? "" : existing.getName(), existing != null);
        TextField idField = createTextField(existing == null ? "" : existing.getIdentity(), existing != null);

        ComboBox<Gender> genderCombo = createComboBox(Gender.values(), existing != null);
        if (existing != null && existing.getGender() != null) {
            genderCombo.setValue(existing.getGender());
        } else {
            genderCombo.setValue(Gender.MALE);
        }

        DatePicker birthPicker = createDatePicker(existing != null);
        if (existing != null && existing.getBirth() != null) {
            birthPicker.setValue(existing.getBirth().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        }

        ComboBox<StudentStatus> statusCombo = createComboBox(StudentStatus.values(), false);
        statusCombo.setValue(existing != null && existing.getStatus() != null ? existing.getStatus() : StudentStatus.ENROLLED);

        DatePicker enrollPicker = createDatePicker(false);
        if (existing != null && existing.getEnrollment() != null) {
            enrollPicker.setValue(existing.getEnrollment().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        }

        ComboBox<PoliticalStatus> politicalCombo = createComboBox(PoliticalStatus.values(), false);
        politicalCombo.setValue(existing != null && existing.getPoliticalStat() != null ? existing.getPoliticalStat() : PoliticalStatus.MASSES);

        TextField birthPlaceField = createTextField(existing == null ? "" : existing.getBirthPlace(), false);
        TextField majorField = createTextField(existing == null ? "" : existing.getMajor(), false);
        TextField schoolField = createTextField(existing == null ? "" : existing.getSchool(), false);

        // 创建表单布局
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20, 30, 20, 30));

        Function<String, Label> createLabel = text -> {
            Label label = new Label(text);
            label.setStyle(LABEL_STYLE);
            return label;
        };

        int row = 0;
        addFormRow(grid, row++, createLabel.apply("一卡通号:"), cardField, createLabel.apply("学号:"), stuNumField);
        addFormRow(grid, row++, createLabel.apply("姓名:"), nameField, createLabel.apply("身份证号:"), idField);
        addFormRow(grid, row++, createLabel.apply("性别:"), genderCombo, createLabel.apply("出生日期:"), birthPicker);
        addFormRow(grid, row++, createLabel.apply("学籍状态:"), statusCombo, createLabel.apply("入学时间:"), enrollPicker);
        addFormRow(grid, row++, createLabel.apply("政治面貌:"), politicalCombo, createLabel.apply("籍贯:"), birthPlaceField);
        addFormRow(grid, row++, createLabel.apply("专业:"), majorField, createLabel.apply("学院:"), schoolField);

        // 创建按钮
        Button saveButton = new Button("保存");
        saveButton.setStyle(BTN_PRIMARY_STYLE);
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(BTN_PRIMARY_HOVER_STYLE));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(BTN_PRIMARY_STYLE));

        Button cancelButton = new Button("取消");
        cancelButton.setStyle(BTN_SECONDARY_STYLE);
        cancelButton.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(15, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 10, 0));

        // 设置保存按钮操作
        saveButton.setOnAction(e -> {
            if (!validateForm(existing, nameField, idField, stuNumField, genderCombo,
                    birthPicker, statusCombo, enrollPicker, politicalCombo)) {
                return;
            }

            Student newStudent = buildStudent(existing, nameField, idField, stuNumField,
                    genderCombo, birthPicker, statusCombo, enrollPicker,
                    politicalCombo, birthPlaceField, majorField, schoolField);

            new Thread(() -> submitForm(existing, newStudent, service, dialog, onSuccess)).start();
        });

        // 创建主容器
        VBox container = new VBox(10, grid, buttonBox);
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");
        container.setPadding(new Insets(20, 0, 20, 0));

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");

        StackPane root = new StackPane(scrollPane);
        root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.setMinWidth(WIDTH);
        dialog.setMaxWidth(WIDTH);
        dialog.setMinHeight(HEIGHT);
        dialog.setMaxHeight(HEIGHT);
        dialog.showAndWait();
    }

    private static TextField createTextField(String text, boolean disabled) {
        TextField field = new TextField(text);
        field.setStyle(disabled ? DISABLED_FIELD_STYLE : FIELD_STYLE);
        field.setDisable(disabled);
        field.setPrefHeight(38);
        return field;
    }

    private static <T> ComboBox<T> createComboBox(T[] items, boolean disabled) {
        ComboBox<T> combo = new ComboBox<>();
        combo.getItems().addAll(items);
        combo.setStyle(disabled ? DISABLED_FIELD_STYLE : FIELD_STYLE);
        combo.setDisable(disabled);
        combo.setPrefHeight(38);
        combo.setPrefWidth(200);
        return combo;
    }

    private static DatePicker createDatePicker(boolean disabled) {
        DatePicker picker = new DatePicker();
        picker.setStyle(disabled ? DISABLED_FIELD_STYLE : FIELD_STYLE);
        picker.setDisable(disabled);
        picker.setPrefHeight(38);
        return picker;
    }

    private static void addFormRow(GridPane grid, int row, Label label1, Control field1, Label label2, Control field2) {
        grid.add(label1, 0, row);
        grid.add(field1, 1, row);
        grid.add(label2, 2, row);
        grid.add(field2, 3, row);

        GridPane.setHgrow(field1, Priority.ALWAYS);
        GridPane.setHgrow(field2, Priority.ALWAYS);

        field1.setMaxWidth(Double.MAX_VALUE);
        field2.setMaxWidth(Double.MAX_VALUE);
    }

    private static boolean validateForm(Student existing, TextField nameField, TextField idField,
                                        TextField stuNumField, ComboBox<Gender> genderCombo, DatePicker birthPicker,
                                        ComboBox<StudentStatus> statusCombo, DatePicker enrollPicker,
                                        ComboBox<PoliticalStatus> politicalCombo) {

        if (nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "输入错误", "姓名不能为空");
            return false;
        }

        if (existing == null && idField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "输入错误", "身份证号不能为空");
            return false;
        }

        if (stuNumField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "输入错误", "学号不能为空");
            return false;
        }

        if (existing == null && birthPicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "输入错误", "请选择出生日期");
            return false;
        }

        if (enrollPicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "输入错误", "请选择入学时间");
            return false;
        }

        if (existing == null && genderCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "输入错误", "请选择性别");
            return false;
        }

        if (statusCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "输入错误", "请选择学籍状态");
            return false;
        }

        if (politicalCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "输入错误", "请选择政治面貌");
            return false;
        }

        String idVal = idField.getText().trim();
        if (!idVal.isEmpty() && !idVal.matches(ID_PATTERN)) {
            showAlert(Alert.AlertType.WARNING, "格式错误", "身份证号需18位：前17位数字最后一位数字或X");
            return false;
        }

        if (!stuNumField.getText().trim().matches(STU_NUM_PATTERN)) {
            showAlert(Alert.AlertType.WARNING, "格式错误", "学号必须为8位数字");
            return false;
        }

        return true;
    }

    private static Student buildStudent(Student existing, TextField nameField, TextField idField,
                                        TextField stuNumField, ComboBox<Gender> genderCombo, DatePicker birthPicker,
                                        ComboBox<StudentStatus> statusCombo, DatePicker enrollPicker,
                                        ComboBox<PoliticalStatus> politicalCombo, TextField birthPlaceField,
                                        TextField majorField, TextField schoolField) {

        Student student = new Student();

        if (existing == null) {
            student.setIdentity(idField.getText().trim());
            student.setGender(genderCombo.getValue());
            student.setBirth(toUtcDate(birthPicker.getValue()));
        } else {
            student.setCardNumber(existing.getCardNumber());
            student.setIdentity(existing.getIdentity());
            student.setGender(existing.getGender());
            student.setBirth(existing.getBirth());
        }

        student.setName(nameField.getText().trim());
        student.setStudentNumber(stuNumField.getText().trim());
        student.setMajor(majorField.getText().trim());
        student.setSchool(schoolField.getText().trim());
        student.setBirthPlace(birthPlaceField.getText().trim());
        student.setStatus(statusCombo.getValue());
        student.setPoliticalStat(politicalCombo.getValue());
        student.setEnrollment(toUtcDate(enrollPicker.getValue()));

        return student;
    }

    private static java.util.Date toUtcDate(java.time.LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return java.util.Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private static void submitForm(Student existing, Student newStudent,
                                   StudentClientService service, Stage dialog, Runnable onSuccess) {

        try {
            boolean success = (existing == null) ?
                    service.addStudent(newStudent) : service.updateStudent(newStudent);

            Platform.runLater(() -> {
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "成功",
                            existing == null ? "学生添加成功" : "学生信息更新成功");
                    dialog.close();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "失败",
                            existing == null ? "学生添加失败" : "学生信息更新失败");
                }
            });
        } catch (Exception ex) {
            Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "操作失败", ex.getMessage()));
        }
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(title);
        UIUtil.applyLogoToAlert(alert);
        alert.showAndWait();
    }
}