package cr0s.javara.render.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;
import redhorizon.utilities.BufferUtility;
import assets.Assets;

import cr0s.javara.main.GUI;
import cr0s.javara.render.map.ResourcesLayer.ResourceCell;
import cr0s.javara.render.viewport.Camera;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;

import javafx.application.Application;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class TileMap extends Application {
    private short width, height;
    private int zTrans = -800;
    private TheaterCache theater;
    private TileSet tileSet;

    private TileReference[][] mapTiles;
    private ResourcesLayer resourcesLayer;
    private LinkedList<MapEntity> mapEntities;

    private final int GRASS_ID = 0xFF; // 255
    private final int GRASS_ID_BIG = 0xFFFF; // 65635

    private Rectangle bounds;
    private ArrayList<Pos> spawns;

    // Viewport and darkness shifts in tiles
    public static final int MAP_OFFSET_TILES = 16;
    public static final int ALLOWED_DARKNESS_SHIFT = 3;
    public static final int ALLOWED_DARKNESS_SHIFT_XMAX = 7; // this needed to be able put sidebar inside darkness if viewport is on right edge of map

    private Color blockedColor = Color.rgb(255, 0, 0, 32f/255);
    private Assets assets = new Assets();
    public SmudgeLayer smudges;

    public TileMap(){
        this("haos-ridges");
        //this("forest-path");
    }
    
    public TileMap(String mapName) {
	InputStream input;
	try {
	    input = assets.getInputStream("/assets/maps/" + (mapName + ".yaml").toLowerCase());

	    Yaml mapYaml = new Yaml();
	    Map<String, Object> mapYamlMap = (Map) mapYaml.load(input);	    

	    this.spawns = new ArrayList<>();
	    Map<String, Object> spawnsMap = (Map) mapYamlMap.get("Spawns");
	    for (Object v : spawnsMap.values()) {
		Map<String, Object> actor = (Map) v;

		int x = (Integer) actor.get("LocationX");
		int y = (Integer) actor.get("LocationY");

		System.out.println("[MAP] Added spawn: (" + x + "; " + y + ")");

		this.spawns.add(new Pos(x, y));
	    }

	    TileSet tileYamlSet = new TileSet((String) mapYamlMap.get("Tileset"));
	    this.tileSet = tileYamlSet;

	    input = new FileInputStream(new File(ResourceManager.RESOURCE_FOLDER + (System.getProperty("file.separator") + "trees.yaml").toLowerCase()));

	    Map<String, Object> treesYamlMap = (Map) mapYaml.load(input);	    

	    this.mapEntities = new LinkedList<>();
	    Map<String, Object> entitiesMap = (Map) mapYamlMap.get("Actors");
	    for (Object v : entitiesMap.values()) {
		Map<String, Object> actor = (Map) v;

		String id = (String) actor.get("Name");

		String footprint = ((Map<String, String>) (((Map<String, Object>) treesYamlMap.get(id.toUpperCase())).get("Building"))).get("Footprint");
		String dimensions = ((Map<String, String>) (((Map<String, Object>) treesYamlMap.get(id.toUpperCase())).get("Building"))).get("Dimensions");
		System.out.println("[MAP] Loaded Actor. ID: " + id + "(" + dimensions + "): " + footprint);
		int x = (Integer) actor.get("LocationX");
		int y = (Integer) actor.get("LocationY");

		ShpTexture st = ResourceManager.getInstance()
			.getTemplateShpTexture(this.tileSet.getSetName(), id + ".tem");

		if (st != null) {
		    MapEntity me = new MapEntity(x, y, st, footprint, dimensions);
		    this.mapEntities.add(me);
		}
	    }

	    this.theater = new TheaterCache(this, this.tileSet);

	    // Read binary map
	    loadBinaryMap(mapName);
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
    }

    public static void main(String[] args){
        //TileMap map = new TileMap(null, "forest-path");
        launch();
    }
    
    private void loadBinaryMap(String mapName) {
	try (RandomAccessFile randomAccessFile = new RandomAccessFile(ResourceManager.MAPS_FOLDER + (mapName + System.getProperty("file.separator") + "map.bin").toLowerCase(), "r")) {
	    FileChannel inChannel = randomAccessFile.getChannel();

	    // Read one byte and pair of two shorts: map height and width
	    ByteBuffer mapHeader = ByteBuffer.allocate(5);
	    mapHeader.order(ByteOrder.LITTLE_ENDIAN);
	    inChannel.read(mapHeader);
	    mapHeader.rewind();

	    if (mapHeader.get() != 1) {
		System.err.println("Invalid map.");
		return;
	    }

	    this.width = mapHeader.getShort();
	    this.height = mapHeader.getShort();

	    this.bounds = new Rectangle(TileMap.MAP_OFFSET_TILES * 24, TileMap.MAP_OFFSET_TILES * 24, (this.width - 2*TileMap.MAP_OFFSET_TILES) * 24, (this.height - 2*TileMap.MAP_OFFSET_TILES) * 24);

	    this.mapTiles = new TileReference[width][height];

	    System.out.println("Map size: " + this.width + " x " + this.height);

	    // Height, Width and sizeof(short) + sizeof(byte)
	    ByteBuffer mapBytes = BufferUtility.readRemaining(inChannel);
	    mapBytes.order(ByteOrder.LITTLE_ENDIAN);

	    Random r = new Random();
	    for (int x = 0; x < this.width; x++) {
		for (int y = 0; y < this.height; y++) {
		    int tile = (int) (mapBytes.getShort() & 0xFFFF);

		    short index = (short) (mapBytes.get() & 0xFF);

		    // Randomize clear grass
		    if (tile == GRASS_ID || tile == GRASS_ID_BIG) {
			index = (short) r.nextInt(16);
		    }

		    this.mapTiles[x][y] = new TileReference<>(tile, (byte) index);
		}
	    }

	    this.resourcesLayer = new ResourcesLayer(this);
	    for (int x = 0; x < this.width; x++) {
		for (int y = 0; y < this.height; y++) {
		    byte tile = mapBytes.get();
		    byte index = mapBytes.get();

		    if (tile != 0) {
			this.resourcesLayer.resources[x][y] = this.resourcesLayer.new ResourceCell(tile, index);
		    }
		}
	    }	   

	    this.resourcesLayer.setInitialDensity();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public short getWidth() {
	return width;
    }

    public short getHeight() {
	return height;
    }

    public void render(Stage c, Scene g, Camera camera) {
	//Color pColor = g.getColor();	
	this.theater.getSpriteSheet().startUse();

	// Draw tiles layer
	for (int y = (int) (-GUI.getInstance().getCamera().getTranslateY()) / 24; y < this.getHeight(); y++) {
	    for (int x = (int) (-GUI.getInstance().getCamera().getTranslateX()) / 24; x < this.getWidth(); x++) {
		// Don't render tile, if it shrouded and surrounding tiles shrouded too
		if (GUI.getInstance().getPlayer().getShroud() != null && GUI.getInstance().getPlayer().getShroud().isAreaShrouded(x, y, 2, 2)) {
		    continue;
		}

		if ((int) this.mapTiles[x][y].getTile() != 0) {
		    Pos sheetPoint = this.theater
			    .getTileTextureSheetCoord(this.mapTiles[x][y]);

		    int index = (int) ((byte) this.mapTiles[x][y].getIndex() & 0xFF);

		    int sX = (int) sheetPoint.getX();
		    int sY = (int) sheetPoint.getY();

		    if (sX != -1 && sY != -1) {
			this.theater.getSpriteSheet().renderInUse(x * 24, y * 24, sX / 24, (sY / 24) + index);
		    }
		    
		    this.resourcesLayer.renderCell(x, y);
		}
	    }
	}

	this.smudges.render(g);
	
	this.theater.getSpriteSheet().endUse();	
	//this.theater.getSpriteSheet().draw(24 * 20, 24 * 20);
    }

    public LinkedList<MapEntity> getMapEntities() {
	return this.mapEntities;
    }

    public void fillBlockingMap(int[][] blockingMap) {
	for (int y = 0; y < this.height; y++) {
	    for (int x = 0; x < this.width; x++) {
		int id = (int) ((int) this.mapTiles[x][y].getTile() & 0xFFFF);
		int index = (int) ((byte) this.mapTiles[x][y].getIndex() & 0xFF);

		Integer[] surfaces = this.theater.tilesSurfaces.get(id);

		if (surfaces != null && index >= surfaces.length) {
		    continue;
		}

		if (surfaces != null) {
		    blockingMap[x][y] = surfaces[index];
		} 
	    }
	}

	fillWithMapEntities(blockingMap);
    }

    private void fillWithMapEntities(int[][] blockingMap) {
	for (MapEntity me : this.mapEntities) {
	    for (int cX = 0; cX < me.getWidth(); cX++) {
		for (int cY = 0; cY < me.getHeight(); cY++) {
		    if (blockingMap[me.getX() + cX][me.getY() + cY] == 0) { 
			blockingMap[me.getX() + cX][me.getY() + cY] = me.getFootprintCells()[cX][cY]; 
		    }
		}
	    }
	}
    }

    public TileSet getTileSet() {
	return this.tileSet;
    }

    public Rectangle getBounds() {
	return this.bounds;
    }

    @Override
    public void start(Stage c) throws InterruptedException {
        BorderPane root = new BorderPane();
        //RotateCamera g = new RotateCamera();
	//this.theater.getSpriteSheet().startUse();
	// Draw map entities
	for (MapEntity me : this.mapEntities) {
	    int x = me.getX();
	    int y = me.getY();

	    ShpTexture t = me.getTexture();

	    Pos sheetPoint = this.theater.getShpTexturePoint(t.getTextureName());

	    int sX = (int) sheetPoint.getX();
	    int sY = (int) sheetPoint.getY();

	    ImageView view = this.theater.getSpriteSheet()
	    .getSubImage(sX, sY, t.width, t.height);
            view.setX(x * 24);
            view.setY(y * 24);
            //System.out.println(x+","+y);
            root.getChildren().add(view);
            //break;
	}
	
	//this.theater.getSpriteSheet().endUse();
        //ScrollPane scrollPane = new ScrollPane(root);
        //scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        //scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        Scene scene = new Scene(root, 800, 600);
        javafx.scene.Camera camera = new PerspectiveCamera(true);
        camera.setFarClip(Integer.MAX_VALUE);
        camera.setNearClip(0.1);
        scene.setCamera(camera);
        scene.getCamera().setTranslateX(800);
        scene.getCamera().setTranslateY(800);
        c.setScene(scene);
        c.show();
        scene.setOnScroll((ScrollEvent event) -> {
            zTrans += event.getDeltaY() * (zTrans / -50);
        });
        scene.setOnKeyPressed((KeyEvent e) -> {
            KeyCode code = e.getCode();
            switch (code) {
                case LEFT:
                    scene.getCamera().setTranslateX(scene.getCamera().getTranslateX() - 100);
                    break;
                case RIGHT:
                    scene.getCamera().setTranslateX(scene.getCamera().getTranslateX() + 100);
                    break;
                case UP:
                    scene.getCamera().setTranslateY(scene.getCamera().getTranslateY() - 100);
                    break;
                case DOWN:
                    scene.getCamera().setTranslateY(scene.getCamera().getTranslateY() + 100);
                    break;
                case HOME:
                    //g.xRotate.setAngle(-90);
                    //g.yRotate.setAngle(0);
                    //g.zRotate.setAngle(0);
                    scene.getCamera().setTranslateX(800);
                    scene.getCamera().setTranslateY(800);
                    scene.getCamera().setTranslateZ(-800);
                    break;
                default:
                    break;
            }
        });
        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                scene.getCamera().setTranslateZ(zTrans);
            }
        }.start();
    }

    public boolean isInMap(double x, double y) {
	return this.bounds.contains(x, y);
    }

    public ArrayList<Pos> getSpawnPoints() {
	return this.spawns;
    }

    public TheaterCache getTheater() {
	return this.theater;
    }
    
    public int getSurfaceIdAt(Pos pos) {
	return getSurfaceIdAt((int) pos.getX(), (int) pos.getY());
    }
    
    public int getSurfaceIdAt(int cellX, int cellY) {
	if (!this.isInMap(cellX * 24, cellY * 24)) {
	    return 0;
	}
	
	if (this.resourcesLayer.resources[cellX][cellY] != null) {
	    if (this.resourcesLayer.resources[cellX][cellY].type == 1) {
		return TileSet.SURFACE_ORE_GOLD;
	    } else {
		return TileSet.SURFACE_ORE_GEM;
	    }
	}
	
	Integer id = (Integer) this.mapTiles[cellX][cellY].getTile();
	Byte index = (Byte) this.mapTiles[cellX][cellY].getIndex();
	Integer[] surfaces = this.theater.tilesSurfaces.get(id);

	if (surfaces != null && index >= surfaces.length) {
	    return 0;
	}

	if (surfaces != null) {
	    return surfaces[index];
	} 
	
	return 0;
    }

    public ResourcesLayer getResourcesLayer() {
	return this.resourcesLayer;
    }

    public boolean isInMap(Pos targetCell) {
	return this.isInMap(targetCell.getX(), targetCell.getY());
    }
    
    public boolean isCellInMap(Pos targetCell) {
	return this.isInMap(targetCell.getX() * 24, targetCell.getY() * 24);
    }    
}
