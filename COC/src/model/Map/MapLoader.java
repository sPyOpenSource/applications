package model.Map;

import javafx.scene.image.Image;
import javafx.scene.layout.*;

import java.io.InputStream;
import java.util.ArrayList;
import assets.Assets;
import model.building.*;
import org.yaml.snakeyaml.Yaml;

import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.common.EntityConstructionYard;
import cr0s.javara.render.map.Map;
import cr0s.javara.util.Pos;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.building.soviet.EntityTeslaCoil;

public class MapLoader extends Map {
    private final ArrayList<Entity> buildingsMap;
    private final Assets assets = new Assets();
    
    public MapLoader(int id, String map) {
        super(id);
        buildingsMap = new ArrayList<>();
        this.buildMap(map);
    }
    
    @Override
    public ArrayList<Entity> getBuildingsMap() {
        return buildingsMap;
    }
    
    @Override
    public AnchorPane getMapView() {
        AnchorPane root = new AnchorPane();
        root.setBackground(new Background(new BackgroundImage(new Image("assets/png/classic12.png"), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
        for (Entity node : buildingsMap) {
            if (node instanceof EntityBuilding building){
                root.getChildren().add(building.getTexture());
            }
        }
        return root;
    }
    
    private void buildMap(String map){
        InputStream input = assets.getInputStream("/assets/maps/" + map + ".yaml");
        Yaml mapYaml = new Yaml();
	java.util.Map<String, Object> mapYamlMap = (java.util.Map) mapYaml.load(input);
        java.util.Map<String, Object> entitiesMap = (java.util.Map) mapYamlMap.get("Actors");
        for(Object v : entitiesMap.values()){
            java.util.Map<String, Object> actor = (java.util.Map) v;
            String id = (String) actor.get("Name");
            int x = (Integer) actor.get("LocationX");
            int y = (Integer) actor.get("LocationY");
            switch(id){
                case "archerTower":
                    ArcherTower archerTower = new ArcherTower(x, y);
                    buildingsMap.add(archerTower);
                    break;
                case "townHall":
                    //TownHall townHall = new TownHall(x, y);
                    EntityConstructionYard townHall = new EntityConstructionYard(new Pos((double)x, (double)y));
                    buildingsMap.add(townHall);
                    break;
                case "tesla":
                    EntityTeslaCoil tesla = new EntityTeslaCoil(new Pos((double)x, (double)y));
                    buildingsMap.add(tesla);
                    break;
                case "infernoTower":
                    InfernoTower infernoTower = new InfernoTower(x, y);
                    buildingsMap.add(infernoTower);
                    break;
                case "elixirCollector":
                    ElixirCollector elixirCollector = new ElixirCollector(x, y);
                    buildingsMap.add(elixirCollector);
                    break;
                case "goldMine":
                    GoldMine goldMine = new GoldMine(x, y);
                    buildingsMap.add(goldMine);
                    break;
                case "barrack":
                    Barrack barrack = new Barrack(x, y);
                    buildingsMap.add(barrack);
                    break;
                default:
                    System.out.println(id);
            }
        }
    }
}
