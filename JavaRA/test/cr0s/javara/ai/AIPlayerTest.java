package cr0s.javara.ai;

import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.common.EntityConstructionYard;
import cr0s.javara.entity.vehicle.common.EntityMcv;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;

import java.util.ArrayList;

import javafx.scene.paint.Color;

import org.junit.Test;
import static org.junit.Assert.*;

public class AIPlayerTest {

    @Test
    public void thereIsNoMcvReturnsFalseWhenOwnedMcvExists() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);

        EntityMcv mcv = new EntityMcv(new Pos(0, 0));
        mcv.owner = ai;
        ai.entities.add(mcv);

        assertFalse("thereIsNoMcv() should return false when an owned MCV exists", ai.thereIsNoMcv());
    }

    @Test
    public void thereIsNoMcvReturnsTrueWhenNoMcv() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);

        assertTrue("thereIsNoMcv() should return true when no MCV exists", ai.thereIsNoMcv());
    }

    @Test
    public void thereIsNoMcvReturnsTrueWhenMcvIsDead() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);

        EntityMcv mcv = new EntityMcv(new Pos(0, 0));
        mcv.owner = null;             // not yet assigned an owner
        mcv.setDead();                // owner-null guard avoids touching World.root (null in tests)
        ai.entities.add(mcv);

        assertTrue("thereIsNoMcv() should return true when the only MCV is dead", ai.thereIsNoMcv());
    }

    @Test
    public void thereIsNoMcvReturnsTrueWhenMcvOwnedByOtherPlayer() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);
        AIPlayer other = new AIPlayer(world, "EnemyAI", Alignment.SOVIET, Color.RED);

        EntityMcv mcv = new EntityMcv(new Pos(0, 0));
        mcv.owner = other;
        ai.entities.add(mcv);

        assertTrue("thereIsNoMcv() should return true when MCV is owned by another player", ai.thereIsNoMcv());
    }

    @Test
    public void thereIsNoMcvReturnsTrueAndNoNpeWhenMcvHasNullOwner() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);

        EntityMcv mcv = new EntityMcv(new Pos(0, 0));
        // owner intentionally left null (MCV not yet assigned an owner)
        ai.entities.add(mcv);

        assertTrue("thereIsNoMcv() should return true (and not NPE) when MCV owner is null", ai.thereIsNoMcv());
    }

    @Test
    public void findEnemyConstructionYardsReturnsEnemyYardsOnly() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "TestAI", Alignment.SOVIET, Color.RED);
        AIPlayer enemy = new AIPlayer(world, "EnemyAI", Alignment.ALLIES, Color.BLUE);

        Team aiTeam = new Team();
        Team enemyTeam = new Team();
        ai.setTeam(aiTeam);
        enemy.setTeam(enemyTeam);

        world.addPlayer(ai);
        world.addPlayer(enemy);

        EntityConstructionYard friendly =
            new EntityConstructionYard(new Pos(0, 0));
        friendly.owner = ai;
        ai.entities.add(friendly);

        EntityConstructionYard enemyYard =
            new EntityConstructionYard(new Pos(100, 100));
        enemyYard.owner = enemy;
        enemy.entities.add(enemyYard);

        ArrayList<EntityBuilding> result = ai.findEnemyConstructionYards();

        assertEquals(1, result.size());
        assertEquals(enemyYard, result.get(0));
    }
}
