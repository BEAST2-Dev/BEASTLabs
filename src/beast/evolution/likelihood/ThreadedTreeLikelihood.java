/*
* File TreeLikelihood.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/



package beast.evolution.likelihood;




import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import beast.app.BeastMCMC;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.AscertainedAlignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.branchratemodel.StrictClockModel;
import beast.evolution.likelihood.BeagleTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;


@Description("Calculates the likelihood of sequence data on a beast.tree given a site and substitution model using " +
		"a variant of the 'peeling algorithm'. For details, see" +
		"Felsenstein, Joseph (1981). Evolutionary trees from DNA sequences: a maximum likelihood approach. J Mol Evol 17 (6): 368-376.")
public class ThreadedTreeLikelihood extends Distribution {

    public Input<Alignment> m_data = new Input<Alignment>("data", "sequence data for the beast.tree", Validate.REQUIRED);
    public Input<Tree> m_tree = new Input<Tree>("tree", "phylogenetic beast.tree with sequence data in the leafs", Validate.REQUIRED);
    public Input<SiteModel.Base> m_pSiteModel = new Input<SiteModel.Base>("siteModel", "site model for leafs in the beast.tree", Validate.REQUIRED);
    public Input<BranchRateModel.Base> m_pBranchRateModel = new Input<BranchRateModel.Base>("branchRateModel",
            "A model describing the rates on the branches of the beast.tree.");
    public Input<Boolean> m_useAmbiguities = new Input<Boolean>("useAmbiguities", "flag to indicate leafs that sites containing ambigue states should be handled instead of ignored (the default)", false);
    
    public Input<Integer> maxNrOfThreads = new Input<Integer>("threads","maximum number of threads to use, if less than 1 the number of threads in BeastMCMC is used (default -1)", -1);
    public Input<Boolean> useJava = new Input<Boolean>("useJava", "prefer java, even if beagle is available", true);

    public Input<String> proportionsInput = new Input<String>("proportions", "specifies proportions of patterns used per thread as space "
    		+ "delimted string. This is useful when using a mixture of BEAGLE devices that run at different speeds, e.g GPU and CPU. "
    		+ "The string is duplicated if there are more threads than proportions specified. For example, "
    		+ "'1 2' as well as '33 66' with 2 threads specifies that the first thread gets hald the patterns and the second "
    		+ "two thirds. With 3 threads, it is interpreted as '1 2 1' = 25%, 50%, 25% and with 7 threads it is "
    		+ "'1 2 1 2 1 2 1' = 10% 20% 10% 20% 10% 20% 10%. If not specified, all threads get the same proprtion of patterns.");
    
    enum Scaling {none, always, _default};
    public Input<Scaling> scaling = new Input<ThreadedTreeLikelihood.Scaling>("scaling", "type of scaling to use, one of " + Arrays.toString(Scaling.values()) + ". If not specified, the -beagle_scaling flag is used.", Scaling._default, Scaling.values());
    
    
    /** calculation engine **/
    protected ThreadedLikelihoodCore m_likelihoodCore;
    BeagleTreeLikelihood [] m_beagle;
    
    /** Plugin associated with inputs. Since none of the inputs are StateNodes, it
     * is safe to link to them only once, during initAndValidate.
     */
    SubstitutionModel.Base m_substitutionModel;
    protected SiteModel.Base m_siteModel;
    BranchRateModel.Base m_branchRateModel;

    /** flag to indicate the 
    // when CLEAN=0, nothing needs to be recalculated for the node
    // when DIRTY=1 indicates a node partial needs to be recalculated
    // when FILTHY=2 indicates the indices for the node need to be recalculated
    // (often not necessary while node partial recalculation is required)
     */
    int m_nHasDirt;

    /** Lengths of the branches in the tree associated with each of the nodes
     * in the tree through their node  numbers. By comparing whether the 
     * current branch length differs from stored branch lengths, it is tested
     * whether a node is dirty and needs to be recomputed (there may be other
     * reasons as well...).
     * These lengths take branch rate models in account.
     */
    double [] m_branchLengths;
    double [] m_StoredBranchLengths;
    
    /** memory allocation for likelihoods for each of the patterns **/
    double[] m_fPatternLogLikelihoods;
    /** memory allocation for probability tables obtained from the SiteModel **/
    double[] m_fProbabilities;

    int m_nMatrixSize;
    
    /** flag to indicate ascertainment correction should be applied **/
    boolean m_bAscertainedSitePatterns = false;

    /** dealing with proportion of site being invariant **/
    double m_fProportionInvariant = 0;
    List<Integer> m_iConstantPattern = null;
    
    /** number of threads to use, changes when threading causes problems **/
	int m_nThreads;
	double [] logPByThread;
	

	int [] cacheNode1;
	int [] cacheNode2;
	int [] cacheNode3;
	int cacheNodeCount;
	
	// specified a set ranges of patterns assigned to each thread
	// first patternPoints contains 0, then one point for each thread
	int [] patternPoints;
	
    @Override
    public void initAndValidate() throws Exception {
		m_nThreads = BeastMCMC.m_nThreads;
		if (maxNrOfThreads.get() > 0) {
			m_nThreads = Math.min(maxNrOfThreads.get(), BeastMCMC.m_nThreads);
		}
		logPByThread = new double[m_nThreads];

    	// sanity check: alignment should have same #taxa as tree
    	if (m_data.get().getNrTaxa() != m_tree.get().getLeafNodeCount()) {
    		throw new Exception("The number of nodes in the tree does not match the number of sequences");
    	}
    	
    	m_beagle = new BeagleTreeLikelihood[m_nThreads];
    	String sJavaOnly = null;
    	if (useJava.get()) {
    		sJavaOnly = System.getProperty("java.only");
    		System.setProperty("java.only", "" + true);
    	}
    	if (m_nThreads == 1) {
    		m_beagle[0] = new BeagleTreeLikelihood();
    		m_beagle[0].initByName("data", m_data.get(), "tree", m_tree.get(), "siteModel", m_pSiteModel.get(), "branchRateModel", m_pBranchRateModel.get(), "useAmbiguities", m_useAmbiguities.get());
    	} else {
        	for (int i = 0; i < m_nThreads; i++) {
        		m_beagle[i] = new BeagleTreeLikelihood();
        		FilteredAlignment filter = new FilteredAlignment();
        		filter.initByName("data", m_data.get()/*, "userDataType", m_data.get().getDataType()*/, "filter", (i+1)+"::"+m_nThreads);
        		m_beagle[i].initByName("data", filter, "tree", m_tree.get(), "siteModel", m_pSiteModel.get(), "branchRateModel", m_pBranchRateModel.get(), "useAmbiguities", m_useAmbiguities.get());
        	}
    	}
    	if (useJava.get()) {
	    	if (sJavaOnly != null) {
	    		System.setProperty("java.only", sJavaOnly);
	    	} else {
	    		System.clearProperty("java.only");
	    	}
    	}

    	if (m_beagle[0].beagle != null) {
    		//a Beagle instance was found, so we use it
    		return;
    	}
//		// No Beagle instance was found, so we use the good old java likelihood core
    	m_beagle = null;
    	
        int nodeCount = m_tree.get().getNodeCount();
        m_siteModel = m_pSiteModel.get();
        m_siteModel.setDataType(m_data.get().getDataType());
        m_substitutionModel = m_siteModel.substModelInput.get();

        if (m_pBranchRateModel.get() != null) {
        	m_branchRateModel = m_pBranchRateModel.get();
        } else {
            m_branchRateModel = new StrictClockModel();
        }
    	m_branchLengths = new double[nodeCount];
    	m_StoredBranchLengths = new double[nodeCount];
    	
        int nStateCount = m_data.get().getMaxStateCount();
        int nPatterns = m_data.get().getPatternCount();
        if (nStateCount == 4) {
        	m_likelihoodCore = new ThreadedBeerLikelihoodCore4();
        } else {
        	m_likelihoodCore = new ThreadedBeerLikelihoodCore(nStateCount);
        }
    	//m_likelihoodCore = new ThreadedLikelihoodCoreNative(nStateCount);
        System.err.println("TreeLikelihood uses " + m_likelihoodCore.getClass().getName());

        m_fProportionInvariant = m_siteModel.getProportionInvariant();
        m_siteModel.setPropInvariantIsCategory(false);
        if (m_fProportionInvariant > 0) {
        	calcConstantPatternIndices(nPatterns, nStateCount);
        }
        
        initCore();
        
        m_fPatternLogLikelihoods = new double[nPatterns];
        m_nMatrixSize = (nStateCount +1)* (nStateCount+1);
        m_fProbabilities = new double[(nStateCount +1)* (nStateCount+1)];
        Arrays.fill(m_fProbabilities, 1.0);

        if (m_data.get() instanceof AscertainedAlignment) {
            m_bAscertainedSitePatterns = true;
        }
        
    	cacheNode1 = new int[nodeCount];
    	cacheNode2 = new int[nodeCount];
    	cacheNode3 = new int[nodeCount];

		patternPoints = new int[m_nThreads + 1];
		if (proportionsInput.get() == null) {
			int range = nPatterns / m_nThreads;
			for (int i = 0; i < m_nThreads - 1; i++) {
				patternPoints[i+1] = range * (i+1);
			}
			patternPoints[m_nThreads] = nPatterns;
		} else {
			String [] strs = proportionsInput.get().split("\\s+");
			double [] proportions = new double[m_nThreads];
			for (int i = 0; i < m_nThreads; i++) {
				proportions[i] = Double.parseDouble(strs[i % strs.length]);
			}
			// normalise
			double sum = 0;
			for (double d : proportions) {
				sum += d;
			}
			for (int i = 0; i < m_nThreads; i++) {
				proportions[i] /= sum;
			}
			// cummulative 
			for (int i = 1; i < m_nThreads; i++) {
				proportions[i] += proportions[i- 1];
			}
			// calc ranges
			for (int i = 0; i < m_nThreads; i++) {
				patternPoints[i+1] = (int) (proportions[i] * nPatterns + 0.5);
			}
		}
    }


	/** Determine indices of m_fRootProbabilities that need to be updates
	// due to sites being invariant. If none of the sites are invariant,
	// the 'site invariant' category does not contribute anything to the
	// root probability. If the site IS invariant for a certain character,
	// taking ambiguities in account, there is a contribution of 1 from
	// the 'site invariant' category.
	 **/
    void calcConstantPatternIndices(int nPatterns, int nStateCount) {
		m_iConstantPattern = new ArrayList<Integer>();
		for (int i = 0; i < nPatterns; i++) {
			int [] pattern = m_data.get().getPattern(i);
			boolean [] bIsInvariant = new boolean[nStateCount];
			Arrays.fill(bIsInvariant, true);
			for (int j = 0; j < pattern.length; j++) {
				int state = pattern[j];
				boolean [] bStateSet = m_data.get().getStateSet(state);
				if (m_useAmbiguities.get() || !m_data.get().getDataType().isAmbiguousState(state)) {
	    			for (int k = 0; k < nStateCount; k++) {
	    				bIsInvariant[k] &= bStateSet[k];
	    			}
				}
			}
				for (int k = 0; k < nStateCount; k++) {
				if (bIsInvariant[k]) {
	    			m_iConstantPattern.add(i * nStateCount + k);    					
				}
			}
		}
    }
    
    void initCore() {
        int nodeCount = m_tree.get().getNodeCount();
        m_likelihoodCore.initialize(
	        nodeCount,
	        m_data.get().getPatternCount(),
	        m_siteModel.getCategoryCount(),
	        m_data.get().getWeights(),
	        m_iConstantPattern,
	        m_nThreads,
	        true
        );
        int extNodeCount = nodeCount / 2 + 1;
        int intNodeCount = nodeCount / 2;

        if (m_useAmbiguities.get()) {
        	setPartials(m_tree.get().getRoot(), m_data.get().getPatternCount());
        } else {
        	setStates(m_tree.get().getRoot(), m_data.get().getPatternCount());
        }
        m_nHasDirt = Tree.IS_FILTHY;
        for (int i = 0; i < intNodeCount; i++) {
       		m_likelihoodCore.createNodePartials(extNodeCount + i);
        }
    }
    
    /**
     * This method samples the sequences based on the tree and site model.
     */
    public void sample(State state, Random random) {
        throw new UnsupportedOperationException("Can't sample a fixed alignment!");
    }

    /** set leaf states in likelihood core **/
    void setStates(Node node, int patternCount) {
        if (node.isLeaf()) {
            int i;
            int[] states = new int[patternCount];
            int iTaxon = m_data.get().getTaxonIndex(node.getID());
            for (i = 0; i < patternCount; i++) {
                states[i] = m_data.get().getPattern(iTaxon, i);
            }
        	m_likelihoodCore.setNodeStates(node.getNr(), states);
        } else {
            setStates(node.getLeft(), patternCount);
            setStates(node.getRight(), patternCount);
        }
    }

    /** set leaf partials in likelihood core **/
    void setPartials(Node node, int patternCount) {
        if (node.isLeaf()) {
        	Alignment data = m_data.get();
        	int nStates = data.getDataType().getStateCount();
            double[] partials = new double[patternCount * nStates];

            int k = 0;
            int iTaxon = m_data.get().getTaxonIndex(node.getID());
            for (int iPattern = 0; iPattern < patternCount; iPattern++) {
            	int nState = data.getPattern(iTaxon, iPattern);
            	boolean [] stateSet = data.getStateSet(nState);
        		for (int iState = 0; iState < nStates; iState++) {
        			partials[k++] = (stateSet[iState] ? 1.0 : 0.0);
            	}
            }
       		m_likelihoodCore.setNodePartials(node.getNr(), partials);

        } else {
        	setPartials(node.getLeft(), patternCount);
        	setPartials(node.getRight(), patternCount);
        }
    }
    
    /**
     * Calculate the log likelihood of the current state.
     * @return the log likelihood.
     */
    double m_fScale = 1.01;
    int m_nScale = 0;
    int X = 1000;
    double[] frequencies;
    double[] proportions;

    @Override
    public double calculateLogP() throws Exception {
    	if (m_beagle != null) {
    		logP =  calculateLogPByBeagle();
    		return logP;
    	}
        Tree tree = m_tree.get();

       
        cacheNodeCount = 0;
        m_root = tree.getRoot();
        frequencies = m_substitutionModel.getFrequencies();
        proportions = m_siteModel.getCategoryProportions(m_root);

        traverseSetup(m_root);
       	threadedTraverse(m_root);
       	calcLogP();

//        System.err.println(Arrays.toString(m_fRootPartials[0]));
//        System.err.println(Arrays.toString(m_fPatternLogLikelihoods));
//        System.err.println(logP);
        m_nScale++;
        if (logP > 0 || (m_likelihoodCore.getUseScaling() && m_nScale > X)) {
            System.err.println("Switch off scaling");
            m_likelihoodCore.setUseScaling(1.0);
            m_likelihoodCore.unstore();
            m_nHasDirt = Tree.IS_FILTHY;
            X *= 2;
            cacheNodeCount = 0;
            traverseSetup(m_root);
           	threadedTraverse(m_root);
            calcLogP();
            return logP;
        } else if (logP == Double.NEGATIVE_INFINITY && m_fScale < 10 && !scaling.get().equals(Scaling.none)) { // && !m_likelihoodCore.getUseScaling()) {
//        	if (true) {
//        		System.err.print("x");
//        		return logP;
//        	}
        	m_nScale = 0;
        	m_fScale *= 1.01;
            System.err.println("Turning on scaling to prevent numeric instability " + m_fScale);
            m_likelihoodCore.setUseScaling(m_fScale);
            m_likelihoodCore.unstore();
            m_nHasDirt = Tree.IS_FILTHY;
            cacheNodeCount = 0;
            traverseSetup(m_root);
           	threadedTraverse(m_root);
            calcLogP();
            return logP;
        }
        return logP;
    }



	private void calcLogP() throws Exception {
        logP = 0.0;
        if (m_bAscertainedSitePatterns) {
        	m_fPatternLogLikelihoods = m_likelihoodCore.getPatternLogLikelihoods();
            double ascertainmentCorrection = ((AscertainedAlignment)m_data.get()).getAscertainmentCorrection(m_fPatternLogLikelihoods);
            for (int i = 0; i < m_data.get().getPatternCount(); i++) {
            	logP += (m_fPatternLogLikelihoods[i] - ascertainmentCorrection) * m_data.get().getPatternWeight(i);
            }
        } else {
        	for (int i = 0; i < m_nThreads; i++) {
        		logP += logPByThread[i];
        	}
//	        for (int i = 0; i < m_data.get().getPatternCount(); i++) {
//	            logP += m_fPatternLogLikelihoods[i] * m_data.get().getPatternWeight(i);
//	        }
        }
    }

    private double calcPartialLogP(int iFrom, int iTo) throws Exception {
        double logP = 0.0;
        if (m_bAscertainedSitePatterns) {
        	return 0;
//            double ascertainmentCorrection = ((AscertainedAlignment)m_data.get()).getAscertainmentCorrection(m_fPatternLogLikelihoods);
//            for (int i = iFrom; i < iTo; i++) {
//            	logP += (m_fPatternLogLikelihoods[i] - ascertainmentCorrection) * m_data.get().getPatternWeight(i);
//            }
        } else {
            for (int i = iFrom; i < iTo; i++) {
	            logP += m_fPatternLogLikelihoods[i] * m_data.get().getPatternWeight(i);
	        }
        }
        return logP;
    }
    
    
    private final List<Callable<Double>> coreCallers = new ArrayList<Callable<Double>>();

	Node m_root;
//    class CoreCaller implements Callable<Double> {
//
//    	public CoreCaller(int iThread, Node root, int iFrom, int iTo, ThreadedLikelihoodCore core) {
//		    m_iThread = iThread;
//			m_root = root;
//			m_iFrom = iFrom;
//			m_iTo = iTo;
//			m_core = core;
//    	}
//
//        public Double call() throws Exception {
//  		  	try {
//  		  		traverse(m_root, m_iFrom, m_iTo, m_core, m_iThread);
//  		  	} catch (Exception e) {
//  		  		System.err.println("Something went wrong in a traversal thread from " + m_iFrom + " to " + m_iTo);
//				e.printStackTrace();
//				System.exit(0);
//			}
//  		  	return 0.0;
//        }
//
//		int m_iThread;
//		private int m_iFrom;
//		private int m_iTo;
//		ThreadedLikelihoodCore m_core;
//    }

    
	//Lock [] m_lock;
	class CoreRunnable implements Runnable {
		int m_iThread;
		private Node m_root;
		private int m_iFrom;
		private int m_iTo;
		ThreadedLikelihoodCore m_core;
		
		CoreRunnable(int iThread, Node root, int iFrom, int iTo, ThreadedLikelihoodCore core) {
			    m_iThread = iThread;
				m_root = root;
				m_iFrom = iFrom;
				m_iTo = iTo;
				m_core = core;
		}

        public void run() {
  		  	try {
  		    	
	            if (m_iConstantPattern != null) { // && !SiteModel.g_bUseOriginal) {
	            	m_fProportionInvariant = m_siteModel.getProportionInvariant();
	            }
	        	//m_core.calculateAllPartials(cacheNode1, cacheNode2, cacheNode3, cacheNodeCount, m_iFrom, m_iTo);
	            logPByThread[m_iThread] = m_core.calcLogP(m_iThread, 
	            		cacheNode1, cacheNode2, cacheNode3, cacheNodeCount,
	            		m_iFrom, m_iTo, m_root.getNr(), proportions, m_fProportionInvariant, frequencies);
  		  	} catch (Exception e) {
  		  		System.err.println("Something went wrong in a traversal thread from " + m_iFrom + " to " + m_iTo);
				e.printStackTrace();
				System.exit(0);
			}
  		    m_nCountDown.countDown();
        }

	} // CoreRunnable

	CountDownLatch m_nCountDown;
	
	void threadedTraverse(Node root) throws Exception {
		try {
//			if (m_nThreads > 1) {
//				int nPatterns = m_fPatternLogLikelihoods.length;
//				int nRange = nPatterns / m_nThreads;
//				//coreCallers.clear();
//				m_root = root;
//				if (coreCallers.size() == 0) {
//					int iFrom = 0;
//					// kick off the threads
//			    	for (int iThread = 1; iThread < m_nThreads; iThread++) {
//			    		int iTo = iFrom + nRange; 
//			    		CoreCaller coreCaller = new CoreCaller(iThread, root, iFrom, iTo, m_likelihoodCore);
//			    		coreCallers.add(coreCaller);
//						iFrom = iTo;
//			    	}
//		    		CoreCaller coreCaller = new CoreCaller(0, root, iFrom, nPatterns, m_likelihoodCore);
//		    		coreCallers.add(coreCaller);
//				}
////				int iFrom = nRange * (m_nThreads - 1);
//                List<Future<Double>> results = BeastMCMC.g_exec.invokeAll(coreCallers);
//
////		    	traverse(root, iFrom, nPatterns, m_likelihoodCore, 0);
//		    	// wait for all other threads to join
//		    	logP = 0;
//                for (Future<Double> result : results) {
//                	logP += result.get();
//                }
//			} else {
//				int nPatterns = m_fPatternLogLikelihoods.length;
//		    	traverse(root, 0, nPatterns, m_likelihoodCore, 0);
//			}
			int nPatterns = m_fPatternLogLikelihoods.length;
			if (m_nThreads >= 1) {
				m_nCountDown = new CountDownLatch(m_nThreads);
				int iFrom = patternPoints[0];
				int nRange = nPatterns / m_nThreads;
				// kick off the threads
		    	for (int iThread = 1; iThread < m_nThreads; iThread++) {
		    		//int iTo = iFrom + nRange;
		    		int iTo = patternPoints[iThread];
		    		CoreRunnable coreRunnable = new CoreRunnable(iThread, root, iFrom, iTo, m_likelihoodCore);
		    		BeastMCMC.g_exec.execute(coreRunnable);
					iFrom = iTo;
		    	}
	    		CoreRunnable coreRunnable = new CoreRunnable(0, root, iFrom, nPatterns, m_likelihoodCore);
	    		BeastMCMC.g_exec.execute(coreRunnable);
		    	//traverse(root, iFrom, nPatterns, m_likelihoodCore, 0);
	    	
				m_nCountDown.await();
			} else {
		        if (m_iConstantPattern != null) { // && !SiteModel.g_bUseOriginal) {
		        	m_fProportionInvariant = m_siteModel.getProportionInvariant();
		        }
				//m_likelihoodCore.calculateAllPartials(cacheNode1, cacheNode2, cacheNode3, cacheNodeCount, 0, nPatterns);
		        logPByThread[0] = m_likelihoodCore.calcLogP(0, 
	            		cacheNode1, cacheNode2, cacheNode3, cacheNodeCount,
		        		0, nPatterns, m_root.getNr(), proportions, m_fProportionInvariant, frequencies);
				
//		    	traverse(root, 0, nPatterns, m_likelihoodCore, 0);
			}
		} catch (RejectedExecutionException e) {
			m_nThreads--;
			System.err.println("Reducing nr of threads to " + m_nThreads);
			// refresh thread pool
			BeastMCMC.g_exec = Executors.newFixedThreadPool(BeastMCMC.m_nThreads);
			threadedTraverse(root);
		}
    }
	
	
	
	
	class BeagleCoreRunnable implements Runnable {
		int m_iThread;
		BeagleTreeLikelihood beagle;
		
		BeagleCoreRunnable(int iThread, BeagleTreeLikelihood beagle) {
			    m_iThread = iThread;
			    this.beagle = beagle;
		}

        public void run() {
  		  	try {
	            logPByThread[m_iThread] = beagle.calculateLogP();
  		  	} catch (Exception e) {
  		  		System.err.println("Something went wrong ith thread " + m_iThread);
				e.printStackTrace();
				System.exit(0);
			}
  		    m_nCountDown.countDown();
        }

	} // CoreRunnable
	
    private double calculateLogPByBeagle() throws Exception {
		try {
			if (m_nThreads >= 1) {
				m_nCountDown = new CountDownLatch(m_nThreads);
		    	for (int iThread = 0; iThread < m_nThreads; iThread++) {
		    		BeagleCoreRunnable coreRunnable = new BeagleCoreRunnable(iThread, m_beagle[iThread]);
		    		BeastMCMC.g_exec.execute(coreRunnable);
		    	}
				m_nCountDown.await();
		    	logP = 0;
		    	for (double f : logPByThread) {
		    		logP += f;
		    	}
			} else {
				logP = m_beagle[0].calculateLogP();
			}
		} catch (RejectedExecutionException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return logP;
	}
    
	
    /* Assumes there IS a branch rate model as opposed to traverse() */
    int traverseSetup(Node node) throws Exception {

        int update = (node.isDirty()| m_nHasDirt);

        int iNode = node.getNr();

        double branchRate = m_branchRateModel.getRateForBranch(node);
        double branchTime = node.getLength() * branchRate;

        // First update the transition probability matrix(ices) for this branch
        //if (!node.isRoot() && (update != Tree.IS_CLEAN || branchTime != m_StoredBranchLengths[iNode])) {
        if (!node.isRoot() && (update != Tree.IS_CLEAN || branchTime != m_branchLengths[iNode])) {
            m_branchLengths[iNode] = branchTime;        
            Node parent = node.getParent();
            m_likelihoodCore.setNodeMatrixForUpdate(iNode);
//            synchronized (this) {
	            for (int i = 0; i < m_siteModel.getCategoryCount(); i++) {
	                double jointBranchRate = m_siteModel.getRateForCategory(i, node) * branchRate;
	            	m_substitutionModel.getTransitionProbabilities(node, parent.getHeight(), node.getHeight(), jointBranchRate, m_fProbabilities);
                	m_likelihoodCore.setNodeMatrix(iNode, i, m_fProbabilities);
	            }
//            }
            update |= Tree.IS_DIRTY;
        }

        // If the node is internal, update the partial likelihoods.
        if (!node.isLeaf()) {

            // Traverse down the two child nodes
            Node child1 = node.getLeft(); //Two children
            int update1 = traverseSetup(child1);

            Node child2 = node.getRight();
            int update2 = traverseSetup(child2);


            update |= (update1|update2);

            // If either child node was updated then update this node too
            if (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN) {
	            if (m_siteModel.integrateAcrossCategories()) {
	            	cacheNode1[cacheNodeCount] = child1.getNr();
	            	cacheNode2[cacheNodeCount] = child2.getNr();
	            	cacheNode3[cacheNodeCount] = iNode;
	            	cacheNodeCount++;
	            } else {
	                throw new Exception("Error TreeLikelihood 201: Site categories not supported");
	                //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
	            }

//                m_likelihoodCore.setNodePartialsForUpdate(iNode);
//                if (update >= Tree.IS_FILTHY) {
//                	m_likelihoodCore.setNodeStatesForUpdate(iNode);
//                }
            }
        }
        return update;
    } // traverseSetup
	
    /* Assumes there IS a branch rate model as opposed to traverse() */
//    int traverse(Node node, int iFrom, int iTo, ThreadedLikelihoodCore core, int iThread) throws Exception {
//        
//        int update = (node.isDirty()| m_nHasDirt);
//
//        int iNode = node.getNr();
//
////        double branchRate = m_branchRateModel.getRateForBranch(node);
////        double branchTime = node.getLength() * branchRate;
////        synchronized (this) {
////        	m_branchLengths[iNode] = branchTime;
////        }
//        double branchTime = m_branchLengths[iNode];
//        
//        // First update the transition probability matrix(ices) for this branch
//        if (!node.isRoot() && (update != Tree.IS_CLEAN || branchTime != m_StoredBranchLengths[iNode])) {
//            update |= Tree.IS_DIRTY;
//        }
//
//        // If the node is internal, update the partial likelihoods.
//        if (!node.isLeaf()) {
//
//            // Traverse down the two child nodes
//            Node child1 = node.m_left; //Two children
//            int update1 = traverse(child1, iFrom, iTo, core, iThread);
//
//            Node child2 = node.m_right;
//            int update2 = traverse(child2, iFrom, iTo, core, iThread);
//
//            // If either child node was updated then update this node too
//            if (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN) {
//
//                int childNum1 = child1.getNr();
//                int childNum2 = child2.getNr();
//
//                update |= (update1|update2);
//
//                if (m_siteModel.integrateAcrossCategories()) {
//                	core.calculatePartials(childNum1, childNum2, iNode, iFrom, iTo);
//                } else {
//                    throw new Exception("Error TreeLikelihood 201: Site categories not supported");
//                    //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
//                }
//
//                if (node.isRoot()) {
//                    synchronized (this) {
//	                    // No parent this is the root of the beast.tree -
//	                    // calculate the pattern likelihoods
////	                    double[] frequencies = //m_pFreqs.get().
////	                            m_substitutionModel.getFrequencies();
////	
////	                    double[] proportions = m_siteModel.getCategoryProportions(node);
////	                    core.integratePartials(node.getNr(), proportions, iThread, iFrom, iTo);
//	
//	                    if (m_iConstantPattern != null) { // && !SiteModel.g_bUseOriginal) {
//	                    	m_fProportionInvariant = m_siteModel.getProportianInvariant();
//	                    	// some portion of sites is invariant, so adjust root partials for this
//	                    	//double [] fRootPartials = m_fRootPartials[iThread];
////	    	            	core.calcInvarCorrection(m_fProportionInvariant, iThread);
//	                    }
////	                    core.calculateLogLikelihoods(iThread, frequencies, iFrom, iTo);
////	                    logPByThread[iThread] = core.calcPartialLogP(iFrom, iTo);
//	    	            logPByThread[iThread] = core.calcLogP(iThread, iFrom, iTo, node.getNr(), proportions, m_fProportionInvariant, frequencies);
//					}
//                }
//
//            }
//        }
//        return update;
//    } // traverse

    /** CalculationNode methods **/

    /**
     * check state for changed variables and update temp results if necessary *
     */
    @Override
    protected boolean requiresRecalculation() {
    	if (m_beagle != null) {
    		boolean requiresRecalculation = false;
    		for (BeagleTreeLikelihood b : m_beagle) {
    			requiresRecalculation |= b.requiresRecalculation();
    		}
    		return requiresRecalculation;
    	}
        m_nHasDirt = Tree.IS_CLEAN;

        if (m_data.get().isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_FILTHY;
            return true;
        }
        if (m_siteModel.isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_DIRTY;
            return true;
        }
        if (m_branchRateModel != null && m_branchRateModel.isDirtyCalculation()) {
            //m_nHasDirt = Tree.IS_FILTHY;
            return true;
        }
        return m_tree.get().somethingIsDirty();
    }

    @Override
    public void store() {
    	if (m_beagle != null) {
    		for (BeagleTreeLikelihood b : m_beagle) {
    			b.store();
    		}
    		return;
    	}
    	if (m_likelihoodCore != null) {
    		m_likelihoodCore.store();
    	}
        super.store();
        System.arraycopy(m_branchLengths, 0, m_StoredBranchLengths, 0, m_branchLengths.length);
    }

    @Override
    public void restore() {
    	if (m_beagle != null) {
    		for (BeagleTreeLikelihood b : m_beagle) {
    			b.restore();
    		}
    		return;
    	}
    	if (m_likelihoodCore != null) {
       		m_likelihoodCore.restore();
    	}
        super.restore();
        double [] tmp = m_branchLengths;
        m_branchLengths = m_StoredBranchLengths;
        m_StoredBranchLengths = tmp;
    }
        
    /**
     * @return a list of unique ids for the state nodes that form the argument
     */
    public List<String> getArguments() {
        return Collections.singletonList(m_data.get().getID());
    }

    /**
     * @return a list of unique ids for the state nodes that make up the conditions
     */
    public List<String> getConditions() {
        return m_siteModel.getConditions();
    }
    
} // class TreeLikelihood
