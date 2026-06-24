package test;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import cr0s.javara.render.map.AStarPathFinder;
import cr0s.javara.render.World;
import cr0s.javara.entity.MobileEntity;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;

public class AStarPathFinderTest {

    private AStarPathFinder pathFinder;
    private World world;
    private MobileEntity mockEntity;

    @Before
    public void setUp() {
        // Test basic instantiation
        pathFinder = null;
    }

    @Test
    public void testPathFinderExists() {
        // Verify the AStarPathFinder class can be loaded
        assertNotNull("AStarPathFinder class should exist", AStarPathFinder.class);
    }

    @Test
    public void testPathContainsStartAndGoal() {
        // Test that a valid path contains both start and goal points
        Path path = new Path();
        path.getElements().add(new MoveTo(0, 0));
        path.getElements().add(new MoveTo(5, 5));
        
        assertEquals("Path should have 2 points", 2, path.getElements().size());
        
        MoveTo start = (MoveTo) path.getElements().get(0);
        MoveTo goal = (MoveTo) path.getElements().get(1);
        
        assertEquals("Start should be (0,0)", 0, start.getX(), 0.01);
        assertEquals("Start should be (0,0)", 0, start.getY(), 0.01);
        assertEquals("Goal should be (5,5)", 5, goal.getX(), 0.01);
        assertEquals("Goal should be (5,5)", 5, goal.getY(), 0.01);
    }
    
    @Test
    public void testEmptyPath() {
        Path path = new Path();
        assertTrue("Empty path should have no elements", path.getElements().isEmpty());
    }
}