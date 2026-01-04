package mazesolver;

import cr0s.javara.util.Pos;
import java.util.ArrayList;

/**
 * Implementation of a simple Ant.
 */
public class Ant implements Runnable {    
    
    private final ArrayList<Pos> path;
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
                [subRoute.getMaze().getWidth()]
                [subRoute.getMaze().getHeight()];
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
        Pos next = nextPoint();
        
        // Check if point is valid
        if(next == null) return false;
        
        // Add point to route
        path.add(next);

        // Update stepMap
        pathmap[next.getCellX()][next.getCellY()] = 1;
        int value = /*subRoute.getMaze().getValue(next.getX(), next.getY()) -*/ 50;
        //subRoute.getMaze().setValue(next.getX(), next.getY(), value);
        return false;
    }
    
    /**
     * Determines which of the 4 surrounding nodes the Ant should take.
     * @return 
     */
    public Pos nextPoint() {
        // Store current x and y coordinates
        int x = currentPoint().getCellX();
        int y = currentPoint().getCellY();
        
        // Create 4 surrounding locations
        Pos[] points = new Pos[]{
            new Pos(x, y - 1),
            new Pos(x - 1, y),
            new Pos(x + 1, y),
            new Pos(x, y + 1),
            new Pos(x - 1, y + 1),
            new Pos(x - 1, y - 1),
            new Pos(x + 1, y + 1),
            new Pos(x + 1, y - 1)
        };
        
        // Check each location with maze/path
        float[] w = new float[8];
        for(byte i = 0; i < 8; i++) {
            w[i] = subRoute.getMaze().getValue(points[i].getCellX(), points[i].getCellY());
            if(w[i] < 0) w[i] = 0;
            else w[i] = 100000;
            if(w[i] != 0){
                if(points[i].equals(previousPoint()))
                    w[i] = Settings.Ant.factorReverse;
                /*else if(pathmap[points[i].getY()][points[i].getX()] == 1000)
                    w[i] = Settings.Ant.factorOld;*/
                else if(pathmap[points[i].getCellX()][points[i].getCellY()] == 1)
                    w[i] = pathmap[points[i].getCellX()][points[i].getCellY()] * 100;
            }
        }
        
        // Calculate total
        float total = w[0] + w[1] + w[2] + w[3];
        total += w[4] + w[5] + w[6] + w[7];
        
        // Spin the roulette wheel
        float rand = (float) (Math.random() * total);
        
        // Select point from possibilities
        if(rand < w[0]) return points[0];
        if(w[1] != 0 && rand < w[0] + w[1]) return points[1];
        if(w[2] != 0 && rand < w[0] + w[1] + w[2]) return points[2];
        if(w[3] != 0 && rand < w[0] + w[1] + w[2] + w[3]) return points[3];
        if(w[4] != 0 && rand < w[0] + w[1] + w[2] + w[3] + w[4]) return points[4];
        if(w[5] != 0 && rand < w[0] + w[1] + w[2] + w[3] + w[4] + w[5]) return points[5];
        if(w[6] != 0 && rand < w[0] + w[1] + w[2] + w[3] + w[4] + w[5] + w[6]) return points[6];

        // Return null if no step is possible
        return points[7];
    }
          
    /**
     * Returns the route taken by Ant.
     * @return path
     */
    public ArrayList<Pos> getPath() { 
        return path; 
    }
    
    /**
     * Returns current position of Ant.
     * @return Point
     */
    private Pos currentPoint() { 
        return path.get(path.size() - 1); 
    }
    
    /**
     * Returns previous position of Ant.
     * @return previous Point
     */
    private Pos previousPoint() {
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
