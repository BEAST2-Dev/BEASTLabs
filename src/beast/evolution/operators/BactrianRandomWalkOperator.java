package beast.evolution.operators;



import java.text.DecimalFormat;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;


@Description("A random walk operator that selects a random dimension of the real parameter and perturbs the value a " +
        "random amount according to a Bactrian distribution (Yang & Rodrigues, 2013), which is a mixture of two Gaussians:"
        + "p(x) = 1/2*N(x;-m,1-m^2) + 1/2*N(x;+m,1-m^2) and more efficient than RealRandomWalkOperator")
public class BactrianRandomWalkOperator extends Operator {
    final public Input<Double> windowSizeInput = new Input<>("m", "standard deviation for Bactrian distribution. "
    		+ "Larger values give more peaked distributions. "
    		+ "The default 0.95 is claimed to be a good choice (Yang 2014, book p.224).", 0.95);
    final public Input<RealParameter> parameterInput = new Input<>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);
    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);

    double m = 1;    
    double scaleFactor;

    @Override
	public void initAndValidate() {
        m = windowSizeInput.get();
        if (m <=0 || m >= 1) {
        	throw new IllegalArgumentException("m should be withing the (0,1) range");
        }
        scaleFactor = scaleFactorInput.get();
    }

    @Override
    public double proposal() {

        RealParameter param = parameterInput.get(this);

        int i = Randomizer.nextInt(param.getDimension());
        double value = param.getValue(i);
        double newValue = value;
        if (Randomizer.nextBoolean()) {
        	newValue += scaleFactor * (m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
        } else {
        	newValue += scaleFactor * (-m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
        }
        
        if (newValue < param.getLower() || newValue > param.getUpper()) {
            return Double.NEGATIVE_INFINITY;
        }

        param.setValue(i, newValue);

        return 0;
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
        double delta = calcDelta(logAlpha);

        delta += Math.log(scaleFactor);
        scaleFactor = Math.exp(delta);
    }

    @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = scaleFactor * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }
} // class BactrianRandomWalkOperator