package test.beast.evolution.operators;

import beast.core.*;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.BactrianRandomWalkOperator;
import beast.math.distributions.Normal;
import beast.math.distributions.ParametricDistribution;
import beast.math.distributions.Prior;
import beast.util.Randomizer;
import junit.framework.TestCase;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


public class BactrianRandomWalkOperatorTest extends TestCase {


	/**
	 * @throws Exception 
	 */
	@Test
	public void testNormalDistribution() throws Exception {

		// Fix seed: will hopefully ensure success of test unless something
		// goes terribly wrong.
		Randomizer.setSeed(127);

		// Assemble model:
		RealParameter param = new RealParameter("0.0");
		param.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		ParametricDistribution p = new Normal();
		p.initByName("mean", "1.0", "sigma", "1.0");
		Prior prior = new Prior();
		prior.initByName("x", param, "distr", p);

		
		// Set up state:
		State state = new State();
		state.initByName("stateNode", param);

		// Set up operator:
		BactrianRandomWalkOperator bactrianOperator = new BactrianRandomWalkOperator();
		bactrianOperator.initByName("weight", "1", "parameter", param);

//		RealRandomWalkOperator bactrianOperator = new RealRandomWalkOperator();
//		bactrianOperator.initByName("weight", "1", "parameter", param, "windowSize", 1.0, "useGaussian", true);

		// Set up logger:
		TraceReport traceReport = new TraceReport();
		traceReport.initByName(
				"logEvery", "10",
				"burnin", "2000",
				"log", param,
				"silent", true
				);

		// Set up MCMC:
		MCMC mcmc = new MCMC();
		mcmc.initByName(
				"chainLength", "2000000",
				"state", state,
				"distribution", prior,
				"operator", bactrianOperator,
				"logger", traceReport
				);

		// Run MCMC:
		mcmc.run();

		List<Double> values = traceReport.getAnalysis();
		double[] v = new double[values.size()];
		for (int i = 0; i < v.length; i++) {
			v[i] = values.get(i);
		}
		double m = StatUtils.mean(v);
		double s = StatUtils.variance(v);
		assertEquals(1.0, m, 5e-3);
		assertEquals(1.0, s, 5e-3);


	}

	/**
	 * Modified logger which analyses a sequence of tree states generated
	 * by an MCMC run.
	 */
	public class TraceReport extends Logger {

		public Input<Integer> burninInput = new Input<Integer>("burnin",
				"Number of samples to skip (burn in)", Input.Validate.REQUIRED);

		public Input<Boolean> silentInput = new Input<Boolean>("silent",
				"Don't display final report.", false);

		RealParameter paramToTrack;

		int m_nEvery = 1;
		int burnin;
		boolean silent = false;

		List<Double> values;
		List<Double[]> values2;

		@Override
		public void initAndValidate() {

			List<BEASTObject> loggers = loggersInput.get();
			final int nLoggers = loggers.size();
			if (nLoggers == 0) {
				throw new IllegalArgumentException("Logger with nothing to log specified");
			}

			if (everyInput.get() != null)
				m_nEvery = everyInput.get();

			burnin = burninInput.get();

			if (silentInput.get() != null)
				silent = silentInput.get();

			paramToTrack = (RealParameter)loggers.get(0);
			values = new ArrayList<>();
			values2 = new ArrayList<>();
		}

		@Override
		public void init() { }

		@Override
		public void log(long nSample) {

			if ((nSample % m_nEvery > 0) || nSample<burnin)
				return;

			values.add(paramToTrack.getValue());
			values2.add(paramToTrack.getValues());
		}

		@Override
		public void close() {

			if (!silent) {
				System.out.println("\n----- Tree trace analysis -----------------------");
				double[] v = new double[values.size()];
				for (int i = 0; i < v.length; i++) {
					v[i] = values.get(i);
				}
				double m = StatUtils.mean(v);
				double s = StatUtils.variance(v);
				System.out.println("Mean: " + m + " variance: " + s);
				System.out.println("-------------------------------------------------");
				System.out.println();
				
				try {
					PrintStream log = new PrintStream(new File("/tmp/bactrian.log"));
					log.print("Sample\t");
					int n = values2.get(0).length;
					for (int j = 0; j < n; j++) {
						log.print("param" + (j+1) + "\t");
					}
					log.println();
					for (int i = 0; i < v.length; i++) {
						log.print(i + "\t");
						for (int j = 0; j < n; j++) {
							log.print(values2.get(i)[j] + "\t");
						}
						log.println();
					}
					log.close();
					System.out.println("trace log written to /tmp/bactrian.log");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * Obtain completed analysis.
		 *
		 * @return trace.
		 */
		public List<Double> getAnalysis() {
			return values;
		}
		
		public List<Double[]> getAnalysis2() {
			return values2;
		}

	}
}
