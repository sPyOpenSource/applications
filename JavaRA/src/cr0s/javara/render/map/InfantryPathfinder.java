package cr0s.javara.render.map;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.infantry.EntityInfantry;
import cr0s.javara.render.World;
import javafx.scene.shape.Path;

/**
 * A* pathfinding class for vehicles.
 * @author Cr0s
 */
public class InfantryPathfinder {
    private final AStarPathFinder pathfinder;
    private static final int MAX_SEARCH_DISTANCE = 512;
    
    public InfantryPathfinder(World world) {
	this.pathfinder = new AStarPathFinder(world, MAX_SEARCH_DISTANCE, true);
    }
    
    public Path findPathFromTo(EntityInfantry me, int goalX, int goalY) {
	MobileEntity m = (MobileEntity) me;
	return this.pathfinder.findPath(me, (int) m.getCellPos().getX(), (int) m.getCellPos().getY(), goalX, goalY);
    }
}

class InfantryHeuristic {
    
    public static final float ADJACENT_COST = 1f;
    public static final float DIAGONAL_COST = (float)Math.sqrt(2);
    
    public float getCost(TileMap ctx, Mover mover, int x, int y,
	    int goalX, int goalY) {
	return Math.max(Math.abs(x - goalX), Math.abs(y - goalY));
    }

}
