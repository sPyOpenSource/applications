package Attic;

/**
 * An extremely simple swarm optimization example
 * 
 * Maximize function
 * 		f( x1 , x2 ) = 1 - Sqrt( ( x1 - 1/2 )^2 + ( x2 - 1/2 )^2 )
 * Solution is (obviously): [ 1/2 , 1/2 ]
 * 
 * @@author Pablo Cingolani <pcingola@@sinectis.com>
 */
public class Example {

	//-------------------------------------------------------------------------
	// Main
	//-------------------------------------------------------------------------
	public static void main(String[] args) {
		System.out.println("Begin: Example 1\n");

		// Create a swarm (using 'MyParticle' as sample particle and 'MyFitnessFunction' as finess function)
		Swarm swarm = new Swarm(Swarm.DEFAULT_NUMBER_OF_PARTICLES, new MyParticle(), new MyFitnessFunction());

		// Set position (and velocity) constraints. I.e.: where to look for solutions
		swarm.setMaxPosition(1);
		swarm.setMinPosition(0);

		// Optimize (and time it)
		for( int i = 0; i < 20; i++ )
			swarm.evolve();

		// Print en results
		System.out.println(swarm.toStringStats());
		System.out.println("End: Example 1");
	}
}
