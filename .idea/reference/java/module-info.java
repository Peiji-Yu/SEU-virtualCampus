module com.example.logindemo {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.logindemo to javafx.fxml;
    exports com.example.logindemo;
}