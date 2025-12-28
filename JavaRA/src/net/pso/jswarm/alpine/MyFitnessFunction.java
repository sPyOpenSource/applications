package net.pso.jswarm.alpine;

import net.pso.jswarm.FitnessFunction;

/**
 * Maximize Alpine function,  http://clerc.maurice.free.fr/pso/Alpine/Alpine_Function.htm
 * 	
 * 		f( x1 , x2 ) = sin( x2 ) * sin( x2 ) * Sqrt( x1 * x2 )
 * 	
 * @@author Alvaro Jaramillo Duque <aduque@@inescporto.pt>
 */
public class MyFitnessFunction extends FitnessFunction {

	//-------------------------------------------------------------------------
	// Methods
	//-------------------------------------------------------------------------

	/**
	 * Evaluates a particles at a given position
	 * @@param position : Particle's position
	 * @@return Fitness function for a particle
	 */
        @Override
	public double evaluate(double position[]) {
		double x1 = position[0];
		double x2 = position[1];
		return Math.sin(x1) * Math.sin(x2) * Math.sqrt(x1 * x2);
	}

}
