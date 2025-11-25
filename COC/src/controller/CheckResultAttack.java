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
import model.data.UpdatePlayerData;
import view.PlayerPanel;
import cr0s.javara.render.map.Map;

import assets.Assets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckResultAttack extends Thread {
    private Assets assets = new Assets();
    
    public CheckResultAttack(Stage stage, AnchorPane root, Player attackingPlayer, Player defensivePlayer, AtomicInteger capacityInt, ArrayList<Player> players, MediaPlayer mediaPlayer) {
        this.map = defensivePlayer.getMap();
        this.firstSize = defensivePlayer.getMap().getBuildingsMap().size();
        this.root = root;
        this.attackingPlayer = attackingPlayer;
        this.defensivePlayer = defensivePlayer;
        this.capacityInt = capacityInt;
        this.stage = stage;
        this.players = players;
        this.newView();
        this.mediaPlayer = mediaPlayer;
    }

    private final Map map;
    private final double firstSize;
    private final AnchorPane root;
    private final AtomicInteger capacityInt;
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
                    e.printStackTrace();
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
            e.printStackTrace();
        }
        while (true){
            if (defensivePlayer.getMap().getBuildingsMap().isEmpty()){
                defensivePlayer.setNumberLose(defensivePlayer.getNumberLose()+1);
                attackingPlayer.setNumberWin(attackingPlayer.getNumberWin()+1);
                if (defensivePlayer.getLevel() > 1){
                    defensivePlayer.setLevel(defensivePlayer.getLevel()-1);
                }
                if (attackingPlayer.getLevel() < 4){
                    attackingPlayer.setLevel(attackingPlayer.getLevel()+1);
                }
                new UpdatePlayerData(attackingPlayer).start();
                new UpdatePlayerData(defensivePlayer).start();
                Platform.runLater(() -> {
                    starView.setImage(new Image("assets/png/star3.png"));
                    viewBack.setImage(new Image("assets/png/back_home.png"));
                    myNotify();
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            } else if (capacityInt.get() == 0 && map.getAttackingHeroes().isEmpty()){
                if ((map.getBuildingsMap().size() / firstSize) < 0.15){
                    defensivePlayer.setNumberWin(defensivePlayer.getNumberWin() + 1);
                    attackingPlayer.setNumberLose(attackingPlayer.getNumberLose() + 1);
                    if (defensivePlayer.getLevel() < 4){
                        defensivePlayer.setLevel(defensivePlayer.getLevel() + 1);
                    }
                    if (attackingPlayer.getLevel() > 1){
                        attackingPlayer.setLevel(attackingPlayer.getLevel() - 1);
                    }
                    new UpdatePlayerData(attackingPlayer).start();
                    new UpdatePlayerData(defensivePlayer).start();
                    Platform.runLater(() -> {
                        starView.setImage(new Image("assets/png/star2.png"));
                        viewBack.setImage(new Image("assets/png/back_home.png"));
                        textPercent.setText((100 - ((int)((map.getBuildingsMap().size() * 100) / firstSize))) + "%");
                        myNotify();
                    });
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                } else if ((map.getBuildingsMap().size() / firstSize) < 0.5){
                    defensivePlayer.setNumberWin(defensivePlayer.getNumberWin() + 1);
                    attackingPlayer.setNumberLose(attackingPlayer.getNumberLose() + 1);
                    if (defensivePlayer.getLevel() < 4){
                        defensivePlayer.setLevel(defensivePlayer.getLevel() + 1);
                    }
                    if (attackingPlayer.getLevel() > 1){
                        attackingPlayer.setLevel(attackingPlayer.getLevel() - 1);
                    }
                    new UpdatePlayerData(attackingPlayer).start();
                    new UpdatePlayerData(defensivePlayer).start();
                    Platform.runLater(() -> {
                        starView.setImage(new Image("assets/png/star1.png"));
                        viewBack.setImage(new Image("assets/png/back_home.png"));
                        textPercent.setText((100 - ((int)((map.getBuildingsMap().size() * 100) / firstSize))) + "%");
                        myNotify();
                    });
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                defensivePlayer.setNumberWin(defensivePlayer.getNumberWin() + 1);
                attackingPlayer.setNumberLose(attackingPlayer.getNumberLose() + 1);
                if (defensivePlayer.getLevel() < 4){
                    defensivePlayer.setLevel(defensivePlayer.getLevel() + 1);
                }
                if (attackingPlayer.getLevel() > 1){
                    attackingPlayer.setLevel(attackingPlayer.getLevel() - 1);
                }
                new UpdatePlayerData(attackingPlayer).start();
                new UpdatePlayerData(defensivePlayer).start();
                Platform.runLater(() -> {
                    starView.setImage(new Image("assets/png/star0.png"));
                    viewBack.setImage(new Image("assets/png/back_home.png"));
                    textPercent.setText((100 - ((int)((map.getBuildingsMap().size() * 100) / firstSize))) + "%");
                    myNotify();
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
