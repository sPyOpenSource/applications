package cr0s.javara.entity.infantry;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.render.Sequence;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;

public class Barbarian extends EntityInfantry implements IHaveCost {
    public Barbarian(Pos pos) {
        super(pos);
        setHp(1200);
        setDamagePerSecond(150);
        this.texture = ResourceManager.getInstance().getInfantryTexture("e1.shp");
        this.standSequence = new Sequence(texture, 0, 8, 0, 0, null);
        Image image = standSequence.render();
        setImageView(new ImageView(image));
        getImageView().setX(pos.getX());
        getImageView().setY(pos.getY());
        getImages().add(image);
    }

    @Override
    public Path findPathFromTo(cr0s.javara.entity.MobileEntity e, Pos aGoal) {
        return null;
    }

    @Override
    public float getMoveSpeed() {
        return 30;
    }

    @Override
    public int getMinimumEnoughRange() {
        return 0;
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
        return true;
    }

    @Override
    public int getWaitAverageTime() {
        return 0;
    }

    @Override
    public int getWaitSpreadTime() {
        return 0;
    }

    @Override
    public int getRevealingRange() {
        return 0;
    }

    @Override
    public StackPane renderEntity() {
        if (getImageView() == null) {
            setImageView(new ImageView(getImages().get(0)));
        }
        StackPane combined = new StackPane();
        combined.getChildren().add(getImageView());
        return combined;
    }

    @Override
    public int getBuildingCost() {
        return 5;
    }
}