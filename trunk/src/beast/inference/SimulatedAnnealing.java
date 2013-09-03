package beast.inference;

import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Logger;
import beast.core.MCMC;
import beast.core.Operator;
import beast.util.Randomizer;



@Description("Maximum likelihood by simulated annealing")
public class SimulatedAnnealing extends MCMC implements Loggable {
	public Input<Double> startTemp = new Input<Double>("startTemp","starting temperature (default 1.0)", 1.0);
	public Input<Double> endTemp = new Input<Double>("endTemp","end temperature. Together with startTemp this " +
			"determines the temperature trajectory (default 1e-4)", 1e-4);
	
	double m_fDeltaLogTemp;
	
	@Override
	public void initAndValidate() throws Exception {
		super.initAndValidate();
		m_fDeltaLogTemp = Math.log(endTemp.get()) - Math.log(startTemp.get());
	}
	
	
	double fTemp;
	
	@Override
    /** main MCMC loop **/ 
    protected void doLoop() throws Exception {
        String sBestXML = state.toXML(0);
        double fBestLogLikelihood = oldLogLikelihood;
		double fTemp0 = startTemp.get();
		fTemp = fTemp0;
		
		// find lowest log frequency
		int nLogEvery = chainLength;
		for (Logger logger :  loggersInput.get()) {
			nLogEvery = Math.min(logger.everyInput.get(), nLogEvery);
		}
			
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
                if (logAlpha >= 0 || Randomizer.nextDouble() > Math.exp(-Math.exp(logAlpha) * fTemp)) {
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
            
            
            
            if (iSample % nLogEvery == 0) {
                String sXML = state.toXML(iSample);
            	state.fromXML(sBestXML);
            	oldLogLikelihood = robustlyCalcPosterior(posterior); 
            	log(iSample);
            	state.fromXML(sXML);
            	oldLogLikelihood = robustlyCalcPosterior(posterior); 
            }

            
            
            if (debugFlag && iSample % 3 == 0 || iSample % 10000 == 0) { 
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
            
            fTemp = fTemp0 * Math.exp(iSample * m_fDeltaLogTemp / chainLength);
        }
    }


	@Override
	public void init(PrintStream out) throws Exception {
		out.append("temperature\t");
	}


	@Override
	public void log(int nSample, PrintStream out) {
		out.append(fTemp + "\t");
	}


	@Override
	public void close(PrintStream out) {
	}    

}
