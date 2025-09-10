package Client.DeepSeekChat;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

// Gson导入
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class DeepSeekChat extends Application {

    private VBox chatContainer;
    private TextField inputField;
    private Button sendButton;
    private ScrollPane scrollPane;
    private List<JsonObject> conversationHistory = new ArrayList<>();
    private String apiKey = "sk-8e26ac6a67c6403aa419ce1d31c46f45"; // 请替换为您的API密钥
    private String apiUrl = "https://api.deepseek.com/v1/chat/completions"; // 根据实际API调整

    // Gson实例
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void start(Stage primaryStage) {
        // 创建主布局
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // 创建顶部标题栏
        HBox header = createHeader();
        root.setTop(header);

        // 创建聊天区域
        scrollPane = createChatArea();
        root.setCenter(scrollPane);

        // 创建底部输入区域
        HBox inputArea = createInputArea();
        root.setBottom(inputArea);

        // 创建场景
        Scene scene = new Scene(root, 900, 700);

        // 加载浅色模式CSS
        try {
            String cssPath = getClass().getResource("/Css/DeepSeekChat.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            System.out.println("浅色模式CSS加载成功: " + cssPath);
        } catch (NullPointerException e) {
            System.err.println("CSS文件未找到! 使用默认样式");
        }

        // 设置舞台
        primaryStage.setTitle("DeepSeek Chat");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 初始聚焦到输入框
        Platform.runLater(() -> inputField.requestFocus());
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);

        // 添加Logo
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/Image/deepseek-logo.jpeg")));
        logo.setFitHeight(30);
        logo.setPreserveRatio(true);

        Label title = new Label("DeepSeek Chat");
        title.getStyleClass().add("title");

        HBox.setMargin(title, new Insets(0, 0, 0, 10));
        header.getChildren().addAll(logo, title);

        return header;
    }

    private ScrollPane createChatArea() {
        chatContainer = new VBox();
        chatContainer.getStyleClass().add("chat-container");
        chatContainer.setPadding(new Insets(20));
        chatContainer.setSpacing(15);

        // 添加欢迎消息
        addWelcomeMessage();

        scrollPane = new ScrollPane(chatContainer);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // 绑定滚动条到底部
        chatContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });

        return scrollPane;
    }

    private void addWelcomeMessage() {
        HBox messageBox = new HBox();
        messageBox.getStyleClass().add("ai-message-container");
        messageBox.setAlignment(Pos.TOP_LEFT);

        // AI头像
        Circle aiAvatar = new Circle(20);
        aiAvatar.getStyleClass().add("ai-avatar");

        VBox messageContent = new VBox();
        messageContent.setSpacing(5);

        Label nameLabel = new Label("DeepSeek");
        nameLabel.getStyleClass().add("message-name");

        TextFlow textFlow = new TextFlow();
        textFlow.getStyleClass().add("ai-message");

        Text messageText = new Text("您好！我是DeepSeek AI助手，有什么可以帮您的吗？");
        textFlow.getChildren().add(messageText);

        messageContent.getChildren().addAll(nameLabel, textFlow);
        HBox.setMargin(messageContent, new Insets(0, 0, 0, 10));

        messageBox.getChildren().addAll(aiAvatar, messageContent);

        chatContainer.getChildren().add(messageBox);

        // 添加淡入动画
        FadeTransition ft = new FadeTransition(Duration.millis(500), messageBox);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private HBox createInputArea() {
        HBox inputArea = new HBox();
        inputArea.getStyleClass().add("input-area");
        inputArea.setPadding(new Insets(15));
        inputArea.setSpacing(10);
        inputArea.setAlignment(Pos.CENTER);

        inputField = new TextField();
        inputField.getStyleClass().add("input-field");
        inputField.setPromptText("输入您的问题...");
        inputField.setPrefHeight(40);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        sendButton = new Button();
        sendButton.getStyleClass().add("send-button");
        sendButton.setPrefSize(40, 40);

        // 发送按钮图标
        ImageView sendIcon = new ImageView(new Image(getClass().getResourceAsStream("/Image/send-icon.jpg")));
        sendIcon.setFitHeight(20);
        sendIcon.setPreserveRatio(true);
        sendButton.setGraphic(sendIcon);

        // 发送消息事件处理
        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        inputArea.getChildren().addAll(inputField, sendButton);
        return inputArea;
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        // 添加用户消息到聊天界面
        addUserMessage(message);

        // 清空输入框
        inputField.clear();

        // 显示AI正在思考的指示器
        showTypingIndicator();

        // 调用API获取响应
        callDeepSeekAPI(message);
    }

    private void addUserMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.getStyleClass().add("user-message-container");
        messageBox.setAlignment(Pos.TOP_RIGHT);

        VBox messageContent = new VBox();
        messageContent.setSpacing(5);
        messageContent.setAlignment(Pos.TOP_RIGHT);

        Label nameLabel = new Label("用户姓名");
        nameLabel.getStyleClass().add("message-name");

        TextFlow textFlow = new TextFlow();
        textFlow.getStyleClass().add("user-message");

        Text messageText = new Text(message);
        textFlow.getChildren().add(messageText);

        messageContent.getChildren().addAll(nameLabel, textFlow);
        HBox.setMargin(messageContent, new Insets(0, 10, 0, 0));

        messageBox.getChildren().add(messageContent);

        // 添加到聊天容器
        chatContainer.getChildren().add(messageBox);

        // 添加淡入动画
        FadeTransition ft = new FadeTransition(Duration.millis(300), messageBox);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // 添加到对话历史
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        conversationHistory.add(userMessage);
    }

    private void addAiMessage(String message) {
        // 移除正在输入指示器
        removeTypingIndicator();

        HBox messageBox = new HBox();
        messageBox.getStyleClass().add("ai-message-container");
        messageBox.setAlignment(Pos.TOP_LEFT);

        // AI头像
        Circle aiAvatar = new Circle(20);
        aiAvatar.getStyleClass().add("ai-avatar");

        VBox messageContent = new VBox();
        messageContent.setSpacing(5);

        Label nameLabel = new Label("DeepSeek");
        nameLabel.getStyleClass().add("message-name");

        TextFlow textFlow = new TextFlow();
        textFlow.getStyleClass().add("ai-message");

        // 处理消息中的换行和格式
        String formattedMessage = message.replace("\n", "\n");
        Text messageText = new Text(formattedMessage);
        textFlow.getChildren().add(messageText);

        messageContent.getChildren().addAll(nameLabel, textFlow);
        HBox.setMargin(messageContent, new Insets(0, 0, 0, 10));

        messageBox.getChildren().addAll(aiAvatar, messageContent);

        // 添加到聊天容器
        chatContainer.getChildren().add(messageBox);

        // 添加淡入动画
        FadeTransition ft = new FadeTransition(Duration.millis(500), messageBox);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // 添加到对话历史
        JsonObject aiMessage = new JsonObject();
        aiMessage.addProperty("role", "assistant");
        aiMessage.addProperty("content", message);
        conversationHistory.add(aiMessage);
    }

    private void showTypingIndicator() {
        HBox typingIndicator = new HBox();
        typingIndicator.getStyleClass().add("typing-indicator");
        typingIndicator.setAlignment(Pos.TOP_LEFT);
        typingIndicator.setId("typing-indicator"); // 设置ID以便后续移除

        Circle aiAvatar = new Circle(20);
        aiAvatar.getStyleClass().add("ai-avatar");

        VBox indicatorContent = new VBox();
        indicatorContent.setSpacing(5);

        Label nameLabel = new Label("DeepSeek");
        nameLabel.getStyleClass().add("message-name");

        HBox dots = new HBox(5);
        dots.getStyleClass().add("typing-dots");
        dots.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.getStyleClass().add("typing-dot");
            dots.getChildren().add(dot);
        }

        indicatorContent.getChildren().addAll(nameLabel, dots);
        HBox.setMargin(indicatorContent, new Insets(0, 0, 0, 10));

        typingIndicator.getChildren().addAll(aiAvatar, indicatorContent);

        chatContainer.getChildren().add(typingIndicator);
    }

    private void removeTypingIndicator() {
        // 查找并移除正在输入指示器
        chatContainer.getChildren().removeIf(node ->
                node.getId() != null && node.getId().equals("typing-indicator"));
    }

    private void callDeepSeekAPI(String userMessage) {
        Task<String> apiTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    // 使用Gson构建请求JSON
                    JsonObject requestBody = new JsonObject();
                    requestBody.addProperty("model", "deepseek-chat"); // 根据实际情况调整模型名称

                    // 添加对话历史
                    JsonArray messages = new JsonArray();
                    for (JsonObject msg : conversationHistory) {
                        messages.add(msg);
                    }
                    requestBody.add("messages", messages);
                    requestBody.addProperty("temperature", 0.7);
                    requestBody.addProperty("max_tokens", 2000);

                    // 创建HTTP客户端和请求
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(apiUrl))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                            .build();

                    // 发送请求并获取响应
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        // 使用Gson解析响应
                        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                        JsonArray choices = jsonResponse.getAsJsonArray("choices");
                        JsonObject firstChoice = choices.get(0).getAsJsonObject();
                        JsonObject message = firstChoice.getAsJsonObject("message");
                        return message.get("content").getAsString();
                    } else {
                        return "抱歉，发生错误: " + response.statusCode() + " - " + response.body();
                    }
                } catch (IOException | InterruptedException e) {
                    return "抱歉，请求过程中出现错误: " + e.getMessage();
                }
            }
        };

        // 设置任务完成后的处理
        apiTask.setOnSucceeded(e -> {
            String response = apiTask.getValue();
            addAiMessage(response);
        });

        apiTask.setOnFailed(e -> {
            addAiMessage("抱歉，发生了错误: " + apiTask.getException().getMessage());
        });

        // 在新线程中执行API调用
        Thread apiThread = new Thread(apiTask);
        apiThread.setDaemon(true);
        apiThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
