package beast.math.distributions;


import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.GammaDistributionImpl;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;

@Description("Gamma distribution in which the scale parameter is forced to equal the reciprocal of the shape parameter. " +
        "This fixes the mean at 1.0.")
public class SingleParamGamma extends ParametricDistribution {
    final public Input<RealParameter> alphaInput = new Input<>("alpha", "shape parameter, defaults to 2");

    org.apache.commons.math.distribution.GammaDistribution m_dist = new GammaDistributionImpl(1, 1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double alpha;
        if (alphaInput.get() == null) {
            alpha = 2;
        } else {
            alpha = alphaInput.get().getValue();
        }
        m_dist.setAlpha(alpha);
        m_dist.setBeta(1 / alpha);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return m_dist;
    }

    
    @Override
    protected double getMeanWithoutOffset() {
    	return m_dist.getAlpha() * m_dist.getBeta();
    }
} // class Gamma
