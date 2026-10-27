package cr0s.javara.ai;

import java.util.ArrayList;

import cr0s.javara.ai.Squad.SquadType;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.order.Order;
import cr0s.javara.order.Target;

public class Squad {
    public enum SquadType { ASSAULT, AIR, RUSH, PROTECTION, DEFENSIVE, HARVEST, CONSTRUCTION };
    public enum Formation { LINE, WEDGE, COLUMN, BOX }
    
    private Formation currentFormation;
    private Target target;
    private AIPlayer bot;
    private SquadType type;
    
    private ArrayList<EntityActor> units = new ArrayList<>();
    private StateMachine fsm;
    
    public Squad(AIPlayer bot, SquadType type) {
	this(bot, type, null);
    }
    
    public Squad(AIPlayer bot, SquadType type, EntityActor target) {
	this.target = new Target(target);
	
	this.bot = bot;
	this.type = type;
	
	this.fsm = new StateMachine();
    }
    
    public void adoptFormation(Formation f) {  }
    public void maintainCohesion() {  }
    
    public boolean isValid() {
	return !this.units.isEmpty() && this.units.stream().anyMatch(u -> !u.isDead());
    }

    public void removeDeadAndNotOwnUnits() {
	ArrayList<EntityActor> newUnits = new ArrayList<>();
	
	for (EntityActor a : this.units) {
	    if (!a.isDead() && a.owner == bot) {
		newUnits.add(a);
	    }
	}
	
	this.units = newUnits;
    }

    public SquadType getType() {
	return this.type;
    }

    public void update() {
	if (!this.isValid()) {
	    return;
	}

	if (!this.targetIsValid()) {
	    if (!this.units.isEmpty()) {
		ArrayList<EntityActor> enemies = this.bot.findClosestEnemy(this.units.get(0).getPosition());
		if (!enemies.isEmpty()) {
		    this.setTarget(enemies.get(0));
		}
	    }
	    return;
	}

	EntityActor tgt = (EntityActor) this.target.getTargetEntity();
	for (EntityActor u : this.units) {
	    if (!u.isIdle()) {
		continue;
	    }
	    if (u instanceof ICanAttack iCanAttack) {
		AttackBase ab = (AttackBase) iCanAttack.getAttackStrategy();
		if (ab.chooseArmamentForTarget(new Target(tgt)) != null) {
		    u.resolveOrder(new Order("Attack", u, null, tgt));
		} else {
		    u.resolveOrder(new Order("Move", u, tgt.getCellPosition()));
		}
	    } else {
		u.resolveOrder(new Order("Move", u, tgt.getCellPosition()));
	    }
	}
    }

    public void addUnit(EntityActor a) {
	this.units.add(a);
    }

    public boolean targetIsValid() {
	return !this.units.isEmpty() && this.target.isValidFor(this.units.get(0));
    }

    public void setTarget(EntityActor tgt) {
	this.target = new Target(tgt);
	for (EntityActor u : this.units) {
	    if (u instanceof ICanAttack iCanAttack) {
		AttackBase ab = (AttackBase) iCanAttack.getAttackStrategy();
		if (ab.chooseArmamentForTarget(new Target(tgt)) != null) {
		    u.resolveOrder(new Order("Attack", u, null, tgt));
		} else {
		    u.resolveOrder(new Order("Move", u, tgt.getCellPosition()));
		}
	    } else {
		u.resolveOrder(new Order("Move", u, tgt.getCellPosition()));
	    }
	}
    }
    
    public AIPlayer getBot() {
	return this.bot;
    }

    public ArrayList<EntityActor> getUnits() {
	return this.units;
    }
    
    public StateMachine getFsm() {
	return this.fsm;
    }
}
