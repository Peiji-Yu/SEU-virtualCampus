package Client.library.paperSketch;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;

public class paperSketch extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 创建WebView组件
        WebView webView = new WebView();

        // 加载HTML文件
        URL url = getClass().getResource("/paperSketch.html");
        webView.getEngine().load(url.toExternalForm());

        // 设置WebView属性
        webView.setPrefSize(1500, 1000);

        // 创建主布局
        BorderPane root = new BorderPane();
        root.setCenter(webView);

        // 创建场景
        Scene scene = new Scene(root, 1000, 700);

        // 设置舞台
        primaryStage.setTitle("学术文献统一搜索平台");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
