package cr0s.javara.entity.actor.activity.activities;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.actor.activity.activities.Turn.RotationDirection;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.EntityBlockingMap.Influence;
import cr0s.javara.util.PointsUtil;
import cr0s.javara.util.Pos;
import cr0s.javara.util.RotationUtil;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class Move extends Activity {

    private Pos destCell;
    private int destRange;
    private EntityBuilding ignoreBuilding;

    public Path currentPath;
    public int currentPathIndex;
    public boolean forceRange = false;

    private boolean hasNotifiedBlocker, hasWaited;
    private boolean isNewPath;
    private final static int REPATHING_INTERVAL_TICKS = 35;

    private int waitTicksRemaining;
    private int ticksBeforeRepath = REPATHING_INTERVAL_TICKS;
    private int randomWaitTicks;

    public Move(MobileEntity me, Pos destinationCell) {
	this.destCell = destinationCell;

	this.randomWaitTicks = me.world.getRandomInt(1, 3);

	chooseNewPath(me);
    }

    public Move(MobileEntity me, Pos destinationCell, int enoughRange) {
	this.randomWaitTicks = me.world.getRandomInt(1, 3);
	this.destRange = enoughRange;
	this.destCell = destinationCell;

	chooseNewPath(me);
    }

    public Move(MobileEntity me, Pos destinationCell, int enoughRange, EntityBuilding aIgnoreBuilding) {
	this(me, destinationCell, enoughRange);

	this.ignoreBuilding = aIgnoreBuilding;
    }

    public Move(MobileEntity me, Path scriptedPath, Pos destinationCell, EntityBuilding aIgnoreBuilding) {
	this(me, destinationCell, 0);

	this.currentPath = scriptedPath;
	this.ignoreBuilding = aIgnoreBuilding;
    }

    private Pos popPath(MobileEntity me) {
	if (this.currentPath == null || currentPathIndex >= this.currentPath.getElements().size() || this.currentPath.getElements().size() < 1) {
	    this.currentPath = null;
	    return null;
	}

	MoveTo s = (MoveTo)currentPath.getElements().get(currentPathIndex);
	double px = s.getX();
	double py = s.getY();

	Pos nextCell = new Pos(px, py);

	if (!me.canEnterCell(nextCell) && me.world.isCellBlockedByEntity(nextCell)) {
	    // This building we ignore
	    if (this.ignoreBuilding != null && me.world.getBuildingInCell(nextCell) == this.ignoreBuilding) {
		this.hasNotifiedBlocker = false;
		this.hasWaited = false;

		return null;		
	    }

	    // See if we close enough
	    double dx = destCell.getX() - nextCell.getX();
	    double dy = destCell.getY() - nextCell.getY();

	    if (dx * dx + dy * dy <= this.destRange * this.destRange) {
		//System.out.println("We're close enough. Range: " + Math.sqrt(dx * dx + dy * dy) + " <= " + this.destRange);
		this.currentPathIndex = this.currentPath.getElements().size(); // stop and skip all path
		this.currentPath = null;

		return null;
	    }

	    // Notify all friendly blockers inside cell
	    if (!this.hasNotifiedBlocker) {
		/*for (Influence i : me.world.blockingEntityMap.getCellInfluences(nextCell)) {
		    Entity blocker = i.entity;

		    if (blocker instanceof MobileEntity mobileEntity) {
			// Notify blocker 
			if (blocker != null && mobileEntity.isFrendlyTo(me)) {
			    mobileEntity.notifyBlocking(me);
			}
		    }
		}*/

		this.hasNotifiedBlocker = true;
	    }

	    // Wait a bit
	    if (!this.hasWaited) {
		this.waitTicksRemaining = me.getWaitAverageTime() + me.world.getRandomInt(-me.getWaitSpreadTime(), me.getWaitSpreadTime());

		//System.out.println("Waiting time: " + this.waitTicksRemaining);
		this.hasWaited = true;
	    }

	    if (--this.waitTicksRemaining >= 0) { // We're waiting now
		//System.out.println("\tWaiting ticks: " + this.waitTicksRemaining);
		return null;
	    }

	    // We're totally blocked, try to calculate new path
	    chooseNewPath(me);

	    return null;
	}

	if (--this.ticksBeforeRepath <= 0) {
	    this.ticksBeforeRepath = Move.REPATHING_INTERVAL_TICKS;

	    chooseNewPath(me);
	}

	this.currentPathIndex++;
	this.hasNotifiedBlocker = false;
	this.hasWaited = false;

	return nextCell;
    }

    private void chooseNewPath(MobileEntity me) {
	this.currentPath = me.findPathFromTo(me, this.destCell);
	this.currentPathIndex = 1;

	this.isNewPath = true;

	// It seems destination cell are blocked, try to choose nearest free cell as new destination cell
	if (this.currentPath == null) {
	    Pos newDestCell = chooseClosestToDestCell(me);

	    // Give up
	    if (newDestCell == null) {
		this.isNewPath = false;
		return;
	    }

	    this.destCell = newDestCell;
	    this.currentPath = me.findPathFromTo(me, destCell);
	    this.currentPathIndex = 1;
	    this.isNewPath = true;
	}
    }

    public Pos chooseClosestToDestCell(MobileEntity me) {
	return me.world.chooseClosestPassableCellInRangeAroundOtherCell(me.getCellPos(), this.destCell, this.destRange);
    }    

    @Override
    public Activity tick(EntityActor a) {
	if (isCancelled || !(a instanceof MobileEntity)) {
	    return nextActivity;
	}

	MobileEntity me = (MobileEntity) a;

	if (currentPath == null) {
	    return nextActivity;
	}

	if (randomWaitTicks > 0) {
	    randomWaitTicks--;
	    return this;
	}	

	Pos nextCell = popPath(me);

	if (nextCell == null) {
	    return this;
	}

	me.isMovingToCell = true;
	me.targetCell.setX(nextCell.getX());
	me.targetCell.setY(nextCell.getY());

	/*
	 * 1. Turn to required facing
	 * 2. Drag to required cell
	 * 3. Continue moving
	 * 
	 *         Movement system:
	 *         
	 *                     +-----<------+
	 *                     |            |
	 *                     v            ^
	 * Activity queue: MovePart --+     | (change facing and drag to target cell)
	 *                            |     ^
	 *                            v     |
	 *                           Move ->+ (if we have next cell to move)
	 */

	MovePart movePartActivity = new MovePart(this, me, me.getPos(), nextCell);
	if (this.isNewPath) {
	    movePartActivity.setIsNewPath(true);

	    this.isNewPath = false;
	}

	movePartActivity.tick(me); // Tick once to avoid tick skipping without movement when current activiy is Move

	return movePartActivity;
    }

    public void setIgnoreBuilding(EntityBuilding eb) {
	this.ignoreBuilding = eb;
    }

    public class MovePart extends Activity {
	public Move parentMove;
        
	private final MobileEntity me;
	private final Pos start;
	private final Pos end;

	private final int lengthInTicks;
	private int ticks;

	private final int desiredFacing, startFacing;
	private RotationDirection rotationDirection;
	private boolean isNewPath;

	public MovePart(Move aParentMove, MobileEntity aMe, Pos aStart, Pos aDestCell) {
	    this.parentMove = aParentMove;

	    this.me = aMe;
	    this.me.targetCell.setX(aDestCell.getX());
	    this.me.targetCell.setY(aDestCell.getY());

	    this.end = new Pos(aDestCell.getX() * 24, aDestCell.getY() * 24);
	    this.start = aStart;

	    this.lengthInTicks = (int) (20 - (10 * me.getMoveSpeed()));

	    this.desiredFacing = RotationUtil.getRotationFromXY(start.getX() + 12, start.getY() + 12, end.getX() + 12, end.getY() + 12) % Turn.MAX_FACING;
	    this.startFacing = me.currentFacing;

	    if (me.currentFacing >= 24 && desiredFacing <= 8) {
		this.rotationDirection = RotationDirection.LEFT;
	    } else if (me.currentFacing <= 8 && desiredFacing >= 24) {
		this.rotationDirection = RotationDirection.RIGHT;
	    } else {
		if (me.currentFacing < desiredFacing) {
		    this.rotationDirection = RotationDirection.LEFT;
		} else if (me.currentFacing > desiredFacing){
		    this.rotationDirection = RotationDirection.RIGHT;
		}
	    }	    
	}

	@Override
	public Activity tick(EntityActor a) {
	    Pos nextPos;

	    if (lengthInTicks > 1) { 
		nextPos = PointsUtil.interpolatePos(start, end, ticks, lengthInTicks - 1);
	    } else {
		nextPos = end;
	    }

	    if (me.currentFacing != this.desiredFacing) {
		turnFacing();

		// Don't move while our turn is not finished for new direction
		if (isNewPath) { 
		    return this;
		}
	    } else {
		this.isNewPath = false;
	    }

	    me.setPos(nextPos);

	    ticks++;
	    // If move is finished, return control to parent activity
	    if ((me.getPos().getX() == end.getX() && me.getPos().getY() == end.getY()) || ticks >= lengthInTicks) {
		me.currentFacing = this.desiredFacing % Turn.MAX_FACING; // how rough!
		me.finishMoving();

		// Parent Move activity is cancelled, lets switch to next activity (user send move order when Move/PartMove activity is working)
		if (this.nextActivity instanceof Move || this.parentMove.isCancelled() || this.isCancelled) {
		    return this.nextActivity;
		} else {
		    return parentMove;
		}
	    } else {
		return this;
	    }
	}

	public void setIsNewPath(boolean aIsNewPath) {
	    this.isNewPath = aIsNewPath;
	}

	private void turnFacing() {
	    int oldFacing = me.currentFacing;

	    int newFacing = me.currentFacing;
	    if (this.rotationDirection == RotationDirection.LEFT) {
		newFacing = (me.currentFacing + 1) % Turn.MAX_FACING;
	    } else if (this.rotationDirection == RotationDirection.RIGHT) {
		newFacing = (me.currentFacing - 1) % Turn.MAX_FACING;
	    }

	    // Turn by circle
	    if (newFacing < 0) {
		newFacing = Turn.MAX_FACING - 1;
	    }

	    me.currentFacing = newFacing % Turn.MAX_FACING;
	}
    }
}
