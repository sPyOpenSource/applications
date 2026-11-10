package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.states.GroundCombatState;
import cr0s.javara.ai.states.GroundRecoverState;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.entity.vehicle.common.EntityMcv;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.paint.Color;

public class GroundRecoverStateTest {
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
    public void recoverAbsorbsIdleAttackUnitsUpToDesiredSize() {
        AIPlayer ai = newAi(new World("forest-path"));
        ai.squadSize = 5;
        ai.reengageHealthPercent = 100;
        ai.minAttackSquadSize = 1;

        Squad squad = ai.registerNewSquad(Squad.SquadType.ASSAULT);
        squad.addUnit(newUnit(ai, 10, 10));

        for (int i = 0; i < 20; i++) {
            ai.getUnitsHangingAroundTheBase().add(newUnit(ai, 20 + i, 20));
        }

        squad.getFsm().changeState(squad, new GroundRecoverState(), false);
        squad.update();

        assertEquals(5, squad.getLivingCount());
        assertEquals(16, ai.getUnitsHangingAroundTheBase().size());
        assertTrue(squad.getFsm().getCurrentState() instanceof GroundCombatState);
    }

    @Test
    public void recoverIgnoresNonAttackAndEnemyUnits() {
        AIPlayer ai = newAi(new World("forest-path"));
        AIPlayer enemy = newAi(new World("forest-path"));
        ai.squadSize = 3;
        ai.minAttackSquadSize = 1;

        Squad squad = ai.registerNewSquad(Squad.SquadType.ASSAULT);
        squad.addUnit(newUnit(ai, 10, 10));

        EntityMcv mcv = new EntityMcv(new Pos(20, 20));
        mcv.owner = ai;
        ai.getUnitsHangingAroundTheBase().add(mcv);

        EntityRifleTrooper enemyUnit = new EntityRifleTrooper(new Pos(21, 20));
        enemyUnit.owner = enemy;
        ai.getUnitsHangingAroundTheBase().add(enemyUnit);

        squad.getFsm().changeState(squad, new GroundRecoverState(), false);
        squad.update();

        assertEquals(1, squad.getLivingCount());
        assertTrue(ai.getUnitsHangingAroundTheBase().contains(mcv));
        assertTrue(ai.getUnitsHangingAroundTheBase().contains(enemyUnit));
    }

    @Test
    public void recoverReturnsToCombatWhenHealthyAndLargeEnough() {
        AIPlayer ai = newAi(new World("forest-path"));
        ai.squadSize = 2;
        ai.minAttackSquadSize = 2;
        ai.reengageHealthPercent = 80;

        Squad squad = ai.registerNewSquad(Squad.SquadType.ASSAULT);
        squad.addUnit(newUnit(ai, 10, 10));
        squad.addUnit(newUnit(ai, 11, 10));

        squad.getFsm().changeState(squad, new GroundRecoverState(), false);
        squad.update();

        assertTrue(squad.getFsm().getCurrentState() instanceof GroundCombatState);
        assertEquals(2, squad.getInitialStrength());
    }

    @Test
    public void recoverDisbandsAfterTimeoutWhenBelowMinimumSize() {
        AIPlayer ai = newAi(new World("forest-path"));
        ai.squadSize = 10;
        ai.minAttackSquadSize = 4;
        ai.recoverTimeoutTicks = 2;

        Squad squad = ai.registerNewSquad(Squad.SquadType.ASSAULT);
        EntityRifleTrooper survivor = newUnit(ai, 10, 10);
        squad.addUnit(survivor);

        squad.getFsm().changeState(squad, new GroundRecoverState(), false);
        squad.update();
        squad.update();

        assertFalse(ai.getSquads().contains(squad));
        assertTrue(ai.getUnitsHangingAroundTheBase().contains(survivor));
    }

    @Test
    public void reinforcementDoesNotExceedDesiredSize() {
        AIPlayer ai = newAi(new World("forest-path"));
        ai.squadSize = 3;

        Squad squad = ai.registerNewSquad(Squad.SquadType.ASSAULT);
        squad.addUnit(newUnit(ai, 10, 10));
        for (int i = 0; i < 10; i++) {
            ai.getUnitsHangingAroundTheBase().add(newUnit(ai, 20 + i, 20));
        }

        ai.reinforceSquad(squad);

        assertEquals(3, squad.getLivingCount());
        assertEquals(8, ai.getUnitsHangingAroundTheBase().size());
        assertNotNull(squad.getUnits().get(0));
    }
}
