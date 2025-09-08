package Client.coursemgmt.admin;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员-课程管理（前端演示版，无后端）。
 * 功能：搜索（按学生/教师/教学班）、表格展示、增删改、刷新。
 */
public class CourseAdminPanel extends VBox {
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String PRIMARY_HOVER_COLOR = "#3d7bff";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final double BUTTON_WIDTH = 110;

    // 简单课程模型（演示用）
    public static class Course {
        public String id;         // 课程号
        public String name;       // 课程名
        public String teacher;    // 任课教师
        public String clazz;      // 教学班
        public String room;       // 教室
        public int capacity;      // 容量
        public List<String> students = new ArrayList<>(); // 已选学生姓名
        public String schedule;   // 上课时间（字符串）
        public int selected() {return students==null?0:students.size();}
    }

    private final List<Course> allData = new ArrayList<>(); // 全量演示数据
    private TableView<Course> table;

    public CourseAdminPanel() {
        setPadding(new Insets(18));
        setSpacing(10);
        init();
    }

    private void init() {
        Label title = new Label("课程管理");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");
        getChildren().add(title);

        Client.coursemgmt.admin.CourseSearchBar searchBar = new Client.coursemgmt.admin.CourseSearchBar(new Client.coursemgmt.admin.CourseSearchBar.SearchListener() {
            @Override public void onSearch(String type, String value, boolean fuzzy) { doSearch(type,value,fuzzy);}
            @Override public void onClear() { loadAll(); }
        });
        getChildren().add(searchBar);

        table = createTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);

        HBox opBox = createOpBox();
        getChildren().add(opBox);

        seedDemo();
        loadAll();
    }

    private TableView<Course> createTable() {
        TableView<Course> t = new TableView<>();
        t.setPrefHeight(460);
        t.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        t.setTableMenuButtonVisible(true);
        TableColumn<Course,String> cId = new TableColumn<>("课程号"); cId.setCellValueFactory(d-> new SimpleStringProperty(nv(d.getValue().id))); cId.setPrefWidth(110);
        TableColumn<Course,String> cName = new TableColumn<>("课程名"); cName.setCellValueFactory(d-> new SimpleStringProperty(nv(d.getValue().name))); cName.setPrefWidth(160);
        TableColumn<Course,String> cTeacher = new TableColumn<>("任课教师"); cTeacher.setCellValueFactory(d-> new SimpleStringProperty(nv(d.getValue().teacher))); cTeacher.setPrefWidth(110);
        TableColumn<Course,String> cClazz = new TableColumn<>("教学班"); cClazz.setCellValueFactory(d-> new SimpleStringProperty(nv(d.getValue().clazz))); cClazz.setPrefWidth(110);
        TableColumn<Course,String> cRoom = new TableColumn<>("教室"); cRoom.setCellValueFactory(d-> new SimpleStringProperty(nv(d.getValue().room))); cRoom.setPrefWidth(110);
        TableColumn<Course,Number> cCap = new TableColumn<>("容量"); cCap.setCellValueFactory(d-> new SimpleIntegerProperty(d.getValue().capacity)); cCap.setPrefWidth(70);
        TableColumn<Course,Number> cSel = new TableColumn<>("已选"); cSel.setCellValueFactory(d-> new SimpleIntegerProperty(d.getValue().selected())); cSel.setPrefWidth(70);
        TableColumn<Course,String> cSched = new TableColumn<>("时间"); cSched.setCellValueFactory(d-> new SimpleStringProperty(nv(d.getValue().schedule))); cSched.setPrefWidth(180);
        t.getColumns().addAll(cId,cName,cTeacher,cClazz,cRoom,cCap,cSel,cSched);
        return t;
    }

    private String nv(String s){return (s==null||s.isEmpty())?"未设置":s;}

    private HBox createOpBox() {
        HBox box = new HBox(20); box.setPadding(new Insets(15,0,0,0)); box.setAlignment(Pos.CENTER);
        Button add = new Button("添加课程"); setPrimaryButtonStyle(add); uniformButtonWidth(add);
        Button edit = new Button("修改选中"); setPrimaryButtonStyle(edit); uniformButtonWidth(edit);
        Button del = new Button("删除选中"); setPrimaryButtonStyle(del); uniformButtonWidth(del);
        Button ref = new Button("刷新"); setPrimaryButtonStyle(ref); uniformButtonWidth(ref);
        Region s1=new Region();Region s2=new Region();Region s3=new Region();Region l=new Region();Region r=new Region();
        HBox.setHgrow(l,Priority.ALWAYS);HBox.setHgrow(r,Priority.ALWAYS);HBox.setHgrow(s1,Priority.ALWAYS);HBox.setHgrow(s2,Priority.ALWAYS);HBox.setHgrow(s3,Priority.ALWAYS);

        add.setOnAction(e-> CourseEditDialog.open(null, c->{ allData.add(c); refreshFromAll(); }));
        edit.setOnAction(e->{ Course sel = table.getSelectionModel().getSelectedItem(); if(sel!=null){CourseEditDialog.open(sel, x->{ refreshFromAll(); });} else {showWarn("请先选择要修改的课程");}});
        del.setOnAction(e->{ Course sel = table.getSelectionModel().getSelectedItem(); if(sel!=null){ CourseDeleteHelper.confirmAndDelete(sel, allData, this::refreshFromAll);} else {showWarn("请先选择要删除的课程");}});
        ref.setOnAction(e->loadAll());

        box.getChildren().addAll(l,add,s1,edit,s2,del,s3,ref,r);
        return box;
    }

    private void loadAll() { doSearch("all","", true); }

    private void doSearch(String type, String value, boolean fuzzy){
        // 前端过滤演示
        new Thread(() -> {
            try {
                List<Course> filtered;
                String v = value==null?"":value.trim();
                if (type==null || "all".equals(type) || v.isEmpty()) {
                    filtered = new ArrayList<>(allData);
                } else if ("byStudent".equals(type)) {
                    filtered = allData.stream().filter(c -> matchList(c.students, v, fuzzy)).collect(Collectors.toList());
                } else if ("byTeacher".equals(type)) {
                    filtered = allData.stream().filter(c -> matchStr(c.teacher, v, fuzzy)).collect(Collectors.toList());
                } else if ("byClazz".equals(type)) {
                    filtered = allData.stream().filter(c -> matchStr(c.clazz, v, fuzzy)).collect(Collectors.toList());
                } else {
                    filtered = new ArrayList<>(allData);
                }
                Platform.runLater(() -> table.getItems().setAll(filtered));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "搜索失败", e.getMessage()));
            }
        }).start();
    }

    private boolean matchStr(String s, String v, boolean fuzzy){
        if (s==null) return false; return fuzzy ? s.contains(v) : s.equals(v);
    }
    private boolean matchList(List<String> list, String v, boolean fuzzy){
        if (list==null) return false; for(String s:list){ if (matchStr(s,v,fuzzy)) return true; } return false;
    }

    public void refreshFromAll(){
        // 编辑/删除后刷新表格，但保留当前简单筛选：直接重载全部
        loadAll();
    }

    private void setPrimaryButtonStyle(Button b){
        b.setPrefHeight(40);
        b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);");
        b.setOnMouseEntered(e-> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_HOVER_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(61,123,255,0.4), 10,0,0,3);"));
        b.setOnMouseExited(e-> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);"));
    }
    private void uniformButtonWidth(Button b){b.setPrefWidth(BUTTON_WIDTH);b.setMinWidth(BUTTON_WIDTH);b.setMaxWidth(BUTTON_WIDTH);}

    private void showWarn(String msg){ showAlert(Alert.AlertType.WARNING, "选择提示", msg); }
    private void showAlert(Alert.AlertType type,String title,String msg){Alert a=new Alert(type);a.setTitle(title);a.setHeaderText(null);a.setContentText(msg);a.showAndWait();}

    private void seedDemo(){
        allData.clear();
        Course c1 = new Course(); c1.id="CS101"; c1.name="数据结构"; c1.teacher="张老师"; c1.clazz="计科2101"; c1.room="教四-201"; c1.capacity=60; c1.schedule="周二(3-5节), 周四(6-7节)"; c1.students= new ArrayList<>(Arrays.asList("张三","李四","王五"));
        Course c2 = new Course(); c2.id="CS202"; c2.name="计算机网络"; c2.teacher="李老师"; c2.clazz="软工2102"; c2.room="实验中心-204"; c2.capacity=48; c2.schedule="周一(11-13节)"; c2.students= new ArrayList<>(Arrays.asList("赵六","钱七"));
        Course c3 = new Course(); c3.id="MA110"; c3.name="线性代数"; c3.teacher="周老师"; c3.clazz="计科2101"; c3.room="教一-308"; c3.capacity=80; c3.schedule="周五(8-10节)"; c3.students= new ArrayList<>(Arrays.asList("孙八","吴九","郑十"));
        allData.addAll(Arrays.asList(c1,c2,c3));
    }
}
