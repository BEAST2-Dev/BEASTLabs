package beast.inference;

import java.util.List;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Evaluator;
import beast.core.Input;
import beast.core.Logger;
import beast.core.MCMC;
import beast.core.Operator;
import beast.core.StateNodeInitialiser;
import beast.core.util.CompoundDistribution;
import beast.util.Randomizer;

@Description("Calculate marginal likelihood through path sampling for a single step")
public class PathSamplingStep extends MCMC {

	public Input<Double> betaInput = new Input<Double>("beta","power used for likelihood: 1 = using full posterior, 0 = using prior only", 1.0);

	double beta;
	Distribution prior;
	Distribution likelihood;
	
	@Override
	public void initAndValidate() throws Exception {
		super.initAndValidate();
		
		beta = betaInput.get();
		posterior = posteriorInput.get();
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
			prior = list.get(1);
			likelihood = list.get(0);
		} else {
			if (list.get(0).getID().toLowerCase().startsWith("prior")) {
				prior = list.get(0);
				likelihood = list.get(1);
			} else {
				throw new Exception("Expected posterior with only likelihood and prior as IDs");
			}
		}
	}
	
	
    @Override
    public void run() throws Exception {
        // set up state (again). Other plugins may have manipulated the
        // StateNodes, e.g. set up bounds or dimensions
        state.initAndValidate();
        // also, initialise state with the file name to store and set-up whether to resume from file
        state.setStateFileName(m_sStateFile);
        operatorSchedule.setStateFileName(m_sStateFile);

        nBurnIn = m_oBurnIn.get();
        nChainLength = m_oChainLength.get();
        int nInitiliasiationAttemps = 0;
        state.setEverythingDirty(true);
        posterior = posteriorInput.get();

        if (m_bRestoreFromFile) {
            state.restoreFromFile();
            operatorSchedule.restoreFromFile();
            nBurnIn = 0;
            fOldLogLikelihood = robustlyCalcPosterior(posterior);
        } else {
            do {
                for (StateNodeInitialiser initialiser : m_initilisers.get()) {
                    initialiser.initStateNodes();
                }
                fOldLogLikelihood = robustlyCalcPosterior(posterior);
            } while (Double.isInfinite(fOldLogLikelihood) && nInitiliasiationAttemps++ < 10);
        }
        long tStart = System.currentTimeMillis();

        // do the sampling
        logAlpha = 0;
        bDebug = Boolean.valueOf(System.getProperty("beast.debug"));


//        System.err.println("Start state:");
//        System.err.println(state.toString());

        System.err.println("Start likelihood: " + fOldLogLikelihood + " " + (nInitiliasiationAttemps > 1 ? "after " + nInitiliasiationAttemps + " initialisation attempts" : ""));
        if (Double.isInfinite(fOldLogLikelihood) || Double.isNaN(fOldLogLikelihood)) {
            reportLogLikelihoods(posterior, "");
            throw new Exception("Could not find a proper state to initialise. Perhaps try another seed.");
        }

        // initialises log so that log file headers are written, etc.
        for (Logger log : m_loggers.get()) {
            log.init();
        }

        doLoop();

        operatorSchedule.showOperatorRates(System.out);
        long tEnd = System.currentTimeMillis();
        System.out.println("Total calculation time: " + (tEnd - tStart) / 1000.0 + " seconds");
        close();

        System.err.println("End likelihood: " + fOldLogLikelihood);
//        System.err.println(state);
        state.storeToFile(nChainLength);
        // Do not store operator optimisation information
        // since this may not be valid for the next step
        // especially when sampling from the prior only
//        operatorSchedule.storeToFile();
    } // run;
	
	
	
    /**
     * main MCMC loop *
     */
    protected void doLoop() throws Exception {
    	
        double logPriorProb = prior.calculateLogP();
        double logLikelihood = likelihood.calculateLogP();        
        fOldLogLikelihood = logPriorProb + logLikelihood * beta; 
    	
    	
        for (int iSample = -nBurnIn; iSample <= nChainLength; iSample++) {
            final int currentState = iSample;

            state.store(currentState);
            if (m_nStoreEvery > 0 && iSample % m_nStoreEvery == 0 && iSample > 0) {
                state.storeToFile(iSample);
                // Do not store operator optimisation information
                // since this may not be valid for the next step
                // especially when sampling from the prior only
            	//operatorSchedule.storeToFile();
            }

            Operator operator = operatorSchedule.selectOperator();
            //System.out.print("\n" + iSample + " " + operator.getName()+ ":");

            final Distribution evaluatorDistribution = operator.getEvaluatorDistribution();
            Evaluator evaluator = null;

            if (evaluatorDistribution != null) {
                evaluator = new Evaluator() {
                    @Override
                    public double evaluate() {
                        double logP = 0.0;

                        state.storeCalculationNodes();
                        state.checkCalculationNodesDirtiness();

                        try {
                            logP = evaluatorDistribution.calculateLogP();
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }

                        state.restore();
                        state.store(currentState);

                        return logP;
                    }
                };
            }

            double fLogHastingsRatio = operator.proposal(evaluator);

            if (fLogHastingsRatio != Double.NEGATIVE_INFINITY) {

                state.storeCalculationNodes();
                state.checkCalculationNodesDirtiness();

                
                posterior.calculateLogP();
                logPriorProb = prior.getArrayValue();
                logLikelihood = likelihood.getArrayValue();
                
                fNewLogLikelihood = logPriorProb + logLikelihood * beta; 

                logAlpha = fNewLogLikelihood - fOldLogLikelihood + fLogHastingsRatio; //CHECK HASTINGS
                //System.out.println(logAlpha + " " + fNewLogLikelihood + " " + fOldLogLikelihood);
                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
                    // accept
                    fOldLogLikelihood = fNewLogLikelihood;
                    state.acceptCalculationNodes();

                    if (iSample >= 0) {
                        operator.accept();
                    }
                    //System.out.print(" accept");
                } else {
                    // reject
                    if (iSample >= 0) {
                        operator.reject();
                    }
                    state.restore();
                    state.restoreCalculationNodes();
                    //System.out.print(" reject");
                }
                state.setEverythingDirty(false);
            } else {
                // operation failed
                if (iSample >= 0) {
                    operator.reject();
                }
                state.restore();
                //System.out.print(" direct reject");
            }
            log(iSample);

            operator.optimize(logAlpha);
            callUserFunction(iSample);
        }
    }
}
