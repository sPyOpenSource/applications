package view;

import controller.attack.*;
import controller.CheckResultAttack;
import controller.defense.ArcherTowerDefense;
import controller.defense.InfernoTowerDefense;
import controller.defense.TeslaDefense;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.layout.AnchorPane;

import model.Player;
import assets.Assets;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.building.china.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class Battle extends Application {
    private final Assets assets = new Assets();
    private final static DropShadow shadow = new DropShadow(10, Color.GOLD);
    private final Player attackingPlayer;
    private final Player defensivePlayer;
    private final ArrayList<Player> players;
    private final AtomicInteger capacityInt;
    private final ArrayList<UnitIcon> icons = new ArrayList<>();
    
    public Battle(Player attackingPlayer, Player defensivePlayer, ArrayList<Player> players) {
        this.attackingPlayer = attackingPlayer;
        this.defensivePlayer = defensivePlayer;
        this.players = players;
        this.capacityInt = new AtomicInteger(attackingPlayer.getLevel() * 0);
        players.remove(defensivePlayer);
    }
    
    @Override
    public void start(Stage stage) {
        AnchorPane root = attack();
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
            for (Entity building : defensivePlayer.getMap().getBuildingsMap()){
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

    private AnchorPane attack(){
        AnchorPane root = defensivePlayer.getMap().getMapView();

        Rectangle rectangle = new Rectangle(0, 635, 1000, 737 - 635);
        rectangle.setFill(Color.rgb(0, 0, 0, 0.4));

        Text capacity = new Text("Capacity : " + capacityInt.get());
        capacity.setId("text");
        capacity.setX(810);
        capacity.setY(50);

        ImageView imageViewCapacity = new ImageView("assets/png/capacity.png");
        imageViewCapacity.setX(800);
        imageViewCapacity.setY(20);
        imageViewCapacity.setFitWidth(150);
        imageViewCapacity.setFitHeight(50);

        installIcon("assets/png/dragon_drag.png", 20, 10, 1,
                (x, y) -> new DragonAttack(x, y, root, defensivePlayer.getMap()));
        installIcon("assets/png/panda_drag.png", 110, 10, 1,
                (x, y) -> new PandaAttack(x, y, root, defensivePlayer.getMap()));
        installIcon("assets/png/balloon_drag.png", 200, 10, 2,
                (x, y) -> new DefBalloonAttack(x, y, root, defensivePlayer.getMap()));
        installIcon("assets/png/goblin_drag.png", 290, 2, 3,
                (x, y) -> new GoblinBalloonAttack(x, y, root, defensivePlayer.getMap()));
        installIcon("assets/png/archer_drag.png", 380, 3, 4,
                (x, y) -> new ArcherBalloonAttack(x, y, root, defensivePlayer.getMap()));
        installIcon("assets/png/goblin_drag.png"//new Barbarian(new Pos(0, 0)).getImages().get(0)
                , 470, 5, 1,
                (x, y) -> new BarbarianAttack(x, y, root, defensivePlayer.getMap()));
        installIcon("assets/png/reimu.png", 560, 20, 2,
                (x, y) -> new WizardAttack(x, y, root, defensivePlayer.getMap()));

        for (UnitIcon icon : icons) {
            if (attackingPlayer.getLevel() >= icon.minLevel) {
                root.getChildren().add(icon.view);
            }
        }
        icons.get(5).view.setEffect(shadow);

        //root.getChildren().addAll(rectangle, imageViewCapacity, capacity);
        root.setOnMouseClicked(mouseEvent -> deploy(mouseEvent.getX(), mouseEvent.getY(), root, capacity));
        return root;
    }

    private void deploy(double x, double y, AnchorPane root, Text capacity) {
        if (y >= 575) {
            return;
        }
        for (UnitIcon icon : icons) {
            if (icon.view.getEffect() != null
                    && capacityInt.get() >= icon.cost
                    && root.getChildren().contains(icon.view)) {
                capacityInt.addAndGet(-icon.cost);
                capacity.setText("Capacity : " + capacityInt.get());
                icon.spawn.apply(x, y).start();
            }
        }
        refreshBar(root);
    }

    private void refreshBar(AnchorPane root) {
        for (UnitIcon icon : icons) {
            if (capacityInt.get() < icon.cost && root.getChildren().contains(icon.view)) {
                root.getChildren().remove(icon.view);
            } else if(!root.getChildren().contains(icon.view)){
                root.getChildren().add(icon.view);
            }
        }
    }

    private UnitIcon installIcon(String imagePath, double x, int cost, int minLevel,
            BiFunction<Double, Double, Thread> spawn) {
        return installIcon(new ImageView(imagePath), x, cost, minLevel, spawn);
    }

    private UnitIcon installIcon(Image image, double x, int cost, int minLevel,
            BiFunction<Double, Double, Thread> spawn) {
        return installIcon(new ImageView(image), x, cost, minLevel, spawn);
    }

    private UnitIcon installIcon(ImageView view, double x, int cost, int minLevel,
            BiFunction<Double, Double, Thread> spawn) {
        UnitIcon icon = new UnitIcon(view, cost, minLevel, spawn);
        icon.view.setX(x);
        icon.view.setY(642);
        icon.view.setFitWidth(80);
        icon.view.setFitHeight(85);
        icon.view.setOnMouseClicked(mouseEvent -> {
            for (UnitIcon other : icons) {
                other.view.setEffect(null);
            }
            icon.view.setEffect(shadow);
        });
        icons.add(icon);
        return icon;
    }
}
