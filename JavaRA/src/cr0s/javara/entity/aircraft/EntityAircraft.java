package cr0s.javara.entity.aircraft;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.actor.activity.activities.Move;
import cr0s.javara.util.Pos;
import cr0s.javara.util.RotationUtil;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public abstract class EntityAircraft extends MobileEntity {

    public EntityAircraft(Pos pos,
	    float aSizeWidth, float aSizeHeight) {
	super(pos, aSizeWidth, aSizeHeight);
    }

    @Override
    public Path findPathFromTo(MobileEntity e, Pos aGoalX) {
	// Aircraft fly in straight lines; no grid-based pathfinding is needed.
	Path path = new Path();
	path.getElements().add(new MoveTo(e.getPosition().getX(), e.getPosition().getY()));
	path.getElements().add(new MoveTo(aGoalX.getX(), aGoalX.getY()));
	return path;
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
	return owner.world.getMap().isInMap(cellPos);
    }

    @Override
    protected Activity moveToRange(Pos cellPos, int range) {
	Move move = new Move(this, cellPos, range);
	move.forceRange = true;
	return move;
    }

    protected Pos flyStep(int facing) {
	return new Pos(-1, -1, this.getPosition().getZ()).rotate2D(RotationUtil.facingToAngle(facing, this.getMaxFacings())).mul(this.getMoveSpeed());
    }
    
    public boolean canLand(Pos cellPos) {
	if (!owner.world.getMap().isInMap(cellPos)) {
	    return false;
	}
	
	return owner.world.blockingEntityMap.isAnyUnitInCell(cellPos);
    }
    
}
