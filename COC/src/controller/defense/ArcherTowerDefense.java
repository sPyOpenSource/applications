package controller.defense;

import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import cr0s.javara.render.map.Map;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.china.ArcherTower;
import controller.attack.AttackEffects;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import util.Log;

public class ArcherTowerDefense extends Thread {
    public ArcherTowerDefense(AnchorPane root, Map map, ArcherTower archerTower, AtomicInteger capacityInt) {
        this.root = root;
        this.map = map;
        this.archerTower = archerTower;
        this.capacityInt = capacityInt;
    }

    private final AnchorPane root;
    private final Map map;
    private final ArcherTower archerTower;
    private final AtomicInteger capacityInt;
    private Circle beamNode;

    @Override
    public synchronized void run() {
        while (!archerTower.isDead()) {
            MobileEntity hero = selectHero();
            if (archerTower.isDead())
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
                width = Math.sqrt((Math.pow(archerTower.getImageView().getX() + 48 - attackingHero.getImageView().localToScene(attackingHero.getImageView().getLayoutBounds()).getCenterX(), 2))
                        + Math.pow(archerTower.getImageView().getY() + 20 - attackingHero.getImageView().localToScene(attackingHero.getImageView().getLayoutBounds()).getCenterY(), 2));
                if (width < archerTower.getRange()) {
                    hero = attackingHero;
                }
            }
        }
        if (hero != null) {
            Log.trace("defense", archerTower + " selected target " + hero);
        }
        if (map.getBuildingsMap().isEmpty() || archerTower.getHp() <= 0) {
            archerTower.setDead();
        }
        if (!archerTower.isDead() && hero == null){
            try {
                wait(1000);
            } catch (InterruptedException e) {
                Log.error("defense", "Interrupted in ArcherTowerDefense", e);
            }
        }
        return hero;
    }

    private synchronized void attack(MobileEntity hero){
        while (hero.getHp() >= 0) {
            if (archerTower.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
                archerTower.setDead();
                break;
            }
            
            double heroCx = hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX();
            double heroCy = hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY();
            double towerX = archerTower.getImageView().getX() + 48;
            double towerY = archerTower.getImageView().getY() + 20;
            double dist = Math.sqrt(Math.pow(towerX - heroCx, 2) + Math.pow(towerY - heroCy, 2));
            if (dist >= archerTower.getRange()) {
                break;
            }
            if (!root.getChildren().contains(hero.getImageView())) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            Circle previousBeam = beamNode;
            Circle finalCircle = new Circle(towerX, towerY, 3);
            beamNode = finalCircle;
            Platform.runLater(() -> {
                if (previousBeam != null) {
                    root.getChildren().remove(previousBeam);
                }
                root.getChildren().add(finalCircle);
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.error("defense", "Interrupted in ArcherTowerDefense", e);
            }

            if (root.getChildren().contains(hero.getImageView())) {
                MoveTo moveTo = new MoveTo(towerX, towerY);
                LineTo lineTo = new LineTo(
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX(),
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY());
                Path path = new Path();
                path.getElements().addAll(new MoveTo(moveTo.getX(), moveTo.getY()), lineTo);
                PathTransition pathTransition = new PathTransition();
                pathTransition.setDuration(Duration.millis(1000));
                pathTransition.setPath(path);
                pathTransition.setCycleCount(1);
                pathTransition.setNode(finalCircle);
                Platform.runLater(pathTransition::play);
                hero.setHp(hero.getHp() - (archerTower.getDamagePerSecond()));
                Log.trace("defense", archerTower + " hit " + hero + " (hp=" + hero.getHp() + ")");
                Platform.runLater(() -> AttackEffects.flashImpact(root,
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX(),
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY(),
                        Color.web("#B442F7")));
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    Log.error("defense", "Interrupted in ArcherTowerDefense", e);
                }
                Platform.runLater(() -> {
                    root.getChildren().remove(finalCircle);
                    myNotify();
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.error("defense", "Interrupted in ArcherTowerDefense", e);
                }
            }
        }
        Circle finalBeamNode = beamNode;
        Platform.runLater(() -> {
            if (finalBeamNode != null) {
                root.getChildren().remove(finalBeamNode);
            }
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            Log.error("defense", "Interrupted in ArcherTowerDefense", e);
        }
    }

    public synchronized void myNotify(){
        notify();
    }
}
