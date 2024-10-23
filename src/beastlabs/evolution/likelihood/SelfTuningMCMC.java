package beastlabs.evolution.likelihood;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.inference.MCMC;

@Description("MCMC that can tune the treelikelihood for efficiency")
public class SelfTuningMCMC extends MCMC {
	
	SelfTuningCompoundDistribution stCompoundDistribution;
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();
		
		stCompoundDistribution = findSTCompoundDistribution(posteriorInput.get());	
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
