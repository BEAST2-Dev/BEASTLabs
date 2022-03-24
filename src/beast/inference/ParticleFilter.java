package beast.inference;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.StateNodeInitialiser;
import beast.base.core.BEASTObject;
import beast.base.core.Input.Validate;
import beast.pkgmgmt.BEASTClassLoader;
import beast.base.util.DiscreteStatistics;
import beast.base.util.Randomizer;
import beast.base.parser.XMLProducer;




@Description("MCMC Inference by particle filter approach. This works only when run with many threads, one per particle is optimal.")
public class ParticleFilter extends beast.base.inference.Runnable {

	String DEFAULT_PARTICLE_LAUNCER = ParticleLauncher.class.getName();
	public static String POSTERIOR_LOG_FILE = "posterior.log";

	
	public Input<Integer> m_nParticlesInput = new Input<Integer>("nrofparticles", "the number of particles to use, default 100", 100);
	public Input<Integer> m_nStepSizeInput = new Input<Integer>("stepsize", "number of steps after which a new particle set is determined during burn in, default 100", 100);
	public Input<String> m_sRootDir = new Input<String>("rootdir", "root directory for storing particle states and log files (default /tmp)", "/tmp");
	public Input<MCMC> m_mcmc = new Input<MCMC>("mcmc", "MCMC analysis used to specify model and operations in each of the particles", Validate.REQUIRED);
	public Input<String> m_sParticleLauncher = new Input<String>("launcher", "class name for particle launcher, default " + DEFAULT_PARTICLE_LAUNCER, DEFAULT_PARTICLE_LAUNCER);
	public Input<String> m_sScriptInput = new Input<String>("value", "script for launching a job. " +
			"$(dir) is replaced by the directory associated with the particle " +
			"$(seed) is replaced by a random number seed that differs with every launch " +
			"$(host) is replaced by a host from the list of hosts", Validate.REQUIRED);
	public Input<String> m_sHostsInput = new Input<String>("hosts", "comma separated list of hosts. " +
			"If there are k hosts in the list, for particle i the term $(host) in the script will be replaced " +
			"by the (i modulo k) host in the list. " +
			"Note that whitespace is removed");

	int m_nParticles;
	// nr of steps = MCMC.chainLength / step size
	long m_nSteps;
	String m_sScript;
	String [] m_sHosts;
	
	/** states and associated posteriors. Used to sample next state from **/
	String [] m_sStates;
	double [] m_fPosteriors;
	/** flag used to initialise states and posteriors properly **/
//	boolean m_bFirstParticle = true;

	CountDownLatch m_nCountDown;

	
	DecimalFormat formatter;
	String getParticleDir(int iParticle) {
		return m_sRootDir.get() + "/particle" + formatter.format(iParticle);
	}


	@Override
	public void initAndValidate() {
		m_nParticles = m_nParticlesInput.get();
		m_sScript = m_sScriptInput.get();
		int nStepSize = m_nStepSizeInput.get();
		if (m_sHostsInput.get() != null) {
			m_sHosts = m_sHostsInput.get().split(",");
			// remove whitespace
			for (int i = 0; i < m_sHosts.length; i++) {
				m_sHosts[i] = m_sHosts[i].replaceAll("\\s", "");
			}
		}
		
		File rootDir = new File(m_sRootDir.get());
		if (!rootDir.exists()) {
			throw new IllegalArgumentException("Directory " + m_sRootDir.get() + " does not exist.");
		}
		if (!rootDir.isDirectory()) {
			throw new IllegalArgumentException(m_sRootDir.get() + " is not a directory.");
		}
		
		// initialise MCMC
		MCMC mcmc = m_mcmc.get();
		// set up chain length for a single step
		mcmc.burnInInput.setValue(0, mcmc);
		m_nSteps = mcmc.chainLengthInput.get() / nStepSize;
		if (mcmc instanceof MCMCParticle) {
			((MCMCParticle)mcmc).m_stepSize.setValue(nStepSize, mcmc);
		} else {
			mcmc.chainLengthInput.setValue(nStepSize, mcmc);
		}
		
		// add posterior logger
		Logger logger = new Logger();
		logger.initByName("fileName", POSTERIOR_LOG_FILE, "log", mcmc.posteriorInput.get(), "logEvery", nStepSize);
		mcmc.loggersInput.setValue(logger, mcmc);

		// set up directories with beast.xml files in each of them
		String sFormat = "";
		for (int i = m_nParticles; i > 0; i /= 10) {
			sFormat += "#";
		}
		formatter = new DecimalFormat(sFormat);
		
		XMLProducer producer = new XMLProducer();
		String sXML = producer.toXML(m_mcmc.get());
		for (int i = 0; i < m_nParticles; i++) {
			File particleDir = new File(getParticleDir(i));
			if (!particleDir.exists() && !particleDir.mkdir()) {
				throw new IllegalArgumentException("Failed to make directory " + particleDir.getName());
			}
			particleDir.setWritable(true, false);
			try {
	        	FileOutputStream xmlFile;
				xmlFile = new FileOutputStream(particleDir.getAbsoluteFile() + "/beast.xml");
	        	PrintStream out = new PrintStream(xmlFile);
	            out.print(sXML);
				out.close();
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		m_sStates = new String[m_nParticles];
		m_fPosteriors = new double[m_nParticles];
		for (StateNodeInitialiser init : mcmc.initialisersInput.get()) {
			init.initStateNodes();
		}
		State state = mcmc.startStateInput.get();
		if (state == null) {
	        // State initialisation
	        HashSet<StateNode> operatorStateNodes = new HashSet<StateNode>();
	        for (Operator op : mcmc.operatorsInput.get()) {
	        	for (BEASTInterface o : op.listActiveBEASTObjects()) {
	        		if (o instanceof StateNode) {
	        			operatorStateNodes.add((StateNode) o);
	        		}
	        	}
	        }
            // create state from scratch by collecting StateNode inputs from Operators
            state = new State();
            for (StateNode stateNode : operatorStateNodes) {
            	state.stateNodeInput.setValue(stateNode, state);
            }
            state.m_storeEvery.setValue(mcmc.storeEveryInput.get(), state);
	        state.initialise();
	    }
		String sState = state.toXML(0);
		double fPosterior = mcmc.robustlyCalcPosterior(mcmc.posteriorInput.get());
		for (int i = 0; i < m_nParticles; i++) {
			m_sStates[i] = sState;
			m_fPosteriors[i] = fPosterior;
		}
		
		
		
	} // initAndValidate
	
	synchronized void updateStates(int step) throws Exception {
		for (int iParticle = 0; iParticle < m_nParticles; iParticle++) {
			// load state
			String sStateFileName = getParticleDir(iParticle) + "/beast.xml.state";
			if (new File(sStateFileName).exists()) {
				m_sStates[iParticle] = getTextFile(sStateFileName);
				
				// load posterior
				String sLog = getTextFile(getParticleDir(iParticle) + "/" + POSTERIOR_LOG_FILE);
				String [] sLogs = sLog.split("\n");
				String sPosterior = sLogs[sLogs.length-1].split("\t")[1];
				m_fPosteriors[iParticle] = Double.parseDouble(sPosterior);
			}
		}

		System.out.print("\n" + DiscreteStatistics.mean(m_fPosteriors) + " " + DiscreteStatistics.variance(m_fPosteriors));
		System.out.print(" " + Arrays.toString(m_fPosteriors));
		System.out.println();

		// sample new states
		double fMax = m_fPosteriors[0];
		int iMax = 0;
		for (int i = 0; i < m_nParticles; i++) {
			if (fMax < m_fPosteriors[i]) {
				fMax = m_fPosteriors[i];
				iMax = i;
			}
		}
		double [] fPosteriors = new double[m_nParticles];
		for (int i = 0; i < m_nParticles; i++) {
			fPosteriors[i] = Math.exp(m_fPosteriors[i] - fMax);
		}
		double fSum = 0;
		for (int i = 0; i < m_nParticles; i++) {
			fSum += fPosteriors[i];
		}
		
		double [] fNewPosteriors = new double[m_nParticles];
		String [] sNewStates = new String[m_nParticles];
		
		fNewPosteriors[0] = m_fPosteriors[0];
		sNewStates[0] = m_sStates[0];
		for (int iParticle = 0; iParticle < m_nParticles; iParticle++) {
			double fRand = Randomizer.nextDouble() * fSum;
			int iNewState = 0;
			while (fRand > fPosteriors[iNewState]) {
				fRand -= fPosteriors[iNewState];
				iNewState++;
			}
			fNewPosteriors[iParticle] = m_fPosteriors[iNewState];
			sNewStates[iParticle] = m_sStates[iNewState];
		}	

		
		final double DELTA = 0.0025;
		
		// slightly perturb weights of operators
		for (int iParticle = 0; iParticle < m_nParticles; iParticle++) {
			String [] sXML = sNewStates[iParticle].split("</itsabeastystatewerein>\n");
			String [] sStrs = sXML[1].split("\n");
			int nOperators = sStrs.length - 3; 
			double [] fWeights = new double[nOperators];
            for (int i = 0; i < nOperators; i++) {
            	String [] sStrs2 = sStrs[i+2].split(" ");
            	fWeights[i] = Double.parseDouble(sStrs2[1]);
            }
            // convert from cumulative weights
            for (int i = nOperators - 1; i > 0; i--) {
            	fWeights[i] -= fWeights[i - 1];
            }
            // delta exchange
            for (int i = 0; i < nOperators; i++) {
            	double fDelta = Randomizer.nextDouble() * DELTA;
            	int iFrom = Randomizer.nextInt(nOperators);
            	int iTo = Randomizer.nextInt(nOperators);
            	if (iFrom != iTo && fWeights[iFrom] > fDelta && fWeights[iTo] < 1.0 - fDelta) {
            		fWeights[iFrom] -= fDelta;
            		fWeights[iTo] += fDelta;
            	}
            }
            // convert to cumulative weights
            for (int i = 1; i < nOperators; i++) {
            	fWeights[i] += fWeights[i - 1];
            }
            fWeights[nOperators-1] = 1.0;
            
            String sStates = sXML[0] + "</itsabeastystatewerein>\n";
            sStates += "<!--\nID Weight Paramvalue #Accepted #Rejected #CorrectionAccepted #CorrectionRejected\n";
            for (int i = 0; i < nOperators; i++) {
            	String [] sStrs2 = sStrs[i+2].split(" ");
            	String sStr = sStrs2[0] + " " + fWeights[i] + " " + sStrs2[2] + " " + sStrs2[3] + " " + sStrs2[4] + " " + sStrs2[5] + " " + sStrs2[6] + "\n";
            	sStates += sStr;
            }
            sStates += "-->";
            sNewStates[iParticle] = sStates;
		}
		
		
		m_fPosteriors = fNewPosteriors;
		m_sStates = sNewStates;
		// write state files
		for (int iParticle = 0; iParticle < m_nParticles; iParticle++) {
			String sStateFileName = getParticleDir(iParticle) + "/beast.xml.state";
	    	FileOutputStream xmlFile = new FileOutputStream(sStateFileName);
	    	PrintStream out = new PrintStream(xmlFile);
	        out.print(m_sStates[iParticle]);
			out.close();
		}

		System.out.print((step+1) * m_nStepSizeInput.get() + " " + Arrays.toString(m_fPosteriors));
		System.out.println();
	} // updateStates
	
	synchronized void updateState(int iParticle) throws Exception {
		
		// load state
		String sStateFileName = getParticleDir(iParticle) + "/beast.xml.state";
		if (new File(sStateFileName).exists()) {
			m_sStates[iParticle] = getTextFile(sStateFileName);
			
			// load posterior
			String sLog = getTextFile(getParticleDir(iParticle) + "/" + POSTERIOR_LOG_FILE);
			String [] sLogs = sLog.split("\n");
			String sPosterior = sLogs[sLogs.length-1].split("\t")[1];
			m_fPosteriors[iParticle] = Double.parseDouble(sPosterior);
		}
//		if (m_bFirstParticle) {
//			// make sure the states and posteriors are initialised
//			for (int i = 0; i < m_nParticles; i++) {
//				m_sStates[i] = m_sStates[iParticle];
//				m_fPosteriors[i] = m_fPosteriors[iParticle];
//			}			
//			m_bFirstParticle = false;
//		}
		
		// sample new state with probability proportional to posterior
		double fMax = m_fPosteriors[0];
		int iMax = 0;
		for (int i = 0; i < m_nParticles; i++) {
			if (fMax < m_fPosteriors[i]) {
				fMax = m_fPosteriors[i];
				iMax = i;
			}
		}
		double [] fPosteriors = new double[m_nParticles];
		for (int i = 0; i < m_nParticles; i++) {
			fPosteriors[i] = Math.exp(m_fPosteriors[i] - fMax);
		}
		double fSum = 0;
		for (int i = 0; i < m_nParticles; i++) {
			fSum += fPosteriors[i];
		}
		double fRand = Randomizer.nextDouble() * fSum;
		int iNewState = 0;
		while (fRand > fPosteriors[iNewState]) {
			fRand -= fPosteriors[iNewState];
			iNewState++;
		}

		//iNewState = iMax;
		m_fPosteriors[iParticle] = m_fPosteriors[iNewState];
		m_sStates[iParticle] = m_sStates[iNewState];
		
		// write stat
    	FileOutputStream xmlFile = new FileOutputStream(sStateFileName);
    	PrintStream out = new PrintStream(xmlFile);
        out.print(m_sStates[iNewState]);
		out.close();
		File stateFile = new File(sStateFileName);
		stateFile.setWritable(true, false);


		// report some statistics
		System.out.print(iParticle + "=" + m_fPosteriors[iNewState] + " "); 
		if (iParticle == m_nParticles-1) {
			System.out.print(" " + DiscreteStatistics.mean(m_fPosteriors) + " " + DiscreteStatistics.variance(m_fPosteriors));
			System.out.print(" " + Arrays.toString(m_fPosteriors));
			System.out.println();
		}
	
	}

	private String getTextFile(String sFileName) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(sFileName));
		StringBuffer buf = new StringBuffer();
		while (fin.ready()) {
			String sStr = fin.readLine();
			buf.append(sStr);
			buf.append('\n');
		}
		fin.close();
		return buf.toString();
	}
	
	
    @Override
    public void run() throws Exception {
    	long startTime = System.currentTimeMillis();
		m_nCountDown = new CountDownLatch(m_nParticles);

		for (int i = 0; i < m_nParticles; i++) {
	    	Object o = BEASTClassLoader.forName(m_sParticleLauncher.get()).newInstance();
	    	if (!(o instanceof ParticleLauncher)) {
	    		throw new Exception(m_sParticleLauncher.get() + " is not a particle launcher.");
	    	}
	    	ParticleLauncher launcher = (ParticleLauncher) o;
	    	launcher.setParticle(i, this);
			launcher.start();
			// wait a second to ensure threads are not all launced at the same time, which can cause problems.
			Thread.sleep(1000);
		}
		
		m_nCountDown.await();
    	long endTime = System.currentTimeMillis();
    	System.out.println("\n\nTotal wall time: " + (endTime-startTime)/1000 + " seconds\nDone");
    } // run;	


	public boolean isResuming() {
		return restoreFromFile;
	}
	
}
