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
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.building.EntityBuilding;
import model.hero.Dragon;
import assets.Assets;

public class DragonAttack extends Thread {
    private final Assets assets = new Assets();
    
    public DragonAttack(double x, double y, AnchorPane root, Map map) {
        this.root = root;
        this.dragon = new Dragon(x, y);
        this.viewDragonL = new ImageView(dragon.getImageViews().get(0));
        this.viewDragonAttackL = new ImageView(dragon.getImageViews().get(1));
        this.viewDragonR = new ImageView(dragon.getImageViews().get(2));
        this.viewDragonAttackR = new ImageView(dragon.getImageViews().get(3));
        this.viewDragon = new ImageView(viewDragonL.getImage());
        this.map = map;
        dragon.setImageView(viewDragon);
        this.map.getAttackingHeroes().add(dragon);
        mediaPlayerAttack.setVolume(0.2);
    }
    
    private final AnchorPane root;
    private final ImageView viewDragonL;
    private final ImageView viewDragonR;
    private final ImageView viewDragonAttackL;
    private final ImageView viewDragonAttackR;
    private final ImageView viewDragon ;
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
            Platform.runLater(() -> {
                root.getChildren().addAll(new MediaView(mediaPlayer), new MediaView(mediaPlayerAttack));
                root.getChildren().add(viewDragon);
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!dragon.isDead()){
                EntityBuilding building = moveToward();
                if (dragon.isDead())
                    break;
                attack(building);
            }
            Platform.runLater(() -> {
                root.getChildren().remove(viewDragon);
                mediaPlayer.stop();
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            map.getAttackingHeroes().remove(dragon);
        }
    }
    
    private synchronized EntityBuilding moveToward(){
        double widthLowe = 10000;
        double width;
        EntityBuilding building = null;
        for (Entity node : map.getBuildingsMap()){
            if (node instanceof EntityBuilding entityBuilding){
                width = Math.sqrt((Math.pow(viewDragon.getX() - entityBuilding.getTexture().getX(), 2)) + Math.pow(viewDragon.getY() - entityBuilding.getTexture().getY(), 2));
                if(width < widthLowe){
                    widthLowe = width;
                    building = entityBuilding;
                }
            }
        }
        if (building != null){
            EntityBuilding finalBuilding = building;
            Platform.runLater(() -> {
                if (viewDragon.getX() < finalBuilding.getTexture().getX()){
                    viewDragon.setImage(viewDragonL.getImage());
                    viewDragon.setFitWidth(viewDragonL.getFitWidth());
                    viewDragon.setFitHeight(viewDragonL.getFitHeight());
                } else {
                    viewDragon.setImage(viewDragonR.getImage());
                    viewDragon.setFitWidth(viewDragonR.getFitWidth());
                    viewDragon.setFitHeight(viewDragonR.getFitHeight());
                }
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            MoveTo moveTo = new MoveTo(viewDragon.getX(), viewDragon.getY());
            LineTo lineTo;
            if (viewDragon.getImage().equals(viewDragonL.getImage())){
                lineTo = new LineTo(building.getTexture().getX(), building.getTexture().getY());
            } else {
                lineTo = new LineTo(building.getTexture().getX() + building.getTexture().getFitWidth(), building.getTexture().getY());
            }
            Path path = new Path();
            path.getElements().addAll(moveTo, lineTo);
            PathTransition transition = new PathTransition();
            transition.setDuration(Duration.millis((widthLowe / dragon.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(viewDragon);
            transition.setAutoReverse(false);
            transition.setPath(path);
            Platform.runLater(transition::play);
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            while (elapsedTime < (long) ((widthLowe / dragon.getMoveSpeed()) * 300)) {
                if (dragon.getHp() <= 0){
                    dragon.setDead();
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
            dragon.setDead();
        }
        return building;
    }
    
    private synchronized void attack(EntityBuilding building){
        Platform.runLater(() -> {
            if (viewDragon.getImage().equals(viewDragonR.getImage())){
                viewDragon.setImage(viewDragonAttackR.getImage());
                viewDragon.setFitWidth(viewDragonAttackR.getFitWidth());
                viewDragon.setFitHeight(viewDragonAttackR.getFitHeight());
            } else {
                viewDragon.setImage(viewDragonAttackL.getImage());
                viewDragon.setFitWidth(viewDragonAttackL.getFitWidth());
                viewDragon.setFitHeight(viewDragonAttackL.getFitHeight());
            }
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (building.getHp() >= 0){
            if (dragon.getHp() <= 0){
                dragon.setDead();
                break;
            }
            Platform.runLater(mediaPlayerAttack::play);
            building.setHp((int)(building.getHp() - (dragon.getDamagePerSecond() * 1.25)));
            try {
                wait(1250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(mediaPlayerAttack::stop);
        }
        if (!dragon.isDead()){
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
                if (viewDragon.getImage().equals(viewDragonAttackR.getImage())) {
                    viewDragon.setX(building.getImageView().getX() + building.getImageView().getFitWidth());
                    viewDragon.setY(building.getImageView().getY());
                } else {
                    viewDragon.setX(building.getImageView().getX());
                    viewDragon.setY(building.getImageView().getY());
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
