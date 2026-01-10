package cr0s.javara.render.map;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.MobileEntity;

import javafx.scene.layout.*;
import java.util.ArrayList;

public abstract class Map {
    public Map(int mapId) {
        this.mapId = mapId;
        this.attackingHeroes = new ArrayList<>();
    }
    
    private final int mapId;
    private final ArrayList<MobileEntity> attackingHeroes;

    public int getMapId() {
        return mapId;
    }
    public ArrayList<MobileEntity> getAttackingHeroes() {
        return attackingHeroes;
    }
    public abstract AnchorPane getMapView();
    public abstract ArrayList<Entity> getBuildingsMap();
}
