package beast.inference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Logger;
import beast.base.inference.Logger.LOGMODE;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.StateNodeInitialiser;
import beast.base.core.Input.Validate;
import beast.base.core.Log;

@Description("Extension to MCMC used by IndependentMCMC")
public class ConvergableMCMC extends MCMC {
	
	
	
	
	// Dummy inputs
//	public final Input<Integer> checkForConvergenceEveryInput = new Input<Integer>("checkEvery", "dummy input");
//	public final Input<Double> thresholdInput = new Input<Double>("threshold", "dummy input");
//	public final Input<Integer> convergenceLengthInput = new Input<Integer>("convergedFor", "dummy input");
//	public final Input<String> tempDirInput = new Input<>("tempDir","dummy input", "/tmp/");
//	public final Input<List<Logger>> treeStorersInput = new Input<>("treeStorer", "dummy input", new ArrayList<Logger>());
//	public final Input<Integer> burnInPercentageInput = new Input<Integer>("convergenceBurnin", "dummy input");
//	public final Input<Logger> rHatLoggerInput = new Input<>("rhatLogger", "dummy input", Input.Validate.OPTIONAL);
//	public final Input<Double> minESSInput = new Input<>("ESS", "dummy input", 200.0);
//	public final Input<Double> maxRHatInput = new Input<>("rhat", "dummy input", 1.05);
	
	
	
	protected int chainNumber;
	protected int currentSample = 0;
	protected int corrections = 0;
	protected boolean isStochastic;
	protected boolean allowScreenLogging = true;
	
	
	
	// Specify whether this chain is allowed to screen log or not
	public void allowScreenLogging(boolean allow) {
		this.allowScreenLogging = allow;
	}
	
	
	// Set the chain number
	public void setChainNr(int nr) {
		this.chainNumber = nr;
	}
	
	
	// Get the chain number
	public int getChainNr() {
		return this.chainNumber;
	}
	
	
	// Add a logger
	public void addLogger(Logger logger) {
		if (loggers == null) loggers = new ArrayList<Logger>();
		this.loggers.add(logger);
	}
	
	
	
	public ConvergableMCMC() {
		
		// Do not allow the user to specify chain length
		chainLengthInput.setRule(Validate.FORBIDDEN);
	}


	
	@Override
    public void run() throws SAXException, IOException, ParserConfigurationException {
    	

    	
        // Set up state (again). Other plugins may have manipulated the
        // StateNodes, e.g. set up bounds or dimensions
        state.initAndValidate();
        
        // Also, initialise state with the file name to store and set-up whether to resume from file
        state.setStateFileName(stateFileName);
        operatorSchedule.setStateFileName(stateFileName);

        burnIn = burnInInput.get();
        int nInitialisationAttempts = 0;
        //state.setEverythingDirty(true);
        posterior = posteriorInput.get();

        if (restoreFromFile) {
            state.restoreFromFile();
            operatorSchedule.restoreFromFile();
            burnIn = 0;
            oldLogLikelihood = state.robustlyCalcPosterior(posterior);
        } else {
            do {
                for (final StateNodeInitialiser initialiser : initialisersInput.get()) {
                    initialiser.initStateNodes();
                }
                oldLogLikelihood = state.robustlyCalcPosterior(posterior);
                nInitialisationAttempts += 1;
            } while (Double.isInfinite(oldLogLikelihood) && nInitialisationAttempts < numInitializationAttempts.get());
        }
        final long startTime = System.currentTimeMillis();

        // do the sampling
        logAlpha = 0;
        debugFlag = Boolean.valueOf(System.getProperty("beast.debug"));


//        System.err.println("Start state:");
//        System.err.println(state.toString());

        System.err.println("Start likelihood: " + oldLogLikelihood + " " + (nInitialisationAttempts > 1 ? "after " + nInitialisationAttempts + " initialisation attempts" : ""));
        if (Double.isInfinite(oldLogLikelihood) || Double.isNaN(oldLogLikelihood)) {
            reportLogLikelihoods(posterior, "");
            throw new RuntimeException("Could not find a proper state to initialise. Perhaps try another seed.");
        }

        // Get / add loggers
        if (loggers == null) loggers = loggersInput.get();
        else loggers.addAll(loggersInput.get());
        

        // put the loggers logging to stdout at the bottom of the logger list so that screen output is tidier.
        Collections.sort(loggers, (o1, o2) -> {
            if (o1.isLoggingToStdout()) {
                return o2.isLoggingToStdout() ? 0 : 1;
            } else {
                return o2.isLoggingToStdout() ? -1 : 0;
            }
        });
        
        // Remove all screen loggers if applicable
        boolean hasScreenLog = false;
        List<Integer> loggersToRemove = new ArrayList<Integer>();
        for (int logNum = 0; logNum < loggers.size(); logNum ++) {
        	Logger l = loggers.get(logNum);
        	if (l.isLoggingToStdout() || (l.getID() != null && l.getID().equals("screenlog"))) {
        		if (!allowScreenLogging) loggersToRemove.add(logNum);
        		hasScreenLog = true;
        	}
        }
        
        if (allowScreenLogging && hasScreenLog) {
        	Log.warning.println("Chain " + this.getChainNr() + "'s log will be printed on screen.");
        }
        
        else if (!allowScreenLogging && hasScreenLog) {
        	Log.warning.println("Chain " + this.getChainNr() + "'s log will NOT be printed on screen.");
        }
        
        
        Collections.reverse(loggersToRemove);
        for (int logNum : loggersToRemove) {
        	loggers.remove(logNum);
        }
        

        // Initialises log so that log file headers are written, etc.
        for (final Logger log : loggers) {
        	log.init();
        }
        
        
        currentSample = 0;
        corrections = 0;
        isStochastic = posterior.isStochastic();

    } 
    
    

    
    
    

	// run MCMC inner loop for resampleEvery nr of samples
	protected long runForNSteps(int nSteps) throws Exception {
		
		
		
	       int corrections = 0;
	       for (int sampleNr = currentSample; sampleNr < currentSample + nSteps; sampleNr++) {
	            final int currentState = sampleNr;
	            
	            final Operator operator = propagateState(sampleNr);

	            if (debugFlag && sampleNr % 3 == 0 || sampleNr % 10000 == 0) {
	                // check that the posterior is correctly calculated at every third
	                // sample, as long as we are in debug mode
	            	final double originalLogP = isStochastic ? posterior.getNonStochasticLogP() : oldLogLikelihood;
	                final double logLikelihood = isStochastic ? state.robustlyCalcNonStochasticPosterior(posterior) : state.robustlyCalcPosterior(posterior);
	                if (isTooDifferent(logLikelihood, originalLogP)) {
	                    reportLogLikelihoods(posterior, "");
	                    Log.err.println("At sample " + sampleNr + "\nLikelihood incorrectly calculated: " + originalLogP + " != " + logLikelihood
	                    		+ "(" + (originalLogP - logLikelihood) + ")"
	                            + " Operator: " + operator.getName());
	                }
	                if (sampleNr > NR_OF_DEBUG_SAMPLES * 3) {
	                    // switch off debug mode once a sufficient large sample is checked
	                    debugFlag = false;
	                    if (isTooDifferent(logLikelihood, originalLogP)) {
	                        // incorrect calculation outside debug period.
	                        // This happens infrequently enough that it should repair itself after a robust posterior calculation
	                        corrections++;
	                        if (corrections > 100) {
	                            // after 100 repairs, there must be something seriously wrong with the implementation
	                        	Log.err.println("Too many corrections. There is something seriously wrong that cannot be corrected");
	                            state.storeToFile(sampleNr);
	                            operatorSchedule.storeToFile();
	                            System.exit(1);
	                        }
	                        oldLogLikelihood = state.robustlyCalcPosterior(posterior);;
	                    }
	                } else {
	                    if (isTooDifferent(logLikelihood, originalLogP)) {
	                        // halt due to incorrect posterior during intial debug period
	                        state.storeToFile(sampleNr);
	                        operatorSchedule.storeToFile();
	                        System.exit(1);
	                    }
	                }
	            } else {
	                if (sampleNr >= 0) {
	                	operator.optimize(logAlpha);
	                }
	            }
	            callUserFunction(sampleNr);

	            // make sure we always save just before exiting
	            if (storeEvery > 0 && (sampleNr + 1) % storeEvery == 0 || sampleNr == chainLength) {
	                /*final double logLikelihood = */
	                state.robustlyCalcNonStochasticPosterior(posterior);
	                state.storeToFile(sampleNr);
	                operatorSchedule.storeToFile();
	            }
	            
	            if (posterior.getCurrentLogP() == Double.POSITIVE_INFINITY) {
	            	throw new RuntimeException("Encountered a positive infinite posterior. This is a sign there may be numeric instability in the model.");
	            }

	        }
	        if (corrections > 0) {
	            System.err.println("\n\nNB: " + corrections + " posterior calculation corrections were required. This analysis may not be valid!\n\n");
	        }
	        currentSample += nSteps;
	        return System.currentTimeMillis();
	}

	
    protected boolean isTooDifferent(double logLikelihood, double originalLogP) {
    	return Math.abs(logLikelihood - originalLogP) > 1e-6;
	}


	public TreeStoreLogger getTreeStoreLoggers(int i) {
		
		int count = 0;
		for (Logger log : loggers) {
        	if (log instanceof TreeStoreLogger) {
        		if (count == i) return (TreeStoreLogger)log;
        		count ++;
        	}
        }
		
		return null;
	}


	public void finish() throws IOException {
		Log.info.println("MCMC chain " + this.getChainNr() + ":");
	    operatorSchedule.showOperatorRates(System.out);
	    Log.info.println();
	    
	    close();
	
	    Log.warning.println("End likelihood: " + oldLogLikelihood);
	    Log.info.println();
	    state.storeToFile(chainLength);
	    operatorSchedule.storeToFile();
	    
	}
	
	

}
