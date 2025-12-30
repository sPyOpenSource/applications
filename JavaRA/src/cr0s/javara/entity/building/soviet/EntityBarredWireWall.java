package cr0s.javara.entity.building.soviet;

import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.common.EntityWall;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.util.Pos;

public class EntityBarredWireWall extends EntityWall {

    private static final int BUILDING_COST = 30;

    public EntityBarredWireWall(Pos aTile,
	    float aSizeWidth, float aSizeHeight,
	    String aFootprint) {
	super(aTile, aSizeWidth, aSizeHeight, "x");
	
	this.textureName = "fenc.shp";
	loadTextures();
	
	setBibType(BibType.NONE);
	setProgressValue(-1);

	setMaxHp(5);
	setHp(getMaxHp());

	this.makeTextureName = "";
	this.unitProductionAlingment = Alignment.NEUTRAL;
	
	this.setName("fenc");
    }

    public EntityBarredWireWall(Pos aTile) {
	this(aTile, 24, 24, "x");
    }

    @Override
    public int getBuildingCost() {
	return EntityBarredWireWall.BUILDING_COST;
    }
}
