package cr0s.javara.entity.vehicle.soviet;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackTurreted;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.Weapon105mm;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.common.EntityWarFactory;
import cr0s.javara.entity.turreted.IHaveTurret;
import cr0s.javara.entity.turreted.Turret;
import cr0s.javara.entity.vehicle.EntityVehicle;

import cr0s.javara.main.GUI;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.Scene;
import javafx.scene.shape.Path;
import javafx.scene.image.ImageView;

public class EntityHeavyTank extends EntityVehicle implements ISelectable, IHaveCost, IHaveTurret, ICanAttack {
    private final String TEXTURE_NAME = "3tnk.shp";
    private final SpriteSheet texture;

    private final int ROTATION_START_TEXTURE_INDEX = 0;
    private final int ROTATION_END_TEXTURE_INDEX = 31;
    private final int MAX_ROTATION = 32;	

    private static final int TEXTURE_WIDTH = 36;
    private static final int TEXTURE_HEIGHT = 36;
    private static final int SHROUD_REVEALING_RANGE = 8;
    private static final int WAIT_FOR_BLOCKER_AVERAGE_TIME_TICKS = 15;
    private static final int WAIT_FOR_BLOCKER_TIME_SPREAD_TICKS = 5;

    private final int updateTicks = 0;
    private final Entity targetEntity = null;

    private final float MOVE_SPEED = 0.3f;
    private final float SHIFT = 12;

    private final int BUILDING_COST = 1150;
    private final Turret turret;

    private final AttackTurreted attack;
    private final AutoTarget autoTarget;

    public EntityHeavyTank(Float posX, Float posY) {
	super(posX, posY, TEXTURE_WIDTH, TEXTURE_HEIGHT);

	texture = new SpriteSheet(ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME).getAsCombinedImage(null), TEXTURE_WIDTH, TEXTURE_HEIGHT);
	Random r = new Random();

	this.isVisible = true;

	this.setMaxHp(550);
	this.setHp(this.getMaxHp());

	this.turret = new Turret(this, new Pos(0, 0), this.texture, 32, 32);
	this.turret.setTurretSize(TEXTURE_WIDTH, TEXTURE_HEIGHT);

	Armament arma = new Armament(this, new Weapon105mm());
	arma.addBarrel(new Pos(12, -3), 0);
	arma.addBarrel(new Pos(12, 3), 0);

	attack = new AttackTurreted(this);
	attack.armaments.add(arma);

	this.autoTarget = new AutoTarget(this, this.attack);

	this.ordersList.addAll(attack.getOrders());

	this.setName("3tnk");

	this.requiredToBuild.add(EntityWarFactory.class);
	//this.requiredToBuild.add(EntityFix.class);
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);

	if (!this.attack.isAttacking) {
	    if (!this.isIdle()) { 
		this.turret.setTarget(new Pos(goalX * 24, goalY * 24));
	    } else {
		this.turret.rotateTurretTo(this.currentFacing);
	    }
	}

	//boundingBox.setBounds(posX + (TEXTURE_WIDTH / 4) - 6, posY + (TEXTURE_WIDTH / 4) - 12, TEXTURE_WIDTH / 2, TEXTURE_HEIGHT / 2);
        getImageView().setViewport(texture.getSubImage(0, currentFacing).getViewport());

	this.attack.update(delta);
	this.autoTarget.update(delta);
    }

    @Override
    public ImageView renderEntity() {
	//super.renderEntity(g);

	if (GUI.DEBUG_MODE) {
	    //g.setLineWidth(1);
	    //g.setColor(owner.playerColor);
	    //g.draw(boundingBox);
	    //g.drawOval(posX - 1, posY - 1, this.boundingBox.getWidth() + 1, this.boundingBox.getHeight() + 1);
	}

	//g.drawRect(this.getTextureX(), this.getTextureY(), TEXTURE_WIDTH, TEXTURE_HEIGHT);

	//texture.startUse();
	setImageView(texture.getSubImage(0, currentFacing));//.drawEmbedded(this.getTextureX(), this.getTextureY(), TEXTURE_WIDTH, TEXTURE_HEIGHT);
        getImageView().setX(this.getTextureX());
        getImageView().setY(this.getTextureY());
        return getImageView();
	//this.turret.render(g);
	//texture.endUse();

	/*
	Pos actorCenter = (this instanceof IHaveTurret)
		? ((IHaveTurret) this).getTurrets().get(0).getCenterPos()
			: this.getPosition();
		g.setColor(Color.white);
		g.fillOval(actorCenter.getX() - 2, actorCenter.getY() - 2, 4, 4);
		g.setColor(owner.playerColor);		

		float angle = RotationUtil.facingToAngle(this.turret.getCurrentFacing());
		g.setColor(Color.red);
		g.setLineWidth(1);
		g.drawLine(actorCenter.getX(), actorCenter.getY(), (float) (actorCenter.getX() - 30 * Math.sin(angle)), (float) (actorCenter.getY() - 30 * Math.cos(angle)));

		g.setColor(Color.green);
		g.drawLine((float) (actorCenter.getX() - 15 * Math.cos(angle)), (float) (actorCenter.getY() - 15 * -Math.sin(angle)),
			(float) (actorCenter.getX() + 15 * Math.cos(angle)), (float) (actorCenter.getY() + 15 * -Math.sin(angle)));

		g.setColor(Color.white);
		for (int i = 0; i < this.arma.getBarrels().size(); i++) {
		    Barrel b = this.arma.getBarrels().get(i);

		    Pos muzzlePos = this.arma.getMuzzlePos(b);

		    //g.setColor(i % 2 == 0 ? Color.green : Color.red);
		    g.fillRect(muzzlePos.getX() - 2, muzzlePos.getY() - 2, 4, 4);
		}*/

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
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

    @Override
    public float getMoveSpeed() {
	return this.MOVE_SPEED;
    }

    public double getTextureX() {
	return boundingBox.getX() - (TEXTURE_WIDTH / 2) + 12;
    }

    public double getTextureY() {
	return boundingBox.getY() - (TEXTURE_HEIGHT / 2) + 6; 
    }

    @Override
    public int getRevealingRange() {
	return EntityHeavyTank.SHROUD_REVEALING_RANGE;
    }

    @Override
    public Path findPathFromTo(MobileEntity e, int aGoalX, int aGoalY) {
	return world.getVehiclePathfinder().findPathFromTo(this, aGoalX, aGoalY);
    }

    @Override
    public int getMinimumEnoughRange() {
	return 2;
    }    

    @Override
    public int getWaitAverageTime() {
	return EntityHeavyTank.WAIT_FOR_BLOCKER_AVERAGE_TIME_TICKS;
    }

    @Override
    public int getWaitSpreadTime() {
	return EntityHeavyTank.WAIT_FOR_BLOCKER_TIME_SPREAD_TICKS;
    }

    @Override
    public int getBuildingCost() {
	return this.BUILDING_COST;
    }

    @Override
    public void drawTurrets(Scene g) {
    }

    @Override
    public void updateTurrets(long delta) {
	this.turret.update(delta);
    }

    @Override
    public List<Turret> getTurrets() {
	LinkedList<Turret> res = new LinkedList<>();

	res.add(this.turret);

	return res;
    }    

    @Override
    public Order issueOrder(Entity self, OrderTargeter targeter, Target target, InputAttributes ia) {
	if (super.issueOrder(self, targeter, target, ia) == null) {
	    return this.attack.issueOrder(self, targeter, target, ia);
	}

	this.attack.cancelAttack();

	return super.issueOrder(self, targeter, target, ia);
    }

    @Override
    public void resolveOrder(Order order) {
	this.attack.resolveOrder(order);
	super.resolveOrder(order);
    }

    @Override
    public AttackBase getAttackStrategy() {
	return this.attack;
    }    
}
