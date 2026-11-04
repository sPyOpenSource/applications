package cr0s.javara.entity.aircraft;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.building.soviet.EntityAirField;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * A fast, cheap reconnaissance/attack drone aircraft.
 * Built from an Air Field.
 * @author Cr0s
 */
public class EntityDrone extends EntityAircraft implements IHaveCost {
    public EntityDrone(Pos x) {
        super(x, 56, 56);
        setHp(100);
        setMaxHp(100);
        setDamagePerSecond(35);

        ShpTexture tex = ResourceManager.getInstance().getConquerTexture("yak.shp");
        if (tex != null) {
            setImageView(new ImageView(SwingFXUtils.toFXImage(tex.getAsCombinedImage(null), null)));
            getImages().add(getImageView().getImage());
        }

        name = "drone";

        this.unitProductionAlingment = Alignment.SOVIET;
        this.requiredToBuild.add(EntityAirField.class);
    }

    @Override
    public float getMoveSpeed() {
        return 22;
    }

    @Override
    public int getMinimumEnoughRange() {
        return 3;
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
        return true;
    }

    @Override
    public int getWaitAverageTime() {
        return 1;
    }

    @Override
    public int getWaitSpreadTime() {
        return 1;
    }

    @Override
    public StackPane renderEntity() {
        if (getImageView() == null) {
            ShpTexture tex = ResourceManager.getInstance().getConquerTexture("yak.shp");
            if (tex != null) {
                setImageView(new ImageView(SwingFXUtils.toFXImage(tex.getAsCombinedImage(null), null)));
                getImages().add(getImageView().getImage());
            }
        }
        StackPane combined = new StackPane();
        combined.getChildren().add(getImageView());
        return combined;
    }

    @Override
    public int getBuildingCost() {
        return 25;
    }
}
