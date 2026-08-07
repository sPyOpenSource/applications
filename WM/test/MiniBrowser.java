package jx.start;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class MiniBrowser extends Application {
    @Override
    public void start(Stage stage) {
        WebView view = new WebView();
        WebEngine engine = view.getEngine();

        TextField addressBar = new TextField("https://google.nl");
        addressBar.setOnAction(e -> engine.load(addressBar.getText()));

        // Update de adresbalk als er op een link wordt geklikt
        engine.locationProperty().addListener((obs, oldV, newV) -> addressBar.setText(newV));

        BorderPane root = new BorderPane();
        root.setTop(addressBar);
        root.setCenter(view);

        stage.setScene(new Scene(root, 1024, 768));
        stage.setTitle("Java Webbrowser");
        stage.show();

        engine.load(addressBar.getText());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
