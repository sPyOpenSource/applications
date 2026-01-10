package model.building;

import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class Barrack extends EntityBuilding {
    public Barrack(double x, double y) {
        //super(BuildingType.NORMAL, 980, "assets/png/barrack.png", 70, 70, x, y);
        super(new Pos(x,y),70,70,"");
        setHp(980);
        Image view = new Image("assets/png/barrack.png", true);
        setImageView(new ImageView(view));
        getImageView().setX(x);
        getImageView().setY(y);
        getImageView().setFitWidth(70);
        getImageView().setFitHeight(70);
        getImageViews().add(view);
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
