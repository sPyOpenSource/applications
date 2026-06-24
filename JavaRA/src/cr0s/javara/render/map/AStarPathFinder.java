package cr0s.javara.render.map;

import cr0s.javara.entity.MobileEntity;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;

/**
 * A proper A* Pathfinding implementation using Manhattan distance heuristic.
 * Replaces the inefficient Ant Colony Optimization simulation.
 * 
 * @author opencode
 */
public class AStarPathFinder {
    
    private final World world;
    private final int maxSearchDistance;

    public AStarPathFinder(World world, int MAX_SEARCH_DISTANCE) {
        this.world = world;
        this.maxSearchDistance = MAX_SEARCH_DISTANCE;
    }

    private static class Node implements Comparable<Node> {
        final int x, y;
        int gCost = Integer.MAX_VALUE;
        int hCost;
        Node parent;

        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int getFCost() {
            return gCost + hCost;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.getFCost(), other.getFCost());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node node = (Node) o;
            return x == node.x && y == node.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public Path findPath(MobileEntity me, Pos startPos, Pos goalPos) {
        int startX = startPos.getCellX();
        int startY = startPos.getCellY();
        int goalX = goalPos.getCellX();
        int goalY = goalPos.getCellY();

        if (startX == goalX && startY == goalY) {
            return new Path();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Integer, Node> allNodes = new HashMap<>();

        Node startNode = new Node(startX, startY);
        startNode.gCost = 0;
        startNode.hCost = Math.abs(startX - goalX) + Math.abs(startY - goalY);
        
        openSet.add(startNode);
        allNodes.put(startX * 10000 + startY, startNode);

        int[][] blockingMap = world.blockingEntityMap.blockingMap;
        int mapW = blockingMap.length;
        int mapH = (mapW > 0) ? blockingMap[0].length : 0;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(current);
            }

            // Neighbors (4-connectivity)
            int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] dir : dirs) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                if (nx < 0 || nx >= mapW || ny < 0 || ny >= mapH) continue;
                
                // Obstacle check: in this engine, -1 typically means occupied/blocked
                if (blockingMap[nx][ny] == -1) continue;

                int newGCost = current.gCost + 1;
                int nodeKey = nx * 10000 + ny;
                Node neighbor = allNodes.getOrDefault(nodeKey, new Node(nx, ny));

                if (newGCost < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = newGCost;
                    neighbor.hCost = Math.abs(nx - goalX) + Math.abs(ny - goalY);
                    
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                    allNodes.put(nodeKey, neighbor);
                }
            }
            
            // Safety break to prevent infinite loop or excessive search
            if (allNodes.size() > maxSearchDistance * maxSearchDistance) break;
        }

        return null; // Return null if no route found
    }

    private Path reconstructPath(Node endNode) {
        ArrayList<Pos> pathPoints = new ArrayList<>();
        Node curr = endNode;
        while (curr != null) {
            pathPoints.add(0, new Pos(curr.x, curr.y));
            curr = curr.parent;
        }

        Path path = new Path();
        for (Pos p : pathPoints) {
            path.getElements().add(new MoveTo(p.getX(), p.getY()));
        }
        return path;
    }
}
