package Client.library.admin;

import Client.ClientNetworkHelper;
import Client.library.util.model.Book;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModifyBookPanel extends BorderPane {
    private TextField searchField;
    private Button searchButton;
    private Label resultsLabel;
    private VBox booksContainer;
    private ScrollPane scrollPane;
    private Label statusLabel;

    // 添加一个变量来跟踪当前展开的卡片
    private ExpandableBookCard currentExpandedCard;

    private Gson gson;

    public ModifyBookPanel() {
        // 创建配置了LocalDate和UUID适配器的Gson实例
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(15));

        // 顶部搜索区域
        VBox topBox = new VBox(15);

        Label titleLabel = new Label("修改书籍");
        titleLabel.setFont(Font.font(20));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("输入书名、作者或ISBN进行搜索");
        searchField.setPrefWidth(400);
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-font-size: 14px;");

        searchButton = new Button("搜索");
        searchButton.setPrefWidth(80);
        searchButton.setPrefHeight(40);
        searchButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        searchButton.setOnAction(event -> performSearch());

        searchBox.getChildren().addAll(searchField, searchButton);

        // 结果数量标签
        resultsLabel = new Label("就绪");
        resultsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        topBox.getChildren().addAll(titleLabel, searchBox, resultsLabel);
        setTop(topBox);

        // 中心图书列表区域
        booksContainer = new VBox(15);
        booksContainer.setPadding(new Insets(10, 0, 10, 0));

        scrollPane = new ScrollPane(booksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        setCenter(scrollPane);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    private void performSearch() {
        String searchText = searchField.getText().trim();

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                // 构建搜索请求
                Map<String, Object> data = new HashMap<>();
                data.put("searchText", searchText.isEmpty() ? "" : searchText);
                Request request = new Request("searchBooks", data);

//                // 使用ClientNetworkHelper发送请求
//                String response = ClientNetworkHelper.send(request);
//                System.out.println("搜索响应: " + response);

                // 模拟服务器响应
                String response = "{\n" +
                        "  \"code\": 200,\n" +
                        "  \"message\": \"搜索成功\",\n" +
                        "  \"data\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Java编程思想\",\n" +
                        "      \"author\": \"Bruce Eckel\",\n" +
                        "      \"publisher\": \"机械工业出版社\",\n" +
                        "      \"isbn\": \"9787111213826\",\n" +
                        "      \"description\": \"Java学习经典书籍，全面介绍Java编程语言特性和面向对象程序设计思想\",\n" +
                        "      \"totalCopies\": 5,\n" +
                        "      \"availableCopies\": 3,\n" +
                        "      \"copies\": [\n" +
                        "        {\n" +
                        "          \"copyId\": \"1987bb9a-e5d3-4a14-82d7-879e0e15c83c\",\n" +
                        "          \"location\": \"A区3排2架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"f30216a5-09f2-47e4-ad40-3cfdcdc9c0ea\",\n" +
                        "          \"location\": \"A区3排2架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"10537680-9ae7-4d99-9ced-bcf8aa3e30ea\",\n" +
                        "          \"location\": \"A区3排2架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"691df85d-7895-406e-bc75-20ca9f1f208f\",\n" +
                        "          \"location\": \"B区1排5架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"36cab9d8-021a-474f-890a-d64e7e09880a\",\n" +
                        "          \"location\": \"B区1排5架\",\n" +
                        "          \"status\": \"已预约\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"title\": \"深入理解计算机系统\",\n" +
                        "      \"author\": \"Randal E. Bryant, David R. O'Hallaron\",\n" +
                        "      \"publisher\": \"机械工业出版社\",\n" +
                        "      \"isbn\": \"9787111544937\",\n" +
                        "      \"description\": \"计算机系统经典教材，从程序员的视角深入理解计算机系统的工作原理\",\n" +
                        "      \"totalCopies\": 3,\n" +
                        "      \"availableCopies\": 1,\n" +
                        "      \"copies\": [\n" +
                        "        {\n" +
                        "          \"copyId\": \"ff85f69b-ff3a-4b8e-b490-e3956f567e5a\",\n" +
                        "          \"location\": \"C区2排3架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"711bfcc6-99cd-42b5-89e6-930ede35dbc3\",\n" +
                        "          \"location\": \"C区2排3架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"2ad1aee1-25ea-4777-a5de-8e7b144a99c0\",\n" +
                        "          \"location\": \"C区2排3架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"title\": \"算法导论\",\n" +
                        "      \"author\": \"Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein\",\n" +
                        "      \"publisher\": \"机械工业出版社\",\n" +
                        "      \"isbn\": \"9787111407010\",\n" +
                        "      \"description\": \"算法领域的经典权威著作，全面深入地介绍了算法设计与分析\",\n" +
                        "      \"totalCopies\": 4,\n" +
                        "      \"availableCopies\": 2,\n" +
                        "      \"copies\": [\n" +
                        "        {\n" +
                        "          \"copyId\": \"f77c0f64-8fd2-4153-9dc7-2bf7bcadb9bd\",\n" +
                        "          \"location\": \"D区4排1架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"2854bb4f-5321-42eb-ba83-fcc4dbf580db\",\n" +
                        "          \"location\": \"D区4排1架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"8555ebb3-6319-4e75-937c-989f19b5be92\",\n" +
                        "          \"location\": \"D区4排1架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"f0ae2ab2-a52c-45bb-aac6-26b9a49ac880\",\n" +
                        "          \"location\": \"D区4排1架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取图书列表
                    Type bookListType = new TypeToken<List<Book>>(){}.getType();
                    List<Book> books = gson.fromJson(gson.toJson(responseMap.get("data")), bookListType);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        booksContainer.getChildren().clear();
                        // 重置当前展开的卡片
                        currentExpandedCard = null;

                        if (books.isEmpty()) {
                            Label noResults = new Label("未找到相关图书");
                            noResults.setStyle("-fx-text-fill: #666; -fx-font-size: 16px; -fx-padding: 20;");
                            booksContainer.getChildren().add(noResults);
                            resultsLabel.setText("找到 0 本图书");
                        } else {
                            for (Book book : books) {
                                // 创建可折叠的卡片
                                ExpandableBookCard card = new ExpandableBookCard(book);
                                booksContainer.getChildren().add(card);
                            }
                            resultsLabel.setText("找到 " + books.size() + " 本图书");
                        }

                        setStatus("搜索完成");
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("搜索失败: " + responseMap.get("message"));
                        resultsLabel.setText("搜索失败");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage());
                    resultsLabel.setText("通信错误");
                });
                e.printStackTrace();
            }
        }).start();
    }

    // 可折叠的图书卡片内部类
    private class ExpandableBookCard extends VBox {
        private final Book book;
        private boolean isExpanded = false;
        private VBox detailsBox;
        private Button modifyButton;

        public ExpandableBookCard(Book book) {
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
            // 如果已经有展开的卡片且不是当前卡片，则先关闭它
            if (currentExpandedCard != null && currentExpandedCard != this) {
                currentExpandedCard.collapse();
            }

            // 切换当前卡片的展开状态
            if (isExpanded) {
                collapse();
            } else {
                expand();
            }
        }

        public void expand() {
            isExpanded = true;
            // 展开显示详细信息
            showDetails();
            setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 8; " +
                    "-fx-border-color: #4e8cff; -fx-border-radius: 8; -fx-border-width: 2; " +
                    "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 10, 0, 0, 3);");
            // 更新当前展开的卡片
            currentExpandedCard = this;
        }

        public void collapse() {
            if (isExpanded) {
                isExpanded = false;
                if (detailsBox != null) {
                    getChildren().remove(detailsBox);
                    detailsBox = null;
                }
                setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
                // 如果当前展开的卡片是这张卡片，则清除引用
                if (currentExpandedCard == this) {
                    currentExpandedCard = null;
                }
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

            // 修改按钮
            modifyButton = new Button("修改");
            modifyButton.setPrefWidth(80);
            modifyButton.setPrefHeight(30);
            modifyButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
            modifyButton.setOnAction(e -> {
                // 阻止事件冒泡，避免触发卡片的点击事件
                e.consume();
                openModifyDialog(book);
            });

            HBox buttonBox = new HBox();
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            buttonBox.getChildren().add(modifyButton);

            detailsBox.getChildren().add(buttonBox);
            getChildren().add(detailsBox);
        }
    }

    private void openModifyDialog(Book book) {
        // 创建修改对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("修改书籍信息");
        dialog.setHeaderText("修改《" + book.getTitle() + "》的信息");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(book.getTitle());
        TextField authorField = new TextField(book.getAuthor());
        TextField publisherField = new TextField(book.getPublisher());
        DatePicker publishDatePicker = new DatePicker(book.getPublishDate());
        TextArea descriptionArea = new TextArea(book.getDescription());
        descriptionArea.setPrefRowCount(3);
        TextField categoryField = new TextField(book.getCategory());

        grid.add(new Label("书名:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("作者:"), 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(new Label("出版社:"), 0, 2);
        grid.add(publisherField, 1, 2);
        grid.add(new Label("出版日期:"), 0, 3);
        grid.add(publishDatePicker, 1, 3);
        grid.add(new Label("简介:"), 0, 4);
        grid.add(descriptionArea, 1, 4);
        grid.add(new Label("类别:"), 0, 5);
        grid.add(categoryField, 1, 5);

        // 副本管理区域
        Label copiesLabel = new Label("副本管理:");
        copiesLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        grid.add(copiesLabel, 0, 6, 2, 1);

        // 副本表格
        TableView<Book.BookCopy> copiesTable = new TableView<>();
        ObservableList<Book.BookCopy> copiesData = FXCollections.observableArrayList(book.getCopies());
        copiesTable.setItems(copiesData);
        copiesTable.setPrefHeight(150);

        TableColumn<Book.BookCopy, String> idColumn = new TableColumn<>("副本ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("copyId"));
        idColumn.setPrefWidth(100);

        TableColumn<Book.BookCopy, String> locationColumn = new TableColumn<>("馆藏位置");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationColumn.setPrefWidth(150);

        TableColumn<Book.BookCopy, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);

        copiesTable.getColumns().addAll(idColumn, locationColumn, statusColumn);
        grid.add(copiesTable, 0, 7, 2, 1);

        // 添加和删除副本按钮
        HBox copiesButtonBox = new HBox(10);
        copiesButtonBox.setAlignment(Pos.CENTER_RIGHT);
        copiesButtonBox.setPadding(new Insets(10, 0, 0, 0));

        Button addCopyButton = new Button("添加副本");
        addCopyButton.setOnAction(e -> {
            // 弹出添加副本对话框
            Dialog<Book.BookCopy> addCopyDialog = new Dialog<>();
            addCopyDialog.setTitle("添加副本");
            addCopyDialog.setHeaderText("添加新副本");

            GridPane addCopyGrid = new GridPane();
            addCopyGrid.setHgap(10);
            addCopyGrid.setVgap(10);
            addCopyGrid.setPadding(new Insets(20, 150, 10, 10));

            TextField locationField = new TextField();
            locationField.setPromptText("输入馆藏位置");

            addCopyGrid.add(new Label("位置:"), 0, 0);
            addCopyGrid.add(locationField, 1, 0);

            addCopyDialog.getDialogPane().setContent(addCopyGrid);
            addCopyDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            addCopyDialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    Book.BookCopy newCopy = new Book.BookCopy();
                    newCopy.setCopyId(UUID.randomUUID());
                    newCopy.setLocation(locationField.getText());
                    newCopy.setStatus("可借");
                    return newCopy;
                }
                return null;
            });

            addCopyDialog.showAndWait().ifPresent(newCopy -> {
                copiesData.add(newCopy);
                book.setCopies(copiesData);
                book.setTotalCopies(copiesData.size());
                // 更新可借数量
                long availableCount = copiesData.stream()
                        .filter(copy -> "可借".equals(copy.getStatus()))
                        .count();
                book.setAvailableCopies((int) availableCount);
            });
        });

        Button deleteCopyButton = new Button("删除选中副本");
        deleteCopyButton.setOnAction(e -> {
            Book.BookCopy selectedCopy = copiesTable.getSelectionModel().getSelectedItem();
            if (selectedCopy != null) {
                // 确认删除对话框
                Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmDialog.setTitle("确认删除");
                confirmDialog.setHeaderText("确认删除副本");
                confirmDialog.setContentText("确定要删除这个副本吗？");

                confirmDialog.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // 检查是否是最后一个副本
                        if (copiesData.size() == 1) {
                            // 确认删除整个书籍
                            Alert deleteBookDialog = new Alert(Alert.AlertType.CONFIRMATION);
                            deleteBookDialog.setTitle("确认删除书籍");
                            deleteBookDialog.setHeaderText("这是最后一个副本");
                            deleteBookDialog.setContentText("删除最后一个副本将同时删除整个书籍。确定要继续吗？");

                            deleteBookDialog.showAndWait().ifPresent(bookResponse -> {
                                if (bookResponse == ButtonType.OK) {
                                    // 删除整个书籍
                                    deleteBook(book);
                                    dialog.close();
                                }
                            });
                        } else {
                            copiesData.remove(selectedCopy);
                            book.setCopies(copiesData);
                            book.setTotalCopies(copiesData.size());
                            // 更新可借数量
                            long availableCount = copiesData.stream()
                                    .filter(copy -> "可借".equals(copy.getStatus()))
                                    .count();
                            book.setAvailableCopies((int) availableCount);
                        }
                    }
                });
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("未选择副本");
                alert.setContentText("请先选择一个要删除的副本。");
                alert.showAndWait();
            }
        });

        copiesButtonBox.getChildren().addAll(addCopyButton, deleteCopyButton);
        grid.add(copiesButtonBox, 0, 8, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // 更新书籍信息
                book.setTitle(titleField.getText());
                book.setAuthor(authorField.getText());
                book.setPublisher(publisherField.getText());
                book.setPublishDate(publishDatePicker.getValue());
                book.setDescription(descriptionArea.getText());
                book.setCategory(categoryField.getText());

                // 发送更新请求
                updateBook(book);
            }
            // 对话框关闭后，确保当前展开的卡片状态正确
            if (currentExpandedCard != null) {
                currentExpandedCard.expand(); // 重新展开以刷新内容
            }
        });
    }

    private void updateBook(Book book) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("更新中..."));

                // 构建更新请求
                Map<String, Object> data = new HashMap<>();
                data.put("book", book);
                Request request = new Request("updateBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("更新响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("更新成功");
                        // 刷新搜索结果
                        performSearch();
                    });
                } else {
                    Platform.runLater(() ->
                            setStatus("更新失败: " + responseMap.get("message")));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("更新错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteBook(Book book) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("删除中..."));

                // 构建删除请求
                Map<String, Object> data = new HashMap<>();
                data.put("isbn", book.getIsbn());
                Request request = new Request("deleteBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("删除响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("删除成功");
                        // 刷新搜索结果
                        performSearch();
                    });
                } else {
                    Platform.runLater(() ->
                            setStatus("删除失败: " + responseMap.get("message")));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("删除错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
