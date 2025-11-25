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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import cr0s.javara.render.map.Map;
import view.ImageViewClone;
import model.hero.ArcherBalloon;

import assets.Assets;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.infantry.EntityGrenadeTrooper;

public class ArcherBalloonAttack extends Thread {
    private final Assets assets = new Assets();
    
    public ArcherBalloonAttack(double x, double y, AnchorPane root, Map map) {
        this.root = root;
        this.archerBalloon = new EntityGrenadeTrooper(x, y);
        this.viewBalloon = new ImageViewClone(archerBalloon.getImageViews().get(0));
        this.map = map;
        archerBalloon.setViewHero(viewBalloon);
        this.map.getAttackingHeroes().add(archerBalloon);
        this.nearestPointLineTo = new double[2];
    }
    
    private final AnchorPane root;
    private final ImageView viewBalloon;
    private final MobileEntity archerBalloon;
    private final Map map;
    private boolean insideRadius;
    private double widthLowe;
    double[] nearestPointLineTo;
    
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
            while (!archerBalloon.isDead()){
                EntityBuilding building = moveToward();
                if (archerBalloon.isDead())
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
            map.getAttackingHeroes().remove(archerBalloon);
        }
    }
    
    private synchronized EntityBuilding moveToward(){
        widthLowe = 10000;
        double width;
        EntityBuilding building = null;
        for (Node node : map.getBuildingsMap()){
            if (node instanceof EntityBuilding){
                width = distance(viewBalloon.getX() + (viewBalloon.getFitWidth() / 2), viewBalloon.getY() + (viewBalloon.getFitHeight() / 2),
                        ((EntityBuilding) node).getImageViews().get(0).getX() + (((EntityBuilding) node).getImageViews().get(0).getFitWidth() / 2),
                        ((EntityBuilding) node).getImageViews().get(0).getY() + (((EntityBuilding) node).getImageViews().get(0).getFitHeight() / 2));
                if(width < widthLowe){
                    widthLowe = width;
                    building = (EntityBuilding) node;
                    insideRadius = true;
                }
            }
        }
        if (archerBalloon.getRang() < widthLowe && building != null){
            for (Node node : map.getBuildingsMap()){
                if (node instanceof EntityBuilding){
                    double[] nearestPoint = nearestPointOnCircle(((EntityBuilding) node).getImageViews().get(0).getX() + (((EntityBuilding) node).getImageViews().get(0).getFitWidth() / 2),
                            ((EntityBuilding) node).getImageViews().get(0).getY() + (((EntityBuilding) node).getImageViews().get(0).getFitHeight() / 2),
                            archerBalloon.getRang(), viewBalloon.getX() + (viewBalloon.getFitWidth() / 2),
                            viewBalloon.getY() + (viewBalloon.getFitHeight() / 2));
                    width = distance(viewBalloon.getX() + (viewBalloon.getFitWidth() / 2), viewBalloon.getY() + (viewBalloon.getFitHeight() / 2),
                            nearestPoint[0], nearestPoint[1]);
                    if (width < widthLowe) {
                        widthLowe = width;
                        building = (EntityBuilding) node;
                        nearestPointLineTo = nearestPoint;
                        insideRadius = false;
                    }
                }
            }
            MoveTo moveTo = new MoveTo(viewBalloon.getX(), viewBalloon.getY());
            LineTo lineTo = new LineTo(nearestPointLineTo[0], nearestPointLineTo[1]);
            Path path = new Path();
            path.getElements().addAll(moveTo, lineTo);
            PathTransition transition = new PathTransition();
            transition.setDuration(Duration.millis((widthLowe / archerBalloon.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(viewBalloon);
            transition.setAutoReverse(false);
            transition.setPath(path);
            Platform.runLater(transition::play);
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            while (elapsedTime < (long) ((widthLowe / archerBalloon.getMoveSpeed()) * 300)) {
                if (archerBalloon.getHp() <= 0){
                    archerBalloon.setDead();
                    break;
                }
                try {
                    wait(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                elapsedTime = System.currentTimeMillis() - startTime;
            }
        }
        else if (building == null){
            archerBalloon.setDead();
        }
        return building;
    }
    
    private synchronized void attack(EntityBuilding building){
        if (!insideRadius){
            Platform.runLater(() -> {
                viewBalloon.setX(nearestPointLineTo[0]);
                viewBalloon.setY(nearestPointLineTo[1]);
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Path path = new Path();
        MoveTo moveTo = new MoveTo(viewBalloon.getX(), viewBalloon.getY()+20);
        LineTo lineTo = new LineTo(building.getImageViews().get(0).getX()+(building.getImageViews().get(0).getFitWidth()/2),
                building.getImageViews().get(0).getY()+20);
        Circle circle = new Circle(viewBalloon.getX()+28.8, viewBalloon.getY()+60,3);
        circle.setFill(Color.web("#B442F7"));
        path.getElements().addAll(moveTo, lineTo);
        Platform.runLater(() -> {
            root.getChildren().add(circle);
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (building.getHp() >= 0){
            if (archerBalloon.getHp() <= 0){
                archerBalloon.setDead();
                break;
            }
            PathTransition pathTransition = new PathTransition();
            pathTransition.setDuration(Duration.millis(1000));
            pathTransition.setPath(path);
            pathTransition.setCycleCount(1);
            pathTransition.setNode(circle);
            Platform.runLater(pathTransition::play);
            building.setHp(building.getHp() - archerBalloon.getDamagePerSecond());
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Platform.runLater(() -> {
            root.getChildren().remove(circle);
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!archerBalloon.isDead()){
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
        }
    }

    public synchronized void myNotify(){
        notify();
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static double[] nearestPointOnCircle(double cx, double cy, double r, double x, double y) {
        double distanceToCenter = distance(x, y, cx, cy);
        if (distanceToCenter < r) {
            return new double[]{x, y};
        } else {
            double m1 = (cy - y) / (cx - x);

            double intercept = y - m1 * x;

            double a = 1 + Math.pow(m1, 2);
            double b = -2 * cx + 2 * m1 * (intercept - cy);
            double c = Math.pow(cx, 2) + Math.pow(intercept - cy, 2) - Math.pow(r, 2);

            final double sqrt = Math.sqrt(Math.pow(b, 2) - 4 * a * c);
            double x1 = (-b + sqrt) / (2 * a);
            double x2 = (-b - sqrt) / (2 * a);

            double y1 = m1 * x1 + intercept;
            double y2 = m1 * x2 + intercept;

            double distance1 = distance(x, y, x1, y1);
            double distance2 = distance(x, y, x2, y2);

            if (distance1 < distance2) {
                return new double[]{x1, y1};
            } else {
                return new double[]{x2, y2};
            }
        }
    }
}
