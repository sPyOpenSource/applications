package model.building;

import cr0s.javara.entity.building.BuildingType;
import cr0s.javara.entity.building.Defensive;
import javafx.scene.layout.StackPane;

public class ArcherTower extends Defensive {
    public ArcherTower(double x, double y) {
        super(BuildingType.DEFENSIVE, 1230, "assets/png/archerTower.png", 100, 52, 100, 100, "", x, y);
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

    @Override
    public StackPane renderEntity() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
