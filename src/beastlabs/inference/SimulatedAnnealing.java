package beastlabs.inference;

import java.io.IOException;
import java.io.PrintStream;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import beast.base.core.Log;
import beast.base.util.Randomizer;



@Description("Maximum likelihood by simulated annealing")
public class SimulatedAnnealing extends MCMC implements Loggable {
	public Input<Double> startTemp = new Input<Double>("startTemp","starting temperature (default 1.0)", 1.0);
	public Input<Double> endTemp = new Input<Double>("endTemp","end temperature. Together with startTemp this " +
			"determines the temperature trajectory (default 1e-4)", 1e-4);
	
	protected double m_fDeltaLogTemp;
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();
		m_fDeltaLogTemp = Math.log(endTemp.get()) - Math.log(startTemp.get());
        posterior = posteriorInput.get();
        if (posterior == null) {
        	Log.warning("POSTERIOR=NULL");
        }
        chainLength = chainLengthInput.get();
        loggers = loggersInput.get();
	}
	
	
	protected double fTemp;
	
	@Override
    /** main MCMC loop **/ 
    protected void doLoop() throws IOException {
        if (posterior == null) {
        	Log.warning("POSTERIOR=NULL");
            posterior = posteriorInput.get();
        	Log.warning(posterior + "");
        }
        String sBestXML = state.toXML(0);
        double fBestLogLikelihood = oldLogLikelihood;
		double fTemp0 = startTemp.get();
		fTemp = fTemp0;
		
		// find lowest log frequency
		long nLogEvery = chainLength;
		for (Logger logger :  loggersInput.get()) {
			nLogEvery = Math.min(logger.everyInput.get(), nLogEvery);
		}
		
//    	reportLogLikelihoods(posterior, "");

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
                if (logAlpha >= 0 || Randomizer.nextDouble() > Math.exp(-Math.exp(logAlpha + fLogHastingsRatio) * fTemp)) {
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
                    throw new RuntimeException("At sample "+ iSample + "\nLikelihood incorrectly calculated: " + oldLogLikelihood + " != " + fLogLikelihood
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
        
    	state.fromXML(sBestXML);
    	oldLogLikelihood = robustlyCalcPosterior(posterior);
    	state.storeToFile(chainLength);
//    	reportLogLikelihoods(posterior, "");
//    	Log.warning.println("logP = " + oldLogLikelihood + "\n" + sBestXML);
		for (StateNode sn : state.stateNodeInput.get()) {
			if (sn instanceof TreeInterface) {
				Log.debug(sn.getID() + ": " + ((TreeInterface) sn).getRoot().toNewick());
			} else {
				Log.debug(sn.getID() + ": " + sn.toString());
			}
		}
    }


	@Override
	public void init(PrintStream out) {
		out.append("temperature\t");
	}


	@Override
	public void log(long nSample, PrintStream out) {
		out.append(fTemp + "\t");
	}


	@Override
	public void close(PrintStream out) {
	}    

}
