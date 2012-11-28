package beast.inference;

import java.util.List;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Evaluator;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.Operator;
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
            	operatorSchedule.storeToFile();
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
