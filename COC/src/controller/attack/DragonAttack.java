package controller.attack;

import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import cr0s.javara.render.map.Map;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.building.EntityBuilding;
import assets.Assets;
import cr0s.javara.entity.aircraft.Dragon;
import cr0s.javara.util.Pos;
import util.Log;

public class DragonAttack extends Thread {
    private final Assets assets = new Assets();
    
    public DragonAttack(double x, double y, AnchorPane root, Map map) {
        this.root = root;
        this.dragon = new Dragon(new Pos(x, y));
        this.viewDragonL = new ImageView(dragon.getImages().get(0));
        this.viewDragonAttackL = new ImageView(dragon.getImages().get(1));
        this.viewDragonR = new ImageView(dragon.getImages().get(2));
        this.viewDragonAttackR = new ImageView(dragon.getImages().get(3));
        this.map = map;
        dragon.setImageView(new ImageView(viewDragonL.getImage()));
        this.map.getAttackingHeroes().add(dragon);
        mediaPlayerAttack.setVolume(0.2);
    }
    
    private final AnchorPane root;
    private final ImageView viewDragonL;
    private final ImageView viewDragonR;
    private final ImageView viewDragonAttackL;
    private final ImageView viewDragonAttackR;
    private final Dragon dragon;
    private final Map map;
    private final String audioFilePathAttack = assets.get("/assets/audio/fire.mp3");
    private final MediaPlayer mediaPlayerAttack = new MediaPlayer(new Media(audioFilePathAttack));
    
    @Override
    public void run() {
        synchronized (this){
            String audioFilePath = assets.get("/assets/audio/fly.mp3");
            MediaPlayer mediaPlayer = new MediaPlayer(new Media(audioFilePath));
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(10000);
            mediaPlayer.setVolume(0.3);
            MediaView mediaViewFly = new MediaView(mediaPlayer);
            MediaView mediaViewAttack = new MediaView(mediaPlayerAttack);
            Platform.runLater(() -> {
                root.getChildren().addAll(mediaViewFly, mediaViewAttack, dragon.getImageView());
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.error("combat", "Interrupted in DragonAttack", e);
            }
            while (!dragon.isDead()){
                Log.trace("combat", dragon + " seeking nearest building");
                EntityBuilding building = moveToward();
                if (dragon.isDead())
                    break;
                attack(building);
            }
            Platform.runLater(() -> {
                root.getChildren().remove(dragon.getImageView());
                root.getChildren().remove(mediaViewFly);
                root.getChildren().remove(mediaViewAttack);
                mediaPlayer.stop();
                mediaPlayerAttack.stop();
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.error("combat", "Interrupted in DragonAttack", e);
            }
            map.getAttackingHeroes().remove(dragon);
        }
    }
    
    private synchronized EntityBuilding moveToward(){
        double widthLowe = Double.MAX_VALUE;
        double width;
        EntityBuilding building = null;
        for (Entity node : map.getBuildingsMap()){
            if (node instanceof EntityBuilding entityBuilding){
                width = Math.sqrt((Math.pow(dragon.getImageView().getX() - entityBuilding.getImageView().getX(), 2)) + Math.pow(dragon.getImageView().getY() - entityBuilding.getImageView().getY(), 2));
                if(width < widthLowe){
                    widthLowe = width;
                    building = entityBuilding;
                }
            }
        }
        if (building != null){
            EntityBuilding finalBuilding = building;
            Platform.runLater(() -> {
                if (dragon.getImageView().getX() < finalBuilding.getImageView().getX()){
                    dragon.getImageView().setImage(viewDragonL.getImage());
                } else {
                    dragon.getImageView().setImage(viewDragonR.getImage());
                }
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.error("combat", "Interrupted in DragonAttack", e);
            }


            Log.trace("combat", dragon + " moving toward " + building);
            MoveTo moveTo = new MoveTo(dragon.getImageView().getX(), dragon.getImageView().getY());
            LineTo lineTo;
            if (dragon.getImageView().getImage().equals(viewDragonL.getImage())){
                lineTo = new LineTo(building.getImageView().getX(), building.getImageView().getY());
            } else {
                lineTo = new LineTo(building.getImageView().getX() + building.getImageView().getFitWidth(), building.getImageView().getY());
            }
            Path path = new Path();
            path.getElements().addAll(moveTo, lineTo);
            PathTransition transition = new PathTransition();
            transition.setDuration(Duration.millis((widthLowe / dragon.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(dragon.getImageView());
            transition.setAutoReverse(false);
            transition.setPath(path);
            Platform.runLater(transition::play);
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            while (elapsedTime < (long) ((widthLowe / dragon.getMoveSpeed()) * 400)) {
                if (dragon.getHp() <= 0){
                    dragon.setDead();
                    break;
                }
                try {
                    wait(300);
                } catch (InterruptedException e) {
                    Log.error("combat", "Interrupted in DragonAttack", e);
                }
                elapsedTime = System.currentTimeMillis() - startTime;
            }
        } else {
            dragon.setDead();
        }
        return building;
    }
    
    private synchronized void attack(EntityBuilding building){
        Platform.runLater(() -> {
            if (dragon.getImageView().getImage().equals(viewDragonR.getImage())){
                dragon.getImageView().setImage(viewDragonAttackR.getImage());
            } else {
                dragon.getImageView().setImage(viewDragonAttackL.getImage());
            }
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            Log.error("combat", "Interrupted in DragonAttack", e);
        }
        while (building.getHp() >= 0){
            Log.trace("combat", dragon + " attacking " + building + " (hp=" + building.getHp() + ")");
            if (dragon.getHp() <= 0){
                dragon.setDead();
                break;
            }
            Platform.runLater(mediaPlayerAttack::play);
            Platform.runLater(() -> AttackEffects.flashImpact(root,
                    building.getImageView().getX() + building.getImageView().getFitWidth() / 2,
                    building.getImageView().getY() + building.getImageView().getFitHeight() / 2,
                    Color.web("#FF4500")));
            building.setHp((int)(building.getHp() - (dragon.getDamagePerSecond() * 1.25)));
            Platform.runLater(() -> AttackEffects.pulse(building.getImageView()));
            try {
                wait(1250);
            } catch (InterruptedException e) {
                Log.error("combat", "Interrupted in DragonAttack", e);
            }
            Platform.runLater(mediaPlayerAttack::stop);
        }
        if (!dragon.isDead()){
            map.getBuildingsMap().remove(building);
            Log.trace("combat", dragon + " destroyed " + building);
            Platform.runLater(() -> {
                root.getChildren().remove(building.getImageView());
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.error("combat", "Interrupted in DragonAttack", e);
            }

            Platform.runLater(() -> {
                if (dragon.getImageView().getImage().equals(viewDragonAttackR.getImage())) {
                    dragon.getImageView().setX(building.getImageView().getX() + building.getImageView().getFitWidth());
                    dragon.getImageView().setY(building.getImageView().getY());
                } else {
                    dragon.getImageView().setX(building.getImageView().getX());
                    dragon.getImageView().setY(building.getImageView().getY());
                }
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.error("combat", "Interrupted in DragonAttack", e);
            }
        }
    }

    public synchronized void myNotify(){
        notify();
    }
}
