package cr0s.javara.entity.vehicle.common;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.IDeployable;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.activity.activities.Deploy;
import cr0s.javara.entity.actor.activity.activities.Turn;
import cr0s.javara.entity.building.common.EntityConstructionYard;
import cr0s.javara.entity.building.common.EntityWarFactory;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.vehicle.EntityVehicle;

import cr0s.javara.main.GUI;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.ui.cursor.CursorType;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.shape.Path;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class EntityMcv extends EntityVehicle implements ISelectable, IDeployable, IHaveCost {

    private final String TEXTURE_NAME = "mcv.shp";
    private final SpriteSheet texture;

    private final int ROTATION_START_TEXTURE_INDEX = 0;
    private final int ROTATION_END_TEXTURE_INDEX = 31;
    private final int MAX_ROTATION = 32;	
    private final int BUILD_ROTATION = 12;

    private static final int TEXTURE_WIDTH = 48;
    private static final int TEXTURE_HEIGHT = 48;
    private static final int SHROUD_REVEALING_RANGE = 5;
    private static final int WAIT_FOR_BLOCKER_AVERAGE_TIME_TICKS = 50;
    private static final int WAIT_FOR_BLOCKER_TIME_SPREAD_TICKS = 15;

    private final int updateTicks = 0;
    private final int rotationDirection = 1;
    private boolean isDeploying = false;

    private final float MOVE_SPEED = 0.1f;
    private final int BUILDING_COST = 2000;

    public EntityMcv(Pos pos) {
	super(pos, TEXTURE_WIDTH, TEXTURE_HEIGHT);

	//boundingBox.setBounds(posX, posY, (TEXTURE_WIDTH / 2), (TEXTURE_HEIGHT / 2));
	//boundingBox.setCenterX(this.getCenterPosX());
	//boundingBox.setCenterY(this.getCenterPosY());
	
	texture = new SpriteSheet(ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME).getAsCombinedImage(null), TEXTURE_WIDTH, TEXTURE_HEIGHT);

	this.setHp(600);
	this.setMaxHp(600);
	
	this.currentFacing = 16;
	
	this.ordersList.add(new McvDeployTargeter(this));
	
	this.setName("mcv");
	
	this.requiredToBuild.add(EntityWarFactory.class);
	//this.requiredToBuild.add(EntityFix.class);
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);
	//boundingBox.setBounds(posX, posY - 6, (TEXTURE_WIDTH / 2), (TEXTURE_HEIGHT / 2));
	//boundingBox.setCenterX(this.getCenterPosX());
	//boundingBox.setCenterY(this.getCenterPosY());
        getImageView().setViewport(texture.getSubImage(0, currentFacing).getViewport());
    }

    @Override
    public StackPane renderEntity() {
	//super.renderEntity(g);
	
	if (GUI.DEBUG_MODE) {
	    //g.setLineWidth(1);
	    //g.setColor(owner.playerColor);
	    //g.draw(boundingBox);
	    //g.drawOval(posX - 1, posY - 1, this.boundingBox.getWidth() + 1, this.boundingBox.getHeight() + 1);
	}

	double tx = this.getTextureX();
	double ty = this.getTextureY();

	//g.drawRect(tx, ty, TEXTURE_WIDTH, TEXTURE_HEIGHT);

	//texture.startUse();
	setImageView(texture.getSubImage(0, currentFacing));//.drawEmbedded(tx, ty, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        getImageView().setX(tx);
        getImageView().setY(ty);
        StackPane combined = new StackPane();
combined.getChildren().add(getImageView());
//combined.relocate(ty, ty);
        return combined;
	//texture.endUse();
	
	//g.setColor(Color.white);
	//g.fillOval(this.getCenterPosX(), this.getCenterPosY(), 5, 5);
	//g.setColor(owner.playerColor);		

	//drawPath(g);
    }

    @Override
    public void select() {
	this.isSelected = true;
    }

    @Override
    public void cancelSelect() {
	this.isSelected = false;
    }

    @Override
    public boolean isSelected() {
	return this.isSelected;
    }

    @Override
    public boolean canDeploy() {
	// Check deploy possibility via World blockingMap
	int bx = (int) (boundingBox.getX() / 24) - (EntityConstructionYard.WIDTH_TILES / 2);
	int by = (int) (boundingBox.getY() / 24) - (EntityConstructionYard.HEIGHT_TILES / 2);
	
	/*for (int x = 0; x < EntityConstructionYard.WIDTH_TILES; x++) {
	    for (int y = 0; y < EntityConstructionYard.HEIGHT_TILES; y++) {
		if (!world.isCellBuildable(bx + x, by + y, true)) {
		    return false;
		}
		
		Entity e = world.getEntityInPoint((bx + x) * 24, (by + y) * 24);
		if (e != null && !(e instanceof EntityMcv)) {
		    return false;
		}
	    }
	}*/
	
	return true;
    }

    @Override
    public void deploy() {
	//if (canDeploy()) { 
	    deployConstructionYard();
	//}
    }

    private void deployConstructionYard() {
	this.isDeploying = true;

	queueActivity(new Turn(this, this.BUILD_ROTATION, 3));
	queueActivity(new Deploy());
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

    @Override
    public float getMoveSpeed() {
	return this.MOVE_SPEED;
    }

    public double getTextureX() {
	return boundingBox.getX() - (TEXTURE_WIDTH / 2) + 18;
    }

    public double getTextureY() {
	return boundingBox.getY() - (TEXTURE_HEIGHT / 2) + 12; 
    }
    
    @Override
    public int getRevealingRange() {
	return EntityMcv.SHROUD_REVEALING_RANGE;
    }

    @Override
    public Path findPathFromTo(MobileEntity e, Pos aGoal) {
	return world.getVehiclePathfinder().findPathFromTo(this, aGoal);
    }

    @Override
    public void executeDeployment() {
	if (!this.isDeploying) {
	    return;
	}
	
	EntityConstructionYard cy = new EntityConstructionYard(new Pos(boundingBox.getX() - (EntityConstructionYard.WIDTH_TILES / 2 * 24), boundingBox.getY() - (EntityConstructionYard.HEIGHT_TILES / 2 * 24)));
	cy.isVisible = true;
	cy.isSelected = true;
        //owner.entities.add(cy);
        //cy.owner = owner;
        EntityBuilding ebp = world.addBuildingTo(cy);
        ebp.owner = owner;
	owner.entities.add(ebp);

	setDead();
    }
    
    @Override
    public int getMinimumEnoughRange() {
	return 3;
    }
    
    @Override
    public int getWaitAverageTime() {
	return EntityMcv.WAIT_FOR_BLOCKER_AVERAGE_TIME_TICKS;
    }

    @Override
    public int getWaitSpreadTime() {
	return EntityMcv.WAIT_FOR_BLOCKER_TIME_SPREAD_TICKS;
    }   
    
    @Override
    public Order issueOrder(Entity self, OrderTargeter targeter, Target target, InputAttributes ia) {
	if (targeter.orderString.equals("Deploy") && ia.mouseButton == 1) {
	    return new Order("Deploy", null, null);
	}
	
	return super.issueOrder(self, targeter, target, ia);
    }
    
    @Override
    public void resolveOrder(Order order) {
	//super.resolveOrder(order);
	
	if (order.orderString.equals("Deploy")) {
	    this.deploy();
	}
    }
    
    private class McvDeployTargeter extends OrderTargeter {
	public McvDeployTargeter(EntityActor ent) {
	    super("Deploy", 8, true, false, ent);
	}

	@Override
	public boolean canTarget(Entity self, Target target) {
	    return (self instanceof EntityMcv) && (target.getTargetEntity() == self);
	}

	@Override
	public CursorType getCursorForTarget(Entity self, Target target) {
	    return (((EntityMcv)self).canDeploy()) ? CursorType.CURSOR_DEPLOY : CursorType.CURSOR_NO_DEPLOY;
	}
    }

    @Override
    public int getBuildingCost() {
	return this.BUILDING_COST;
    }
}
