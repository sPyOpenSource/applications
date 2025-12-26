package mazesolver;

import java.util.ArrayList;
import javafx.animation.AnimationTimer;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.H;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.UP;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;

/**
 * Contains all graphical tools.
 */
public class Graphics extends AnimationTimer {
    private final Director director;
    private final Maze maze;
    
    private final BorderPane root;
    private double offsetX = 50, offsetY = 50;
    private final float SCROLL_SPEED = 0.5f; // TODO: config this
    private int zTrans = -200;

    /**
     * Constructs new graphics handler.
     * @param director 
     */
    public Graphics(Director director) {
        this.director = director;
        this.maze = director.getMaze();
    
        // Create a new LWJGL window    
        root = new BorderPane();
        Scene scene = new Scene(root, Settings.Graphics.width, Settings.Graphics.height);
        scene.setFill(Color.GRAY);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFarClip(Integer.MAX_VALUE);
        camera.setNearClip(0.1);
        scene.setCamera(camera);
        scene.getCamera().setTranslateX(offsetX);
        scene.getCamera().setTranslateY(offsetY);
        scene.setOnScroll((ScrollEvent event) -> {
            zTrans += event.getDeltaY() * (zTrans / -50);
            //offsetX -= event.getDeltaX() * SCROLL_SPEED;
            //offsetY -= event.getDeltaY() * SCROLL_SPEED;
        });
        scene.setOnKeyPressed((KeyEvent e) -> {
            KeyCode code = e.getCode();
            switch (code) {
                case LEFT:
                    offsetX = scene.getCamera().getTranslateX() - 40;
                    break;
                case RIGHT:
                    offsetX = scene.getCamera().getTranslateX() + 40;
                    break;
                case UP:
                    offsetY = scene.getCamera().getTranslateY() - 40;
                    break;
                case DOWN:
                    offsetY = scene.getCamera().getTranslateY() + 40;
                    break;
                case H:
                    director.run();
                    //g.xRotate.setAngle(-90);
                    //g.yRotate.setAngle(0);
                    //g.zRotate.setAngle(0);
                    //offsetX = player.getPlayerSpawnPoint().getX();
                    //offsetY = player.getPlayerSpawnPoint().getY();
                    zTrans = -200;
                    break;
                default:
                    break;
            }
        });
        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                /*if(zTrans < -2000) zTrans = -2000;
                if(zTrans > -400) zTrans = -400;
                if(offsetX < 800) offsetX = 800;
                if(offsetX > 128 * 24 - 800) offsetX = 128 * 24 - 800;
                if(offsetY < 600) offsetY = 600;
                if(offsetY > 128 * 24 - 600) offsetY = 128 * 24 - 600;*/
                scene.getCamera().setTranslateX(offsetX);
                scene.getCamera().setTranslateY(offsetY);
                scene.getCamera().setTranslateZ(zTrans);
            }
        }.start();

        // Set the display title
        director.stage.setTitle(Settings.Graphics.title);
        
        // Setup opengl viewing space
        director.stage.setScene(scene);
        director.stage.show();
        
        // Draw the maze
        drawMaze();
    }

    /**
     * Build new display.
     * @param delta
     */
    @Override
    public void handle(long delta) {
            // Only draw at certain delta time
            if(delta % Settings.Graphics.drawWaitTime == 0) {
                // Draw to screen
                //GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                // Draw best route so far
                //GL11.glLineWidth(Settings.Graphics.lineWidth);
                Color line = Color.color(
                        Settings.Graphics.lineColor[0],
                        Settings.Graphics.lineColor[1],
                        Settings.Graphics.lineColor[2]
                );
                drawPath(director.getBestRoute(), line);
                // Draw the startPoint
                Color start = Color.color(0.275f, 0.537f, 0.4f);
                drawPoint(maze.getStartPoint(), 0.8f, start);

                // Draw the endPoint
                Color end = Color.color(0.557f, 0.157f, 0);
                drawPoint(maze.getEndPoint(), 0.8f, end);

                // Draw the venues
                Color venue = Color.color(1, 0.69f, 0.231f);
                for (Venue ptv : maze.getVenues()) {
                    drawPoint(ptv.getLocation(), 0.7f, venue);
                }
                
                // Process input
                //handleInput();                
            }
            
            // Process messages and sync
            //Display.processMessages();
            //Display.sync(Settings.Graphics.framesPerSecond);
    }
    
    /*public void stop(){
        Display.setTitle(Display.getTitle() + " | STOPPED!");

        // Stop maze iterations
        director.interrupted = true;

        // Keep showing until closed again
        while (!Display.isCloseRequested()) {
            handleInput();
            Display.processMessages();
            Display.sync(Settings.Graphics.framesPerSecond);
        }

        // Destroy the window
        Display.destroy();

        // Thread ends here
        Helper.log("Display destroyed");
    }*/
    
    /**
     * Handles mouse events.
     */
    /*private void handleInput() {
        while(Mouse.next()) {
            if(Mouse.getEventButtonState() == true) {
                // Get mouse coordinates
                int x = Mouse.getEventX();
                int y = Display.getHeight() - Mouse.getEventY();
                
                // Transform to maze coordinates
                int mx = maze.getWidth() * x / Display.getWidth();
                int my = maze.getHeight() * y / Display.getHeight();
                
                // Print coordinates 
                Helper.log("Clicked at: " + mx + "," + my);
            }
        }
    }*/
    
    /**
     * Draws maze on screen.
     */
    private void drawMaze() {                
        // Draw the nodes
        Color node = Color.color(1, 1, 1);
        for(int x = 0; x < maze.getWidth(); x++) {
            for(int y = 0; y < maze.getHeight(); y++) {
                if(maze.isNode(x, y)) drawNode(x, y, node);
            }
        }
    }
    
    /**
     * Draws point on screen.
     * @param p
     * @param size 
     */
    private void drawPoint(Point p, float size, Color color) {
        drawQuad(p.getX(), p.getY(), size, color);
    }
    
    /**
     * Draws node on screen.
     * @param x
     * @param y 
     */
    private void drawNode(int x, int y, Color color) {
        drawQuad(x, y, 1, color);
    }
    
    /**
     * Draws square on screen.
     * @param x
     * @param y
     * @param size 
     */
    private void drawQuad(int x, int y, float size, Color color) {
        float half = (1 - size) / 2;
        Polygon body = new Polygon(
                x + half, 
                y + half, 
                x + 1 - half, 
                y + half,
                x + 1 - half, 
                y + 1 - half,
                x + half, 
                y + 1 - half
        );
        body.setFill(color);
        //body.setStroke(Color.AQUA);
        //body.setStrokeWidth(0.001);
        root.getChildren().add(body);
    }
    
    /**
     * Draws path on screen.
     * @param path 
     */
    private void drawPath(ArrayList<Point> path, Color color) {
        if(path.isEmpty()) return;
        Path p = new Path();
        MoveTo start = new MoveTo(path.get(0).getX() + 0.5f, path.get(0).getY() + 0.5f);
        p.getElements().add(start);
        for (int i = 1; i < path.size(); i++){
            p.getElements().add(new LineTo(path.get(i).getX() + 0.5f, path.get(i).getY() + 0.5f));
        }
        p.setStroke(Color.AQUA);
        p.setStrokeWidth(0.5);
        root.getChildren().add(p);
    }
}
