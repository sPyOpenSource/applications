package model.building;

import cr0s.javara.entity.building.BuildingType;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public abstract class Building extends Node {
    public Building(BuildingType buildingType, int hitPoints, String path, double width, double height, double x, double y) {
        this.buildingType = buildingType;
        this.hitPoints = hitPoints;
        this.imageView = new ImageView(new Image(path, true));
        this.imageView.setFitWidth(width);
        this.imageView.setFitHeight(height);
        this.imageView.setX(x);
        this.imageView.setY(y);
    }

    private final BuildingType buildingType;
    private int hitPoints;

    private final ImageView imageView;
    
    public BuildingType getBuildingType() {
        return buildingType;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public void setHitPoints(int hitPoints) {
        this.hitPoints = hitPoints;
    }
    
    public ImageView getImageView() {
        return imageView;
    }
}