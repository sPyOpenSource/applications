package cr0s.javara.ai.states;

import java.util.ArrayList;

import cr0s.javara.ai.Squad;
import cr0s.javara.ai.StateMachine.IState;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.order.Order;
import cr0s.javara.order.Target;

public class GroundCombatState extends GroundStateBase implements IState {

    @Override
    public void activate(Squad squad) {
    }

    @Override
    public void tick(Squad squad) {
        if (!squad.isValid()) {
            return;
        }

        if (squad.shouldRetreat()) {
            squad.getFsm().changeState(squad, new GroundRetreatState(), true);
            return;
        }

        if (!squad.targetIsValid()) {
            ArrayList<EntityActor> enemies =
                    squad.getBot().findClosestEnemy(squad.getUnits().get(0).getPosition());
            if (enemies.isEmpty()) {
                return;
            }
            squad.setTarget(enemies.get(0));
            return;
        }

        EntityActor target = (EntityActor) squad.getTarget().getTargetEntity();
        for (EntityActor unit : squad.getUnits()) {
            if (!unit.isIdle()) {
                continue;
            }
            if (unit instanceof ICanAttack iCanAttack) {
                AttackBase attack = (AttackBase) iCanAttack.getAttackStrategy();
                if (attack.chooseArmamentForTarget(new Target(target)) != null) {
                    unit.resolveOrder(new Order("Attack", unit, null, target));
                } else {
                    unit.resolveOrder(new Order("Move", unit, target.getCellPosition()));
                }
            } else {
                unit.resolveOrder(new Order("Move", unit, target.getCellPosition()));
            }
        }
    }

    @Override
    public void deactivate(Squad squad) {
    }
}
