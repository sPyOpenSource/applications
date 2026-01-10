package model.building;

import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class GoldMine extends EntityBuilding {
    public GoldMine(double x, double y) {
        //super(BuildingType.REFERENCES, 1180, "assets/png/gold_mine.png", 70, 60, x, y);
        super(new Pos(x, y), 70, 60, "");
        setHp(1180);
        Image view = new Image("assets/png/gold_mine.png", true);
        setImageView(new ImageView(view));
        getImageView().setX(x);
        getImageView().setY(y);
        getImages().add(view);
    }

    @Override
    public StackPane renderEntity() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public float getHeightInTiles() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public float getWidthInTiles() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
