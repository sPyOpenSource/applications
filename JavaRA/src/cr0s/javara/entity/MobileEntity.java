package cr0s.javara.entity;

import java.util.ArrayList;
import java.util.Collections;

import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.actor.activity.activities.Follow;
import cr0s.javara.entity.actor.activity.activities.Move;
import cr0s.javara.entity.actor.activity.activities.MoveInfantry;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.MoveOrderTargeter;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.util.Pos;
import cr0s.javara.main.GUI;

import javafx.scene.Scene;
import javafx.scene.shape.Path;

public abstract class MobileEntity extends EntityActor implements INotifyBlockingMove {
    protected float moveSpeed = 0.1f;
    private int damagePerSecond;

    public int targetCellX, targetCellY;
    public boolean isMovingToCell;   
    
    public int goalX, goalY;
    public SubCell currentSubcell;
    public SubCell desiredSubcell;
    
    public MobileEntity(double posX, double posY,
	    float aSizeWidth, float aSizeHeight) {
	super(posX, posY, aSizeWidth, aSizeHeight);
	
	ordersList = new ArrayList<>();
	ordersList.add(new MoveOrderTargeter(this));
    }

    public abstract Path findPathFromTo(MobileEntity e, int aGoalX, int aGoalY);

    public Pos getCenterPos() {
	return new Pos(this.getCenterPosX(), this.getCenterPosY());
    }
    
    public Pos getPos() {
	return new Pos(this.boundingBox.getX(), this.boundingBox.getY());
    }
    
    public Pos getCellPos() {
	return new Pos((int) getCenterPosX() / 24, (int) getCenterPosY() / 24);
    }    
    
    public Pos getTexturePos() {
	return new Pos(this.boundingBox.getX(), this.boundingBox.getY());
    }

    public void setPos(Pos pos) {
	getImageView().setX(pos.getX());
	getImageView().setY(pos.getY());
    }
    
    public int getDamagePerSecond() {
        return damagePerSecond;
    }
    
    public void setDamagePerSecond(int damagePerSecond){
        this.damagePerSecond = damagePerSecond;
    }
    
    public void setCenterPos(Pos pos) {
	this.setCenterX(pos.getX());
	this.setCenterY(pos.getY());
    }
    
    public int getRang(){
        return 5;
    }
    
    protected void drawPath(Scene g) {
	if (!GUI.DEBUG_MODE) {
	    return;
	}

	if (this.currentActivity != null) {
	    Path currentPath = null;
	    int pathIndex = 0;
	    
	    // For vehicles
	    if ((this.currentActivity instanceof Move) && ((Move) this.currentActivity).currentPath != null) {
		currentPath = ((Move) this.currentActivity).currentPath;
		pathIndex = ((Move) currentActivity).currentPathIndex;
	    } else if ((this.currentActivity instanceof Move.MovePart) && ((Move.MovePart) this.currentActivity).parentMove.currentPath != null) {
		currentPath = ((Move.MovePart) this.currentActivity).parentMove.currentPath;
		pathIndex = ((Move.MovePart) this.currentActivity).parentMove.currentPathIndex;	
	    }
	    
	    // For infantry
	    if ((this.currentActivity instanceof MoveInfantry) && ((MoveInfantry) this.currentActivity).currentPath != null) {
		currentPath = ((MoveInfantry) this.currentActivity).currentPath;
		pathIndex = ((MoveInfantry) currentActivity).currentPathIndex;
	    } else if ((this.currentActivity instanceof MoveInfantry.MovePart) && ((MoveInfantry.MovePart) this.currentActivity).parentMove.currentPath != null) {
		currentPath = ((MoveInfantry.MovePart) this.currentActivity).parentMove.currentPath;
		pathIndex = ((MoveInfantry.MovePart) this.currentActivity).parentMove.currentPathIndex;	
	    }
	    
	    
	    if (currentPath == null) {
		return;
	    }
	    
	    //g.setColor(Color.GREEN);
	    //g.setLineWidth(1);
	    
	    if (pathIndex == currentPath.getElements().size()) {
		return;
	    }
	    
	    //g.drawLine(this.getCenterPosX(), this.getCenterPosY(), currentPath.getStep(pathIndex - 1).getX() * 24 + 12, currentPath.getStep(pathIndex - 1).getY() * 24 + 12);
	    
	    //g.fillOval(this.goalX * 24 + 12 - 2, this.goalY * 24 + 12 - 2, 5, 5);

	    for (int i = pathIndex - 1; i < currentPath.getElements().size() - 1; i++) {
		//Step from = currentPath.getStep(i);
		//Step to = currentPath.getStep(i + 1);

		//g.fillOval(from.getX() * 24 + 12 - 2, from.getY() * 24 + 12 - 2, 5, 5);
		//g.fillOval(to.getX() * 24 + 12 - 2, to.getY() * 24 + 12 - 2, 5, 5);

		//g.drawLine(from.getX() * 24 + 12, from.getY() * 24 + 12, to.getX() * 24 + 12, to.getY() * 24 + 12);
	    }

	    //g.setColor(Color.orange);
	    //g.fillOval(this.targetCellX * 24 + 12, this.targetCellY * 24 + 12, 5, 5);		
	}

	// Draw grid
	/*final int GRID_SIZE = 3;
	g.setColor(Color.gray); 
	for (int i = (int) (posX / 24 - GRID_SIZE); i < posX / 24 + GRID_SIZE; i++) {
	    for (int j = (int) (posY / 24 - GRID_SIZE); j < posY / 24 + GRID_SIZE; j++) {
		g.drawRect(i * 24, j * 24, 24, 24);
	    }
	}*/
    }

    public void finishMoving() {
	this.moveX = 0;
	this.moveY = 0;

	this.isMovingToCell = false;
    }

    public double getCenterPosX() {
	return this.boundingBox.getX() + (this.sizeWidth / 2);
    }

    public double getCenterPosY() {
	return this.boundingBox.getY() + (this.sizeHeight / 2);
    }	

    public void setPositionByCenter(double x, double y) {
	setCenterX(x);
	setCenterY(y);
    }

    private void setCenterX(double x) {
	//this.posX = x - (this.sizeWidth / 2) + 6;
    }

    private void setCenterY(double y) {
	//this.posY = y - (this.sizeHeight / 2) + 12;	    
    }    
    
    public void nudge(MobileEntity nudger, boolean force) {
	nudge(nudger, force, 1);
    }
    
    public void nudge(MobileEntity nudger, boolean force, int nudgingDepth) {
	// Don't allow non-forced nudges if we doing something
	if (!force && !this.isIdle()) {
	    return;
	}
	
	// All possible adjacent cells directions
	int dx[] = { +1, -1,  0,  0, +1, -1, +1, -1 };
	int dy[] = {  0,  0, +1, -1, +1, +1, -1, -1 };
	
	Pos nudgerPos = null;
	if (nudger != null) {
	    nudgerPos = nudger.getCellPos();
	}
	
	ArrayList<Pos> availCells = new ArrayList<>();
	ArrayList<Pos> smartCells = new ArrayList<>(); // smart cells is cells which blocked, but we can try to nudge actor inside it
	
	for (int i = 0; i < 8; i++) {
	    int newCellX = (int) this.getCellPos().getX() + dx[i];
	    int newCellY = (int) this.getCellPos().getY() + dy[i];
	    
	    // Skip cell with nudger position
	    if (nudger != null && (nudgerPos.getX() == newCellX && nudgerPos.getY() == newCellY)) {
		continue;
	    }
	    
	    if (world.isCellPassable(newCellX, newCellY)) {
		availCells.add(new Pos(newCellX, newCellY));
	    } else {
		smartCells.add(new Pos(newCellX, newCellY));
	    }
	}
	
	Pos pointToGetOut = null; // target point to stand down
	
	// Choose random cell, first from of available, if not, then, select from smart cells
	if (!availCells.isEmpty()) {
	    pointToGetOut = availCells.get(world.getRandomInt(0, availCells.size()));
	} else {
	    if (!smartCells.isEmpty()) {
		pointToGetOut = smartCells.get(world.getRandomInt(0, smartCells.size()));
	    }
	}
	
	if (pointToGetOut != null && !availCells.isEmpty()) {
	    this.cancelActivity();
	    
	    this.moveTo(pointToGetOut);
	} else { 
	    // All cells seems to be blocked, lets try to nudge someone around
	    // Check depth to avoid stack overflow if we fall into recursion
	    if (nudgingDepth > 3) {
		return;
	    }

	    Collections.shuffle(smartCells);
	    
	    for (Pos cell : smartCells) {
		MobileEntity blocker = world.getMobileEntityInCell(cell);
		
		if (blocker != this && blocker != null && blocker.isFrendlyTo(this)) {
		    blocker.nudge(this, force, nudgingDepth + 1);
		}
	    }
	}
    }
    
    public void moveTo(Pos destCell) {
	this.moveTo(destCell, null);
    }
    
    public void moveTo(Pos destCell, EntityBuilding ignoreBuilding) {
	this.goalX = (int) destCell.getX();
	this.goalY = (int) destCell.getY();

	Move move = new Move(this, destCell, getMinimumEnoughRange(), ignoreBuilding);
	
	// If we already moving
	if (this.currentActivity instanceof Move) {
	    this.currentActivity.cancel();
	} else if (this.currentActivity instanceof Move.MovePart) {
	    this.currentActivity.queueActivity(move);
	    return;
	}
	
	queueActivity(move);
    }
    
    public void startMovingByPath(Path p, EntityBuilding ignoreBuilding) {
	this.goalX = 0;//(int) p.getX(p.getElements().size() - 1);
	this.goalY = 0;//(int) p.getY(p.getElements().size() - 1);
	
	queueActivity(new Move(this, p, new Pos(goalX, goalY), ignoreBuilding));
    }    
    
    @Override
    public void notifyBlocking(MobileEntity from) {
	if (this.isIdle() && from.isFrendlyTo(this)) {
	    this.nudge(from, true); // we being nudged by from 
	}
    }    
    
    public abstract float getMoveSpeed();    
    public abstract int getMinimumEnoughRange();
    public abstract boolean canEnterCell(Pos cellPos);    
    public abstract int getWaitAverageTime();
    public abstract int getWaitSpreadTime();
    
    // Orders section
    @Override
    public ArrayList<OrderTargeter> getOrders() {
	return this.ordersList;
    }
    
    @Override
    public Order issueOrder(Entity self, OrderTargeter targeter, Target target, InputAttributes ia) {
	if (targeter.orderString.equals("Move") && ia.mouseButton == 1) {
	    if (!target.isCellTarget()) {
		return null;
	    }
	    
	    return new Order("Move", null, target.getTargetCell());
	}
	
	return null;
    }
    
    @Override
    public void resolveOrder(Order order) {
	if (order.orderString.equals("Move") && order.targetPosition != null) {
	    if (!order.isQueued) {
		this.cancelActivity();
	    }
	    
	    this.moveTo(order.targetPosition);
	}
    }

    public void setCellPos(Pos exitPoint) {
	//this.posX = exitPoint.getX() * 24;
	//this.posY = exitPoint.getY() * 24;
    }

    public Activity moveFollow(EntityActor self, Target target, int range) {
	return new Follow(self, target, range);
    }

    public Activity moveWithinRange(Target target, int range) {
	return this.moveToRange(target.centerPosition().getCellPos(), range);
    }

    protected abstract Activity moveToRange(Pos cellPos, int range);
}
