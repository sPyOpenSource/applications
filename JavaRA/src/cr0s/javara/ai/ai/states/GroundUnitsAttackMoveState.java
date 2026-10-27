package cr0s.javara.ai.ai.states;

import cr0s.javara.ai.Squad;
import cr0s.javara.ai.StateMachine.IState;
import cr0s.javara.ai.states.GroundStateBase;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.render.World;

import java.util.ArrayList;

public class GroundUnitsAttackMoveState extends GroundStateBase implements IState {

    @Override
    public void activate(Squad bot) {
    }

    @Override
    public void tick(Squad owner) {
	if (!owner.isValid()) {
	    return;
	}

	if (!owner.targetIsValid()) {
	    if (owner.getUnits().isEmpty()) {
		return;
	    }
	    EntityActor randomUnit = owner.getUnits().get(World.random.nextInt(owner.getUnits().size()));
	    ArrayList<EntityActor> enemies = owner.getBot().findClosestEnemy(randomUnit.getCellPosition());
	    if (enemies.isEmpty()) {
		return;
	    }
	    EntityActor closestEnemy = enemies.get(0);

	    if (closestEnemy != null) {
		owner.setTarget(closestEnemy);
	    } else {
		// TODO: owner.getFsm().changeState(owner, new GroundUnitsFleeState(), true);
		
		return;
	    }
	}
	
	// TODO: finish this
	//EntityActor leader = 
    }

    @Override
    public void deactivate(Squad bot) {
    }
}