package Client.panel.student.admin;

import Client.util.UIUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * 学籍搜索栏组件（已移至 admin 包，仅管理员使用）。
 * 提供搜索类型选择、关键字输入、模糊开关、搜索与清空按钮。
 * 若后续需要学生端共用，可再抽回 common。
 * 作者: @Msgo-srAm
 */
public class StudentSearchBar extends HBox {
    public interface SearchListener {void onSearch(String type, String value, boolean fuzzy);void onClear();}
    private static final double BUTTON_WIDTH = 110;
    private final ComboBox<String> typeCombo = new ComboBox<>();
    private final TextField keywordField = new TextField();
    private final CheckBox fuzzyCheck = new CheckBox("模糊搜索");
    private final Button searchButton = new Button("搜索");
    private final Button clearButton = new Button("清空");
    private final SearchListener listener;
    public StudentSearchBar(SearchListener listener) {
        this.listener = listener;
        setSpacing(12);
        setPadding(new Insets(15));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8,0,0,2);");
        buildUI();
        bindEvents();
    }
    private void buildUI() {
        Label label = new Label("搜索条件:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-font-size: 14px;");
        typeCombo.getItems().addAll("按姓名","按学号","按一卡通号");
        typeCombo.setValue("按姓名");
        typeCombo.setPrefWidth(120);
        typeCombo.setPrefHeight(40);
        typeCombo.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 1;");
        keywordField.setPromptText("请输入搜索内容");
        keywordField.setPrefHeight(40);
        keywordField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 1; -fx-font-size: 14px;");
        HBox.setHgrow(keywordField, Priority.ALWAYS);
        fuzzyCheck.setSelected(true);
        fuzzyCheck.setStyle("-fx-text-fill: #2a4d7b; -fx-font-size: 14px;");
        stylePrimaryButton(searchButton);
        stylePrimaryButton(clearButton);
        getChildren().addAll(label,typeCombo,keywordField,fuzzyCheck,searchButton,clearButton);
    }
    private void stylePrimaryButton(Button b) {
        b.setPrefHeight(40);
        b.setPrefWidth(BUTTON_WIDTH);
        b.setMinWidth(BUTTON_WIDTH);
        b.setMaxWidth(BUTTON_WIDTH);
        b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: #4e8cff; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);");
        b.setOnMouseEntered(e -> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: #3d7bff; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(61,123,255,0.4), 10,0,0,3);"));
        b.setOnMouseExited(e -> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: #4e8cff; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);"));
    }
    private void bindEvents() {
        typeCombo.valueProperty().addListener((o,ov,nv)->{
            if("按姓名".equals(nv)) {
                if(!getChildren().contains(fuzzyCheck)) {getChildren().add(4,fuzzyCheck);}
                fuzzyCheck.setSelected(true);
            } else {
                getChildren().remove(fuzzyCheck);
            }
        });
        searchButton.setOnAction(e -> {
            String kw = keywordField.getText().trim();
            if(kw.isEmpty()) {showAlert("输入提示","请输入搜索内容");return;}
            String type = mapSearchType(typeCombo.getValue());
            boolean fuzzy = "按姓名".equals(typeCombo.getValue()) && fuzzyCheck.isSelected();
            listener.onSearch(type,kw,fuzzy);
        });
        clearButton.setOnAction(e -> {keywordField.clear();listener.onClear();});
    }
    private String mapSearchType(String display){
        if("按学号".equals(display)) {return "byStudentNumber";}
        if("按一卡通号".equals(display)) {return "byCardNumber";}
        return "byName";
    }
    private void showAlert(String title,String msg){
        Alert alert=new Alert(Alert.AlertType.WARNING,msg,ButtonType.OK);
        alert.setHeaderText(title);
        UIUtil.applyLogoToAlert(alert);
        alert.showAndWait();
    }
}
