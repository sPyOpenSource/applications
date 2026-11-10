package test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.states.GroundCombatState;
import cr0s.javara.ai.states.GroundRecoverState;
import cr0s.javara.ai.states.GroundRetreatState;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.paint.Color;

public class GroundCombatStateTest {
    @BeforeClass
    public static void startJavaFx() {
        FxTestSupport.ensureStarted();
    }

    @AfterClass
    public static void stopJavaFx() {
        FxTestSupport.shutdown();
    }

    private AIPlayer newAi(World world) {
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);
        ai.setSpawn(new Pos(10, 10));
        return ai;
    }

    private EntityRifleTrooper newUnit(AIPlayer ai, int cellX, int cellY) {
        EntityRifleTrooper unit = new EntityRifleTrooper(new Pos(cellX, cellY));
        unit.owner = ai;
        ai.entities.add(unit);
        return unit;
    }

    @Test
    public void losingSquadTransitionsFromCombatToRetreat() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = new Squad(ai, Squad.SquadType.ASSAULT);
        EntityRifleTrooper a = newUnit(ai, 40, 40);
        EntityRifleTrooper b = newUnit(ai, 41, 40);
        a.setHp(20);
        b.setHp(20);
        squad.addUnit(a);
        squad.addUnit(b);

        squad.getFsm().changeState(squad, new GroundCombatState(), false);
        squad.update();

        assertTrue(squad.getFsm().getCurrentState() instanceof GroundRetreatState);
        assertFalse(squad.targetIsValid());
    }

    @Test
    public void healthySquadWithoutEnemiesStaysInCombat() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = new Squad(ai, Squad.SquadType.ASSAULT);
        squad.addUnit(newUnit(ai, 40, 40));
        squad.addUnit(newUnit(ai, 41, 40));

        squad.getFsm().changeState(squad, new GroundCombatState(), false);
        squad.update();

        assertTrue(squad.getFsm().getCurrentState() instanceof GroundCombatState);
    }

    @Test
    public void retreatAtBaseTransitionsToRecover() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = new Squad(ai, Squad.SquadType.ASSAULT);
        squad.addUnit(newUnit(ai, 10, 10));
        squad.addUnit(newUnit(ai, 11, 10));

        squad.getFsm().changeState(squad, new GroundRetreatState(), false);
        squad.update();

        assertTrue(squad.getFsm().getCurrentState() instanceof GroundRecoverState);
    }

    @Test
    public void retreatClearsTarget() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = new Squad(ai, Squad.SquadType.ASSAULT);
        EntityRifleTrooper a = newUnit(ai, 40, 40);
        squad.addUnit(a);
        squad.setTarget(a);

        assertTrue(squad.targetIsValid());

        squad.getFsm().changeState(squad, new GroundRetreatState(), false);

        assertFalse(squad.targetIsValid());
    }
}
