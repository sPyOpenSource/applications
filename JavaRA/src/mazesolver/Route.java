package mazesolver;

/**
 * Contains a Route and uses a Genetic Algorithm to find the best solution.
 */
public class Route {
    private int[] bestOrder;
    private final int[] initOrder;
    private int[][] lengthMatrix, population;

    /**
     * Constructs new Route, and initializes a population.
     * @param length 
     */
    public Route(int length) {
        population = new int[Settings.Route.populationSize][];
        initOrder = new int[length];
        
        for(int i = 0; i < length; i++)
            initOrder[i] = i;
    }
    
    /**
     * Returns the best route found so far.
     * @return order
     */
    public synchronized int[] getBestOrder() {
        return (bestOrder == null) ? new int[0] : bestOrder.clone();
    }
    
    /**
     * Replace order, if a shorter solution is found.
     * @param order 
     */
    public synchronized void updateBestOrder(int[] order) {
        if(bestOrder == null ||
                getRouteLength(order) < getRouteLength(bestOrder))
            bestOrder = order.clone();
    }
    
    /**
     * Calculate length of a route based on order.
     * @param order
     * @return length
     */
    public int getRouteLength(int[] order) {
        int length = 0;

        for(int i = 1; i < order.length; i++)
            length += lengthMatrix[order[i - 1]][order[i]];
        
        return length;
    }
    
    /**
     * Updates length matrix.
     * @param lengthMatrix 
     */
    public void setLengthMatrix(int[][] lengthMatrix) {
        this.lengthMatrix = lengthMatrix;
    }
    
    /**
     * Creates new thread to find best route.
     * @return thread
     */
    public Thread findRoute() {
        // Create new random population
        for(int i = 0; i < Settings.Route.populationSize; i++)
            population[i] = Helper.shuffle(initOrder);
        
        // Return new logic thread
        return new Thread(new RouteLogic());
    }
    
    /**
     * Contains logic of the Genetic Algorithm.
     */
    private class RouteLogic implements Runnable {
        @Override
        public void run() {
            //announce thread start
            Helper.debug("Start thread " + Thread.currentThread().getName());
            
            for(int i = 0; i < Settings.Route.generationsPerRun; i++) {
                int[] bestRoute = null;
                float bestFitness = 0;
                
                float[] fitness = new float[Settings.Route.populationSize];
                int fitnessTotal = 0;
                
                for(int j = 0; j < Settings.Route.populationSize; j++) {
                    // TODO: what if routelength = 0? best route already! stop!

                    // Calculate fitness of each route
                    fitness[j] = 1.0f / getRouteLength(population[j]);
                    
                    // Rlso find best route
                    if(fitness[j] > bestFitness) {
                        bestRoute = population[j];
                        bestFitness = 1.0f / getRouteLength(bestRoute);
                    }
                    
                    // Add to total
                    fitnessTotal += fitness[j];
                }
                
                // Update best route
                updateBestOrder(bestRoute);
                
                // Create new population
                int[][] newPopulation = new int[Settings.Route.populationSize][];
                
                int o = 0;
                
                // Add first route to new population
                newPopulation[o++] = bestRoute;
                
                while(o < Settings.Route.populationSize) {
                    // Temporary order holder
                    int[] tmp = null;
                    
                    // Spin the roulette
                    float roulette = (float) (Math.random() * fitnessTotal);
                    
                    // Choose a route following the roulette principle
                    for(int k = 0; k < Settings.Route.populationSize; k++) {
                        roulette -= fitness[k];
                        if(roulette < 0 || k == Settings.Route.populationSize - 1) {
                            tmp = population[k];
                            break;
                        }
                    }
                    
                    // Do crossover if odds are in favor
                    if(Math.random() < Settings.Route.crossoverOdds) {
                        int n = (int) (Math.random() * o);
                        tmp = Helper.crossover(tmp, newPopulation[n]);
                    }
                    
                    // Mutations
                    if(Math.random() < Settings.Route.invertOdds)
                        tmp = Helper.invert(tmp);
                    if(Math.random() < Settings.Route.shuffleOdds)
                        tmp = Helper.shuffle(tmp);
                    if(Math.random() < Settings.Route.swapOdds)
                        tmp = Helper.swap(tmp);
                    
                    newPopulation[o++] = tmp;
                }
                population = newPopulation;
            }
            
            // Announce thread release
            Helper.debug("Finished thread " + Thread.currentThread().getName());  
        }
    }
}
