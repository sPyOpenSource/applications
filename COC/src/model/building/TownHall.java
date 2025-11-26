package model.building;

import cr0s.javara.entity.building.EntityBuilding;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class TownHall extends EntityBuilding {
    public TownHall(double x, double y) {
        //super(BuildingType.REFERENCES, 6800, "assets/png/town.png", 120, 120, x, y);
        super(x,y,120,120,"");
        setHp(6800);
        ImageView view = new ImageView(new Image("assets/png/town.png", true));
        view.setX(x);
        view.setY(y);
        view.setFitWidth(120);
        view.setFitHeight(120);
        getImageViews().add(view);
    }

    @Override
    public ImageView renderEntity() {
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
