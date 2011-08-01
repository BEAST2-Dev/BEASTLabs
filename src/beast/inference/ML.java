package beast.inference;

import beast.core.Description;
import beast.core.MCMC;
import beast.core.Operator;
import beast.util.Randomizer;

@Description("Maximum likelihood throught simple random hill climbing")
public class ML extends MCMC {
	
	@Override
    /** main MCMC loop **/ 
    protected void doLoop() throws Exception {
        String sBestXML = state.toXML();
        double fBestLogLikelihood = fOldLogLikelihood;
		
        for (int iSample = -nBurnIn; iSample <= nChainLength; iSample++) {
            state.store(iSample);

            Operator operator = operatorSet.selectOperator();
            //System.out.print("\n" + iSample + " " + operator.getName()+ ":");
            double fLogHastingsRatio = operator.proposal();
            if (fLogHastingsRatio != Double.NEGATIVE_INFINITY) {
            	state.storeCalculationNodes();
                state.checkCalculationNodesDirtiness();

                fNewLogLikelihood = posterior.calculateLogP();

                double logAlpha = fNewLogLikelihood - fOldLogLikelihood;
                //System.out.println(logAlpha + " " + fNewLogLikelihood + " " + fOldLogLikelihood);
                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
                //if (logAlpha >= 0) {
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
                if (iSample > 0) {
                    operator.reject();
                }
                state.restore();
                //System.out.print(" direct reject");
            }
            
            if (fOldLogLikelihood > fBestLogLikelihood) {
                sBestXML = state.toXML();
                fBestLogLikelihood = fOldLogLikelihood;
            }
            
            if (iSample % 1000 == 0) {
                String sXML = state.toXML();
            	state.fromXML(sBestXML);
            	fOldLogLikelihood = robustlyCalcPosterior(posterior); 
            	log(iSample);
//            	state.fromXML(sXML);
//                robustlyCalcPosterior(posterior); 
            }
            
            if (bDebug && iSample % 2 == 0 || iSample % 10000 == 0) { 
            	// check that the posterior is correctly calculated at every third
            	// sample, as long as we are in debug mode
                double fLogLikelihood = robustlyCalcPosterior(posterior); 
                if (Math.abs(fLogLikelihood - fOldLogLikelihood) > 1e-6) {
                	reportLogLikelihoods(posterior, "");
                    throw new Exception("At sample "+ iSample + "\nLikelihood incorrectly calculated: " + fOldLogLikelihood + " != " + fLogLikelihood
                    		+ " Operator: " + operator.getClass().getName());
                }
                if (iSample > NR_OF_DEBUG_SAMPLES * 3) {
                	// switch of debug mode once a sufficient large sample is checked
                    bDebug = false;
                }
            } else {
                operator.optimize(logAlpha);
            }
            callUserFunction(iSample);
        }
    }    

}
