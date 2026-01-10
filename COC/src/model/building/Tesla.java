package model.building;

import cr0s.javara.entity.building.BuildingType;
import cr0s.javara.entity.building.Defensive;
import javafx.scene.layout.StackPane;

public class Tesla extends Defensive {
    public Tesla(double x, double y) {
        super(BuildingType.DEFENSIVE, 900, "assets/png/tesla.png", 70, 55, 70, 70, "", x, y);
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
