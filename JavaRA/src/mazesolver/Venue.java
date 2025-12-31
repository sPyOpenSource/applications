package mazesolver;

import cr0s.javara.util.Pos;

/**
 * Venue to visit.
 */
public class Venue {
    private final Pos location;
    private final String id;
    
    /**
     * Constructs new Venue.
     * @param id
     * @param location 
     */
    public Venue(String id, Pos location) {
        this.id = id;
        this.location = location;
    }
    
    /**
     * Returns location of Venue
     * @return Point
     */
    public Pos getLocation() { 
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
