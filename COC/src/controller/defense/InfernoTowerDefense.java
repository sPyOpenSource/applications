package controller.defense;

import javafx.application.Platform;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.render.map.Map;
import model.building.InfernoTower;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class InfernoTowerDefense extends Thread {
    public InfernoTowerDefense(AnchorPane root, Map map, InfernoTower infernoTower, AtomicInteger capacityInt) {
        this.root = root;
        this.map = map;
        this.infernoTower = infernoTower;
        this.capacityInt = capacityInt;
    }

    private final AnchorPane root;
    private final Map map;
    private final InfernoTower infernoTower;
    private final AtomicInteger capacityInt;
    
    @Override
    public synchronized void run() {
        while (!infernoTower.isDead()) {
            MobileEntity hero = selectHero();
            if (infernoTower.isDead())
                break;
            if (hero != null)
                attack(hero);
        }
    }
    private synchronized MobileEntity selectHero(){
        double width;
        MobileEntity hero = null;
        for (MobileEntity attackingHero : new ArrayList<>(map.getAttackingHeroes())) {
            if (root.getChildren().contains(attackingHero.getViewHero())){
                width = Math.sqrt((Math.pow(infernoTower.getImageViews().get(0).getX()+26.4 - attackingHero.getViewHero().localToScene(attackingHero.getViewHero().getLayoutBounds()).getCenterX(), 2))
                        + Math.pow(infernoTower.getImageViews().get(0).getY()+13.6 - attackingHero.getViewHero().localToScene(attackingHero.getViewHero().getLayoutBounds()).getCenterY(), 2));
                if (width < infernoTower.getRange()) {
                    hero = attackingHero;
                }
            }
        }
        if (map.getBuildingsMap().isEmpty() || infernoTower.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
            infernoTower.setDead();
        }
        if (!infernoTower.isDead() && hero == null){
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return hero;
    }
    private synchronized void attack(MobileEntity hero){
        if (root.getChildren().contains(hero.getViewHero())) {
            Path path = new Path();
            path.setStroke(Color.web("#FF6F00"));
            path.setStrokeWidth(3);
            MoveTo moveTo = new MoveTo(infernoTower.getImageViews().get(0).getX() + 26.4, infernoTower.getImageViews().get(0).getY() + 13.6);
            LineTo lineTo = new LineTo(hero.getViewHero().localToScene(hero.getViewHero().getLayoutBounds()).getCenterX(), hero.getViewHero().localToScene(hero.getViewHero().getLayoutBounds()).getCenterY());
            Platform.runLater(() -> {
                root.getChildren().add(path);
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (hero.getHp() >= 0 && Math.sqrt(Math.pow(moveTo.getX() - lineTo.getX(), 2) + Math.pow(moveTo.getY() - lineTo.getY(), 2)) < infernoTower.getRange()) {
                if (infernoTower.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
                    infernoTower.setDead();
                    break;
                }
                if (root.getChildren().contains(hero.getViewHero())) {
                    lineTo = new LineTo(hero.getViewHero().localToScene(hero.getViewHero().getLayoutBounds()).getCenterX(), hero.getViewHero().localToScene(hero.getViewHero().getLayoutBounds()).getCenterY());
                    path.getElements().clear();
                    path.getElements().addAll(moveTo, lineTo);
                    Platform.runLater(() -> {
                        root.getChildren().remove(path);
                        root.getChildren().add(path);
                        myNotify();
                    });
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    hero.setHp(hero.getHp() - (infernoTower.getDamagePerSecond()));
                    try {
                        wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Platform.runLater(() -> {
                root.getChildren().remove(path);
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
