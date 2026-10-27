package controller.defense;

import javafx.application.Platform;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.china.InfernoTower;
import cr0s.javara.render.map.Map;
import controller.attack.AttackEffects;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import util.DebugLog;

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
    private Path beamNode;

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
            if (root.getChildren().contains(attackingHero.getImageView())){
                width = Math.sqrt((Math.pow(infernoTower.getImageView().getX()+26.4 - attackingHero.getImageView().localToScene(attackingHero.getImageView().getLayoutBounds()).getCenterX(), 2))
                        + Math.pow(infernoTower.getImageView().getY()+13.6 - attackingHero.getImageView().localToScene(attackingHero.getImageView().getLayoutBounds()).getCenterY(), 2));
                if (width < infernoTower.getRange()) {
                    hero = attackingHero;
                }
            }
        }
        if (hero != null) {
            DebugLog.trace("defense", infernoTower + " selected target " + hero);
        }
        if (map.getBuildingsMap().isEmpty() || infernoTower.getHp() <= 0) {
            infernoTower.setDead();
        }
        if (!infernoTower.isDead() && hero == null){
            try {
                wait(1000);
            } catch (InterruptedException e) {
                DebugLog.error("defense", "Interrupted in InfernoTowerDefense", e);
            }
        }
        return hero;
    }
    private synchronized void attack(MobileEntity hero){
        if (!root.getChildren().contains(hero.getImageView())) {
            return;
        }
        while (hero.getHp() >= 0) {
            if (infernoTower.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
                infernoTower.setDead();
                break;
            }
            double heroCx = hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX();
            double heroCy = hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY();
            double towerX = infernoTower.getImageView().getX() + 26.4;
            double towerY = infernoTower.getImageView().getY() + 13.6;
            double dist = Math.sqrt(Math.pow(towerX - heroCx, 2) + Math.pow(towerY - heroCy, 2));
            if (dist >= infernoTower.getRange()) {
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

            Path previousBeam = beamNode;
            Path path = new Path();
            path.setStroke(Color.web("#FF6F00"));
            path.setStrokeWidth(3);
            MoveTo moveTo = new MoveTo(towerX, towerY);
            LineTo lineTo = new LineTo(heroCx, heroCy);
            beamNode = path;
            Platform.runLater(() -> {
                if (previousBeam != null) {
                    root.getChildren().remove(previousBeam);
                }
                root.getChildren().add(path);
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                DebugLog.error("defense", "Interrupted in InfernoTowerDefense", e);
            }
            if (root.getChildren().contains(hero.getImageView())) {
                lineTo = new LineTo(
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX(),
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY());
                moveTo = new MoveTo(towerX, towerY);
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
                    DebugLog.error("defense", "Interrupted in InfernoTowerDefense", e);
                }
                hero.setHp(hero.getHp() - (infernoTower.getDamagePerSecond()));
                DebugLog.trace("defense", infernoTower + " hit " + hero + " (hp=" + hero.getHp() + ")");
                Platform.runLater(() -> AttackEffects.flashImpact(root,
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX(),
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY(),
                        Color.web("#FF6F00")));
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    DebugLog.error("defense", "Interrupted in InfernoTowerDefense", e);
                }
            }
        }
        Path finalBeamNode = beamNode;
        Platform.runLater(() -> {
            if (finalBeamNode != null) {
                root.getChildren().remove(finalBeamNode);
            }
            myNotify();
        });
            }
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
