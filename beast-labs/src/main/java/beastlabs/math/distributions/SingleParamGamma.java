package beastlabs.math.distributions;


import org.apache.commons.statistics.distribution.GammaDistribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.RealParameter;

@Description("Gamma distribution in which the scale parameter is forced to equal the reciprocal of the shape parameter. " +
        "This fixes the mean at 1.0.")
public class SingleParamGamma extends ParametricDistribution {
    final public Input<RealParameter> alphaInput = new Input<>("alpha", "shape parameter, defaults to 2");

    GammaDistribution m_dist = GammaDistribution.of(1, 1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double alpha;
        if (alphaInput.get() == null) {
            alpha = 2;
        } else {
            alpha = alphaInput.get().getValue();
        }
        m_dist = GammaDistribution.of(alpha, 1.0 / alpha);
    }

    @Override
    public GammaDistribution getDistribution() {
        refresh();
        return m_dist;
    }


    @Override
    protected double getMeanWithoutOffset() {
    	return m_dist.getShape() * m_dist.getScale();
    }
} // class Gamma
