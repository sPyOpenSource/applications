package cr0s.javara.entity.aircraft;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.util.Pos;
import cr0s.javara.util.RotationUtil;
import javafx.scene.shape.Path;

public abstract class EntityAircraft extends MobileEntity {

    public EntityAircraft(Pos pos,
	    float aSizeWidth, float aSizeHeight) {
	super(pos, aSizeWidth, aSizeHeight);
    }

    @Override
    public Path findPathFromTo(MobileEntity e, Pos aGoalX) {
	return null;
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
	return this.world.getMap().isInMap(cellPos);
    }

    @Override
    protected Activity moveToRange(Pos cellPos, int range) {
	return null;
    }

    protected Pos flyStep(int facing) {
	return new Pos(-1, -1, this.getPosition().getZ()).rotate2D(RotationUtil.facingToAngle(facing, this.getMaxFacings())).mul(this.getMoveSpeed());
    }
    
    public boolean canLand(Pos cellPos) {
	if (!this.world.getMap().isInMap(cellPos)) {
	    return false;
	}
	
	return this.world.blockingEntityMap.isAnyUnitInCell(cellPos);
    }
    
}
