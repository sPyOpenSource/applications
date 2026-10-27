package test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.states.ExplorationState;
import cr0s.javara.ai.StateMachine;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import cr0s.javara.gameplay.Team.Alignment;
import javafx.scene.paint.Color;

import org.junit.Test;
import static org.junit.Assert.*;

public class AIPlayerExplorationTest {
    @Test
    public void explorationSquadIsCreatedFromIdleUnits() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);

        EntityRifleTrooper u1 = new EntityRifleTrooper(new Pos(5, 5));
        u1.owner = ai;
        ai.entities.add(u1);
        ai.getUnitsHangingAroundTheBase().add(u1);

        ai.createExplorationSquad();

        Squad s = ai.getSquadOfType(Squad.SquadType.EXPLORATION);
        assertNotNull("exploration squad should exist", s);
        assertEquals(1, s.getUnits().size());
        StateMachine.IState st = s.getFsm().getCurrentState();
        assertTrue("FSM should be in ExplorationState", st instanceof ExplorationState);
    }

    @Test
    public void explorationTickIssuesMoveOrdersWithoutShroud() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);

        EntityRifleTrooper u1 = new EntityRifleTrooper(new Pos(5, 5));
        u1.owner = ai;
        ai.entities.add(u1);
        ai.getUnitsHangingAroundTheBase().add(u1);
        ai.createExplorationSquad();

        Squad s = ai.getSquadOfType(Squad.SquadType.EXPLORATION);
        s.update(); // must not throw
    }
}
