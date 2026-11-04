/*
 * Copyright (C) 2026 xuyi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cr0s.javara.ai.states;

import cr0s.javara.ai.Squad;
import cr0s.javara.ai.StateMachine;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.render.World;

/**
 *
 * @author xuyi
 */
public class GroundUnitsAttackMoveState extends GroundStateBase implements StateMachine.IState {

    @Override
    public void activate(Squad bot) {

    }

    @Override
    public void tick(Squad owner) {
	if (!owner.isValid()) {
	    return;
	}

	if (!owner.targetIsValid()) {
	    EntityActor randomUnit = owner.getUnits().get(World.random.nextInt(owner.getUnits().size()));
	    EntityActor closestEnemy = owner.getBot().findClosestEnemy(randomUnit.getCellPosition()).get(0);

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
