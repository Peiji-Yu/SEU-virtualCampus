package Client.coursemgmt.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * 课程管理-搜索栏：按学生/教师/教学班搜索，支持模糊（仅“按学生”时显示）。
 * 作者: GitHub Copilot
 */
public class CourseSearchBar extends HBox {
    public interface SearchListener { void onSearch(String type, String value, boolean fuzzy); void onClear(); }

    private static final double BUTTON_WIDTH = 110;
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final TextField keyword = new TextField();
    private final CheckBox fuzzy = new CheckBox("模糊搜索");
    private final Button searchBtn = new Button("搜索");
    private final Button clearBtn = new Button("清空");
    private final SearchListener listener;

    public CourseSearchBar(SearchListener listener) {
        this.listener = listener;
        setSpacing(12);
        setPadding(new Insets(15));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8,0,0,2);");
        buildUi();
        bindEvents();
    }

    private void buildUi() {
        Label label = new Label("搜索条件:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-font-size: 14px;");

        typeBox.getItems().addAll("按学生", "按教师", "按教学班");
        typeBox.setValue("按学生");
        typeBox.setPrefWidth(120);
        typeBox.setPrefHeight(40);
        typeBox.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 1;");

        keyword.setPromptText("请输入搜索内容");
        keyword.setPrefHeight(40);
        keyword.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 1; -fx-font-size: 14px;");
        HBox.setHgrow(keyword, Priority.ALWAYS);

        fuzzy.setSelected(true);
        fuzzy.setStyle("-fx-text-fill: #2a4d7b; -fx-font-size: 14px;");

        stylePrimaryButton(searchBtn);
        stylePrimaryButton(clearBtn);

        getChildren().addAll(label, typeBox, keyword, fuzzy, searchBtn, clearBtn);
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
        typeBox.valueProperty().addListener((o, ov, nv) -> {
            if ("按学生".equals(nv)) {
                if (!getChildren().contains(fuzzy)) { getChildren().add(4, fuzzy); }
                fuzzy.setSelected(true);
            } else {
                getChildren().remove(fuzzy);
            }
        });

        searchBtn.setOnAction(e -> {
            String kw = keyword.getText().trim();
            if (kw.isEmpty()) { showAlert("输入提示", "请输入搜索内容"); return; }
            String type = mapType(typeBox.getValue());
            boolean useFuzzy = "按学生".equals(typeBox.getValue()) && fuzzy.isSelected();
            listener.onSearch(type, kw, useFuzzy);
        });
        clearBtn.setOnAction(e -> { keyword.clear(); listener.onClear(); });
    }

    private String mapType(String display) {
        if ("按教师".equals(display)) { return "byTeacher"; }
        if ("按教学班".equals(display)) { return "byClazz"; }
        return "byStudent";
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
