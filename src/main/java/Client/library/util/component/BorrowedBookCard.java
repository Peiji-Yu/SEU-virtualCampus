package Client.library.util.component;

import Client.library.util.model.BorrowedBook;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.time.format.DateTimeFormatter;

public class BorrowedBookCard extends HBox {
    private BorrowedBook borrowedBook;
    private Button renewButton;
    private Runnable onRenewCallback;

    public BorrowedBookCard(BorrowedBook borrowedBook, Runnable onRenewCallback) {
        this.borrowedBook = borrowedBook;
        this.onRenewCallback = onRenewCallback;
        initializeUI();
    }

    private void initializeUI() {
        setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        setPadding(new Insets(15));
        setSpacing(15);
        setAlignment(Pos.CENTER_LEFT);

        // 书籍信息区域
        VBox bookInfoBox = new VBox(8);
        bookInfoBox.setPrefWidth(500);

        Label titleLabel = new Label(borrowedBook.getBookTitle());
        titleLabel.setFont(Font.font(16));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        Label authorLabel = new Label("作者: " + borrowedBook.getAuthor());
        authorLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        // 修改为使用 UUID 的 toString() 方法
        Label copyLabel = new Label("副本ID: " + borrowedBook.getCopyId().toString() + " | 位置: " + borrowedBook.getLocation());
        copyLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Label dateLabel = new Label("借阅日期: " + borrowedBook.getBorrowDate().format(formatter) +
                " | 应还日期: " + borrowedBook.getDueDate().format(formatter));
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        bookInfoBox.getChildren().addAll(titleLabel, authorLabel, copyLabel, dateLabel);

        // 续借按钮区域
        VBox buttonBox = new VBox();
        buttonBox.setAlignment(Pos.CENTER);

        renewButton = new Button("续借");
        renewButton.setPrefWidth(80);
        renewButton.setPrefHeight(35);

        // 设置按钮样式和可用性
        if (borrowedBook.isCanRenew() && borrowedBook.getTotalBorrowDays() < 60) {
            renewButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
            renewButton.setOnAction(event -> {
                if (onRenewCallback != null) {
                    onRenewCallback.run();
                }
            });
        } else {
            renewButton.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666;");
            renewButton.setDisable(true);

            if (borrowedBook.getTotalBorrowDays() >= 60) {
                renewButton.setTooltip(new javafx.scene.control.Tooltip("总借阅时间已达60天，无法续借"));
            } else if (!borrowedBook.isCanRenew()) {
                renewButton.setTooltip(new javafx.scene.control.Tooltip("当前无法续借"));
            }
        }

        buttonBox.getChildren().add(renewButton);

        getChildren().addAll(bookInfoBox, buttonBox);
    }

    public int getRecordId() {
        return borrowedBook.getRecordId();
    }
}