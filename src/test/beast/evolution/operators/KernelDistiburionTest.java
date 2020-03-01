package test.beast.evolution.operators;

import org.junit.Test;

import beast.core.util.Log;
import beast.evolution.operators.KernelDistribution;
import beast.evolution.operators.KernelDistribution.Bactrian.mode;
import beast.util.Randomizer;
import junit.framework.TestCase;

public class KernelDistiburionTest extends TestCase {
	
	@Test
	public void testBactrianKernelDistribution() {
		Randomizer.setSeed(127);

		// testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.cauchy));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.t4));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_t4));

		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_strawhat, 0.2), false);
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_strawhat, 0.5), false);
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_airplane, 0.2), false);
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_airplane, 0.5), false);
		
		
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.normal));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.uniform));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.laplace));
		
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.95, mode.bactrian_normal));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.90, mode.bactrian_normal));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.98, mode.bactrian_normal));

		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.98, mode.bactrian_uniform));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.90, mode.bactrian_uniform));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.95, mode.bactrian_uniform));
		
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.95, mode.bactrian_laplace));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.90, mode.bactrian_laplace));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.98, mode.bactrian_laplace));

		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.95, mode.bactrian_triangle));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.90, mode.bactrian_triangle));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(0.98, mode.bactrian_triangle));

		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_box, 0.2));
		testMeanIsZeroSigmaIsOne(new KernelDistribution.Bactrian(mode.bactrian_box, 0.5));

	}

	private void testMeanIsZeroSigmaIsOne(KernelDistribution.Bactrian distr) {
		testMeanIsZeroSigmaIsOne(distr, true);
	}
	private void testMeanIsZeroSigmaIsOne(KernelDistribution.Bactrian distr, boolean hasSigma1) {
		int N = 1000000;
		double m = 0; 
		double s = 0;
		for (int i = 0; i < N; i++) {
			double x = distr.getRandomDelta(0, 1);
			s = s + x*x;
			m += x;
		}
		m /= N;
		s /=  N;
		// mean
		assertEquals(0.0, m, 1e-2);
		// variance
		if (hasSigma1) {
			assertEquals(1.0, s, 1e-2);
		}
		Log.warning("Testing " + distr.kernelmode + " s= " + s);
	}

	@Test
	public void testMirrorfKernelDistribution() {
		Log.warning("Testing mirror distribution");
		Randomizer.setSeed(127);
		
		KernelDistribution.Mirror distr = new KernelDistribution.Mirror(mode.bactrian_normal);

		int N = 10000;
		double m = 0; 
		double s = 0;
		double x = 0, delta = 0;
		for (int i = 0; i < N; i++) {
			if (i == 1510) {
				int h = 3;
				h++;
				System.out.println(h);				
			}
			double n = i + 1;
			delta = distr.getRandomDelta(0, x, 1);
			x += delta;
			s = s + x*x;
			m = x/n + m * (n-1)/n;
			if (Double.isNaN(m)) {
				int h = 3;
				h++;
				System.out.println(h);
			}
		}
		m /= N;
		s /=  N;
		// mean
		assertEquals(0.0, m, 1e-2);
		// variance
		assertEquals(1.0, s, 1e-2);
	}

	@Test
	public void testMultiDimensionalMirrorfKernelDistribution() {
		Log.warning("Testing mirror distribution");
		Randomizer.setSeed(127);
		
		KernelDistribution.Mirror distr = new KernelDistribution.Mirror();
		distr.initByName("mode", mode.bactrian_normal, "onePerDimension", true);

		int N = 10000;
		double []m = new double[3]; 
		double []s = new double[3];
		double []x = new double[3];
		double [] delta = new double[3];
		for (int i = 0; i < N; i++) {
			if (i == 1510) {
				int h = 3;
				h++;
				System.out.println(h);				
			}
			double n = i + 1;
			int dim = Randomizer.nextInt(3);
			delta[dim] = distr.getRandomDelta(dim, x[dim] + dim, 1);
			x[dim] += delta[dim];
			s[dim] = s[dim] + x[dim]*x[dim];
			m[dim] = x[dim]/n + m[dim] * (n-1)/n;
			if (Double.isNaN(m[dim])) {
				int h = 3;
				h++;
				System.out.println(h);
			}
		}
		for (int dim = 0; dim < 3; dim++) {
			m[dim] /= N;
			s[dim] /=  N;
			// mean
			assertEquals(0.0, m[dim], 1e-2);
			// variance
			assertEquals(1.0, s[dim], 1e-2);
		}
	}
}
