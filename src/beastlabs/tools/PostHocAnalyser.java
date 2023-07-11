package beastlabs.tools;


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
	}

		
	@Override
	public void run() {
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
		
		for (int i = 0; i < n; i++) {
			for (StateNodeSource entry : stateNodeSourceInput.get()) {
				entry.initStateNodes(i);
			}
			
			double logP = posterior.calculateLogP();

			// sanity check
			if (Math.abs(logP - trace.tracelog.getTrace("posterior")[i]) > 1e-4) {
				Log.warning("Substantial difference between calculated logP " + logP + "and logged logP: " + trace.tracelog.getTrace("posterior")[i]);
			}
			
			for (Logger logger : loggers) {
				logger.log(i);
			}
		}
		
	}


	public static void main(String[] args) throws Exception {
		new Application(new PostHocAnalyser(), "PostHocAnalyser", args);
	}

}
