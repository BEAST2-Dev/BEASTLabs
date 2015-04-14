package beast.inference;


import beast.core.Description;
import beast.core.Distribution;
import beast.core.MCMC;
import beast.core.Operator;
import beast.core.util.Evaluator;
import beast.util.Randomizer;

@Description("Base class for doing Metropolis coupled MCMC. Each instance represenst a chain at a different temperature.")
public class HeatedMCMC extends MCMC {
	
	// LAMBDA is temperature multiplier
	final static double LAMBDA = 1.0;
	
	// temperature on which this chain runs
	protected double temperature = 1.0;
	
	// nr of samples between re-arranging states
	protected int resampleEvery = 1000;
	
	// keep track of total nr of states sampled, using currentSample
	protected int currentSample = 0;

	protected double getCurrentLogLikelihood() {
		return oldLogLikelihood / temperature;
	};

	protected double geCurrentLogLikelihoodRobustly() throws Exception {
		oldLogLikelihood = robustlyCalcPosterior(posterior);
		return getCurrentLogLikelihood();
	};
	
	public void setChainNr(int i, int resampleEvery) throws Exception {
		temperature = 1 + i * LAMBDA;
		this.resampleEvery = resampleEvery;
	}

	@Override
	protected void doLoop() throws Exception {
		runTillResample();
	}
	
	// run MCMC inner loop for resampleEvery nr of samples
	protected void runTillResample() throws Exception {
	       int corrections = 0;
	       for (int sampleNr = currentSample; sampleNr <= currentSample + resampleEvery; sampleNr++) {
	            final int currentState = sampleNr;

	            state.store(currentState);
//	            if (m_nStoreEvery > 0 && iSample % m_nStoreEvery == 0 && iSample > 0) {
//	                state.storeToFile(iSample);
//	            	operatorSchedule.storeToFile();
//	            }

	            final Operator operator = operatorSchedule.selectOperator();
	            //System.out.print("\n" + sampleNr + " " + operator.getName()+ ":");

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

	            final double logHastingsRatio = operator.proposal(evaluator);

	            if (logHastingsRatio != Double.NEGATIVE_INFINITY) {

	            	if (operator.requiresStateInitialisation()) {
	            		state.storeCalculationNodes();
	            		state.checkCalculationNodesDirtiness();
	            	}

	                newLogLikelihood = posterior.calculateLogP();

	                logAlpha = newLogLikelihood/temperature - oldLogLikelihood/temperature + logHastingsRatio; 
	                //System.out.println(logAlpha + " " + newLogLikelihood + " " + oldLogLikelihood);
	                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
	                    // accept
	                    oldLogLikelihood = newLogLikelihood;
	                    state.acceptCalculationNodes();

	                    if (sampleNr >= 0) {
	                        operator.accept();
	                    }
	                    //System.out.print(" accept");
	                } else {
	                    // reject
	                    if (sampleNr >= 0) {
	                        operator.reject(newLogLikelihood == Double.NEGATIVE_INFINITY ? -1 : 0);
	                    }
	                    state.restore();
	                    state.restoreCalculationNodes();
	                    //System.out.print(" reject");
	                }
	                state.setEverythingDirty(false);
	            } else {
	                // operation failed
	                if (sampleNr >= 0) {
	                    operator.reject(-2);
	                }
	                state.restore();
					if (!operator.requiresStateInitialisation()) {
	                    state.setEverythingDirty(false);
	                    state.restoreCalculationNodes();
					}
	                //System.out.print(" direct reject");
	            }
	            log(sampleNr);

	            if (debugFlag && sampleNr % 3 == 0 || sampleNr % 10000 == 0) {
	                // check that the posterior is correctly calculated at every third
	                // sample, as long as we are in debug mode
	            	final double fNonStochasticLogP = posterior.getNonStochasticLogP();
	                final double fLogLikelihood = state.robustlyCalcNonStochasticPosterior(posterior);
	                if (Math.abs(fLogLikelihood - fNonStochasticLogP) > 1e-6) {
	                    reportLogLikelihoods(posterior, "");
	                    System.err.println("At sample " + sampleNr + "\nLikelihood incorrectly calculated: " + fNonStochasticLogP + " != " + fLogLikelihood
	                            + " Operator: " + operator.getClass().getName());
	                }
	                if (sampleNr > NR_OF_DEBUG_SAMPLES * 3) {
	                    // switch off debug mode once a sufficient large sample is checked
	                    debugFlag = false;
	                    if (Math.abs(fLogLikelihood - fNonStochasticLogP) > 1e-6) {
	                        // incorrect calculation outside debug period.
	                        // This happens infrequently enough that it should repair itself after a robust posterior calculation
	                        corrections++;
	                        if (corrections > 100) {
	                            // after 100 repairs, there must be something seriously wrong with the implementation
	                            System.err.println("Too many corrections. There is something seriously wrong that cannot be corrected");
	                            state.storeToFile(sampleNr);
	                            operatorSchedule.storeToFile();
	                            System.exit(0);
	                        }
	                        oldLogLikelihood = state.robustlyCalcPosterior(posterior);;
	                    }
	                } else {
	                    if (Math.abs(fLogLikelihood - fNonStochasticLogP) > 1e-6) {
	                        // halt due to incorrect posterior during intial debug period
	                        state.storeToFile(sampleNr);
	                        operatorSchedule.storeToFile();
	                        System.exit(0);
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
	                /*final double fLogLikelihood = */
	                state.robustlyCalcNonStochasticPosterior(posterior);
	                state.storeToFile(sampleNr);
	                operatorSchedule.storeToFile();
	            }
	        }
	        if (corrections > 0) {
	            System.err.println("\n\nNB: " + corrections + " posterior calculation corrections were required. This analysis may not be valid!\n\n");
	        }
	        currentSample += resampleEvery;
	}

	public void reset() throws Exception {
		oldLogLikelihood = state.robustlyCalcNonStochasticPosterior(posterior);		
	}
	
}
