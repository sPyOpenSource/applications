package cr0s.javara.entity.building.soviet;

import java.util.ArrayList;
import java.util.List;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackTurreted;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.weapon.WeaponFireballLauncher;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IDefense;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.Defensive;
import cr0s.javara.entity.building.IPowerConsumer;
import cr0s.javara.entity.turreted.IHaveTurret;
import cr0s.javara.entity.turreted.Turret;

import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class EntityFireTurret extends Defensive implements ISelectable, IPowerConsumer, IShroudRevealer, IHaveCost, IDefense, IHaveTurret {

    private SpriteSheet sheet;
    private final String TEXTURE_NAME = "ftur.shp";
    private final String MAKE_TEXTURE_NAME = "fturmake.shp";

    public static final int WIDTH_TILES = 1;
    public static final int HEIGHT_TILES = 1;

    private static final int POWER_CONSUMPTION_LEVEL = 20;
    private static final int SHROUD_REVEALING_RANGE = 3;
    private static final int BUILDING_COST = 600;
    
    private final AttackTurreted attack;
    private final Armament arma;
    
    private final AutoTarget autoTarget;
    private final Turret turret;
    
    public EntityFireTurret(Pos tile) {
	super(null, 400, null, 0, 0, WIDTH_TILES * 24, HEIGHT_TILES * 24/*, "x"*/, tile.getX(), tile.getY());

	setBibType(BibType.NONE);
	setProgressValue(-1);

	setMaxHp(400);
	setHp(getMaxHp());

	this.makeTextureName = MAKE_TEXTURE_NAME;
	initTextures();
	
	this.unitProductionAlingment = Alignment.SOVIET;

	this.requiredToBuild.add(EntityBarracks.class);
	
	this.attack = new AttackTurreted(this);
	this.arma = new Armament(this, new WeaponFireballLauncher());
	this.arma.addBarrel(new Pos(8, 0, 0), 0);
	
	this.attack.addArmament(this.arma);
	
	this.autoTarget = new AutoTarget(this, this.attack);
	
	this.ordersList.addAll(this.attack.getOrders());
	
	this.turret = new Turret(this, new Pos(0, 0), null, 0, 32);
	this.turret.width = this.turret.height = 24;
	
	this.setName("ftur");
    }
    
    private void initTextures() {
	ShpTexture tex = ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME);
	
	this.sheet = new SpriteSheet(tex.getAsCombinedImage(null), 24, 24);
    }

    @Override
    public StackPane renderEntity() {
        ImageView view;
	int textureIndex = (this.getHp() < this.getMaxHp() / 2) ? 1 : 0;

	view = this.sheet.getSubImage(0, textureIndex);//.draw(nx, ny);
	
	// Draw bounding box if debug mode is on
	if (GUI.DEBUG_MODE) {
	    /*g.setLineWidth(2);
	    g.setColor(owner.playerColor);
	    g.draw(boundingBox);
	    g.setLineWidth(1);*/
	}

	// Draw range circle
	if (this.isSelected) {
	    Circle c = new Circle(this.getPosition().getX(), this.getPosition().getY(), this.attack.getMaxRange() * 24);
	    //g.setColor(Color.YELLOW);
	    //g.draw(c);
	}
	
	// Render repairing wrench
	if (this.repairIconBlink) {
	    //repairImage.draw(this.boundingBox.getX() + this.boundingBox.getWidth() / 2 - repairImage.getWidth() / 2, this.boundingBox.getY() + this.boundingBox.getHeight() / 2 - repairImage.getHeight() / 2);
	}
        view.setX(boundingBox.getX());
        view.setY(boundingBox.getY());
        StackPane combined = new StackPane();
        combined.getChildren().add(view);
        return combined;
    }

    @Override
    public boolean shouldRenderedInPass(int passnum) {
	return passnum == -1;
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);
	
	this.attack.update(delta);
	this.autoTarget.update(delta);
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
    public float getHeightInTiles() {
	return this.tileHeight;
    }

    @Override
    public float getWidthInTiles() {
	return this.tileWidth;
    }

    @Override
    public int getConsumptionLevel() {
	return EntityFireTurret.POWER_CONSUMPTION_LEVEL;
    }

    @Override
    public int getRevealingRange() {
	return EntityFireTurret.SHROUD_REVEALING_RANGE;
    }
    
    @Override
    public ImageView getTexture() {
	return this.sheet.getSubImage(0, 0);
    }

    @Override
    public int getBuildingCost() {
	return BUILDING_COST;
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
    public void drawTurrets(Scene g) {
	
    }

    @Override
    public void updateTurrets(long delta) {
	this.turret.update(delta);
    }

    @Override
    public List<Turret> getTurrets() {
	ArrayList<Turret> a = new ArrayList<>();
	a.add(this.turret);
	
	return a;
    }       
}
