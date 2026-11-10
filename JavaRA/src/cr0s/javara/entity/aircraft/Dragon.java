package cr0s.javara.entity.aircraft;

import cr0s.javara.combat.Armament;
import cr0s.javara.combat.attack.AttackFrontal;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.combat.weapon.WeaponDragon;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.order.Order;
import cr0s.javara.util.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Path;
import javafx.scene.layout.StackPane;

public class Dragon extends EntityAircraft implements IHaveCost, ICanAttack {
    private final AttackFrontal attack;
    private final AutoTarget autoTarget;

    public Dragon(Pos x) {
        super(x, 120, 120);
        setHp(3000);
        setDamagePerSecond(300);
        setImageViews(
                "assets/gif/dragonL.gif", 
                "assets/gif/dragon_attackL.gif", 
                "assets/gif/dragonR.gif", 
                "assets/gif/dragon_attackR.gif"
        );
        name = "dragon";

        this.attack = new AttackFrontal(this);
        Armament arma = new Armament(this, new WeaponDragon());
        arma.addBarrel(new Pos(0, 0), 0);
        this.attack.addArmament(arma);
        this.ordersList.addAll(this.attack.getOrders());
        this.autoTarget = new AutoTarget(this, this.attack);

        setImageView(new ImageView(getImages().get(0)));
    }
    
    @Override
    public float getMoveSpeed(){ return 16; }    
    @Override
    public int getMinimumEnoughRange(){ return 5; }
    @Override
    public boolean canEnterCell(cr0s.javara.util.Pos cellPos){ return true; }   
    @Override
    public int getWaitAverageTime(){ return 0; }
    @Override
    public int getWaitSpreadTime(){ return 0; }
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
    public void updateEntity(long delta) {
        super.updateEntity(delta);
        this.attack.update(delta);
        this.autoTarget.update(delta);
    }

    @Override
    public void resolveOrder(Order order) {
        if (order.orderString.equals("Attack")) {
            this.attack.resolveOrder(order);
            return;
        }
        super.resolveOrder(order);
    }

    @Override
    public AttackFrontal getAttackStrategy() {
        return this.attack;
    }
}
