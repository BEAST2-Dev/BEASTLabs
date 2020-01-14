package beast.evolution.operators;



import java.text.DecimalFormat;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;


@Description("A scale operator that selects a random dimension of the real parameter and scales the value a " +
        "random amount according to a Bactrian distribution such that the parameter remains in its range. "
        + "Supposed to be more efficient than UniformOperator")
public class BactrianIntervalOperator extends Operator {
    final public Input<Double> windowSizeInput = new Input<>("m", "standard deviation for Bactrian distribution. "
    		+ "Larger values give more peaked distributions. "
    		+ "The default 0.95 is claimed to be a good choice (Yang 2014, book p.224).", 0.95);
    final public Input<RealParameter> parameterInput = new Input<>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);
    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);

    double m = 1;    
    double scaleFactor;
    double lower, upper;

    @Override
	public void initAndValidate() {
        m = windowSizeInput.get();
        if (m <=0 || m >= 1) {
        	throw new IllegalArgumentException("m should be withing the (0,1) range");
        }
        scaleFactor = scaleFactorInput.get();

        RealParameter param = parameterInput.get();
        lower = (Double) param.getLower();
        upper = (Double) param.getUpper();

        if (Double.isInfinite(lower)) {
        	throw new IllegalArgumentException("Lower bound should be finite");
        }
        if (Double.isInfinite(upper)) {
        	throw new IllegalArgumentException("Upper bound should be finite");
        }
    }

    @Override
    public double proposal() {

        RealParameter param = parameterInput.get(this);

        int i = Randomizer.nextInt(param.getDimension());
        double value = param.getValue(i);
        double scale = BactrianHelper.getScaler(m, scaleFactor);
        
        // transform value
        double y = (upper - value) / (value - lower);
        y *= scale;
        double newValue = (upper + lower * y) / (y + 1.0);
        
        if (newValue < lower || newValue > upper) {
        	throw new RuntimeException("programmer error: new value proposed outside range");
        }
        
        param.setValue(i, newValue);

        double logHR = Math.log(scale) + 2.0 * Math.log((newValue - lower)/(value - lower));
        return logHR;
    }


    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
    	scaleFactor = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */

    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
    	if (optimiseInput.get()) {
	        double delta = calcDelta(logAlpha);
	        double scaleFactor = getCoercableParameterValue();
	        delta += Math.log(scaleFactor);
	        scaleFactor = Math.exp(delta);
	        setCoercableParameterValue(scaleFactor);
    	}
    }
    
    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }
    


    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = getCoercableParameterValue() * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }
    
} // class BactrianIntervalOperator