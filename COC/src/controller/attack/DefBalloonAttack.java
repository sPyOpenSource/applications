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

import cr0s.javara.render.map.Map;
import view.ImageViewClone;
import model.hero.DefBalloon;

import assets.Assets;
import cr0s.javara.entity.building.EntityBuilding;

public class DefBalloonAttack extends Thread {
    private final Assets assets = new Assets();
    
    public DefBalloonAttack(double x, double y, AnchorPane root, Map map) {
        this.root = root;
        this.defBalloon = new DefBalloon(x,y);
        this.viewBalloonMove = new ImageViewClone(defBalloon.getImageViews().get(0));
        this.viewBalloonAttack = new ImageViewClone(defBalloon.getImageViews().get(1));
        this.viewBalloon = new ImageViewClone(viewBalloonMove);
        this.map = map;
        defBalloon.setViewHero(viewBalloon);
        this.map.getAttackingHeroes().add(defBalloon);
        this.isDied = false;
    }
    
    private final AnchorPane root;
    private final ImageView viewBalloonAttack;
    private final ImageView viewBalloonMove;
    private final ImageView viewBalloon;
    private final DefBalloon defBalloon;
    private final Map map;
    private boolean isDied;

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
            });
            while (!isDied){
                EntityBuilding building = moveToward();
                if (isDied)
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
            map.getAttackingHeroes().remove(defBalloon);
        }
    }
    
    private synchronized EntityBuilding moveToward(){
        double widthLowe = 10000;
        double width;
        EntityBuilding building = null;
        for (Node node : map.getBuildingsMap()){
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
            for (Node node : map.getBuildingsMap()){
                if (node instanceof EntityBuilding){
                    width = Math.sqrt((Math.pow(viewBalloon.getX()-((EntityBuilding) node).getImageViews().get(0).getX(),2))+Math.pow(viewBalloon.getY()-((EntityBuilding) node).getImageViews().get(0).getY(),2));
                    if(width < widthLowe){
                        widthLowe = width;
                        building = (EntityBuilding) node;
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
            transition.setDuration(Duration.millis((widthLowe / defBalloon.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(viewBalloon);
            transition.setAutoReverse(false);
            transition.setPath(path);
            Platform.runLater(transition::play);
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            while (elapsedTime < (long) ((widthLowe / defBalloon.getMoveSpeed()) * 300)) {
                if (defBalloon.getHp() <= 0){
                    isDied = true;
                    Platform.runLater(() -> root.getChildren().remove(viewBalloon));
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
            isDied = true;
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
            if (defBalloon.getHp() <= 0){
                isDied = true;
                Platform.runLater(() -> root.getChildren().remove(viewBalloon));
                break;
            }
            building.setHp(building.getHp() - defBalloon.getDamagePerSecond());
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!isDied){
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
