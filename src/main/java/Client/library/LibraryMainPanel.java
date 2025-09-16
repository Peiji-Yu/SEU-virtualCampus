package Client.library;

import Client.library.admin.AddBookPanel;
import Client.library.admin.BorrowBookPanel;
import Client.library.admin.ModifyBookPanel;
import Client.library.admin.ReturnBookPanel;
import Client.library.student.LibrarySearchPanel;
import Client.library.student.MyBorrowingsPanel;
import Client.library.student.PaperSketchPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
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
        VBox leftBar = new VBox();

        // 设置样式，添加向内阴影效果
        leftBar.setStyle("-fx-background-color: #f4f4f4;"
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.2), 10, 0, 1, 0);");
        leftBar.setPrefWidth(210);

        //设置说明标签
        Label leftLabel = new Label("图书馆");
        leftLabel.setStyle("-fx-text-fill: #303030; -fx-font-family: 'Microsoft YaHei UI'; " +
                "-fx-font-size: 12px; -fx-alignment: center-left; -fx-padding: 10 0 10 15;");

        // 添加分割线
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator.setMaxWidth(Double.MAX_VALUE);

        // 如果是管理员，添加管理功能按钮
        if (isAdmin) {
            Button addBookButton = new Button("添加书籍");
            addBookButton.setPrefWidth(210);
            addBookButton.setPrefHeight(56);
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

            // 添加分割线
            Region separator1 = new Region();
            separator1.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator1.setMaxWidth(Double.MAX_VALUE);

            Button modifyBookButton = new Button("管理书籍");
            modifyBookButton.setPrefWidth(210);
            modifyBookButton.setPrefHeight(56);
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

            // 添加分割线
            Region separator2 = new Region();
            separator2.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator2.setMaxWidth(Double.MAX_VALUE);

            // 添加办理借书按钮
            Button borrowBookButton = new Button("办理借书");
            borrowBookButton.setPrefWidth(210);
            borrowBookButton.setPrefHeight(56);
            resetButtonStyle(borrowBookButton);

            borrowBookButton.setOnAction(e -> {
                if (currentSelectedButton != borrowBookButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(borrowBookButton);
                    currentSelectedButton = borrowBookButton;

                    // 初始化办理借书页面
                    if (borrowBookPanel == null) {
                        borrowBookPanel = new BorrowBookPanel();
                    }
                    setCenter(borrowBookPanel);
                }
            });

            // 添加分割线
            Region separator3 = new Region();
            separator3.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator3.setMaxWidth(Double.MAX_VALUE);

            // 添加办理还书按钮
            Button returnBookButton = new Button("办理还书");
            returnBookButton.setPrefWidth(210);
            returnBookButton.setPrefHeight(56);
            resetButtonStyle(returnBookButton);

            returnBookButton.setOnAction(e -> {
                if (currentSelectedButton != returnBookButton) {
                    resetButtonStyle(currentSelectedButton);
                    setSelectedButtonStyle(returnBookButton);
                    currentSelectedButton = returnBookButton;

                    // 初始化办理还书页面
                    if (returnBookPanel == null) {
                        returnBookPanel = new ReturnBookPanel();
                    }
                    setCenter(returnBookPanel);
                }
            });

            // 添加分割线
            Region separator4 = new Region();
            separator4.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator4.setMaxWidth(Double.MAX_VALUE);

            leftBar.getChildren().addAll(leftLabel, separator,
                    addBookButton, separator1,
                    modifyBookButton, separator2,
                    borrowBookButton, separator3,
                    returnBookButton, separator4);
            setLeft(leftBar);

            // 初始化默认面板
            addBookPanel = new AddBookPanel();
            setCenter(addBookPanel);
        }
        else {
            // 图书搜索按钮
            Button searchButton = new Button("搜索图书");
            searchButton.setPrefWidth(210);
            searchButton.setPrefHeight(56);
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

            // 添加分割线
            Region separator1 = new Region();
            separator1.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator1.setMaxWidth(Double.MAX_VALUE);

            // 我的借阅按钮
            Button borrowingsButton = new Button("我的借阅");
            borrowingsButton.setPrefWidth(210);
            borrowingsButton.setPrefHeight(56);
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

            // 添加分割线
            Region separator2 = new Region();
            separator2.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator2.setMaxWidth(Double.MAX_VALUE);

            // 文献检索按钮
            Button papersketchButton = new Button("文献检索");
            papersketchButton.setPrefWidth(210);
            papersketchButton.setPrefHeight(56);
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

            // 添加分割线
            Region separator3 = new Region();
            separator3.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator3.setMaxWidth(Double.MAX_VALUE);

            leftBar.getChildren().addAll(leftLabel, separator,
                    searchButton, separator1,
                    borrowingsButton, separator2,
                    papersketchButton, separator3);
            setLeft(leftBar);

            // 初始化面板
            searchPanel = new LibrarySearchPanel();
            setCenter(searchPanel);
        }
    }

    private void setSelectedButtonStyle(Button button) {
        button.setStyle("-fx-font-family: 'Microsoft YaHei UI'; -fx-font-size: 16px; " +
                "-fx-background-color: #176B3A; -fx-text-fill: white; " +
                "-fx-alignment: center-left; -fx-padding: 0 0 0 56;");
    }

    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-family: 'Microsoft YaHei UI'; -fx-font-size: 16px; " +
                "-fx-background-color: #f4f4f4; -fx-text-fill: black; " +
                "-fx-alignment: center-left;  -fx-padding: 0 0 0 60;");
    }
}