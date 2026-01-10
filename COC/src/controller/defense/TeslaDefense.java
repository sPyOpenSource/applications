package controller.defense;

import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import cr0s.javara.render.map.Map;
import cr0s.javara.entity.MobileEntity;
import model.building.Tesla;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TeslaDefense extends Thread {
    public TeslaDefense(AnchorPane root, Map map, Tesla tesla, AtomicInteger capacityInt) {
        this.root = root;
        this.map = map;
        this.tesla = tesla;
        this.capacityInt = capacityInt;
        circleImpact = new ImageView("assets/gif/tesla_attack.gif");
        circleImpact.setFitHeight(25);
        circleImpact.setFitWidth(42.5);
    }

    private final AnchorPane root;
    private final Map map;
    private final Tesla tesla;
    private final AtomicInteger capacityInt;
    private final ImageView circleImpact;
     
    @Override
    public synchronized void run() {
        while (!tesla.isDead()) {
            MobileEntity hero = selectHero();
            if (tesla.isDead())
                break;
            if (hero != null)
                attack(hero);
        }
    }
    
    private synchronized MobileEntity selectHero(){
        double width;
        MobileEntity hero = null;
        for (MobileEntity attackingHero : new ArrayList<>(map.getAttackingHeroes())) {
            if (root.getChildren().contains(attackingHero.getImageView())){
                width = Math.sqrt((Math.pow(tesla.getImageView().getX() + 37 - attackingHero.getImageView().localToScene(attackingHero.getImageView().getLayoutBounds()).getCenterX(), 2))
                        + Math.pow(tesla.getImageView().getY() + 18 - attackingHero.getImageView().localToScene(attackingHero.getImageView().getLayoutBounds()).getCenterY(), 2));
                if (width < tesla.getRange()) {
                    hero = attackingHero;
                }
            }
        }
        if (map.getBuildingsMap().isEmpty() || tesla.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
            tesla.setDead();
        }
        if (!tesla.isDead() && hero == null){
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return hero;
    }
    
    private synchronized void attack(MobileEntity hero){
        MoveTo moveTo = new MoveTo(tesla.getImageView().getX() + 37, tesla.getImageView().getY() + 18);
        LineTo lineTo = new LineTo(hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX(),hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY());
        while (hero.getHp() >= 0 && Math.sqrt(Math.pow(moveTo.getX()-lineTo.getX(), 2)+ Math.pow(moveTo.getY()-lineTo.getY(), 2)) < tesla.getRange()){
            if (tesla.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
                tesla.setDead();
                break;
            }
            ImageView finalCircleImpact = circleImpact;
            Platform.runLater(() -> {
                root.getChildren().add(finalCircleImpact);
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (root.getChildren().contains(hero.getImageView())) {
                lineTo = new LineTo(hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX(), hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY());
                Path path = new Path();
                path.getElements().addAll(new MoveTo(moveTo.getX(), moveTo.getY()), lineTo);
                PathTransition pathTransition = new PathTransition();
                pathTransition.setDuration(Duration.millis(80));
                pathTransition.setPath(path);
                pathTransition.setCycleCount(1);
                pathTransition.setNode(finalCircleImpact);
                Platform.runLater(pathTransition::play);
                hero.setHp(hero.getHp() - (tesla.getDamagePerSecond() * 2));
                try {
                    wait(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    root.getChildren().remove(finalCircleImpact);
                    myNotify();
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ImageView finalCircleImpact1 = circleImpact;
        Platform.runLater(() -> {
            root.getChildren().remove(finalCircleImpact1);
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public synchronized void myNotify(){
        notify();
    }
}
