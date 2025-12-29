package Attic;

import java.awt.Color;
import java.awt.Graphics;
import net.pso.jswarm.FitnessFunction;

/**
 * A swarm of particles
 * @@author Pablo Cingolani <pcingola@@sinectis.com>
 */
public class Swarm {

	public static double DEFAULT_GLOBAL_INCREMENT = 0.1;
	public static double DEFAULT_INERTIA = 0.5;
	public static int DEFAULT_NUMBER_OF_PARTICLES = 8;
	public static double DEFAULT_OTHER_PARTICLE_INCREMENT = 0.1;
	public static double DEFAULT_PARTICLE_INCREMENT = 0.1;
	public static double DEFAULT_RANDOM_INCREMENT = 0.1;

	/** Best fitness so far (global best) */
	double bestFitness;
	/** Index of best particle so far */
	int bestParticleIndex;
	/** Best position so far (global best) */
	double bestPosition[];
	/** Fitness function for this swarm */
	FitnessFunction fitnessFunction;
	/** Global increment (for velocity update) */
	double globalIncrement;
	/** Inertia (for velocity update) */
	double inertia;
	/** Maximum position (for each dimentions) */
	double maxPosition[];
	/** Maximum Velocity (for each dimentions) */
	double maxVelocity[];
	/** Minimum position (for each dimentions) */
	double minPosition[];
	/** Minimum Velocity for each dimention. WARNING: Velocity is no in Abs value (so setting minVelocity to 0 is NOT correct!) */
	double minVelocity[];
	/** How many times 'particle.evaluate()' is called? */
	int numberOfEvaliations;
	/** Number of particles in this swarm */
	int numberOfParticles;
	/** Other particle increment */
	double otherParticleIncrement;
	/** Particle's increment (for velocity update) */
	double particleIncrement;
	/** Particles in this swarm */
	Particle particles[];
	/** Random increment */
	double randomIncrement;
	/** A sample particles: Build other particles based on this one */
	Particle sampleParticle;
	/** Use Repulsive Algorithm */
	boolean useRepulsiveAlgorithm;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/**
	 * Create a Swarm and set default values
	 * @@param numberOfParticles : Number of particles in this swarm (should be greater than 0). 
	 * If unsure about this parameter, try Swarm.DEFAULT_NUMBER_OF_PARTICLES or greater
	 * @@param sampleParticle : A particle that is a sample to build all other particles
	 * @@param fitnessFunction : Fitness function used to evaluate each particle
	 */
	public Swarm(int numberOfParticles, Particle sampleParticle, FitnessFunction fitnessFunction) {
		if( sampleParticle == null ) throw new RuntimeException("Sample particle can't be null!");
		if( numberOfParticles <= 0 ) throw new RuntimeException("Number of particles should be greater than zero.");

		this.numberOfParticles = DEFAULT_NUMBER_OF_PARTICLES;
		this.globalIncrement = DEFAULT_GLOBAL_INCREMENT;
		this.inertia = DEFAULT_INERTIA;
		this.particleIncrement = DEFAULT_PARTICLE_INCREMENT;
		this.numberOfEvaliations = 0;
		this.numberOfParticles = numberOfParticles;
		this.sampleParticle = sampleParticle;
		this.fitnessFunction = fitnessFunction;
		this.useRepulsiveAlgorithm = false;
		this.randomIncrement = DEFAULT_RANDOM_INCREMENT;
		this.otherParticleIncrement = DEFAULT_OTHER_PARTICLE_INCREMENT;
	}

	//-------------------------------------------------------------------------
	// Methods
	//-------------------------------------------------------------------------

	/**
	 * Evaluate fitness function for every particle 
	 * Warning: particles[] must be initialized and fitnessFunction must be setted
	 */
	public void evaluate() {
		if( particles == null ) throw new RuntimeException("No particles in this swarm! May be you need to call Swarm.init() method");
		if( fitnessFunction == null ) throw new RuntimeException("No fitness function in this swarm! May be you need to call Swarm.setFitnessFunction() method");

		// Initialize
		double bestFit;
		int bestFitNum = -1;
		bestFit = (fitnessFunction.isMaximize() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);

		//---
		// Evaluate each particle (and find the 'best' one)
		//---
		for( int i = 0; i < particles.length; i++ ) {
			double fit = fitnessFunction.evaluate(particles[i]); // Evaluate particle
			numberOfEvaliations++; // Update counter
			if( (fitnessFunction.isMaximize() && (fit > bestFit)) // Maximize?
					|| (!fitnessFunction.isMaximize() && (fit < bestFit)) ) { // Minimize 
				bestFit = fit;
				bestFitNum = i;
			}
		}

		//---
		// Copy 'best position'
		//---
		this.bestParticleIndex = bestFitNum;
		this.bestFitness = bestFit;
		if( bestPosition == null ) bestPosition = new double[sampleParticle.getDimension()];
                if(bestFitNum == -1) return;
		particles[bestFitNum].copyPosition(bestPosition);
	}

	/**
	 * Make an iteration: 
	 * 	- evaluates the swarm 
	 * 	- updates positions and velocities
	 * 	- applies positions and velocities constraints 
	 */
	public void evolve() {
		// Init (if not already done)
		if( particles == null ) init();

		evaluate(); // Evaluate particles
		update(); // update positions and velocities
	}

	public double getBestFitness() {
		return bestFitness;
	}

	public Particle getBestParticle() {
		return particles[bestParticleIndex];
	}

	public int getBestParticleIndex() {
		return bestParticleIndex;
	}

	public double[] getBestPosition() {
		return bestPosition;
	}

	public FitnessFunction getFitnessFunction() {
		return fitnessFunction;
	}

	public double getGlobalIncrement() {
		return globalIncrement;
	}

	public double getInertia() {
		return inertia;
	}

	public double[] getMaxPosition() {
		return maxPosition;
	}

	public double[] getMaxVelocity() {
		return maxVelocity;
	}

	public double[] getMinPosition() {
		return minPosition;
	}

	public double[] getMinVelocity() {
		return minVelocity;
	}

	public int getNumberOfEvaliations() {
		return numberOfEvaliations;
	}

	public int getNumberOfParticles() {
		return numberOfParticles;
	}

	public double getOtherParticleIncrement() {
		return otherParticleIncrement;
	}

	public double getParticleIncrement() {
		return particleIncrement;
	}

	public Particle[] getParticles() {
		return particles;
	}

	public double getRandomIncrement() {
		return randomIncrement;
	}

	public Particle getSampleParticle() {
		return sampleParticle;
	}

	/**
	 * Initialize every particle
	 * Warning: maxPosition[], minPosition[], maxVelocity[], minVelocity[] must be initialized and setted
	 */
	public void init() {
		// Init particles
		particles = new Particle[numberOfParticles];

		// Check constraints (they will be used to initialize particles)
		if( maxPosition == null ) throw new RuntimeException("maxPosition array is null!");
		if( minPosition == null ) throw new RuntimeException("maxPosition array is null!");
		if( maxVelocity == null ) {
			// Default maxVelocity[]
			int dim = sampleParticle.getDimension();
			maxVelocity = new double[dim];
			for( int i = 0; i < dim; i++ )
				maxVelocity[i] = (maxPosition[i] - minPosition[i]) / 2.0;
		}
		if( minVelocity == null ) {
			// Default minVelocity[]
			int dim = sampleParticle.getDimension();
			minVelocity = new double[dim];
			for( int i = 0; i < dim; i++ )
				minVelocity[i] = -maxVelocity[i];
		}

		// Init each particle
		for( int i = 0; i < numberOfParticles; i++ ) {
			particles[i] = (Particle) sampleParticle.selfFactory(); // Create a new particles (using 'sampleParticle' as reference)
			particles[i].init(maxPosition, minPosition, maxVelocity, minVelocity); // Initialize it
		}
	}

	public boolean isUseRepulsiveAlgorithm() {
		return useRepulsiveAlgorithm;
	}

	public void setBestParticleIndex(int bestParticle) {
		this.bestParticleIndex = bestParticle;
	}

	public void setBestPosition(double[] bestPosition) {
		this.bestPosition = bestPosition;
	}

	public void setFitnessFunction(FitnessFunction fitnessFunction) {
		this.fitnessFunction = fitnessFunction;
	}

	public void setGlobalIncrement(double globalIncrement) {
		this.globalIncrement = globalIncrement;
	}

	public void setInertia(double inertia) {
		this.inertia = inertia;
	}

	/**
	 * Sets every maxVelocity[] and minVelocity[] to 'maxVelocity' and '-maxVelocity' respectively
	 * @@param maxVelocity
	 */
	public void setMaxMinVelocity(double maxVelocity) {
		if( sampleParticle == null ) throw new RuntimeException("Need to set sample particle before calling this method (use Swarm.setSampleParticle() method)");
		int dim = sampleParticle.getDimension();
		this.maxVelocity = new double[dim];
		this.minVelocity = new double[dim];
		for( int i = 0; i < dim; i++ ) {
			this.maxVelocity[i] = maxVelocity;
			this.minVelocity[i] = -maxVelocity;
		}
	}

	/**
	 * Sets every maxPosition[] to 'maxPosition'
	 * @@param maxPosition
	 */
	public void setMaxPosition(double maxPosition) {
		if( sampleParticle == null ) throw new RuntimeException("Need to set sample particle before calling this method (use Swarm.setSampleParticle() method)");
		int dim = sampleParticle.getDimension();
		this.maxPosition = new double[dim];
		for( int i = 0; i < dim; i++ )
			this.maxPosition[i] = maxPosition;
	}

	public void setMaxPosition(double[] maxPosition) {
		this.maxPosition = maxPosition;
	}

	public void setMaxVelocity(double[] maxVelocity) {
		this.maxVelocity = maxVelocity;
	}

	/**
	 * Sets every minPosition[] to 'minPosition'
	 * @@param minPosition
	 */
	public void setMinPosition(double minPosition) {
		if( sampleParticle == null ) throw new RuntimeException("Need to set sample particle before calling this method (use Swarm.setSampleParticle() method)");
		int dim = sampleParticle.getDimension();
		this.minPosition = new double[dim];
		for( int i = 0; i < dim; i++ )
			this.minPosition[i] = minPosition;
	}

	public void setMinPosition(double[] minPosition) {
		this.minPosition = minPosition;
	}

	public void setMinVelocity(double minVelocity[]) {
		this.minVelocity = minVelocity;
	}

	public void setNumberOfEvaliations(int numberOfEvaliations) {
		this.numberOfEvaliations = numberOfEvaliations;
	}

	public void setNumberOfParticles(int numberOfParticles) {
		this.numberOfParticles = numberOfParticles;
	}

	public void setOtherParticleIncrement(double otherParticleIncrement) {
		this.otherParticleIncrement = otherParticleIncrement;
	}

	public void setParticleIncrement(double particleIncrement) {
		this.particleIncrement = particleIncrement;
	}

	public void setParticles(Particle[] particle) {
		this.particles = particle;
	}

	public void setRandomIncrement(double randomIncrement) {
		this.randomIncrement = randomIncrement;
	}

	public void setSampleParticle(Particle sampleParticle) {
		this.sampleParticle = sampleParticle;
	}

	public void setUseRepulsiveAlgorithm(boolean useRepulsiveAlgorithm) {
		this.useRepulsiveAlgorithm = useRepulsiveAlgorithm;
	}

	/**
	 * Show a swarm in a graph 
	 * @@param graphics : Grapics object
	 * @@param foreground : foreground color
	 * @@param width : graphic's width
	 * @@param height : graphic's height
	 * @@param dim0 : Dimention to show ('x' axis)
	 * @@param dim1 : Dimention to show ('y' axis)
	 * @@param showVelocity : Show velocity tails?
	 */
	public void show(Graphics graphics, Color foreground, int width, int height, int dim0, int dim1, boolean showVelocity) {
		graphics.setColor(foreground);

		if( particles != null ) {
			double scalePosW = width / (maxPosition[dim0] - minPosition[dim0]);
			double scalePosH = height / (maxPosition[dim1] - minPosition[dim1]);
			double minPosW = minPosition[dim0];
			double minPosH = minPosition[dim1];

			double scaleVelW = width / (maxVelocity[dim0] - minVelocity[dim0]);
			double scaleVelH = height / (maxVelocity[dim1] - minVelocity[dim1]);
			double minVelW = minVelocity[dim0] + (maxVelocity[dim0] - minVelocity[dim0]) / 2;
			double minVelH = minVelocity[dim1] + (maxVelocity[dim1] - minVelocity[dim1]) / 2;

                    for (Particle particle : particles) {
                        int vx, vy, x, y;
                        double[] pos = particle.getPosition();
                        double[] vel = particle.getVelocity();
                        x = (int) (scalePosW * (pos[dim0] - minPosW));
                        y = height - (int) (scalePosH * (pos[dim1] - minPosH));
                        graphics.setColor(new Color(particle.colorR, particle.colorB, particle.colorG));
                        graphics.fillRect(x - 1, y - 1, 3, 3);
                        if( showVelocity ) {
                            vx = (int) (scaleVelW * (vel[dim0] - minVelW));
                            vy = (int) (scaleVelH * (vel[dim1] - minVelH));
                            graphics.drawLine(x, y, x + vx, y + vy);
                        }
                    }
		}
	}

        @Override
	public String toString() {
		String str = "";

		if( particles != null ) str += "Swarm size: " + particles.length + "\n";

		if( (minPosition != null) && (maxPosition != null) ) {
			str += "Position ranges:\t";
			for( int i = 0; i < maxPosition.length; i++ )
				str += "[" + minPosition[i] + ", " + maxPosition[i] + "]\t";
		}

		if( (minVelocity != null) && (maxVelocity != null) ) {
			str += "\nVelocity ranges:\t";
			for( int i = 0; i < maxVelocity.length; i++ )
				str += "[" + minVelocity[i] + ", " + maxVelocity[i] + "]\t";
		}

		if( sampleParticle != null ) str += "\nSample particle: " + sampleParticle;

		if( particles != null ) {
			str += "\nParticles:";
			for( int i = 0; i < particles.length; i++ ) {
				str += "\n\tParticle: " + i + "\t";
				str += particles[i].toString();
			}
		}
		str += "\n";

		return str;
	}

	/**
	 * Return a string with some (very basic) statistics 
	 * @@return A string
	 */
	public String toStringStats() {
		String stats = "";
		if( !Double.isNaN(bestFitness) ) {
			stats += "Best fitness: " + bestFitness + "\nBest position: \t[";
			for( int i = 0; i < bestPosition.length; i++ )
				stats += bestPosition[i] + (i < (bestPosition.length - 1) ? ", " : "");
			stats += "]\nNumber of evaluations: " + numberOfEvaliations + "\n";
		}
		return stats;
	}

	/**
	 * Update every particle's position and velocity, also apply position and velocity constraints (if any)
	 * Warning: Particles must be already evaluated
	 */
	public void update() {
		// For each particle...
		for( int i = 0; i < particles.length; i++ ) {
			// Update according to algorithm type
			if( useRepulsiveAlgorithm ) {
				// Randomly select other particle
				int randOtherParticle = ((int) (Math.random() * (particles.length - 1)) + i) % particles.length;
				// Update
				particles[i].updateRepulsive(inertia, particleIncrement, otherParticleIncrement, randomIncrement, particles[randOtherParticle].getPosition(), minVelocity, maxVelocity);
			} else {
				particles[i].update(inertia, particleIncrement, globalIncrement, bestPosition);
                                for(Particle particle : particles){
                                    for(Particle other : particles){
                                        if(other == particle) continue;
                                        for(int j = 0; j < particle.position.length; j++){
                                            particle.velocity[j] += 0.1/(particle.position[j] - other.position[j]);
                                        }
                                    }
                                }
			}

			// Apply position and velocity constraints
			particles[i].applyConstraints(minPosition, maxPosition, minVelocity, maxVelocity);
		}
	}
}
