package cr0s.javara.entity.infantry;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponDragon;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.render.Sequence;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;

public class EntityRocketTrooper extends EntityInfantry implements ISelectable, IHaveCost, ICanAttack {
    
    private int BUILD_COST = 300;
    
    public EntityRocketTrooper(Float posX, Float posY) {
	this(posX, posY, SubCell.CENTER);
    }    
    
    public EntityRocketTrooper(double posX, double posY,
	    SubCell sub) {
	super(posX, posY, sub);
	
	this.texture = ResourceManager.getInstance().getInfantryTexture("e3.shp");

	this.setMaxHp(45);
	this.setHp(45);
	
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
	Armament arma = new Armament(this, new WeaponDragon());
	arma.addBarrel(new Pos(0, 0), 0);
	this.attack.addArmament(arma);
	
	this.ordersList.addAll(this.attack.getOrders());
	this.autoTarget = new AutoTarget(this, this.attack);
	
	this.setName("e3");
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
	// TODO Auto-generated method stub
	return 50;
    }

    @Override
    public int getWaitSpreadTime() {
	// TODO Auto-generated method stub
	return 10;
    }

    @Override
    public int getRevealingRange() {
	return 5;
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
