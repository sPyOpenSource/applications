package controller.attack;

import javafx.animation.PathTransition;
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

import view.ImageViewClone;
import model.hero.GoblinBalloon;
import assets.Assets;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.map.Map;

public class GoblinBalloonAttack extends Thread {
    private final Assets assets = new Assets();
    
    public GoblinBalloonAttack(double x, double y, AnchorPane root, Map map) {
        this.root = root;
        this.goblinBalloon = new GoblinBalloon(x,y);
        this.viewBalloonMove = new ImageViewClone(goblinBalloon.getImageViews().get(0));
        this.viewBalloonAttack = new ImageViewClone(goblinBalloon.getImageViews().get(1));
        this.viewBalloon = new ImageViewClone(viewBalloonMove);
        this.map = map;
        goblinBalloon.setViewHero(viewBalloon);
        this.map.getAttackingHeroes().add(goblinBalloon);
    }
    
    private final AnchorPane root;
    private final ImageView viewBalloonAttack;
    private final ImageView viewBalloonMove;
    private final ImageView viewBalloon;
    private final GoblinBalloon goblinBalloon;
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
                root.getChildren().add(viewBalloon);
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!goblinBalloon.isDead()){
                EntityBuilding building = moveToward();
                if (goblinBalloon.isDead())
                    break;
                attack(building);
            }
            String audioFilePathDie = assets.get("/assets/audio/balloonDie.mp3");
            MediaPlayer mediaPlayerDie = new MediaPlayer(new Media(audioFilePathDie));
            mediaPlayerDie.setAutoPlay(true);
            mediaPlayerDie.setVolume(0.5);
            Platform.runLater(() -> {
                root.getChildren().remove(viewBalloon);
                root.getChildren().add(new MediaView(mediaPlayerDie));
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            map.getAttackingHeroes().remove(goblinBalloon);
        }
    }
    
    private synchronized EntityBuilding moveToward(){
        double widthLowe = 10000;
        double width;
        EntityBuilding building = null;
        for (Node node : map.getBuildingsMap()){
            if (node instanceof EntityBuilding building1){
                /*if (building1.getBuildingType().equals(goblinBalloon.getFavoriteTarget())){
                    width = Math.sqrt((Math.pow(viewBalloon.getX()-building1.getImageView().getX(),2))+Math.pow(viewBalloon.getY()-building1.getImageView().getY(),2));
                    if(width < widthLowe){
                        widthLowe = width;
                        building = building1;
                    }
                }*/
            }
        }
        if (building == null) {
            for (Node node : map.getBuildingsMap()){
                if (node instanceof EntityBuilding building1){
                    width = Math.sqrt((Math.pow(viewBalloon.getX()-building1.getImageViews().get(0).getX(),2))+Math.pow(viewBalloon.getY()-building1.getImageViews().get(0).getY(),2));
                    if(width < widthLowe){
                        widthLowe = width;
                        building = building1;
                    }
                }
            }
        }

        if (building != null){
            Platform.runLater(() -> {
                viewBalloon.setImage(viewBalloonMove.getImage());
                viewBalloon.setFitWidth(viewBalloonMove.getFitWidth());
                viewBalloon.setFitHeight(viewBalloonMove.getFitHeight());
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            MoveTo moveTo = new MoveTo(viewBalloon.getX(), viewBalloon.getY());
            LineTo lineTo = new LineTo(building.getImageViews().get(0).getX()+(building.getImageViews().get(0).getFitWidth()/2),building.getImageViews().get(0).getY());
            Path path = new Path();
            path.getElements().addAll(moveTo, lineTo);
            PathTransition transition = new PathTransition();
            transition.setDuration(Duration.millis((widthLowe / goblinBalloon.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(viewBalloon);
            transition.setAutoReverse(false);
            transition.setPath(path);
            Platform.runLater(transition::play);
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            while (elapsedTime < (long) ((widthLowe / goblinBalloon.getMoveSpeed()) * 300)) {
                if (goblinBalloon.getHp() <= 0){
                    goblinBalloon.setDead();
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
            goblinBalloon.setDead();
        }
        return building;
    }
    
    private synchronized void attack(EntityBuilding building){
        Platform.runLater(() -> {
            viewBalloon.setImage(viewBalloonAttack.getImage());
            viewBalloon.setFitWidth(viewBalloonAttack.getFitWidth());
            viewBalloon.setFitHeight(viewBalloonAttack.getFitHeight());
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (building.getHp() >= 0){
            if (goblinBalloon.getHp() <= 0){
                goblinBalloon.setDead();
                break;
            }
            //if (building.getBuildingType().equals(goblinBalloon.getFavoriteTarget())){
              //  building.setHitPoints(building.getHitPoints() - (goblinBalloon.getDamagePerSecond() * 5));
            //} else {
                building.setHp(building.getHp() - goblinBalloon.getDamagePerSecond());
            //}
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!goblinBalloon.isDead()){
            map.getBuildingsMap().remove(building);
            Platform.runLater(() -> {
                root.getChildren().remove(building.getImageViews());
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                viewBalloon.setX(building.getImageViews().get(0).getX()+(building.getImageViews().get(0).getFitWidth()/2));
                viewBalloon.setY(building.getImageViews().get(0).getY());
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
