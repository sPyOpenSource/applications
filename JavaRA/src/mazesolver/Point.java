package mazesolver;

/**
 * Stores a point given by x and y coordinates.
 */
public class Point {
    private final int x, y;
    
    /**
     * Constructs new point with given coordinates.
     * @param x
     * @param y 
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Returns x-coordinate.
     * @return integer
     */
    public int getX() { 
        return x; 
    }
    
    /**
     * Returns y-coordinate.
     * @return integer
     */
    public int getY() { 
        return y; 
    }
    
    /**
     * Implementation of equals method. Checks if coordinates are equal.
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(Object obj){
        if(obj instanceof Point point) {
            return (point.x == x && point.y == y);
        }
        return false;
    }
    
    /**
     * Generates and returns hash code of a point.
     * @return integer
     */
    @Override
    public int hashCode() { return x + y; }
}
