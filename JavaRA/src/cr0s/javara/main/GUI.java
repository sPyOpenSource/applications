package cr0s.javara.main;

import java.util.Random;

import cr0s.javara.gameplay.BuildingOverlay;
import cr0s.javara.gameplay.Player;
import cr0s.javara.gameplay.Team;
import cr0s.javara.render.Controller;
import cr0s.javara.render.World;
import cr0s.javara.render.shrouds.ShroudRenderer;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.ui.GameSideBar;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;

public class GUI extends Application {
    public ResourceManager rm;

    private StateMainMenu menu;
    private StateGameMap gameMap;

    private static GUI instance;

    private World w;
    private Camera camera;
    private Controller controller;

    private Team team;
    private Player player;

    private BuildingOverlay bo;

    public static boolean DEBUG_MODE = false;

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
        startNewGame("haos-ridges");
	//this.addState(new StateMainMenu());		
	//this.addState(new StateGameMap(arg0));
	//this.addState(new StatePauseMenu());
	new StateLoadingScreen().start(stage);
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

	camera = new PerspectiveCamera();
	/*try {
	    camera.init(this.getContainer());
	} catch (SlickException e1) {
	    e1.printStackTrace();
	}

	controller = new Controller(null, camera, this.getContainer().getInput());	
	w = new World("haos-ridges",
		this.getContainer(), camera);*/

	initGame();
    }

    public void initGame() {
	Random r = new Random();

	//this.observerShroudRenderer = new ShroudRenderer(w);

	team = new Team();
	/*player = new Player(w, "Player", Alignment.SOVIET, new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
	player.setTeam(team);

	//player.setShroud(null);
	
	bo = new BuildingOverlay(player, w);

	w.addPlayer(player);

	Pos playerSpawn = player.getPlayerSpawnPoint();	

	//this.getCamera().setOffset(-Math.max(w.getMap().getBounds().getMinX(), (playerSpawn.getX() * 24) - this.getContainer().getWidth() / 2), -Math.max(w.getMap().getBounds().getMinY(), (playerSpawn.getY() * 24)));

	this.getCamera().scrollCenterToCell(playerSpawn);
	
	this.gsb = new GameSideBar(Main.getInstance().getTeam(), Main.getInstance().getPlayer());
	this.gsb.initSidebarPages();
	
	Team team2 = new Team();
	Player otherPlayer = new AIPlayer(w, "NormalAI", Alignment.SOVIET, new Color(128, 0, 0));
	player.setTeam(team2);
	w.addPlayer(otherPlayer);*/
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
