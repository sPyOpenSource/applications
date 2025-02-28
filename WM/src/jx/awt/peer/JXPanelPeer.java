package jx.awt.peer;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.PaintEvent;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import jx.awt.*;
import sun.java2d.pipe.Region;


/**
 * This class implements the Panel peer. Beside this implementation,
 * it also supports an extended panel interface, which can be used by
 * using the jx.awt.ExtendedPanel class.
 */
public class JXPanelPeer 
    extends JXContainerPeer
    implements java.awt.peer.PanelPeer {



    /** Creates a new JXPanelPeer instance */
    public JXPanelPeer(Panel parent, JXToolkit toolkit) {
	super(toolkit, parent);
	ready = true;
    }


    /**
     * Paints this peer.
     */
    public void paint(JXGraphics g) {
	paintPanel(g);
    }

    /**
     * Sets the insets according to the type of panel.
     */
    public Insets insets() {
	if (parent instanceof jx.awt.ExtendedPanel) {
	    return (((ExtendedPanel) parent).getTitle() == null)
		? new Insets(5, 5, 5, 5)
		: new Insets(CHARHEIGHT + 2, 5, 5, 5);
	} else
	    return new Insets(0, 0, 0, 0);
    }
    
    /**
     * Returns the preferred size of the panel.
     */
    public Dimension getPreferredSize() {
	return new Dimension(0, 0);
    }

    /**************************** DRAWING *****************************/

    /**
     * Paints the panel.
     */
    private void paintPanel(JXGraphics g) {
	g.setColor(JXColors.normalBgColor);
	g.fillRect(0, 0, width - 1, height - 1);
	
	if (parent instanceof jx.awt.ExtendedPanel) {
	    String s = ((ExtendedPanel) parent).getTitle();
	    int h = (s != null) ? (CHARHEIGHT + 2) : 5;
	    int y = h / 2;

	    g.setColor(JXColors.normalBgColor);
	    switch (((ExtendedPanel) parent).getBorderType()) {
	    case ExtendedPanel.BORDER_LINE:
		g.setColor(JXColors.normalBgColor.darker());
		g.drawRect(2, y, width - 5, height - 3 - y);
		break;
	    case ExtendedPanel.BORDER_RAISED:
		g.draw3DRect(2, y, width - 5, height - 3 - y, true);
		g.draw3DRect(3, y + 1, width - 7, height - 5 - y, true);
		break;
	    case ExtendedPanel.BORDER_LOWERED:
		g.draw3DRect(2, y, width - 5, height - 3 - y, false);
		g.draw3DRect(3, y + 1, width - 7, height - 5 - y, false);
		break;
	    case ExtendedPanel.BORDER_ETCHED:
		g.draw3DRect(2, y, width - 5, height - 3 - y, false);
		g.draw3DRect(3, y + 1, width - 7, height - 5 - y, true);
		break;
	    case ExtendedPanel.BORDER_NONE:
	    default:
		break;
	    }
	    if (s != null) {
		g.setColor(JXColors.normalBgColor);
		g.fillRect(7, 0, s.length() * CHARWIDTH + 6, h);
		g.setColor(JXColors.normalTextColor);
		g.drawJXString(s, 10, 1);
	    }
	}
    }

    public void beginLayout() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void endLayout() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean isObscured() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean canDetermineObscurity() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void setBounds(int x, int y, int width, int height, int op) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void coalescePaintEvent(PaintEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public ColorModel getColorModel() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public FontMetrics getFontMetrics(Font font) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void setFont(Font f) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void updateCursorImmediately() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean requestFocus(Component lightweightChild, boolean temporary, boolean focusedWindowChangeAllowed, long time, FocusEvent.Cause cause) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean isFocusable() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public Image createImage(int width, int height) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public VolatileImage createVolatileImage(int width, int height) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public GraphicsConfiguration getGraphicsConfiguration() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean handlesWheelScrolling() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void createBuffers(int numBuffers, BufferCapabilities caps) throws AWTException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public Image getBackBuffer() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void flip(int x1, int y1, int x2, int y2, BufferCapabilities.FlipContents flipAction) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void destroyBuffers() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void reparent(ContainerPeer newContainer) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean isReparentSupported() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void layout() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void applyShape(Region shape) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void setZOrder(ComponentPeer above) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
