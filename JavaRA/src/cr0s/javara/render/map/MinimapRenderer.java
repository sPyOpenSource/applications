package cr0s.javara.render.map;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.main.GUI;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class MinimapRenderer {

    private final int width;
    private final int height;
    private final World w;

    private Image minimapImage;

    private static final int ENTITY_ADDITIONAL_SIZE = 1; // grow entity rectangle point in pixels to see on mini map more clear

    public MinimapRenderer(World aWorld, int aWidth, int aHeight) {
	this.w = aWorld;

	this.width = aWidth;
	this.height = aHeight;

	try {
	    //this.minimapImage = new Image(aWidth, aHeight);

	    //this.minimapImage.getGraphics().setColor(Color.BLACK);
	    //this.minimapImage.getGraphics().fillRect(0, 0, width, height);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public void renderMinimap(Pos minimapPos, Scene gr, Color filterColor) {
	int miniX = (int) minimapPos.getX();
	int miniY = (int) minimapPos.getY();

	//this.minimapImage.draw(miniX, miniY, filterColor);
    }

    private Color getMapCellColor(int cellX, int cellY) {
	if (GUI.getInstance().getPlayer().getShroud() != null && !GUI.getInstance().getPlayer().getShroud().isExplored(cellX, cellY)) {
	    return null;
	}

	int surfaceId = this.w.getMap().getSurfaceIdAt(cellX, cellY);
	Color c = this.w.getMap().getTileSet().terrainColors.get(surfaceId);

	if (c != null) {
	    return c;
	} else {
	    return this.w.getMap().getTileSet().terrainColors.get(TileSet.SURFACE_CLEAR_ID);
	}
    }

    public void update(Color filterColor) {
	Scene gr;
	try {
	    //gr = this.minimapImage.getGraphics();

	    if (GUI.getInstance().getPlayer().getShroud() != null) {
		//gr.setColor(Color.BLACK);
		//gr.fillRect(0, 0, this.width, this.height);
	    }

	    // Render terrain
	    for (int x = 0; x < this.width; x++) {
		for (int y = 0; y < this.height; y++) {
		    int r, g, b;

		    Color targetColor = this.getMapCellColor((int) (this.w.getMap().getBounds().getX() / 24 + x), (int) (this.w.getMap().getBounds().getY() / 24 + y));

		    if (targetColor != null) {
			//gr.setColor(targetColor);
			//gr.fillRect(x, y, 1, 1);
		    }
		}
	    }

	    for (Entity e : this.w.getEntitiesList()) {		
		if (!(e instanceof EntityActor)) {
		    continue;
		}
		
		EntityActor a = (EntityActor) e;

		int cellPosX = (int) (a.getPosition().getX() - this.w.getMap().getBounds().getX()) / 24;
		int cellPosY = (int) (a.getPosition().getY() - this.w.getMap().getBounds().getY()) / 24;		
		
		// Don't draw shrouded entities
		if (GUI.getInstance().getPlayer().getShroud() != null && !GUI.getInstance().getPlayer().getShroud().isExplored(a.getCellPosition())) {
		    continue;
		}

		//gr.setColor(e.owner.playerColor);
		//gr.fillRect(cellPosX - ENTITY_ADDITIONAL_SIZE, cellPosY - ENTITY_ADDITIONAL_SIZE, (int) e.sizeWidth / 24 + ENTITY_ADDITIONAL_SIZE, (int) e.sizeHeight / 24 + ENTITY_ADDITIONAL_SIZE);
	    }
	} catch (Exception e1) {
	    e1.printStackTrace();
	}
    }
    
}
