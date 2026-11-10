package test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.entity.actor.activity.activities.Attack;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.entity.aircraft.Dragon;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.order.Order;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.paint.Color;
import org.junit.Test;
import static org.junit.Assert.*;

public class DragonAttackTest {
    @Test
    public void dragonIsAnAttackerWithWeapon() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        Dragon dragon = new Dragon(new Pos(0, 0));
        dragon.owner = ai;

        assertTrue("dragon should be able to attack", dragon instanceof ICanAttack);
        assertNotNull("dragon should expose an attack strategy", dragon.getAttackStrategy());
        AttackBase strategy = (AttackBase) dragon.getAttackStrategy();
        assertTrue("dragon should have at least one armament", strategy.armaments.size() > 0);
    }

    @Test
    public void resolveAttackOrderQueuesAttackActivity() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        Dragon dragon = new Dragon(new Pos(0, 0));
        dragon.owner = ai;
        EntityRifleTrooper target = new EntityRifleTrooper(new Pos(24, 0));
        target.owner = ai;

        dragon.resolveOrder(new Order("Attack", dragon, null, target));
        assertTrue("resolveOrder Attack should queue an Attack activity",
            dragon.getCurrentActivity() instanceof Attack);
    }

    @Test
    public void dragonFiresAtInRangeEnemy() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        Dragon dragon = new Dragon(new Pos(0, 0));
        dragon.owner = ai;
        EntityRifleTrooper target = new EntityRifleTrooper(new Pos(24, 0));
        target.owner = ai;

        AttackBase strategy = (AttackBase) dragon.getAttackStrategy();
        dragon.resolveOrder(new Order("Attack", dragon, null, target));
        assertTrue("attack activity should be queued", dragon.getCurrentActivity() instanceof Attack);

        // The dragon may need a few ticks to face the target before it can fire.
        boolean fired = false;
        for (int i = 0; i < 60 && !fired; i++) {
            dragon.updateEntity(1);
            if (strategy.isReloading()) {
                fired = true;
            }
        }
        assertTrue("dragon should have fired at the enemy (weapon reloading)", fired);
    }
}
