package beast.evolution.operators;

import java.text.DecimalFormat;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.ScaleOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

@Description("Scale operator that finds scale factor according to a Bactrian distribution (Yang & Rodrigues, 2013), "
		+ "which is a mixture of two Gaussians: p(x) = 1/2*N(x;-m,1-m^2) + 1/2*N(x;+m,1-m^2) and more efficient than RealRandomWalkOperator")
public class BactrianScaleOperator extends ScaleOperator {
    final public Input<Double> windowSizeInput = new Input<>("m", "standard deviation for Bactrian distribution. "
    		+ "Larger values give more peaked distributions. "
    		+ "The default 0.95 is claimed to be a good choice (Yang 2014, book p.224).", 0.95);

    double m = 1;

	@Override
	public void initAndValidate() {
        m = windowSizeInput.get();
        if (m <=0 || m >= 1) {
        	throw new IllegalArgumentException("m should be withing the (0,1) range");
        }
        if (scaleUpperLimit.get() == 1 - 1e-8) {
        	scaleUpperLimit.setValue(10.0, this);
        }
		super.initAndValidate();
	}
    
    @Override
	protected double getScaler() {
        double scale = 0;
    	double s = getCoercableParameterValue();
        if (Randomizer.nextBoolean()) {
        	scale = s * (m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
        } else {
        	scale = s * (-m + Randomizer.nextGaussian() * Math.sqrt(1-m*m));
        }
        scale = Math.exp(scale);
		return scale;
	}
    

    @Override
    public double proposal() {
        try {

            double hastingsRatio;
            final double scale = getScaler();

            if (m_bIsTreeScaler) {

                final Tree tree = treeInput.get(this);
                if (rootOnlyInput.get()) {
                    final Node root = tree.getRoot();
                    final double newHeight = root.getHeight() * scale;

                    if (newHeight < Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
                        return Double.NEGATIVE_INFINITY;
                    }
                    root.setHeight(newHeight);
                    return Math.log(scale);
                } else {
                    // scale the beast.tree
                    final int internalNodes = tree.scale(scale);
                    return Math.log(scale) * internalNodes;
                }
            }

            // not a tree scaler, so scale a parameter
            final boolean scaleAll = scaleAllInput.get();
            final int specifiedDoF = degreesOfFreedomInput.get();
            final boolean scaleAllIndependently = scaleAllIndependentlyInput.get();

            final RealParameter param = parameterInput.get(this);

            assert param.getLower() != null && param.getUpper() != null;

            final int dim = param.getDimension();

            if (scaleAllIndependently) {
                // update all dimensions independently.
                hastingsRatio = 0;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);
                    for (int i = 0; i < dim; i++) {
                        if( (impliedOne && (i == 0 || indicator[i-1])) || (!impliedOne && indicator[i]) )  {
                            final double scaleOne = getScaler();
                            final double newValue = scaleOne * param.getValue(i);

                            hastingsRatio += Math.log(scaleOne);

                            if (outsideBounds(newValue, param)) {
                                return Double.NEGATIVE_INFINITY;
                            }

                            param.setValue(i, newValue);
                        }
                    }
                }  else {

                    for (int i = 0; i < dim; i++) {

                        final double scaleOne = getScaler();
                        final double newValue = scaleOne * param.getValue(i);

                        hastingsRatio += Math.log(scaleOne);

                        if( outsideBounds(newValue, param) ) {
                            return Double.NEGATIVE_INFINITY;
                        }

                        param.setValue(i, newValue);
                    }
                }
            } else if (scaleAll) {
                // update all dimensions
                // hasting ratio is dim-2 times of 1dim case. would be nice to have a reference here
                // for the proof. It is supposed to be somewhere in an Alexei/Nicholes article.

                // all Values assumed independent!
                final int computedDoF = param.scale(scale);
                final int usedDoF = (specifiedDoF > 0) ? specifiedDoF : computedDoF ;
                hastingsRatio = usedDoF * Math.log(scale);
            } else {
                hastingsRatio = Math.log(scale);

                // which position to scale
                final int index;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);

                    // available bit locations. there can be hundreds of them. scan list only once.
                    final int[] loc = new int[dimCount + 1];
                    int locIndex = 0;

                    if (impliedOne) {
                        loc[locIndex] = 0;
                        ++locIndex;
                    }
                    for (int i = 0; i < dimCount; i++) {
                        if (indicator[i]) {
                            loc[locIndex] = i + (impliedOne ? 1 : 0);
                            ++locIndex;
                        }
                    }

                    if (locIndex > 0) {
                        final int rand = Randomizer.nextInt(locIndex);
                        index = loc[rand];
                    } else {
                        return Double.NEGATIVE_INFINITY; // no active indicators
                    }

                } else {
                    // any is good
                    index = Randomizer.nextInt(dim);
                }

                final double oldValue = param.getValue(index);

                if (oldValue == 0) {
                    // Error: parameter has value 0 and cannot be scaled
                    return Double.NEGATIVE_INFINITY;
                }

                final double newValue = scale * oldValue;

                if (outsideBounds(newValue, param)) {
                    // reject out of bounds scales
                    return Double.NEGATIVE_INFINITY;
                }

                param.setValue(index, newValue);
                // provides a hook for subclasses
                //cleanupOperation(newValue, oldValue);
            }

            return hastingsRatio;

        } catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }

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

}
