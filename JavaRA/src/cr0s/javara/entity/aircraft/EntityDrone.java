package cr0s.javara.entity.aircraft;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponM1Carabine;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.building.soviet.EntityAirField;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * A fast, cheap reconnaissance/attack drone aircraft.
 * Built from an Air Field. Can engage ground targets with its onboard cannon.
 * @author Cr0s
 */
public class EntityDrone extends EntityAircraft implements IHaveCost, ICanAttack {
    private final AttackFrontal attack;
    private final AutoTarget autoTarget;
    
    public EntityDrone(Pos x) {
	super(x, 56, 56);

	ShpTexture tex = ResourceManager.getInstance().getConquerTexture("yak.shp");
	if (tex != null) {
	    setImageView(new ImageView(SwingFXUtils.toFXImage(tex.getAsImage(0, null), null)));
	    getImages().add(getImageView().getImage());
	}

	setHp(100);
	setMaxHp(100);
	setDamagePerSecond(35);

	name = "drone";

	this.unitProductionAlingment = Alignment.SOVIET;
	this.requiredToBuild.add(EntityAirField.class);

	// Attack setup - drone engages ground targets with a forward-mounted cannon
	this.attack = new AttackFrontal(this);
	Armament arma = new Armament(this, new WeaponM1Carabine());
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
    public float getMoveSpeed() {
	return 22;
    }

    @Override
    public int getMinimumEnoughRange() {
	return 3;
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
	return true;
    }

    @Override
    public int getWaitAverageTime() {
	return 1;
    }

    @Override
    public int getWaitSpreadTime() {
	return 1;
    }

    @Override
    public StackPane renderEntity() {
	if (getImageView() == null) {
	    ShpTexture tex = ResourceManager.getInstance().getConquerTexture("yak.shp");
	    if (tex != null) {
		setImageView(new ImageView(SwingFXUtils.toFXImage(tex.getAsImage(0, null), null)));
		getImages().add(getImageView().getImage());
	    }
	}
        getImageView().setRotate(-90);
	StackPane combined = new StackPane();
	combined.getChildren().add(getImageView());
	return combined;
    }

    @Override
    public int getBuildingCost() {
	return 25;
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
