package cr0s.javara.render.map;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.vehicle.EntityVehicle;
import cr0s.javara.render.World;
import javafx.scene.shape.Path;

/**
 * A* pathfinding class for vehicles.
 * @author Cr0s
 */
public class VehiclePathfinder {
    private final AStarPathFinder pathfinder;
    private static final int MAX_SEARCH_DISTANCE = 512;
    
    public VehiclePathfinder(World world) {
	this.pathfinder = new AStarPathFinder(world, MAX_SEARCH_DISTANCE, true);
    }
    
    public Path findPathFromTo(EntityVehicle me, int goalX, int goalY) {
	MobileEntity m = (MobileEntity) me;
	return this.pathfinder.findPath(me, (int) m.getCellPos().getX(), (int) m.getCellPos().getY(), goalX, goalY);
    }
}

class VehicleHeuristic {
    
    public static final float ADJACENT_COST = 1f;
    public static final float DIAGONAL_COST = (float)Math.sqrt(2);
    
    public float getCost(TileMap ctx, Mover mover, int x, int y,
	    int goalX, int goalY) {
	return Math.max(Math.abs(x - goalX), Math.abs(y - goalY));
	/*
	      float diagonal = Math.min(Math.abs(x - goalX), Math.abs(y - goalY));
	      float straight = (Math.abs(x - goalX) + Math.abs(y - goalY));
	      float h = (DIAGONAL_COST * diagonal) + (ADJACENT_COST * (straight - (2f * diagonal)));

	      return h;*/
    }
    
}