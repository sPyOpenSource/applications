package test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.entity.building.BuildingType;
import cr0s.javara.entity.building.soviet.EntityFireTurret;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.paint.Color;

public class AIPlayerDefensePlacementTest {
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
    public void defensivePlacementUsesPlannerForKnownDefense() {
        World world = new World("forest-path");
        AIPlayer ai = newAi(world);

        EntityFireTurret prototype = new EntityFireTurret(new Pos(0, 0));
        ai.getBase().getProductionQueue().getBuildables().put("fturicon.shp", prototype);

        Pos cell = ai.chooseBuildLocation("ftur", true, BuildingType.DEFENSIVE);

        assertNotNull(cell);
        assertTrue(ai.getBase().isPossibleToBuildHere(cell, prototype));
        double distance = cell.distanceTo(ai.getPlayerSpawnPoint()) / 24.0;
        assertTrue(distance >= ai.defenseMinRadius - 1.0);
        assertTrue(distance <= ai.defenseMaxRadius + 1.0);
    }

    @Test
    public void unknownDefenseFallsBackAndReturnsNullWithoutThrowing() {
        World world = new World("forest-path");
        AIPlayer ai = newAi(world);

        Pos cell = ai.chooseBuildLocation("pbox", true, BuildingType.DEFENSIVE);

        assertNull(cell);
    }

    @Test
    public void defensivePlacementWithoutSpawnReturnsNull() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);
        EntityFireTurret prototype = new EntityFireTurret(new Pos(0, 0));
        ai.getBase().getProductionQueue().getBuildables().put("fturicon.shp", prototype);

        assertNull(ai.chooseBuildLocation("ftur", true, BuildingType.DEFENSIVE));
    }
}
