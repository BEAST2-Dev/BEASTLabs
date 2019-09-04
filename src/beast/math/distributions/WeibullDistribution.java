package beast.math.distributions;

import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.WeibullDistributionImpl;
import org.apache.commons.math.special.Gamma;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.math.distributions.ParametricDistribution;

@Description("Weibull distribution. for x>0  f(x;shape,scale) = scale/shape(x/shape)^{scale-1}e^{-(x/shape)^scale}")
public class WeibullDistribution extends ParametricDistribution {
    final public Input<RealParameter> shapeInput = new Input<>("shape", "shape parameter, defaults to 1");
    final public Input<RealParameter> scaleInput = new Input<>("scale", "scale parameter, defaults to 1 unless meanOne=true, then scale is set to 1/Gamma(1+shape) so mean of the distribution = 1");
	
    final public Input<Boolean> meanOneInput =
            new Input<>("meanOne", "Fix mean to one, ignore scale parameter", false);

    org.apache.commons.math.distribution.WeibullDistribution dist = new WeibullDistributionImpl(1, 1);

	@Override
	public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double shape = 1.0;
        double scale = 1.0;
        if (shapeInput.get() != null) {
            shape = shapeInput.get().getValue();
        }
        if (meanOneInput.get()) {
    		scale = 1.0 / Math.exp(Gamma.logGamma(1.0 + 1.0/shape));
        } else {
        	if (scaleInput.get() != null) {
        		scale = scaleInput.get().getValue();
        	}
        }

        dist.setShape(shape);
        dist.setScale(scale);
    }

	@Override
	public Distribution getDistribution() {
		return dist;
	}

    @Override
    protected double getMeanWithoutOffset() {
    	refresh();
    	return dist.getScale() * Math.exp(Gamma.logGamma(1.0 + 1.0/dist.getShape()));
    }
}
