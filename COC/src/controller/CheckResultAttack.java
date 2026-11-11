package controller;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import model.Player;
import view.PlayerPanel;
import cr0s.javara.render.map.Map;
import util.Log;

import assets.Assets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import view.UnitIcon;

public class CheckResultAttack extends Thread {
    private final Assets assets = new Assets();
    
    public CheckResultAttack(Stage stage, AnchorPane root, Player attackingPlayer, Player defensivePlayer, ArrayList<Player> players, MediaPlayer mediaPlayer, ArrayList<UnitIcon> icons) {
        this.map = defensivePlayer.getMap();
        this.firstSize = defensivePlayer.getMap().getBuildingsMap().size();
        this.root = root;
        this.attackingPlayer = attackingPlayer;
        this.defensivePlayer = defensivePlayer;
        this.capacityInt = new AtomicInteger(0);
        this.stage = stage;
        this.players = players;
        this.newView();
        this.mediaPlayer = mediaPlayer;
        timer = System.nanoTime();
        this.icons = icons;
    }

    private final Map map;
    private final double firstSize;
    private long timer;
    private final AnchorPane root;
    public final AtomicInteger capacityInt;
    private final Player attackingPlayer;
    private final Player defensivePlayer;
    private final ArrayList<Player> players;
    private final Stage stage;
    private ImageView starView;
    private Text textPercent;
    private ImageView viewBack;
    private final MediaPlayer mediaPlayer;
    private final String audioFilePath = assets.get("/assets/audio/click_button.mp3");
    private final MediaPlayer mediaPlayerClick = new MediaPlayer(new Media(audioFilePath));
    ArrayList<UnitIcon> icons;
    
    public void refreshBar() {
        for (UnitIcon icon : icons) {
            if (capacityInt.get() < icon.cost && root.getChildren().contains(icon.view)) {
                Platform.runLater(() -> {
                    root.getChildren().remove(icon.view);
                });
            } else if(capacityInt.get() > icon.cost && !root.getChildren().contains(icon.view)){
                Platform.runLater(() -> {
                    root.getChildren().add(icon.view);
                });
            }
        }
    }
    
    @Override
    public synchronized void run() {
        Platform.runLater(() -> {
            root.getChildren().add(new MediaView(mediaPlayerClick));
            root.getChildren().add(viewBack);
            root.getChildren().add(starView);
            root.getChildren().add(textPercent);
            viewBack.setOnMouseClicked(mouseEvent -> {
                Platform.runLater(mediaPlayerClick::play);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Log.error("result", "Interrupted in CheckResultAttack", e);
                }
                Platform.runLater(mediaPlayerClick::stop);
                new PlayerPanel(attackingPlayer, players).start(new Stage());
                Platform.runLater(mediaPlayer::stop);
                stage.close();
            });
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            Log.error("result", "Interrupted in CheckResultAttack", e);
        }
        while (true){
            if (defensivePlayer.getMap().getBuildingsMap().isEmpty()){
                Log.info("result", "Defender eliminated; attacker wins");
                defensivePlayer.Lose();
                attackingPlayer.Win();
                //new UpdatePlayerData(attackingPlayer).start();
                //new UpdatePlayerData(defensivePlayer).start();
                Platform.runLater(() -> {
                    starView.setImage(new Image("assets/png/star3.png"));
                    viewBack.setImage(new Image("assets/png/back_home.png"));
                    myNotify();
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.error("result", "Interrupted in CheckResultAttack", e);
                }
                break;
            } else if (System.nanoTime() - timer > 30000000000l){
                if ((map.getBuildingsMap().size() / firstSize) < 0.15){
                    Log.info("result", "Attacker reaches 2-star (buildings remaining " + map.getBuildingsMap().size() + "/" + (int) firstSize + ")");
                    defensivePlayer.Win();
                    attackingPlayer.Lose();
                    //new UpdatePlayerData(attackingPlayer).start();
                    //new UpdatePlayerData(defensivePlayer).start();
                    Platform.runLater(() -> {
                        starView.setImage(new Image("assets/png/star2.png"));
                        viewBack.setImage(new Image("assets/png/back_home.png"));
                        textPercent.setText((100 - ((int)((map.getBuildingsMap().size() * 100) / firstSize))) + "%");
                        myNotify();
                    });
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Log.error("result", "Interrupted in CheckResultAttack", e);
                    }
                    break;
                } else if ((map.getBuildingsMap().size() / firstSize) < 0.5){
                    Log.info("result", "Attacker reaches 1-star (buildings remaining " + map.getBuildingsMap().size() + "/" + (int) firstSize + ")");
                    defensivePlayer.Win();
                    attackingPlayer.Lose();
                    //new UpdatePlayerData(attackingPlayer).start();
                    //new UpdatePlayerData(defensivePlayer).start();
                    Platform.runLater(() -> {
                        starView.setImage(new Image("assets/png/star1.png"));
                        viewBack.setImage(new Image("assets/png/back_home.png"));
                        textPercent.setText((100 - ((int)((map.getBuildingsMap().size() * 100) / firstSize))) + "%");
                        myNotify();
                    });
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Log.error("result", "Interrupted in CheckResultAttack", e);
                    }
                    break;
                }
                Log.info("result", "Time expired; defender wins (buildings remaining " + map.getBuildingsMap().size() + "/" + (int) firstSize + ")");
                defensivePlayer.Win();
                attackingPlayer.Lose();
                //new UpdatePlayerData(attackingPlayer).start();
                //new UpdatePlayerData(defensivePlayer).start();
                Platform.runLater(() -> {
                    starView.setImage(new Image("assets/png/star0.png"));
                    viewBack.setImage(new Image("assets/png/back_home.png"));
                    textPercent.setText((100 - ((int)((map.getBuildingsMap().size() * 100) / firstSize))) + "%");
                    myNotify();
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.error("result", "Interrupted in CheckResultAttack", e);
                }
                break;
            }
            try {
                wait(300);
                if(capacityInt.get() < attackingPlayer.getLevel() * 15){
                    capacityInt.addAndGet(2);
                    refreshBar();
                }
            } catch (InterruptedException e) {
                Log.error("result", "Interrupted in CheckResultAttack", e);
            }
        }
    }
    
    public synchronized void myNotify(){
        notify();
    }
    
    private void newView(){
        this.starView = new ImageView();
        starView.setX(231);
        starView.setY(150);
        starView.setFitWidth(537);
        starView.setFitHeight(220);

        this.textPercent = new Text();
        textPercent.setId("text");
        textPercent.setX(480);
        textPercent.setY(240);

        this.viewBack = new ImageView();
        viewBack.setX(450);
        viewBack.setY(500);
        viewBack.setFitWidth(100);
        viewBack.setFitHeight(43);
    }
}
