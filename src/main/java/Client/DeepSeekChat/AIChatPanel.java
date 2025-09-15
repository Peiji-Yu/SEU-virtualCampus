package Client.DeepSeekChat;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import Server.model.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import Client.ClientNetworkHelper;
import Client.util.Config;
import Server.model.Request;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * 嵌入式 AI 助手聊天面板（虚拟校园系统专用）
 */
public class AIChatPanel extends BorderPane {
    private final VBox chatContainer = new VBox();
    private final TextField inputField = new TextField();
    private final Button sendButton = new Button();
    private final ScrollPane scrollPane = new ScrollPane();
    private final List<JsonObject> conversationHistory = new ArrayList<>();

    private String apiKey = "your_api_key"; // 默认占位
    private String apiUrl = "https://api.deepseek.com/v1/chat/completions";

    private final Gson gson = new GsonBuilder().create();

    private Image aiAvatarImg;
    private Image userAvatarImg;

    private final String userDisplayName; // 默认显示名

    public AIChatPanel(String userDisplayName){
        this.userDisplayName = userDisplayName == null ? "未知用户" : userDisplayName;
        // 加载头像
        try { aiAvatarImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/deepseek/deepseek.png"))); } catch (Exception ignore) {}
        try { userAvatarImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/deepseek/用户.png"))); } catch (Exception ignore) {}

        // 加载 API Key
        String envKey = System.getenv("DEEPSEEK_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            this.apiKey = envKey.trim();
        } else {
            String propKey = Config.get("deepseek.api.key");
            if (propKey != null && !propKey.isBlank()) this.apiKey = propKey.trim();
        }

        // 初始化系统消息（无需学生信息也可运行）
        initSystemContext();

        buildUI();
    }

    /** 初始化系统上下文 */
    private void initSystemContext() {
        String systemPrompt =
                "你是东南大学虚拟校园系统的智能助手。\n" +
                        "该系统模块包括：\n" +
                        "1. 用户管理模块：用户登录、登出、修改/忘记密码。\n" +
                        "2. 学籍管理模块：学生可查看姓名、性别、出生日期、身份证号、一卡通号、学号、学院、专业、学籍状态、入学时间、籍贯、政治面貌；管理员可操作学籍信息。\n" +
                        "3. 选课系统模块：学生查询课表、选退课；教师查询教学任务及选课学生名单。\n" +
                        "4. 图书管理模块：学生查询可借书籍以及借阅信息，管理员管理图书及读者信息。\n" +
                        "5. 商店模块：商品浏览、搜索、购物车、订单管理，后台管理仅商店管理员可操作。\n" +
                        "\n" +
                        "请根据提供信息回答问题，如果没有对应信息优先引导学生完成校园系统操作，而不是直接给答案。回答风格活泼友好。";

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);

        conversationHistory.add(systemMsg);
    }

    /**
     * 动态拼接 5 个模块的上下文信息
     */
    private String fetchDynamicContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是学生最新的系统信息，请结合回答：\n\n");
        Integer cardNumber = Integer.valueOf(userDisplayName);
        Gson gson = new Gson();

        try {
            // 2. 学籍管理模块
            String studentInfoJson = ClientNetworkHelper.send(
                    new Request("getSelf", Map.of("cardNumber", cardNumber))
            );
            Response studentInfoResp = gson.fromJson(studentInfoJson, Response.class);
            sb.append("【2. 学籍管理模块】\n")
                    .append(studentInfoResp.isSuccess() ? studentInfoResp.getData() : studentInfoResp.getMessage())
                    .append("\n\n");

            // 3. 选课系统模块
            String courseInfoJson = ClientNetworkHelper.send(
                    new Request("getStudentSelectedCourses", Map.of("cardNumber", cardNumber))
            );
            Response courseInfoResp = gson.fromJson(courseInfoJson, Response.class);
            sb.append("【3. 选课系统模块】\n")
                    .append(courseInfoResp.isSuccess() ? courseInfoResp.getData() : courseInfoResp.getMessage())
                    .append("\n\n");

            // 4. 图书管理模块
            String libraryInfoJson = ClientNetworkHelper.send(
                    new Request("getOwnRecords", Map.of("userId", cardNumber))
            );
            Response libraryInfoResp = gson.fromJson(libraryInfoJson, Response.class);
            sb.append("【4. 图书管理模块】\n")
                    .append(libraryInfoResp.isSuccess() ? libraryInfoResp.getData() : libraryInfoResp.getMessage())
                    .append("\n\n");

            // 5. 商店模块
            String shopOrdersJson = ClientNetworkHelper.send(
                    new Request("getUserOrders", Map.of("cardNumber", cardNumber))
            );
            Response shopOrdersResponse = gson.fromJson(shopOrdersJson, Response.class);

            if (!shopOrdersResponse.isSuccess()) {
                sb.append("【5. 商店模块】请求失败：").append(shopOrdersResponse.getMessage()).append("\n");
            } else {
                String dataJson = gson.toJson(shopOrdersResponse.getData());
                Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {}.getType();
                List<Map<String, Object>> orders = gson.fromJson(dataJson, listType);

                StringBuilder shopSb = new StringBuilder();
                shopSb.append("【5. 商店模块】\n");

                for (Map<String, Object> orderMap : orders) {
                    String uuidStr = (String) orderMap.get("uuid");
                    shopSb.append("订单 UUID: ").append(uuidStr).append("\n");

                    String orderDetailJson = ClientNetworkHelper.send(
                            new Request("getOrder", Map.of("orderId", uuidStr))
                    );
                    Response orderDetailResponse = gson.fromJson(orderDetailJson, Response.class);
                    if (orderDetailResponse.isSuccess()) {
                        shopSb.append("订单详情: ").append(orderDetailResponse.getData()).append("\n\n");
                    } else {
                        shopSb.append("订单详情获取失败: ").append(orderDetailResponse.getMessage()).append("\n\n");
                    }
                }

                sb.append(shopSb.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            sb.append("（部分模块信息获取失败，请稍后重试）");
        }

        return sb.toString();
    }

    /** 构建界面 */
    private void buildUI(){
        getStyleClass().add("ai-root");

        HBox header = createHeader();
        setTop(header);

        createChatArea(); // 内部已 setCenter(wrapper)

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
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/Image/deepseek/deepseek-logo.jpeg")));
            logo.setFitHeight(30);
            logo.setPreserveRatio(true);
            Label title = new Label("东南大学虚拟校园系统智能助手");
            title.getStyleClass().add("title");
            HBox.setMargin(title, new Insets(0,0,0,10));
            header.getChildren().addAll(logo, title);
        } catch (Exception e){
            Label title = new Label("东南大学虚拟校园系统智能助手");
            title.getStyleClass().add("title");
            header.getChildren().add(title);
        }
        return header;
    }

    private void createChatArea(){
        chatContainer.getStyleClass().add("chat-container");
        chatContainer.setPadding(new Insets(20));
        chatContainer.setSpacing(15);
        chatContainer.setFillWidth(true);
        addWelcomeMessage();

        scrollPane.setContent(chatContainer);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        StackPane wrapper = new StackPane(scrollPane);
        wrapper.setStyle("-fx-background-color: white;");
        StackPane.setMargin(scrollPane, Insets.EMPTY);
        wrapper.heightProperty().addListener((o,ov,nv)->{
            if (chatContainer.getHeight() < nv.doubleValue()) {
                chatContainer.setMinHeight(nv.doubleValue());
            }
        });
        setCenter(wrapper);

        chatContainer.heightProperty().addListener((o,ov,nv)-> scrollPane.setVvalue(1.0));
    }

    private void addWelcomeMessage(){
        HBox messageBox = new HBox();
        messageBox.getStyleClass().add("ai-message-container");
        messageBox.setAlignment(Pos.TOP_LEFT);
        Node aiAvatarNode = buildAiAvatarNode();
        VBox messageContent = new VBox();
        messageContent.setSpacing(5);
        Label nameLabel = new Label("东大虚拟校园系统智能助手");
        nameLabel.getStyleClass().add("message-name");
        TextFlow textFlow = new TextFlow();
        textFlow.getStyleClass().add("ai-message");
        Text t = new Text("您好！我是东南大学虚拟校园系统的智能助手，有什么可以帮您的吗？");
        textFlow.getChildren().add(t);
        messageContent.getChildren().addAll(nameLabel, textFlow);
        HBox.setMargin(messageContent, new Insets(0,0,0,10));
        messageBox.getChildren().addAll(aiAvatarNode, messageContent);
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
        sendButton.setAlignment(Pos.CENTER);
        try {
            ImageView sendIcon = new ImageView(new Image(getClass().getResourceAsStream("/Image/deepseek/send-icon.png")));
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

        // 假设你在类里保存了 userId 和 cardNumber
        String dynamicContext = fetchDynamicContext();

        // 删除旧的动态上下文
        conversationHistory.removeIf(ctx ->
                "system".equals(ctx.get("role").getAsString()) &&
                        ctx.get("content").getAsString().startsWith("以下是学生最新的系统信息")
        );

        // 添加新的动态上下文
        JsonObject contextMsg = new JsonObject();
        contextMsg.addProperty("role", "system");
        contextMsg.addProperty("content", dynamicContext);
        conversationHistory.add(contextMsg);

        // 添加用户消息
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

        TextFlow tf = new TextFlow();
        tf.getStyleClass().add("user-message");
        tf.getChildren().add(new Text(message));

        content.getChildren().addAll(name, tf);

        Node userAvatarNode = buildUserAvatarNode();
        HBox.setMargin(content, new Insets(0,10,0,0));
        box.getChildren().addAll(content, userAvatarNode);
        chatContainer.getChildren().add(box);

        FadeTransition ft = new FadeTransition(Duration.millis(300), box);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        JsonObject obj = new JsonObject();
        obj.addProperty("role","user");
        obj.addProperty("content", message);
        conversationHistory.add(obj);
    }

    private void addAiMessage(String message){
        removeTypingIndicator();
        HBox box = new HBox(); box.getStyleClass().add("ai-message-container"); box.setAlignment(Pos.TOP_LEFT);
        Node aiAvatarNode = buildAiAvatarNode();
        VBox content = new VBox(); content.setSpacing(5);
        Label name = new Label("东大虚拟校园系统智能助手"); name.getStyleClass().add("message-name");
        TextFlow tf = new TextFlow(); tf.getStyleClass().add("ai-message");
        tf.getChildren().add(new Text(message));
        content.getChildren().addAll(name, tf); HBox.setMargin(content, new Insets(0,0,0,10));
        box.getChildren().addAll(aiAvatarNode, content); chatContainer.getChildren().add(box);
        FadeTransition ft = new FadeTransition(Duration.millis(500), box); ft.setFromValue(0); ft.setToValue(1); ft.play();
        JsonObject obj = new JsonObject(); obj.addProperty("role","assistant"); obj.addProperty("content", message); conversationHistory.add(obj);
    }

//    private void showTypingIndicator(){
//        HBox box = new HBox();
//        box.getStyleClass().add("ai-message-container");
//        box.setAlignment(Pos.TOP_LEFT);
//        box.setId("typing-indicator");
//
//        Node aiAvatarNode = buildAiAvatarNode();
//        VBox content = new VBox();
//        content.setSpacing(5);
//
//        Label name = new Label("东大虚拟校园系统智能助手");
//        name.getStyleClass().add("message-name");
//
//        HBox dots = new HBox(5);
//        dots.getStyleClass().add("typing-dots");
//        dots.setAlignment(Pos.CENTER_LEFT);
//        dots.setMaxWidth(30);
//
//        for (int i = 0; i < 3; i++) {
//            Circle d = new Circle(4);
//            d.getStyleClass().add("typing-dot");
//            dots.getChildren().add(d);
//        }
//
//        TextFlow tf = new TextFlow(dots);
//        tf.getStyleClass().add("ai-message");
//
//        content.getChildren().addAll(name, tf);
//        HBox.setMargin(content, new Insets(0,0,0,10));
//        box.getChildren().addAll(aiAvatarNode, content);
//        chatContainer.getChildren().add(box);
//    }

    private void showTypingIndicator(){
        HBox typing = new HBox();
        typing.getStyleClass().add("ai-message-container");
        typing.getStyleClass().add("typing-indicator");
        typing.setAlignment(Pos.TOP_LEFT);
        typing.setId("typing-indicator");

        Node aiAvatarNode = buildAiAvatarNode();
        VBox indicatorContent = new VBox();
        indicatorContent.setSpacing(5);

        Label name = new Label("东大虚拟校园系统智能助手");
        name.getStyleClass().add("message-name");

        HBox dots = new HBox(5);
        dots.getStyleClass().add("typing-dots");
        dots.setAlignment(Pos.CENTER_LEFT);
        dots.setMaxWidth(30);

        for (int i = 0; i < 3; i++) {
            Circle d = new Circle(4);
            d.getStyleClass().add("typing-dot");
            dots.getChildren().add(d);
        }

        indicatorContent.getChildren().addAll(name,dots);
        HBox.setMargin(indicatorContent,new Insets(0,0,0,10));
        typing.getChildren().addAll(aiAvatarNode, indicatorContent);
        chatContainer.getChildren().add(typing);
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

    private Node buildAiAvatarNode(){
        if (aiAvatarImg != null) {
            ImageView iv = new ImageView(aiAvatarImg);
            iv.setFitWidth(40); iv.setFitHeight(40); iv.setPreserveRatio(true);
            iv.setSmooth(true); iv.setCache(true);
            return iv;
        }
        Circle fallback = new Circle(20); fallback.getStyleClass().add("ai-avatar"); return fallback;
    }

    private Node buildUserAvatarNode(){
        if (userAvatarImg != null) {
            ImageView iv = new ImageView(userAvatarImg);
            iv.setFitWidth(40); iv.setFitHeight(40); iv.setPreserveRatio(true);
            iv.setSmooth(true); iv.setCache(true);
            return iv;
        }
        Circle fallback = new Circle(20); fallback.getStyleClass().add("user-avatar"); return fallback;
    }
}
