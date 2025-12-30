package cr0s.javara.entity.infantry;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponM1Carabine;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.render.Sequence;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;

public class EntityRifleTrooper extends EntityInfantry implements ISelectable, IHaveCost, ICanAttack {

    private final int BUILD_COST = 100;
    
    public EntityRifleTrooper(Pos pos) {
	this(pos, SubCell.CENTER);
    }    
    
    public EntityRifleTrooper(Pos pos,
	    SubCell sub) {
	super(pos, sub);

	this.texture = ResourceManager.getInstance().getInfantryTexture("e1.shp");

	this.setMaxHp(50);
	this.setHp(50);

	this.currentFrame = 0;

	this.standSequence = new Sequence(texture, 0, 8, 0, 0, null);
	this.runSequence = new Sequence(texture, 16, 8, 6, 2, null);
	this.runSequence.setIsLoop(true);

	this.attackingSequence = new Sequence(texture, 64, 8, 8, 2, null);
	//this.attackingSequence.setIsLoop(true);
	
	this.idleSequences.add(new Sequence(texture, 256, 0, 16, 2, null));
	this.idleSequences.add(new Sequence(texture, 272, 0, 16, 2, null));
	
	this.deathSequences.add(new Sequence(texture, 288, 0, 8, 2, null));
	this.deathSequences.add(new Sequence(texture, 296, 0, 8, 2, null));
	this.deathSequences.add(new Sequence(texture, 304, 0, 8, 2, null));
	this.deathSequences.add(new Sequence(texture, 312, 0, 12, 2, null));
	this.deathSequences.add(new Sequence(texture, 324, 0, 18, 2, null));
	this.deathSequences.add(new Sequence(electro, 0, 14, 0, 1, null));
	
	this.attack = new AttackFrontal(this);
	Armament arma = new Armament(this, new WeaponM1Carabine());
	arma.addBarrel(new Pos(0, 0), 0);
	this.attack.addArmament(arma);
	
	this.ordersList.addAll(this.attack.getOrders());
	this.autoTarget = new AutoTarget(this, this.attack);
	
	this.setName("e1");
    }

    @Override 
    public void updateEntity(long delta) {	
	super.updateEntity(delta);
    }

    @Override
    public int getMinimumEnoughRange() {
	return 3;
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
