package beast.inference;

import java.text.DecimalFormat;

import beast.core.Description;
import beast.core.Plugin;
import beast.util.LogAnalyser;

@Description("Reads logs produces through PathSamples and estimates marginal likelihood")
public class PathSampleAnalyser extends Plugin {
	
	
	DecimalFormat formatter;
	
	@Override
	public void initAndValidate() throws Exception {
	}
	
	double estimateMarginalLikelihood(int nSteps, double alpha, String rootDir, int burnInPercentage) throws Exception {
		String sFormat = "";
		for (int i = nSteps; i > 0; i /= 10) {
			sFormat += "#";
		}
		formatter = new DecimalFormat(sFormat);

		// collect likelihood estimates for each step
		double [] marginalLs = new double[nSteps];
		for (int i = 0; i < nSteps; i++) {
			String logFile = getStepDir(rootDir, i) + "/" + PathSampler.LIKELIHOOD_LOG_FILE;
			LogAnalyser analyser = new LogAnalyser(new String[] {logFile}, 2000, burnInPercentage);
			marginalLs[i] = analyser.getMean("likelihood");
			System.err.println("marginalLs[" + i + " ] = " + marginalLs[i]);
		}
		
		// combine steps
		double marginalL = 0;
		for (int i = 0; i < nSteps - 1; i++) {
			marginalL += marginalLs[i] + marginalLs[i + 1]; 
		}		
		marginalL = marginalL / (2.0 * (nSteps - 1));
		return marginalL;
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
