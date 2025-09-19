package Client.panel.library.student;

import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

import java.net.URL;


public class PaperSketchPanel extends BorderPane {
    private WebView webView;

    public PaperSketchPanel() {
        initializeUI();
    }

    private void initializeUI() {
        // 创建WebView组件
        webView = new WebView();

        // 加载HTML文件
        URL url = getClass().getResource("/paperSketch.html");
        if (url != null) {
            webView.getEngine().load(url.toExternalForm());
        } else {
            webView.getEngine().loadContent("<html><body><h1>Error: paperSketch.html not found</h1></body></html>");
        }

        // 设置WebView属性
        webView.setPrefSize(1500, 1000);

        // 设置主布局
        setCenter(webView);
    }
}
