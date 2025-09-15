package Client.library;

import Client.library.admin.AddBookPanel;
import Client.library.admin.BorrowBookPanel;
import Client.library.admin.ModifyBookPanel;
import Client.library.admin.ReturnBookPanel;
import Client.library.student.LibrarySearchPanel;
import Client.library.student.MyBorrowingsPanel;
import Client.library.student.PaperSketchPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class LibraryMainPanel extends BorderPane {
    private LibrarySearchPanel searchPanel;
    private PaperSketchPanel paperSketchPanel;
    private MyBorrowingsPanel borrowingsPanel;
    private AddBookPanel addBookPanel;
    private ModifyBookPanel modifyBookPanel;
    private BorrowBookPanel borrowBookPanel;
    private ReturnBookPanel returnBookPanel;

    private VBox leftBar;
    private VBox currentSelectedBox;

    private final String cardNumber;
    private final boolean isAdmin;

    public LibraryMainPanel(String cardNumber) {
        this.cardNumber = cardNumber;
        this.isAdmin = isUserAdmin(cardNumber);
        initializeUI();
    }

    private boolean isUserAdmin(String cardNumber) {
        try {
            int num = Integer.parseInt(cardNumber);
            return num <= 1000;
        } catch (Exception e) {
            return false;
        }
    }

    private void initializeUI() {
        // 左侧导航栏
        leftBar = new VBox(12);
        leftBar.setPadding(new Insets(20));
        leftBar.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f9ff, #e8f2ff);");
        leftBar.setPrefWidth(160);

        if (isAdmin) {
            VBox addBookBox = createNavItem("📘 添加书籍");
            VBox modifyBookBox = createNavItem("✏️ 修改书籍");
            VBox borrowBookBox = createNavItem("📖 借书办理");
            VBox returnBookBox = createNavItem("🔄 还书办理");

            leftBar.getChildren().addAll(addBookBox, modifyBookBox, borrowBookBox, returnBookBox);
            setLeft(leftBar);

            // 默认显示添加书籍
            addBookPanel = new AddBookPanel();
            setCenter(addBookPanel);
            selectNavItem(addBookBox);

            // 点击事件
            addBookBox.setOnMouseClicked(e -> switchPanel(addBookBox, () -> {
                if (addBookPanel == null) addBookPanel = new AddBookPanel();
                setCenter(addBookPanel);
            }));

            modifyBookBox.setOnMouseClicked(e -> switchPanel(modifyBookBox, () -> {
                if (modifyBookPanel == null) modifyBookPanel = new ModifyBookPanel();
                setCenter(modifyBookPanel);
            }));

            borrowBookBox.setOnMouseClicked(e -> switchPanel(borrowBookBox, () -> {
                if (borrowBookPanel == null) borrowBookPanel = new BorrowBookPanel();
                setCenter(borrowBookPanel);
            }));

            returnBookBox.setOnMouseClicked(e -> switchPanel(returnBookBox, () -> {
                if (returnBookPanel == null) returnBookPanel = new ReturnBookPanel();
                setCenter(returnBookPanel);
            }));
        } else {
            VBox searchBox = createNavItem("🔍 图书搜索");
            VBox paperSketchBox = createNavItem("📑 文献检索");
            VBox borrowingsBox = createNavItem("📚 我的借阅");

            leftBar.getChildren().addAll(searchBox, paperSketchBox, borrowingsBox);
            setLeft(leftBar);

            // 默认显示搜索
            searchPanel = new LibrarySearchPanel();
            setCenter(searchPanel);
            selectNavItem(searchBox);

            // 点击事件
            searchBox.setOnMouseClicked(e -> switchPanel(searchBox, () -> {
                if (searchPanel == null) searchPanel = new LibrarySearchPanel();
                setCenter(searchPanel);
            }));

            paperSketchBox.setOnMouseClicked(e -> switchPanel(paperSketchBox, () -> {
                paperSketchPanel = new PaperSketchPanel();
                setCenter(paperSketchPanel);
            }));

            borrowingsBox.setOnMouseClicked(e -> switchPanel(borrowingsBox, () -> {
                if (borrowingsPanel == null) {
                    borrowingsPanel = new MyBorrowingsPanel(cardNumber);
                } else {
                    borrowingsPanel.loadBorrowedBooks();
                }
                setCenter(borrowingsPanel);
            }));
        }
    }

    /** 创建一个导航项（卡片风格） */
    private VBox createNavItem(String text) {
        VBox box = new VBox();
        box.setPrefWidth(130);
        box.setPrefHeight(45);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 15, 10, 15));
        box.setSpacing(5);

        Text label = new Text(text);
        label.setStyle("-fx-font-size: 14px; -fx-fill: #2a4d7b;");
        box.getChildren().add(label);

        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);");

        // 鼠标悬浮效果
        box.setOnMouseEntered(e -> {
            if (box != currentSelectedBox) {
                box.setStyle("-fx-background-color: #f0f6ff; -fx-background-radius: 12; "
                        + "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8, 0, 0, 2);");
            }
        });
        box.setOnMouseExited(e -> {
            if (box != currentSelectedBox) {
                box.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);");
            }
        });

        box.setCursor(Cursor.HAND);
        return box;
    }

    /** 切换导航项 */
    private void switchPanel(VBox newBox, Runnable action) {
        if (currentSelectedBox != newBox) {
            resetNavItemStyle(currentSelectedBox);
            selectNavItem(newBox);
            action.run();
        }
    }

    /** 设置选中样式 */
    private void selectNavItem(VBox box) {
        currentSelectedBox = box;
        box.setStyle("-fx-background-color: #e3eeff; -fx-background-radius: 12; "
                + "-fx-border-color: #4e8cff; -fx-border-width: 2; -fx-border-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.4), 10, 0, 0, 3);");
    }

    /** 重置样式 */
    private void resetNavItemStyle(VBox box) {
        if (box != null) {
            box.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);");
        }
    }
}