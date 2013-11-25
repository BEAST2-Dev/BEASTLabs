package beast.inference;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.distribution.BetaDistribution;
import org.apache.commons.math.distribution.BetaDistributionImpl;

import beast.core.Description;
import beast.core.BEASTObject;
import beast.util.LogAnalyser;



@Description("Reads logs produces through PathSampler and estimates marginal likelihood")
public class PathSampleAnalyser extends BEASTObject {
	
	DecimalFormat formatter;
	
	@Override
	public void initAndValidate() throws Exception {
	}
	
	/** estimate marginal likelihoods from logs produced by PathSampler
	 * @param nSteps number of steps used by PathSampler
	 * @param alpha  if < 0 uniform intervals are used, otherwise a Beta(alpha,1.0) distribution is used for intervals
	 * @param rootDir location where log files are stored
	 * @param burnInPercentage percentage of log files to be discarded
	 * @return log of marginal likelihood
	 * @throws Exception
	 */
	double estimateMarginalLikelihood(int nSteps, double alpha, String rootDir, int burnInPercentage) throws Exception {
		List<List<Double>> logdata = new ArrayList<List<Double>>(); 
		
		
		String sFormat = "";
		for (int i = nSteps; i > 0; i /= 10) {
			sFormat += "#";
		}
		formatter = new DecimalFormat(sFormat);

		// collect likelihood estimates for each step
		double [] marginalLs = new double[nSteps];
		Double [] [] marginalLs2 = new Double[nSteps][];
		for (int i = 0; i < nSteps; i++) {
			List<Double> logdata1 = new ArrayList<Double>();
			String logFile = getStepDir(rootDir, i) + "/" + PathSampler.LIKELIHOOD_LOG_FILE;
			LogAnalyser analyser = new LogAnalyser(new String[] {logFile}, 2000, burnInPercentage);
			marginalLs[i] = analyser.getMean("likelihood");
			marginalLs2[i] = analyser.getTrace("likelihood");
			System.err.println("marginalLs[" + i + " ] = " + marginalLs[i]);

			logdata1.add(marginalLs[i]);
			logdata1.add(0.0);
			logdata1.add(analyser.getESS("likelihood"));
			logdata.add(logdata1);
		}
		
		// combine steps
		double logMarginalL = 0;
		if (alpha <= 0) { 
			// uniform intervals
			for (int i = 0; i < nSteps - 1; i++) {
				logMarginalL += (marginalLs[i] + marginalLs[i + 1]); 
			}		
			logMarginalL = logMarginalL / (2.0 * (nSteps - 1));
		} else {
			// intervals follow Beta distribution
			BetaDistribution betaDistribution = new BetaDistributionImpl(alpha, 1.0);
			double [] contrib = new double[nSteps-1];
			
			for (int i = 0; i < nSteps - 1; i++) {
				List<Double> logdata1 = logdata.get(i);;
				double beta1 = betaDistribution.inverseCumulativeProbability((nSteps - 1.0 - i)/ (nSteps - 1));
				double beta2 = betaDistribution.inverseCumulativeProbability((nSteps - 1.0 - (i + 1.0))/ (nSteps - 1));
				double weight = beta2 - beta1;

				// Use formula top right at page 153 of 
				// Xie W, Lewis PO, Fan Y, Kuo L, Chen MH. 2011. Improving marginal
				// likelihood estimation for Bayesian phylogenetic model selection.
				// Syst Biol. 60:150–160.
				Double [] marginal2 = marginalLs2[i];
				double logLmax = max(marginal2);
				logMarginalL += weight * logLmax;
				
				int n = marginal2.length;
				double x = 0;
				for (int j = 0; j < n; j++) {
					x += Math.exp(weight * (marginal2[j] - logLmax)); 
				}
				logMarginalL += Math.log(x/n);

				contrib[i] = weight * logLmax + Math.log(x/n);
				logdata1.set(1, weight * logLmax + Math.log(x/n));

//				logMarginalL += weight * marginalLs[i]; 
			}
						
		}
		
		System.out.println("\nStep        theta         likelihood   contribution ESS");
		BetaDistribution betaDistribution = new BetaDistributionImpl(alpha, 1.0);
		for (int i = 0; i < nSteps; i++) {
			System.out.print(format(i)+" ");
			double beta = betaDistribution != null ?
					betaDistribution.inverseCumulativeProbability((nSteps - 1.0 - i)/ (nSteps - 1)):
						(nSteps - 1.0 - i)/ (nSteps - 1);
			System.out.print(format(beta)+" ");

			
			for (Double d : logdata.get(i)) {
				System.out.print(format(d) + " ");
			}
			System.out.println();
		}		
		System.out.println();
		return logMarginalL;
	}

	private String format(double d) {
		DecimalFormat format = new DecimalFormat("###.####");
		String s = format.format(d);
		if (s.length() < 12) {
			s += "            ".substring(s.length());
		}
		return s;
	}

	private double max(Double[] marginal2) {
		Double max = marginal2[0];
		for (Double v : marginal2) {
			max = Math.max(v, max);
		}
		return max;
	}

	String getStepDir(String rootDir, int iParticle) {
		return rootDir + "/step" + formatter.format(iParticle);
	}
	
	public static void main(String[] args) throws Exception {
		PathSampleAnalyser analyser = new PathSampleAnalyser();
		int nSteps = Integer.parseInt(args[0]);
		double alpha = Double.parseDouble(args[1]);
		String rootDir = args[2];
		int burnInPercentage = Integer.parseInt(args[3]);
		double marginalL = analyser.estimateMarginalLikelihood(nSteps, alpha, rootDir, burnInPercentage);
		System.out.println("marginal L estimate =" + marginalL);
	}
	
}
