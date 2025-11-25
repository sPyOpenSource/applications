package model.building;

import cr0s.javara.entity.building.EntityBuilding;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public abstract class Defensive extends EntityBuilding{
    public Defensive(BuildingType buildingType, int hitPoints, String path, double width, double height, int rang, int damagePerSecond, double x, double y) {
        //super(buildingType, hitPoints, path, width, height, x, y);
        super(x, y, width, height, "");
        this.range = rang;
        this.damagePerSecond = damagePerSecond;
        setHp(hitPoints);
        ImageView view = new ImageView(new Image(path, true));
        view.setX(x);
        view.setY(y);
        view.setFitWidth(width);
        view.setFitHeight(height);
        getImageViews().add(view);
    }
    private final int range;
    private final int damagePerSecond;
    public int getRange() {
        return range;
    }
    public int getDamagePerSecond() {
        return damagePerSecond;
    }
}
