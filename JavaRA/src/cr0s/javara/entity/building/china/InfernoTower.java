package cr0s.javara.entity.building.china;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.building.BuildingType;
import cr0s.javara.entity.building.Defensive;
import cr0s.javara.util.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class InfernoTower extends Defensive implements IHaveCost{
    public InfernoTower(Pos x) {
        super(BuildingType.DEFENSIVE, 2700, "assets/png/inferno_tower.png", 90, 200, 50, 75, "", x);
        name = "infernoTower";
    }

    @Override
    public StackPane renderEntity() {
        if(getImageView() == null){
            setImageView(new ImageView(getImages().get(0)));
        }
        StackPane combined = new StackPane();
        combined.getChildren().add(getImageView());
        return combined;
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getHeightInTiles() {
        return 1;
    }

    @Override
    public int getWidthInTiles() {
        return 1;
    }

    @Override
    public int getBuildingCost() {
        return 100;
    }
}
