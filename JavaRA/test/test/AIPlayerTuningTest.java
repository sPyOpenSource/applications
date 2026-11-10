package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import javafx.scene.paint.Color;

public class AIPlayerTuningTest {
    @BeforeClass
    public static void startJavaFx() {
        FxTestSupport.ensureStarted();
    }

    @AfterClass
    public static void stopJavaFx() {
        FxTestSupport.shutdown();
    }

    @Test
    public void normalProfileLoadsCombatAndDefenseTuning() {
        AIPlayer ai = new AIPlayer(new World("forest-path"), "NormalAI", Alignment.SOVIET, Color.RED);

        assertEquals(16, ai.squadSize);
        assertEquals(50, ai.retreatHealthPercent);
        assertEquals(40, ai.retreatCasualtyPercent);
        assertEquals(80, ai.reengageHealthPercent);
        assertEquals(4, ai.minAttackSquadSize);
        assertEquals(1800, ai.recoverTimeoutTicks);
        assertEquals(30, ai.baseRegenInterval);
        assertEquals(10, ai.baseRegenHp);
        assertEquals(20, ai.baseRegenRadius);
        assertEquals(5, ai.defenseMinRadius);
        assertEquals(20, ai.defenseMaxRadius);
        assertEquals(3, ai.defenseMinSpacing);
        assertEquals(2, ai.defenseOverlapTarget);
        assertEquals(35, ai.defenseThreatRadius);
        assertTrue(ai.idleBaseUnitsMaximum >= ai.squadSize + 4);
    }

    @Test
    public void rushProfileLoadsAggressiveTuning() {
        AIPlayer ai = new AIPlayer(new World("forest-path"), "RushAI", Alignment.SOVIET, Color.RED);

        assertEquals(20, ai.squadSize);
        assertEquals(40, ai.retreatHealthPercent);
        assertEquals(50, ai.retreatCasualtyPercent);
        assertEquals(70, ai.reengageHealthPercent);
        assertEquals(1, ai.defenseOverlapTarget);
        assertEquals(2, ai.defenseMinSpacing);
    }

    @Test
    public void turtleProfileLoadsDefensiveTuning() {
        AIPlayer ai = new AIPlayer(new World("forest-path"), "TurtleAI", Alignment.SOVIET, Color.RED);

        assertEquals(10, ai.squadSize);
        assertEquals(60, ai.retreatHealthPercent);
        assertEquals(30, ai.retreatCasualtyPercent);
        assertEquals(90, ai.reengageHealthPercent);
        assertEquals(5, ai.minAttackSquadSize);
        assertEquals(3, ai.defenseOverlapTarget);
        assertEquals(16, ai.defenseMaxRadius);
    }

    @Test
    public void missingProfileKeepsCodeDefaults() {
        AIPlayer ai = new AIPlayer(new World("forest-path"), "NoSuchAIProfile", Alignment.SOVIET, Color.RED);

        assertEquals(16, ai.squadSize);
        assertEquals(50, ai.retreatHealthPercent);
        assertEquals(40, ai.retreatCasualtyPercent);
        assertEquals(80, ai.reengageHealthPercent);
        assertEquals(4, ai.minAttackSquadSize);
        assertEquals(1800, ai.recoverTimeoutTicks);
        assertEquals(30, ai.baseRegenInterval);
        assertEquals(10, ai.baseRegenHp);
        assertEquals(20, ai.baseRegenRadius);
        assertEquals(5, ai.defenseMinRadius);
        assertEquals(20, ai.defenseMaxRadius);
        assertEquals(3, ai.defenseMinSpacing);
        assertEquals(2, ai.defenseOverlapTarget);
        assertEquals(35, ai.defenseThreatRadius);
    }
}
