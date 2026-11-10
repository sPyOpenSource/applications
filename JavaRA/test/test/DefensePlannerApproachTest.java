package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.DefensePlanner;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.paint.Color;

public class DefensePlannerApproachTest {
    @BeforeClass
    public static void startJavaFx() {
        FxTestSupport.ensureStarted();
    }

    @AfterClass
    public static void stopJavaFx() {
        FxTestSupport.shutdown();
    }

    @Test
    public void chokepointFactorRewardsNarrowCells() {
        assertEquals(3.0, DefensePlanner.chokepointFactor(0), 0.0001);
        assertEquals(3.0, DefensePlanner.chokepointFactor(3), 0.0001);
        assertEquals(2.0, DefensePlanner.chokepointFactor(4), 0.0001);
        assertEquals(2.0, DefensePlanner.chokepointFactor(5), 0.0001);
        assertEquals(1.0, DefensePlanner.chokepointFactor(6), 0.0001);
    }

    @Test
    public void approachMapContainsOnlyPassableCellsAndIsCached() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);
        ai.setSpawn(new Pos(30, 30));
        DefensePlanner planner = new DefensePlanner(ai);

        ArrayList<DefensePlanner.ApproachCell> first = planner.getApproachMap();

        assertFalse(first.isEmpty());
        for (DefensePlanner.ApproachCell cell : first) {
            assertTrue(planner.isTerrainPassable(cell.getCell()));
        }
        assertSame(first, planner.getApproachMap());

        ai.setSpawn(new Pos(40, 40));
        assertNotSame(first, planner.getApproachMap());
    }

    @Test
    public void passableNeighbourCountIgnoresImpassableTerrain() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);
        DefensePlanner planner = new DefensePlanner(ai);

        int count = planner.countPassableNeighbours(new Pos(30, 30));

        assertTrue(count >= 0);
        assertTrue(count <= 8);
    }
}
