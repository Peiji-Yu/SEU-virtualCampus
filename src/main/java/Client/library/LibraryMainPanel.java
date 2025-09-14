package Client.library;

import Client.library.admin.AddBookPanel;
import Client.library.admin.BorrowBookPanel;
import Client.library.admin.ModifyBookPanel;
import Client.library.admin.ReturnBookPanel;
import Client.library.student.LibrarySearchPanel;
import Client.library.student.MyBorrowingsPanel;
import Client.library.student.PaperSketchPanel;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class LibraryMainPanel extends BorderPane {
    private LibrarySearchPanel searchPanel;
    private PaperSketchPanel paperSketchPanel;
    private MyBorrowingsPanel borrowingsPanel;
    private AddBookPanel addBookPanel;
    private ModifyBookPanel modifyBookPanel;
    private BorrowBookPanel borrowBookPanel;
    private ReturnBookPanel returnBookPanel;
    private Button currentSelectedButton;
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
        VBox leftBar = new VBox(10);
        leftBar.setPadding(new Insets(15));
        leftBar.setStyle("-fx-background-color: #e8f2ff; -fx-background-radius: 12;");
        leftBar.setPrefWidth(150);

        // 如果是管理员，添加管理功能按钮
        if (isAdmin) {
            Button addBookButton = new Button("添加书籍");
            addBookButton.setPrefWidth(120);
            addBookButton.setPrefHeight(40);
            setSelectedButtonStyle(addBookButton);
            currentSelectedButton = addBookButton;

            addBookButton.setOnAction(e -> {
                if (currentSelectedButton != addBookButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(addBookButton);
                    currentSelectedButton = addBookButton;
                    setCenter(addBookPanel);
                }
            });

            Button modifyBookButton = new Button("修改书籍");
            modifyBookButton.setPrefWidth(120);
            modifyBookButton.setPrefHeight(40);
            resetButtonStyle(modifyBookButton);

            modifyBookButton.setOnAction(e -> {
                if (currentSelectedButton != modifyBookButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(modifyBookButton);
                    currentSelectedButton = modifyBookButton;

                    // 初始化修改书籍页面
                    if (modifyBookPanel == null) {
                        modifyBookPanel = new ModifyBookPanel();
                    }
                    setCenter(modifyBookPanel);
                }
            });

            // 添加借书办理按钮
            Button borrowBookButton = new Button("借书办理");
            borrowBookButton.setPrefWidth(120);
            borrowBookButton.setPrefHeight(40);
            resetButtonStyle(borrowBookButton);

            borrowBookButton.setOnAction(e -> {
                if (currentSelectedButton != borrowBookButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(borrowBookButton);
                    currentSelectedButton = borrowBookButton;

                    // 初始化借书办理页面
                    if (borrowBookPanel == null) {
                        borrowBookPanel = new BorrowBookPanel();
                    }
                    setCenter(borrowBookPanel);
                }
            });

            // 添加还书办理按钮
            Button returnBookButton = new Button("还书办理");
            returnBookButton.setPrefWidth(120);
            returnBookButton.setPrefHeight(40);
            resetButtonStyle(returnBookButton);

            returnBookButton.setOnAction(e -> {
                if (currentSelectedButton != returnBookButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(returnBookButton);
                    currentSelectedButton = returnBookButton;

                    // 初始化还书办理页面
                    if (returnBookPanel == null) {
                        returnBookPanel = new ReturnBookPanel();
                    }
                    setCenter(returnBookPanel);
                }
            });

            leftBar.getChildren().addAll(addBookButton, modifyBookButton, borrowBookButton, returnBookButton);
            setLeft(leftBar);

            // 初始化默认面板
            addBookPanel = new AddBookPanel();
            setCenter(addBookPanel);
        }
        else {
            // 图书搜索按钮
            Button searchButton = new Button("图书搜索");
            searchButton.setPrefWidth(120);
            searchButton.setPrefHeight(40);
            setSelectedButtonStyle(searchButton);
            currentSelectedButton = searchButton;

            searchButton.setOnAction(e -> {
                if (currentSelectedButton != searchButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(searchButton);
                    currentSelectedButton = searchButton;
                    setCenter(searchPanel);
                }
            });

            // 文献检索按钮
            Button papersketchButton = new Button("文献检索");
            papersketchButton.setPrefWidth(120);
            papersketchButton.setPrefHeight(40);
            resetButtonStyle(papersketchButton);

            papersketchButton.setOnAction(e -> {
                if (currentSelectedButton != papersketchButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(papersketchButton);
                    currentSelectedButton = papersketchButton;

                    // 初始化文献检索页面
                    paperSketchPanel = new PaperSketchPanel();
                    setCenter(paperSketchPanel);
                }
            });

            // 我的借阅按钮
            Button borrowingsButton = new Button("我的借阅");
            borrowingsButton.setPrefWidth(120);
            borrowingsButton.setPrefHeight(40);
            resetButtonStyle(borrowingsButton);

            borrowingsButton.setOnAction(e -> {
                if (currentSelectedButton != borrowingsButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(borrowingsButton);
                    currentSelectedButton = borrowingsButton;

                    // 初始化或刷新我的借阅页面
                    if (borrowingsPanel == null) {
                        borrowingsPanel = new MyBorrowingsPanel(cardNumber);
                    } else {
                        borrowingsPanel.loadBorrowedBooks();
                    }
                    setCenter(borrowingsPanel);
                }
            });

            leftBar.getChildren().addAll(searchButton, papersketchButton, borrowingsButton);
            setLeft(leftBar);

            // 初始化面板
            searchPanel = new LibrarySearchPanel();
            setCenter(searchPanel);
        }
    }

    private void setSelectedButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-border-color: #4e8cff; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-background-color: #f8fbff; -fx-text-fill: #4e8cff; " +
                "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8, 0, 0, 2);");
    }

    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 14px; -fx-background-radius: 10; " +
                "-fx-background-color: white; -fx-text-fill: #2a4d7b; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");
    }
}