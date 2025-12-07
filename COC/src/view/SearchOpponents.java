package view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import assets.Assets;

import model.Map.MapLoader;
import model.Player;

public class SearchOpponents extends Application {
    public SearchOpponents(Player player, ArrayList<Player> players, MediaPlayer mediaPlayer) {
        this.player = player;
        this.players = players;
        this.mediaPlayer = mediaPlayer;
    }

    private final ArrayList<Player> players;
    private final Player player;
    private Scene scene;
    private final Assets assets = new Assets();
    private final AtomicInteger count = new AtomicInteger(0);
    private final MediaPlayer mediaPlayer;
    private final String audioFilePath = assets.get("/assets/audio/click_button.mp3");
    private final MediaPlayer mediaPlayerClick = new MediaPlayer(new Media(audioFilePath));
    private Thread thread;
    
    @Override
    public void start(Stage stage) {
        players.add(player);
        players.add(new Player("", "", new MapLoader(1, "map1")));
        players.add(new Player("", "", new MapLoader(2, "map2")));
        if (players.get(count.get()).equals(player)) {
            count.addAndGet(1);
        }
        scene = new Scene(getMap(stage, players.get(count.get()).getMap().getMapView()), 1000, 737);
        scene.getStylesheets().add("style.css");
        stage.setScene(scene);
        stage.setTitle("Searching Opponents");
        stage.getIcons().add(new Image("assets/jpg/icon.jpg"));
        stage.show();
        stage.setResizable(false);
        thread = new Thread() {
            @Override
            public void run() {  
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    System.getLogger(SearchOpponents.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {  
                        new Battle(player, players.get(count.get()), players).start(new Stage());
                        Platform.runLater(mediaPlayer::stop);
                        stage.close();
                    }
                });
            }
        };
        thread.start();
    }
    
    private AnchorPane getMap(Stage stage, AnchorPane map){
        ImageView imageViewBack = new ImageView("assets/png/back_home.png");
        imageViewBack.setX(20);
        imageViewBack.setY(590);
        imageViewBack.setFitWidth(100);
        imageViewBack.setFitHeight(40);
        imageViewBack.setOnMouseClicked(mouseEvent -> {
            mediaPlayerClick.play();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayerClick.stop();
            new PlayerPanel(player, players).start(new Stage());
            Platform.runLater(mediaPlayer::stop);
            stage.close();
        });

        ImageView levelView;
        switch (players.get(count.get()).getLevel()) {
            case 1:
                levelView = new ImageView("assets/png/level_1.png");
                break;
            case 2:
                levelView = new ImageView("assets/png/level_2.png");
                break;
            case 3:
                levelView = new ImageView("assets/png/level_3.png");
                break;
            default:
                levelView = new ImageView("assets/png/level_4.png");
                break;
        }

        levelView.setX(15);
        levelView.setY(15);
        levelView.setFitWidth(70);
        levelView.setFitHeight(70);

        map.getChildren().add(levelView);

        ImageView nameView = new ImageView("assets/png/back_name.png");
        nameView.setX(84);
        nameView.setY(29);
        nameView.setFitHeight(40);

        Text textName = new Text(players.get(count.get()).getIdPlayer());
        textName.setId("text");
        textName.setX(90);
        textName.setY(55);

        map.getChildren().addAll(nameView, textName);

        ImageView imageViewNext = new ImageView("assets/png/next.png");
        imageViewNext.setX(850);
        imageViewNext.setY(580);
        imageViewNext.setFitWidth(100);
        imageViewNext.setFitHeight(50);
        imageViewNext.setOnMouseClicked(mouseEvent -> {
            mediaPlayerClick.play();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayerClick.stop();
            if (players.size() - 3 > count.get()){
                if (players.get(count.get()).equals(player)) {
                    count.addAndGet(1);
                }
                scene = new Scene(getMap(stage, players.get(count.addAndGet(1)).getMap().getMapView()), 1000, 737);
                scene.getStylesheets().add("style.css");
                stage.setScene(scene);
                thread.stop();
            } else {
                map.getChildren().remove(imageViewNext);
            }
        });

        ImageView attackView = new ImageView("assets/png/attack1.png");
        attackView.setX(730);
        attackView.setY(590);
        attackView.setFitWidth(100);
        attackView.setFitHeight(40);
        attackView.setOnMouseClicked(mouseEvent -> {
            mediaPlayerClick.play();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayerClick.stop();
            new Battle(player, players.get(count.get()), players).start(new Stage());
            Platform.runLater(mediaPlayer::stop);
            stage.close();
            thread.stop();
        });

        bottomHero(map);
        map.getChildren().addAll(imageViewBack, attackView, imageViewNext, new MediaView(mediaPlayerClick));

        return map;
    }

    private void bottomHero(AnchorPane map){
        ImageView imageViewDragon = new ImageView("assets/png/dragon_drag.png");
        imageViewDragon.setX(20);
        imageViewDragon.setY(640);
        imageViewDragon.setFitWidth(79);
        imageViewDragon.setFitHeight(84);

        ImageView imageViewPanda = new ImageView("assets/png/panda_drag.png");
        imageViewPanda.setX(110);
        imageViewPanda.setY(642);
        imageViewPanda.setFitWidth(80);
        imageViewPanda.setFitHeight(83);

        ImageView imageViewBalloon = new ImageView("assets/png/balloon_drag.png");
        imageViewBalloon.setX(200);
        imageViewBalloon.setY(642);
        imageViewBalloon.setFitWidth(80);
        imageViewBalloon.setFitHeight(85);

        ImageView imageViewGoblin = new ImageView("assets/png/goblin_drag.png");
        imageViewGoblin.setX(290);
        imageViewGoblin.setY(642);
        imageViewGoblin.setFitWidth(80);
        imageViewGoblin.setFitHeight(85);

        ImageView imageViewArcher = new ImageView("assets/png/archer_drag.png");
        imageViewArcher.setX(380);
        imageViewArcher.setY(642);
        imageViewArcher.setFitWidth(80);
        imageViewArcher.setFitHeight(85);

        Rectangle rectangle = new Rectangle(0, 635, 1000, 737-635);
        rectangle.setFill(Color.rgb(0,0,0,0.4));

        AtomicInteger capacityInt = new AtomicInteger(player.getLevel() * 40);

        Text capacity = new Text("Capacity : " + capacityInt);
        capacity.setId("text");
        capacity.setX(810);
        capacity.setY(50);

        ImageView imageViewCapacity = new ImageView("assets/png/capacity.png");
        imageViewCapacity.setX(800);
        imageViewCapacity.setY(20);
        imageViewCapacity.setFitWidth(150);
        imageViewCapacity.setFitHeight(50);

        map.getChildren().addAll(rectangle, imageViewCapacity, capacity);

        switch (player.getLevel()) {
            case 1:
                map.getChildren().addAll(imageViewDragon, imageViewPanda);
                break;
            case 2:
                map.getChildren().addAll(imageViewDragon,imageViewPanda, imageViewBalloon);
                break;
            case 3:
                map.getChildren().addAll(imageViewDragon, imageViewPanda, imageViewBalloon, imageViewGoblin);
                break;
            case 4:
                map.getChildren().addAll(imageViewBalloon, imageViewPanda, imageViewGoblin, imageViewArcher, imageViewDragon);
                break;
            default:
                break;
        }
    }
}
