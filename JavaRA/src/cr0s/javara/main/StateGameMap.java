package cr0s.javara.main;

import java.util.LinkedList;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.effect.MoveFlash;
import cr0s.javara.entity.effect.ScreenShaker;
import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;

import cr0s.javara.perfomance.PerfomanceGraphRenderer;
import cr0s.javara.perfomance.Profiler;
import cr0s.javara.resources.SoundManager;
import cr0s.javara.ui.cursor.CursorManager;
import cr0s.javara.ui.cursor.CursorType;
import cr0s.javara.util.Pos;
import javafx.application.Application;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class StateGameMap extends Application {
    public static final int STATE_ID = 1;

    private Stage container;
    private final Pos pressStart = new Pos(0, 0);
    private final Rectangle selectionRect = new Rectangle(0, 0, 0, 0);
    private final Color selectionFillColor = new Color(0, 0, 0, 64f/255);
    private final boolean isAnyMovableEntitySelected = false;
    private final int CURSOR_UPDATE_INTERVAL_TICKS = 100;
    
    private int cursorUpdateTicks = CURSOR_UPDATE_INTERVAL_TICKS;
    private boolean selectionRectVisible = true;
    private Entity mouseOverEntity = null;

    public StateGameMap() {
	//this.container = container;
    }
    
    public static void main(String[] args){
        launch();
    }

    public void mouseDragged(final int oldX, final int oldY, final int newX, final int newY) {
	//if (Main.getInstance().getContainer().getInput().isMouseButtonDown(0) && !Main.getInstance().getSideBar().isMouseInsideBar()) {
	    if (!GUI.getInstance().getBuildingOverlay().isInBuildingMode()) {
		if (!this.selectionRectVisible) {
		    this.selectionRectVisible = true;
		}

		double startX = this.pressStart.getX();
		double startY = this.pressStart.getY();

		double endX = -GUI.getInstance().getCamera().getTranslateX() + newX;
		double endY = -GUI.getInstance().getCamera().getTranslateY() + newY;
		double s;

		// Swap if necessary
		if (startX > endX) {
		    s = endX;
		    endX = startX;
		    startX = s;
		}

		if (startY > endY) {
		    s = endY;
		    endY = startY;
		    startY = s;
		}

		int boxH = (int) (endY - startY);
		int boxW = (int) (endX - startX);

		//this.selectionRect.setBounds(startX, startY, boxW, boxH);	
	    } else {
		GUI.getInstance().getBuildingOverlay().mouseDragged(oldX, oldY, newX, newY);
	    }
	//}
    }

    public final void mouseMoved(final int arg0, final int arg1, final int x, final int y) {
	updateCursor();
    }

    public final void mouseClicked(final int button, final int x, final int y, final int clickCount) {
	GUI.getInstance().getController().mouseClicked(button, x, y, clickCount);

	if (GUI.getInstance().getSideBar().isMouseInsideBar()) {
	    GUI.getInstance().getSideBar().mouseClicked(button, x, y);
	    return;
	}

	if (GUI.getInstance().getBuildingOverlay().isInBuildingMode()) {
	    GUI.getInstance().getBuildingOverlay().mouseClick(button);

	    return;
	}

	Entity e = GUI.getInstance().getWorld().getEntityInPoint(-GUI.getInstance().getCamera().getTranslateX() + x, -GUI.getInstance().getCamera().getTranslateY() + y);

	// Is there entity under mouse
	Target target;
	if (e != null) {
	    target = new Target(e);
	} else {
	    target = new Target(new Pos((-GUI.getInstance().getCamera().getTranslateX() + x) / 24, (-GUI.getInstance().getCamera().getTranslateY() + y) / 24));
	}

	// We have no selected entities
	if (GUI.getInstance().getPlayer().selectedEntities.isEmpty()) {
	    if (target.isEntityTarget() && button == 0) {
		GUI.getInstance().getPlayer().selectOneEntity(e);
	    } else {
		CursorManager.getInstance().setCursorType(CursorType.CURSOR_POINTER);
	    }
	} else {
	    InputAttributes ia = new InputAttributes(button);
	    OrderTargeter targeterForEntity = GUI.getInstance().getPlayer().getBestOrderTargeterForTarget(target);
	    boolean moveFlashSpawned = false;

	    if (targeterForEntity != null) {
		// Issue orders to selected entities
		for (Entity entity : GUI.getInstance().getPlayer().selectedEntities) {
		    if (!(entity instanceof EntityActor) || !entity.isSelected) {
			continue;
		    }

		    EntityActor ea = (EntityActor) entity;

		    Order order = ea.issueOrder(ea, targeterForEntity, target, ia);

		    if (order != null) {
			ea.resolveOrder(order);

			if (order.orderString.equals("Move") && !moveFlashSpawned) {
			    moveFlashSpawned = true;

			    MoveFlash flash = new MoveFlash(order.targetPosition.getX() * 24, order.targetPosition.getY() * 24, 24, 24);
			    flash.setWorld(ea.world);
			    flash.isVisible = true;

			    ea.world.spawnEntityInWorld(flash);
			}
		    } else {
			// Current entity can't resolve this order, so de-select this entity
			if (entity != null) { 
			    entity.isSelected = false;
			}
		    }
		}

		// Play sound only if order given
		if (button == 1) { 
		    if (targeterForEntity.entity.owner == GUI.getInstance().getPlayer()) {
			targeterForEntity.entity.playOrderSound();
		    }
		}
	    } else {
		GUI.getInstance().getWorld().cancelAllSelection();
	    }

	    GUI.getInstance().getPlayer().removeNotActuallySelectedEntities();

	    // No one entity left, set basic pointer cursor
	    if (!GUI.getInstance().getPlayer().isAnyActorEntitySelected()) {		
		if (e != null && button == 0) {
		    GUI.getInstance().getPlayer().selectOneEntity(e);
		} else {
		    CursorManager.getInstance().setCursorType(CursorType.CURSOR_POINTER);
		}
	    }
	}
    }	    	

    public final void mousePressed(final int button, final int x, final int y) {
	if (button == 0) { 
	    this.pressStart.setLocation(-GUI.getInstance().getCamera().getTranslateX() + x, -GUI.getInstance().getCamera().getTranslateY() + y);
	}

	GUI.getInstance().getController().mousePressed(button, x, y);
    }

    public final void mouseReleased(final int button, final int x, final int y) {
	GUI.getInstance().getController().mouseReleased(button, x, y);

	if (button == 0 && this.selectionRectVisible) {
	    this.selectionRectVisible = false;

	    if (this.selectionRect.getWidth() * this.selectionRect.getHeight() > 4) {
		GUI.getInstance().getWorld().cancelAllSelection();
		LinkedList<Entity> entities = GUI.getInstance().getWorld().selectMovableEntitiesInsideBox(this.selectionRect);

		GUI.getInstance().getPlayer().selectedEntities.addAll(entities);

		OrderTargeter targeterForEntity = GUI.getInstance().getPlayer().getBestOrderTargeterForTarget(new Target(new Pos(-GUI.getInstance().getCamera().getTranslateX() + x, -GUI.getInstance().getCamera().getTranslateY() + y)));
		if (targeterForEntity != null) {
		    targeterForEntity.entity.playSelectedSound();
		} else {
		    if (!entities.isEmpty()) { 
			((EntityActor) entities.get(0)).playSelectedSound();
		    }
		}
	    }
	}
    }

    public final void keyPressed(final int arg0, final char arg1) {
	/*if (this.container.getInput().isKeyDown(Input.KEY_ESCAPE)) {
	    Main.getInstance().enterState(StatePauseMenu.STATE_ID);
	}*/
    }

    public final int getID() {
	return this.STATE_ID;
    }

    @Override
    public void start(Stage arg0) throws InterruptedException {
	//Profiler.getInstance().startForSection("Render: world");
	//Main.getInstance().getWorld().render(g);
	//Profiler.getInstance().stopForSection("Render: world");

	/*if (this.selectionRectVisible) {
	    g.setLineWidth(2);
	    g.setColor(Color.WHITE);
	    g.draw(selectionRect);

	    g.setColor(this.selectionFillColor);
	    g.fillRect(selectionRect.getMinX() + 2, selectionRect.getMinY() + 2, selectionRect.getWidth() - 2, selectionRect.getHeight() - 2);
	}*/

	//Main.getInstance().getCamera().renderFinish(container, g);

	//Main.getInstance().getSideBar().render(g);

	//PerfomanceGraphRenderer.render(g, new Pos(10, arg0.getHeight() - PerfomanceGraphRenderer.HEIGHT - 10));
	
	//arg0.getDefaultFont().drawString(0, 0, "FPS: " + arg0.getFPS());
	
	//CursorManager.getInstance().drawCursor(g);
    }

    public final void update(final Stage arg0, final int delta)
	    throws Exception {

	Profiler.getInstance().startForSection("Tick");
	
	GUI.getInstance().getController().update(container, delta);
	
	ScreenShaker.getInstance().update(delta);
	//GUI.getInstance().getCamera().update(container, delta);
	updateCursor();

	GUI.getInstance().getWorld().update(delta);
	GUI.getInstance().getBuildingOverlay().update(delta);
	GUI.getInstance().getSideBar().update(delta);

	SoundManager.getInstance().update(delta);
	
	Profiler.getInstance().stopForSection("Tick");
    }

    private void updateCursor() {
	CursorManager.getInstance().update();

	if (--this.cursorUpdateTicks <= 0) {
	    this.cursorUpdateTicks = this.CURSOR_UPDATE_INTERVAL_TICKS;
	    return;
	}

	if (GUI.getInstance().getSideBar().isMouseInsideBar() || GUI.getInstance().getBuildingOverlay().isInBuildingMode()) {
	    CursorManager.getInstance().setCursorType(CursorType.CURSOR_POINTER);
	    return;
	}

	this.cursorUpdateTicks = this.CURSOR_UPDATE_INTERVAL_TICKS;
	int x = 0;//Main.getInstance().getContainer().getInput().getMouseX();
	int y = 0;//Main.getInstance().getContainer().getInput().getMouseY();
	Entity e = GUI.getInstance().getWorld().getEntityInPoint(-GUI.getInstance().getCamera().getTranslateX() + x, -GUI.getInstance().getCamera().getTranslateY() + y);

	// Is there entity under mouse
	Target target;
	if (e != null) {
	    if (this.mouseOverEntity != null) {
		this.mouseOverEntity.isMouseOver = false;
	    }

	    this.mouseOverEntity = e;
	    e.isMouseOver = true;

	    target = new Target(e);
	} else {
	    if (this.mouseOverEntity != null) {
		this.mouseOverEntity.isMouseOver = false;
		this.mouseOverEntity = null;
	    }

	    target = new Target(new Pos((-GUI.getInstance().getCamera().getTranslateX() + x) / 24, (-GUI.getInstance().getCamera().getTranslateY() + y) / 24));
	}

	// We have no selected entities
	if (GUI.getInstance().getPlayer().selectedEntities.isEmpty()) {
	    if (target.isEntityTarget()) {
		CursorManager.getInstance().setCursorType(CursorType.CURSOR_SELECT);
	    } else {
		CursorManager.getInstance().setCursorType(CursorType.CURSOR_POINTER);
	    }
	} else if (!GUI.getInstance().getPlayer().selectedEntities.isEmpty()) {
	    OrderTargeter targeterForEntity = GUI.getInstance().getPlayer().getBestOrderTargeterForTarget(target);

	    if (targeterForEntity != null) {
		CursorManager.getInstance().setCursorType(targeterForEntity.getCursorForTarget(targeterForEntity.entity, target));
	    } else {
		if (e != null) {
		    CursorManager.getInstance().setCursorType(CursorType.CURSOR_SELECT);
		} else {
		    CursorManager.getInstance().setCursorType(CursorType.CURSOR_POINTER);
		}
	    }
	}
    }
}
