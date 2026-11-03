package cr0s.javara.entity.infantry;

import java.util.ArrayList;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponM1Carabine;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.building.soviet.EntityBarracks;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.render.Sequence;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;

/**
 * Medic - unarmed support infantry that slowly restores health to nearby
 * friendly infantry units. Low health, must get close to be effective.
 * Requires a Barracks to build.
 */
public class EntityMedic extends EntityInfantry implements ISelectable, IHaveCost, ICanAttack {

    private final int BUILD_COST = 300;

    private static final int HEAL_RANGE_CELLS = 3;
    private static final int HEAL_INTERVAL_TICKS = 60;
    private static final int HEAL_AMOUNT = 5;

    private int healTicks = 0;

    public EntityMedic(Pos pos) {
	this(pos, SubCell.CENTER);
    }

    public EntityMedic(Pos pos,
	    SubCell sub) {
	super(pos, sub);

	this.texture = ResourceManager.getInstance().getInfantryTexture("e6.shp");

	this.setMaxHp(40);
	this.setHp(40);

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

	// Medics are unarmed but keep a nominal attack strategy so the
	// ICanAttack interface and auto-target infrastructure are satisfied.
	this.attack = new AttackFrontal(this);
	Armament arma = new Armament(this, new WeaponM1Carabine());
	arma.addBarrel(new Pos(0, 0), 0);
	this.attack.addArmament(arma);

	this.ordersList.addAll(this.attack.getOrders());
	this.autoTarget = new AutoTarget(this, this.attack);

	this.setName("medic");
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);

	// Periodically heal nearby friendly infantry.
	if (++this.healTicks >= HEAL_INTERVAL_TICKS) {
	    this.healTicks = 0;
	    healNearbyFriendlies();
	}
    }

    private void healNearbyFriendlies() {
	if (this.owner == null || this.owner.entities == null) {
	    return;
	}

	final double healRangeSq = HEAL_RANGE_CELLS * 24 * HEAL_RANGE_CELLS * 24;

	ArrayList<EntityActor> toHeal = new ArrayList<>();
	for (Entity e : this.owner.entities) {
	    if (e == this || e.isDead() || !(e instanceof EntityInfantry)) {
		continue;
	    }
	    EntityActor a = (EntityActor) e;
	    double dx = a.getPosition().getX() - this.getPosition().getX();
	    double dy = a.getPosition().getY() - this.getPosition().getY();
	    if (dx * dx + dy * dy < healRangeSq && a.getHp() < a.getMaxHp()) {
		toHeal.add(a);
	    }
	}

	int share = Math.max(1, HEAL_AMOUNT / Math.max(1, toHeal.size()));
	for (EntityActor a : toHeal) {
	    int newHp = a.getHp() + share;
	    if (newHp > a.getMaxHp()) {
		newHp = a.getMaxHp();
	    }
	    a.setHp(newHp);
	}
    }

    @Override
    public int getMinimumEnoughRange() {
	return 1;
    }

    @Override
    public int getWaitAverageTime() {
	return 50;
    }

    @Override
    public int getWaitSpreadTime() {
	return 10;
    }

    @Override
    public int getRevealingRange() {
	return 4;
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
