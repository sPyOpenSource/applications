package test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import java.util.ArrayList;
import javafx.scene.paint.Color;

import org.junit.Test;
import static org.junit.Assert.*;

public class AIPlayerFindClosestEnemyTest {
    @Test
    public void findClosestEnemyReturnsNearestAcrossAllEnemyPlayers() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        AIPlayer nearEnemy = new AIPlayer(world, "Near", Alignment.ALLIED, Color.BLUE);
        AIPlayer farEnemy = new AIPlayer(world, "Far", Alignment.ALLIED, Color.GREEN);

        Team aiTeam = new Team(); Team nearTeam = new Team(); Team farTeam = new Team();
        ai.setTeam(aiTeam); nearEnemy.setTeam(nearTeam); farEnemy.setTeam(farTeam);
        world.addPlayer(ai); world.addPlayer(nearEnemy); world.addPlayer(farEnemy);

        EntityRifleTrooper far = new EntityRifleTrooper(new Pos(100, 0));
        far.owner = farEnemy; farEnemy.entities.add(far);
        EntityRifleTrooper near = new EntityRifleTrooper(new Pos(10, 0));
        near.owner = nearEnemy; nearEnemy.entities.add(near);

        ArrayList<EntityActor> result = ai.findClosestEnemy(new Pos(0, 0));

        assertEquals(2, result.size());
        assertEquals(near, result.get(0));
        assertEquals(far, result.get(1));
    }

    @Test
    public void findClosestEnemyNoEnemiesReturnsEmpty() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        ArrayList<EntityActor> result = ai.findClosestEnemy(new Pos(0, 0));
        assertTrue(result.isEmpty());
    }
}
