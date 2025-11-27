package cr0s.javara.ui;

import java.util.HashMap;
import assets.Assets;

import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.infantry.EntityInfantry;
import cr0s.javara.gameplay.Base;
import cr0s.javara.gameplay.Player;
import cr0s.javara.gameplay.Production;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.perfomance.Profiler;

import cr0s.javara.render.map.MinimapRenderer;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.SoundManager;
import cr0s.javara.ui.sbpages.building.PageBuildingSoviet;
import cr0s.javara.ui.sbpages.infantry.PageInfantry;
import cr0s.javara.ui.sbpages.vehicle.PageVehicle;
import cr0s.javara.ui.sbpages.SideBarPage;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.Scene;
import javafx.application.Application;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

public class GameSideBar extends Application {
    private final Team team;
    private Player player;
    private Rectangle sidebarBounds;

    private static final int BAR_HEIGHT = 482;
    private static final int BAR_WIDTH = 130;

    public static final int BAR_SPACING_W = 25;
    public static final int BAR_SPACING_H = 25;

    private static final int RADAR_HEIGHT = BAR_WIDTH - 4;
    private static final int MENU_START_Y = BAR_SPACING_H + RADAR_HEIGHT + 6;//30;
    private static final Color BG_COLOR = Color.rgb(0xC0, 0xC0, 0xC0, 255f/255);

    private SpriteSheet menuCategoriesSheet;

    //             y  x
    private boolean[][] sideBarCategoriesOpened;

    private Color translucentColor = Color.rgb(255, 255, 255, 120f/255);
    private Color opaqueColor = Color.rgb(255, 255, 255, 255f/255);

    private final HashMap<String, SideBarPage> sideBarPages = new HashMap<>();
    private SideBarPage currentPage;
    private String currentPageName;

    public static final String START_PAGE_NAME = "start";
    public static final String PAGE_BUILDING_SOVIET = "sovbuild";
    public static final String PAGE_VEHICLE = "vehicle";
    public static final String PAGE_INFANTRY = "infantry";

    private Rectangle currentViewportRect = new Rectangle(0, 0, 0, 0);
    private Rectangle radarRect = new Rectangle(0, 0, 0, 0);
    private double previewScale;
    private Pos previewOrigin;

    private MinimapRenderer minimap;
    private PowerBarRenderer powerBar;

    private static final int POWERBAR_WIDTH = 11;
    private final int MINIMAP_UPDATE_INTERVAL_TICKS = 10;
    private int minimapUpdateTicks = MINIMAP_UPDATE_INTERVAL_TICKS;

    private int lowPowerAdviceTicks = 0;
    private int LOW_POWER_ADVICE_INTERVAL = 250;

    private boolean wasLowPower = false;
    private Assets assets = new Assets();
    
    public GameSideBar(){
        team = new Team();
        //player = new Player();
        this.sideBarCategoriesOpened = new boolean[6][2];
	this.sideBarCategoriesOpened[0][1] = true;
	this.sideBarCategoriesOpened[1][1] = true;
        this.menuCategoriesSheet = new SpriteSheet(assets.get("/assets/png/sidebar_buttons.png"), 64, 48);
    }

    public GameSideBar(Team aTeam, Player aPlayer) {
	try {
	    this.menuCategoriesSheet = new SpriteSheet(assets.get("/assets/png/sidebar_buttons.png"), 64, 48);

	    //System.out.println("Button sheet: " + this.menuCategoriesSheet.getHorizontalCount() + " x " + this.menuCategoriesSheet.getVerticalCount());
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	this.team = aTeam;
	this.player = aPlayer;

	//this.sidebarBounds = new Rectangle(GUI.getInstance().getContainer().getWidth() - BAR_WIDTH - BAR_SPACING_W, BAR_SPACING_H, BAR_WIDTH, BAR_HEIGHT);

	this.sideBarCategoriesOpened = new boolean[6][2];
	this.sideBarCategoriesOpened[0][1] = true;
	this.sideBarCategoriesOpened[1][1] = true;

	//this.radarRect.setBounds(Main.getInstance().getContainer().getWidth() - BAR_WIDTH - BAR_SPACING_W + 2, BAR_SPACING_H + 2, BAR_WIDTH - 4, RADAR_HEIGHT);
	//this.minimap = new MinimapRenderer(GUI.getInstance().getWorld(), (int) GUI.getInstance().getWorld().getMap().getBounds().getWidth() / 24, (int) (int) GUI.getInstance().getWorld().getMap().getBounds().getHeight() / 24);

	//this.powerBar = new PowerBarRenderer(new Pos(sidebarBounds.getX() - POWERBAR_WIDTH, sidebarBounds.getY() + sidebarBounds.getHeight()), POWERBAR_WIDTH, (int) sidebarBounds.getHeight());

	switchPage(START_PAGE_NAME);
    }

    public static void main(String[] args){
        launch();
    }
    
    @Override
    public void start(Stage stage) throws InterruptedException {
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, BAR_WIDTH + BAR_SPACING_W * 2, BAR_HEIGHT + BAR_SPACING_H * 2);
	if (this.sideBarPages.isEmpty()) {
	    this.sideBarPages.put(PAGE_BUILDING_SOVIET, new PageBuildingSoviet(new Pos(BAR_SPACING_W + 1, BAR_SPACING_H + 1)));
	    this.sideBarPages.put(PAGE_VEHICLE, new PageVehicle(new Pos(BAR_SPACING_W + 1, BAR_SPACING_H + 1)));
	    this.sideBarPages.put(PAGE_INFANTRY, new PageInfantry(new Pos(BAR_SPACING_W + 1, BAR_SPACING_H + 1)));
	}
 
	//Color pColor = g.getColor();
	//g.setColor(BG_COLOR.multiply(this.getBackgroundColor()));
	//g.fill(this.sidebarBounds);
	//this.powerBar.render(g, this.getBackgroundColor());
	this.drawSideBarButtons(root);
	//this.drawMoney(g);
	//g.setColor(pColor);
        stage.setScene(scene);
        stage.show();
    }

    public void drawMoney(BorderPane g) {
	//g.getFont().drawString(this.sidebarBounds.getX(), this.sidebarBounds.getY() - g.getFont().getLineHeight(), Main.getInstance().getPlayer().getBase().getDisplayCash() + Main.getInstance().getPlayer().getBase().getDisplayOre() + "$", Color.yellow.darker(0.2f));
    }

    public void drawRadar(BorderPane g) {
	/*if (GUI.getInstance().getPlayer().getBase().isRadarDomePresent && !GUI.getInstance().getPlayer().getBase().isLowPower()) {
	    this.minimap.renderMinimap(this.previewOrigin, g, getBackgroundColor());
	} else {
	    //g.setColor(Color.BLACK.multiply(getBackgroundColor()));
	    //g.fill(radarRect);	    
	}*/

	drawCurrentViewportRect(g);
    }

    public void drawCurrentViewportRect(BorderPane g) {
	/*g.setColor(Color.GREEN.multiply(this.getBackgroundColor()));

	g.setLineWidth(1);
	g.draw(this.currentViewportRect);*/
    }

    public void drawSideBarButtons(BorderPane g) {
	Color filterColor = getBackgroundColor();

	if (currentPage == null) {
	    drawStartPage(g, filterColor);
	} else {
	    drawCurrentPage(g, filterColor);
	}
    }

    private void drawCurrentPage(BorderPane g, Color filterColor) {
	if (this.currentPage != null) {
	    //this.currentPage.render(g, filterColor);
	}
    }

    private void drawStartPage(BorderPane g, Color filterColor) {
	drawRadar(g);
	int x = BAR_SPACING_W + 1;
	int y = MENU_START_Y;

	drawBuildingButtons(g, x, y, filterColor);
	drawWarFactoryButton(g, x, y, filterColor);
	drawBarracksButton(g, x, y, filterColor);

	for (int i = 2; i < 4; i++) {
	    //this.menuCategoriesSheet.getSubImage(this.sideBarCategoriesOpened[i][0] ? 1 : 0, 2 * i).draw(x, y + (i * 48), filterColor);
	    //this.menuCategoriesSheet.getSubImage(this.sideBarCategoriesOpened[i][1] ? 1 : 0, 2 * i + 1).draw(x + 64, y + (i * 48), filterColor);
	}	
    }

    private void drawBarracksButton(BorderPane g, final int sX, final int sY, Color filterColor) {
	EntityActor currentActor = null;//this.player.getBase().getProductionQueue().getCurrentInfantryProduction().getTargetActor();

	if (currentActor != null) {
	    //this.player.getBase().getProductionQueue().getCurrentInfantryProduction().drawProductionButton(g, sX + 64, sY + (1 * 48), filterColor, true);
	} else {
	    ImageView view = this.menuCategoriesSheet.getSubImage(this.sideBarCategoriesOpened[1][1] ? 1 : 0, 3);
            view.setTranslateX(sX + 64);
            view.setTranslateY(sY + (1 * 48));
            g.getChildren().add(view);
	}	
    }

    private void drawWarFactoryButton(BorderPane g, final int sX, final int sY, Color filterColor) {
	EntityActor currentActor = null;//this.player.getBase().getProductionQueue().getCurrentProducingVehicle();

	if (currentActor != null) {
	    //this.player.getBase().getProductionQueue().getCurrentVehicleProduction().drawProductionButton(g, sX, sY + (1 * 48), filterColor, true);
	} else {
	    ImageView view = this.menuCategoriesSheet.getSubImage(this.sideBarCategoriesOpened[1][0] ? 1 : 0, 2);
            view.setTranslateX(sX);
            view.setTranslateY(sY + (1 * 48));
            view.setOnMouseClicked(new EventHandler<>() {
                @Override
                public void handle(MouseEvent event) {
                    System.out.println(event);
                }
            });
            g.getChildren().add(view);
	}
    }

    private void drawBuildingButtons(BorderPane g, int sX, int sY, Color filterColor) {
	boolean isSovietLeft = true;//this.player.getAlignment() == Alignment.SOVIET;

	EntityActor currentActor = null;//this.getPlayer().getBase().getProductionQueue().getCurrentProducingBuilding();
	if (currentActor != null) {
	    if (!isSovietLeft && currentActor.unitProductionAlingment != Alignment.SOVIET) {
		sX += 64;
	    } else if (isSovietLeft && currentActor.unitProductionAlingment == Alignment.ALLIED) {
		sX += 64;
	    }

	    //this.getPlayer().getBase().getProductionQueue().getProductionForBuilding(currentActor).drawProductionButton(g, sX, sY, filterColor, true);

	    // Draw blackouted right texture
	    if (isSovietLeft) {
		//this.menuCategoriesSheet.getSubImage(0, 0).draw(sX + 64, sY, filterColor);
	    } else {
		//this.menuCategoriesSheet.getSubImage(0, 1).draw(sX + 64, sY, filterColor);
	    }
	} else {
	    if (isSovietLeft) {
		// Draw soviet, then allied
		//this.menuCategoriesSheet.getSubImage(this.sideBarCategoriesOpened[0][1] ? 1 : 0, 1).draw(sX, sY, filterColor);
		//this.menuCategoriesSheet.getSubImage(this.sideBarCategoriesOpened[0][0] ? 1 : 0, 0).draw(sX + 64, sY, filterColor);
	    } else {
		// Draw allied, then soviet
		//this.menuCategoriesSheet.getSubImage(this.sideBarCategoriesOpened[0][0] ? 1 : 0, 0).draw(sX, sY, filterColor);
		//this.menuCategoriesSheet.getSubImage(this.sideBarCategoriesOpened[0][1] ? 1 : 0, 1).draw(sX + 64, sY, filterColor);
	    }	    
	}
    }

    public void update(int delta) {
	Profiler.getInstance().startForSection("Sidebar tick");

	// Update current viewport rect
	int size = (int)Math.max(GUI.getInstance().getContainer().getWidth(), GUI.getInstance().getContainer().getHeight());
	previewScale =  Math.min(GUI.getInstance().getContainer().getWidth() / 24 * 1.0f / this.radarRect.getWidth(), GUI.getInstance().getContainer().getHeight() / 24 * 1.0f / this.radarRect.getHeight());	

	int spaceLeft = (int) (this.radarRect.getWidth() - GUI.getInstance().getWorld().getMap().getBounds().getWidth() / 24);

	previewOrigin = new Pos(this.radarRect.getX() + spaceLeft / 2, this.radarRect.getY() + spaceLeft / 2);

	Pos vpPoint = this.cellToMinimapPixel(new Pos(-GUI.getInstance().getCamera().getTranslateX() / 24, -GUI.getInstance().getCamera().getTranslateY() / 24));
	//this.currentViewportRect.setBounds(0, 0, previewScale * Main.getInstance().getWorld().getMap().getWidth(), previewScale * Main.getInstance().getWorld().getMap().getHeight());
	//this.currentViewportRect.setLocation(vpPoint.getX(), vpPoint.getY());		
	
	/*if (this.currentPage == null && this.radarRect.contains(mouseX, mouseY) && Main.getInstance().getContainer().getInput().isMouseButtonDown(0)) {
	    mouseX = (int) (mouseX - this.radarRect.getX());
	    mouseY = (int) (mouseY - this.radarRect.getY());

	    Pos mapCellToScroll = this.minimapPixelToCell(new Pos(mouseX, mouseY));
	    Main.getInstance().getCamera().scrollCenterToCell(mapCellToScroll);

	    return;
	}*/

	if (GUI.getInstance().getPlayer().getBase().isLowPower()) {
	    if (--this.lowPowerAdviceTicks <= 0) {
		this.lowPowerAdviceTicks = this.LOW_POWER_ADVICE_INTERVAL;

		SoundManager.getInstance().playSpeechSoundGlobal("lopower1");
	    }

	    if (!wasLowPower) {
		this.wasLowPower = true;

		// If power down occured with radar, play radar disabling sound
		if (GUI.getInstance().getPlayer().getBase().isRadarDomePresent) {
		    SoundManager.getInstance().playSfxGlobal("radardn1", 0.9f);
		}
	    }
	} else {
	    if (wasLowPower) {
		this.wasLowPower = false;

		// If power up occured with radar, play radar enabling sound
		if (GUI.getInstance().getPlayer().getBase().isRadarDomePresent) {
		    SoundManager.getInstance().playSfxGlobal("radaron2", 0.9f);
		}		
	    }
	}

	// Update radar rect
	//this.radarRect.setBounds(Main.getInstance().getContainer().getWidth() - BAR_WIDTH - BAR_SPACING_W + 2, BAR_SPACING_H + 2, BAR_WIDTH - 4, RADAR_HEIGHT);

	if (GUI.getInstance().getPlayer().getBase().isRadarDomePresent && !GUI.getInstance().getPlayer().getBase().isLowPower()) {
	    if (--this.minimapUpdateTicks <= 0) {
		this.minimapUpdateTicks = this.MINIMAP_UPDATE_INTERVAL_TICKS;

		this.minimap.update(this.getBackgroundColor());
	    }
	}

	// Update buttons presents
	Base base = this.player.getBase();


	// Construction yards
	this.sideBarCategoriesOpened[0][1] = base.isSovietCYPresent;
	this.sideBarCategoriesOpened[0][0] = base.isAlliedCYPresent;

	// War Factory
	this.sideBarCategoriesOpened[1][0] = base.isAlliedWarFactoryPresent || base.isSovietWarFactoryPresent;

	// Barracks/Tent
	this.sideBarCategoriesOpened[1][1] = base.isBarracksPresent || base.isTentPresent;

	// Sub Pen, Ship Yard
	this.sideBarCategoriesOpened[2][0] = base.isShipYardPresent || base.isSubPenPresent;

	// Air Field, Helipad
	this.sideBarCategoriesOpened[2][1] = base.isAirLinePresent || base.isHelipadPresent;

	// Any superpower
	this.sideBarCategoriesOpened[3][0] = this.sideBarCategoriesOpened[3][1] = base.isAnySuperPowerPresent;

	if (this.currentPage != null) {
	    Profiler.getInstance().startForSection("Sidebar page update");
	    this.currentPage.update(delta);
	    Profiler.getInstance().stopForSection("Sidebar page update");
	}

	Profiler.getInstance().stopForSection("Sidebar tick");
    }

    public Player getPlayer() {
	return this.player;
    }

    public boolean isMouseInsideBar() {
	int mouseX = 0;//Main.getInstance().getContainer().getInput().getMouseX();
	int mouseY = 0;//Main.getInstance().getContainer().getInput().getMouseY();

	return true;//this.sidebarBounds.contains(mouseX, mouseY);
    }

    private Color getBackgroundColor() {
	if (!isMouseInsideBar()) {
	    return translucentColor;
	} else {
	    return opaqueColor;
	}
    }

    public void productionButtonClick(Production production, int button, int buttonX, int buttonY) {
	if (button == 0) {
	    if (production.isOnHold()) {
		production.setOnHold(false);

		SoundManager.getInstance().playSpeechSoundGlobal("abldgin1"); // "Building"
	    } else {
		if (production.getTargetActor() instanceof EntityBuilding) {
		    SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);

		    if (production.isReady() && !production.isBuilding()) {
			GUI.getInstance().getBuildingOverlay().setBuildingMode((EntityBuilding) production.getTargetActor());
		    } else if (!production.isReady() && production.isBuilding()){
			SoundManager.getInstance().playSpeechSoundGlobal("progres1"); // "Unable to comply, building in progress"
		    } else if (!production.isReady() && !production.isBuilding()) {
			SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
			SoundManager.getInstance().playSpeechSoundGlobal("abldgin1");
			production.restartBuilding();
		    } 
		} else {
		    SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);

		    if (!production.isReady() && !production.isBuilding()) {
			if (!(production.getTargetActor() instanceof EntityInfantry)) { 
			    SoundManager.getInstance().playSpeechSoundGlobal("abldgin1");
			} else {
			    SoundManager.getInstance().playSpeechSoundGlobal("train1");
			}
			production.restartBuilding();
		    } 
		}
	    }
	} else if (button == 1) {
	    if (production.isBuilding()) {
		if (production.isOnHold() || production.isReady()) {
		    production.cancel(true);

		    SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
		    SoundManager.getInstance().playSpeechSoundGlobal("cancld1"); // "Canceled"
		} else if (!production.isOnHold()) {
		    production.setOnHold(true);

		    SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
		    SoundManager.getInstance().playSpeechSoundGlobal("onhold1"); // "On hold"
		} 
	    } else if (!production.isReady() || production.isDeployed()) {
		SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
		production.resetTargetActor();
		openPageByClick(buttonX, buttonY);
	    } else {
		production.cancel(true);

		SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
		SoundManager.getInstance().playSpeechSoundGlobal("cancld1"); // "Canceled"		
	    }
	}	
    }

    public void openPageByClick(int buttonX, int buttonY) {
	switch (buttonY) {
	case 0: // building productions
	    if (buttonX == 0) { // left side click
		if (this.player.getAlignment() == Alignment.SOVIET && this.sideBarCategoriesOpened[0][1]) {
		    // TODO: add allied building page
		    SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
		    switchPage(PAGE_BUILDING_SOVIET);
		} else if (buttonX == 1) {

		}
	    }

	    break;

	case 1:
	    if (buttonX == 0 && this.sideBarCategoriesOpened[1][0]) {
		SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
		switchPage(PAGE_VEHICLE);
	    } else if (buttonX == 1 && this.sideBarCategoriesOpened[1][1]) {
		SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
		switchPage(PAGE_INFANTRY);
	    }

	    break;
	}
    }

    public void startPageClick(int button, int buttonX, int buttonY) {
	switch (buttonY) {
	case 0: // building productions
	    if (buttonX == 0) { // left side click
		EntityActor buildingActor = player.getBase().getProductionQueue().getCurrentProducingBuilding();

		if (buildingActor != null) {
		    Production production = player.getBase().getProductionQueue().getProductionForBuilding(buildingActor);

		    productionButtonClick(production, button, buttonX, buttonY);
		} else {
		    openPageByClick(buttonX, buttonY);
		}
	    } else if (buttonX == 1) { // right side click

	    }
	    break;

	case 1:
	    if (buttonX == 0) {
		EntityActor buildingActor = player.getBase().getProductionQueue().getCurrentProducingVehicle();

		if (buildingActor != null) {
		    Production production = player.getBase().getProductionQueue().getCurrentVehicleProduction();

		    productionButtonClick(production, button, buttonX, buttonY);
		} else {
		    openPageByClick(buttonX, buttonY);
		}		
	    } else {
		EntityActor buildingActor = player.getBase().getProductionQueue().getCurrentInfantryProduction().getTargetActor();

		if (buildingActor != null) {
		    Production production = player.getBase().getProductionQueue().getCurrentInfantryProduction();

		    productionButtonClick(production, button, buttonX, buttonY);
		} else {
		    openPageByClick(buttonX, buttonY);
		}		
	    }
	    break;

	case 2:
	    break;

	case 3:
	    break;
	}
    }

    public void mouseClicked(int button, int x, int y) {
	// Transform absolute mouse coordinates to sidebar-relative
	int barX = (int)GUI.getInstance().getContainer().getWidth() - x - BAR_SPACING_W;
	int barY = y - BAR_SPACING_H;

	// Transform click coordinates to button coordinates
	int buttonX, buttonY;

	if (this.currentPage == null) {
	    if (this.radarRect.contains(x, y)) {
		return;
	    }

	    buttonX = 1 - (barX / 64);
	    buttonY = (y - MENU_START_Y) / 48;

	    this.startPageClick(button, buttonX, buttonY);
	} else {
	    if (button == 1) {
		SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
		this.switchPage("start");
		return;
	    }

	    buttonX = 1 - (barX / 64);
	    buttonY = barY / 48;

	    SoundManager.getInstance().playSfxGlobal("ramenu1", 0.8f);
	    this.currentPage.mouseClick(buttonX, buttonY);
	}

	System.out.println("[" + barX + "; " + barY + "] Button-" + button + " clicked: " + buttonX + " " + buttonY);
    }

    public void switchPage(String pageName) {
	this.currentPage = this.sideBarPages.get(pageName);
	this.currentPageName = pageName;
    }

    public Pos cellToMinimapPixel(Pos p)
    {
	Pos viewOrigin = new Pos(this.previewOrigin.getX(), this.previewOrigin.getY());
	Pos mapOrigin = new Pos(GUI.getInstance().getWorld().getMap().getBounds().getX() / 24, GUI.getInstance().getWorld().getMap().getBounds().getY() / 24);

	return new Pos(viewOrigin.getX() + this.radarRect.getWidth() / 24 * previewScale * (p.getX()- mapOrigin.getX()), viewOrigin.getY() + this.radarRect.getHeight() / 24 * previewScale * (p.getY() - mapOrigin.getY()));
    }

    public Pos minimapPixelToCell(final Pos p)
    {
	return p;
    }    
}
