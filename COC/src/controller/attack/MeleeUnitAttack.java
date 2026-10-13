package controller.attack;

import javafx.animation.PathTransition;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.map.Map;

public abstract class MeleeUnitAttack extends UnitAttack {
    protected MeleeUnitAttack(MobileEntity unit, AnchorPane root, Map map) {
        super(unit, root, map);
    }

    @Override
    protected double attackRange() {
        return 0;
    }

    @Override
    protected EntityBuilding moveToward() {
        EntityBuilding building = nearestBuilding();
        if (building == null) {
            unit.setDead();
            return null;
        }
        MoveTo moveTo = new MoveTo(unit.getImageView().getX(), unit.getImageView().getY());
        LineTo lineTo = new LineTo(building.getImageView().getX(),
                building.getImageView().getY() + (building.getImageView().getFitHeight() / 3));
        Path path = new Path();
        path.getElements().addAll(moveTo, lineTo);
        PathTransition transition = new PathTransition();
        double width = distance(unit.getImageView().getX(), unit.getImageView().getY(),
                building.getImageView().getX(), building.getImageView().getY());
        transition.setDuration(Duration.millis((width / unit.getMoveSpeed()) * 400));
        transition.setCycleCount(1);
        transition.setNode(unit.getImageView());
        transition.setAutoReverse(false);
        transition.setPath(path);
        runOnFx(transition::play);
        long start = System.currentTimeMillis();
        long elapsed = 0;
        while (elapsed < (long) ((width / unit.getMoveSpeed()) * 300)) {
            if (unit.getHp() <= 0) {
                unit.setDead();
                break;
            }
            await(300);
            elapsed = System.currentTimeMillis() - start;
        }
        return building;
    }

    @Override
    protected void attack(EntityBuilding building) {
        MediaPlayer player = audio(attackAudio(), 0.4);
        while (building.getHp() >= 0) {
            if (unit.getHp() <= 0) {
                unit.setDead();
                break;
            }
            runOnFx(player::play);
            building.setHp((int) (building.getHp() - (unit.getDamagePerSecond() * 1.25)));
            await(1250);
            runOnFx(player::stop);
        }
        if (!unit.isDead()) {
            removeDestroyedBuilding(building);
        }
    }
}
