package cr0s.javara.ai.states;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.StateMachine.IState;

public class GroundRecoverState extends GroundStateBase implements IState {

    private int ticksActive;

    @Override
    public void activate(Squad squad) {
	this.ticksActive = 0;
	squad.getBot().reinforceSquad(squad);
    }

    @Override
    public void tick(Squad squad) {
	this.ticksActive++;

	AIPlayer bot = squad.getBot();
	bot.reinforceSquad(squad);

	if (!squad.isValid()) {
	    return;
	}

	int living = squad.getLivingCount();
	boolean healthyEnough = squad.healthRatio() >= bot.reengageHealthPercent / 100.0;
	boolean largeEnough = living >= bot.minAttackSquadSize;

	if (healthyEnough && largeEnough) {
	    squad.markRegrouped();
	    squad.getFsm().changeState(squad, new GroundCombatState(), true);
	    return;
	}

	if (!largeEnough && this.ticksActive >= bot.recoverTimeoutTicks) {
	    bot.disbandSquad(squad);
	}
    }

    @Override
    public void deactivate(Squad squad) {
    }
}
