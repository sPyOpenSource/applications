package cr0s.javara.entity.aircraft;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponDragon;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.util.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class Dragon extends EntityAircraft implements IHaveCost, ICanAttack {
    private final AttackFrontal attack;
    private final AutoTarget autoTarget;
    
    public Dragon(Pos x) {
	super(x, 120, 120);
	setHp(3000);
	setMaxHp(3000);
	setDamagePerSecond(300);
	setImageViews(
		"assets/gif/dragonL.gif",
		"assets/gif/dragon_attackL.gif",
		"assets/gif/dragonR.gif",
		"assets/gif/dragon_attackR.gif"
	);
	name = "dragon";

	// Attack setup - dragon breathes fire on ground targets
	this.attack = new AttackFrontal(this);
	Armament arma = new Armament(this, new WeaponDragon());
	arma.addBarrel(new Pos(0, 0), 0);
	this.attack.addArmament(arma);

	this.autoTarget = new AutoTarget(this, this.attack);

	this.ordersList.addAll(this.attack.getOrders());
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);

	if (this.attack != null) {
	    this.attack.update(delta);
	    if (this.autoTarget != null) {
		this.autoTarget.update(delta);
	    }
	}
    }

    @Override
    public float getMoveSpeed(){ return 16; }    
    @Override
    public int getMinimumEnoughRange(){ return 4; }
    @Override
    public boolean canEnterCell(cr0s.javara.util.Pos cellPos){ return true; }   
    @Override
    public int getWaitAverageTime(){ return 2; }
    @Override
    public int getWaitSpreadTime(){ return 2; }
    @Override
    public StackPane renderEntity(){ 
	if(getImageView() == null){
	    setImageView(new ImageView(getImages().get(0)));
	}
	StackPane combined = new StackPane();
	combined.getChildren().add(getImageView());
	return combined; 
    }

    @Override
    public int getBuildingCost() {
	return 20;
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
