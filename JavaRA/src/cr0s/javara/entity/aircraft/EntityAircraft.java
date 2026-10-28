package cr0s.javara.entity.aircraft;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.actor.activity.activities.Move;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.order.Order;
import cr0s.javara.util.Pos;
import cr0s.javara.util.RotationUtil;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public abstract class EntityAircraft extends MobileEntity {

    public EntityAircraft(Pos pos,
	    float aSizeWidth, float aSizeHeight) {
	super(pos, aSizeWidth, aSizeHeight);
	if (this.ordersList == null) {
	    this.ordersList = new java.util.ArrayList<>();
	}
	try {
	    this.ordersList.add(new cr0s.javara.order.MoveOrderTargeter(this));
	} catch (Throwable t) {
	    // ignore
	}
    }

    @Override
    public void resolveOrder(Order order) {
	if (order != null && order.orderString != null && order.orderString.equals("Move") && order.targetPosition != null) {
	    if (!order.isQueued) {
		try {
		    this.cancelActivity();
		} catch (Throwable t) {
		}
	    }
	    this.moveTo(order.targetPosition);
	    return;
	}
	try {
	    super.resolveOrder(order);
	} catch (Throwable t) {
	    // fallback
	    if (order != null && order.orderString != null && order.orderString.equals("Move") && order.targetPosition != null) {
		this.moveTo(order.targetPosition);
	    }
	}
    }

    @Override
    public void moveTo(Pos destCell) {
	this.moveTo(destCell, null);
    }

    @Override
    public void moveTo(Pos destCell, EntityBuilding ignoreBuilding) {
	if (destCell == null) {
	    return;
	}
	if (this.goal == null) {
	    this.goal = new Pos(destCell.getX(), destCell.getY());
	} else {
	    this.goal.setX(destCell.getX());
	    this.goal.setY(destCell.getY());
	}

	try {
	    Move move = new Move(this, destCell, getMinimumEnoughRange(), ignoreBuilding);
	    move.forceRange = true;

	    if (this.currentActivity instanceof Move) {
		this.currentActivity.cancel();
	    } else if (this.currentActivity instanceof Move.MovePart) {
		this.currentActivity.queueActivity(move);
		return;
	    }

	    this.queueActivity(move);
	} catch (Throwable t) {
	    // do nothing
	}
    }

    @Override
    public Path findPathFromTo(MobileEntity e, Pos aGoalX) {
	try {
	    Path path = new Path();
	    if (aGoalX == null && this.goal != null) {
		path.getElements().add(new MoveTo(this.goal.getX(), this.goal.getY()));
		return path;
	    }
	    path.getElements().add(new MoveTo(aGoalX.getX(), aGoalX.getY()));
	    return path;
	} catch (Throwable t) {
	    Path path = new Path();
	    return path;
	}
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
	try {
	    if (this.owner != null && this.owner.world != null && this.owner.world.getMap() != null) {
		return this.owner.world.getMap().isInMap(cellPos);
	    }
	} catch (Throwable t) {
	}
	return true;
    }

    @Override
    public int getMinimumEnoughRange() {
	return 0;
    }

    @Override
    public float getMoveSpeed() {
	return 12.0f;
    }

    @Override
    public int getWaitAverageTime() {
	return 0;
    }

    @Override
    public int getWaitSpreadTime() {
	return 0;
    }

    @Override
    public Activity moveToRange(Pos cellPos, int range) {
	try {
	    Move move = new Move(this, cellPos, range);
	    move.forceRange = true;
	    return move;
	} catch (Throwable t) {
	    return null;
	}
    }

    protected Pos flyStep(int facing) {
	if (this.getPosition() == null) {
	    return new Pos(0, 0);
	}
	return new Pos(-1, -1, this.getPosition().getZ()).rotate2D(RotationUtil.facingToAngle(facing, this.getMaxFacings())).mul(this.getMoveSpeed());
    }
    
    public boolean canLand(Pos cellPos) {
	try {
	    if (this.owner == null) {
		return false;
	    }
	    if (this.owner.world == null || this.owner.world.getMap() == null) {
		return false;
	    }
	    if (!this.owner.world.getMap().isInMap(cellPos)) {
		return false;
	    }
	    if (this.owner.world.blockingEntityMap == null) {
		return false;
	    }
	    return this.owner.world.blockingEntityMap.isAnyUnitInCell(cellPos);
	} catch (Throwable t) {
	    return false;
	}
    }
    
}
