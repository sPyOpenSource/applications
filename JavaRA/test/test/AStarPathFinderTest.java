package test;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import cr0s.javara.render.World;
import cr0s.javara.render.map.AStarPathFinder;
import cr0s.javara.util.Pos;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.render.EntityBlockingMap;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;

public class AStarPathFinderTest {

    private World world;
    private AStarPathFinder pathFinder;

    @Before
    public void setUp() {
        // Use an existing map to avoid TileMap loading errors
        world = new World("haos-ridges");
        // Clear the blocking map for controlled tests
        int[][] bMap = EntityBlockingMap.blockingMap;
        for (int[] bMap1 : bMap) {
            for (int j = 0; j < bMap1.length; j++) {
                bMap1[j] = 0; // 0 = passable
            }
        }
        pathFinder = new AStarPathFinder(world, 100);
    }

    @Test
    public void testSimplePath() {
        Pos start = new Pos(0, 0);
        Pos goal = new Pos(2, 0);
        
        // We need a MobileEntity because findPath requires one (though it's not used in my current impl)
        // Since MobileEntity might be complex, we can mock it or use a simple subclass
        MobileEntity me = new MobileEntity(new Pos(0,0),10f,10f) {
            @Override public boolean canEnterCell(Pos p) { return false; }
            @Override public int getMinimumEnoughRange() { return 0; }
            @Override public float getMoveSpeed() { return 0; }
            @Override public int getWaitSpreadTime() { return 0; }
            @Override public int getWaitAverageTime() { return 0; }
            @Override public StackPane renderEntity() { return null; }
            @Override public Activity moveToRange(Pos p, int i) { return null; }

            @Override
            public Path findPathFromTo(MobileEntity e, Pos aGoal) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }
        };

        Path path = pathFinder.findPath(me, start, goal);
        assertNotNull("Path should be found", path);
        assertEquals("Path should have 3 points (0,0), (1,0), (2,0)", 3, path.getElements().size());
    }

    @Test
    public void testObstacleAvoidance() {
        Pos start = new Pos(0, 0);
        Pos goal = new Pos(2, 0);
        
        // Place a wall at (1,0)
        EntityBlockingMap.blockingMap[1][0] = -1;

        MobileEntity me = new MobileEntity(new Pos(0,0),0,0) {
            @Override
            public Path findPathFromTo(MobileEntity e, Pos aGoal) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public float getMoveSpeed() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getMinimumEnoughRange() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public boolean canEnterCell(Pos cellPos) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getWaitAverageTime() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getWaitSpreadTime() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            protected Activity moveToRange(Pos cellPos, int range) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public StackPane renderEntity() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }
        };

        Path path = pathFinder.findPath(me, start, goal);
        assertNotNull("Path should be found by going around", path);
        
        // Path should be (0,0) -> (0,1) -> (1,1) -> (2,1) -> (2,0) or similar
        // Definitely should not contain (1,0)
        for (var element : path.getElements()) {
            MoveTo mt = (MoveTo) element;
            assertFalse("Path should not pass through wall at (1,0)", mt.getX() == 1 && mt.getY() == 0);
        }
    }

    @Test
    public void testNoPath() {
        Pos start = new Pos(0, 0);
        Pos goal = new Pos(2, 0);
        
        // Surround start with walls
        EntityBlockingMap.blockingMap[1][0] = -1;
        EntityBlockingMap.blockingMap[0][1] = -1;

        MobileEntity me = new MobileEntity(new Pos(0,0),0,0) {
            @Override
            public Path findPathFromTo(MobileEntity e, Pos aGoal) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public float getMoveSpeed() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getMinimumEnoughRange() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public boolean canEnterCell(Pos cellPos) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getWaitAverageTime() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getWaitSpreadTime() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            protected Activity moveToRange(Pos cellPos, int range) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public StackPane renderEntity() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }
        };

        Path path = pathFinder.findPath(me, start, goal);
        assertNull("Path should be null when blocked", path);
    }

    @Test
    public void testStartEqualsGoal() {
        Pos start = new Pos(5, 5);
        Pos goal = new Pos(5, 5);
        
        MobileEntity me = new MobileEntity(new Pos(0,0),0,0) {
            @Override
            public Path findPathFromTo(MobileEntity e, Pos aGoal) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public float getMoveSpeed() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getMinimumEnoughRange() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public boolean canEnterCell(Pos cellPos) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getWaitAverageTime() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public int getWaitSpreadTime() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            protected Activity moveToRange(Pos cellPos, int range) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public StackPane renderEntity() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }
        };

        Path path = pathFinder.findPath(me, start, goal);
        assertEquals("Path should be empty for start==goal", 0, path.getElements().size());
    }
}
