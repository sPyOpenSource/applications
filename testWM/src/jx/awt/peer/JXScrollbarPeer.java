package jx.awt.peer;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import jx.awt.*;
import sun.java2d.pipe.Region;


/**
 * This class implements a Scrollbar peer. It simply uses
 * an InternalScrollbar class and forwards all events to it.
 */
public class JXScrollbarPeer
    extends JXComponentPeer
    implements java.awt.peer.ScrollbarPeer {
    
    private InternalScrollbar scrollbar;
    private int lastArea;
    private int oldValue;
    
    
    /** Creates a new JXScrollbarPeer instance */
    public JXScrollbarPeer(Scrollbar parent, JXToolkit toolkit) {
	super(toolkit, parent);
	scrollbar = new InternalScrollbar(this, toolkit);
	scrollbar.setOrientation(parent.getOrientation());
	setLineIncrement(parent.getLineIncrement());
	setPageIncrement(parent.getPageIncrement());
	setValues(parent.getValue(), parent.getVisibleAmount(),
		  parent.getMinimum(), parent.getMaximum());
	ready = true;
    }
    
    
    
    public void mousePressed(int x, int y, int button) {
	super.mousePressed(x, y, button);
	if (scrollbar.mouseInScrollbarPressed(x, y, button) != scrollbar.NONE)
	    redraw();
    }
    
    /**
     * Called from JXWindowConnector when a mouse button was released in the scroll bar.
     * This method forwards the event to the InternalScrollbar and takes the result to
     * send an adjustment event.
     */
    public void mouseReleased(int x, int y, int button, boolean over) {
	super.mouseReleased(x, y, button, over);
	int adjustType = 0;
	boolean usable = true;
	switch (scrollbar.mouseInScrollbarReleased(x, y, button, over)) {
	case InternalScrollbar.ARROWTOP:
            adjustType = AdjustmentEvent.UNIT_DECREMENT;
            break;
	case InternalScrollbar.ARROWBOTTOM:
            adjustType = AdjustmentEvent.UNIT_INCREMENT;
            break;
	case InternalScrollbar.SLIDERTOP:
            adjustType = AdjustmentEvent.BLOCK_DECREMENT;
            break;
	case InternalScrollbar.SLIDERBOTTOM:
            adjustType = AdjustmentEvent.BLOCK_INCREMENT;
            break;
	default:usable = false;
	}
	if (usable && oldValue != scrollbar.getValue()) {
	    oldValue = scrollbar.getValue();
	    queue = toolkit.getSystemEventQueue();
	    queue.postEvent(new AdjustmentEvent(((Adjustable) parent),
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						adjustType, oldValue));
	}
	redraw();
    }
    
    /**
     * Called from JXWindowConnector when the mouse is moved in the scroll bar.
     * If the "bubble" is dragged, then an adjustment event is sent.
     */
    public void mouseMoved(int x, int y, int button) {
	super.mouseMoved(x, y, button);
	int ret = scrollbar.mouseInScrollbarMoved(x, y, button);
	if (button !=0 && ret == scrollbar.BUBBLE) {
	    // bubble is being dragged
	    oldValue = scrollbar.getValue();
	    queue = toolkit.getSystemEventQueue();
	    queue.postEvent(new AdjustmentEvent(((Adjustable) parent),
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.TRACK, oldValue));
	}
	if (ret != lastArea || button != 0) {
	    lastArea = ret;
	    redraw();
	}
    }
    
    public void mouseEntered(int x, int y, int button) {
	super.mouseEntered(x, y, button);
	scrollbar.mouseInScrollbarEntered(x, y, button);
	redraw();
    }
    
    public void mouseExited(int x, int y, int button) {
	super.mouseExited(x, y, button);
	scrollbar.mouseInScrollbarExited(x, y, button);
	redraw();
    }

    /** Gets the current value of the internal scroll bar. */
    public int getValue() {
	return scrollbar.getValue();
    }

    public void dispose() {
	scrollbar = null;
	super.dispose();
    }
    
    /** Paints this peer. */
    public void paint(JXGraphics g) {
	switch (peerState) {
	case PEER_NORMAL:
	case PEER_HOVER:paintScrollbar(g, false, false);break;
	case PEER_PRESSED:paintScrollbar(g, true, false);break;
	case PEER_DISABLED:paintScrollbar(g, false, true);break;
	default:System.out.println("*** JXScrollbarPeer: Unknown state!!!");
	}
    }

    /********************************************************
     * methods implemented from ScrollbarPeer                *
     ********************************************************/
    
    public void setLineIncrement(int increment) {
	scrollbar.setSmallIncrement(increment);
    }
    
    public void setPageIncrement(int increment) {
	scrollbar.setBigIncrement(increment);
    }
    
    public void setValues(int value, int visibleAmount, int min, int max) {
	scrollbar.setValues(value, visibleAmount, min, max);
	// set oldValue to be sure that first event always occures
	oldValue = min - 1;
	prefWidth = scrollbar.getPrefWidth();
	prefHeight = scrollbar.getPrefHeight();
    }
    
    /******************** DRAWING ************************/

    /** Paints the scroll bar. */
    private void paintScrollbar(JXGraphics g, boolean pressed, boolean disabled) {
	// do scrollbar specific drawing
	scrollbar.setScrollArea(0, 0, width, height);
	scrollbar.paintScrollbar(g, pressed, disabled);
    }

    @Override
    public boolean isObscured() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean canDetermineObscurity() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setBounds(int x, int y, int width, int height, int op) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void coalescePaintEvent(PaintEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ColorModel getColorModel() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setFont(Font f) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void updateCursorImmediately() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean requestFocus(Component lightweightChild, boolean temporary, boolean focusedWindowChangeAllowed, long time, FocusEvent.Cause cause) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isFocusable() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Image createImage(int width, int height) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public VolatileImage createVolatileImage(int width, int height) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean handlesWheelScrolling() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void createBuffers(int numBuffers, BufferCapabilities caps) throws AWTException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Image getBackBuffer() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void flip(int x1, int y1, int x2, int y2, BufferCapabilities.FlipContents flipAction) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void destroyBuffers() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void reparent(ContainerPeer newContainer) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isReparentSupported() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void layout() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void applyShape(Region shape) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setZOrder(ComponentPeer above) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
