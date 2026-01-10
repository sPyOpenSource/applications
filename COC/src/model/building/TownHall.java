package model.building;

import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class TownHall extends EntityBuilding {
    public TownHall(double x, double y) {
        //super(BuildingType.REFERENCES, 6800, "assets/png/town.png", 120, 120, x, y);
        super(new Pos(x,y),120,120,"");
        setHp(6800);
        Image view = new Image("assets/png/town.png", true);
        setImageView(new ImageView(view));
        getImageView().setX(x);
        getImageView().setY(y);
        getImageView().setFitWidth(120);
        getImageView().setFitHeight(120);
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
