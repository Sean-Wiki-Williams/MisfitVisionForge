module com.visonforge.visionforge {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires jdk.compiler;
    requires java.desktop;
    requires json.simple;
    requires javafx.swing;

    opens com.visonforge.visionforge to javafx.fxml;
    exports com.visonforge.visionforge;
}