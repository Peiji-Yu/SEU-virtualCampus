package Client.studentmgmt.admin;

import Client.studentmgmt.service.StudentClientService;
import Client.util.UIUtil;
import Server.model.student.Student;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.List;

/**
 * 管理员学籍管理面板 (admin 包)。
 * 包含：搜索、表格展示、增删改刷新操作。
 * 作者: @Msgo-srAm
 */
public class StudentAdminPanel extends VBox {
    private final StudentClientService service = new StudentClientService();
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String PRIMARY_HOVER_COLOR = "#3d7bff";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final double BUTTON_WIDTH = 110;
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
        StudentSearchBar searchBar = new StudentSearchBar(new StudentSearchBar.SearchListener() {
            @Override public void onSearch(String searchType, String value, boolean fuzzy) {doSearch(searchType, value, fuzzy);}
            @Override public void onClear() {loadAll();}
        });
        getChildren().add(searchBar);
        table = createTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
        HBox opBox = createOpBox();
        getChildren().add(opBox);
        loadAll();
    }

    private TableView<Student> createTable() {
        TableView<Student> t = new TableView<>();
        t.setPrefHeight(420);
        // 允许列根据内容调整，我们使用自定义逻辑动态计算并设置列宽
        t.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        t.setTableMenuButtonVisible(true);

        // 应用CSS样式到表格
        t.setStyle("-fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;");

        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
        TableColumn<Student,String> cCard = new TableColumn<>("一卡通号"); cCard.setCellValueFactory(d-> new SimpleStringProperty(String.valueOf(d.getValue().getCardNumber()))); cCard.setPrefWidth(90);
        TableColumn<Student,String> cName = new TableColumn<>("姓名"); cName.setCellValueFactory(d-> new SimpleStringProperty(val(d.getValue().getName()))); cName.setPrefWidth(90);
        TableColumn<Student,String> cId = new TableColumn<>("身份证号"); cId.setCellValueFactory(d-> new SimpleStringProperty(val(d.getValue().getIdentity()))); cId.setPrefWidth(170);
        TableColumn<Student,String> cStuNo = new TableColumn<>("学号"); cStuNo.setCellValueFactory(d-> new SimpleStringProperty(val(d.getValue().getStudentNumber()))); cStuNo.setPrefWidth(110);
        TableColumn<Student,String> cGender = new TableColumn<>("性别"); cGender.setCellValueFactory(d-> new SimpleStringProperty(d.getValue().getGender()!=null? d.getValue().getGender().getDescription():"未设置")); cGender.setPrefWidth(55);
        TableColumn<Student,String> cBirth = new TableColumn<>("出生日期"); cBirth.setCellValueFactory(d-> new SimpleStringProperty(d.getValue().getBirth()!=null? df.format(d.getValue().getBirth()):"未设置")); cBirth.setPrefWidth(110);
        TableColumn<Student,String> cEnroll = new TableColumn<>("入学日期"); cEnroll.setCellValueFactory(d-> new SimpleStringProperty(d.getValue().getEnrollment()!=null? df.format(d.getValue().getEnrollment()):"未设置")); cEnroll.setPrefWidth(110);
        TableColumn<Student,String> cBirthPlace = new TableColumn<>("籍贯"); cBirthPlace.setCellValueFactory(d-> new SimpleStringProperty(val(d.getValue().getBirthPlace()))); cBirthPlace.setPrefWidth(120);
        TableColumn<Student,String> cPolitical = new TableColumn<>("政治面貌"); cPolitical.setCellValueFactory(d-> new SimpleStringProperty(d.getValue().getPoliticalStat()!=null? d.getValue().getPoliticalStat().getDescription():"未设置")); cPolitical.setPrefWidth(110);
        TableColumn<Student,String> cSchool = new TableColumn<>("学院"); cSchool.setCellValueFactory(d-> new SimpleStringProperty(val(d.getValue().getSchool()))); cSchool.setPrefWidth(110);
        TableColumn<Student,String> cMajor = new TableColumn<>("专业"); cMajor.setCellValueFactory(d-> new SimpleStringProperty(val(d.getValue().getMajor()))); cMajor.setPrefWidth(140);
        TableColumn<Student,String> cStatus = new TableColumn<>("学籍状态"); cStatus.setCellValueFactory(d-> new SimpleStringProperty(d.getValue().getStatus()!=null? d.getValue().getStatus().getDescription():"未设置")); cStatus.setPrefWidth(100);

        t.getColumns().addAll(cCard,cName,cId,cStuNo,cGender,cBirth,cEnroll,cBirthPlace,cPolitical,cSchool,cMajor,cStatus);

        // 为所有列设置统一样式（在列被添加后执行）
        for (TableColumn<Student, ?> column : t.getColumns()) {
            column.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 8 10 8 10;");
        }

        // 应用CSS样式表
        t.getStylesheets().add(getClass().getResource("/styles/table-styles.css").toExternalForm());

        return t;
    }

    // 根据列头与可见单元格内容计算并调整每一列的宽度，尽量避免文本被截断
    private void adjustColumnWidths() {
        if (table == null) return;
        Platform.runLater(() -> {
            Font font = Font.font(13);
            for (TableColumn<Student, ?> col : table.getColumns()) {
                double max = 30; // 最小宽度
                // 计算表头宽度
                String header = col.getText() == null ? "" : col.getText();
                Text ht = new Text(header);
                ht.setFont(font);
                max = Math.max(max, ht.getLayoutBounds().getWidth() + 30);

                // 计算若干行内容宽度（只遍历当前可见或前 N 行以节省开销）
                int limit = Math.min(table.getItems().size(), 200); // 上限防止过慢
                for (int i = 0; i < limit; i++) {
                    Student s = table.getItems().get(i);
                    try {
                        Object cellObj = col.getCellData(s);
                        String cellText = cellObj == null ? "" : String.valueOf(cellObj);
                        Text ct = new Text(cellText);
                        ct.setFont(font);
                        double w = ct.getLayoutBounds().getWidth();
                        if (w + 30 > max) max = w + 30;
                    } catch (Exception ignored) {}
                }

                // 限制最大列宽以避免单列占用过多空间（可根据需要调整）
                double cap = 600;
                col.setPrefWidth(Math.min(Math.max(max, col.getPrefWidth()), cap));
            }
        });
    }

    private String val(String s) {return (s==null||s.isEmpty())?"未设置":s;}

    private HBox createOpBox() {
        HBox box = new HBox(20); box.setPadding(new Insets(15,0,0,0)); box.setAlignment(Pos.CENTER);
        Button add = new Button("添加学生"); setPrimaryButtonStyle(add); uniformButtonWidth(add);
        Button edit = new Button("修改选中"); setPrimaryButtonStyle(edit); uniformButtonWidth(edit);
        Button del = new Button("删除选中"); setPrimaryButtonStyle(del); uniformButtonWidth(del);
        Button ref = new Button("刷新"); setPrimaryButtonStyle(ref); uniformButtonWidth(ref);
        Region s1=new Region();Region s2=new Region();Region s3=new Region();Region l=new Region();Region r=new Region();
        HBox.setHgrow(l,Priority.ALWAYS);HBox.setHgrow(r,Priority.ALWAYS);HBox.setHgrow(s1,Priority.ALWAYS);HBox.setHgrow(s2,Priority.ALWAYS);HBox.setHgrow(s3,Priority.ALWAYS);
        add.setOnAction(e-> StudentEditDialog.open(null,this::loadAll,service));
        edit.setOnAction(e->{Student sel = table.getSelectionModel().getSelectedItem(); if(sel!=null){StudentEditDialog.open(sel,this::loadAll,service);} else {showAlert(Alert.AlertType.WARNING,"选择提示","请先选择要修改的学生");}});
        del.setOnAction(e->{Student sel = table.getSelectionModel().getSelectedItem(); if(sel!=null){StudentDeleteHelper.confirmAndDelete(sel,service,this::loadAll);} else {showAlert(Alert.AlertType.WARNING,"选择提示","请先选择要删除的学生");}});
        ref.setOnAction(e->loadAll());
        box.getChildren().addAll(l,add,s1,edit,s2,del,s3,ref,r);
        return box;
    }

    private void loadAll() {doSearch("byName","",true);}

    private void doSearch(String type,String value,boolean fuzzy){
        new Thread(() -> {
            try {List<Student> list = service.searchStudents(type,value,fuzzy); Platform.runLater(()->{table.getItems().clear();table.getItems().addAll(list);adjustColumnWidths();});}
            catch (Exception e){Platform.runLater(()->showAlert(Alert.AlertType.ERROR,"搜索失败", e.getMessage()));}
        }).start();
    }

    private void setPrimaryButtonStyle(Button b){
        b.setPrefHeight(40);
        b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);");
        b.setOnMouseEntered(e-> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_HOVER_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(61,123,255,0.4), 10,0,0,3);"));
        b.setOnMouseExited(e-> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);"));
    }

    private void uniformButtonWidth(Button b){b.setPrefWidth(BUTTON_WIDTH);b.setMinWidth(BUTTON_WIDTH);b.setMaxWidth(BUTTON_WIDTH);}

    private void showAlert(Alert.AlertType type,String title,String msg){
        Alert a=new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        UIUtil.applyLogoToAlert(a);
        a.showAndWait();
    }
}
