package cr0s.javara.entity.building.china;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackTurreted;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.weapon.WeaponM1Carabine;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IDefense;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.building.BibType;
import static cr0s.javara.entity.building.BuildingType.DEFENSIVE;
import cr0s.javara.entity.building.Defensive;
import cr0s.javara.entity.building.IPowerConsumer;
import cr0s.javara.entity.turreted.IHaveTurret;
import cr0s.javara.entity.turreted.Turret;

import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.util.Pos;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ArcherTower extends Defensive implements ISelectable, IPowerConsumer, IShroudRevealer, IHaveCost, IDefense, IHaveTurret {

    private static final int WIDTH_TILES = 1;
    private static final int HEIGHT_TILES = 1;

    private static final int POWER_CONSUMPTION_LEVEL = 10;
    private static final int SHROUD_REVEALING_RANGE = 2;
    private static final int BUILDING_COST = 100;

    private final AttackTurreted attack;
    private final Armament arma;
    private final AutoTarget autoTarget;
    private final Turret turret;

    public ArcherTower(Pos tile) {
        super(DEFENSIVE, 1230, "assets/png/archerTower.png", 100, 52, WIDTH_TILES * 24, HEIGHT_TILES * 24, "x", tile);

        setBibType(BibType.NONE);
        setProgressValue(-1);

        setMaxHp(1230);
        setHp(getMaxHp());

        this.attack = new AttackTurreted(this);
        this.arma = new Armament(this, new WeaponM1Carabine());
        this.arma.addBarrel(new Pos(8, 0, 0), 0);
        this.attack.addArmament(this.arma);

        this.autoTarget = new AutoTarget(this, this.attack);

        this.ordersList.addAll(this.attack.getOrders());

        this.turret = new Turret(this, new Pos(0, 0), null, 0, 32);
        this.turret.width = this.turret.height = 24;

        this.setName("artw");
    }

    @Override
    public StackPane renderEntity() {
        if (getImageView() == null) {
            setImageView(new ImageView(getImages().get(0)));
        }
        StackPane combined = new StackPane();
        combined.getChildren().add(getImageView());
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
    public int getHeightInTiles() {
        return this.tileHeight;
    }

    @Override
    public int getWidthInTiles() {
        return this.tileWidth;
    }

    @Override
    public int getConsumptionLevel() {
        return POWER_CONSUMPTION_LEVEL;
    }

    @Override
    public int getRevealingRange() {
        return SHROUD_REVEALING_RANGE;
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
