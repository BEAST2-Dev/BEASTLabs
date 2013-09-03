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
        String sBestXML = state.toXML(0);
        double fBestLogLikelihood = oldLogLikelihood;
		
        for (int iSample = -burnIn; iSample <= chainLength; iSample++) {
            state.store(iSample);
            if (storeEvery > 0 && iSample % storeEvery == 0 && iSample > 0) {
                state.storeToFile(iSample);
            	operatorSchedule.storeToFile();
            }

            Operator operator = operatorSchedule.selectOperator();
            //System.out.print("\n" + iSample + " " + operator.getName()+ ":");
            double fLogHastingsRatio = operator.proposal();
            if (fLogHastingsRatio != Double.NEGATIVE_INFINITY) {
            	state.storeCalculationNodes();
                state.checkCalculationNodesDirtiness();

                newLogLikelihood = posterior.calculateLogP();

                double logAlpha = newLogLikelihood - oldLogLikelihood;
                //System.out.println(logAlpha + " " + fNewLogLikelihood + " " + fOldLogLikelihood);
                if (logAlpha >= 0 || Randomizer.nextDouble() < Math.exp(logAlpha)) {
                //if (logAlpha >= 0) {
                    // accept
                    oldLogLikelihood = newLogLikelihood;
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
            
            if (oldLogLikelihood > fBestLogLikelihood) {
                sBestXML = state.toXML(iSample);
                fBestLogLikelihood = oldLogLikelihood;
            }
            
            if (iSample % 1000 == 0) {
                String sXML = state.toXML(iSample);
            	state.fromXML(sBestXML);
            	oldLogLikelihood = robustlyCalcPosterior(posterior); 
            	log(iSample);
//            	state.fromXML(sXML);
//                robustlyCalcPosterior(posterior); 
            }
            
            if (debugFlag && iSample % 2 == 0 || iSample % 10000 == 0) { 
            	// check that the posterior is correctly calculated at every third
            	// sample, as long as we are in debug mode
                double fLogLikelihood = robustlyCalcPosterior(posterior); 
                if (Math.abs(fLogLikelihood - oldLogLikelihood) > 1e-6) {
                	reportLogLikelihoods(posterior, "");
                    throw new Exception("At sample "+ iSample + "\nLikelihood incorrectly calculated: " + oldLogLikelihood + " != " + fLogLikelihood
                    		+ " Operator: " + operator.getClass().getName());
                }
                if (iSample > NR_OF_DEBUG_SAMPLES * 3) {
                	// switch of debug mode once a sufficient large sample is checked
                    debugFlag = false;
                }
            } else {
                operator.optimize(logAlpha);
            }
            callUserFunction(iSample);
        }
    }    

}
