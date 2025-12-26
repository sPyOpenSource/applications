package mazesolver;

import java.util.*;

/**
 * Contains a SubRoute and uses ACO to determine fastest path.
 */
public class SubRoute {
    private final Maze maze;
    private final Point startPoint;
    private final Point endPoint;
    
    private ArrayList<Point> shortestPath;
    
    /**
     * Constructs a new subRoute, based on start- and endpoint.
     * @param maze
     * @param startPoint
     * @param endPoint 
     */
    public SubRoute(Maze maze, Point startPoint, Point endPoint) {
        this.maze = maze;
        
        // Check if start/end-points are valid
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }
    
    
    /**
     * Returns maze used by subRoute.
     * @return 
     */
    public Maze getMaze() { 
        return maze; 
    }
    
    /**
     * Returns startPoint of subRoute.
     * @return 
     */
    public Point getStartPoint() { 
        return startPoint; 
    }
    
    /**
     * Returns endPoint of subRoute.
     * @return 
     */
    public Point getEndPoint() { 
        return endPoint; 
    }

    /**
     * Gets shortest path.
     * @param reversed
     * @return null if no path available
     */
    public ArrayList<Point> getPath(boolean reversed) {
        // Create a temp arraylist
        ArrayList<Point> tmp;
        
        synchronized(this) {
            if(shortestPath == null) return null;
            
            // Make a copy of shortestPath
            tmp = new ArrayList<>(shortestPath);
        }
        
        // Reverse if needed
        if(reversed) Collections.reverse(tmp);
        
        return tmp;
    }
    
    /**
     * Updates subRoute.
     * @param path 
     */
    public void updatePath(ArrayList<Point> path) {
        // Optimize given path
        Helper.optimizePath(path, maze);
        
        synchronized(this) {
            // If there is no shortestPath, set it to current path
            if(shortestPath == null) {
                Helper.debug("new shortestPath!");
                shortestPath = path;
                return;
            }
        }
        
        // Create a a temp arraylist and get path
        ArrayList<Point> tmp = getPath(false);
        
        // Merge old and new path
        tmp = Helper.mergePaths(tmp, path, maze);
        
        // Optimize result
        Helper.optimizePath(tmp, maze);
        
        synchronized(this) {
            // Update shortestPath
            shortestPath = tmp;
        }
    }

    /**
     * Returns length of shortestPath.
     * @return 
     */
    synchronized public int getPathLength() {
        return (shortestPath == null) ? -1 : shortestPath.size() - 1;
    }
    
    /**
     * Creates new thread to find best paths.
     * @return 
     */
    public Thread findSubRoute() {
        // Return new logic thread
        return new Thread(new SubRouteLogic());
    }
    
    /**
     * Contains logic of a subRoute.
     */
    private class SubRouteLogic implements Runnable {
        @Override
        public void run() {
            // Announce thread start
            Helper.debug("Start thread " + Thread.currentThread().getName());
            
            // Create a new flock
            ArrayList<Ant> flock = new ArrayList<>(Settings.SubRoute.flockSize);
            
            // Fill the flock with new ants
            for (int i = 0; i < Settings.SubRoute.flockSize; i++)
                flock.add(new Ant(SubRoute.this));
            
            // Run ants, run!
            for (int i = 0; i < Settings.Ant.maxSteps; i++) {
                for (int j = 0; j < flock.size(); ) {
                    Ant ant = flock.get(j);
                    
                    // Let the ant do a step
                    boolean finished = ant.doStep();

                    // If the ant has finished
                    if (finished) {
                        // Update the shortestPath
                        updatePath(ant.getPath());

                        // Remove ant from flock
                        flock.remove(ant);
                    } else j++;
                }
            }
            
            // Announce thread release
            Helper.debug("Finished thread " + Thread.currentThread().getName());
        }
    }
}
