package beastlabs.evolution.likelihood;

import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.MCMC;

@Description("MCMC that can tune the treelikelihood for efficiency")
public class SelfTuningMCMC extends MCMC {
	
	SelfTuningCompoundDistribution stCompoundDistribution;
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();
		
		stCompoundDistribution = findSTCompoundDistribution(posteriorInput.get());
		if (stCompoundDistribution == null) {
			// no SelfTuningCompoundDistribution found, so replace CompoundDistribution with id="likelihood"
			// with one
			stCompoundDistribution = createdSTCompoundDistribution(posteriorInput.get());
		}
	}

	private SelfTuningCompoundDistribution createdSTCompoundDistribution(BEASTInterface o) {
		if (o instanceof CompoundDistribution && o.getID().equals("likelihood")) {
			CompoundDistribution cd = (CompoundDistribution) o;
			// replace cd with SelfTuningCompoundDistribution
			SelfTuningCompoundDistribution stcd = new SelfTuningCompoundDistribution();
			stcd.initByName("distribution", cd.pDistributions.get(), "ignore", cd.ignoreInput.get());
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
