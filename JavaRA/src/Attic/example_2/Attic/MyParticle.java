package Attic.example_2.Attic;

import Attic.Particle;

/**
 * Simple particle example
 * @@author Pablo Cingolani <pcingola@@sinectis.com>
 */
public class MyParticle extends Particle {

	/** Number of dimentions for this particle */
	public static int NUMBER_OF_DIMENTIONS = 2;
	
	/** Totally useless, just to see how an example with local data works */
	double particleData;

	//-------------------------------------------------------------------------
	// Constructor/s
	//-------------------------------------------------------------------------
	
	/**
	 * Default constructor
	 */
	public MyParticle() {
		super(NUMBER_OF_DIMENTIONS); // Create a 2-dimentional particle
		particleData = Math.random(); // Add some custom 'local' data
	}

	//-------------------------------------------------------------------------
	// Methods
	//-------------------------------------------------------------------------

	/** Convert to string() */
        @Override
	public String toString() {
		String str = super.toString();
		return str + "\tParticle's data: " + particleData + "\n";
	}
        
}
