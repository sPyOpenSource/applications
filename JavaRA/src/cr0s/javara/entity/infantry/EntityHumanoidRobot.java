package cr0s.javara.entity.infantry;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponM1Carabine;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.building.soviet.EntityBarracks;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.render.Sequence;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;

/**
 * Humanoid Robot - tough combat robot that walks like infantry and engages
 * enemies at range. Higher health and cost than standard infantry.
 * Requires a Barracks to build.
 */
public class EntityHumanoidRobot extends EntityInfantry implements ISelectable, IHaveCost, ICanAttack {

    private final int BUILD_COST = 180;

    public EntityHumanoidRobot(Pos pos) {
	this(pos, SubCell.CENTER);
    }

    public EntityHumanoidRobot(Pos pos,
	    SubCell sub) {
	super(pos, sub);

	this.texture = ResourceManager.getInstance().getInfantryTexture("e5.shp");

	this.setMaxHp(160);
	this.setHp(160);

	this.unitProductionAlingment = Alignment.SOVIET;
	this.requiredToBuild.add(EntityBarracks.class);

	this.currentFrame = 0;

	this.standSequence = new Sequence(texture, 0, 8, 0, 0, null);
	this.runSequence = new Sequence(texture, 16, 8, 6, 2, null);
	this.runSequence.setIsLoop(true);

	this.attackingSequence = new Sequence(texture, 64, 8, 8, 2, null);

	this.idleSequences.add(new Sequence(texture, 272, 0, 14, 2, null));
	this.idleSequences.add(new Sequence(texture, 287, 0, 16, 2, null));

	this.deathSequences.add(new Sequence(texture, 304, 0, 8, 2, null));
	this.deathSequences.add(new Sequence(texture, 312, 0, 8, 2, null));
	this.deathSequences.add(new Sequence(texture, 320, 0, 8, 2, null));
	this.deathSequences.add(new Sequence(texture, 328, 0, 12, 2, null));
	this.deathSequences.add(new Sequence(texture, 340, 0, 18, 2, null));
	this.deathSequences.add(new Sequence(electro, 0, 14, 0, 1, null));

	this.attack = new AttackFrontal(this);
	Armament arma = new Armament(this, new WeaponM1Carabine());
	arma.addBarrel(new Pos(0, 0), 0);
	this.attack.addArmament(arma);

	this.ordersList.addAll(this.attack.getOrders());
	this.autoTarget = new AutoTarget(this, this.attack);

	this.setName("humanoidrobot");
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);
    }

    @Override
    public float getMoveSpeed() {
	return 0.9f;
    }

    @Override
    public int getMinimumEnoughRange() {
	return 4;
    }

    @Override
    public int getWaitAverageTime() {
	return 45;
    }

    @Override
    public int getWaitSpreadTime() {
	return 10;
    }

    @Override
    public int getRevealingRange() {
	return 7;
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
    public int getBuildingCost() {
	return BUILD_COST;
    }

    @Override
    public AttackBase getAttackStrategy() {
	return this.attack;
    }
}
