package beastlabs.math.distributions;

import org.apache.commons.numbers.gamma.LogGamma;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import beast.base.inference.distribution.ParametricDistribution;

@Description("Weibull distribution. for x>0  f(x;shape,scale) = scale/shape(x/shape)^{scale-1}e^{-(x/shape)^scale}")
public class WeibullDistribution extends ParametricDistribution {
    final public Input<RealScalar<? extends PositiveReal>> shapeInput = new Input<>("shape", "shape parameter, defaults to 1");
    final public Input<RealScalar<? extends PositiveReal>> scaleInput = new Input<>("scale", "scale parameter, defaults to 1 unless meanOne=true, then scale is set to 1/Gamma(1+shape) so mean of the distribution = 1");

    final public Input<Boolean> meanOneInput =
            new Input<>("meanOne", "Fix mean to one, ignore scale parameter", false);

    org.apache.commons.statistics.distribution.WeibullDistribution dist = org.apache.commons.statistics.distribution.WeibullDistribution.of(1, 1);

	@Override
	public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double shape = 1.0;
        double scale = 1.0;
        if (shapeInput.get() != null) {
            shape = shapeInput.get().get();
        }
        if (meanOneInput.get()) {
    		scale = 1.0 / Math.exp(LogGamma.value(1.0 + 1.0/shape));
        } else {
        	if (scaleInput.get() != null) {
        		scale = scaleInput.get().get();
        	}
        }

        dist = org.apache.commons.statistics.distribution.WeibullDistribution.of(shape, scale);
    }

	@Override
	public org.apache.commons.statistics.distribution.WeibullDistribution getDistribution() {
		return dist;
	}

    @Override
    protected double getMeanWithoutOffset() {
    	refresh();
    	return dist.getScale() * Math.exp(LogGamma.value(1.0 + 1.0/dist.getShape()));
    }
}
