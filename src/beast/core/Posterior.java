package beast.core;

import java.util.List;
import java.util.Random;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Distribution;
import beast.base.inference.State;

@Description("Posterior, consisting of a prior and a likehood")
public class Posterior extends Distribution {
	public Input<Prior> m_prior = new Input<Prior>("prior", "prior distribution over the state", Validate.REQUIRED);
	public Input<Likelihood> m_likelihood = new Input<Likelihood>("likelihood", "Likelihood distribution over the data", Validate.REQUIRED);
	
	@Override 
    public double calculateLogP() {
        logP = m_prior.get().calculateLogP();
        if (Double.isInfinite(logP)) {
        	return logP;
        }
        logP += m_likelihood.get().calculateLogP(); 
        return logP;
    }
	
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
	@Override public void sample(State state, Random random) {}
}
