package cr0s.javara.entity.building.common;

import cr0s.javara.entity.building.BibType;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.util.Pos;

public class EntityConcreteWall extends EntityWall {

    private static final int BUILDING_COST = 350;

    public EntityConcreteWall(Pos aTile) {
	this(aTile, 24, 24, "x");
    }    
    
    public EntityConcreteWall(Pos aTile,
	    int aSizeWidth, int aSizeHeight,
	    String aFootprint) {
	super(aTile, aSizeWidth, aSizeHeight, "x");
	
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
