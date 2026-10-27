package test;

import cr0s.javara.ai.Squad;
import cr0s.javara.ai.states.GroundStateBase;
import cr0s.javara.ai.states.GroundStateBase.GroundUnitsIdleState;
import cr0s.javara.ai.states.GroundStateBase.GroundUnitsAttackMoveState;
import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.StateMachine;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import java.util.ArrayList;
import javafx.scene.paint.Color;

import org.junit.Test;
import static org.junit.Assert.*;

public class GroundStateBaseTest {
    @Test
    public void idleStateNoEnemyDoesNotCrash() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        Squad squad = new Squad(ai, Squad.SquadType.ASSAULT);
        GroundUnitsIdleState state = new GroundStateBase.GroundUnitsIdleState();
        state.activate(squad);
        state.tick(squad);
        state.deactivate(squad);
    }

    @Test
    public void attackMoveStateNoEnemyDoesNotCrash() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        Squad squad = new Squad(ai, Squad.SquadType.ASSAULT);
        GroundUnitsAttackMoveState state = new GroundStateBase.GroundUnitsAttackMoveState();
        state.activate(squad);
        state.tick(squad);
        state.deactivate(squad);
    }
}