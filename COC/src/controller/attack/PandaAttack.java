package controller.attack;

import javafx.animation.PathTransition;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.util.Duration;

import javafx.scene.Node;
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

import view.ImageViewClone;
import model.hero.Panda;
import java.util.concurrent.atomic.AtomicInteger;
import assets.Assets;

public class PandaAttack extends Thread {
    private final Assets assets = new Assets();
    
    public PandaAttack(double x, double y, AnchorPane root, Map map) {
        this.root = root;
        this.panda = new Panda(x, y);
        this.viewPandaL = new ImageViewClone(panda.getImageViews().get(7));
        this.viewPandaAttackL = new ImageViewClone(panda.getImageViews().get(1));
        this.viewPandaR = new ImageViewClone(panda.getImageViews().get(2));
        this.viewPandaAttackR = new ImageViewClone(panda.getImageViews().get(3));
        this.viewPanda = new ImageViewClone(viewPandaL);
        this.map = map;
        panda.setImageView(viewPanda);
        this.map.getAttackingHeroes().add(panda);
        root.getChildren().add(new MediaView(mediaPlayerMove));
        mediaPlayerMove.setVolume(0.2);
    }
    
    private final AnchorPane root;
    private final ImageView viewPandaL;
    private final ImageView viewPandaR;
    private final ImageView viewPandaAttackL;
    private final ImageView viewPandaAttackR;
    private final ImageView viewPanda ;
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
                root.getChildren().add(viewPanda);
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
                root.getChildren().remove(viewPanda);
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
        for (Node node : map.getBuildingsMap()){
            if (node instanceof EntityBuilding building1){
                width = Math.sqrt((Math.pow(viewPanda.getX() - building1.getImageViews().get(0).getX(), 2)) + Math.pow(viewPanda.getY() - building1.getImageViews().get(0).getY(), 2));
                if(width < widthLowe){
                    widthLowe = width;
                    building = building1;
                }
            }
        }
        if (building != null){
            EntityBuilding finalBuilding = building;
            Platform.runLater(() -> {
                if (viewPanda.getX() < finalBuilding.getImageViews().get(0).getX()){
                    viewPanda.setImage(viewPandaL.getImage());
                    viewPanda.setFitWidth(viewPandaL.getFitWidth());
                    viewPanda.setFitHeight(viewPandaL.getFitHeight());
                } else {
                    viewPanda.setImage(viewPandaR.getImage());
                    viewPanda.setFitWidth(viewPandaR.getFitWidth());
                    viewPanda.setFitHeight(viewPandaR.getFitHeight());
                }
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            MoveTo moveTo = new MoveTo(viewPanda.getX(), viewPanda.getY());
            LineTo lineTo;
            if (viewPanda.getImage().equals(viewPandaL.getImage())){
                lineTo = new LineTo(building.getImageViews().get(0).getX(), building.getImageViews().get(0).getY()+(building.getImageViews().get(0).getFitHeight()/3));
            } else {
                lineTo = new LineTo(building.getImageViews().get(0).getX()+building.getImageViews().get(0).getFitWidth(), building.getImageViews().get(0).getY()+(building.getImageViews().get(0).getFitHeight()/3));
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
            transition.setNode(viewPanda);
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
            if (viewPanda.getImage().equals(viewPandaR.getImage())){
                viewPanda.setImage(viewPandaAttackR.getImage());
                viewPanda.setFitWidth(viewPandaAttackR.getFitWidth());
                viewPanda.setFitHeight(viewPandaAttackR.getFitHeight());
            } else {
                viewPanda.setImage(viewPandaAttackL.getImage());
                viewPanda.setFitWidth(viewPandaAttackL.getFitWidth());
                viewPanda.setFitHeight(viewPandaAttackL.getFitHeight());
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
                root.getChildren().remove(building.getTexture());
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                if (viewPanda.getImage().equals(viewPandaAttackR.getImage())) {
                    viewPanda.setX(building.getImageViews().get(0).getX() + building.getImageViews().get(0).getFitWidth());
                    viewPanda.setY(building.getImageViews().get(0).getY());
                } else {
                    viewPanda.setX(building.getImageViews().get(0).getX());
                    viewPanda.setY(building.getImageViews().get(0).getY());
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
