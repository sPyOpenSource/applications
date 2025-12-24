package cr0s.javara.entity.vehicle.soviet;

import java.util.Random;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.ArmorType;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponSCUD;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.common.EntityRadarDome;
import cr0s.javara.entity.building.common.EntityWarFactory;
import cr0s.javara.entity.vehicle.EntityVehicle;

import cr0s.javara.main.GUI;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;
import cr0s.javara.util.RotationUtil;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.shape.Path;
import javafx.scene.image.ImageView;

public class EntityV2Launcher extends EntityVehicle implements ISelectable, IHaveCost, ICanAttack {

    private final String TEXTURE_NAME = "v2rl.shp";
    private final SpriteSheet texture;

    private static final int TEXTURE_WIDTH = 40;
    private static final int TEXTURE_HEIGHT = 40;
    private static final int ATTACK_OFFSET = 64;
    private static final int ATTACKING_FACINGS = 8;
    
    private static final int SHROUD_REVEALING_RANGE = 5;
    private static final int WAIT_FOR_BLOCKER_AVERAGE_TIME_TICKS = 15;
    private static final int WAIT_FOR_BLOCKER_TIME_SPREAD_TICKS = 5;

    private final int updateTicks = 0;
    private final Entity targetEntity = null;

    private final float MOVE_SPEED = 0.3f;
    private final float SHIFT = 12;
    private final int BUILDING_COST = 1150;

    private final AttackFrontal attack;
    private final AutoTarget autoTarget;
    
    public EntityV2Launcher(Float posX, Float posY) {
	super(posX, posY, TEXTURE_WIDTH, TEXTURE_HEIGHT);

	texture = new SpriteSheet(ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME).getAsCombinedImage(null), TEXTURE_WIDTH, TEXTURE_HEIGHT);
	Random r = new Random();

	this.isVisible = true;

	this.setMaxHp(200);
	this.setHp(this.getMaxHp());
	
	this.armorType = ArmorType.LIGHT;

	Armament arma = new Armament(this, new WeaponSCUD());
	arma.addBarrel(new Pos(0, 0), 0);

	attack = new AttackFrontal(this);
	attack.armaments.add(arma);

	this.autoTarget = new AutoTarget(this, this.attack);
	
	this.ordersList.addAll(attack.getOrders());
	
	this.setName("v2rl");
	
	this.requiredToBuild.add(EntityWarFactory.class);
	this.requiredToBuild.add(EntityRadarDome.class);
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);

	//boundingBox.setBounds(posX + (TEXTURE_WIDTH / 4) - 7, posY + (TEXTURE_WIDTH / 4) - 12, (TEXTURE_WIDTH / 2), (TEXTURE_HEIGHT / 2));	

	this.attack.update(delta);
	this.autoTarget.update(delta);
    }

    @Override
    public ImageView renderEntity() {
	//super.renderEntity(g);

	if (GUI.DEBUG_MODE) {
	    /*g.setLineWidth(1);
	    g.setColor(owner.playerColor);
	    g.draw(boundingBox);*/
	}


	//texture.startUse();
	
	int textureIndex = (this.attack.armaments.get(0).isReloading() ? 32 : 0) + currentFacing;
	if (this.attack.isAttacking) {
	    int attackingFacing = RotationUtil.quantizeFacings(this.currentFacing, EntityV2Launcher.ATTACKING_FACINGS) % EntityV2Launcher.ATTACKING_FACINGS;
	    textureIndex = EntityV2Launcher.ATTACK_OFFSET + (this.attack.armaments.get(0).isReloading() ? EntityV2Launcher.ATTACKING_FACINGS : 0) + attackingFacing;
	}
	
	//texture.getSubImage(0, textureIndex).drawEmbedded(this.getTextureX(), this.getTextureY(), TEXTURE_WIDTH, TEXTURE_HEIGHT);
	
	//texture.endUse();

	//drawPath(g);
        return null;
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
	return getTranslateX() - (TEXTURE_WIDTH / 2) + 12;
    }

    public double getTextureY() {
	return getTranslateY() - (TEXTURE_HEIGHT / 2) + 6; 
    }

    @Override
    public int getRevealingRange() {
	return this.SHROUD_REVEALING_RANGE;
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
	return this.WAIT_FOR_BLOCKER_AVERAGE_TIME_TICKS;
    }

    @Override
    public int getWaitSpreadTime() {
	return this.WAIT_FOR_BLOCKER_TIME_SPREAD_TICKS;
    }

    @Override
    public int getBuildingCost() {
	return this.BUILDING_COST;
    }  

    @Override
    public Order issueOrder(Entity self, OrderTargeter targeter, Target target, InputAttributes ia) {
	if (super.issueOrder(self, targeter, target, ia) == null) {
	    return this.attack.issueOrder(self, targeter, target, ia);
	}

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
