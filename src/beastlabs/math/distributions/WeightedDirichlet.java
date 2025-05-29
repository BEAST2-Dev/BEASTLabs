package beastlabs.math.distributions;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.util.Randomizer;
import org.apache.commons.math.distribution.Distribution;

/**
 * WeightedDirichlet includes an optional input: the expected mean, which defaults to 1.
 * During both sampling and inference, the values drawn from the distribution are scaled to maintain this expected mean.
 * @author Alexei Drummond
 * @author Walter Xie
 */
@Description("Weighted Dirichlet distribution that scales dimensions by weight, where the values are scaled to maintain the expected mean (default to 1).")
public class WeightedDirichlet extends ParametricDistribution {
    final public Input<Function> alphaInput = new Input<>("alpha", "coefficients of the Dirichlet distribution", Validate.REQUIRED);
    // IntegerParameter
    final public Input<Function> weightsInput = new Input<>("weights", "weights of the scaled Dirichlet distribution", Validate.REQUIRED);
    // optional
    final public Input<Double> meanInput = new Input<>("mean",
            "expected mean of the values, default to 1", 1.0);

    // the expectedMean is default to 1, mostly used for 3 relative rates to fix the mean to 1
    private double expectedMean = 1.0;

    @Override
    public void initAndValidate() {
//        super.initAndValidate();
        expectedMean = meanInput.get();
    }

    @Override
    public double calcLogP(Function pX) {

        double[] alpha = alphaInput.get().getDoubleValues();
        if (alphaInput.get().getDimension() != pX.getDimension()) {
            throw new IllegalArgumentException("Dimensions of alpha and x should be the same, but dim(alpha)=" + alphaInput.get().getDimension()
                    + " and dim(x)=" + pX.getDimension());
        }
        double[] weight = weightsInput.get().getDoubleValues();
        if (weightsInput.get().getDimension() != pX.getDimension()) {
            throw new IllegalArgumentException("Dimensions of weight and x should be the same, but dim(weight)=" + weightsInput.get().getDimension()
                    + " and dim(x)=" + pX.getDimension());
        }

        double logP = 0;
        double sumAlpha = 0;
        double sumWeight = 0;
        double sumX = 0;

        // check sumX first
        for (int i = 0; i < pX.getDimension(); i++) {
            sumX += pX.getArrayValue(i) * weight[i];
            sumWeight += weight[i];
        }
        if (sumWeight <= 0)
            throw new RuntimeException("sum of weights (" + sumWeight +") must > 0 !");
        // re-normalise sumX based on weights
        sumX /= sumWeight;

        int dim = pX.getDimension();
        if (Math.abs(sumX - (expectedMean*dim)) > 1e-6) {
            Log.trace("sum of values (" + sumX +") differs significantly from the expected sum of values (" + expectedMean +")");
            return Double.NEGATIVE_INFINITY;
        }

        for (int i = 0; i < pX.getDimension(); i++) {
            double x = pX.getArrayValue(i) / sumX;

            logP += (alpha[i] - 1) * Math.log(x);
            logP -= org.apache.commons.math.special.Gamma.logGamma(alpha[i]);
            sumAlpha += alpha[i];
        }

        logP += org.apache.commons.math.special.Gamma.logGamma(sumAlpha);
        // area = sumX^(dim-1)
        logP -= (pX.getDimension() - 1) * Math.log(sumX);

        return logP;
    }

    @Override
    public Double[][] sample(int size) {
        int dim = alphaInput.get().getDimension();
        Double[][] samples = new Double[size][];
        for (int i = 0; i < size; i++) {
            Double[] dirichletSample = new Double[dim];
            double sum = 0.0;
            for (int j = 0; j < dim; j++) {
                dirichletSample[j] = Randomizer.nextGamma(alphaInput.get().getArrayValue(j), 1.0);
                sum += dirichletSample[j] * weightsInput.get().getArrayValue(j);
            }
            for (int j = 0; j < dim; j++) {
                // adjust the sum to the expectation
                dirichletSample[j] = (dirichletSample[j] / sum) * expectedMean * dim;
            }
            samples[i] = dirichletSample;
        }
        return samples;
    }

    @Override
    public Distribution getDistribution() {
        return null;
    }

}
