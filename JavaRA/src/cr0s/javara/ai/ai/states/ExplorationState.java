package cr0s.javara.ai.ai.states;

import java.util.ArrayList;
import java.util.Collections;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.StateMachine.IState;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.order.Order;
import cr0s.javara.render.World;
import cr0s.javara.render.shrouds.Shroud;
import cr0s.javara.util.Pos;

public class ExplorationState implements IState {

    @Override
    public void activate(Squad squad) {}

    @Override
    public void tick(Squad squad) {
        if (!squad.isValid() || squad.getUnits().isEmpty()) {
            return;
        }
        if (squad.targetIsValid()) {
            return;
        }

        AIPlayer bot = squad.getBot();
        Pos dest = pickDestination(bot, squad.getUnits().get(0).getCellPosition());
        if (dest == null) {
            return;
        }

        for (EntityActor u : squad.getUnits()) {
            if (!u.isDead() && u.owner == bot && u.isIdle()) {
                u.resolveOrder(new Order("Move", u, dest, true));
            }
        }
    }

    @Override
    public void deactivate(Squad squad) {}

    private Pos pickDestination(AIPlayer bot, Pos nearCell) {
        Shroud shroud = bot.getShroud();
        int mapW = bot.world.getMap().getWidth();
        int mapH = bot.world.getMap().getHeight();
        Pos spawn = bot.getPlayerSpawnPoint();
        if (spawn == null) {
            spawn = nearCell;
        }

        ArrayList<Pos> candidates = new ArrayList<>();
        if (shroud != null) {
            for (int x = 0; x < mapW; x++) {
                for (int y = 0; y < mapH; y++) {
                    Pos c = new Pos(x, y);
                    if (!bot.world.isCellPassable(c)) {
                        continue;
                    }
                    if (shroud.isExplored(c)) {
                        continue;
                    }
                    if (c.distanceToSq(spawn) < 400) {
                        continue;
                    }
                    candidates.add(c);
                }
            }
        }
        if (candidates.isEmpty()) {
            for (int i = 0; i < 200; i++) {
                int x = World.getRandomInt(0, mapW);
                int y = World.getRandomInt(0, mapH);
                Pos c = new Pos(x, y);
                if (bot.world.isCellPassable(c)
                        && c.distanceToSq(spawn) >= 400) {
                    return c;
                }
            }
            return null;
        }
        Collections.shuffle(candidates, World.random);
        return candidates.get(0);
    }
}