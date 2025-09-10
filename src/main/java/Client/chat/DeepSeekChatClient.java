package Client.chat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 浅色模式 DeepSeek 聊天界面（仿照图片风格）
 */
public class DeepSeekChatClient extends Application {

    private BorderPane root;
    private VBox sidebar;
    private VBox messagesBox;
    private ScrollPane messagesScrollPane;
    private TextArea inputArea;
    private Button sendButton;
    private ProgressIndicator loadingIndicator;
    private Label centerTitle;
    private VBox sessionList;

    private List<ChatMessage> chatHistory = new ArrayList<>();
    private String apiKey = "YOUR_DEEPSEEK_API_KEY";
    private String model = "deepseek-chat";

    // 颜色
    private static final String BG = "#ffffff";
    private static final String SIDEBAR_BG = "#f5f6f8";
    private static final String ACCENT = "#6c5ce7";
    private static final String TEXT = "#111827";
    private static final String MUTED = "#6b7280";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("DeepSeek - 浅色模式");
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + "; -fx-font-family: 'Microsoft YaHei', 'Segoe UI';");

        createSidebar();
        createCenterArea();

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createSidebar() {
        sidebar = new VBox();
        sidebar.setPadding(new Insets(20));
        sidebar.setSpacing(16);
        sidebar.setPrefWidth(300);
        sidebar.setStyle("-fx-background-color: " + SIDEBAR_BG + ";");

        // logo
        Label logo = new Label("deepseek");
        logo.setTextFill(Color.web(ACCENT));
        logo.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20));

        // 新建对话按钮
        Button newChat = new Button("＋ 开启新对话");
        newChat.setMaxWidth(Double.MAX_VALUE);
        newChat.setStyle("-fx-background-color: white; -fx-text-fill: " + TEXT + "; -fx-background-radius: 999; -fx-border-color: transparent;");
        newChat.setOnAction(e -> startNewChat());
        newChat.setPadding(new Insets(10));

        // 会话列表标题
        Label listTitle = new Label("最近对话");
        listTitle.setTextFill(Color.web(MUTED));
        listTitle.setFont(Font.font("Microsoft YaHei", FontWeight.SEMI_BOLD, 12));

        // 会话列表
        sessionList = new VBox();
        sessionList.setSpacing(6);
        sessionList.setPadding(new Insets(6, 0, 0, 0));

        // 添加示例会话
        addSessionItem("IDEA中使用JavaFX设计前端界面...");
        addSessionItem("智慧校园系统设计说明书编写");
        addSessionItem("论文proof reading错误纠正建议");

        // 底部用户简要
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label user = new Label("hz****05@sina.com");
        user.setTextFill(Color.web(MUTED));
        user.setFont(Font.font(12));

        sidebar.getChildren().addAll(logo, newChat, listTitle, sessionList, spacer, user);
        root.setLeft(sidebar);
    }

    private void addSessionItem(String title) {
        HBox item = new HBox();
        item.setAlignment(Pos.CENTER_LEFT);
        item.setSpacing(10);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: rgba(0,0,0,0.03); -fx-background-radius: 8;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;"));

        Circle dot = new Circle(8, Color.web("#d1d5db"));
        Label t = new Label(title);
        t.setTextFill(Color.web(TEXT));
        t.setFont(Font.font(13));
        t.setMaxWidth(180);
        t.setWrapText(true);

        item.getChildren().addAll(dot, t);
        item.setOnMouseClicked(e -> {
            // 切换会话：清空当前消息（示例）
            messagesBox.getChildren().clear();
            chatHistory.clear();
            centerTitle.setText("今天有什么可以帮到你?");
        });

        sessionList.getChildren().add(item);
    }

    private void createCenterArea() {
        StackPane centerPane = new StackPane();
        centerPane.setStyle("-fx-background-color: " + BG + ";");

        VBox container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.setSpacing(30);
        container.setPadding(new Insets(60));

        centerTitle = new Label("今天有什么可以帮到你?");
        centerTitle.setTextFill(Color.web(TEXT));
        centerTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));

        // messages area (empty in center design)
        messagesBox = new VBox();
        messagesBox.setSpacing(12);
        messagesBox.setPadding(new Insets(10));
        messagesBox.setPrefHeight(360);

        messagesScrollPane = new ScrollPane(messagesBox);
        messagesScrollPane.setFitToWidth(true);
        messagesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagesScrollPane.setPrefHeight(360);
        messagesScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        // 中央输入卡片（大圆角、阴影）
        HBox inputCard = new HBox();
        inputCard.setAlignment(Pos.CENTER_LEFT);
        inputCard.setPadding(new Insets(14));
        inputCard.setSpacing(10);
        inputCard.setMaxWidth(760);
        inputCard.setStyle("-fx-background-color: white; -fx-background-radius: 999; -fx-border-radius: 999;");
        inputCard.setEffect(new DropShadow(14, Color.rgb(15, 23, 42, 0.06)));

        inputArea = new TextArea();
        inputArea.setPromptText("给 DeepSeek 发送消息");
        inputArea.setPrefRowCount(1);
        inputArea.setWrapText(true);
        inputArea.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 14px;");
        inputArea.setPrefWidth(600);
        inputArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    if (!event.isShiftDown()) {
                        event.consume();
                        sendMessage();
                    }
                    break;
                default:
            }
        });

        // 两个模式按钮（示例）
        Button mode1 = new Button("深度思考");
        mode1.setStyle("-fx-background-color: transparent; -fx-border-color: #e6e7eb; -fx-border-radius: 999; -fx-padding: 6 12; -fx-text-fill: " + MUTED + ";");
        Button mode2 = new Button("联网搜索");
        mode2.setStyle("-fx-background-color: transparent; -fx-border-color: #e6e7eb; -fx-border-radius: 999; -fx-padding: 6 12; -fx-text-fill: " + MUTED + ";");

        VBox leftBox = new VBox(mode1, mode2);
        leftBox.setSpacing(8);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(28, 28);

        sendButton = new Button("→");
        sendButton.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-background-radius: 999; -fx-min-width: 44; -fx-min-height: 44;");
        sendButton.setOnAction(e -> sendMessage());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        inputCard.getChildren().addAll(leftBox, inputArea, spacer, loadingIndicator, sendButton);

        container.getChildren().addAll(centerTitle, inputCard);
        centerPane.getChildren().add(container);

        root.setCenter(centerPane);
    }

    private void sendMessage() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;

        // 显示用户消息（右侧样式）
        addUserMessage(text);
        inputArea.clear();

        loadingIndicator.setVisible(true);
        sendButton.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                // 这里调用真实API前可加入 chatHistory 等
                return callDeepSeekAPI(text);
            }

            @Override
            protected void succeeded() {
                String resp = getValue();
                Platform.runLater(() -> {
                    addAIMessage(resp);
                    loadingIndicator.setVisible(false);
                    sendButton.setDisable(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    addAIMessage("抱歉，发生错误: " + getException().getMessage());
                    loadingIndicator.setVisible(false);
                    sendButton.setDisable(false);
                });
            }
        };
        new Thread(task).start();
    }

    private void addUserMessage(String message) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(6));

        Label bubble = new Label(message);
        bubble.setStyle("-fx-background-color: #eef2ff; -fx-text-fill: " + TEXT + "; -fx-padding: 10 14; -fx-background-radius: 16;");
        bubble.setMaxWidth(560);
        bubble.setWrapText(true);

        box.getChildren().add(bubble);
        messagesBox.getChildren().add(box);
        chatHistory.add(new ChatMessage("user", message));
    }

    private void addAIMessage(String message) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6));
        VBox avatarAndBubble = new VBox();
        avatarAndBubble.setSpacing(6);

        // 小头像
        Circle avatar = new Circle(18, Color.web(ACCENT));
        Label a = new Label("D");
        a.setTextFill(Color.WHITE);
        a.setFont(Font.font(12));
        StackPane avatarStack = new StackPane(avatar, a);

        Label bubble = new Label(message);
        bubble.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: " + TEXT + "; -fx-padding: 10 14; -fx-background-radius: 16;");
        bubble.setMaxWidth(560);
        bubble.setWrapText(true);

        avatarAndBubble.getChildren().addAll(avatarStack, bubble);
        box.getChildren().add(avatarAndBubble);
        messagesBox.getChildren().add(box);
        chatHistory.add(new ChatMessage("assistant", message));
    }

    private String callDeepSeekAPI(String message) throws IOException {
        // 真实调用请替换此方法实现。当前返回模拟回复以便界面展示。
        try {
            Thread.sleep(600); // 模拟网络延迟
        } catch (InterruptedException ignored) {
        }
        return "这是 DeepSeek 的模拟回复：已收到 \"" + (message.length() > 60 ? message.substring(0, 60) + "..." : message) + "\"。";
    }

    private void startNewChat() {
        chatHistory.clear();
        messagesBox.getChildren().clear();
        centerTitle.setText("今天有什么可以帮到你?");
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static class ChatMessage {
        private final String role;
        private final String content;
        ChatMessage(String role, String content) { this.role = role; this.content = content; }
        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
