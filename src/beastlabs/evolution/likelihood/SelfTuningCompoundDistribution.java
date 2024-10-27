package beastlabs.evolution.likelihood;





import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import beagle.BeagleFactory;
import beagle.ResourceDetails;
import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.core.ProgramStatus;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.evolution.likelihood.BeagleTreeLikelihood.PartialsRescalingScheme;
import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beast.base.evolution.likelihood.ThreadedTreeLikelihood;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Distribution;
import beast.base.inference.State;


@Description("Takes a collection of tree likelihoods and combines them into the compound of these distributions " +
        "while tuning "
        + "the number of threads, "
        + "types of BEAGLE instances, and"
        + "whether to use BEAGLE 2 or 3 API (i.e. seperate TreeLikelihoods or MultiPartitionTreeLikelihood). "
        + "Self tuning replacement of CompounDistribution with id=='likelihood'.")
public class SelfTuningCompoundDistribution extends Distribution {
    // no need to make this input REQUIRED. If no distribution input is
    // specified the class just returns probability 1.
    final public Input<List<Distribution>> pDistributions =
            new Input<>("distribution",
                    "individual probability distributions, e.g. the likelihood and prior making up a posterior",
                    new ArrayList<>());
    final public Input<Boolean> useThreadsInput = new Input<>("useThreads", "calculated the distributions in parallel using threads (default true)", true);
    final public Input<Integer> minNrOfThreadsInput = new Input<>("minThreads","minimum number of threads to use (default 1)", 1);
    final public Input<Integer> maxNrOfThreadsInput = new Input<>("maxThreads","maximum number of threads to use, if less than 1 the number of threads in BeastMCMC is used (default -1)", -1);
    final public Input<Boolean> ignoreInput = new Input<>("ignore", "ignore all distributions and return 1 as distribution (default false)", false);
    

    final public Input<Long> swithcCountInput = new Input<>("switchCount", "number of milli seconds to calculate likelihood before switching configuration", 500l);
    final public Input<Long> reconfigCountInput = new Input<>("reconfigCount", "number of times to calculate likelihood before self tuning again", 100000l);
    final public Input<Integer> stopAfterSamerResultsInput = new Input<>("stopAfterSamerResults", "number of times the same configuration is optimal in a row before stopping to tune", 3);
    
    final public Input<Boolean> includeMPTLInput = new Input<>("includeMPTL", "include multi-partition (BEAGLE 3) tree likelihood in configurations", true);
    final public Input<Boolean> includeSPTLInput = new Input<>("includeSPTL", "include single-partition (BEAGLE 2) tree likelihood in configurations", true);
    
    class Configuration {
    	long nrOfSamples;
    	double totalRunTime;
    	int threadCount;
    	
    	Configuration(int threadCount) {
    		this.nrOfSamples = 0;
    		this.totalRunTime = 0;
    		this.threadCount = threadCount;
    	}
    
    	
    	// defaults to single threaded likelihood
    	double calculateLogP() {
    		nrOfSamples++;
    		
    		double logP = 0;
            if (ignore) {
            	return logP;
            }
            int workAvailable = 0;
            if (threadCount > 1) {
    	        for (Distribution dists : pDistributions.get()) {
    	            if (dists.isDirtyCalculation()) {
    	            	workAvailable++;
    	            }
    	        }
            }
            if (threadCount > 1 && workAvailable > 1) {
                logP = calculateLogPUsingThreads();
            } else {
                for (Distribution dists : pDistributions.get()) {
                    if (dists.isDirtyCalculation()) {
                        logP += dists.calculateLogP();
                    } else {
                        logP += dists.getCurrentLogP();
                    }
                    if (Double.isInfinite(logP) || Double.isNaN(logP)) {
                        return logP;
                    }
                }
            }
            return logP;
    	}
    	
    	void reset() {
//	        for (Distribution dists : pDistributions.get()) {
//	        	Tree tree = (Tree) dists.getInput("tree").get();
//	        	tree.setEverythingDirty(true);
//	        }
//    		calculateLogP();
    	}
    	
    	@Override
    	public String toString() {
    		return "Separate partition " + threadCount + " thread" + (threadCount > 1 ? "s" :"");
    	}
    }

    class MultiPartitionConfiguration extends Configuration {
    	private MultiPartitionTreeLikelihood mpTreeLikelihood;

    	MultiPartitionConfiguration(MultiPartitionTreeLikelihood mpt) {
    		super(1);
    		this.mpTreeLikelihood = mpt;
    	}
    	
    	double calculateLogP() {
    		nrOfSamples++;
    		return mpTreeLikelihood.calculateLogP();
    	}
    	
    	@Override
    	void reset() {
//        	Tree tree = (Tree) mpTreeLikelihood.likelihoodsInput.get().get(0).getInput("tree").get();
//        	tree.setEverythingDirty(true);
//    		calculateLogP();
    	}
    	
    	@Override
    	public String toString() {
    		String hardware = mpTreeLikelihood.getProcessor();
    		return "Multipartition " + hardware;
    	}

    }
    
    private List<Configuration> configurations;
    private Configuration bestConfigurationSoFar, lastBestConfiguration = null;
    private Configuration currentConfiguration = null;
    
    // represent current configuration
	private long switchTime = 0;
	private long switchCount;
	
	
	private boolean keepTuning;
	private boolean initialMeasurement;
	private int sameOptimumCount;

    
    /**
     * flag to indicate threads should be used. Only effective if the useThreadsInput is
     * true and BeasMCMC.nrOfThreads > 1
     */
    boolean useThreads;
    int maxNrOfThreads;
    boolean ignore;
    public static ExecutorService exec;
    
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        if (minNrOfThreadsInput.get() < 1) {
        	throw new IllegalArgumentException("minThreads must be at least 1");
        }
        
        if (stopAfterSamerResultsInput.get() < 1) {
        	throw new IllegalArgumentException("stopAfterSamerResults must be at least 1");
        }
        
        useThreads = useThreadsInput.get() && (ProgramStatus.m_nThreads > 1);
		maxNrOfThreads = useThreads ? ProgramStatus.m_nThreads : 1;
		if (useThreads && maxNrOfThreadsInput.get() > 0) {
			maxNrOfThreads = Math.min(maxNrOfThreadsInput.get(), ProgramStatus.m_nThreads);
		}
		if (useThreads) {
		     exec = Executors.newFixedThreadPool(maxNrOfThreads);
		}

        ignore = ignoreInput.get();
        
        switchCount = swithcCountInput.get();

        if (pDistributions.get().size() == 0) {
            logP = 0;
        }
        
        
        MultiPartitionTreeLikelihood mpTreeLikelihood = null;
        if (includeMPTLInput.get()) {
        	mpTreeLikelihood = createMultiPartitionTreeLikelihood();
        }
        
        currentConfiguration = initConfigurations(mpTreeLikelihood);
		Log.warning("Starting with " + currentConfiguration.toString());

		keepTuning = configurations.size() > 1;
		sameOptimumCount = 0;
		initialMeasurement = true;
		
		switchTime = System.currentTimeMillis();
		
		
    }


    private MultiPartitionTreeLikelihood createMultiPartitionTreeLikelihood() {
    	
        GenericTreeLikelihood tl0 = null;
        TreeInterface tree = null;
        BranchRateModel branchRateModel = null;
        boolean useAmbiguities = false;
        boolean useTipLikelihoods = false;
        PartialsRescalingScheme rescalingScheme = null;
		List<Alignment> alignments = new ArrayList<>();
        List<SiteModel> siteRateModels = new ArrayList<>();
        List<GenericTreeLikelihood> distributions = new ArrayList<>();
        String dataType = null;

        // TODO: what if the first partition is not sharing the same data type, but others are
        for (Distribution distr : pDistributions.get()) {
    		if (distr instanceof GenericTreeLikelihood) {
    			GenericTreeLikelihood tl = (GenericTreeLikelihood) distr;
    			if (tl0 == null) {
    		        tl0 = (GenericTreeLikelihood)distr;
    		        tree = tl0.treeInput.get();
    		        branchRateModel = tl0.branchRateModelInput.get();
    		        useAmbiguities = (boolean) tl0.getInput("useAmbiguities").get();
    		        useTipLikelihoods = tl0 instanceof TreeLikelihood ? (boolean) tl0.getInput("useTipLikelihoods").get() : false;
    		        rescalingScheme = MultiPartitionTreeLikelihood.getRescalingScheme(tl0);
    		        dataType = tl0.dataInput.get().getDataType().toString();
    			} else {
    	        	if (tree != tl.treeInput.get()) {
    	        		Log.warning("Tree of likelihood " + tl.getID() + " does not match tree in " + tl0.getID() +"\n"
    	        				+ "All likelihoods must share the same tree -- MultiPartitionTreeLikelihood not considered.");
    	        		return null;
    	        	}

    		        if (!tl.dataInput.get().getDataType().toString().equals(dataType)) {
    	        		Log.warning("Data types of tree of likelihood " + tl.getID() + " does not match that in " + tl0.getID() +"\n"
    	        				+ "All likelihoods must share the same data type -- MultiPartitionTreeLikelihood not considered.");
    	        		return null;
    		        }

    	        	
    	        	if (tl.branchRateModelInput.get() != null && branchRateModel != tl.branchRateModelInput.get()) {
    	        		Log.warning("Tree of likelihood " + tl.getID() + " does not match branch rate model in " + tl0.getID() +"\n"
    	        				+ "All likelihoods must share the same branch rate model -- MultiPartitionTreeLikelihood not considered.");
    	        		return null;
    	        	}
    	        	
    	        	if (useAmbiguities != (boolean)tl.getInput("useAmbiguities").get()) {
    	        		Log.warning("All partitions must use ambiguities, or ignore ambiguities, but found a difference between " +
    	        				tl.getID() + " and " + tl0.getID()+"  -- MultiPartitionTreeLikelihood not considered");
    	        		return null;
    	        	}
    	        	if (tl instanceof TreeLikelihood && useTipLikelihoods != (boolean)tl.getInput("useTipLikelihoods").get()) {
    	        		Log.warning("All partitions must use tip likelihoods, or ignore tip likelihoods, but found a difference between " +
    	        				tl.getID() + " and " + tl0.getID() + " -- MultiPartitionTreeLikelihood not considered");
    	        		return null;
    	        	}
    	        	
    	        	if (!rescalingScheme.equals(MultiPartitionTreeLikelihood.getRescalingScheme(tl))) {
    	        		Log.warning("Tree of likelihood " + tl.getID() + " does not match scaling scheme of " + tl0.getID() +"\n"
    	        				+ "All scaling must be the same.  -- MultiPartitionTreeLikelihood not considered");
    	        	}
    				
    			}
	        	alignments.add(tl.dataInput.get());
	        	siteRateModels.add((SiteModel) tl.siteModelInput.get());
    			distributions.add(tl);
    		}
    	}
        
        MultiPartitionTreeLikelihood mpt = new MultiPartitionTreeLikelihood();
        mpt.likelihoodsInput.get().addAll(distributions);
        try {
        	mpt.initialise(tree, alignments, branchRateModel, siteRateModels, useAmbiguities, useTipLikelihoods, rescalingScheme, useTipLikelihoods);
        } catch (Exception e) {
        	e.printStackTrace();
        	return null;
        }
    	
        likelihoodsInput.get().add(mpt);
        return mpt;
	}


	private Configuration initConfigurations(MultiPartitionTreeLikelihood mpTreeLikelihood) {
        List<ResourceDetails> resourceDetails = BeagleFactory.getResourceDetails();
        ResourceDetails rd = resourceDetails.get(0);
        rd.getNumber();

        configurations = new ArrayList<>();

        if (mpTreeLikelihood != null) {
        	configurations.add(new MultiPartitionConfiguration(mpTreeLikelihood));
        }

        if (includeSPTLInput.get()) {
	        for (int threadCount = minNrOfThreadsInput.get(); threadCount <= maxNrOfThreads; threadCount++) {
	        	Configuration oneThreadCfg = new Configuration(threadCount);
	        	configurations.add(oneThreadCfg);
	        }
        }


        return configurations.get(0);
	}


	private boolean switchConfiguration() {
		if (bestConfigurationSoFar != null || !keepTuning) {
			// all configurations tried, and best one found
			return false;
		}
		
		if (initialMeasurement) {
			currentConfiguration.nrOfSamples = 1;
			switchTime = System.currentTimeMillis();
			initialMeasurement = false;
			Log.warning("Start timing " + currentConfiguration.toString());
			return true;
		}
		
		long endTime = System.currentTimeMillis();
		currentConfiguration.totalRunTime += endTime - switchTime;
    	
		int i = configurations.indexOf(currentConfiguration) + 1;
		if (i >= configurations.size()) {
			// tried all configurations, so pick best one
			Configuration cfg0 = configurations.get(0);
			double best = cfg0.totalRunTime / cfg0.nrOfSamples;
			bestConfigurationSoFar = cfg0;
			for (Configuration cfg : configurations) {
				double score = cfg.totalRunTime / cfg.nrOfSamples;
				Log.warning(cfg.toString() + ": " + cfg.totalRunTime + "/" + cfg.nrOfSamples + " = " + cfg.totalRunTime / cfg.nrOfSamples);
				if (score < best) {
					bestConfigurationSoFar = cfg;
					best = score;
				}
			}
			
			currentConfiguration = bestConfigurationSoFar;
			
			if (lastBestConfiguration == bestConfigurationSoFar) {
				// stop tuning if  consecutive tuning sessions gave the same configuration
				sameOptimumCount++;
				if (sameOptimumCount == stopAfterSamerResultsInput.get()-1) {
					keepTuning = false;
				}
			} else {
				sameOptimumCount = 0;
				keepTuning = true;
			}
			lastBestConfiguration = bestConfigurationSoFar;

		} else {
			// continue with next configuration
			currentConfiguration = configurations.get(i);
		}
		
		if (exec != null) {
			exec.shutdown();
		}
		if (currentConfiguration.threadCount > 1) {
			exec = Executors.newFixedThreadPool(currentConfiguration.threadCount);
		}
		
		currentConfiguration.reset();
		
		currentConfiguration.nrOfSamples = 1;
//		currentConfiguration.totalRunTime = 0;

		if (keepTuning) {
			Log.warning((i != configurations.size() ? "Start timing ": "Using ") + currentConfiguration.toString());
		} else {
			Log.warning("Settling for " + currentConfiguration.toString() + " -- tuning finished");
		}
		switchTime = System.currentTimeMillis();
		return true;
	}
	


	/**
     * Distribution implementation follows *
     */
    @Override
    public double calculateLogP() {
        logP = 0;
        if (ignore) {
        	return logP;
        }
        
//        if (currentConfiguration.nrOfSamples % switchCount == 0 && currentConfiguration.nrOfSamples > 0) {
//        	switchConfiguration();
//        }
////        if (System.currentTimeMillis() - switchTime > switchCount) {
////        	switchConfiguration();
////        }
//        
//        if (currentConfiguration.nrOfSamples % reconfigCountInput.get() == 0) {
//        	restartTuning();
//        }

        logP = currentConfiguration.calculateLogP();
        return logP;
    }

    private void restartTuning() {
    	if (!keepTuning) {
    		return;
    	}
    	
    	lastBestConfiguration = bestConfigurationSoFar;

		bestConfigurationSoFar = null;
		
		currentConfiguration = configurations.get(0);
		if (exec != null) {
			exec.shutdown();
		}
		if (currentConfiguration.threadCount > 1) {
			exec = Executors.newFixedThreadPool(currentConfiguration.threadCount);
		}
		
		currentConfiguration.reset();

		for (Configuration cfg : configurations) {
			cfg.nrOfSamples = 0;
			cfg.totalRunTime = 0;
		}
		Log.warning("Start timing " + currentConfiguration.toString());
		switchTime = System.currentTimeMillis();
	}

	class CoreRunnable implements java.lang.Runnable {
        Distribution distr;

        CoreRunnable(Distribution core) {
            distr = core;
        }

        @Override
		public void run() {
            try {
                if (distr.isDirtyCalculation()) {
                    logP += distr.calculateLogP();
                } else {
                    logP += distr.getCurrentLogP();
                }
            } catch (Exception e) {
                Log.err.println("Something went wrong in a calculation of " + distr.getID());
                e.printStackTrace();
                System.exit(1);
            }
            countDown.countDown();
        }

    } // CoreRunnable

    CountDownLatch countDown;

    private double calculateLogPUsingThreads() {
        try {

            int dirtyDistrs = 0;
            for (Distribution dists : pDistributions.get()) {
                if (dists.isDirtyCalculation()) {
                    dirtyDistrs++;
                }
            }
            countDown = new CountDownLatch(dirtyDistrs);
            // kick off the threads
            for (Distribution dists : pDistributions.get()) {
                if (dists.isDirtyCalculation()) {
                    CoreRunnable coreRunnable = new CoreRunnable(dists);
                    exec.execute(coreRunnable);
                }
            }
            countDown.await();
            logP = 0;
            for (Distribution distr : pDistributions.get()) {
                logP += distr.getCurrentLogP();
            }
            return logP;
        } catch (RejectedExecutionException | InterruptedException e) {
            useThreads = false;
            Log.err.println("Stop using threads: " + e.getMessage());
            return calculateLogP();
        }
    }


    @Override
    public void sample(State state, Random random) {
        if (sampledFlag)
            return;

        sampledFlag = true;

        for (Distribution distribution : pDistributions.get()) {
            distribution.sample(state, random);
        }
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();
        for (Distribution distribution : pDistributions.get()) {
            arguments.addAll(distribution.getArguments());
        }
        return arguments;
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();
        for (Distribution distribution : pDistributions.get()) {
            conditions.addAll(distribution.getConditions());
        }
        conditions.removeAll(getArguments());

        return conditions;
    }

    @Override
    public List<BEASTInterface> listActiveBEASTObjects() {
    	if (ignoreInput.get()) {
    		return new ArrayList<>();
    	} else {
    		return super.listActiveBEASTObjects();
    	}
    }

    @Override
    public boolean isStochastic() {
        for (Distribution distribution : pDistributions.get()) {
            if (distribution.isStochastic())
                return true;
        }
        
        return false;
    }
    
    @Override
    public double getNonStochasticLogP() {
        double logP = 0;
        if (ignore) {
        	return logP;
        }
        // The loop could gain a little bit from being multithreaded
        // though getNonStochasticLogP is called for debugging purposes only
        // so efficiency is not an immediate issue.
        for (Distribution dists : pDistributions.get()) {
            logP += dists.getNonStochasticLogP();
            if (Double.isInfinite(logP) || Double.isNaN(logP)) {
                return logP;
            }
        }
        return logP;
    }
    
    
    /** private list of likelihoods, to notify framework of TreeLikelihoods being created in initAndValidate() **/
    final private Input<List<Distribution>> likelihoodsInput = new Input<>("*","",new ArrayList<>());

    @Override
    public List<Input<?>> listInputs() {
    	List<Input<?>> list =  super.listInputs();
    	if (!ProgramStatus.name.equals("BEAUti") && System.getProperty("beast.is.junit.testing") == null) {
    		// do not expose internal likelihoods to BEAUti or junit tests
    		list.add(likelihoodsInput);
    	}
    	return list;
    }

    
//    @Override
//    public void restore() {
//    	if (currentConfiguration.nrOfSamples <= 2) {
//    		currentConfiguration.reset();
//    	}
//    	super.restore();
//    }


	public boolean update(long sample) {
        if (sample > 0 && sample % reconfigCountInput.get() == 0 && configurations.size() > 1) {
        	restartTuning();
        	return true;
        }

        if (currentConfiguration.nrOfSamples % switchCount == 0 && currentConfiguration.nrOfSamples > 0) {
        	return switchConfiguration();
        }
//        if (System.currentTimeMillis() - switchTime > switchCount) {
//        	switchConfiguration();
//        }
        
    	return false;
	}

} // class CompoundDistribution
