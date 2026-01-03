package mazesolver;

import cr0s.javara.util.Pos;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import javafx.stage.Stage;

/**
 * Handles the interaction between the different classes.
 */
public class Director implements Runnable {
    private final Maze maze;
    private final SubRoute[][] subRouteMatrix;
    private final Route routeFinder;
    
    public final Stage stage;
    
    /**
     * Creates new Director for Maze maze.
     * @param maze 
     * @param stage 
     */
    public Director(Maze maze, Stage stage) { 
        this.stage = stage;
        
        // Create new Route object
        routeFinder = new Route(maze.getVenues().length + 2);
        
        // Create new SubRoute matrix
        subRouteMatrix = new SubRoute
                [maze.getVenues().length + 2]
                [maze.getVenues().length + 2];
        
        // Loop through the matrix and create new SubRoutes
        for (int y = 0; y < subRouteMatrix.length - 1; y++) {
            for (int x = y + 1; x < subRouteMatrix.length; x++) {
                // Define start and end point of subroute                
                Pos tmpStart = maze.getStartPoint();
                Pos tmpEnd = maze.getEndPoint();
                
                /*if (y != 0)
                    tmpStart = maze.getVenues()[y - 1].getLocation();
                if (x != subRouteMatrix.length - 1)
                    tmpEnd = maze.getVenues()[x - 1].getLocation();*/
                
                // Create SubRoute
                SubRoute tmp = new SubRoute(maze, tmpStart, tmpEnd);
                
                // Store SubRoute symmetrically
                subRouteMatrix[y][x] = tmp;
                subRouteMatrix[x][y] = tmp;
                
                // Display debug message
                Helper.debug("subRouteMatrix " + x + "," + y + " = " + tmp);
            }
        }
        
        // Set maze reference
        this.maze = maze;
    }
    
    /**
     * Run a single iteration.
     */
    @Override
    public void run() {
        // Store start time
        long startTime = System.currentTimeMillis();
        while(true){
            // Create a list of threads from this iteration
            ArrayList<Thread> threads = new ArrayList<>();

            // Loop through the possible subRoutes and create new threads
            for (int y = 0; y < subRouteMatrix.length - 1; y++) {
                for (int x = y + 1; x < subRouteMatrix.length; x++) {
                    Thread thread = subRouteMatrix[y][x].findSubRoute();
                    thread.setName("subRoute(" + y + " -> " + x + ")");
                    threads.add(thread);
                }
            }

            // Get lengthmatrix to be passed on
            int[][] lengthMatrix = Helper.createLengthMatrix(subRouteMatrix);

            // Create new routeFinder thread
            if(lengthMatrix != null) {
                routeFinder.setLengthMatrix(lengthMatrix);

                Thread thread = routeFinder.findRoute();
                thread.setName("routeFinder");
                threads.add(thread);
            }

            // Start or run threads
            for(Thread thread : threads) {
                if (Settings.Main.multiThreading)
                    thread.start();
                else
                    thread.run();
            }

            // Wait for threads to finish
            while (Settings.Main.multiThreading) {
                // Check if threads are alive
                boolean atleastOneRunning = false;
                for (Thread thread : threads) {
                    if (thread.isAlive()) {
                        atleastOneRunning = true;
                        break;
                    }
                }

                // Break out of loop if all threads are done
                if (!atleastOneRunning) break;

                // Wait for 20ms
                try { Thread.sleep(20); } catch (InterruptedException ex) {}
            }

            // Show route matrix
            Helper.debug(subRouteMatrixString(subRouteMatrix));

            // Show best route
            int[] bestorder = routeFinder.getBestOrder();
            if(System.currentTimeMillis() - startTime > 1000) break;
            if(bestorder.length != 0){
                Helper.log("Best route order: " + orderString(bestorder));
                Helper.log("Best route length: " +
                        routeFinder.getRouteLength(bestorder) + " steps");
                break;
            }
        }
        // Announce finish of iteration and print runtime
        Helper.log("Iteration finished after " +
                (System.currentTimeMillis() - startTime) + "ms");
    }
    
    /**
     * Returns Maze from the Director.
     * @return maze
     */
    public Maze getMaze() { 
        return maze;
    }
    
    /**
     * Returns SubRoute between two points.
     * @param i from
     * @param j to
     * @return subRoute
     */
    public SubRoute getSubRoute(int i, int j) { 
        return subRouteMatrix[i][j]; 
    }
    
    /**
     * Returns ArrayList of Points created from best found order.
     * @return Route
     */
    public ArrayList<Pos> getBestRoute() {
        int[] order = routeFinder.getBestOrder();
        
        ArrayList<Pos> route = new ArrayList<>();
        
        for(int i = 0; i < order.length - 1; i++) {
            int from = order[i];
            int to = order[i + 1];
            
            // Retreive sub route
            SubRoute sb = subRouteMatrix[from][to];
            
            // Get shortest path as copy
            ArrayList<Pos> tmp = sb.getPath(from > to);
            
            // Copy to route
            route.addAll(tmp);
        }
        return route;
    }
    
    /**
     * Generates String with the order of Venues.
     * @param order
     * @return String
     */
    public String orderString(int[] order) {
        if(order.length < 2) return "[]";
        
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < order.length; i++) {
            if(i == 0)
                sb.append("[start");
            else if(i == order.length - 1)
                sb.append(", end]");
            else {
                Venue v = maze.getVenues()[order[i] - 1];
                sb.append(", ").append(v.getId());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Generates String with matrix of SubRoute lengths.
     * @param matrix
     * @return String
     */
    public String subRouteMatrixString(SubRoute[][] matrix) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(matrix.length).append("x").
                append(matrix.length).append(" matrix");
        
        for (SubRoute[] mat : matrix) {
            sb.append(System.lineSeparator()).append("[");
            for (int x = 0; x < matrix.length; x++) {
                if(x != 0)
                    sb.append(",");
                if (mat[x] != null) {
                    sb.append(mat[x].getPathLength());
                } else {
                    sb.append("-");
                }
            }
            sb.append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * Exports the best found Route to file. This method is implemented with
     * adherence to the file syntax from TI2735-A Assignment 4.
     */
    public void writeRouteToFile() {
        int[] order = routeFinder.getBestOrder();

        if(order.length == 0) return;
        
        routeFinder.setLengthMatrix(Helper.createLengthMatrix(subRouteMatrix));
        
        int length = routeFinder.getRouteLength(order);
        
        SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy HH.mm.ss");
        String time = df.format(java.util.Calendar.getInstance().getTime());
        
        String outFile = "action " + length + " " + time + ".txt"; 
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            
            writer.write(length + order.length - 2 + ";");
            writer.newLine();

            // No start point for Grand Challenge!
//            writer.write(
//                    maze.getStartPoint().getX() + ","
//                    + maze.getStartPoint().getY() + ";");
//            writer.newLine();
            
            Venue[] ptv = maze.getVenues();
            
            for(int i = 0; i < order.length - 1; i++) {
                int from = order[i], to = order[i+1];
                
                if (i != 0) {
                    writer.write("take product #" + ptv[from-1].getId() + ";");
                    writer.newLine();
                }
                
                //writeSubRoute(writer, subRouteMatrix[from][to].getPath(from > to));
            }
            
            Helper.log("Route saved to " + outFile);
            
            writer.close();
        } catch (IOException e) {
            Helper.error("Not able to write to file");
        } /*catch (InvalidRouteException e) {
            Helper.error("Route invalid!");
        }*/
    }
    
    /**
     * Writes actions of a SubRoute to the writer.
     * @param writer
     * @param subRoute
     * @throws IOException
     * @throws InvalidRouteException 
     */
    private void writeSubRoute(BufferedWriter writer, ArrayList<Pos> subRoute)
            throws IOException, InvalidRouteException {
        Pos prev = null;

        for (Pos point : subRoute) {
            if (prev != null) {
                int dx = point.getCellX() - prev.getCellX();
                int dy = prev.getCellY() - point.getCellY();
                
                if (dx == 1 && dy == 0) writer.write(0 + ";");
                else if(dx == 0 && dy == 1) writer.write(1 + ";");
                else if(dx == -1 && dy == 0) writer.write(2 + ";");
                else if(dx == 0 && dy == -1) writer.write(3 + ";");
                else throw new InvalidRouteException();
                
                writer.newLine();
            }
            prev = point;
        }
    }
    
    /**
     * Custom Exception
     */
    private class InvalidRouteException extends Exception { }
}
