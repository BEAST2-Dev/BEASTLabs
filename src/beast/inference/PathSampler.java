package beast.inference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.math.distribution.BetaDistribution;
import org.apache.commons.math.distribution.BetaDistributionImpl;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.util.CompoundDistribution;
import beast.core.Logger;
import beast.core.MCMC;
import beast.util.Randomizer;
import beast.util.XMLProducer;

@Description("Calculate marginal likelihood through path sampling. " +
		"Perform multiple steps and calculate estimate")
public class PathSampler extends beast.core.Runnable {
	public static String LIKELIHOOD_LOG_FILE = "likelihood.log";

	public Input<Integer> stepsInput = new Input<Integer>("nrofsteps", "the number of steps to use, default 8", 8);
	public Input<Double> alphaInput = new Input<Double>("alpha", "alpha parameter of Beta(alpha,1) distribution used to space out steps, default 0.3", 0.3);
	public Input<String> rootDirInput = new Input<String>("rootdir", "root directory for storing particle states and log files (default /tmp)", "/tmp");
	public Input<MCMC> mcmcInput = new Input<MCMC>("mcmc", "MCMC analysis used to specify model and operations in each of the particles", Validate.REQUIRED);
	public Input<Integer> chainLengthInput = new Input<Integer>("chainLength", "number of sample to run a chain for a single step", 100000);
	public Input<Integer> burnInPercentageInput = new Input<Integer>("burnInPercentage", "burn-In Percentage used for analysing log files", 50);
	public Input<String> m_sScriptInput = new Input<String>("value", "script for launching a job. " +
			"$(dir) is replaced by the directory associated with the particle " +
			"$(java.class.path) is replaced by a java class path used to launch this application " +
			"$(java.library.path) is replaced by a java library path used to launch this application " +
			"$(seed) is replaced by a random number seed that differs with every launch " +
			"$(host) is replaced by a host from the list of hosts", Validate.REQUIRED);
	public Input<String> m_sHostsInput = new Input<String>("hosts", "comma separated list of hosts. " +
			"If there are k hosts in the list, for particle i the term $(host) in the script will be replaced " +
			"by the (i modulo k) host in the list. " +
			"Note that whitespace is removed");
	
	int m_nSteps;
	String [] m_sHosts;
	String m_sScript;
	int burnInPercentage;

	//CountDownLatch m_nCountDown;

	
	DecimalFormat formatter;
	String getParticleDir(int iParticle) {
		return rootDirInput.get() + "/step" + formatter.format(iParticle);
	}


	@Override
	public void initAndValidate() throws Exception {
		m_sScript = m_sScriptInput.get();
		if (m_sHostsInput.get() != null) {
			m_sHosts = m_sHostsInput.get().split(",");
			// remove whitespace
			for (int i = 0; i < m_sHosts.length; i++) {
				m_sHosts[i] = m_sHosts[i].replaceAll("\\s", "");
			}
		}
		
		m_nSteps = stepsInput.get();
		burnInPercentage = burnInPercentageInput.get();
		if (burnInPercentage < 0 || burnInPercentage >= 100) {
			throw new Exception("burnInPercentage should be between 0 and 100");
		}
		
		File rootDir = new File(rootDirInput.get());
		if (!rootDir.exists()) {
			throw new Exception("Directory " + rootDirInput.get() + " does not exist.");
		}
		if (!rootDir.isDirectory()) {
			throw new Exception(rootDirInput.get() + " is not a directory.");
		}
		
		// initialise MCMC
		MCMC mcmc = mcmcInput.get();
		int chainLength = chainLengthInput.get();
		// set up chain length for a single step
		mcmc.m_oBurnIn.setValue(0, mcmc);
		mcmc.m_oChainLength.setValue(chainLength, mcmc);
		
		// add posterior logger
		Logger logger = new Logger();
		Distribution likelihood = extractLikelihood(mcmc); 
		logger.initByName("fileName", LIKELIHOOD_LOG_FILE, "log", likelihood, "logEvery", chainLength/1000);
		mcmc.m_loggers.setValue(logger, mcmc);

		// set up directories with beast.xml files in each of them
		String sFormat = "";
		for (int i = m_nSteps; i > 0; i /= 10) {
			sFormat += "#";
		}
		formatter = new DecimalFormat(sFormat);
		
		XMLProducer producer = new XMLProducer();
		String sXML = producer.toXML(mcmcInput.get());
		BetaDistribution betaDistribution = new BetaDistributionImpl(alphaInput.get(), 1.0);
		
		for (int i = 0; i < m_nSteps; i++) {
			double beta = betaDistribution.inverseCumulativeProbability((i + 0.0)/ m_nSteps);
			if (!sXML.contains("spec=\"MCMC\"")) {
				throw new Exception("Expected to find spec=\"MCMC\" in the XML");
			}
			String sXMLStep = sXML.replaceAll("spec=\"MCMC\"", "spec=\"beast.inference.PathSamplingStep\" beta='" + beta +"'");
			File particleDir = new File(getParticleDir(i));
			if (!particleDir.exists() && !particleDir.mkdir()) {
				throw new Exception("Failed to make directory " + particleDir.getName());
			}
			particleDir.setWritable(true, false);
        	FileOutputStream xmlFile = new FileOutputStream(particleDir.getAbsoluteFile() + "/beast.xml");
        	PrintStream out = new PrintStream(xmlFile);
            out.print(sXMLStep);
			out.close();
			
			String cmd = getCommand(particleDir.getAbsolutePath(), i);
        	FileOutputStream cmdFile = 
        			(beast.app.util.Utils.isWindows()?
        					new FileOutputStream(particleDir.getAbsoluteFile() + "/run.bat"):
        					new FileOutputStream(particleDir.getAbsoluteFile() + "/run.sh"));
        	PrintStream out2 = new PrintStream(cmdFile);
            out2.print(cmd);
			out2.close();
			File script = new File(particleDir.getAbsoluteFile() + 
					(beast.app.util.Utils.isWindows()? "/run.bat": "/run.sh"));
			script.setExecutable(true);
		}
		
	} // initAndValidate
	
	private Distribution extractLikelihood(MCMC mcmc) throws Exception {
		Distribution posterior = mcmc.posteriorInput.get();
		// expect compound distribution with likelihood and prior
		if (!(posterior instanceof CompoundDistribution)) {
			throw new Exception("Expected posterior being a CompoundDistribution");
		}
		CompoundDistribution d = (CompoundDistribution) posterior;
		List<Distribution> list = d.pDistributions.get();
		if (list.size() != 2) {
			throw new Exception("Expected posterior with only likelihood and prior as distributions");
		}
		if (list.get(0).getID().toLowerCase().startsWith("likelihood")) {
			return list.get(0);
		} else {
			if (list.get(1).getID().toLowerCase().startsWith("likelihood")) {
				return list.get(1);
			} else {
				throw new Exception("Expected posterior with only likelihood and prior as IDs");
			}
		}
	}

	String getCommand(String sStepDir, int iStep) {
		String sCommand = m_sScript.replaceAll("\\$\\(dir\\)", sStepDir);
		//while (sCommand.matches("$(seed)")) {
			sCommand = sCommand.replaceAll("\\$\\(seed\\)", Math.abs(Randomizer.nextInt())+"");
		//}
		sCommand = sCommand.replaceAll("\\$\\(java.library.path\\)", System.getProperty("java.library.path"));
		sCommand = sCommand.replaceAll("\\$\\(java.class.path\\)", System.getProperty("java.class.path"));
		if (m_sHosts != null) {
			sCommand = sCommand.replaceAll("\\$\\(host\\)", m_sHosts[iStep % m_sHosts.length]);
		}
		return sCommand;
	}

    @Override
    public void run() throws Exception {
    	long startTime = System.currentTimeMillis();
		//m_nCountDown = new CountDownLatch(m_nSteps);

		for (int i = 0; i < m_nSteps; i++) {
			File particleDir = new File(getParticleDir(i));
			if (!particleDir.exists()) {
				throw new Exception("Failed to find directory " + particleDir.getName());
			}
        	String cmd = 
        			(beast.app.util.Utils.isWindows()?
        					particleDir.getAbsoluteFile() + "/run.bat":
        					particleDir.getAbsoluteFile() + "/run.sh");
        	
        	
			try {
				System.out.println(cmd);
				Process p = Runtime.getRuntime().exec(cmd);
				BufferedReader pout = new BufferedReader((new InputStreamReader(p.getInputStream())));
				BufferedReader perr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = pout.readLine()) != null) {
					System.out.println(line);
				}
				pout.close();
				while ((line = perr.readLine()) != null) {
					System.err.println(line);
				}
				perr.close();
				p.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//m_nCountDown.await();
    	long endTime = System.currentTimeMillis();
    	
    	PathSampleAnalyser analyser = new PathSampleAnalyser();
    	double marginalL = analyser.estimateMarginalLikelihood(m_nSteps, alphaInput.get(), rootDirInput.get(), burnInPercentage);
		System.out.println("marginal L estimate =" + marginalL);
    	System.out.println("\n\nTotal wall time: " + (endTime-startTime)/1000 + " seconds\nDone");
    	
    	
    } // run;	
	
} // PathSampler
