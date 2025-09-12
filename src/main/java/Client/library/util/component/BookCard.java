package Client.library.util.component;

import Client.library.util.model.Book;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class BookCard extends VBox {
    private final Book book;
    private boolean isExpanded = false;
    private VBox detailsBox;

    public BookCard(Book book) {
        this.book = book;
        initializeUI();

        // 设置点击事件
        setOnMouseClicked(event -> {
            // 阻止事件冒泡，避免被父容器处理
            event.consume();
            toggleExpand();
        });
    }

    private void initializeUI() {
        setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        setPadding(new Insets(15));
        setSpacing(10);

        // 基本信息区域
        HBox basicInfoBox = new HBox(10);
        basicInfoBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleAuthorBox = new VBox(5);
        titleAuthorBox.setPrefWidth(400);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.setFont(Font.font(16));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        Label authorLabel = new Label(book.getAuthor() + " | " + book.getPublisher());
        authorLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        titleAuthorBox.getChildren().addAll(titleLabel, authorLabel);

        HBox copiesBox = new HBox(10);
        copiesBox.setAlignment(Pos.CENTER_RIGHT);
        copiesBox.setPrefWidth(200);

        Label totalLabel = new Label("馆藏: " + book.getTotalCopies());
        totalLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        Label availableLabel = new Label("可借: " + book.getAvailableCopies());
        availableLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px; -fx-font-weight: bold;");

        copiesBox.getChildren().addAll(totalLabel, availableLabel);

        basicInfoBox.getChildren().addAll(titleAuthorBox, copiesBox);
        getChildren().add(basicInfoBox);
    }

    public void toggleExpand() {
        isExpanded = !isExpanded;

        if (isExpanded) {
            // 展开显示详细信息
            showDetails();
            setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 8; " +
                    "-fx-border-color: #4e8cff; -fx-border-radius: 8; -fx-border-width: 2; " +
                    "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 10, 0, 0, 3);");
        } else {
            // 折叠隐藏详细信息
            if (detailsBox != null) {
                getChildren().remove(detailsBox);
                detailsBox = null;
            }
            setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                    "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        }
    }

    private void showDetails() {
        if (detailsBox != null) {
            getChildren().remove(detailsBox);
        }

        detailsBox = new VBox(10);
        detailsBox.setStyle("-fx-padding: 10 0 0 0; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        // ISBN和简介
        if (book.getIsbn() != null && !book.getIsbn().isEmpty()) {
            Label isbnLabel = new Label("ISBN: " + book.getIsbn());
            isbnLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            detailsBox.getChildren().add(isbnLabel);
        }

        Label descLabel = new Label("简介: " +
                (book.getDescription() != null && !book.getDescription().isEmpty() ?
                        book.getDescription() : "暂无简介"));
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        descLabel.setWrapText(true);
        detailsBox.getChildren().add(descLabel);

        // 副本信息表格
        if (book.getCopies() != null && !book.getCopies().isEmpty()) {
            Label copiesTitle = new Label("副本信息:");
            copiesTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-font-size: 14px;");

            TableView<Book.BookCopy> copiesTable = new TableView<>();
            copiesTable.setPrefHeight(150);
            copiesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Book.BookCopy, String> idColumn = new TableColumn<>("ID");
            idColumn.setCellValueFactory(new PropertyValueFactory<>("copyId"));
            idColumn.setPrefWidth(80);

            TableColumn<Book.BookCopy, String> locationColumn = new TableColumn<>("馆藏位置");
            locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
            locationColumn.setPrefWidth(150);

            TableColumn<Book.BookCopy, String> statusColumn = new TableColumn<>("状态");
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusColumn.setPrefWidth(100);

            copiesTable.getColumns().addAll(idColumn, locationColumn, statusColumn);
            copiesTable.getItems().addAll(book.getCopies());

            detailsBox.getChildren().addAll(copiesTitle, copiesTable);
        }

        getChildren().add(detailsBox);
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void collapse() {
        if (isExpanded) {
            toggleExpand();
        }
    }
}