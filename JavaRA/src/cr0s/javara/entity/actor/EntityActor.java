package cr0s.javara.entity.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.combat.TargetType;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.INotifySelected;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.order.IOrderIssuer;
import cr0s.javara.order.IOrderResolver;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.util.Pos;

public abstract class EntityActor extends Entity implements IOrderIssuer, IOrderResolver, INotifySelected {
    protected ArrayList<OrderTargeter> ordersList;
    protected HashMap<String, Integer[]> selectedSounds;
    protected int unitVersion = 0; // for same voice per unit
    
    public Alignment unitProductionAlingment = Alignment.NEUTRAL;
    public LinkedList<Class> requiredToBuild;
    public Activity currentActivity;

    public ArmorType armorType = ArmorType.NONE;
    public TreeSet<TargetType> targetTypes = new TreeSet<>();
    
    public int maxFacings = 32;
    public String name;
    
    public EntityActor(double posX, double posY,
	    final double aSizeWidth, final double aSizeHeight) {
	super(posX, posY, aSizeWidth, aSizeHeight);
	
	this.ordersList = new ArrayList<>();
	this.selectedSounds = new HashMap<>();
	
	requiredToBuild = new LinkedList<>();
    }

    @Override
    public void updateEntity(long delta) {
	if (this.currentActivity != null) {
	    this.currentActivity = this.currentActivity.tick(this);
	}
    }

    public void queueActivity(Activity a) {
	if (this.currentActivity != null) {
	    this.currentActivity.queueActivity(a);
	} else {
	    this.currentActivity = a;
	}
    }
    
    public void cancelActivity() {
	if (this.currentActivity != null) {
	    this.currentActivity.cancel();
	} 	
    }

    @Override
    public boolean shouldRenderedInPass(final int passNum) {
	return false;
    }

    public boolean isFrendlyTo(EntityActor other) {
        // TODO: add ally logic
	return this.owner == other.owner;
    }
    
    public boolean isIdle() {
	return this.currentActivity == null;
    }

    @Override
    public abstract void resolveOrder(Order order);

    @Override
    public ArrayList<OrderTargeter> getOrders() {
	return this.ordersList;
    }

    @Override
    public abstract Order issueOrder(Entity self, OrderTargeter targeter, Target target, InputAttributes ia);
    
    @Override
    public void notifySelected() {
    }
    
    public HashMap<String, Integer[]> getSounds() {
	return this.selectedSounds;
    }
    
    public String getSelectSound() {
	return "";
    }
    
    public void playSelectedSound() {	
    }

    public void playOrderSound() {
    }
    
    public EntityActor newInstance() {
	Constructor ctor;
	
	try {
	    ctor = (this.getClass()).getDeclaredConstructor(Float.class, Float.class);
	    ctor.setAccessible(true);
	    EntityActor newEntity = (EntityActor) ctor.newInstance(this.getTranslateX(), this.getTranslateY());

	    return newEntity;
	} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            System.getLogger(EntityActor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
	
	return null;
    }

    public Pos getPosition() {
	return new Pos(this.boundingBox.getX() + this.boundingBox.getWidth() / 2, this.boundingBox.getY() + this.boundingBox.getArcHeight() / 2, this.getTranslateZ());
    }
    
    public Pos getCellPosition() {
	return getPosition().getCellPos();
    }   
    
    public int getMaxFacings() {
	return this.maxFacings;
    }
    
    public String getName() {
	return this.name;
    }
    
    public void setName(String name) {
	this.name = name;
    }

    public Activity getCurrentActivity() {
	return this.currentActivity;
    }
}
