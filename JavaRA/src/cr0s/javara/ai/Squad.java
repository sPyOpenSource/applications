package cr0s.javara.ai;

import java.util.ArrayList;

import cr0s.javara.ai.Squad.SquadType;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.order.Order;
import cr0s.javara.order.Target;

public class Squad {
    public enum SquadType { ASSAULT, AIR, RUSH, PROTECTION, DEFENSIVE, HARVEST, CONSTRUCTION, EXPLORATION };
    public enum Formation { LINE, WEDGE, COLUMN, BOX }
    
    private Formation currentFormation;
    private Target target;
    private AIPlayer bot;
    private SquadType type;
    
    private ArrayList<EntityActor> units = new ArrayList<>();
    private StateMachine fsm;
    private int initialStrength;
    private int desiredSize;
    
    public Squad(AIPlayer bot, SquadType type) {
	this(bot, type, null);
    }
    
    public Squad(AIPlayer bot, SquadType type, EntityActor target) {
	this.target = new Target(target);
	
	this.bot = bot;
	this.type = type;
	
	this.fsm = new StateMachine();
	this.desiredSize = Math.max(1, bot.squadSize);
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
	if (this.units.isEmpty()) {
	    return;
	}

	this.removeDeadAndNotOwnUnits();

	if (this.units.isEmpty()) {
	    return;
	}

	if (this.fsm.getCurrentState() != null) {
	    this.fsm.update(this);
	    return;
	}

	legacyUpdate();
    }

    private void legacyUpdate() {
	if (!this.targetIsValid()) {
	    ArrayList<EntityActor> enemies = this.bot.findClosestEnemy(this.units.get(0).getPosition());
	    if (!enemies.isEmpty()) {
		this.setTarget(enemies.get(0));
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
	this.initialStrength = Math.max(this.initialStrength, this.getLivingCount());
    }

    public int getLivingCount() {
	int living = 0;
	for (EntityActor unit : this.units) {
	    if (!unit.isDead()) {
		living++;
	    }
	}
	return living;
    }

    public int getInitialStrength() {
	return this.initialStrength;
    }

    public int getDesiredSize() {
	return this.desiredSize;
    }

    public double healthRatio() {
	int hp = 0;
	int maxHp = 0;
	for (EntityActor unit : this.units) {
	    if (unit.isDead()) {
		continue;
	    }
	    hp += unit.getHp();
	    maxHp += Math.max(1, unit.getMaxHp());
	}
	return maxHp == 0 ? 0.0 : hp / (double) maxHp;
    }

    public double casualtyRatio() {
	if (this.initialStrength <= 0) {
	    return 0.0;
	}
	double ratio = 1.0 - (this.getLivingCount() / (double) this.initialStrength);
	return Math.max(0.0, Math.min(1.0, ratio));
    }

    public boolean shouldRetreat() {
	return this.healthRatio() <= this.bot.retreatHealthPercent / 100.0
		|| this.casualtyRatio() >= this.bot.retreatCasualtyPercent / 100.0;
    }

    public void markRegrouped() {
	this.initialStrength = this.getLivingCount();
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

    public Target getTarget() {
	return this.target;
    }

    public void clearTarget() {
	this.target = new Target((EntityActor) null);
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
