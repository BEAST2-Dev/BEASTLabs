package beastlabs.tools;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beastfx.app.tools.Application;

@Description("Useful for logging information based on a previous MCMC run. "
		+ "This class attempts to restore the state from the trace and tree logs, "
		+ "and can be used to calculate custom statistics on the MCMC run.")
public class PostHocAnalyser extends MCMC {
	final public Input<List<StateNodeSource>> stateNodeSourceInput = new Input<>("source", "each state node source entry defines how trace and trees are mapped to state nodes", new ArrayList<>());;
	

	@Override
	public void initAndValidate() {
		super.initAndValidate();
		
		posterior = posteriorInput.get();
	}

		
	@Override
	public void run() throws IOException {
        loggers = loggersInput.get();
        for (final Logger log : loggers) {
        	if (log.everyInput.get()> 1) {
        		Log.warning("WARNING: logEvery on logger " + log.getID() + " is larger than 1, so not all trace entries will be logged");
        	}
            log.init();
        }

        TraceStateNodeSource trace = null;
		for (StateNodeSource entry : stateNodeSourceInput.get()) {
			if (entry instanceof TraceStateNodeSource) {
				trace = (TraceStateNodeSource) entry;
				break;
			}
		}
		if (trace == null) {
			throw new IllegalArgumentException("expected at least one trace file as map entry");
		}
		
		int n = trace.tracelog.getTraces()[0].length;
		long delta = (long)(trace.tracelog.getTraces()[0][1] - trace.tracelog.getTraces()[0][0]);
		
		
		for (int i = 0; i < n; i++) {
			for (StateNodeSource entry : stateNodeSourceInput.get()) {
				entry.initStateNodes(i);
			}
			
			double logP = state.robustlyCalcPosterior(posterior);

			// sanity check
			if (Math.abs(logP - trace.tracelog.getTrace("posterior")[i]) > 1e-4) {
				Log.warning("Substantial difference between calculated logP " + logP + " and logged logP: " + trace.tracelog.getTrace("posterior")[i]);
				if (Double.isInfinite(logP)) {
					reportLogLikelihoods(posterior, "");
				}
			}
			
			for (Logger logger : loggers) {
				logger.log((long) i * delta);
			}
		}
		
		for (final Logger log : loggers) {
			log.close();
		}
	}


	public static void main(String[] args) throws Exception {
		new Application(new PostHocAnalyser(), "PostHocAnalyser", args);
	}

}
