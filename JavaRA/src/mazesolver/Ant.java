package mazesolver;

import java.util.ArrayList;

/**
 * Implementation of a simple Ant.
 */
public class Ant implements Runnable {    
    
    private final ArrayList<Point> path;
    private final int[][] pathmap;
    private final SubRoute subRoute;
    
    /**
     * Creates new Ant for finding a path between the points of subRoute.
     * @param subRoute 
     */
    public Ant(SubRoute subRoute) {
        this.subRoute = subRoute;
        
        path = new ArrayList<>();
        path.add(subRoute.getStartPoint());
        
        pathmap = new int
                [subRoute.getMaze().getHeight()]
                [subRoute.getMaze().getWidth()];
    }
    
    /**
     * Ant determines next step and takes it.
     * @return true if at end point
     */
    public boolean doStep() {
        // If at endPoint (happens if two PTV at same location)
        if(subRoute.getEndPoint().equals(currentPoint()))
            return true;
        
        // Choose next point to move
        Point next = nextPoint();
        
        // Check if point is valid
        if(next == null) return false;
        
        // Add point to route
        path.add(next);

        // Update stepMap
        pathmap[next.getY()][next.getX()] = 1;
        int value = /*subRoute.getMaze().getValue(next.getX(), next.getY()) -*/ 5;
        subRoute.getMaze().setValue(next.getX(), next.getY(), value);
        return false;
    }
    
    /**
     * Determines which of the 4 surrounding nodes the Ant should take.
     * @return 
     */
    public Point nextPoint() {
        // Store current x and y coordinates
        int x = currentPoint().getX();
        int y = currentPoint().getY();
        
        // Create 4 surrounding locations
        Point[] points = new Point[4];
        points[0] = new Point(x, y - 1);
        points[1] = new Point(x - 1, y);
        points[2] = new Point(x + 1, y);
        points[3] = new Point(x, y + 1);
        
        // Check each location with maze/path
        float[] w = new float[4];
        for(byte i = 0; i < 4; i++) {
            w[i] = subRoute.getMaze().getValue(points[i].getX(), points[i].getY());
            /*if(w[i] != 0){
                /*if(points[i].equals(previousPoint()))
                    w[i] = Settings.Ant.factorReverse;
                else if(pathmap[points[i].getY()][points[i].getX()] == 1000)
                    w[i] = Settings.Ant.factorOld;
                if(pathmap[points[i].getY()][points[i].getX()] == 1)
                    w[i] = pathmap[points[i].getY()][points[i].getX()] * 50;
            }*/
        }
        
        // Calculate total
        float total = w[0] + w[1] + w[2] + w[3];
        
        // Spin the roulette wheel
        float rand = (float) (Math.random() * total);
        
        // Select point from possibilities
        if(w[0] != 0 && rand < w[0]) return points[0];
        if(w[1] != 0 && rand < w[0] + w[1]) return points[1];
        if(w[2] != 0 && rand < w[0] + w[1] + w[2]) return points[2];
        if(w[3] != 0) return points[3];
        
        // Return null if no step is possible
        return null;
    }
          
    /**
     * Returns the route taken by Ant.
     * @return path
     */
    public ArrayList<Point> getPath() { 
        return path; 
    }
    
    /**
     * Returns current position of Ant.
     * @return Point
     */
    private Point currentPoint() { 
        return path.get(path.size() - 1); 
    }
    
    /**
     * Returns previous position of Ant.
     * @return previous Point
     */
    private Point previousPoint() {
        return (path.size() < 2) ? null : path.get(path.size() - 2);
    }

    @Override
    public void run() {
        for (int i = 0; i < Settings.Ant.maxSteps; i++) {
            // Let the ant do a step
            boolean finished = doStep();

            // If the ant has finished
            if (finished) {
                // Update the shortestPath
                subRoute.updatePath(getPath());
                break;
            }
        }
    }

}
