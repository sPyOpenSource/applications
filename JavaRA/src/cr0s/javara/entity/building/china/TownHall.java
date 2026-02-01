package cr0s.javara.entity.building.china;

import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class TownHall extends EntityBuilding {
    public TownHall(Pos x) {
        //super(BuildingType.REFERENCES, 6800, "assets/png/town.png", 120, 120, x, y);
        super(x, 120, 120, "-xxx- xxxxx xxxxx xxxxx -xxx-");
        setHp(6800);
        Image view = new Image("assets/png/town.png", true);
        setImageView(new ImageView(view));
        getImageView().setX(x.getX());
        getImageView().setY(x.getY());
        getImageView().setFitWidth(120);
        getImageView().setFitHeight(120);
        getImages().add(view);
        name = "townHall";
    }

    @Override
    public StackPane renderEntity() {
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
        return 2;
    }

    @Override
    public int getWidthInTiles() {
        return 2;
    }
}
