package jx.awt.peer;


import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.PaintEvent;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import java.util.*;
import jx.awt.*;
import sun.java2d.pipe.Region;


/**
 * This class implements the Label peer. Beside this implementation,
 * it also provides support for a multiple-line format. To use this,
 * just use the jx.awt.ExtendedLabel class.
 */
public class JXLabelPeer
    extends JXComponentPeer
    implements java.awt.peer.LabelPeer {

    private final String newline = System.getProperty("line.separator");
    private final int BORDER = 2;

    /** String containing text if in "normal" mode */
    private String text;
    /** String vector containing text if in multiple-line mode */
    private Vector texts;



    /** Creates a new JXLabelPeer instance */
    public JXLabelPeer(Label parent, JXToolkit toolkit) {
	super(toolkit, parent);
	setText(parent.getText());
	ready = true;
    }




    /**
     * Paints this peer.
     */
    public void paint(JXGraphics g) {
	switch (peerState) {
	case PEER_DISABLED:paintLabel(g, true);break;
	default:paintLabel(g, false);break;
	}
    }

    /**
     * Creates new vector containing all text lines from the string.
     */
    private Vector getMultipleLines(String text) {
	Vector v = new Vector();
	StringTokenizer st = new StringTokenizer(text, newline);
	while (st.hasMoreTokens())
	    v.addElement(st.nextToken());
	return v;
    }

    /** 
     * Calculates the size of the peer.
     */
    private Dimension getLabelSize() {
	Dimension d = new Dimension(0, 0);
	if (parent instanceof jx.awt.ExtendedLabel) {
	    int lines = texts.size();
	    int max = 0;
	    for (int i = 0; i < texts.size(); i++) {
		String s = (String) texts.elementAt(i);
		if (s.length() > max)
		    max = s.length();
	    }
	    d.width = max * CHARWIDTH;
	    d.height = lines * CHARHEIGHT + (lines - 1) * BORDER;
	} else {
	    d.width = text.length() * CHARWIDTH;
	    d.height = CHARHEIGHT;
	}
	return d;
    }


    /*******************************************************
     * methods implemented from LabelPeer                   *
     *******************************************************/



    public void setAlignment(int alignment) {}
            
    public void setText(String text) {
	if (parent instanceof jx.awt.ExtendedLabel)
	    texts = getMultipleLines(text);
	else
	    this.text = text;	
	Dimension d = getLabelSize();
	prefWidth = 2 * BORDER + d.width;
	prefHeight = 2 * BORDER + d.height;
	redraw();
    }

    /***************************** DRAWING ****************************/

    /**
     * Paints the label.
     */
    protected void paintLabel(JXGraphics g, boolean disabled) {
	g.setColor(JXColors.normalBgColor);
	g.fillRect(0, 0, width - 1, height - 1);
	if (disabled)
	    g.setColor(JXColors.disabledTextColor);
	else	
	    g.setColor(JXColors.normalTextColor);
	if (parent instanceof jx.awt.ExtendedLabel) {
	    int x = BORDER;
	    int y = BORDER;
	    for (int i = 0; i < texts.size(); i++) {
		String s = (String) texts.elementAt(i);
		g.drawJXString(s, x, y);
		y += CHARHEIGHT + BORDER;
	    }
	} else
	    g.drawJXString(text, BORDER, (height - CHARHEIGHT) / 2);
    }

    @Override
    public boolean isObscured() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean canDetermineObscurity() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setBounds(int x, int y, int width, int height, int op) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void coalescePaintEvent(PaintEvent e) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ColorModel getColorModel() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setFont(Font f) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void updateCursorImmediately() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean requestFocus(Component lightweightChild, boolean temporary, boolean focusedWindowChangeAllowed, long time, FocusEvent.Cause cause) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isFocusable() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Image createImage(int width, int height) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public VolatileImage createVolatileImage(int width, int height) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean handlesWheelScrolling() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void createBuffers(int numBuffers, BufferCapabilities caps) throws AWTException {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Image getBackBuffer() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void flip(int x1, int y1, int x2, int y2, BufferCapabilities.FlipContents flipAction) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void destroyBuffers() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void reparent(ContainerPeer newContainer) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isReparentSupported() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void layout() {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void applyShape(Region shape) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setZOrder(ComponentPeer above) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        throw new java.lang.UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
