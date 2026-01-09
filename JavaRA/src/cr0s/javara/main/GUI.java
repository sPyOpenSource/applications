package cr0s.javara.main;

import cr0s.javara.gameplay.BuildingOverlay;
import cr0s.javara.gameplay.Player;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.Controller;
import cr0s.javara.render.World;
import cr0s.javara.render.shrouds.ShroudRenderer;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.ui.GameSideBar;
import cr0s.javara.ai.AIPlayer;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.application.Platform;

public class GUI extends Application {
    public ResourceManager rm;
    public static boolean DEBUG_MODE = false;
    
    private Stage stage;
    private StateMainMenu menu;
    private StateGameMap gameMap;
    private static GUI instance;

    private World w;
    private Camera camera;
    private Controller controller;

    private Team team;
    private Player player;
    private BuildingOverlay bo;

    private GameSideBar gsb;
    private ShroudRenderer observerShroudRenderer;

    public GUI() {
	//SoundStore.get().init();
    }

    public static GUI getInstance() {
	if (instance == null) {
	    instance = new GUI();
	}

	return instance;
    }

    /**
     * Entry point.
     * 
     * @param argv
     *            The argument passed on the command line (if any)
     */
    public static void main(String[] argv) {
	/*try {
	    AppGameContainer container = new AppGameContainer(Main.getInstance(), 1200,
		    700, false);

	    //Renderer.setRenderer(Renderer.VERTEX_ARRAY_RENDERER);
	    
	    container.setMinimumLogicUpdateInterval(50);
	    container.setMaximumLogicUpdateInterval(50);
	    container.setShowFPS(false);
	    //container.setSmoothDeltas(true);
	    //container.setVSync(true);
	    container.setTargetFrameRate(75);
	    container.setClearEachFrame(false);

	    container.start(); 
	} catch (Exception e) {
	    e.printStackTrace();
	}*/
        
        launch();
    }

    @Override
    public void start(Stage stage) throws InterruptedException {
        this.stage = stage;
        startNewGame("haos-ridges");
	//this.addState(new StateMainMenu());		
	//this.addState(new StateGameMap(arg0));
	//this.addState(new StatePauseMenu());
	//new StateLoadingScreen().start(stage);
	//this.addState(new StateTestScreen());

	// Disable native cursor
	/*Cursor emptyCursor;
	try {
	    emptyCursor = new Cursor(1, 1, 0, 0, 1, BufferUtils.createIntBuffer(1), null);
	    Mouse.setNativeCursor(emptyCursor);	  		
	} catch (LWJGLException e) {
	    e.printStackTrace();
	}*/
    }

    public Camera getCamera() {
	return camera;
    }

    public Controller getController() {
	return controller;
    }

    public World getWorld() {
	return w;
    }

    public void setWorld(World w) {
	this.w = w;
    }

    public void startNewGame(String mapName) {	
	rm = ResourceManager.getInstance();
	rm.loadBibs();

	camera = new PerspectiveCamera(true);

        w = new World(mapName);
        
	//this.observerShroudRenderer = new ShroudRenderer(w);

	team = new Team();
	player = new AIPlayer(
                w, 
                "NormalAI", 
                Alignment.SOVIET, 
                Color.rgb(
                        World.random.nextInt(256), 
                        World.random.nextInt(256), 
                        World.random.nextInt(256)
                )
        );
	player.setTeam(team);

	//player.setShroud(null);
	
	bo = new BuildingOverlay(player, w);
        Scene scene = new Scene(w.render(), 1200, 700);
        scene.setFill(Color.GREEN);
	w.addPlayer(player);
        
	//this.getCamera().setOffset(-Math.max(w.getMap().getBounds().getMinX(), (playerSpawn.getX() * 24) - this.getContainer().getWidth() / 2), -Math.max(w.getMap().getBounds().getMinY(), (playerSpawn.getY() * 24)));

	//this.getCamera().scrollCenterToCell(playerSpawn);
	
	this.gsb = new GameSideBar(player);
	//this.gsb.initSidebarPages();
	Platform.runLater(new Runnable() {
            @Override
            public void run() {             
                /*try {
                    gsb.start(new Stage());
                } catch (InterruptedException ex) {
                    System.getLogger(GUI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }*/
            }
        });
	Team team2 = new Team();
	Player otherPlayer = new AIPlayer(w, "NormalAI", Alignment.SOVIET, Color.rgb(128, 0, 0));
	player.setTeam(team2);
	w.addPlayer(otherPlayer);
        
        controller = new Controller(player, camera, scene);
        stage.setScene(scene);
        stage.show();
        w.start();
    }

    public Player getPlayer() {
	return this.player;
    }

    public Team getTeam() {
	return this.player.getTeam();
    }

    public GameSideBar getSideBar() {
	return this.gsb;
    }

    public ShroudRenderer getObserverShroudRenderer() {
	return this.observerShroudRenderer;
    }

    public BuildingOverlay getBuildingOverlay() {
	return this.bo;
    }

    public Stage getContainer() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
