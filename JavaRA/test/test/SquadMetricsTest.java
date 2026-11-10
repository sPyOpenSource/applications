package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.paint.Color;

public class SquadMetricsTest {
    @BeforeClass
    public static void startJavaFx() {
        FxTestSupport.ensureStarted();
    }

    @AfterClass
    public static void stopJavaFx() {
        FxTestSupport.shutdown();
    }

    private AIPlayer newAi(World world) {
        return new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);
    }

    private EntityRifleTrooper newUnit(AIPlayer ai, int cellX, int cellY) {
        EntityRifleTrooper unit = new EntityRifleTrooper(new Pos(cellX, cellY));
        unit.owner = ai;
        ai.entities.add(unit);
        return unit;
    }

    private Squad newSquad(AIPlayer ai) {
        return new Squad(ai, Squad.SquadType.ASSAULT);
    }

    @Test
    public void healthRatioUsesLivingUnitsAndMaxHp() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = newSquad(ai);
        EntityRifleTrooper a = newUnit(ai, 10, 10);
        EntityRifleTrooper b = newUnit(ai, 11, 10);
        squad.addUnit(a);
        squad.addUnit(b);

        assertEquals(1.0, squad.healthRatio(), 0.0001);

        a.setHp(25);
        assertEquals(0.75, squad.healthRatio(), 0.0001);
    }

    @Test
    public void deadUnitsDoNotContributeToHealthRatio() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = newSquad(ai);
        EntityRifleTrooper a = newUnit(ai, 10, 10);
        EntityRifleTrooper b = newUnit(ai, 11, 10);
        squad.addUnit(a);
        squad.addUnit(b);

        b.setDead();
        squad.removeDeadAndNotOwnUnits();

        assertEquals(1.0, squad.healthRatio(), 0.0001);
    }

    @Test
    public void casualtyRatioUsesInitialStrength() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = newSquad(ai);
        EntityRifleTrooper a = newUnit(ai, 10, 10);
        EntityRifleTrooper b = newUnit(ai, 11, 10);
        EntityRifleTrooper c = newUnit(ai, 12, 10);
        EntityRifleTrooper d = newUnit(ai, 13, 10);
        squad.addUnit(a);
        squad.addUnit(b);
        squad.addUnit(c);
        squad.addUnit(d);

        a.setDead();
        squad.removeDeadAndNotOwnUnits();

        assertEquals(3, squad.getLivingCount());
        assertEquals(0.25, squad.casualtyRatio(), 0.0001);
    }

    @Test
    public void squadBelowHealthThresholdShouldRetreat() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = newSquad(ai);
        EntityRifleTrooper a = newUnit(ai, 10, 10);
        EntityRifleTrooper b = newUnit(ai, 11, 10);
        squad.addUnit(a);
        squad.addUnit(b);

        a.setHp(25);
        b.setHp(25);

        assertEquals(0.5, squad.healthRatio(), 0.0001);
        assertTrue(squad.shouldRetreat());
    }

    @Test
    public void squadBelowCasualtyThresholdShouldRetreat() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = newSquad(ai);
        for (int i = 0; i < 5; i++) {
            squad.addUnit(newUnit(ai, 10 + i, 10));
        }

        squad.getUnits().get(0).setDead();
        squad.getUnits().get(1).setDead();
        squad.removeDeadAndNotOwnUnits();

        assertEquals(0.4, squad.casualtyRatio(), 0.0001);
        assertTrue(squad.shouldRetreat());
    }

    @Test
    public void healthySquadShouldNotRetreat() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = newSquad(ai);
        squad.addUnit(newUnit(ai, 10, 10));
        squad.addUnit(newUnit(ai, 11, 10));

        assertFalse(squad.shouldRetreat());
    }

    @Test
    public void markRegroupedResetsCasualtyBaseline() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = newSquad(ai);
        for (int i = 0; i < 4; i++) {
            squad.addUnit(newUnit(ai, 10 + i, 10));
        }

        squad.getUnits().get(0).setDead();
        squad.getUnits().get(1).setDead();
        squad.removeDeadAndNotOwnUnits();
        assertEquals(0.5, squad.casualtyRatio(), 0.0001);

        squad.markRegrouped();

        assertEquals(0.0, squad.casualtyRatio(), 0.0001);
        assertFalse(squad.shouldRetreat());
    }

    @Test
    public void updatePrunesDeadAndForeignUnits() {
        AIPlayer ai = newAi(new World("forest-path"));
        AIPlayer other = newAi(new World("forest-path"));
        Squad squad = newSquad(ai);
        EntityRifleTrooper alive = newUnit(ai, 10, 10);
        EntityRifleTrooper dead = newUnit(ai, 11, 10);
        EntityRifleTrooper foreign = new EntityRifleTrooper(new Pos(12, 10));
        foreign.owner = other;
        squad.addUnit(alive);
        squad.addUnit(dead);
        squad.addUnit(foreign);

        dead.setDead();
        squad.update();

        assertEquals(1, squad.getUnits().size());
        assertEquals(alive, squad.getUnits().get(0));
    }
}
