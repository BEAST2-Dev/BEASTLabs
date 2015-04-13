package beast.inference;


import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;
import beast.core.MCMC;
import beast.core.State;
import beast.core.StateNode;
import beast.util.Randomizer;
import beast.util.XMLParser;
import beast.util.XMLProducer;

// Altekar G, Dwarkadas S, Huelsenbeck J and Ronquist F (2004). 
// “Parallel Metropolis Coupled Markov Chain Monte Carlo For Bayesian Phylogenetic Inference.” 
// Bioinformatics, 20. ISSN 1367-4803, 
// http://dx.doi.org/10.1093/bioinformatics/btg427.

@Description("Metropolis-Coupled Markov Chain Monte Carlo" +
		"" +
		"Note that log file names should have $(seed) in their name so " +
		"that the first chain uses the actual seed in the file name and all subsequent chains add one to it." +
		"Furthermore, the log and tree log should have the same sample frequency.")
public class MCMCMC extends MCMC {
	public Input<Integer> m_nrOfChains = new Input<Integer>("chains", " number of chains to run in parallel (default 2)", 2);
	public Input<String> heatedMCMCClassInput = new Input<String>("heatedMCMCClass", "Name of the class used for heated chains", HeatedMCMC.class.getName());
	
	// nr of samples between re-arranging states
	int resampleEvery = 1000;
	
	
	/** plugins representing MCMC with model, loggers, etc **/
	HeatedMCMC [] m_chains;
	/** threads for running MCMC chains **/
	Thread [] m_threads;
	/** keep track of time taken between logs to estimate speed **/
    long m_nStartLogTime;


	/** index of log and tree log among the MCMC loggers**/
	int m_iTreeLog = 0;
	int m_iLog = 0;
	
	List<StateNode> tmpStateNodes;

	@Override
	public void initAndValidate() throws Exception {
		m_chains = new HeatedMCMC[m_nrOfChains.get()];

		// the difference between the various chains is
		// 1. it runs an MCMC, not a  MultiplMCMC
		// 2. remove chains attribute
		// 3. output logs change for every chain
		// 4. log to stdout is removed to prevent clutter on stdout
		String sXML = new XMLProducer().toXML(this);
		sXML = sXML.replaceAll("chains=[^ /]*", "");
		String sMCMCMC = this.getClass().getName();
		while (sMCMCMC.length() > 0) {
			sXML = sXML.replaceAll("\\b"+MCMCMC.class.getName()+"\\b", heatedMCMCClassInput.get());
			if (sMCMCMC.indexOf('.') >= 0) {
				sMCMCMC = sMCMCMC.substring(sMCMCMC.indexOf('.')+1);
			} else {
				sMCMCMC = "";
			}
		}
		long nSeed = Randomizer.getSeed();
		
		// create new chains
		XMLParser parser = new XMLParser();
		
		// get a copy of the list of state nodes to facilitate swapping states
		HeatedMCMC tmp = (HeatedMCMC) parser.parseFragment(sXML, true);
		tmpStateNodes = tmp.startStateInput.get().stateNodeInput.get();
		
		for (int i = 0; i < m_chains.length; i++) {
			String sXML2 = sXML;
			sXML2 = sXML2.replaceAll("\\$\\(seed\\)", nSeed+i+"");
			if (sXML2.equals(sXML)) {
				// Uh oh, no seed in log name => logs will overwrite
				throw new Exception("Use $(seed) in log file name to guarantee log files do not overwrite");
			}
			m_chains[i] = (HeatedMCMC) parser.parseFragment(sXML2, true);
			// remove log to stdout, if any
//			for (int iLogger = m_chains[i].loggersInput.get().size()-1; iLogger >= 0; iLogger--) {
//				if (m_chains[i].loggersInput.get().get(iLogger).fileNameInput.get() == null) {
//					m_chains[i].loggersInput.get().remove(iLogger);
//				}
//			}
			// remove all loggers, except for main cahin
			if (i != 0) {
				m_chains[i].loggersInput.get().clear();
			}
			m_chains[i].setChainNr(i, resampleEvery);
		}
	
		// collect indices for tree log file names
		while (m_chains[0].loggersInput.get().get(m_iTreeLog).mode != Logger.LOGMODE.tree) {
			m_iTreeLog++;
		}
		while (m_chains[0].loggersInput.get().get(m_iLog).mode != Logger.LOGMODE.compound) {
			m_iLog++;
		}
		int nEveryLog = m_chains[0].loggersInput.get().get(m_iLog).everyInput.get();
		int nEveryTree = m_chains[0].loggersInput.get().get(m_iTreeLog).everyInput.get();
		if (nEveryLog != nEveryTree) {
			throw new Exception("log frequencey and tree log frequencey should be the same.");
		}
	} // initAndValidate
	
	@Override 
	public void run() throws Exception {
		
		
		for (int sampleNr = 0; sampleNr < chainLength; sampleNr += resampleEvery) {
			// start threads with individual chains here.
			m_threads = new Thread[m_chains.length];
			int k = 0;
			for (final HeatedMCMC mcmc : m_chains) {
				mcmc.setStateFile(stateFileName + "." +k, restoreFromFile);
				m_threads[k] = new Thread() {
					public void run() {
						try {
							mcmc.runTillResample();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				m_threads[k].start();
				k++;
			}
	
			// wait for the chains to finish
	        m_nStartLogTime = System.currentTimeMillis();
			for (Thread thread : m_threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			// resample state
			int i = Randomizer.nextInt(m_chains.length);
			int j = i;
			while (i == j) {
				j = Randomizer.nextInt(m_chains.length);
			}
			
			double p1 = m_chains[i].getCurrentLogLikelihood();
			double p2 = m_chains[j].getCurrentLogLikelihood();
			double t1 = m_chains[i].getTemperature();
			double t2 = m_chains[j].getTemperature();
			swapStates(m_chains[i], m_chains[j]);
			double p1b = m_chains[i].robustlyCalcPosterior(m_chains[i].posteriorInput.get());
			double p2b = m_chains[j].robustlyCalcPosterior(m_chains[j].posteriorInput.get());
			
			if (p1/t1 + p1b/t2 - p2/t1 - p2b/t2 < Randomizer.nextDouble()) {
				// swap fails
				swapStates(m_chains[i], m_chains[j]);
			}
			m_chains[i].startStateInput.get().setEverythingDirty(true);
			m_chains[j].startStateInput.get().setEverythingDirty(true);
			
		}

		// wait 5 seconds for the log to complete
		Thread.sleep(5000);
	} // run
	
	/* swaps the states of mcmc1 and mcmc2 */
	void swapStates(MCMC mcmc1, MCMC mcmc2) {
		State state1 = mcmc1.startStateInput.get();
		State state2 = mcmc2.startStateInput.get();
		
		List<StateNode> stateNodes1 = state1.stateNodeInput.get();
		List<StateNode> stateNodes2 = state2.stateNodeInput.get();
		for (int i = 0; i < stateNodes1.size(); i++) {
			StateNode stateNode1 = stateNodes1.get(i);
			StateNode stateNode2 = stateNodes2.get(i);
			StateNode tmp = tmpStateNodes.get(i);
			tmp.assignFromWithoutID(stateNode1);
			stateNode1.assignFromWithoutID(stateNode2);
			stateNode2.assignFromWithoutID(tmp);
		}
	}
	
	
} // class MCMCMC



