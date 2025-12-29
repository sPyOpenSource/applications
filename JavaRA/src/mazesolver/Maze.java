package mazesolver;

import java.io.*;
import java.util.*;

/**
 * Implementation of the Maze.
 */
public class Maze {
    private final int[][] nodes;    
    private final Point startPoint;
    private final Point endPoint;
    
    private final Venue[] venues;
    private final Random random = new Random();
    
    /**
     * Creates new Maze based on given arguments.
     * @param nodes
     * @param startPoint
     * @param endPoint
     * @param venues Points to visit
     */
    public Maze(int[][] nodes, Point startPoint, Point endPoint, Venue[] venues) {
        // Check if nodes are valid
        if (nodes == null || nodes.length == 0 || nodes[0].length == 0)
            Helper.error("Invalid maze nodes!");
        
        this.nodes = nodes;
        
        // Check if startPoint is valid
        while (startPoint == null || !isNode(startPoint)){
            startPoint = new Point(
                    startPoint.getX() + random.nextInt(5) - 2, 
                    startPoint.getY() + random.nextInt(5) - 2);
            //Helper.error("Invalid start point!");
        }
        
        this.startPoint = startPoint;
        
        // Check if endPoint is valid
        while (endPoint == null || !isNode(endPoint)){
            endPoint = new Point(
                    endPoint.getX() + random.nextInt(5) - 2, 
                    endPoint.getY() + random.nextInt(5) - 2);
            //Helper.error("Invalid end point!");
        }
        
        this.endPoint = endPoint;
        
        // If no venues are given, create empty venue list
        if (venues == null)
            venues = new Venue[0];
        
        // Check if venues are valid
        for (Venue venue : venues)
            if (venue == null || !isNode(venue.getLocation()))
                Helper.error("Invalid venues!");

        this.venues = venues;
    }
        
    /**
     * Check if a point is a node.
     * @param p
     * @return boolean
     */
    public boolean isNode(Point p) { 
        return isNode(p.getX(), p.getY()); 
    }
    
    /**
     * Checks if a position is a node.
     * @param x coordinate
     * @param y coordinate
     * @return boolean
     */
    public boolean isNode(int x, int y) { 
        if(x < 0 || x > nodes[0].length - 1) return false;
        if(y < 0 || y > nodes.length - 1) return false;
        return nodes[x][y] >= 0;    
    }
    
    public int getValue(int x, int y){
        if(x < 0 || x > nodes[0].length - 1) return -1;
        if(y < 0 || y > nodes.length - 1) return -1;
        return nodes[x][y];
    }
    
    public void setValue(int x, int y, int value){
        if(x < 0 || x > nodes[0].length - 1) return;
        if(y < 0 || y > nodes.length - 1) return;
        if(value <= 0) value = 1;
        nodes[x][y] = value;
    }
    
    /**
     * Returns width of Maze.
     * @return width
     */
    public int getWidth() { 
        return nodes[0].length; 
    }
    
    /**
     * Returns height of Maze.
     * @return height
     */
    public int getHeight() { 
        return nodes.length; 
    }
    
    /**
     * Returns start point of Maze.
     * @return startPoint
     */
    public Point getStartPoint() { 
        return startPoint; 
    }
    
    /**
     * Returns end point of Maze.
     * @return endPoint
     */
    public Point getEndPoint() { 
        return endPoint; 
    }

    /**
     * Returns venues.
     * @return venues
     */
    public Venue[] getVenues() { 
        return venues;
    } 

    /**
     * Construct a new Maze object from given files. This method is implemented
     * with adherence to the file syntax from TI2735-A Assignment 4.
     * @param fileNodes
     * @param fileCoords
     * @param fileVenues
     * @param fileVisits
     * @return 
     */
    public static Maze readMaze(String fileNodes, String fileCoords, String fileVenues, String fileVisits) {
        if(fileNodes == null || fileCoords == null) return null;
        
        int[][] nodes; int width, height; 
        Point startPoint = null, endPoint = null;
        Venue[] venues = null;
        
        // Read the nodes file
        try {
            Scanner sc = new Scanner(new FileReader(new File(fileNodes)));
            
            width = sc.nextInt();
            height = sc.nextInt();
            sc.nextLine();
            
            nodes = new int[height * 2][width * 2];
            
            for(int y = 0; y < height; y++) {
                for(int x = 0; x < width; x++){
                    nodes[x * 2][y * 2] = sc.nextByte() * Settings.Ant.factorNew;
                    nodes[x * 2 + 1][y * 2 + 1] = nodes[x * 2][y * 2];
                    nodes[x * 2][y * 2 + 1] = nodes[x * 2][y * 2];
                    nodes[x * 2 + 1][y * 2] = nodes[x * 2][y * 2];
                }
                sc.nextLine();
            }
            
            Helper.debug("Nodes created with size " + nodes[0].length + "x" + nodes.length);
        } catch(FileNotFoundException ex) { 
            ex.printStackTrace(); return null; 
        }
        
        // Read the coordinates file
        try {
            Scanner sc = new Scanner(new FileReader(new File(fileCoords)));
            sc.useDelimiter("[,;\\s]+");

            startPoint = new Point(sc.nextInt() * 2, sc.nextInt() * 2);
            endPoint = new Point(sc.nextInt() * 2, sc.nextInt() * 2);
            
            Helper.debug("startPoint is " + startPoint);
            Helper.debug("endPoint is " + endPoint);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            startPoint = null;
            endPoint = null;
        }
        
        // Read the venues file
        if(fileVenues != null) {
            try {
                Scanner sc = new Scanner(new FileReader(new File(fileVenues)));
                sc.useDelimiter("[,:;\\s]+");
                
                int num = sc.nextInt();
                
                HashMap<String, Point> locations = new HashMap<>(num);

                for(int i = 0; i < num; i++)
                    locations.put(sc.next(), new Point(sc.nextInt(), sc.nextInt()));
                
                sc.close();
                
                // Read the visits file
                if(fileVisits != null) {
                    Scanner sc2 = new Scanner(new FileReader(new File(fileVisits)));
                    
                    ArrayList<Venue> v = new ArrayList<>(); int i = 1;
                    
                    while(sc2.hasNext()) {
                        String id = Integer.toString(i++);
                        Point loc = locations.get(sc2.next());
                        v.add(new Venue(id, loc));
                    }
                    
                    venues = new Venue[v.size()]; i = 0;
                    for(Venue venue : v)
                        venues[i++] = venue;
                } else {
                    venues = new Venue[num];
                    
                    for(int i = 0; i < num; i++) {
                        String id = Integer.toString(i + 1);
                        venues[i] = new Venue(id, locations.get(id));
                    }
                }                
            } catch(FileNotFoundException ex) {
                ex.printStackTrace(); venues = null;
            }
        }
        
        return new Maze(nodes, startPoint, endPoint, null);
    }
}
