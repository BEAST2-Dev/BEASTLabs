package beastlabs.math.distributions;


import org.apache.commons.statistics.distribution.GammaDistribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;



@Description("One parameter (shape) gamma distribution, used as prior. Scale = 1/shape, so that mean = 1." +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class GammaOneP extends ParametricDistribution {
    final public Input<RealScalar<? extends PositiveReal>> shapeInput = new Input<>("shape", "shape parameter, defaults to 1");

    GammaDistribution m_dist = GammaDistribution.of(1.0, 1.0);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double shape;
        if (shapeInput.get() == null) {
            shape = 1;
        } else {
            shape = shapeInput.get().get();
        }
        m_dist = GammaDistribution.of(shape, 1.0 / shape);
    }

    @Override
    public GammaDistribution getDistribution() {
        refresh();
        return m_dist;
    }



    @Override
    public double inverseCumulativeProbability(double p) {
    	double x = super.inverseCumulativeProbability(p);
    	return x;
    }

    @Override
    protected double getMeanWithoutOffset() {
    	return 1.0;
    }
    
} // class GammaOneP
