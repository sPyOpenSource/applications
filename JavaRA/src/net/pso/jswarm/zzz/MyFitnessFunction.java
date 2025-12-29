package net.pso.jswarm.zzz;

import net.pso.jswarm.FitnessFunction;

/**
 * Sample Fitness function
 * 		f( x1 , x2 ) =  20.0 +(x1 * x1) + (x2 * x2) - 10.0
 */
public class MyFitnessFunction extends FitnessFunction {

	public double penaltyFactor = 1e6;

	@Override
	public double evaluate(double position[]) {
		double x1 = position[0];
		double x2 = position[1];

		// Penalize if (x1+x2) > 500
		double penalty = 0;
		double sumX = (x1 + x2) - 500;
		if( sumX > 0 ) penalty = penaltyFactor * sumX;

		double y = 20.0 + (x1 * x1) + (x2 * x2) - 10.0 - penalty;
		return y;
	}
}
