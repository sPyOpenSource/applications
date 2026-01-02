package cr0s.javara.entity.actor.activity.activities.harvester;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.actor.activity.activities.Move;
import cr0s.javara.entity.vehicle.common.EntityHarvester;
import cr0s.javara.util.Pos;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class FindResources extends Activity {

    public FindResources() {
    }

    @Override
    public Activity tick(EntityActor a) {
	if (isCancelled || !(a instanceof EntityHarvester)) {
	    return nextActivity;
	}

	MobileEntity me = (MobileEntity) a;
	EntityHarvester harv = (EntityHarvester) a;

	Path pathToResource = findPathToClosestResourceCell(harv);
	Move moveActivity;

	if (pathToResource != null) {
	    if (harv.isFull()) {
		DeliverResources deliverActivity = new DeliverResources();
		deliverActivity.queueActivity(nextActivity);

		return deliverActivity;
	    }
            MoveTo goal = (MoveTo)pathToResource.getElements().get(pathToResource.getElements().size() - 1);
	    moveActivity = new Move(me, pathToResource, new Pos(goal.getX(), goal.getY()), null);
	} else {
	    if (!harv.isEmpty()) {
		DeliverResources deliverActivity = new DeliverResources();
		deliverActivity.queueActivity(nextActivity);

		return deliverActivity;
	    }

	    return nextActivity;
	}

	moveActivity.queueActivity(new HarvestResource());
	moveActivity.queueActivity(nextActivity);
	return moveActivity;
    }

    private Pos getUnvisitedChildNode(Set<Pos> visited, Pos currentNode,
	    EntityHarvester harv) {
	// All possible adjacent cells directions
	int dx[] = { +1, -1,  0,  0, +1, -1, +1, -1 };
	int dy[] = {  0,  0, +1, -1, +1, +1, -1, -1 };

	for (int i = 0; i < 8; i++) {
	    int newCellX = currentNode.getCellX() + dx[i];
	    int newCellY = currentNode.getCellY() + dy[i];

	    Pos cell = new Pos(newCellX, newCellY);
if(!harv.world.getMap().isInMap(cell)) continue;
	    /*if (!harv.world.isCellPassable(cell)) {
		continue;
	    }*/

	    if (!visited.contains(cell)) {
		return cell;
	    }
	}

	return null;
    }

    /**
     * Searches closest cell with resources using Breadth-First Search
     * @param harv
     * @return
     */
    private Pos getClosestResourceCellBfs(final EntityHarvester harv) {
	int searchRadius = (harv.lastOrderPoint != null || harv.lastHarvestedPoint != null) ? EntityHarvester.SEARCH_RADIUS_FROM_ORDER
		: EntityHarvester.SEARCH_RADIUS_FROM_PROC;
	final Pos centerPos = (harv.lastOrderPoint != null) ? harv.lastOrderPoint
		: (harv.lastHarvestedPoint != null) ? harv.lastHarvestedPoint
			: harv.getCellPos();

	if (isCellChoosable(harv, centerPos)) {
	    return centerPos;
	}

	Queue<Pos> queue = new LinkedList<>();
	queue.add(centerPos);

	Set<Pos> visited = new HashSet<>();
	visited.add(centerPos);

	while (!queue.isEmpty()) {
	    Pos node = (Pos) queue.remove();
	    Pos child;
	    while ((child = getUnvisitedChildNode(visited, node, harv)) != null) {
		visited.add(child);
//harv.setPos(child);
                /*try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    System.getLogger(FindResources.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }*/
		if (isCellChoosable(harv, child)) {
		    return child;
		}

		queue.add(child);
	    }
	}

	return null;
    }

    private boolean isCellChoosable(EntityHarvester harv, Pos cellPos) {
	/*boolean shroudObscures = harv.owner.getShroud() != null
		&& !harv.owner.getShroud().isExplored(cellPos);*/
	boolean isHarvesterPoint = cellPos == harv.getCellPos();

	return //!shroudObscures && 
                //(isHarvesterPoint || harv.world.isCellPassable(cellPos)) && 
                !harv.world.getMap().getResourcesLayer()
			.isCellEmpty(cellPos);
    }

    private Path findPathToClosestResourceCell(final EntityHarvester harv) {
	Pos resourcePos = getClosestResourceCellBfs(harv);
	if (resourcePos != null) {
	    Path pathToResource = harv.findPathFromTo(harv, resourcePos);

	    return pathToResource;
	}

	return null;
    }
}
