package cr0s.javara.entity.building;

import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public abstract class Defensive extends EntityBuilding{
    private final int range;
    private final int damagePerSecond;
    
    public Defensive(BuildingType buildingType, int hitPoints, String path, int rang, int damagePerSecond, int width, int height, String footprint, double x, double y) {
        //super(buildingType, hitPoints, path, width, height, x, y);
        super(new Pos(x, y), width, height, footprint);
        this.range = rang;
        this.damagePerSecond = damagePerSecond;
        setHp(hitPoints);
        if(path == null) return;
        ImageView view = new ImageView(new Image(path, true));
        view.setX(x);
        view.setY(y);
        view.setFitWidth(width);
        view.setFitHeight(height);
        getImageViews().add(view);
    }
    
    public int getRange() {
        return range;
    }
    
    public int getDamagePerSecond() {
        return damagePerSecond;
    }
}
