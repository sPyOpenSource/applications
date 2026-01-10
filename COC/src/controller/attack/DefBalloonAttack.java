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
import model.hero.DefBalloon;

import assets.Assets;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.building.EntityBuilding;

public class DefBalloonAttack extends Thread {
    private final Assets assets = new Assets();
    
    public DefBalloonAttack(double x, double y, AnchorPane root, Map map) {
        this.root = root;
        this.defBalloon = new DefBalloon(x,y);
        this.viewBalloonMove = new ImageView(defBalloon.getImageViews().get(0));
        this.viewBalloonAttack = new ImageView(defBalloon.getImageViews().get(1));
        this.map = map;
        defBalloon.setImageView(new ImageView(viewBalloonMove.getImage()));
        this.map.getAttackingHeroes().add(defBalloon);
    }
    
    private final AnchorPane root;
    private final ImageView viewBalloonAttack;
    private final ImageView viewBalloonMove;
    private final DefBalloon defBalloon;
    private final Map map;

    @Override
    public void run() {
        synchronized (this){
            String audioFilePath = assets.get("/assets/audio/balloonDeploy.mp3");
            MediaPlayer mediaPlayer = new MediaPlayer(new Media(audioFilePath));
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setVolume(0.5);
            Platform.runLater(() -> {
                root.getChildren().add(new MediaView(mediaPlayer));
                root.getChildren().add(defBalloon.getImageView());
            });
            while (!defBalloon.isDead()){
                EntityBuilding building = moveToward();
                if (defBalloon.isDead())
                    break;
                attack(building);
            }
            String audioFilePathDie = assets.get("/assets/audio/balloonDie.mp3");
            MediaPlayer mediaPlayerDie = new MediaPlayer(new Media(audioFilePathDie));
            mediaPlayerDie.setAutoPlay(true);
            mediaPlayerDie.setVolume(0.5);
            Platform.runLater(() -> {
                root.getChildren().remove(defBalloon.getImageView());
                root.getChildren().add(new MediaView(mediaPlayerDie));
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            map.getAttackingHeroes().remove(defBalloon);
        }
    }
    
    private synchronized EntityBuilding moveToward(){
        double widthLowe = 10000;
        double width;
        EntityBuilding building = null;
        for (Entity node : map.getBuildingsMap()){
            if (node instanceof EntityBuilding){
                /*if (((Building) node).getBuildingType().equals(defBalloon.getFavoriteTarget())){
                    width = Math.sqrt((Math.pow(viewBalloon.getX()-((Building) node).getImageView().getX(),2))+Math.pow(viewBalloon.getY()-((Building) node).getImageView().getY(),2));
                    if(width < widthLowe){
                        widthLowe = width;
                        building = (Building) node;
                    }
                }*/
            }
        }
        if (building == null) {
            for (Entity node : map.getBuildingsMap()){
                if (node instanceof EntityBuilding entityBuilding){
                    width = Math.sqrt((Math.pow(defBalloon.getImageView().getX()-entityBuilding.getImageView().getX(),2))+Math.pow(defBalloon.getImageView().getY()-entityBuilding.getImageView().getY(),2));
                    if(width < widthLowe){
                        widthLowe = width;
                        building = entityBuilding;
                    }
                }
            }
        }

        if (building != null){
            Platform.runLater(() -> {
                defBalloon.getImageView().setImage(viewBalloonMove.getImage());
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            MoveTo moveTo = new MoveTo(defBalloon.getImageView().getX(), defBalloon.getImageView().getY());
            LineTo lineTo = new LineTo(building.getImageView().getX()+(building.getImageView().getFitWidth()/2),building.getImageView().getY());
            Path path = new Path();
            path.getElements().addAll(moveTo, lineTo);
            PathTransition transition = new PathTransition();
            transition.setDuration(Duration.millis((widthLowe / defBalloon.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(defBalloon.getImageView());
            transition.setAutoReverse(false);
            transition.setPath(path);
            Platform.runLater(transition::play);
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            while (elapsedTime < (long) ((widthLowe / defBalloon.getMoveSpeed()) * 300)) {
                if (defBalloon.getHp() <= 0){
                    defBalloon.setDead();
                    Platform.runLater(() -> root.getChildren().remove(defBalloon.getImageView()));
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
            defBalloon.setDead();
        }
        return building;
    }
    
    private synchronized void attack(EntityBuilding building){
        Platform.runLater(() -> {
            defBalloon.getImageView().setImage(viewBalloonAttack.getImage());
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (building.getHp() >= 0){
            if (defBalloon.getHp() <= 0){
                defBalloon.setDead();
                Platform.runLater(() -> root.getChildren().remove(defBalloon.getImageView()));
                break;
            }
            building.setHp(building.getHp() - defBalloon.getDamagePerSecond());
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!defBalloon.isDead()){
            map.getBuildingsMap().remove(building);
            Platform.runLater(() -> {
                root.getChildren().remove(building.getImageViews().get(0));
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                defBalloon.getImageView().setX(building.getImageView().getX()+(building.getImageView().getFitWidth()/2));
                defBalloon.getImageView().setY(building.getImageView().getY());
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
