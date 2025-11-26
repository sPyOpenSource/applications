package cr0s.javara.entity.building.common;

import cr0s.javara.entity.building.BibType;
import cr0s.javara.gameplay.Team.Alignment;

public class EntityConcreteWall extends EntityWall {

    private static final int BUILDING_COST = 350;

    public EntityConcreteWall(Float aTileX, Float aTileY) {
	this(aTileX, aTileY, 24, 24, "x");
    }    
    
    public EntityConcreteWall(Float aTileX, Float aTileY,
	    float aSizeWidth, float aSizeHeight,
	    String aFootprint) {
	super(aTileX, aTileY, aSizeWidth, aSizeHeight, "x");
	
	this.textureName = "brik.shp";
	loadTextures();
	
	setBibType(BibType.NONE);
	setProgressValue(-1);

	setMaxHp(10);
	setHp(getMaxHp());

	this.makeTextureName = "";
	this.unitProductionAlingment = Alignment.NEUTRAL;
	
	this.setName("brik");
    }

    @Override
    public int getBuildingCost() {
	return EntityConcreteWall.BUILDING_COST;
    }
}
