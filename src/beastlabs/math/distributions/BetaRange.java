package beastlabs.math.distributions;

import java.util.List;
import java.util.Random;

import org.apache.commons.math.distribution.BetaDistributionImpl;

import beast.base.core.Description;
import beast.base.inference.Distribution;
import beast.base.core.Input;
import beast.base.inference.State;
import beast.base.inference.parameter.RealParameter;

@Description("Expansion of the Beta distribution, except it respects the upper/lower limits of the parameter " +
 " Can handle a different prior per parameter if the dimension of 'x' is equal to the dimension of the gamma parameters " +
 " If alpha / beta are set to 0, then the parameter will be skipped (ie. 0 log density) " +
 " If the parameter exceeds lower/upper, the log density is negative infinity")
public class BetaRange extends Distribution  {
	
	final public Input<RealParameter> parameterInput = new Input<>("x", "the parameter at which the density is calculated", Input.Validate.REQUIRED);
	final public Input<RealParameter> alphaInput = new Input<>("alpha", "first shape parameter (default 1)");
	final public Input<RealParameter> betaInput = new Input<>("beta", "the other shape parameter (default 1)");
    final public Input<RealParameter> lowerInput = new Input<>("lower", "lower limit of the parameter (default 0)");
    final public Input<RealParameter> upperInput = new Input<>("upper", "upper limit of the parameter (default 1)");
	
	
	
    org.apache.commons.math.distribution.BetaDistribution m_dist = new BetaDistributionImpl(1, 1);
    
	@Override
    public void initAndValidate() {
		
		System.out.println("BETARANGE");
  
    }
	
	

	
	@SuppressWarnings("deprecation")
	@Override
	public double calculateLogP() {
		
		System.out.println("BETARANGE");
		
		logP = 0;
		
		RealParameter param = parameterInput.get();
		for (int i = 0; i < param.getDimension(); i ++) {
			
			System.out.println("BETARANGE " + i + ": " + logP);
			
			double val = param.getValue(i);
			double alpha = getValOrDefault(alphaInput.get(), i, 1);
			double beta = getValOrDefault(betaInput.get(), i, 1);
			double lower = getValOrDefault(lowerInput.get(), i, 0);
			double upper = getValOrDefault(upperInput.get(), i, 1);
			
			// Range check
			if (alpha <= 0 || beta <= 0) continue;
			if (val <= lower || val >= upper) {
				logP = Double.NEGATIVE_INFINITY;
				return logP;
			}
			
			// Standardise into (0,1)
			double tval = (val - lower) / (upper - lower);
			
			// Get the density of the standardised vairable
			m_dist.setAlpha(alpha);
		    m_dist.setBeta(beta);
			logP += m_dist.logDensity(tval);
			
		}
		

		
		return logP;
	}
	
	
	/**
	 * Gets the value of this parameter at this index, or returns the default
	 * @param param
	 * @param index
	 * @param defaultVal
	 * @return
	 */
	private double getValOrDefault(RealParameter param, int index, double defaultVal) {
		
		// If the parameter is null, use the default value
		if (param == null) return defaultVal;
		
		// If the parameter has 1 dimension get the 1st value
		if (param.getDimension() == 1) return param.getValue();
		
		// Multiple dimensions -> return the value at 'index'
		return param.getValue(index);
		
	}
	


	@Override
	public List<String> getArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getConditions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sample(State arg0, Random arg1) {
		// TODO Auto-generated method stub
		
	}

	
	

}
