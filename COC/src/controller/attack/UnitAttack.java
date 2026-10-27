package controller.attack;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import assets.Assets;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.map.Map;
import util.DebugLog;

public abstract class UnitAttack extends Thread {
    protected final Assets assets = new Assets();
    protected final AnchorPane root;
    protected final Map map;
    protected final MobileEntity unit;
    protected final ImageView sprite;

    protected UnitAttack(MobileEntity unit, AnchorPane root, Map map) {
        this.root = root;
        this.map = map;
        this.unit = unit;
        this.sprite = new ImageView(unit.getImages().get(0));
        unit.setImageView(new ImageView(sprite.getImage()));
        map.getAttackingHeroes().add(unit);
    }

    @Override
    public void run() {
        synchronized (this) {
            MediaPlayer deploy = audio(deployAudio(), 0.5);
            deploy.setAutoPlay(true);
            runOnFx(() -> {
                root.getChildren().add(new MediaView(deploy));
                root.getChildren().add(unit.getImageView());
                myNotify();
            });
            await();
            while (!unit.isDead()) {
                DebugLog.trace("combat", unit + " seeking nearest building");
                EntityBuilding building = moveToward();
                if (unit.isDead()) {
                    DebugLog.trace("combat", unit + " died while moving");
                    break;
                }
                if (building == null) {
                    DebugLog.trace("combat", unit + " found no target");
                    break;
                }
                DebugLog.trace("combat", unit + " attacking " + building);
                attack(building);
            }
            MediaPlayer die = audio(dieAudio(), 0.5);
            die.setAutoPlay(true);
            runOnFx(() -> {
                root.getChildren().remove(unit.getImageView());
                root.getChildren().add(new MediaView(die));
                myNotify();
            });
            await();
            DebugLog.trace("combat", unit + " removed from attacking heroes");
            map.getAttackingHeroes().remove(unit);
        }
    }

    protected final MediaPlayer audio(String assetPath, double volume) {
        MediaPlayer player = new MediaPlayer(new Media(assets.get(assetPath)));
        player.setVolume(volume);
        return player;
    }

    protected final void runOnFx(Runnable task) {
        Platform.runLater(task);
    }

    public final synchronized void myNotify() {
        notify();
    }

    protected final void await() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected final void await(long millis) {
        try {
            wait(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected final EntityBuilding nearestBuilding() {
        EntityBuilding nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity node : map.getBuildingsMap()) {
            if (node instanceof EntityBuilding building) {
                double width = distance(unit.getImageView().getX(), unit.getImageView().getY(),
                        building.getImageView().getX(), building.getImageView().getY());
                if (width < best) {
                    best = width;
                    nearest = building;
                }
            }
        }
        return nearest;
    }

    protected final void removeDestroyedBuilding(EntityBuilding building) {
        map.getBuildingsMap().remove(building);
        runOnFx(() -> {
            root.getChildren().remove(building.getImageView());
            myNotify();
        });
        await();
    }

    protected static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    protected static double[] nearestPointOnCircle(double cx, double cy, double r, double x, double y) {
        double distanceToCenter = distance(x, y, cx, cy);
        if (distanceToCenter < r) {
            return new double[]{x, y};
        }
        if (Math.abs(cx - x) < 1e-9) {
            double y1 = cy - r;
            double y2 = cy + r;
            return distance(x, y, cx, y1) < distance(x, y, cx, y2)
                    ? new double[]{cx, y1} : new double[]{cx, y2};
        }
        double m1 = (cy - y) / (cx - x);
        double intercept = y - m1 * x;
        double a = 1 + Math.pow(m1, 2);
        double b = -2 * cx + 2 * m1 * (intercept - cy);
        double c = Math.pow(cx, 2) + Math.pow(intercept - cy, 2) - Math.pow(r, 2);
        double sqrt = Math.sqrt(Math.pow(b, 2) - 4 * a * c);
        double x1 = (-b + sqrt) / (2 * a);
        double x2 = (-b - sqrt) / (2 * a);
        double y1 = m1 * x1 + intercept;
        double y2 = m1 * x2 + intercept;
        return distance(x, y, x1, y1) < distance(x, y, x2, y2)
                ? new double[]{x1, y1} : new double[]{x2, y2};
    }

    protected abstract EntityBuilding moveToward();

    protected abstract void attack(EntityBuilding building);

    protected abstract String deployAudio();

    protected abstract String attackAudio();

    protected abstract String dieAudio();

    protected abstract double attackRange();
}
