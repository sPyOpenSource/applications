package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.states.GroundCombatState;
import cr0s.javara.ai.states.GroundRetreatState;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.actor.activity.activities.Wait;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.entity.vehicle.common.EntityMcv;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.paint.Color;

public class AIPlayerCombatTest {
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

    private void addIdle(AIPlayer ai, EntityActor unit) {
        ai.getUnitsHangingAroundTheBase().add(unit);
    }

    @Test
    public void createAttackForceWaitsForSquadMass() {
        AIPlayer ai = newAi(new World("forest-path"));
        ai.squadSize = 3;
        addIdle(ai, newUnit(ai, 20, 20));
        addIdle(ai, newUnit(ai, 21, 20));

        ai.createAttackForce();

        assertTrue(ai.getSquads().isEmpty());

        for (int i = 0; i < 5; i++) {
            addIdle(ai, newUnit(ai, 22 + i, 20));
        }

        ai.createAttackForce();

        assertEquals(1, ai.getSquads().size());
        Squad squad = ai.getSquads().get(0);
        assertEquals(Squad.SquadType.ASSAULT, squad.getType());
        assertTrue(squad.getFsm().getCurrentState() instanceof GroundCombatState);
        assertEquals(7, squad.getLivingCount());
        assertTrue(ai.getUnitsHangingAroundTheBase().isEmpty());
    }

    @Test
    public void createAttackForceLeavesNonAttackUnitsIdle() {
        AIPlayer ai = newAi(new World("forest-path"));
        ai.squadSize = 2;
        for (int i = 0; i < 6; i++) {
            addIdle(ai, newUnit(ai, 20 + i, 20));
        }
        EntityMcv mcv = new EntityMcv(new Pos(30, 20));
        mcv.owner = ai;
        addIdle(ai, mcv);

        ai.createAttackForce();

        assertEquals(6, ai.getSquads().get(0).getLivingCount());
        assertEquals(1, ai.getUnitsHangingAroundTheBase().size());
        assertEquals(mcv, ai.getUnitsHangingAroundTheBase().get(0));
    }

    @Test
    public void tryToRushAttackSelectsAssaultSquadOnly() {
        World world = new World("forest-path");
        AIPlayer ai = newAi(world);
        AIPlayer enemy = new AIPlayer(world, "EnemyAI", Alignment.ALLIED, Color.BLUE);

        // Add the enemy directly to the world player list: world.addPlayer()
        // calls spawn(), which would create extra MCVs and tanks and make the
        // closest-enemy assertion nondeterministic.
        ai.setTeam(new Team());
        enemy.setTeam(new Team());
        world.getPlayers().add(enemy);

        EntityRifleTrooper target = newUnit(enemy, 100, 100);

        ai.squadSize = 2;
        for (int i = 0; i < 6; i++) {
            addIdle(ai, newUnit(ai, 20 + i, 20));
        }
        ai.createAttackForce();

        for (int i = 0; i < 4; i++) {
            addIdle(ai, newUnit(ai, 40 + i, 20));
        }
        ai.createExplorationSquad();

        Squad assault = ai.getSquadOfType(Squad.SquadType.ASSAULT);
        Squad exploration = ai.getSquadOfType(Squad.SquadType.EXPLORATION);
        assertNotNull(assault);
        assertNotNull(exploration);
        assertNotNull(target);

        ai.tryToRushAttack();

        assertTrue(assault.targetIsValid());
        assertTrue(assault.getTarget().getTargetEntity() == target);
        assertFalse(exploration.targetIsValid());
    }

    @Test
    public void tryToRushAttackSkipsRetreatingSquads() {
        World world = new World("forest-path");
        AIPlayer ai = newAi(world);
        AIPlayer enemy = new AIPlayer(world, "EnemyAI", Alignment.ALLIED, Color.BLUE);

        ai.setTeam(new Team());
        enemy.setTeam(new Team());
        world.getPlayers().add(enemy);

        EntityRifleTrooper target = newUnit(enemy, 100, 100);
        assertNotNull(target);

        ai.squadSize = 2;
        for (int i = 0; i < 6; i++) {
            addIdle(ai, newUnit(ai, 20 + i, 20));
        }
        ai.createAttackForce();

        Squad assault = ai.getSquadOfType(Squad.SquadType.ASSAULT);
        assault.getFsm().changeState(assault, new GroundRetreatState(), false);

        ai.tryToRushAttack();

        assertFalse(assault.targetIsValid());
    }

    @Test
    public void findNewUnitsIgnoresEnemyOwnedUnits() {
        AIPlayer ai = newAi(new World("forest-path"));
        AIPlayer enemy = new AIPlayer(new World("forest-path"), "EnemyAI", Alignment.ALLIED, Color.BLUE);

        EntityRifleTrooper own = newUnit(ai, 20, 20);
        EntityRifleTrooper foreign = new EntityRifleTrooper(new Pos(21, 20));
        foreign.owner = enemy;
        ai.entities.add(foreign);

        ai.findNewUnits();

        assertEquals(1, ai.getUnitsHangingAroundTheBase().size());
        assertTrue(ai.getUnitsHangingAroundTheBase().contains(own));
        assertFalse(ai.getUnitsHangingAroundTheBase().contains(foreign));
    }

    @Test
    public void registerHealsOnlyOwnIdleDamagedUnitsInRadius() {
        AIPlayer ai = newAi(new World("forest-path"));
        AIPlayer enemy = new AIPlayer(new World("forest-path"), "EnemyAI", Alignment.ALLIED, Color.BLUE);

        EntityRifleTrooper damaged = newUnit(ai, 10, 10);
        damaged.setHp(20);
        EntityRifleTrooper healthy = newUnit(ai, 11, 10);
        EntityRifleTrooper farAway = newUnit(ai, 50, 50);
        farAway.setHp(20);
        EntityRifleTrooper busy = newUnit(ai, 12, 10);
        busy.setHp(20);
        busy.queueActivity(new Wait(100));
        EntityRifleTrooper foreign = new EntityRifleTrooper(new Pos(13, 10));
        foreign.owner = enemy;
        foreign.setHp(20);

        ai.regenerateBaseUnits();

        assertEquals(30, damaged.getHp());
        assertEquals(50, healthy.getHp());
        assertEquals(20, farAway.getHp());
        assertEquals(20, busy.getHp());
        assertEquals(20, foreign.getHp());
    }

    @Test
    public void regenerationCapsAtMaxHp() {
        AIPlayer ai = newAi(new World("forest-path"));
        EntityRifleTrooper damaged = newUnit(ai, 10, 10);
        damaged.setHp(45);

        ai.regenerateBaseUnits();

        assertEquals(50, damaged.getHp());
    }

    @Test
    public void cleanSquadsRemovesEmptySquads() {
        AIPlayer ai = newAi(new World("forest-path"));
        Squad squad = ai.registerNewSquad(Squad.SquadType.ASSAULT);
        EntityRifleTrooper unit = newUnit(ai, 20, 20);
        squad.addUnit(unit);
        unit.setDead();

        ai.cleanSquads();

        assertTrue(ai.getSquads().isEmpty());
    }
}
