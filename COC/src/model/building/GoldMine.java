package model.building;

import cr0s.javara.entity.building.EntityBuilding;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class GoldMine extends EntityBuilding {
    public GoldMine(double x, double y) {
        //super(BuildingType.REFERENCES, 1180, "assets/png/gold_mine.png", 70, 60, x, y);
        super(x,y,70,60,"");
        setHp(1180);
        ImageView view = new ImageView(new Image("assets/png/gold_mine.png", true));
        view.setX(x);
        view.setY(y);
        getImageViews().add(view);
    }

    @Override
    public void renderEntity(Scene g) {
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
