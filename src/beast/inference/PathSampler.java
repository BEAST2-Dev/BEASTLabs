package beast.inference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.math.distribution.BetaDistribution;
import org.apache.commons.math.distribution.BetaDistributionImpl;

import beast.app.BeastMCMC;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.util.CompoundDistribution;
import beast.core.Logger;
import beast.core.MCMC;
import beast.util.Randomizer;
import beast.util.XMLProducer;

@Description("Calculate marginal likelihood through path/stepping stone sampling. " +
		"Perform multiple steps and calculate estimate." +
		"Uses multiple threads if specified as command line option to BEAST.")
public class PathSampler extends beast.core.Runnable {
	public static String LIKELIHOOD_LOG_FILE = "likelihood.log";

	public Input<Integer> stepsInput = new Input<Integer>("nrofsteps", "the number of steps to use, default 8", 8);
	public Input<Double> alphaInput = new Input<Double>("alpha", "alpha parameter of Beta(alpha,1) distribution used to space out steps, default 0.3" +
			"If alpha <= 0, uniform intervals are used.", 0.3);
	public Input<String> rootDirInput = new Input<String>("rootdir", "root directory for storing particle states and log files (default /tmp)", "/tmp");
	public Input<MCMC> mcmcInput = new Input<MCMC>("mcmc", "MCMC analysis used to specify model and operations in each of the particles", Validate.REQUIRED);
	public Input<Integer> chainLengthInput = new Input<Integer>("chainLength", "number of sample to run a chain for a single step", 100000);
	public Input<Integer> burnInPercentageInput = new Input<Integer>("burnInPercentage", "burn-In Percentage used for analysing log files", 50);
	public Input<Integer> preBurnInInput = new Input<Integer>("preBurnin", "number of samples that are discarded for the first step, but not the others", 100000);
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
	public Input<Boolean> doNotRun = new Input<Boolean>("doNotRun", "Set up all files but do not run analysis if true. " +
			"This can be useful for setting up an analysis on a cluster", false);
	
	public Input<Boolean> deleteOldLogsInpuyt = new Input<Boolean>("deleteOldLogs", "delete existing log files from root dir", false);
	
	int m_nSteps;
	String [] m_sHosts;
	String m_sScript;
	int burnInPercentage;

	CountDownLatch m_nCountDown;

    final static String fileSep = System.getProperty("file.separator");

	DecimalFormat formatter;
	String getStepDir(int iParticle) {
		return rootDirInput.get() + "/step" + formatter.format(iParticle);
	}


	@Override
	public void initAndValidate() throws Exception {
		// grab info from inputs
		m_sScript = m_sScriptInput.get();
		if (m_sHostsInput.get() != null) {
			m_sHosts = m_sHostsInput.get().split(",");
			// remove whitespace
			for (int i = 0; i < m_sHosts.length; i++) {
				m_sHosts[i] = m_sHosts[i].replaceAll("\\s", "");
			}
		}
		
		m_nSteps = stepsInput.get();
		if (m_nSteps <= 1) {
			throw new Exception("number of steps should be at least 2");
		}
		burnInPercentage = burnInPercentageInput.get();
		if (burnInPercentage < 0 || burnInPercentage >= 100) {
			throw new Exception("burnInPercentage should be between 0 and 100");
		}
		int preBurnIn = preBurnInInput.get();
		
		// root directory sanity checks
		File rootDir = new File(rootDirInput.get());
		if (!rootDir.exists()) {
			throw new Exception("Directory " + rootDirInput.get() + " does not exist.");
		}
		if (!rootDir.isDirectory()) {
			throw new Exception(rootDirInput.get() + " is not a directory.");
		}
		
		// initialise MCMC
		MCMC mcmc = mcmcInput.get();

		if (!mcmc.getClass().equals(MCMC.class)) {
			System.out.println("WARNING: class is not beast.core.MCMC, which may result in unexpected behavior ");
		}
		PathSamplingStep step = new PathSamplingStep();
		for (Input<?> input : mcmc.listInputs()) {
			try {
				if (input.get() instanceof List) {
					for (Object o : (List<?>) input.get()) {
						step.setInputValue(input.getName(), o);
					}
				} else {
					step.setInputValue(input.getName(), input.get());
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		mcmc = step;
		
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
		BetaDistribution betaDistribution = null;
		if (alphaInput.get() > 0){
			betaDistribution = new BetaDistributionImpl(alphaInput.get(), 1.0);
		}
		
		
		PrintStream [] cmdFiles = new PrintStream[BeastMCMC.m_nThreads];
    	for (int i = 0; i < BeastMCMC.m_nThreads; i++) {
    		FileOutputStream outStream = (beast.app.util.Utils.isWindows()?
    					new FileOutputStream(rootDirInput.get() + "/run" + i +".bat"):
    					new FileOutputStream(rootDirInput.get() + "/run" + i +".sh"));
    		 cmdFiles[i] = new PrintStream(outStream);
    	}

		
		
		for (int i = 0; i < m_nSteps; i++) {
			if (i < BeastMCMC.m_nThreads) {
				mcmc.m_oBurnIn.setValue(preBurnIn, mcmc);
			} else {
				mcmc.m_oBurnIn.setValue(0, mcmc);
			}
			// create XML for a single step
			double beta = betaDistribution != null ?
					betaDistribution.inverseCumulativeProbability((i + 0.0)/ (m_nSteps - 1)):
						(i + 0.0)/ (m_nSteps - 1);
			step.setInputValue("beta", beta);
			String sXML = producer.toXML(step);
			File stepDir = new File(getStepDir(i));
			if (!stepDir.exists() && !stepDir.mkdir()) {
				throw new Exception("Failed to make directory " + stepDir.getName());
			}
			stepDir.setWritable(true, false);
        	FileOutputStream xmlFile = new FileOutputStream(stepDir.getAbsoluteFile() + "/beast.xml");
        	PrintStream out = new PrintStream(xmlFile);
            out.print(sXML);
			out.close();
			
			String cmd = getCommand(stepDir.getAbsolutePath(), i);
        	FileOutputStream cmdFile = 
        			(beast.app.util.Utils.isWindows()?
        					new FileOutputStream(stepDir.getAbsoluteFile() + "/run.bat"):
        					new FileOutputStream(stepDir.getAbsoluteFile() + "/run.sh"));
        	PrintStream out2 = new PrintStream(cmdFile);
            out2.print(cmd);
			out2.close();

        	cmdFile = 
        			(beast.app.util.Utils.isWindows()?
        					new FileOutputStream(stepDir.getAbsoluteFile() + "/resume.bat"):
        					new FileOutputStream(stepDir.getAbsoluteFile() + "/resume.sh"));
        	cmd = cmd.replace("-overwrite", "-resume");
        	out2 = new PrintStream(cmdFile);
            out2.print(cmd);
			out2.close();
//TODO: probably more efficient to group cmdFiles in block of #steps/#threads
//instead of skipping #threads steps every time.
			if (i >= BeastMCMC.m_nThreads) {
				String copyCmd = (beast.app.util.Utils.isWindows()
						? "copy " + getStepDir(i - BeastMCMC.m_nThreads) + "\\beast.xml.state " + getStepDir(i)
						: "cp " + getStepDir(i - BeastMCMC.m_nThreads) + "/beast.xml.state " + getStepDir(i)
							);
				cmdFiles[i % BeastMCMC.m_nThreads].print(copyCmd);				
			}
			cmdFiles[i % BeastMCMC.m_nThreads].print(cmd);
			File script = new File(stepDir.getAbsoluteFile() + 
					(beast.app.util.Utils.isWindows()? "/run.bat": "/run.sh"));
			script.setExecutable(true);
		}
    	for (int k = 0; k < BeastMCMC.m_nThreads; k++) {
    		cmdFiles[k].close();
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
		sStepDir = sStepDir.replace("\\", "\\\\");
		String sCommand = m_sScript.replaceAll("\\$\\(dir\\)", sStepDir);
		//while (sCommand.matches("$(seed)")) {
			sCommand = sCommand.replaceAll("\\$\\(seed\\)", Math.abs(Randomizer.nextInt())+"");
		//}
		sCommand = sCommand.replaceAll("\\$\\(java.library.path\\)", sanitise(System.getProperty("java.library.path")));
		sCommand = sCommand.replaceAll("\\$\\(java.class.path\\)", sanitise(System.getProperty("java.class.path")));
		if (m_sHosts != null) {
			sCommand = sCommand.replaceAll("\\$\\(host\\)", m_sHosts[iStep % m_sHosts.length]);
		}
		if (iStep < BeastMCMC.m_nThreads) {
			sCommand = sCommand.replaceAll("\\$\\(resume/overwrite\\)", "-overwrite");
		} else {
			sCommand = sCommand.replaceAll("\\$\\(resume/overwrite\\)", "-resume");
		}		
		return sCommand;
	}

	
	private String sanitise(String property) {
		// sanitise for windows
		if (beast.app.util.Utils.isWindows()) {
			String cwd = System.getProperty("user.dir");
			cwd = cwd.replace("\\", "/");
			property = property.replaceAll(";\\.", ";" +  cwd + ".");
			property = property.replace("\\", "/");
		}
		return property;
	}


	class StepThread extends Thread {
		int stepNr;
		
		StepThread(int stepNr) {
			this.stepNr = stepNr;
		}
		
		@Override
		public void run() {
			try {
				System.err.println("Starting step " + stepNr);
				File stepDir = new File(getStepDir(stepNr));
				if (!stepDir.exists()) {
					throw new Exception("Failed to find directory " + stepDir.getName());
				}
	        	String cmd = 
        			(beast.app.util.Utils.isWindows()?
        					stepDir.getAbsoluteFile() + "/run.bat":
        					stepDir.getAbsoluteFile() + "/run.sh");
	        	
				ProcessBuilder pb = new ProcessBuilder(cmd);
				pb.redirectErrorStream(true); // merge stdout and stderr
				Process p = pb.start();
//	        	
//				Process p = Runtime.getRuntime().exec(cmd);
				BufferedReader pout = new BufferedReader((new InputStreamReader(p.getInputStream())));
				String line;
				while ((line = pout.readLine()) != null) {
					//System.out.println(line);
				}
				pout.close();
				
				p.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.err.println("Finished step " + stepNr);
			m_nCountDown.countDown();
		}
	}
	
    @Override
    public void run() throws Exception {
    	if (doNotRun.get()) {
    		System.out.println("batch files can be found in " + rootDirInput.get());
    		System.out.println("Run these and then run"); 
    		System.out.println("java beast.inference.PathSampleAnalyser " + m_nSteps + " " + alphaInput.get() + " " + rootDirInput.get() + " " + burnInPercentage);
    		return;
    	}
    	long startTime = System.currentTimeMillis();

		for (int i = 0; i < m_nSteps; i++) {
	    	if (BeastMCMC.m_nThreads > 1) {
	    		int nSteps = Math.min(BeastMCMC.m_nThreads, m_nSteps - i);
	    		m_nCountDown = new CountDownLatch(nSteps);
	    		for (int j = 0; j < nSteps; j++) {
	    			new StepThread(i).start();
	    			i++;
	    		}
	    		m_nCountDown.await();	    		
    			for (int j = 0; j < BeastMCMC.m_nThreads && i+j < m_nSteps; j++) {
    				copyStateFile(i-1, i + j);	    			
    				checkLogFiles(i + j);
    			}
	    		i--;
	    	} else {
				File stepDir = new File(getStepDir(i));
				if (!stepDir.exists()) {
					throw new Exception("Failed to find directory " + stepDir.getName());
				}
	        	String cmd = 
	        			(beast.app.util.Utils.isWindows()?
	        					stepDir.getAbsoluteFile() + "\\run.bat":
	        					stepDir.getAbsoluteFile() + "/run.sh");
	        	
	        	
//				try {
	    		if (i > 0) {
	    			copyStateFile(i-1, i);
	    		}
				checkLogFiles(i);
					
					System.out.println(cmd);
					
					ProcessBuilder pb = new ProcessBuilder(cmd);
					pb.redirectErrorStream(true); // merge stdout and stderr
					Process p = pb.start();
					
					BufferedReader pout = new BufferedReader((new InputStreamReader(p.getInputStream())));
					String line;
					while ((line = pout.readLine()) != null) {
						System.out.println(line);
					}
					pout.close();
					p.waitFor();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
	    	}
		}
    	long endTime = System.currentTimeMillis();
    	
    	PathSampleAnalyser analyser = new PathSampleAnalyser();
    	double marginalL = analyser.estimateMarginalLikelihood(m_nSteps, alphaInput.get(), rootDirInput.get(), burnInPercentage);
		System.out.println("marginal L estimate =" + marginalL);

		System.out.println("\n\nTotal wall time: " + (endTime-startTime)/1000 + " seconds\nDone");
    } // run;	


	/** check for log files in directory for step i **/
	private void checkLogFiles(int i) throws Exception {
		File stepDir = new File(getStepDir(i));
		
		// remove any existing likglihood.log file
		File logFile = new File(stepDir.getPath() + fileSep + "likelihood.log");
		if (logFile.exists()) {
			if (deleteOldLogsInpuyt.get()) {
				System.err.println("WARNING: deleting file " + logFile.getPath());
				logFile.delete();
			} else {
				throw new Exception("Found old log file " + logFile.getPath() + " and will not overwrite (unless deleteOldLogs flag is set to true)");
			}
		}
		
		// process other log and tree files
		for (File file : stepDir.listFiles()) {
			if (file.getPath().endsWith(".log") || 
					file.getPath().endsWith(".trees")) {
				if (deleteOldLogsInpuyt.get()) {
				System.err.println("WARNING: deleting file " + file.getPath());
					file.delete();
				} else {
					throw new Exception("Found old log file " + file.getPath() + " and will not overwrite (unless deleteOldLogs flag is set to true)");
				}
			}
		}
	}

	/** copy beast.xml.state file from previous directory **/
	private void copyStateFile(int iFrom, int iTo) throws Exception {
		File prevStepDir = new File(getStepDir(iFrom));
		File stepDir = new File(getStepDir(iTo));
        InputStream in = new FileInputStream(new File(prevStepDir.getPath() + fileSep + "beast.xml.state"));
        OutputStream out = new FileOutputStream(new File(stepDir.getPath() + fileSep + "beast.xml.state"));
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
	}
	
} // PathSampler
