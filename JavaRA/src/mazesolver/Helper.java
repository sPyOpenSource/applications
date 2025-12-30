package mazesolver;

import cr0s.javara.util.Pos;
import java.util.*;

/**
 * Contains common used static methods used by the application.
 */
public class Helper {
    /**
     * Prints log messages to console.
     * @param s message
     */
    public static void log(String s) {
        if(Settings.Main.showLog) System.out.println("Log: " + s);
    }
    
    /**
     * Prints debug messages to console.
     * @param s message
     */
    public static void debug(String s) {
        if(Settings.Main.showDebug) System.out.println("Debug: " + s);
    }
    
    /**
     * Throws error.
     * @param s message
     */
    public static void error(String s) {
        throw new RuntimeException(s);
    }
    
    /**
     * Inverts random selection in array.
     * @param order
     * @return reversedOrder
     */
    public static int[] invert(int[] order) {
        int length = order.length;
        int[] order2 = order.clone();
        
        if(length < 4) return order2;

        int size = 1 + (int)(Math.random() * (length - 2));
        int index = 1 + (int)(Math.random() * (length - 1 - size));
        
        for(int i = 0; i < size; i++) {
            order2[index + i] = order[index+size-1 - i];
        }
        
        return order2;
    }
    
    /**
     * Swaps two random integers in order.
     * @param order
     * @return swappedOrder
     */
    public static int[] swap(int[] order) {
        int[] order2 = order.clone();
        
        if(order.length < 4) return order2;
        
        int x, y;
        
        x = 1 + (int) (Math.random() * (order.length-2));
        y = 1 + (int) (Math.random() * (order.length-2));

        int tmp = order2[x];
        order2[x] = order2[y];
        order2[y] = tmp;
        
        return order2;
    }
    
    /**
     * Shuffles order represented by integer array.
     * @param order
     * @return shuffledOrder
     */
    public static int[] shuffle(int[] order) {
        int[] order2 = order.clone();
        
        if(order.length < 4) return order2;
        
        for(int x = order.length - 2; x > 1; x--) {
            int i = 1 + (int) (Math.random() * x);
            
            int tmp = order2[x];
            order2[x] = order2[i];
            order2[i] = tmp;
        }
        return order2;
    }
    
    /**
     * Merges two orders by means of cross-over.
     * @param orderA
     * @param orderB
     * @return mergedOrder
     */
    public static int[] crossover(int[] orderA, int[] orderB) {
        // Get order length
        int length = orderA.length;
        
        // Early out if crossover would yield nothing
        if (length < 4) return orderA;

        // Determine random cut location and length
        int cutlength = 1 + (int)(Math.random() * (length - 2));
        int cutindex = 1 + (int)(Math.random() * (length - 1 - cutlength));
        
        // Create parent and child from given arrays      
        int[] parent = orderA.clone();
        int[] child = orderB.clone();
        
        // Loop through the parent and strikeout crossovers
        for(int i = 1; i < length-1; i++)
            for(int j = 0; j < cutlength; j++)
                if(parent[i] == child[cutindex + j])
                    parent[i] = -1;
        
        // Reloop through parent and add to child
        for(int i = 1, j = 1; i < length-1; i++) {
            if(j == cutindex)
                j += cutlength;
            
            if(parent[i] != -1)
                child[j++] = parent[i];
        }
        
        return child;
    }
    
    /**
     * Creates a step map from path.
     * @param path
     * @param width
     * @param height
     * @return map
     */
    public static int[][] mapPath(ArrayList<Pos> path, int width, int height) {
        // Initialize a step map
        int[][] map = new int[height][width];
        
        // Walk along path and fill map
        for (int i = 1; i < path.size(); i++) 
            map[path.get(i).getCellY()][path.get(i).getCellX()] = i;
        
        return map;
    }
    
    /**
     * Merges two paths. This algorithm finds the intersection points of both
     * paths. At each pair of intersection points, the algorithm finds the
     * shortest path between both points.
     * @param pathA
     * @param pathB
     * @param maze
     * @return newPath
     */
    public static ArrayList<Pos> mergePaths(
            ArrayList<Pos> pathA, ArrayList<Pos> pathB, Maze maze) {
        // Initialize new arraylist with expected capacity
        ArrayList<Pos> newPath = new ArrayList<>(pathA.size());

        // Create step map of B
        int[][] map = mapPath(pathB, maze.getWidth(), maze.getHeight());

        // Start at 0 for both
        int beginA = 0, beginB = 0;

        // Walk along path A
        for(int endA = 0; endA < pathA.size(); endA++) {
            
            // Find intersection in step map
            int endB = map[pathA.get(endA).getCellY()][pathA.get(endA).getCellX()];
            
            // If intersection is old, continue to next step
            if (endB <= beginB) continue;

            // Take the shortests of both paths
            if ((endB - beginB) < (endA - beginA))
                newPath.addAll(pathB.subList(beginB, endB));
            else
                newPath.addAll(pathA.subList(beginA, endA));

            // Store new intersection point as last
            beginA = endA; beginB = endB;
        }

        // Add end point of path
        newPath.add(pathA.get(pathA.size() - 1));
        
        return newPath;
    }
    
    /**
     * Cuts off loops in routes. This algorithm finds shortcuts in the given
     * path and removes detours.
     * @param path
     * @param maze
     */
    public static void optimizePath(ArrayList<Pos> path, Maze maze) {
        // Nothing to do here if no path!
        if(path == null || path.size() < 2) return;
        
        // Create a stepMap
        int[][] map = mapPath(path, maze.getWidth(), maze.getHeight());
        
        // Copy the path into oldPath
        ArrayList<Pos> oldPath = new ArrayList<>(path);
        
        // Clear path and add startpoint
        path.clear(); path.add(oldPath.get(0));
        
        // We start at 0
        int step = 0;
        
        // Walk along the old path
        while(step < oldPath.size() - 1) {
            
            // Get coordinates of current step
            int x = oldPath.get(step).getCellX();
            int y = oldPath.get(step).getCellY();
            
            // Look around for shortcuts
            if(maze.isNode(new Pos(x + 1, y)) && map[y][x + 1] > step) step = map[y][x + 1];
            if(maze.isNode(new Pos(x - 1, y)) && map[y][x - 1] > step) step = map[y][x - 1];
            if(maze.isNode(new Pos(x, y + 1)) && map[y + 1][x] > step) step = map[y + 1][x];
            if(maze.isNode(new Pos(x, y - 1)) && map[y - 1][x] > step) step = map[y - 1][x];
            
            // Add new step to path
            path.add(oldPath.get(step));
        }
    }
    
    /**
     * Creates a new length matrix.
     * @param matrix
     * @return matrix
     */
    public static int[][] createLengthMatrix(SubRoute[][] matrix) {
        int[][] lmatrix = new int[matrix.length][matrix.length];
        
        for (int y = 0; y < matrix.length-1; y++) {
            for (int x = y+1; x < matrix.length; x++) {
                
                // Get length from subroute
                int length = matrix[y][x].getPathLength();
                
                // If there is no path, exit
                if (length == -1) return null;
                
                lmatrix[y][x] = length;
                lmatrix[x][y] = length;
            }
        }
        return lmatrix;
    }
}
