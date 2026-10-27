package controller.attack;

import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.map.Map;
import javafx.scene.layout.AnchorPane;

public abstract class RangedUnitAttack extends UnitAttack {
    private boolean insideRadius;
    private double widthLowe;
    private double[] nearestPointLineTo;

    protected RangedUnitAttack(MobileEntity unit, AnchorPane root, Map map) {
        super(unit, root, map);
        this.nearestPointLineTo = new double[2];
    }

    private double centerX() {
        return unit.getImageView().getX() + (unit.getImageView().getFitWidth() / 2);
    }

    private double centerY() {
        return unit.getImageView().getY() + (unit.getImageView().getFitHeight() / 2);
    }

    @Override
    protected EntityBuilding moveToward() {
        widthLowe = Double.MAX_VALUE;
        EntityBuilding building = null;
        for (Entity node : map.getBuildingsMap()) {
            if (node instanceof EntityBuilding entityBuilding) {
                double width = distance(centerX(), centerY(),
                        entityBuilding.getImageView().getX() + (entityBuilding.getImageView().getFitWidth() / 2),
                        entityBuilding.getImageView().getY() + (entityBuilding.getImageView().getFitHeight() / 2));
                if (width < widthLowe) {
                    widthLowe = width;
                    building = entityBuilding;
                    insideRadius = true;
                }
            }
        }
        if (attackRange() < widthLowe && building != null) {
            for (Entity node : map.getBuildingsMap()) {
                if (node instanceof EntityBuilding entityBuilding) {
                    double[] nearestPoint = nearestPointOnCircle(
                            entityBuilding.getImageView().getX() + (entityBuilding.getImageView().getFitWidth() / 2),
                            entityBuilding.getImageView().getY() + (entityBuilding.getImageView().getFitHeight() / 2),
                            attackRange(), centerX(), centerY());
                    double width = distance(centerX(), centerY(), nearestPoint[0], nearestPoint[1]);
                    if (width < widthLowe) {
                        widthLowe = width;
                        building = entityBuilding;
                        nearestPointLineTo = nearestPoint;
                        insideRadius = false;
                    }
                }
            }
            MoveTo moveTo = new MoveTo(unit.getImageView().getX(), unit.getImageView().getY());
            LineTo lineTo = new LineTo(nearestPointLineTo[0], nearestPointLineTo[1]);
            Path path = new Path();
            path.getElements().addAll(moveTo, lineTo);
            PathTransition transition = new PathTransition();
            transition.setDuration(Duration.millis((widthLowe / unit.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(unit.getImageView());
            transition.setAutoReverse(false);
            transition.setPath(path);
            runOnFx(transition::play);
            long start = System.currentTimeMillis();
            long elapsed = 0;
            while (elapsed < (long) ((widthLowe / unit.getMoveSpeed()) * 400)) {
                if (unit.getHp() <= 0) {
                    unit.setDead();
                    break;
                }
                await(300);
                elapsed = System.currentTimeMillis() - start;
            }
        } else if (building == null) {
            unit.setDead();
        }
        return building;
    }

    @Override
    protected void attack(EntityBuilding building) {
        if (!insideRadius) {
            runOnFx(() -> {
                unit.getImageView().setX(nearestPointLineTo[0]);
                unit.getImageView().setY(nearestPointLineTo[1]);
                myNotify();
            });
            await();
        }
        Path path = new Path();
        MoveTo moveTo = new MoveTo(unit.getImageView().getX(), unit.getImageView().getY() + 20);
        LineTo lineTo = new LineTo(building.getImageView().getX() + (building.getImageView().getFitWidth() / 2),
                building.getImageView().getY() + 20);
        path.getElements().addAll(moveTo, lineTo);

        while (building.getHp() >= 0) {
            if (unit.getHp() <= 0) {
                unit.setDead();
                break;
            }
            Circle circle = new Circle(unit.getImageView().getX() + 28.8, unit.getImageView().getY() + 60, 3);
            circle.setFill(Color.web("#00BFFF"));
            runOnFx(() -> {
                root.getChildren().add(circle);
                myNotify();
            });
            await();

            PathTransition pathTransition = new PathTransition();
            pathTransition.setDuration(Duration.millis(1000));
            pathTransition.setPath(path);
            pathTransition.setCycleCount(1);
            pathTransition.setNode(circle);
            pathTransition.setOnFinished(e -> {
                root.getChildren().remove(circle);
                AttackEffects.flashImpact(root,
                        building.getImageView().getX() + building.getImageView().getFitWidth() / 2,
                        building.getImageView().getY() + building.getImageView().getFitHeight() / 2,
                        Color.web("#00BFFF"));
            });
            runOnFx(pathTransition::play);
            building.setHp(building.getHp() - unit.getDamagePerSecond());
            Platform.runLater(() -> AttackEffects.pulse(building.getImageView()));
            await(1000);
        }
        if (!unit.isDead()) {
            map.getBuildingsMap().remove(building);
            runOnFx(() -> {
                root.getChildren().remove(building.getImageView());
                myNotify();
            });
            await();
        }
    }
}
