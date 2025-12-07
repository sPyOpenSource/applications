package view;

import controller.attack.*;
import controller.CheckResultAttack;
import controller.defense.ArcherTowerDefense;
import controller.defense.InfernoTowerDefense;
import controller.defense.TeslaDefense;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import model.Player;
import model.building.ArcherTower;
import model.building.InfernoTower;
import model.building.Tesla;

import assets.Assets;
import cr0s.javara.render.map.TileMap;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;

public class Battle extends Application {
    private final Assets assets = new Assets();
    
    public Battle(Player attackingPlayer, Player defensivePlayer, ArrayList<Player> players) {
        this.attackingPlayer = attackingPlayer;
        this.defensivePlayer = defensivePlayer;
        this.players = players;
        this.capacityInt = new AtomicInteger(attackingPlayer.getLevel() * 40);
        players.remove(defensivePlayer);
    }
    
    @Override
    public void start(Stage stage) {
        AnchorPane root = attack(stage);
        Scene scene = new Scene(root, 1000, 737);
        stage.setScene(scene);
        scene.getStylesheets().add("style.css");
        stage.show();
        stage.getIcons().add(new Image("assets/jpg/icon.jpg"));
        stage.setTitle("Attack");
        stage.setResizable(false);
        String audioFilePath = assets.get("/assets/audio/attack_audio.mp3");
        MediaPlayer mediaPlayer = new MediaPlayer(new Media(audioFilePath));
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setVolume(0.12);
        root.getChildren().add(new MediaView(mediaPlayer));
        new CheckResultAttack(stage, root, attackingPlayer, defensivePlayer, capacityInt, players, mediaPlayer).start();
        synchronized (this){
            for (Node building : defensivePlayer.getMap().getBuildingsMap()){
                if (building instanceof InfernoTower infernoTower){
                    new InfernoTowerDefense(root, defensivePlayer.getMap(), infernoTower, capacityInt).start();
                } else if (building instanceof ArcherTower archerTower){
                    new ArcherTowerDefense(root, defensivePlayer.getMap(), archerTower, capacityInt).start();
                } else if (building instanceof Tesla tesla){
                    new TeslaDefense(root, defensivePlayer.getMap(), tesla, capacityInt).start();
                }
            }
        }
    }
    
    private final static DropShadow shadow = new DropShadow(10, Color.GOLD);

    private final Player attackingPlayer;
    private final Player defensivePlayer;
    private final ArrayList<Player> players;
    private final AtomicInteger capacityInt;

    private AnchorPane attack(Stage stage){
        AnchorPane root = defensivePlayer.getMap().getMapView();

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

        Text capacity = new Text("Capacity : " + capacityInt.get());
        capacity.setId("text");
        capacity.setX(810);
        capacity.setY(50);

        ImageView imageViewCapacity = new ImageView("assets/png/capacity.png");
        imageViewCapacity.setX(800);
        imageViewCapacity.setY(20);
        imageViewCapacity.setFitWidth(150);
        imageViewCapacity.setFitHeight(50);
imageViewPanda.setEffect(shadow);
        imageViewBalloon.setOnMouseClicked(mouseEvent -> {
            imageViewBalloon.setEffect(shadow);
            imageViewArcher.setEffect(null);
            imageViewGoblin.setEffect(null);
            imageViewDragon.setEffect(null);
            imageViewPanda.setEffect(null);
        });

        imageViewArcher.setOnMouseClicked(mouseEvent -> {
            imageViewBalloon.setEffect(null);
            imageViewArcher.setEffect(shadow);
            imageViewGoblin.setEffect(null);
            imageViewDragon.setEffect(null);
            imageViewPanda.setEffect(null);
        });

        imageViewGoblin.setOnMouseClicked(mouseEvent -> {
            imageViewBalloon.setEffect(null);
            imageViewArcher.setEffect(null);
            imageViewGoblin.setEffect(shadow);
            imageViewDragon.setEffect(null);
            imageViewPanda.setEffect(null);
        });

        imageViewDragon.setOnMouseClicked(mouseEvent -> {
            imageViewBalloon.setEffect(null);
            imageViewArcher.setEffect(null);
            imageViewGoblin.setEffect(null);
            imageViewDragon.setEffect(shadow);
            imageViewPanda.setEffect(null);
        });

        imageViewPanda.setOnMouseClicked(mouseEvent -> {
            imageViewBalloon.setEffect(null);
            imageViewArcher.setEffect(null);
            imageViewGoblin.setEffect(null);
            imageViewDragon.setEffect(null);
            imageViewPanda.setEffect(shadow);
        });

//        imageViewGoblin.setOnMouseDragged(mouseEvent -> {
//            capacityInt.addAndGet(1);
//            capacity.setText("Capacity : " + capacityInt);
//            new GoblinBalloonAttack(mouseEvent.getX(), mouseEvent.getY(), map, defensivePlayer.getMap()).start();
//        });

        root.setOnMouseClicked(mouseEvent -> {
                if (imageViewBalloon.getEffect() != null && mouseEvent.getY() < 575 && capacityInt.get() >= 10){
                    capacityInt.addAndGet(-10);
                    capacity.setText("Capacity : " + capacityInt);
                    new DefBalloonAttack(mouseEvent.getX(), mouseEvent.getY(), root, defensivePlayer.getMap()).start();
                } else if (imageViewArcher.getEffect() != null  && mouseEvent.getY() < 575 && capacityInt.get() >= 30){
                    capacityInt.addAndGet(-3);
                    capacity.setText("Capacity : " + capacityInt);
                    new ArcherBalloonAttack(mouseEvent.getX(), mouseEvent.getY(), root, defensivePlayer.getMap()).start();
                } else if (imageViewGoblin.getEffect() != null && mouseEvent.getY() < 575 && capacityInt.get() >= 2){
                    capacityInt.addAndGet(-2);
                    capacity.setText("Capacity : " + capacityInt);
                    new GoblinBalloonAttack(mouseEvent.getX(), mouseEvent.getY(), root, defensivePlayer.getMap()).start();
                } else if (imageViewDragon.getEffect() != null && mouseEvent.getY() < 575 && capacityInt.get() >= 20){
                    capacityInt.addAndGet(-20);
                    capacity.setText("Capacity : " + capacityInt);
                    new DragonAttack(mouseEvent.getX(), mouseEvent.getY(), root, defensivePlayer.getMap()).start();
                } else if (imageViewPanda.getEffect() != null && mouseEvent.getY() < 575 && capacityInt.get() >= 10){
                    capacityInt.addAndGet(-10);
                    capacity.setText("Capacity : " + capacityInt);
                    new PandaAttack(mouseEvent.getX(), mouseEvent.getY(), root, defensivePlayer.getMap()).start();
                }
            if (capacityInt.get() < 30){
                root.getChildren().remove(imageViewArcher);
            }
            if (capacityInt.get() < 20){
                root.getChildren().remove(imageViewDragon);
            }
            if (capacityInt.get() < 10){
                root.getChildren().remove(imageViewBalloon);
                root.getChildren().remove(imageViewPanda);
            }
            if (capacityInt.get() < 2){
                root.getChildren().remove(imageViewGoblin);
            }
        });

        root.getChildren().addAll(rectangle, imageViewCapacity, capacity);
        new Thread(){
            BorderPane rbp = new TileMap().getPane();
            @Override
            public void run(){
                Platform.runLater(() -> {
                    root.getChildren().addAll(rbp.getChildren());
                });
            }
        };
        
        switch (attackingPlayer.getLevel()) {
            case 1:
                root.getChildren().addAll(imageViewDragon, imageViewPanda);
                break;
            case 2:
                root.getChildren().addAll(imageViewDragon,imageViewPanda, imageViewBalloon);
                break;
            case 3:
                root.getChildren().addAll(imageViewDragon, imageViewPanda, imageViewBalloon, imageViewGoblin);
                break;
            case 4:
                root.getChildren().addAll(imageViewBalloon, imageViewPanda, imageViewGoblin, imageViewArcher, imageViewDragon);
                break;
            default:
                break;
        }
        return root;
    }
}
