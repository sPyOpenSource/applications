package cr0s.javara.ai.ai.states;

import cr0s.javara.ai.Squad;
import cr0s.javara.ai.StateMachine.IState;
import cr0s.javara.ai.states.GroundStateBase;
import cr0s.javara.ai.states.StateBase;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.render.World;

import java.util.ArrayList;

public class GroundUnitsIdleState extends GroundStateBase implements IState {

    @Override
    public void activate(Squad bot) {
	// TODO Auto-generated method stub
    }

    @Override
    public void tick(Squad owner) {
	if (!owner.isValid()) {
	    return;
	}

	if (!owner.targetIsValid()) {
	    if (!owner.getUnits().isEmpty()) {
		ArrayList<EntityActor> enemies = owner.getBot().findClosestEnemy(owner.getUnits().get(0).getPosition());
		if (enemies.isEmpty()) {
		    return;
		}
		EntityActor t = enemies.get(0);
		if (t == null) {
		    return;
		}

		owner.setTarget(t);
	    }
	}
    }

    @Override
    public void deactivate(Squad bot) {
	// TODO Auto-generated method stub
    }
}