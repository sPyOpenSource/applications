package controller.attack;

import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import cr0s.javara.render.map.Map;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.Entity;

import model.hero.Panda;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;
import assets.Assets;

public class PandaAttack extends Thread {
    private final Assets assets = new Assets();
    private final Random random = new Random();
    
    public PandaAttack(double x, double y, AnchorPane root, Map map) {
        this.root = root;
        this.panda = new Panda(x, y);
        this.viewPandaL = new ImageView(panda.getImageViews().get(random.nextInt(panda.getImageViews().size())));
        this.viewPandaAttackL = new ImageView(panda.getImageViews().get(1));
        this.viewPandaR = new ImageView(panda.getImageViews().get(2));
        this.viewPandaAttackR = new ImageView(panda.getImageViews().get(3));
        this.map = map;
        panda.setImageView(new ImageView(viewPandaL.getImage()));
        this.map.getAttackingHeroes().add(panda);
        root.getChildren().add(new MediaView(mediaPlayerMove));
        mediaPlayerMove.setVolume(0.2);
    }
    
    private final AnchorPane root;
    private final ImageView viewPandaL;
    private final ImageView viewPandaR;
    private final ImageView viewPandaAttackL;
    private final ImageView viewPandaAttackR;
    private final Panda panda;
    private final Map map;
    String audioFilePathMove = assets.get("/assets/audio/not_path.mp3");
    MediaPlayer mediaPlayerMove = new MediaPlayer(new Media(audioFilePathMove));
    private final AtomicInteger count = new AtomicInteger(0);
    
    @Override
    public void run() {
        synchronized (this){
            String audioFilePath = assets.get("/assets/audio/enter_panda.mp3");
            MediaPlayer mediaPlayer = new MediaPlayer(new Media(audioFilePath));
            mediaPlayer.setAutoPlay(true);
            Platform.runLater(() -> {
                root.getChildren().add(new MediaView(mediaPlayer));
                root.getChildren().add(panda.getImageView());
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!panda.isDead()){
                EntityBuilding building = moveToward();
                if (panda.isDead())
                    break;
                attack(building);
            }
            Platform.runLater(() -> {
                root.getChildren().remove(panda.getImageView());
                mediaPlayer.stop();
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            map.getAttackingHeroes().remove(panda);
        }
    }
    
    private synchronized EntityBuilding moveToward(){
        double widthLowe = 10000;
        double width;
        EntityBuilding building = null;
        for (Entity node : map.getBuildingsMap()){
            if (node instanceof EntityBuilding building1){
                width = Math.sqrt((Math.pow(panda.getImageView().getX() - building1.getImageView().getX(), 2)) + Math.pow(panda.getImageView().getY() - building1.getImageView().getY(), 2));
                if(width < widthLowe){
                    widthLowe = width;
                    building = building1;
                }
            }
        }
        if (building != null){
            EntityBuilding finalBuilding = building;
            Platform.runLater(() -> {
                if (panda.getImageView().getX() < finalBuilding.getImageView().getX()){
                    panda.getImageView().setImage(viewPandaL.getImage());
                } else {
                    panda.getImageView().setImage(viewPandaR.getImage());
                }
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            MoveTo moveTo = new MoveTo(panda.getImageView().getX(), panda.getImageView().getY());
            LineTo lineTo;
            if (panda.getImageView().getImage().equals(viewPandaL.getImage())){
                lineTo = new LineTo(building.getImageView().getX(), building.getImageView().getY()+(building.getImageView().getFitHeight()/3));
            } else {
                lineTo = new LineTo(building.getImageView().getX()+building.getImageView().getFitWidth(), building.getImageView().getY()+(building.getImageView().getFitHeight()/3));
            }
            Platform.runLater(mediaPlayerMove::stop);
            Path path = new Path();
            if (count.getAndAdd(1) % 3 == 1){
                Platform.runLater(mediaPlayerMove::play);
            }
            path.getElements().addAll(moveTo, lineTo);
            PathTransition transition = new PathTransition();
            transition.setDuration(Duration.millis((widthLowe / panda.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(panda.getImageView());
            transition.setAutoReverse(false);
            transition.setPath(path);
            Platform.runLater(transition::play);
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            while (elapsedTime < (long) ((widthLowe / panda.getMoveSpeed()) * 300)) {
                if (panda.getHp() <= 0){
                    panda.setDead();
                    break;
                }
                try {
                    wait(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                elapsedTime = System.currentTimeMillis() - startTime;
            }
        } else {
            panda.setDead();
        }
        return building;
    }
    
    private synchronized void attack(EntityBuilding building){
        Platform.runLater(() -> {
            if (panda.getImageView().getImage().equals(viewPandaR.getImage())){
                panda.getImageView().setImage(viewPandaAttackR.getImage());
            } else {
                panda.getImageView().setImage(viewPandaAttackL.getImage());
            }
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (building.getHp() >= 0){
            if (panda.getHp() <= 0){
                panda.setDead();
                break;
            }
//            Platform.runLater(mediaPlayer::play);
            building.setHp((int)(building.getHp() - (panda.getDamagePerSecond() * 1.25)));
            try {
                wait(1250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            Platform.runLater(mediaPlayer::stop);
        }
        if (!panda.isDead()){
            map.getBuildingsMap().remove(building);
            Platform.runLater(() -> {
                root.getChildren().remove(building.getImageView());
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                if (panda.getImageView().getImage().equals(viewPandaAttackR.getImage())) {
                    panda.getImageView().setX(building.getImageView().getX() + building.getImageView().getFitWidth());
                    panda.getImageView().setY(building.getImageView().getY());
                } else {
                    panda.getImageView().setX(building.getImageView().getX());
                    panda.getImageView().setY(building.getImageView().getY());
                }
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void myNotify(){
        notify();
    }
}
