package model.building;

import cr0s.javara.entity.building.EntityBuilding;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ElixirCollector extends EntityBuilding {
    public ElixirCollector(double x, double y) {
        //super(BuildingType.REFERENCES, 1180, "assets/png/elixir_collector.png", 50, 60, x, y);
        super(x,y,50,60,"");
        setHp(1180);
        ImageView view = new ImageView(new Image("assets/png/elixir_collector.png", true));
        view.setX(x);
        view.setY(y);
        view.setFitWidth(50);
        view.setFitHeight(60);
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
