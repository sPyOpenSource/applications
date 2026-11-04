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

/**
 *
 * @author xuyi
 */
public class GroundUnitsIdleState extends GroundStateBase implements StateMachine.IState {

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
		EntityActor t = owner.getBot().findClosestEnemy(owner.getUnits().get(0).getPosition()).get(0);
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
