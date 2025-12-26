package mazesolver;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main class, application entry point.
 */
public class Main extends Application {
    
    public Main(){
        
    }
    
    /**
     * Creates the new maze, and starts the director.
     * @param args 
     */
    public static void main(String[] args) {
        launch();
    }
    
    @Override
    public void start(Stage stage) throws InterruptedException {
        // Create a new Maze
        Maze maze = Maze.readMaze(
                Settings.Main.mazeFile,
                Settings.Main.coordinatesFile,
                Settings.Main.venuesFile,
                Settings.Main.visitsFile
        );
        
        Helper.log("Maze created of size " +
                maze.getWidth() + "x" + maze.getHeight());
        
        Settings.Ant.maxSteps = maze.getHeight() * maze.getWidth();
        
        // Create new Director
        Director director = new Director(maze, stage);        
        
        // Create graphics thread and start it
        Graphics graphics = new Graphics(director);
        graphics.start();
        
        Helper.log("Graphics thread created and started");
        
        // Do iterations while not interrupted
        /*Helper.log("Starting iterations"); int i = 0;
        
        // Announce application end
        Helper.log("Iteration cycle stopped");
        
        // Write best path to file
        director.writeRouteToFile();*/
    }
    
}
