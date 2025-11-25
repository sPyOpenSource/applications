package view;

import assets.Assets;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import model.Player;
import controller.LogInController;

import java.util.ArrayList;

public class LogIn extends Application {
    private final ArrayList<Player> players;
    private final LogInController controller;
    private final Assets assets = new Assets();
    private final String audioFilePath = assets.get("/assets/audio/click_button.mp3");
    private final MediaPlayer mediaPlayerClick = new MediaPlayer(new Media(audioFilePath));

    public LogIn(ArrayList<Player> players) {
        this.players = players;
        this.controller = new LogInController(players);
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundImage(new Image("assets/jpg/poster1.jpg"),BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT)));
        root.setCenter(vBoxFields(stage));
        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        scene.getStylesheets().add("style.css");
        stage.setTitle("Log in");
        stage.getIcons().add(new Image("assets/jpg/icon.jpg"));
        stage.show();
        stage.setResizable(false);
        root.getChildren().add(new MediaView(mediaPlayerClick));
    }
    
    private final static DropShadow shadow = new DropShadow(20, Color.web("#000000"));
    
    private VBox vBoxFields(Stage stage){
        VBox vBox = new VBox();
        vBox.setPadding(new Insets(20));
        vBox.setSpacing(15);
        vBox.setAlignment(Pos.CENTER);
        TextField textFieldID = new TextField();
        textFieldID.setPromptText("username");
        TextField textFieldPassword = new TextField();
        textFieldPassword.setPromptText("password");
        textFieldID.setEffect(shadow);
        textFieldPassword.setEffect(shadow);

        Button buttonLogin = new Button("Log In");
        buttonLogin.setEffect(shadow);
        buttonLogin.setOnMouseClicked(mouseEvent -> {
            mediaPlayerClick.play();
            try {
                Player player = controller.getPlayer(textFieldID.getText(), textFieldPassword.getText());
                mediaPlayerClick.play();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new PlayerPanel(player, players).start(new Stage());
                stage.close();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        });

        Button buttonSignUp = new Button("Sign Up");
        buttonSignUp.setEffect(shadow);
        buttonSignUp.setOnMouseClicked(mouseEvent -> {
            mediaPlayerClick.play();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stage.close();
            new SignUp(players).start(new Stage());
        });

        HBox hBoxButton = new HBox(buttonSignUp, buttonLogin);
        hBoxButton.setSpacing(10);
        hBoxButton.setAlignment(Pos.CENTER);

        Text text = new Text("Log In");
        text.setId("text");
        vBox.getChildren().addAll(text, textFieldID, textFieldPassword, hBoxButton);

        return vBox;
    }
}
