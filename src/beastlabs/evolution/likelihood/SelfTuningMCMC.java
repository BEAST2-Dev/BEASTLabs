package beastlabs.evolution.likelihood;

import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.ProgramStatus;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;

@Description("MCMC that can tune the treelikelihood for efficiency")
public class SelfTuningMCMC extends MCMC {
	
    final public Input<Boolean> useThreadsInput = new Input<>("useThreads", "calculated the distributions in parallel using threads (default true)-- only used if no SelfTuningCompoundDistribution specified.", true);
    final public Input<Integer> minNrOfThreadsInput = new Input<>("minThreads","minimum number of threads to use (default 1)-- only used if no SelfTuningCompoundDistribution specified.", 1);
    final public Input<Integer> maxNrOfThreadsInput = new Input<>("maxThreads","maximum number of threads to use, if less than 1 the number of threads in BeastMCMC is used (default -1)-- only used if no SelfTuningCompoundDistribution specified.", -1);

    final public Input<Long> swithcCountInput = new Input<>("switchCount", "number of milli seconds to calculate likelihood before switching configuration-- only used if no SelfTuningCompoundDistribution specified.", 500l);
    final public Input<Long> reconfigCountInput = new Input<>("reconfigCount", "number of times to calculate likelihood before self tuning again-- only used if no SelfTuningCompoundDistribution specified.", 100000l);
    final public Input<Integer> stopAfterSamerResultsInput = new Input<>("stopAfterSamerResults", "number of times the same configuration is optimal in a row before stopping to tune-- only used if no SelfTuningCompoundDistribution specified.", 3);
    
    final public Input<Boolean> includeMPTLInput = new Input<>("includeMPTL", "include multi-partition (BEAGLE 3) tree likelihood in configurations-- only used if no SelfTuningCompoundDistribution specified.", true);
    final public Input<Boolean> includeSPTLInput = new Input<>("includeSPTL", "include single-partition (BEAGLE 2) tree likelihood in configurations-- only used if no SelfTuningCompoundDistribution specified.", true);

	
	SelfTuningCompoundDistribution stCompoundDistribution;
	
	@Override
	public void initAndValidate() {
		
		if (!ProgramStatus.name.equals("BEAUti")) {
			stCompoundDistribution = findSTCompoundDistribution(posteriorInput.get());
			if (stCompoundDistribution == null) {
				// no SelfTuningCompoundDistribution found, so replace CompoundDistribution with id="likelihood"
				// with one
				stCompoundDistribution = createdSTCompoundDistribution(posteriorInput.get());
			}
			
			for (Logger logger: loggersInput.get()) {
				logger.initAndValidate();
			}
		}

		super.initAndValidate();
	}

	private SelfTuningCompoundDistribution createdSTCompoundDistribution(BEASTInterface o) {
		if (o instanceof CompoundDistribution && o.getID().equals("likelihood")) {
			CompoundDistribution cd = (CompoundDistribution) o;
			// replace cd with SelfTuningCompoundDistribution
			SelfTuningCompoundDistribution stcd = new SelfTuningCompoundDistribution();
			stcd.initByName("distribution", cd.pDistributions.get(), 
					"ignore", cd.ignoreInput.get(),
					"useThreads", useThreadsInput.get(),
					"minThreads",minNrOfThreadsInput.get(),
					"maxThreads",maxNrOfThreadsInput.get(),
					"switchCount",swithcCountInput.get(),
					"reconfigCount",reconfigCountInput.get(),
					"stopAfterSamerResults",stopAfterSamerResultsInput.get(),
					"includeMPTL",includeMPTLInput.get(),
					"includeSPTL",includeSPTLInput.get()
					);
			for (BEASTInterface out : cd.getOutputs()) {
				for (Input<?> in : out.listInputs()) {
					if (in.get() instanceof List) {
						List list = (List) in.get();
						int i = list.indexOf(cd);
						if (i >= 0) {
							list.set(i, stcd);
							stcd.getOutputs().add(out);
						}
					} else {
						if (in.get() == cd) {
							in.set(stcd);
							stcd.getOutputs().add(out);
						}
					}
				}
			}
			stcd.setID("likelihood");
			return stcd;
		}
		for (BEASTInterface o2 : o.listActiveBEASTObjects()) {
			Object o3 = createdSTCompoundDistribution(o2);
			if (o3 != null) {
				return (SelfTuningCompoundDistribution) o3;
			}
		}
		return null;
	}

	private SelfTuningCompoundDistribution findSTCompoundDistribution(BEASTInterface o) {
		if (o instanceof SelfTuningCompoundDistribution) {
			return (SelfTuningCompoundDistribution) o;
		}
		for (BEASTInterface o2 : o.listActiveBEASTObjects()) {
			Object o3 = findSTCompoundDistribution(o2);
			if (o3 != null) {
				return (SelfTuningCompoundDistribution) o3;
			}
		}
		return null;
	}

	
	@Override
	protected void callUserFunction(final long sample) { 		
		if (stCompoundDistribution.update(sample)) {
			robustlyCalcPosterior(posterior);
		}
	}

}
