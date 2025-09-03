package Client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.beans.property.SimpleStringProperty;


import Server.model.Request;
import Server.model.student.Student;
import Server.model.student.Gender;
import Server.model.student.StudentStatus;
import Server.model.student.PoliticalStatus;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class StudentManagementPanel extends VBox {
    private final String cardNumber;
    private final String userType;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private final Gson gson = new Gson();

    public StudentManagementPanel(String cardNumber, String userType) {
        this.cardNumber = cardNumber;
        this.userType = userType;
        setPadding(new Insets(18));
        setSpacing(10);

        if ("student".equals(userType)) {
            initStudentView();
        } else if ("admin".equals(userType)) {
            initAdminView();
        } else {
            Label info = new Label("暂无可用学籍管理功能");
            getChildren().add(info);
        }
    }

    // 初始化学生视图
    private void initStudentView() {
        Label title = new Label("我的学籍信息");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        getChildren().add(title);

        // 显示加载状态
        Label loadingLabel = new Label("正在加载学籍信息...");
        loadingLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        getChildren().add(loadingLabel);

        // 异步获取学籍信息
        new Thread(() -> {
            try {
                Student student = getSelfFromServer();
                Platform.runLater(() -> {
                    getChildren().remove(loadingLabel);
                    if (student != null) {
                        displayStudentInfo(student);
                    } else {
                        Label errorLabel = new Label("学籍信息获取失败，请稍后重试");
                        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
                        getChildren().add(errorLabel);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    getChildren().remove(loadingLabel);
                    Label errorLabel = new Label("网络连接失败: " + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
                    getChildren().add(errorLabel);
                });
            }
        }).start();
    }

    // 从服务器获取学生自己的学籍信息
    private Student getSelfFromServer() throws Exception {
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;

        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // 构建请求
            Map<String, Object> data = new HashMap<>();
            data.put("cardNumber", Integer.parseInt(cardNumber));
            Request request = new Request("getSelf", data);

            // 发送请求
            String jsonRequest = gson.toJson(request);
            byte[] jsonData = jsonRequest.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(jsonData.length);
            dos.write(jsonData);
            dos.flush();

            // 接收响应
            int responseLength = dis.readInt();
            byte[] responseData = new byte[responseLength];
            dis.readFully(responseData);
            String response = new String(responseData, StandardCharsets.UTF_8);

            // 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = gson.fromJson(response, Map.class);
            if (responseMap.get("code") != null &&
                ((Double)responseMap.get("code")).intValue() == 200) {

                // 将数据转换为Student对象
                @SuppressWarnings("unchecked")
                Map<String, Object> studentData = (Map<String, Object>) responseMap.get("data");
                return convertMapToStudent(studentData);
            } else {
                System.err.println("获取学籍信息失败: " + response);
                return null;
            }
        } finally {
            if (dis != null) {
                try { dis.close(); } catch (IOException ignored) {}
            }
            if (dos != null) {
                try { dos.close(); } catch (IOException ignored) {}
            }
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    // 将Map数据转换为Student对象
    private Student convertMapToStudent(Map<String, Object> data) {
        Student student = new Student();

        if (data.get("cardNumber") != null) {
            student.setCardNumber(((Double)data.get("cardNumber")).intValue());
        }
        if (data.get("name") != null) {
            student.setName((String)data.get("name"));
        }
        if (data.get("identity") != null) {
            student.setIdentity((String)data.get("identity"));
        }
        if (data.get("studentNumber") != null) {
            student.setStudentNumber((String)data.get("studentNumber"));
        }
        if (data.get("major") != null) {
            student.setMajor((String)data.get("major"));
        }
        if (data.get("school") != null) {
            student.setSchool((String)data.get("school"));
        }
        if (data.get("birthPlace") != null) {
            student.setBirthPlace((String)data.get("birthPlace"));
        }

        // 处理枚举类型
        if (data.get("gender") != null) {
            try {
                student.setGender(Gender.valueOf((String)data.get("gender")));
            } catch (IllegalArgumentException e) {
                // 如果枚举值无效，设为null
            }
        }

        if (data.get("status") != null) {
            try {
                student.setStatus(StudentStatus.valueOf((String)data.get("status")));
            } catch (IllegalArgumentException e) {
                // 如果枚举值无效，设为null
            }
        }

        if (data.get("politicalStat") != null) {
            try {
                student.setPoliticalStat(PoliticalStatus.valueOf((String)data.get("politicalStat")));
            } catch (IllegalArgumentException e) {
                // 如果枚举值无效，设为null
            }
        }

        // 处理日期类型 - 这里假设服务器返回的是时间戳或日期字符串
        // 具体实现可能需要根据服务器实际返回格式调整

        return student;
    }

    // 显示学生信息
    private void displayStudentInfo(Student student) {
        // 创建信息展示区域
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 8; -fx-padding: 15;");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // 基本信息
        Label basicTitle = new Label("基本信息");
        basicTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        infoBox.getChildren().add(basicTitle);

        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(20);
        basicGrid.setVgap(8);
        basicGrid.setStyle("-fx-padding: 10 0 10 0;");

        int row = 0;
        if (student.getName() != null) {
            addInfoRow(basicGrid, row++, "姓名:", student.getName());
        }
        if (student.getCardNumber() != null) {
            addInfoRow(basicGrid, row++, "一卡通号:", String.valueOf(student.getCardNumber()));
        }
        if (student.getStudentNumber() != null) {
            addInfoRow(basicGrid, row++, "学号:", student.getStudentNumber());
        }
        if (student.getIdentity() != null) {
            addInfoRow(basicGrid, row++, "身份证号:", student.getIdentity());
        }
        if (student.getGender() != null) {
            addInfoRow(basicGrid, row++, "性别:", student.getGender().getDescription());
        }
        if (student.getBirth() != null) {
            addInfoRow(basicGrid, row++, "出生日期:", dateFormat.format(student.getBirth()));
        }
        if (student.getBirthPlace() != null) {
            addInfoRow(basicGrid, row++, "籍贯:", student.getBirthPlace());
        }
        if (student.getPoliticalStat() != null) {
            addInfoRow(basicGrid, row, "政治面貌:", student.getPoliticalStat().getDescription());
        }

        infoBox.getChildren().add(basicGrid);

        // 学籍信息
        Label academicTitle = new Label("学籍信息");
        academicTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-padding: 10 0 0 0;");
        infoBox.getChildren().add(academicTitle);

        GridPane academicGrid = new GridPane();
        academicGrid.setHgap(20);
        academicGrid.setVgap(8);
        academicGrid.setStyle("-fx-padding: 10 0 10 0;");

        row = 0;
        if (student.getSchool() != null) {
            addInfoRow(academicGrid, row++, "学院:", student.getSchool());
        }
        if (student.getMajor() != null) {
            addInfoRow(academicGrid, row++, "专业:", student.getMajor());
        }
        if (student.getStatus() != null) {
            addInfoRow(academicGrid, row++, "学籍状态:", student.getStatus().getDescription());
        }
        if (student.getEnrollment() != null) {
            addInfoRow(academicGrid, row, "入学日期:", dateFormat.format(student.getEnrollment()));
        }

        infoBox.getChildren().add(academicGrid);

        // 添加刷新按钮
        Button refreshBtn = new Button("刷新信息");
        refreshBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #4e8cff; -fx-text-fill: white; " +
                          "-fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-margin: 10 0 0 0;");
        refreshBtn.setOnAction(e -> {
            getChildren().clear();
            initStudentView();
        });

        VBox refreshBox = new VBox(refreshBtn);
        refreshBox.setStyle("-fx-padding: 10 0 0 0;");
        infoBox.getChildren().add(refreshBox);

        getChildren().add(infoBox);
    }

    // 添加信息行到GridPane
    private void addInfoRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-min-width: 80;");

        Label valueNode = new Label(value != null ? value : "未设置");
        valueNode.setStyle("-fx-text-fill: #333; -fx-font-size: 14px;");

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    // 初始化管理员视图
    private void initAdminView() {
        Label title = new Label("学籍管理");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        getChildren().add(title);

        // 搜索区域
        HBox searchBox = createSearchBox();
        getChildren().add(searchBox);

        // 学生信息表格
        TableView<Student> studentTable = createStudentTable();
        getChildren().add(studentTable);

        // 操作按钮区域
        HBox buttonBox = createButtonBox(studentTable);
        getChildren().add(buttonBox);

        // 初始化时加载所有学生数据
        loadAllStudents(studentTable);
    }

    // 创建搜索区域
    private HBox createSearchBox() {
        HBox searchBox = new HBox(10);
        searchBox.setPadding(new Insets(10, 0, 10, 0));
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("搜索条件:");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        ComboBox<String> searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("按姓名", "按学号", "按一卡通号");
        searchTypeCombo.setValue("按姓名");
        searchTypeCombo.setPrefWidth(120);

        TextField searchField = new TextField();
        searchField.setPromptText("请输入搜索内容");
        searchField.setPrefWidth(200);

        CheckBox fuzzyCheckBox = new CheckBox("模糊搜索");
        fuzzyCheckBox.setSelected(true);

        Button searchBtn = new Button("搜索");
        searchBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-background-radius: 6;");

        Button clearBtn = new Button("清空");
        clearBtn.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #333; -fx-background-radius: 6;");

        searchBtn.setOnAction(e -> {
            String searchType = getSearchTypeValue(searchTypeCombo.getValue());
            String searchValue = searchField.getText().trim();
            boolean fuzzy = fuzzyCheckBox.isSelected();

            if (searchValue.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "输入提示", "请输入搜索内容");
                return;
            }

            searchStudents(searchType, searchValue, fuzzy,
                          (TableView<Student>) getChildren().get(2)); // 获取表格
        });

        clearBtn.setOnAction(e -> {
            searchField.clear();
            loadAllStudents((TableView<Student>) getChildren().get(2));
        });

        searchBox.getChildren().addAll(searchLabel, searchTypeCombo, searchField,
                                     fuzzyCheckBox, searchBtn, clearBtn);
        return searchBox;
    }

    // 创建学生信息表格
    private TableView<Student> createStudentTable() {
        TableView<Student> table = new TableView<>();
        table.setPrefHeight(300);

        // 创建表格列
        TableColumn<Student, String> cardCol = new TableColumn<>("一卡通号");
        cardCol.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getCardNumber())));
        cardCol.setPrefWidth(100);

        TableColumn<Student, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(80);

        TableColumn<Student, String> studentNumCol = new TableColumn<>("学号");
        studentNumCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStudentNumber()));
        studentNumCol.setPrefWidth(120);

        TableColumn<Student, String> majorCol = new TableColumn<>("专业");
        majorCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getMajor()));
        majorCol.setPrefWidth(150);

        TableColumn<Student, String> schoolCol = new TableColumn<>("学院");
        schoolCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getSchool()));
        schoolCol.setPrefWidth(120);

        TableColumn<Student, String> statusCol = new TableColumn<>("学籍状态");
        statusCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStatus() != null ?
                                   data.getValue().getStatus().getDescription() : "未设置"));
        statusCol.setPrefWidth(80);

        TableColumn<Student, String> genderCol = new TableColumn<>("性别");
        genderCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getGender() != null ?
                                   data.getValue().getGender().getDescription() : "未设置"));
        genderCol.setPrefWidth(60);

        table.getColumns().addAll(cardCol, nameCol, studentNumCol, majorCol,
                                 schoolCol, statusCol, genderCol);
        return table;
    }

    // 创建操作按钮区域
    private HBox createButtonBox(TableView<Student> table) {
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("添加学生");
        addBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");

        Button editBtn = new Button("修改选中");
        editBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");

        Button deleteBtn = new Button("删除选中");
        deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");

        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");

        // 按钮事件处理
        addBtn.setOnAction(e -> showStudentEditDialog(null, table));

        editBtn.setOnAction(e -> {
            Student selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showStudentEditDialog(selected, table);
            } else {
                showAlert(Alert.AlertType.WARNING, "选择提示", "请先选择要修改的学生");
            }
        });

        deleteBtn.setOnAction(e -> {
            Student selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteStudent(selected, table);
            } else {
                showAlert(Alert.AlertType.WARNING, "选择提示", "请先选择要删除的学生");
            }
        });

        refreshBtn.setOnAction(e -> loadAllStudents(table));

        buttonBox.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn);
        return buttonBox;
    }

    // 搜索学生
    private void searchStudents(String searchType, String searchValue, boolean fuzzy, TableView<Student> table) {
        new Thread(() -> {
            try {
                List<Student> students = searchStudentsFromServer(searchType, searchValue, fuzzy);
                Platform.runLater(() -> {
                    table.getItems().clear();
                    table.getItems().addAll(students);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "搜索失败", "搜索学生信息失败: " + e.getMessage()));
            }
        }).start();
    }

    // 从服务器搜索学生
    private List<Student> searchStudentsFromServer(String searchType, String searchValue, boolean fuzzy) throws Exception {
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;

        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // 构建请求
            Map<String, Object> data = new HashMap<>();
            data.put("searchType", searchType);
            data.put("searchValue", searchValue);
            data.put("fuzzy", fuzzy);
            Request request = new Request("searchStudents", data);

            // 发送请求
            String jsonRequest = gson.toJson(request);
            byte[] jsonData = jsonRequest.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(jsonData.length);
            dos.write(jsonData);
            dos.flush();

            // 接收响应
            int responseLength = dis.readInt();
            byte[] responseData = new byte[responseLength];
            dis.readFully(responseData);
            String response = new String(responseData, StandardCharsets.UTF_8);

            // 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = gson.fromJson(response, Map.class);
            if (responseMap.get("code") != null &&
                ((Double)responseMap.get("code")).intValue() == 200) {

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> studentsData = (List<Map<String, Object>>) responseMap.get("data");
                return studentsData.stream()
                    .map(this::convertMapToStudent)
                    .collect(java.util.stream.Collectors.toList());
            } else {
                throw new Exception("服务器返回错误: " + responseMap.get("message"));
            }
        } finally {
            if (dis != null) {
                try { dis.close(); } catch (IOException ignored) {}
            }
            if (dos != null) {
                try { dos.close(); } catch (IOException ignored) {}
            }
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    // 加载所有学生（使用空搜索条件）
    private void loadAllStudents(TableView<Student> table) {
        searchStudents("BY_NAME", "", true, table);
    }

    // 显示学生编辑对话框
    private void showStudentEditDialog(Student student, TableView<Student> table) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(student == null ? "添加学生" : "修改学生信息");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // 创建输入字段
        TextField cardNumberField = new TextField(student != null ? String.valueOf(student.getCardNumber()) : "");
        cardNumberField.setPromptText(student == null ? "系统自动生成" : "一卡通号");
        cardNumberField.setDisable(true); // 添加时不可编辑（系统生成），修改时也不可编辑（主键）

        TextField nameField = new TextField(student != null ? student.getName() : "");
        nameField.setPromptText("姓名");

        TextField identityField = new TextField(student != null ? student.getIdentity() : "");
        identityField.setPromptText("身份证号");

        TextField studentNumberField = new TextField(student != null ? student.getStudentNumber() : "");
        studentNumberField.setPromptText("学号");

        TextField majorField = new TextField(student != null ? student.getMajor() : "");
        majorField.setPromptText("专业");

        TextField schoolField = new TextField(student != null ? student.getSchool() : "");
        schoolField.setPromptText("学院");

        TextField birthPlaceField = new TextField(student != null ? student.getBirthPlace() : "");
        birthPlaceField.setPromptText("籍贯");

        ComboBox<Gender> genderCombo = new ComboBox<>();
        genderCombo.getItems().addAll(Gender.values());
        if (student != null && student.getGender() != null) {
            genderCombo.setValue(student.getGender());
        }

        ComboBox<StudentStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(StudentStatus.values());
        if (student != null && student.getStatus() != null) {
            statusCombo.setValue(student.getStatus());
        }

        ComboBox<PoliticalStatus> politicalCombo = new ComboBox<>();
        politicalCombo.getItems().addAll(PoliticalStatus.values());
        if (student != null && student.getPoliticalStat() != null) {
            politicalCombo.setValue(student.getPoliticalStat());
        }

        // 按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("保存");
        saveBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 6;");

        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 6;");

        saveBtn.setOnAction(e -> {
            try {
                Student newStudent = new Student();

                if (student == null) {
                    // 添加新学生 - 不设置一卡通号，让服务器自动生成
                    // newStudent.setCardNumber() 不调用，让服务器端自动生成
                } else {
                    // 修改现有学生
                    newStudent.setCardNumber(student.getCardNumber());
                }

                newStudent.setName(nameField.getText().trim());
                newStudent.setIdentity(identityField.getText().trim());
                newStudent.setStudentNumber(studentNumberField.getText().trim());
                newStudent.setMajor(majorField.getText().trim());
                newStudent.setSchool(schoolField.getText().trim());
                newStudent.setBirthPlace(birthPlaceField.getText().trim());
                newStudent.setGender(genderCombo.getValue());
                newStudent.setStatus(statusCombo.getValue());
                newStudent.setPoliticalStat(politicalCombo.getValue());

                boolean success;
                if (student == null) {
                    success = addStudentToServer(newStudent);
                } else {
                    success = updateStudentToServer(newStudent);
                }

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "成功",
                             student == null ? "学生添加成功" : "学生信息更新成功");
                    dialog.close();
                    loadAllStudents(table); // 刷新表格
                } else {
                    showAlert(Alert.AlertType.ERROR, "失败",
                             student == null ? "学生添加失败" : "学生信息更新失败");
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "操作失败", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(saveBtn, cancelBtn);

        content.getChildren().addAll(
            new Label("一卡通号:"), cardNumberField,
            new Label("姓名:"), nameField,
            new Label("身份证号:"), identityField,
            new Label("学号:"), studentNumberField,
            new Label("专业:"), majorField,
            new Label("学院:"), schoolField,
            new Label("籍贯:"), birthPlaceField,
            new Label("性别:"), genderCombo,
            new Label("学籍状态:"), statusCombo,
            new Label("政治面貌:"), politicalCombo,
            buttonBox
        );

        Scene scene = new Scene(new ScrollPane(content), 400, 600);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // 添加学生到服务器
    private boolean addStudentToServer(Student student) throws Exception {
        return sendStudentOperation("addStudent", student);
    }

    // 更新学生到服务器
    private boolean updateStudentToServer(Student student) throws Exception {
        return sendStudentOperation("updateStudent", student);
    }

    // 发送学生操作请求
    private boolean sendStudentOperation(String operation, Student student) throws Exception {
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;

        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // 构建请求
            Map<String, Object> data = new HashMap<>();
            data.put("student", convertStudentToMap(student));
            Request request = new Request(operation, data);

            // 发送请求
            String jsonRequest = gson.toJson(request);
            byte[] jsonData = jsonRequest.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(jsonData.length);
            dos.write(jsonData);
            dos.flush();

            // 接收响应
            int responseLength = dis.readInt();
            byte[] responseData = new byte[responseLength];
            dis.readFully(responseData);
            String response = new String(responseData, StandardCharsets.UTF_8);

            // 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = gson.fromJson(response, Map.class);
            return responseMap.get("code") != null &&
                   ((Double)responseMap.get("code")).intValue() == 200;
        } finally {
            if (dis != null) {
                try { dis.close(); } catch (IOException ignored) {}
            }
            if (dos != null) {
                try { dos.close(); } catch (IOException ignored) {}
            }
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    // 删除学生
    private void deleteStudent(Student student, TableView<Student> table) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("删除学生");
        confirmAlert.setContentText("确定要删除学生 " + student.getName() + " 吗？此操作不可撤销。");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        boolean success = deleteStudentFromServer(student.getCardNumber());
                        Platform.runLater(() -> {
                            if (success) {
                                showAlert(Alert.AlertType.INFORMATION, "成功", "学生删除成功");
                                loadAllStudents(table);
                            } else {
                                showAlert(Alert.AlertType.ERROR, "失败", "学生删除失败");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "删除失败", "删除学生失败: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    // 从服务器删除学生
    private boolean deleteStudentFromServer(Integer cardNumber) throws Exception {
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;

        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // 构建请求
            Map<String, Object> data = new HashMap<>();
            data.put("cardNumber", cardNumber);
            Request request = new Request("deleteStudent", data);

            // 发送请求
            String jsonRequest = gson.toJson(request);
            byte[] jsonData = jsonRequest.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(jsonData.length);
            dos.write(jsonData);
            dos.flush();

            // 接收响应
            int responseLength = dis.readInt();
            byte[] responseData = new byte[responseLength];
            dis.readFully(responseData);
            String response = new String(responseData, StandardCharsets.UTF_8);

            // 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = gson.fromJson(response, Map.class);
            return responseMap.get("code") != null &&
                   ((Double)responseMap.get("code")).intValue() == 200;
        } finally {
            if (dis != null) {
                try { dis.close(); } catch (IOException ignored) {}
            }
            if (dos != null) {
                try { dos.close(); } catch (IOException ignored) {}
            }
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    // 将Student对象转换为Map
    private Map<String, Object> convertStudentToMap(Student student) {
        Map<String, Object> map = new HashMap<>();
        if (student.getCardNumber() != null) map.put("cardNumber", student.getCardNumber());
        if (student.getName() != null) map.put("name", student.getName());
        if (student.getIdentity() != null) map.put("identity", student.getIdentity());
        if (student.getStudentNumber() != null) map.put("studentNumber", student.getStudentNumber());
        if (student.getMajor() != null) map.put("major", student.getMajor());
        if (student.getSchool() != null) map.put("school", student.getSchool());
        if (student.getBirthPlace() != null) map.put("birthPlace", student.getBirthPlace());
        if (student.getGender() != null) map.put("gender", student.getGender().name());
        if (student.getStatus() != null) map.put("status", student.getStatus().name());
        if (student.getPoliticalStat() != null) map.put("politicalStat", student.getPoliticalStat().name());
        if (student.getBirth() != null) map.put("birth", student.getBirth().getTime());
        if (student.getEnrollment() != null) map.put("enrollment", student.getEnrollment().getTime());
        return map;
    }

    // 获取搜索类型值
    private String getSearchTypeValue(String displayName) {
        switch (displayName) {
            case "按姓名": return "BY_NAME";
            case "按学号": return "BY_STUDENT_NUMBER";
            case "按一卡通号": return "BY_CARD_NUMBER";
            default: return "BY_NAME";
        }
    }

    // 显示提示对话框
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
