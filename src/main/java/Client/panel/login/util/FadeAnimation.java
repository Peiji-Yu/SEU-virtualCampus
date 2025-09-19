package Client.panel.login.util;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class FadeAnimation {
    public static void fadeIn(Duration duration, Node node, Duration delay){
        FadeTransition ft = new FadeTransition(duration,node);
        ft.setToValue(1);
        ft.setDelay(delay);
        ft.play();
        node.setDisable(false);
    }
    public static void fadeOut(Duration duration, Node node){
        FadeTransition ft = new FadeTransition(duration,node);
        ft.setToValue(0);
        ft.play();
        node.setDisable(true);
    }
}
