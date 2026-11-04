package cr0s.javara.entity.vehicle.soviet;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponM1Carabine;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.common.EntityWarFactory;
import cr0s.javara.entity.vehicle.EntityVehicle;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.render.EntityBlockingMap.FillsSpace;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.shape.Path;
import javafx.scene.layout.StackPane;

/**
 * Quad - fast, light reconnaissance/attack vehicle. High speed and low cost
 * make it ideal for scouting and hit-and-run strikes, but it is fragile
 * compared to tanks. Requires a War Factory to build.
 */
public class EntityQuad extends EntityVehicle implements ISelectable, IHaveCost, ICanAttack {

    private static final int TEXTURE_WIDTH = 36;
    private static final int TEXTURE_HEIGHT = 36;
    private static final int SHROUD_REVEALING_RANGE = 6;

    private static final float MOVE_SPEED = 0.6f;
    private static final int BUILDING_COST = 300;

    private final SpriteSheet texture;
    private final AttackBase attack;
    private final AutoTarget autoTarget;

    public EntityQuad(Pos pos) {
	super(pos, TEXTURE_WIDTH, TEXTURE_HEIGHT);

	texture = new SpriteSheet(ResourceManager.getInstance().getConquerTexture("jeep.shp").getAsCombinedImage(null), TEXTURE_WIDTH, TEXTURE_HEIGHT);

	this.isVisible = true;

	this.setMaxHp(200);
	this.setHp(this.getMaxHp());

	this.fillsSpace = FillsSpace.ONE_CELL;

	this.unitProductionAlingment = Alignment.SOVIET;
	this.requiredToBuild.add(EntityWarFactory.class);

	this.currentFacing = 16;

	this.attack = new AttackFrontal(this);
	Armament arma = new Armament(this, new WeaponM1Carabine());
	arma.addBarrel(new Pos(12, 0), 0);
	this.attack.addArmament(arma);

	this.autoTarget = new AutoTarget(this, this.attack);

	this.ordersList.addAll(this.attack.getOrders());

	this.setName("quad");
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);

	getImageView().setViewport(texture.getSubImage(0, currentFacing).getViewport());

	this.attack.update(delta);
	this.autoTarget.update(delta);
    }

    @Override
    public StackPane renderEntity() {
	setImageView(texture.getSubImage(0, currentFacing));
	getImageView().setX(getTextureX());
	getImageView().setY(getTextureY());
	StackPane combined = new StackPane();
	combined.getChildren().add(getImageView());
	return combined;
    }

    public double getTextureX() {
	return boundingBox.getX() - (TEXTURE_WIDTH / 2) + 12;
    }

    public double getTextureY() {
	return boundingBox.getY() - (TEXTURE_HEIGHT / 2) + 6;
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
    public Path findPathFromTo(MobileEntity e, Pos aGoal) {
	return owner.world.getVehiclePathfinder().findPathFromTo(this, aGoal);
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
	return owner.world.blockingEntityMap.isEntityInCell(cellPos, this) || owner.world.isCellPassable(cellPos);
    }

    @Override
    public float getMoveSpeed() {
	return MOVE_SPEED;
    }

    @Override
    public int getMinimumEnoughRange() {
	return 2;
    }

    @Override
    public int getWaitAverageTime() {
	return 15;
    }

    @Override
    public int getWaitSpreadTime() {
	return 5;
    }

    @Override
    public int getRevealingRange() {
	return SHROUD_REVEALING_RANGE;
    }

    @Override
    public int getBuildingCost() {
	return BUILDING_COST;
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
	if (order.orderString.equals("Attack") || order.orderString.equals("Stop")) {
	    this.attack.resolveOrder(order);
	} else {
	    super.resolveOrder(order);
	}
    }

    @Override
    public AttackBase getAttackStrategy() {
	return this.attack;
    }
}
