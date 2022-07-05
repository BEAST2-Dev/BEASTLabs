package beastlabs.inference;


import beastfx.app.treeannotator.TreeAnnotator;
import beastfx.app.treeannotator.TreeAnnotator.MemoryFriendlyTreeSet;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.StateNode;
import beast.base.inference.util.ESS;
import beast.base.core.Log;
import beast.base.evolution.tree.CladeSet;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;
import beastlabs.core.*;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLProducer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;



@Description("Coupled MCMC. Two independent MCMC chains which will only stop after their clade posteriors have converged" +
		"" +
		"Note that log file names should have $(seed) in their name so " +
		"that the first chain uses the actual seed in the file name and all subsequent chains add one to it." +
		"Furthermore, the log and tree log should have the same sample frequency.")
public class IndependentMCMC extends MCMC {
	
	
	public final Input<Integer> checkForConvergenceEveryInput = new Input<Integer>("checkEvery", "number of samples in between checking for convergence", 100000);
	public final Input<Double> thresholdInput = new Input<Double>("cladeprob", "maximum difference in clade probability required to declare convergence", 0.15);
	public final Input<Integer> convergenceLengthInput = new Input<Integer>("convergedFor", "maximum difference in clade probability must be less than threshold "
			+ "										this many checks in a row before convergence is declared and the chains are stopped", 3);
	public final Input<String> tempDirInput = new Input<>("tempDir","directory where temporary files are written", "/tmp/");
	
	public final Input<List<TreeStoreLogger>> treeStorersInput = new Input<>("treeStorer", "list of tree loggers to check convergence with", new ArrayList<TreeStoreLogger>());
	
	public final Input<Logger> rHatLoggerInput = new Input<>("rhatLogger", "The logger which will be used for computing Rhat (optional)", Input.Validate.OPTIONAL);
	public final Input<Double> maxRHatInput = new Input<>("rhat", "The maximum acceptable value of Rhat (across all parameters in the log file) required to declare convergence (set to 0 to skip this step)", 1.05);
	
	public final Input<Double> minESSInput = new Input<>("ESS", "The minimum ESS of all values in either chain required to declare convergence (set to 0 to skip this step)", 200.0);
	
	
	// Max Rhat
	boolean computeRhat;
	double maxRHat;
	String rHatLogFileID;
	
	// How often to check for convergence
	int checkForConvergenceEvery;
	
	// Maximum difference in clade probability must be less than threshold to converge
	double threshold;
	
	// If the maximum difference in clade probability must be less than threshold this many checks 
	// in a row, then the chain is ended
	int convergenceLength;
	
	// How often to save the current tree
	int storeTreeEvery;
	
	// Threads
	Thread [] threads;
	
	// Keep track of time taken between logs to estimate speed
    long startLogTime;
    
    // MCMC chains
    ConvergableMCMC[] chains;
    
    // Trees
    List<TreeStoreLogger> treeStorers;
    int numTreesTotal;
	int numTrees;
	List<LinkedHashMap<String, Integer>> cladeMapList1 = new ArrayList<LinkedHashMap<String, Integer>>();
	List<LinkedHashMap<String, Integer>> cladeMapList2 = new ArrayList<LinkedHashMap<String, Integer>>();
	List<LinkedHashMap<String, Boolean>> allClades = new ArrayList<LinkedHashMap<String, Boolean>>();
	
	// Keep track of when threads finish in order to optimise thread usage
	long [] finishTimes;
	List<StateNode> tmpStateNodes;
	
	// Tables of logs, one for each thread + one for the total
	List<Double[]>[] m_logTables;
	int[] numberOfLogLinesRead;
	
	// Pre-calculated sum of items and sum of items squared for all threads and all items 
	double [][] m_fSums;
	double [][] m_fSquaredSums;
	
	// ESS
	ESS ESSutil = new ESS();
	double minESS;
	
	
	public IndependentMCMC() {
		
		// Do not allow the user to specify chain length
		chainLengthInput.setRule(Validate.FORBIDDEN);
	}
	
	

	@Override
	public void initAndValidate() {
		
		
		if (this.getID() == null) throw new RuntimeException("Please provide an ID for IndependentMCMC");
		
		
		// Parse user settings
		checkForConvergenceEvery = checkForConvergenceEveryInput.get();
		if (checkForConvergenceEvery < 1) throw new RuntimeException("checkEvery must be at least 1");
		threshold = thresholdInput.get();
		convergenceLength = convergenceLengthInput.get();
		if (convergenceLength < 0) throw new RuntimeException("convergedFor must be at least 1");
		
		
		numTrees = 0;
		numTreesTotal = 0;
		
		// Create the MCMC objects
		chains = new ConvergableMCMC[2];

		
		// Tree storers
		treeStorers = treeStorersInput.get();
		if (treeStorers.size() == 0) Log.warning("WARNING: no tree storers have been specifed. Provide one or more"
				+ "tree storers so that clade posterior convergence can be detected.");
		
		for (TreeStoreLogger logger : treeStorers) {
			if (logger.everyInput.get() > checkForConvergenceEvery) throw new RuntimeException("checkEvery must be at least the logEvery of all tree storers");
		}
		
		
		cladeMapList1 = new ArrayList<LinkedHashMap<String, Integer>>();
		cladeMapList2 = new ArrayList<LinkedHashMap<String, Integer>>();
		allClades = new ArrayList<LinkedHashMap<String, Boolean>>();
		for (int c = 0; c < treeStorers.size(); c++) {
			cladeMapList1.add(new LinkedHashMap<String, Integer>());
			cladeMapList2.add(new LinkedHashMap<String, Integer>());
			allClades.add(new LinkedHashMap<String, Boolean>());
		}
		
		
		
		
		// The difference between the various chains is
		// 1. it runs an MCMC, not a  CoupledMCMC
		// 2. remove chains attribute
		// 3. output logs change for every chain
		// 4. log to stdout is removed to prevent clutter on stdout
		String sXML = new XMLProducer().toXML(this);
		

		
        String sMCMCMC = this.getClass().getName();
		while (sMCMCMC.length() > 0) {
			sXML = sXML.replaceAll("\\b"+IndependentMCMC.class.getName()+"\\b", ConvergableMCMC.class.getName());
			if (sMCMCMC.indexOf('.') >= 0) {
				sMCMCMC = sMCMCMC.substring(sMCMCMC.indexOf('.')+1);
			} else {
				sMCMCMC = "";
			}
		}
		
		
		
		long nSeed = Randomizer.getSeed();
		
		// Create new chains		
		for (int i = 0; i < chains.length; i++) {
			XMLParser parser = new XMLParser();
			String sXML2 = sXML;
			sXML2 = sXML2.replaceAll("\\$\\(seed\\)", nSeed+i+"");
			sXML2 = sXML2.replaceAll("\\$\\(chain\\)", "chain" + (i+1));
			
			
			// Add the tree storage loggers
			for (Logger logger : treeStorers) {
				String id = logger.getID();
				if (id == null) throw new RuntimeException("Please provide an ID for every treeStorer");
				sXML2 = sXML2.replaceAll("id=['\"]" + id + "['\"]", "id=\"" + id + "\" name=\"logger\"");
			}

			
			
			
			try {
		        FileWriter outfile = new FileWriter(new File(tempDirInput.get() + IndependentMCMC.class.getName() + ".xml"));
		        outfile.write(sXML2);
		        outfile.close();
		        
		        
				
				chains[i] = (ConvergableMCMC) parser.parseFragment(sXML2, true);

	
				// Only allow chain 1 to screen log
				chains[i].allowScreenLogging(i == 0);
				
				// Initialise the MCMC
				chains[i].setChainNr(i+1);
				chains[i].setStateFile(stateFileName + "."  + (i+1), restoreFromFile);
				chains[i].run();
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// get a copy of the list of state nodes to facilitate swapping states
		tmpStateNodes = startStateInput.get().stateNodeInput.get();

		
		finishTimes = new long[chains.length];
		
		
		computeRhat = rHatLoggerInput.get() != null;
		if (computeRhat) {
			minESS = minESSInput.get();
			maxRHat = maxRHatInput.get();
			rHatLogFileID = rHatLoggerInput.get().getID();
			if (rHatLogFileID == null) throw new RuntimeException("Please provide an ID for the rhatLogger");
			
			// Log files
			m_logTables = new List[chains.length + 1];
			for (int i = 0; i < chains.length+1; i++) {
				m_logTables[i] = new ArrayList<Double[]>();
			}
			numberOfLogLinesRead = new int[2];
			
		}
		
		
		
	} // initAndValidate
	
	
	
	class CoupledChainThread extends Thread {
		final int chainNr;
		CoupledChainThread(int chainNr) {
			this.chainNr = chainNr;
		}
		public void run() {
			try {
				finishTimes[chainNr] = chains[chainNr].runForNSteps(checkForConvergenceEvery);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		
	}
	
	@Override 
	public void run() throws IOException {
		
		int numberOfConvergedChecks = 0;
		
		long sampleNr = 0;
		long startTime = System.currentTimeMillis();
		while (numberOfConvergedChecks < convergenceLength) {
			
			// Start threads with individual chains here.
			threads = new Thread[chains.length];
			
			for (int k = 0; k < chains.length; k++) {
				threads[k] = new CoupledChainThread(k);
				threads[k].start();
			}
			
			sampleNr += checkForConvergenceEvery;

			// Wait for the chains to finish
	        startLogTime = System.currentTimeMillis();
			for (Thread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			
			// Check for convergence.
			
			// Maximum clade probability difference
			double maximumCladePosteriorDelta = getMaxCladeDifference();
			
			
			double maximumRGstat = 0;
			double minimumESS = Double.POSITIVE_INFINITY;
			
			if (computeRhat) {
				
				// Rubin-Gelman statistic (if required)
				maximumRGstat = maxRHat > 0 ? calcGRStats() : Double.NEGATIVE_INFINITY;
				
				// ESS
				minimumESS = minESS > 0 ? getMinESS() : Double.POSITIVE_INFINITY;
			}
			
			String toPrint = String.format("max clade difference = %.3f; Rhat = %.3f; ESS = %.1f", maximumCladePosteriorDelta, maximumRGstat, minimumESS);
			
			
			Log.info.println("\tIndependentMCMC (" + sampleNr + "): " + toPrint);
			//Log.warning("\t maxDeltaClade: " + maxDeltaClade + " numTreesTotal " + numTreesTotal + " numClades " + allClades.get(0).keySet().size());
			
			boolean thresholdMet = maximumCladePosteriorDelta <= threshold && maximumRGstat <= maxRHat && minimumESS >= minESS;
			if (thresholdMet) {
				numberOfConvergedChecks ++;
			}else {
				numberOfConvergedChecks = 0;
			}

		}
		
		Log.info.println("\tIndependentMCMC: convergence detected, MCMC stopping.");
		
		for (int k = 0; k < chains.length; k++) {
			chains[k].finish();
		}
		

       

        Log.info.println();
        final long endTime = System.currentTimeMillis();
        Log.info.println("Total calculation time: " + (endTime - startTime) / 1000.0 + " seconds");


		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// ignore
		}
	} // run
	
	
	
	
	
	// Returns the maximum clade probability difference between the two chains
	protected double getMaxCladeDifference() throws IOException {
		
		//if (true) return 0;
		double maximumCladePosteriorDelta = 0;
		for (int i = 0; i < treeStorers.size(); i ++) {
			
			// Get the ith tree file for either chain
			LinkedHashMap<String, Integer> cladeMap1 = cladeMapList1.get(i);
			LinkedHashMap<String, Integer> cladeMap2 = cladeMapList2.get(i);
			LinkedHashMap<String, Boolean> cladeList = allClades.get(i);
			
			
			// Chain 1
			TreeStoreLogger logger1 = chains[0].getTreeStoreLoggers(i);
			String fileName = logger1.getFileName();
			CladeSet cladeSet1 = getCladeSet(fileName);
			
			
			// Chain 2
			TreeStoreLogger logger2 = chains[1].getTreeStoreLoggers(i);
			fileName = logger2.getFileName();
			CladeSet cladeSet2 = getCladeSet(fileName);
			
			
			if (numTrees == 0) return 0;
			numTreesTotal += numTrees;
			

			// Update map of clades to support values in each set
			for (int c = 0; c < cladeSet1.getCladeCount(); c++) {
				String clade = cladeSet1.getClade(c);
				int support = cladeSet1.getFrequency(c);
				if (cladeMap1.containsKey(clade)) {
					cladeMap1.replace(clade, cladeMap1.get(clade) + support);
				}
				else cladeMap1.put(clade, support);
				
				if (!cladeList.containsKey(clade)) cladeList.put(clade, true);
				
			}
			
			
			for (int c = 0; c < cladeSet2.getCladeCount(); c++) {
				String clade = cladeSet2.getClade(c);
				int support = cladeSet2.getFrequency(c);
				if (cladeMap2.containsKey(clade)) {
					cladeMap2.replace(clade, cladeMap2.get(clade) + support);
				}
				else cladeMap2.put(clade, support);
				
				if (!cladeList.containsKey(clade)) cladeList.put(clade, true);
			}
			
			
			
			
			// Find maximum difference
			//System.out.println("numTreesTotal: " + numTreesTotal);
			for (String clade : cladeList.keySet()) {
				
				double delta = 0;
				
				// Clade only in chain 2
				if (!cladeMap1.containsKey(clade)) {
					double support = 1.0 * cladeMap2.get(clade)/numTreesTotal;
					delta = support;
					//System.out.println("2: " + support);
				}
				
				// Clade only in chain 1
				else if (!cladeMap2.containsKey(clade)) {
					double support = 1.0 * cladeMap1.get(clade)/numTreesTotal;
					delta = support;
					//System.out.println("1: " + support);
				}
				
				// Clade in both chains
				else {
					double support1 = 1.0 * cladeMap1.get(clade)/numTreesTotal;
					double support2 = 1.0 * cladeMap2.get(clade)/numTreesTotal;
					delta = Math.abs(support1 - support2);
					//System.out.println("Both: " + support1 + " - " + support2);
				}

				//System.out.println(clade + "|delta: " + delta);
				if (delta > maximumCladePosteriorDelta) {
					maximumCladePosteriorDelta = delta;
				}
			}
			
			
			// Reset the loggers 
			logger1.init();
			logger2.init();
			
		}
		
		
		return maximumCladePosteriorDelta;
		
		
		
	}
	
	
	
	
	protected CladeSet getCladeSet(String path) throws IOException {
		//Log.warning("Processing " + path);
		MemoryFriendlyTreeSet srcTreeSet = new TreeAnnotator().new MemoryFriendlyTreeSet(path, 0);
		srcTreeSet.reset();
		
		if (!srcTreeSet.hasNext()) return null;
		
		Tree tree = srcTreeSet.next();
		CladeSet cladeSet1 = new CladeSet(tree);
		numTrees = 1;
		while (srcTreeSet.hasNext()) {
			tree = srcTreeSet.next();
			cladeSet1.add(tree);
			numTrees++;
		}
		return cladeSet1;
	}
	
	
	
	
	
		
	
	//	http://hosho.ees.hokudai.ac.jp/~kubo/Rdoc/library/coda/html/gelman.diag.html
	//	Brooks, SP. and Gelman, A. (1997) 
	//	General methods for monitoring convergence of iterative simulations. 
	//	Journal of Computational and Graphical Statistics, 
	//	7, 
	//	434-455. 
	//
	//  m = # threads
	//	n = # samples
	//	B = variance within chain
	//	W = variance among chains
	//	R=(m+1/m)(W(n-1)/n + B/n + B/(mn))/W - (n-1)/nm 
	//	=>
	//	R=(m+1/m)((n-1)/n + B/Wn + B/(Wmn)) - (n-1)/nm
	//	=>
	//	R=(m+1/m)((n-1)/n + B/W(1/n + 1/mn) - (n-1)/nm
	//	=>
	//	R=(m+1/m)((n-1)/n + B/W((m+1)/nm)) - (n-1)/nm
	/** This calculates the Gelman Rubin statistic from scratch (using 10% burn in)
	 * and reports the log of the first chain, annotated with the R statistic.
	 * This number approaches 1 on convergence, so during the run of the chain
	 * you can check how well the chain converges.
	 *
	 * Exploit potential for efficiency by storing means and squared means
	 * NB: when the start of the chain changes, this needs to be taken in account.
	 */
	protected double calcGRStats() throws IOException {
		
		int nThreads = threads.length;
		
		// Read the log files
		for (int i = 0; i < nThreads; i ++) {
			
			// Get the logger file with the correct ID
			List<Logger> loggers = chains[i].loggersInput.get();
			Logger log = null;
			for (Logger l : loggers) {
				if (l.getID().contentEquals(rHatLogFileID)) {
					log = l;
					break;
				}
			}
			if(log == null) return -1;
			

			// Open the log file
			String logFileName = log.getFileName(); 
			BufferedReader br = new BufferedReader(new FileReader(logFileName));
			
			// Skip to the current line
			int lineNum = 0;
			while (lineNum < numberOfLogLinesRead[i]) {
				br.readLine();
				lineNum++;
			}
			
			
			String sStr = br.readLine();
			while (sStr != null) {
				
				
				// Skip comment lines
				//sStr = br.readLine();
				String [] sStrs = sStr.split("\\s+");
				while (sStr.startsWith(("#")) || sStrs.length <= 1) {
					sStr = br.readLine();
					if (sStr == null) break;
					numberOfLogLinesRead[i]++;
					sStrs = sStr.split("\\s+");
				}
				if (sStr == null) break;
				
				
				// Read in the new line
				int nItems = sStrs.length;
				Double[] vals = new Double[nItems];
				
				try {
					for (int j = 0; j < nItems; j++) {
						vals[j] = Double.parseDouble(sStrs[j]);
					}
					m_logTables[i].add(vals);
				} catch (Exception e) {
					//Ignore, probably a parse errors
				}
				
				// Next line
				sStr = br.readLine();
				numberOfLogLinesRead[i]++;
				
			}

			
		}
		
		
		// Get the GR statistic with 10% burnin
		int nCurrentSample = Math.min(m_logTables[0].size(), m_logTables[1].size()) - 1;
		int nLogItems = m_logTables[0].get(0).length;
		int nSamples = nCurrentSample - nCurrentSample/10;
		if (nSamples <= 5) return -1;
		

		// One for each chain plus one for combined chains
		if (m_fSums == null) {
			m_fSums = new double[(nThreads+1)][nLogItems];
			m_fSquaredSums = new double[(nThreads+1)][nLogItems];
		}
		
		
		// The Gelman Rubin statistic for each log item 
		double [] fR = new double [nLogItems];
		

		int nStartSample = nCurrentSample/10;
		int nOldStartSample = (nCurrentSample-1)/10;
		if (nStartSample != nOldStartSample) {
			// we need to remove log line from means
			// calc means and squared means
			int iSample = nOldStartSample;
			for (int iThread2 = 0; iThread2 < nThreads; iThread2++) {
				Double[] fLine = m_logTables[iThread2].get(iSample);
				for (int iItem = 1; iItem < nLogItems; iItem++) {
					m_fSums[iThread2][iItem] -= fLine[iItem];
					m_fSquaredSums[iThread2][iItem] -= fLine[iItem] * fLine[iItem];
				}
			}
			// sum to get totals
			for (int iItem = 1; iItem < nLogItems; iItem++) {
				double fMean = 0;
				for (int iThread2 = 0; iThread2 < nThreads; iThread2++) {
					fMean += m_logTables[iThread2].get(iSample)[iItem];
				}
				fMean /= nThreads;
				m_fSums[nThreads][iItem] -= fMean;
				m_fSquaredSums[nThreads][iItem] -= fMean * fMean;
			}
		}

		// calc means and squared means
		int iSample = nCurrentSample;
		for (int iThread2 = 0; iThread2 < nThreads; iThread2++) {
			Double[] fLine = m_logTables[iThread2].get(iSample);
			for (int iItem = 1; iItem < nLogItems; iItem++) {
				m_fSums[iThread2][iItem] += fLine[iItem];
				m_fSquaredSums[iThread2][iItem] += fLine[iItem] * fLine[iItem];
			}
		}
		// sum to get totals
		for (int iItem = 1; iItem < nLogItems; iItem++) {
			double fMean = 0;
			for (int iThread2 = 0; iThread2 < nThreads; iThread2++) {
				fMean += m_logTables[iThread2].get(iSample)[iItem];
			}
			fMean /= nThreads;
			m_fSums[nThreads][iItem] += fMean;
			m_fSquaredSums[nThreads][iItem] += fMean * fMean;
		}

		// calculate variances for all (including total counts)
		double [][] fVars = new double[(nThreads+1)][nLogItems];
		for (int iThread2 = 0; iThread2 < nThreads + 1; iThread2++) {
			for (int iItem = 1; iItem < nLogItems; iItem++) {
				double fMean = m_fSums[iThread2][iItem];
				double fMean2 = m_fSquaredSums[iThread2][iItem];
				fVars[iThread2][iItem] = (fMean2 - fMean * fMean);
			}
		}
		
		for (int iItem = 1; iItem < nLogItems; iItem++) {
			// average variance for this item
			double fW = 0;
			for (int i = 0 ; i < nThreads; i++ ){
				fW += fVars[i][iItem];
			}
			fW /= (nThreads*(nSamples -1));
			// variance for joint
			double fB = fVars[nThreads][iItem]/((nThreads-1) * nSamples);
			fR[iItem] = ((nThreads + 1.0)/nThreads) * ((nSamples-1.0) / nSamples + fB/fW * (nThreads+1)/(nSamples * nThreads)) - (nSamples-1.0)/(nSamples * nThreads); 
		}
		
		
		
		// Get the maximum Rhat
		double maximumGR = 0;
		for (int i = 0; i < fR.length; i ++) {
			if (!Double.isNaN(fR[i])) maximumGR = Math.max(maximumGR, fR[i]);
		}
	
		return maximumGR;
	}
	
	
	
	protected double getMinESS() {
		
		double minimumESS = Double.POSITIVE_INFINITY;
		int nThreads = threads.length;
		
		// 10% burnin
		int nCurrentSample = Math.min(m_logTables[0].size(), m_logTables[1].size()) - 1;
		int nLogItems = m_logTables[0].get(0).length;
		int nSamples = nCurrentSample - nCurrentSample/10;
		if (nSamples < 1) return 0;
		//double[] values = new double[nSamples];
		List<Double> values = new ArrayList<Double>();
		for (int k = 0; k < nSamples; k ++) {
			values.add(0.0);
		}
		
		// For each chain
		for (int i = 0; i < nThreads; i ++) {
			List<Double[]> loggedVals = m_logTables[i];

			// For each column (except column 1 which is sample number)
			for (int k = 1; k < nLogItems; k ++) {
			
				
			
				// For each row
				for (int j = 0; j < nSamples; j ++) {
					int sampleNum = j + nCurrentSample - nSamples;
					double val = loggedVals.get(sampleNum)[k];
					values.set(j, val);
				}
				
				
				double ESS = ESSutil.calcESS(values);
				if (!Double.isNaN(ESS)) minimumESS = Math.min(minimumESS, ESS);
				//System.out.println("ESS for " + k + " = " + ESS);
				
			}
			
			
		}
		
		return minimumESS;
		
	}
	
	
	

	
} // class MCMCMC



