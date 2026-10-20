package cr0s.javara.ai;

import cr0s.javara.entity.vehicle.common.EntityMcv;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;

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
}
