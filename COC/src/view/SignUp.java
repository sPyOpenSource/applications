package view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import model.Map.*;
import model.Player;
import model.IdPlayerException;
import model.data.SaveData;

import cr0s.javara.render.map.Map;
import java.util.ArrayList;
import controller.SignUpController;
import assets.Assets;

public class SignUp extends Application {
    public SignUp(ArrayList<Player> players) {
        this.controller = new SignUpController(players);
        this.players = players;
    }

    private final SignUpController controller;
    private final ArrayList<Player> players;
    private final static DropShadow shadow = new DropShadow(20, Color.web("#000000"));
    private final static ImageView imageView1 = new ImageView(new Image("assets/png/map1.png"));
    private final static ImageView imageView2 = new ImageView(new Image("assets/png/map2.png"));
    private final static ImageView imageView3 = new ImageView(new Image("assets/png/map3.png"));
    private final static ImageView imageView4 = new ImageView(new Image("assets/png/map4.png"));
    private final static Assets assets = new Assets();
    private final String audioFilePath = assets.get("/assets/audio/click_button.mp3");
    private final MediaPlayer mediaPlayerClick = new MediaPlayer(new Media(audioFilePath));
    
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setCenter(vBoxFields(stage));
        Image image = new Image("assets/jpg/poster.jpg");
        root.setBackground(new Background(new BackgroundImage(image,BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT)));
        Scene scene = new Scene(root, 1350, 666);
        scene.getStylesheets().add("style.css");
        stage.setScene(scene);
        stage.show();
        root.getChildren().add(new MediaView(mediaPlayerClick));
    }
    
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
        Button buttonSignUp = new Button("SignUp");
        buttonSignUp.setEffect(shadow);
        HBox hBoxMap = hBoxMaps();
        String audioFilePath = assets.get("/assets/audio/home.mp3");
        MediaPlayer mediaPlayer = new MediaPlayer(new Media(audioFilePath));
        mediaPlayer.setAutoPlay(true);
        vBox.getChildren().add(new MediaView(mediaPlayer));

        buttonSignUp.setOnMouseClicked(mouseEvent -> {
            try {
                Map map;
                if (textFieldID.getText().equals("") || textFieldPassword.getText().equals(""))
                    throw new RuntimeException("Fields cannot be empty!");

                controller.checkId(textFieldID.getText());
                if (imageView1.getEffect() != null){
                    map = new MapLoader(1, "map1");
                } else if (imageView2.getEffect() != null){
                    map = new MapLoader(2, "map2");
                } else if (imageView3.getEffect() != null){
                    map = new MapLoader(3, "map3");
                } else {
                    map = new MapLoader(4, "map4");
                }
                mediaPlayerClick.play();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Player player = new Player(textFieldID.getText(), textFieldPassword.getText(), map);
                new SaveData(player).start();
                new PlayerPanel(player,players).start(new Stage());
                stage.close();
                mediaPlayer.stop();
            } catch (IdPlayerException | RuntimeException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        });

        Button buttonLogin = new Button("Log In");
        buttonLogin.setEffect(shadow);
        buttonLogin.setOnMouseClicked(mouseEvent -> {
            mediaPlayerClick.play();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new LogIn(players).start(new Stage());
            stage.close();
            mediaPlayer.stop();
        });

        HBox hBoxButton = new HBox(buttonLogin, buttonSignUp);
        hBoxButton.setSpacing(10);
        hBoxButton.setAlignment(Pos.CENTER);

        Text text = new Text("Sign Up");
        text.setStroke(Color.web("#FBFFDC"));
        text.setFill(Color.web("#FBFFDC"));
        text.setId("text");

        Text text1 = new Text("Select a Map");
        text1.setStroke(Color.web("#FBFFDC"));
        text1.setFill(Color.web("#FBFFDC"));
        text1.setId("text");

        vBox.getChildren().addAll(text, textFieldID, textFieldPassword, text1, hBoxMap, hBoxButton);

        return vBox;
    }
    
    private HBox hBoxMaps(){
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(20));
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(imageView1, imageView2, imageView3, imageView4);

        imageView1.setCursor(Cursor.HAND);
        imageView2.setCursor(Cursor.HAND);
        imageView3.setCursor(Cursor.HAND);
        imageView4.setCursor(Cursor.HAND);

        imageView1.setOnMouseClicked(mouseEvent -> {
            imageView1.setEffect(shadow);
            imageView2.setEffect(null);
            imageView3.setEffect(null);
            imageView4.setEffect(null);
        });
        imageView2.setOnMouseClicked(mouseEvent -> {
            imageView2.setEffect(shadow);
            imageView1.setEffect(null);
            imageView3.setEffect(null);
            imageView4.setEffect(null);
        });
        imageView3.setOnMouseClicked(mouseEvent -> {
            imageView3.setEffect(shadow);
            imageView2.setEffect(null);
            imageView1.setEffect(null);
            imageView4.setEffect(null);
        });
        imageView4.setOnMouseClicked(mouseEvent -> {
            imageView4.setEffect(shadow);
            imageView2.setEffect(null);
            imageView3.setEffect(null);
            imageView1.setEffect(null);
        });
        return hBox;
    }
}
