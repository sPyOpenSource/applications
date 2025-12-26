package mazesolver;

/**
 * Venue to visit.
 */
public class Venue {
    private final Point location;
    private final String id;
    
    /**
     * Constructs new Venue.
     * @param id
     * @param location 
     */
    public Venue(String id, Point location) {
        this.id = id;
        this.location = location;
    }
    
    /**
     * Returns location of Venue
     * @return Point
     */
    public Point getLocation() { 
        return location; 
    }
    
    /**
     * Returns id of Venue
     * @return id
     */
    public String getId() { 
        return id; 
    }
}
