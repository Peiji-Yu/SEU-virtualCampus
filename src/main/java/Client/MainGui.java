package Client;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainGui extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Binge");
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(MainGui.class, args);
    }
}