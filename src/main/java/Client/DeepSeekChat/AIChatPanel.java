package Client.DeepSeekChat;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import Client.util.Config; // 新增：读取 application.properties

/**
 * 嵌入式 AI 助手聊天面板（替代独立 Application 的 DeepSeekChat）。
 * 直接作为主界面 center 内容使用，不再创建 Stage。\n * 说明：API Key 与 URL 通过配置 / 环境变量加载。
 */
public class AIChatPanel extends BorderPane {
    private final VBox chatContainer = new VBox();
    private final TextField inputField = new TextField();
    private final Button sendButton = new Button();
    private final ScrollPane scrollPane = new ScrollPane();
    private final List<JsonObject> conversationHistory = new ArrayList<>();

    // 可外部注入（后续可改为从配置文件或输入框设置）
    private String apiKey = "your_api_key"; // 默认占位，启动时会被覆盖
    private String apiUrl = "https://api.deepseek.com/v1/chat/completions";

    private final Gson gson = new GsonBuilder().create();
    private final String userDisplayName; // 用于显示在用户消息头

    public AIChatPanel(String userDisplayName){
        this.userDisplayName = userDisplayName == null ? "用户" : userDisplayName;
        // 启动前尝试加载外部配置：优先环境变量 > application.properties > 默认占位
        String envKey = System.getenv("DEEPSEEK_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            this.apiKey = envKey.trim();
        } else {
            String propKey = Config.get("deepseek.api.key");
            if (propKey != null && !propKey.isBlank()) this.apiKey = propKey.trim();
        }
        buildUI();
    }

    private void buildUI(){
        getStyleClass().add("ai-root");

        HBox header = createHeader();
        setTop(header);

        createChatArea();
        setCenter(scrollPane);

        HBox inputArea = createInputArea();
        setBottom(inputArea);

        // 加载样式
        try {
            String cssPath = getClass().getResource("/Css/DeepSeekChat.css").toExternalForm();
            getStylesheets().add(cssPath);
        } catch (Exception ignore) {}

        Platform.runLater(() -> inputField.requestFocus());
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);

        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/Image/deepseek-logo.jpeg")));
            logo.setFitHeight(30);
            logo.setPreserveRatio(true);
            Label title = new Label("AI助手");
            title.getStyleClass().add("title");
            HBox.setMargin(title, new Insets(0,0,0,10));
            header.getChildren().addAll(logo, title);
        } catch (Exception e){
            Label title = new Label("AI助手");
            title.getStyleClass().add("title");
            header.getChildren().add(title);
        }
        return header;
    }

    private void createChatArea(){
        chatContainer.getStyleClass().add("chat-container");
        chatContainer.setPadding(new Insets(20));
        chatContainer.setSpacing(15);
        addWelcomeMessage();

        scrollPane.setContent(chatContainer);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        chatContainer.heightProperty().addListener((o,ov,nv)-> scrollPane.setVvalue(1.0));
    }

    private void addWelcomeMessage(){
        HBox messageBox = new HBox();
        messageBox.getStyleClass().add("ai-message-container");
        messageBox.setAlignment(Pos.TOP_LEFT);
        Circle aiAvatar = new Circle(20);
        aiAvatar.getStyleClass().add("ai-avatar");
        VBox messageContent = new VBox();
        messageContent.setSpacing(5);
        Label nameLabel = new Label("DeepSeek");
        nameLabel.getStyleClass().add("message-name");
        TextFlow textFlow = new TextFlow();
        textFlow.getStyleClass().add("ai-message");
        Text t = new Text("您好！我是AI助手，有什么可以帮您的吗？");
        textFlow.getChildren().add(t);
        messageContent.getChildren().addAll(nameLabel, textFlow);
        HBox.setMargin(messageContent, new Insets(0,0,0,10));
        messageBox.getChildren().addAll(aiAvatar, messageContent);
        chatContainer.getChildren().add(messageBox);
        FadeTransition ft = new FadeTransition(Duration.millis(500), messageBox);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private HBox createInputArea(){
        HBox area = new HBox();
        area.getStyleClass().add("input-area");
        area.setPadding(new Insets(15));
        area.setSpacing(10); area.setAlignment(Pos.CENTER);

        inputField.getStyleClass().add("input-field");
        inputField.setPromptText("输入您的问题...");
        inputField.setPrefHeight(40);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        sendButton.getStyleClass().add("send-button");
        sendButton.setPrefSize(40,40);
        try {
            ImageView sendIcon = new ImageView(new Image(getClass().getResourceAsStream("/Image/send-icon.jpg")));
            sendIcon.setFitHeight(20); sendIcon.setPreserveRatio(true);
            sendButton.setGraphic(sendIcon);
        } catch (Exception ignore) {}

        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnKeyPressed(e -> { if (e.getCode()== KeyCode.ENTER) sendMessage(); });
        area.getChildren().addAll(inputField, sendButton);
        return area;
    }

    private void sendMessage(){
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        addUserMessage(msg);
        inputField.clear();
        showTypingIndicator();
        callDeepSeekAPI(msg);
    }

    private void addUserMessage(String message){
        HBox box = new HBox();
        box.getStyleClass().add("user-message-container");
        box.setAlignment(Pos.TOP_RIGHT);
        VBox content = new VBox();
        content.setSpacing(5); content.setAlignment(Pos.TOP_RIGHT);
        Label name = new Label(userDisplayName);
        name.getStyleClass().add("message-name");
        TextFlow tf = new TextFlow(); tf.getStyleClass().add("user-message");
        tf.getChildren().add(new Text(message));
        content.getChildren().addAll(name, tf);
        HBox.setMargin(content, new Insets(0,10,0,0));
        box.getChildren().add(content);
        chatContainer.getChildren().add(box);
        FadeTransition ft = new FadeTransition(Duration.millis(300), box); ft.setFromValue(0); ft.setToValue(1); ft.play();
        JsonObject obj = new JsonObject(); obj.addProperty("role","user"); obj.addProperty("content", message); conversationHistory.add(obj);
    }

    private void addAiMessage(String message){
        removeTypingIndicator();
        HBox box = new HBox(); box.getStyleClass().add("ai-message-container"); box.setAlignment(Pos.TOP_LEFT);
        Circle aiAvatar = new Circle(20); aiAvatar.getStyleClass().add("ai-avatar");
        VBox content = new VBox(); content.setSpacing(5);
        Label name = new Label("DeepSeek"); name.getStyleClass().add("message-name");
        TextFlow tf = new TextFlow(); tf.getStyleClass().add("ai-message");
        tf.getChildren().add(new Text(message));
        content.getChildren().addAll(name, tf); HBox.setMargin(content, new Insets(0,0,0,10));
        box.getChildren().addAll(aiAvatar, content); chatContainer.getChildren().add(box);
        FadeTransition ft = new FadeTransition(Duration.millis(500), box); ft.setFromValue(0); ft.setToValue(1); ft.play();
        JsonObject obj = new JsonObject(); obj.addProperty("role","assistant"); obj.addProperty("content", message); conversationHistory.add(obj);
    }

    private void showTypingIndicator(){
        HBox typing = new HBox(); typing.getStyleClass().add("typing-indicator"); typing.setAlignment(Pos.TOP_LEFT); typing.setId("typing-indicator");
        Circle aiAvatar = new Circle(20); aiAvatar.getStyleClass().add("ai-avatar");
        VBox indicatorContent = new VBox(); indicatorContent.setSpacing(5);
        Label name = new Label("DeepSeek"); name.getStyleClass().add("message-name");
        HBox dots = new HBox(5); dots.getStyleClass().add("typing-dots"); dots.setAlignment(Pos.CENTER_LEFT);
        for(int i=0;i<3;i++){ Circle d = new Circle(4); d.getStyleClass().add("typing-dot"); dots.getChildren().add(d);}
        indicatorContent.getChildren().addAll(name,dots); HBox.setMargin(indicatorContent,new Insets(0,0,0,10));
        typing.getChildren().addAll(aiAvatar, indicatorContent); chatContainer.getChildren().add(typing);
    }

    private void removeTypingIndicator(){
        chatContainer.getChildren().removeIf(n -> "typing-indicator".equals(n.getId()));
    }

    private void callDeepSeekAPI(String userMessage){
        Task<String> task = new Task<>(){
            @Override protected String call(){
                if (apiKey == null || apiKey.isBlank() || apiKey.equals("your_api_key")) {
                    return "未配置有效的 API Key，请设置环境变量 DEEPSEEK_API_KEY 或在 application.properties 中添加 deepseek.api.key";
                }
                try {
                    JsonObject req = new JsonObject();
                    req.addProperty("model", "deepseek-chat");
                    JsonArray messages = new JsonArray();
                    for (JsonObject m : conversationHistory) messages.add(m);
                    req.add("messages", messages);
                    req.addProperty("temperature", 0.7);
                    req.addProperty("max_tokens", 2000);
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(apiUrl))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(req)))
                            .build();
                    HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode()==200){
                        JsonObject jr = gson.fromJson(resp.body(), JsonObject.class);
                        JsonArray choices = jr.getAsJsonArray("choices");
                        if (choices!=null && choices.size()>0){
                            JsonObject first = choices.get(0).getAsJsonObject();
                            JsonObject msg = first.getAsJsonObject("message");
                            if (msg!=null && msg.has("content")) return msg.get("content").getAsString();
                        }
                        return "(无内容返回)";
                    } else {
                        return "请求失败: " + resp.statusCode();
                    }
                } catch (IOException | InterruptedException ex){
                    return "调用出错: " + ex.getMessage();
                } catch (Exception ex){
                    return "解析异常: " + ex.getMessage();
                }
            }
        };
        task.setOnSucceeded(e -> addAiMessage(task.getValue()));
        task.setOnFailed(e -> addAiMessage("任务失败: " + task.getException().getMessage()));
        Thread th = new Thread(task, "ai-chat-api"); th.setDaemon(true); th.start();
    }

    // 提供外部设置 API Key 的方法（可后续添加到 UI）
    public void setApiKey(String apiKey){ this.apiKey = apiKey; }
    public void setApiUrl(String apiUrl){ this.apiUrl = apiUrl; }
}

