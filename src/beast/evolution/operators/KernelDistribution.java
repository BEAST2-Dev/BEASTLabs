package beast.evolution.operators;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.util.Log;
import beast.util.Randomizer;

public interface KernelDistribution {

	/**
	 * @param m determines shape of Bactrian distribution. m=0.95 is recommended
	 * @param scaleFactor determines range of scale values, larger is bigger random scale changes
	 * @return random scale factor for scaling parameters
	 */
	public double getScaler(double value, double scaleFactor);
	default public double getScaler(double windowSize) {
		return getScaler(Double.NaN, windowSize);
	}

	/**
	 * @param m determines shape of Bactrian distribution. m=0.95 is recommended
	 * @param windowSize determines range of random delta values, larger is bigger random updates
	 * @return random delta value for random walks
	 */
	public double getRandomDelta(double value, double windowSize);
	default public double getRandomDelta(double windowSize) {
		return getRandomDelta(Double.NaN, windowSize);
	}

	@Description("Kernel distribution with two modes, so called Bactrian distribution")
	public class Bactrian extends BEASTObject implements KernelDistribution {
	    final public Input<Double> windowSizeInput = new Input<>("m", "standard deviation for Bactrian distribution. "
	    		+ "Larger values give more peaked distributions. "
	    		+ "The default 0.95 is claimed to be a good choice (Yang 2014, book p.224).", 0.95);
	    
	    double m = 0.95;
	    
		@Override
		public void initAndValidate() {
			m = windowSizeInput.get();
	        if (m <=0 || m >= 1) {
	        	throw new IllegalArgumentException("m should be withing the (0,1) range");
	        }
		}

		public double getScaler(double oldValue, double scaleFactor) {
	        double scale = 0;
	        if (Randomizer.nextBoolean()) {
	        	scale = scaleFactor * (m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
	        } else {
	        	scale = scaleFactor * (-m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
	        }
	        scale = Math.exp(scale);
			return scale;
		}
		
		public double getRandomDelta(double oldValue, double windowSize) {
	        double value;
	        if (Randomizer.nextBoolean()) {
	        	value = windowSize * (m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
	        } else {
	        	value = windowSize * (-m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
	        }
	        return value;
		}	
	}
	
	@Description("Distribution that learns mean m and variance s from the values provided")
	public class MirrorDistribution extends Bactrian {
		final public Input<Integer> initialInput = new Input<>("initial", "Number of proposals before m and s are considered in proposal. "
				+ "Must be larger than burnin, if specified. "
				+ "If not specified (or < 0), the operator uses 200", -1); 
		final public Input<Integer> burninInput = new Input<>("burnin", "Number of proposals that are ignored before m and s are being updated. "
				+ "If initial is not specified, uses half the default initial value (which equals 100)", 0);
		
		double mean, sigma;
		int callcount;
		int initial, burnin;
		double sum, sum2;

		@Override
		public void initAndValidate() {
			callcount = 0;
			initial = initialInput.get();
			if (initial < 0) {
				initial = 200;
			}
			burnin = burninInput.get();
			if (burnin <= 0) {
				burnin = 100;
			}
			sum = 0; sum2 = 0;					
		}
				
		@Override
		public double getScaler(double value, double scaleFactor) {
			callcount++;
			if (callcount > initial) {
				sum += value;
				sum2 += value * value;
			}
			if (Double.isNaN(value) || callcount < initial + burnin) {
				return super.getScaler(value, scaleFactor);
			}
			
			double mean = sum / (callcount - initial);
			
			double scale = scaleFactor * (2*mean - value) * Randomizer.nextGaussian();
			
			return scale;
		}
		
		@Override
		public double getRandomDelta(double value, double windowSize) {
			callcount++;
			if (callcount > initial) {
				sum += value;
				sum2 += value * value;
			}
			if (Double.isNaN(value) || callcount < initial + burnin) {
				return super.getRandomDelta(value, windowSize);
			}
			
			double mean = sum / (callcount - initial);
			double stdev = Math.sqrt((sum * sum - sum2)/(callcount - initial));
			
			double delta = windowSize * Randomizer.nextGaussian() * stdev;
			
			return delta + (2 * mean - value);
		} 

		
	}
	
}
