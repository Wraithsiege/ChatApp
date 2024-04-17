module raf.rs.chatapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens raf.rs.chatapp to javafx.fxml;
    exports raf.rs.chatapp.client;
}