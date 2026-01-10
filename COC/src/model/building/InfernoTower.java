package model.building;

import cr0s.javara.entity.building.BuildingType;
import cr0s.javara.entity.building.Defensive;
import javafx.scene.layout.StackPane;

public class InfernoTower extends Defensive {
    public InfernoTower(double x, double y) {
        super(BuildingType.DEFENSIVE, 2700, "assets/png/inferno_tower.png", 90, 200, 50, 75, "", x, y);
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
