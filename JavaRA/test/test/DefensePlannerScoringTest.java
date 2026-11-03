package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.DefensePlanner;
import cr0s.javara.entity.building.Defensive;
import cr0s.javara.entity.building.soviet.EntityFireTurret;
import cr0s.javara.entity.building.soviet.EntityTeslaCoil;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.paint.Color;

public class DefensePlannerScoringTest {
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
        ai.setSpawn(world.getMap().getSpawnPoints().get(0));
        return ai;
    }

    @Test
    public void threatSideCellsGetHigherMultiplier() {
        Pos base = new Pos(50, 50);
        Pos threat = new Pos(80, 50);

        double east = DefensePlanner.threatMultiplier(new Pos(60, 50), base, threat);
        double west = DefensePlanner.threatMultiplier(new Pos(40, 50), base, threat);

        assertTrue(east > west);
        assertEquals(1.5, east, 0.0001);
        assertEquals(1.0, west, 0.0001);
    }

    @Test
    public void symmetricThreatCenterDisablesThreatBias() {
        Pos base = new Pos(50, 50);

        assertEquals(1.0, DefensePlanner.threatMultiplier(new Pos(60, 50), base, base), 0.0001);
    }

    @Test
    public void spacingRejectsTooCloseAndPenalizesNearbyCells() {
        List<Pos> existing = List.of(new Pos(11, 10));

        assertEquals(0.0, DefensePlanner.spacingMultiplier(new Pos(10, 10), existing, 3), 0.0001);
        assertTrue(DefensePlanner.spacingMultiplier(new Pos(14, 10), existing, 3) < 1.0);
        assertEquals(1.05, DefensePlanner.spacingMultiplier(new Pos(17, 10), existing, 3), 0.0001);
        assertEquals(1.0, DefensePlanner.spacingMultiplier(new Pos(20, 10), List.of(), 3), 0.0001);
    }

    @Test
    public void teslaAndFlameTowerHaveDifferentRoleMultipliers() {
        Defensive fire = new EntityFireTurret(new Pos(0, 0));
        Defensive tesla = new EntityTeslaCoil(new Pos(0, 0));

        assertEquals(1.0, DefensePlanner.roleMultiplier(fire, 0, false), 0.0001);
        assertEquals(1.15, DefensePlanner.roleMultiplier(fire, 1, false), 0.0001);
        assertTrue(DefensePlanner.roleMultiplier(tesla, 0, true)
                > DefensePlanner.roleMultiplier(fire, 0, false));
    }

    @Test
    public void unknownPrototypeReturnsNull() {
        AIPlayer ai = newAi(new World("forest-path"));
        DefensePlanner planner = new DefensePlanner(ai);

        assertNull(planner.chooseCell("pbox"));
        assertNull(planner.chooseCell((Defensive) null));
    }

    @Test
    public void fireTurretPlacementIsBuildableAndWithinRadius() {
        World world = new World("forest-path");
        AIPlayer ai = newAi(world);
        DefensePlanner planner = new DefensePlanner(ai);
        Defensive prototype = new EntityFireTurret(new Pos(0, 0));

        Pos cell = planner.chooseCell(prototype);

        assertNotNull(cell);
        assertTrue(ai.getBase().isPossibleToBuildHere(cell,
                (cr0s.javara.entity.building.EntityBuilding) prototype));
        double distance = cell.distanceTo(ai.getPlayerSpawnPoint()) / 24.0;
        assertTrue(distance >= ai.defenseMinRadius - 1.0);
        assertTrue(distance <= ai.defenseMaxRadius + 1.0);
    }

    @Test
    public void secondDefenseRespectsMinimumSpacing() {
        World world = new World("forest-path");
        AIPlayer ai = newAi(world);
        DefensePlanner planner = new DefensePlanner(ai);

        Defensive first = new EntityFireTurret(new Pos(0, 0));
        Pos firstCell = planner.chooseCell(first);
        assertNotNull(firstCell);
        first.changeCellPos(firstCell);
        ai.getBase().addBuilding(first);

        Defensive second = new EntityFireTurret(new Pos(0, 0));
        Pos secondCell = planner.chooseCell(second);

        assertNotNull(secondCell);
        double spacing = secondCell.distanceTo(firstCell) / 24.0;
        assertTrue(spacing >= ai.defenseMinSpacing - 0.001);
    }

    @Test
    public void candidateApproachMapWeightsChokepointsHigher() {
        ArrayList<DefensePlanner.ApproachCell> cells = new ArrayList<>();
        cells.add(new DefensePlanner.ApproachCell(new Pos(1, 1), 3));
        cells.add(new DefensePlanner.ApproachCell(new Pos(2, 1), 8));

        assertTrue(cells.get(0).getWeight() > cells.get(1).getWeight());
    }
}
