package controller.defense;

import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import cr0s.javara.render.map.Map;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.china.Tesla;
import controller.attack.AttackEffects;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import util.DebugLog;

public class TeslaDefense extends Thread {
    public TeslaDefense(AnchorPane root, Map map, Tesla tesla, AtomicInteger capacityInt) {
        this.root = root;
        this.map = map;
        this.tesla = tesla;
        this.capacityInt = capacityInt;
    }

    private final AnchorPane root;
    private final Map map;
    private final Tesla tesla;
    private final AtomicInteger capacityInt;
    private ImageView beamNode;

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
        if (hero != null) {
            DebugLog.trace("defense", tesla + " selected target " + hero);
        }
        if (map.getBuildingsMap().isEmpty() || tesla.getHp() <= 0) {
            tesla.setDead();
        }
        if (!tesla.isDead() && hero == null){
            try {
                wait(1000);
            } catch (InterruptedException e) {
                DebugLog.error("defense", "Interrupted in TeslaDefense", e);
            }
        }
        return hero;
    }

    private synchronized void attack(MobileEntity hero){
        while (hero.getHp() >= 0) {
            if (tesla.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
                tesla.setDead();
                break;
            }
                tesla.setDead();
                break;
            }
            double heroCx = hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX();
            double heroCy = hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY();
            double towerX = tesla.getImageView().getX() + 37;
            double towerY = tesla.getImageView().getY() + 18;
            double dist = Math.sqrt(Math.pow(towerX - heroCx, 2) + Math.pow(towerY - heroCy, 2));
            if (dist >= tesla.getRange()) {
                break;
            }
            if (!root.getChildren().contains(hero.getImageView())) {
                try {
                    wait(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            ImageView previousBeam = beamNode;
            ImageView finalCircleImpact = new ImageView("assets/gif/tesla_attack.gif");
            finalCircleImpact.setFitHeight(25);
            finalCircleImpact.setFitWidth(42.5);
            beamNode = finalCircleImpact;
            Platform.runLater(() -> {
                if (previousBeam != null) {
                    root.getChildren().remove(previousBeam);
                }
                root.getChildren().add(finalCircleImpact);
                myNotify();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                DebugLog.error("defense", "Interrupted in TeslaDefense", e);
            }

            if (root.getChildren().contains(hero.getImageView())) {
                MoveTo moveTo = new MoveTo(towerX, towerY);
                LineTo lineTo = new LineTo(
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX(),
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY());
                Path path = new Path();
                path.getElements().addAll(new MoveTo(moveTo.getX(), moveTo.getY()), lineTo);
                PathTransition pathTransition = new PathTransition();
                pathTransition.setDuration(Duration.millis(80));
                pathTransition.setPath(path);
                pathTransition.setCycleCount(1);
                pathTransition.setNode(finalCircleImpact);
                Platform.runLater(pathTransition::play);
                hero.setHp(hero.getHp() - (tesla.getDamagePerSecond() * 2));
                DebugLog.trace("defense", tesla + " hit " + hero + " (hp=" + hero.getHp() + ")");
                Platform.runLater(() -> AttackEffects.flashImpact(root,
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterX(),
                        hero.getImageView().localToScene(hero.getImageView().getLayoutBounds()).getCenterY(),
                        Color.web("#00FFFF")));
                try {
                    wait(500);
                } catch (InterruptedException e) {
                    DebugLog.error("defense", "Interrupted in TeslaDefense", e);
                }
                Platform.runLater(() -> {
                    root.getChildren().remove(finalCircleImpact);
                    myNotify();
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    DebugLog.error("defense", "Interrupted in TeslaDefense", e);
                }
            }
        }
        ImageView finalBeamNode = beamNode;
        Platform.runLater(() -> {
            if (finalBeamNode != null) {
                root.getChildren().remove(finalBeamNode);
            }
            myNotify();
        });
        try {
            wait();
        } catch (InterruptedException e) {
            DebugLog.error("defense", "Interrupted in TeslaDefense", e);
        }
    }

    public synchronized void myNotify(){
        notify();
    }
}
