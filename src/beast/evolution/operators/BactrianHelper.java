package beast.evolution.operators;

import beast.util.Randomizer;

public interface BactrianHelper {

	/**
	 * @param m determines shape of Bactrian distribution. m=0.95 is recommended
	 * @param scaleFactor determines range of scale values, larger is bigger random scale changes
	 * @return random scale factor for scaling parameters
	 */
	static public double getScaler(double m, double scaleFactor) {
        double scale = 0;
        if (Randomizer.nextBoolean()) {
        	scale = scaleFactor * (m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
        } else {
        	scale = scaleFactor * (-m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
        }
        scale = Math.exp(scale);
		return scale;
	}
	
	/**
	 * 
	 * @param m determines shape of Bactrian distribution. m=0.95 is recommended
	 * @param windowSize determines range of random delta values, larger is bigger random updates
	 * @return random delta value for random walks
	 */
	static public double getRandomDelta(double m, double windowSize) {
        double value;
        if (Randomizer.nextBoolean()) {
        	value = windowSize * (m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
        } else {
        	value = windowSize * (-m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
        }
        return value;
	}
	
}
