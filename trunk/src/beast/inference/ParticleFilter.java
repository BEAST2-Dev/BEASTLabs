package beast.inference;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Logger;
import beast.core.MCMC;
import beast.core.Operator;
import beast.util.Randomizer;

@Description("MCMC Inference by particle filter approach. This works only when run with many threads, one per particle is optimal.")
public class ParticleFilter extends MCMC {

	public Input<Integer> m_nParticles = new Input<Integer>("nrofparticles", "the number of particles to use, default 100", 100);
	public Input<Integer> m_nStepSize = new Input<Integer>("stepsize", "number of steps after which a new particle set is determined during burn in, default 100", 100);

    @Override
    public void run() throws Exception {
        // initialises log so that log file headers are written, etc.
        for (Logger log : m_loggers.get()) {
            log.init();
        }
    	// set up state (again). Other plugins may have manipulated the
    	// StateNodes, e.g. set up bounds or dimensions
    	state.initAndValidate();
    	// also, initialise state with the file name to store and set-up whether to resume from file
    	state.setStateFileName(m_sStateFile);
        int nBurnIn = m_oBurnIn.get();
        int nChainLength = m_oChainLength.get();
        if (m_bRestoreFromFile) {
        	state.restoreFromFile();
        	nBurnIn = 0;
        }
        long tStart = System.currentTimeMillis();

        System.err.println("Start state:");
        System.err.println(state.toString());

        state.setEverythingDirty(true);
        Distribution posterior = posteriorInput.get();

        // do the sampling
        double logAlpha = 0;

        boolean bDebug = true;
        state.setEverythingDirty(true);
        state.checkCalculationNodesDirtiness();
        double fOldLogLikelihood = robustlyCalcPosterior(posterior); 
        System.err.println("Start likelihood: " + fOldLogLikelihood);
        
        
        double fBestLogLikelihood = fOldLogLikelihood;
        String sBestXML = state.toXML();
        String sStartXML = state.toXML();
        
        int nParticles = m_nParticles.get();
        int nStepSize = m_nStepSize.get();

        // main MCMC loop 
        for (int iSample = 0; iSample <= nBurnIn + nChainLength; iSample++) {
            state.store(iSample);

            Operator operator = operatorSet.selectOperator();
            //System.out.print("\n" + iSample + " " + operator.getName()+ ":");
            double fLogHastingsRatio = operator.proposal();
            if (fLogHastingsRatio != Double.NEGATIVE_INFINITY) {
            	state.storeCalculationNodes();
                state.checkCalculationNodesDirtiness();

                double fNewLogLikelihood = posterior.calculateLogP();

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
                if (iSample > 0) {
                    operator.reject();
                }
                state.restore();
                //System.out.print(" direct reject");
            }
            log(iSample);
            
            if (iSample < nBurnIn) {
            	if (iSample % nStepSize == 0) {
	                // during burn-in
	            	if (fOldLogLikelihood > fBestLogLikelihood) {
		            	sBestXML = state.toXML();
		            	fBestLogLikelihood = fOldLogLikelihood;
	                }
	            	// go to best state, after all particles have reported
	            	if (iSample % (nStepSize * nParticles) == 0) {
	            		sStartXML = sBestXML;
	            	}
	            	state.fromXML(sStartXML);
            	}
            } else if (iSample == nBurnIn) {
            	// switch to reporting chain each of the particles
            	nStepSize = nChainLength/nParticles;
            } else {
            	if (iSample % nStepSize == 0) {
                	// go to next particle
	            	state.fromXML(sStartXML);
            	}            	
            }
            

//            if (iSample % 10000 == 0) {
//                state.store(-1);
//                state.setEverythingDirty(true);
//                state.checkCalculationNodesDirtiness();
//                posterior.calculateLogP();
//            }
            
            if (bDebug && iSample % 3 == 0) { // || iSample % 10000 == 0) {
            	//System.out.print("*");
            	// check that the posterior is correctly calculated
                state.store(-1);
                state.setEverythingDirty(true);
                state.checkCalculationNodesDirtiness();

                double fLogLikelihood = posterior.calculateLogP();

                if (Math.abs(fLogLikelihood - fOldLogLikelihood) > 1e-6) {
                    throw new Exception("At sample "+ iSample + "\nLikelihood incorrectly calculated: " + fOldLogLikelihood + " != " + fLogLikelihood);
                }
                if (iSample > NR_OF_DEBUG_SAMPLES * 3) {
                    bDebug = false;
                }
                state.setEverythingDirty(false);
            } else {
                operator.optimize(logAlpha);
            }
        }
        operatorSet.showOperatorRates(System.out);
        long tEnd = System.currentTimeMillis();
        System.out.println("Total calculation time: " + (tEnd - tStart) / 1000.0 + " seconds");
        close();

        System.err.println("End likelihood: " + fOldLogLikelihood);
        System.err.println(state);
        state.storeToFile();
    } // run;	
	
}
