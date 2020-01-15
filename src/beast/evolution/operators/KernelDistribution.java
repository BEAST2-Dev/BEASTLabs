package beast.evolution.operators;

import java.util.Arrays;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.util.Randomizer;

public interface KernelDistribution {
	public static Input<Integer> defaultInitialInput = new Input<>("defaultInitial", "Number of proposals skipped before learning about the val" , 200);
	public static Input<Integer> defaultBurninInput = new Input<>("defaultBurnin", "Number of proposals skipped before any learned informatin is applied" , 200);

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
	
	static KernelDistribution newDefaultKernelDistribution() {
//		Bactrian kdist = new Bactrian();
//		return kdist;
		MirrorDistribution kdist = new MirrorDistribution();
		kdist.initAndValidate();
		return kdist;
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
				+ "If not specified (or < 0), the operator uses " + defaultInitialInput.get(), -1); 
		final public Input<Integer> burninInput = new Input<>("burnin", "Number of proposals that are ignored before m and s are being updated. "
				+ "If initial is not specified, uses half the default initial value (which equals " + defaultBurninInput.get() + ")", 0);
		
		double mean, sigma;
		int callcount;
		int initial, burnin;
		double sum, sum2, low, up;
		double [] cache;
		int cacheIndex;

		@Override
		public void initAndValidate() {
			callcount = 0;
			initial = initialInput.get();
			if (initial < 0) {
				initial = defaultInitialInput.get();
			}
			burnin = burninInput.get();
			if (burnin <= 0) {
				burnin = defaultBurninInput.get();
			}
			sum = 0; sum2 = 0;	
			low = Double.POSITIVE_INFINITY;
			up = Double.NEGATIVE_INFINITY;
			
			
			cache = new double[200];
			cacheIndex = 0;
		}
				
//		@Override
//		public double getScaler(double value, double scaleFactor) {
//			callcount++;
//			if (callcount > initial) {
//				sum += value;
//				sum2 += value * value;
//				up = Math.max(up,  value);
//				low = Math.min(low, value);
//				cache[cacheIndex] = value;				
//				cacheIndex++;
//				if (cacheIndex == cache.length) {
//					cacheIndex = 0;
//				}
//			}
//			if (Double.isNaN(value) || callcount < initial + burnin) {
//				return super.getScaler(value, scaleFactor);
//			}
//			
//			double mean = sum / (callcount - initial);
//			
//			double [] x = new double[cache.length];
//			System.arraycopy(cache, 0, x, 0, cache.length);
//			Arrays.sort(x);
//			mean = x[x.length/2];
//			
//			double scale;
//	        if (value < mean) {
//	        	scale = scaleFactor * (m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
//	        } else {
//	        	scale = scaleFactor * (-m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
//	        }
//	        scale = Math.exp(scale);
//			return scale;
//		}
//		
		@Override
		public double getRandomDelta(double value, double windowSize) {
			callcount++;
			if (callcount > initial) {
				if (callcount < initial + burnin)
				sum += value;
//				sum -= cache[cacheIndex]; // initially, cache contains zeros, till cache.length updates have been done

				sum2 += value * value;
				up = Math.max(up,  value);
				low = Math.min(low, value);
				cache[cacheIndex] = value;				
				cacheIndex++;
				if (cacheIndex == cache.length) {
					cacheIndex = 0;
				}
			}
			if (Double.isNaN(value) || callcount < initial + burnin) {
				return super.getRandomDelta(value, windowSize);
			}
			
			double mean = sum / burnin;//(callcount - initial);
			// double stdev = Math.sqrt((sum * sum - sum2)/(callcount - initial));
			double stdev = (up - low) / 4.0;

//			up = cache[0];
//			low = cache[0];
//			for (double d : cache) {
//				up = Math.max(d, up);
//				low = Math.min(d, low);
//			}
//			
//			mean = sum / cache.length;			
			stdev = (up - low) / 4.0;
			
			double delta = windowSize * Randomizer.nextGaussian() * stdev;			
			return delta + (2 * mean - 2 * value);
		} 

		
	}
	
}
