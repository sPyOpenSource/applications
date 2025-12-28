package Attic.example_2.Attic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * A drawing area
 * 
 * @@author pcingola@@sinectis.com
 */
public class DrawingArea extends JComponent {

	protected SwarmShow2D controller;
	protected Dimension preferredSize;

	//-------------------------------------------------------------------------
	// Constructor
	//-------------------------------------------------------------------------

	public DrawingArea(SwarmShow2D controller) {
		this.controller = controller;
		setBackground(Color.WHITE);
		setOpaque(true);
	}

	//-------------------------------------------------------------------------
	// Methods
	//-------------------------------------------------------------------------

	/** Clear screen */
	public void clear() {
		paintComponent(this.getGraphics());
	}

	/** Get dimention */
        @Override
	public Dimension getPreferredSize() {
		return controller.getPreferredSize();
	}

	/** Paint */
        @Override
	protected void paintComponent(Graphics g) {
		// Paint background if we're opaque.
		if( isOpaque() ) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	/** Run a swarm */
	protected void runSwarm() {
		Thread thread = new SwarmThread(controller);
                thread.start();
		try {
			thread.join();
		} catch(InterruptedException e) {} // It's OK to interrupt this process
	}

	/** Show swarm's points */
	protected void showSwarm() {
		controller.getSwarm().show(getGraphics(), getForeground(), getWidth(), getHeight(), controller.getShowDimention0(), controller.getShowDimention1(), controller.isShowVelocity());
	}

}
