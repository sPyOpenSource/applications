package net.pso.jswarm.Attic.example_2.Attic;

import java.util.Random;
import net.pso.jswarm.Particle;

/**
 * Simple particle example
 * @@author Pablo Cingolani <pcingola@@sinectis.com>
 */
public class MyParticle extends Particle {

	/** Number of dimentions for this particle */
	public static int NUMBER_OF_DIMENTIONS = 2;
	
	/** Totally useless, just to see how an example with local data works */

	//-------------------------------------------------------------------------
	// Constructor/s
	//-------------------------------------------------------------------------
	
	/**
	 * Default constructor
	 */
	public MyParticle() {
		super(NUMBER_OF_DIMENTIONS); // Create a 2-dimentional particle
		colorR = new Random().nextInt(256); // Add some custom 'local' data
                colorB = new Random().nextInt(256);
                colorG = new Random().nextInt(256);
	}

	//-------------------------------------------------------------------------
	// Methods
	//-------------------------------------------------------------------------

	/** Convert to string() */
        @Override
	public String toString() {
		String str = super.toString();
		return str + "\tParticle's data: " + colorR + "\n";
	}
        
}
