package beast.math.distributions;


import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.GammaDistributionImpl;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;



@Description("One parameter (shape) gamma distribution, used as prior. Scale = 1/shape, so that mean = 1." +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class GammaOneP extends ParametricDistribution {
    final public Input<RealParameter> shapeInput = new Input<>("shape", "shape parameter, defaults to 1");

    org.apache.commons.math.distribution.GammaDistribution m_dist = new GammaDistributionImpl(1.0, 1.0);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double shape;
        if (shapeInput.get() == null) {
            shape = 1;
        } else {
            shape = shapeInput.get().getValue();
        }
        m_dist.setAlpha(shape);
        m_dist.setBeta(1.0 / shape);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return m_dist;
    }
    
    
    
    @Override
    public double inverseCumulativeProbability(double p) throws MathException {
    	double x = super.inverseCumulativeProbability(p);
    	return x;
    }
    
    @Override
    protected double getMeanWithoutOffset() {
    	return 1.0;
    }
    
} // class GammaOneP
