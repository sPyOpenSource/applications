package cr0s.javara.ai.states;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.StateMachine.IState;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.order.Order;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;

public class GroundRetreatState extends GroundStateBase implements IState {

    @Override
    public void activate(Squad squad) {
        squad.clearTarget();
        issueRallyOrders(squad);
    }

    @Override
    public void tick(Squad squad) {
        if (!squad.isValid()) {
            return;
        }

        AIPlayer bot = squad.getBot();
        Pos spawn = bot.getPlayerSpawnPoint();
        if (spawn == null) {
            return;
        }

        issueRallyOrders(squad);

        if (allUnitsWithinRadius(squad, spawn, bot.baseRegenRadius)) {
            squad.getFsm().changeState(squad, new GroundRecoverState(), true);
        }
    }

    @Override
    public void deactivate(Squad squad) {
    }

    private void issueRallyOrders(Squad squad) {
        Pos spawn = squad.getBot().getPlayerSpawnPoint();
        if (spawn == null) {
            return;
        }

        for (EntityActor unit : squad.getUnits()) {
            if (!unit.isDead() && unit.owner == squad.getBot() && unit.isIdle()) {
                unit.resolveOrder(new Order("Move", unit, rallyCell(squad, spawn)));
            }
        }
    }

    private Pos rallyCell(Squad squad, Pos spawn) {
        for (int attempt = 0; attempt < 6; attempt++) {
            int dx = World.getRandomInt(-3, 4);
            int dy = World.getRandomInt(-3, 4);
            Pos candidate = new Pos(spawn.getCellX() + dx, spawn.getCellY() + dy);
            if (squad.getBot().world.isCellPassable(candidate)) {
                return candidate;
            }
        }
        return spawn;
    }

    private boolean allUnitsWithinRadius(Squad squad, Pos spawn, int radiusInCells) {
        double radius = radiusInCells * 24.0;
        for (EntityActor unit : squad.getUnits()) {
            if (!unit.isDead() && unit.getPosition().distanceTo(spawn) > radius) {
                return false;
            }
        }
        return true;
    }
}
