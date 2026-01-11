package model.building;

import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ElixirCollector extends EntityBuilding {
    public ElixirCollector(Pos x) {
        //super(BuildingType.REFERENCES, 1180, "assets/png/elixir_collector.png", 50, 60, x, y);
        super(x,50,60,"");
        setHp(1180);
        Image view = new Image("assets/png/elixir_collector.png", true);
        setImageView(new ImageView(view));
        getImageView().setX(x.getX());
        getImageView().setY(x.getY());
        getImageView().setFitWidth(50);
        getImageView().setFitHeight(60);
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
