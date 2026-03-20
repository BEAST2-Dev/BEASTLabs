package beastlabs.math.distributions;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.util.Randomizer;
import org.apache.commons.math.distribution.Distribution;

import java.util.Arrays;

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
            "the expected weighted mean of the values, default to 1", 1.0);

    // the expectedMean is default to 1, mostly used for 3 relative rates to fix the mean to 1
    private double expectedMean = 1.0;

    @Override
    public void initAndValidate() {
//        super.initAndValidate();
        expectedMean = meanInput.get();
        if (weightsInput.get().getDimension() != alphaInput.get().getDimension()) {
            throw new IllegalArgumentException("Dimensions of alpha and weights should be the same, but dim(alphaInput)=" + alphaInput.get().getDimension()
                    + " and dim(weights)=" + weightsInput.get().getDimension());
        }
    }

    @Override
    public double calcLogP(Function pX) {
        final double[] x = pX.getDoubleValues();
        final double[] alpha = alphaInput.get().getDoubleValues();
        validateDimensions(alpha, x, "alpha", "values");

        final double[] weights = weightsInput.get().getDoubleValues();
        validateDimensions(weights, x, "weights", "values");

        final int dim = x.length;
        double weightSum = 0.0; // ∑ (weight[i])
        for (int i = 0; i < dim; i++)
            weightSum += weights[i];

        // Normalize weights to be scale-invariant
        double[] weightsNorm = new double[dim];
        // ∑ (x[i] * weights_norm[i]) == target_sum
        double weightsNormSumX = 0.0;
        for (int i = 0; i < dim; i++) {
            // weights_norm = weight[i] * len(weights) / ∑ (weight[i])
            weightsNorm[i] = weights[i] * dim / weightSum;
            // weightsNormSumX = ∑ (x[i] * weights_norm[i]), and it is also target_sum
            weightsNormSumX += x[i] * weightsNorm[i];
        }

        // (weightsNormSumX / dim) is the weighted mean
        if (Math.abs(weightsNormSumX / dim - expectedMean) > 1e-6) {
//            return Double.NEGATIVE_INFINITY;
            throw new RuntimeException("The weighted mean of values (" + (weightsNormSumX / dim) +
                    ") must be same as the expected mean of values (" + expectedMean + ") !");
        }

        // log_density = gammaln(np.sum(conc)) - np.sum(gammaln(conc))
        //        + np.sum((conc - 1) * np.log(y))
        double sumLgWeightsNorm = 0.0; // for Jacobian term
        double logP = 0.0;
        double sumAlpha = 0.0;
        for (int i = 0; i < dim; i++) {
            // Transform to standard Dirichlet space
            // y = (x[i] * weights_norm[i]) / target_sum
            double y = (x[i] * weightsNorm[i]) / weightsNormSumX;
            // log density
            logP += (alpha[i] - 1) * Math.log(y);
            logP -= org.apache.commons.math.special.Gamma.logGamma(alpha[i]);
            sumAlpha += alpha[i];

            sumLgWeightsNorm += Math.log(weightsNorm[i]);
        }
        logP += org.apache.commons.math.special.Gamma.logGamma(sumAlpha);

        // TODO Jacobian term needs to test
        // log_jacobian = np.sum(np.log(weights_norm)) - len(x) * np.log(target_sum)
        double logJacobian = sumLgWeightsNorm - dim * Math.log(weightsNormSumX);
        return logP + logJacobian;
    }

    @Override
    public Double[][] sample(int size) {
        final double[] alpha = alphaInput.get().getDoubleValues();
        final double[] weights = weightsInput.get().getDoubleValues();
        validateDimensions(alpha, weights, "alpha", "weights");

        final int dim = alpha.length;
        double weightSum = 0.0;
        for (int j = 0; j < dim; j++)
            weightSum += weights[j];

        Double[][] samples = new Double[size][];
        for (int i = 0; i < size; i++) {
            Double[] x = new Double[dim];
            // step 1: Sample from standard Dirichlet
            double sum = 0.0;
            for (int j = 0; j < dim; j++) {
                x[j] = Randomizer.nextGamma(alpha[j], 1.0);
                sum += x[j];
            }
            for (int j = 0; j < dim; j++) {
                // if expectedMean != 1, then adjust it
                x[j] = x[j] / sum;
            }

            // step 2: Transform to weighted rates:
            for (int j = 0; j < dim; j++) {
                x[j] = expectedMean * x[j] * weightSum / weights[j];
            }

            // validate : weighted mean = ∑(x[j] * weight[j]) / ∑(weight[j])
            double weightedMeanX = getWeightedMean(Arrays.stream(x)
                    .mapToDouble(Double::doubleValue).toArray(), weights);
            if (Math.abs(weightedMeanX  - expectedMean) > 1e-6)
                throw new RuntimeException("The weighted mean of values (" + weightedMeanX +
                        ") differs significantly from the expected weighted mean of values (" + expectedMean +") !");

            samples[i] = x;
        }
        return samples;
    }

    public static double getWeightedMean(double[] samples, double[] weights) {
        validateDimensions(weights, samples, "weights", "values");
        // the weight mean = sum(x[i] * weight[i]) / sum(weight[i])
        double weightedSumX = 0.0;
        for (int i = 0; i < samples.length; i++)
            weightedSumX += samples[i] * weights[i];
        double weightSum = 0.0;
        for (double weight : weights)
            weightSum += weight;
        if (weightSum <= 0.0)
            return Double.NEGATIVE_INFINITY;
        return weightedSumX / weightSum;
    }

    private static void validateDimensions(double[] arr, double[] reference, String arrName, String referenceName) {
        if (arr.length != reference.length) {
            throw new IllegalArgumentException( "Dimensions of " + arrName + " and " +
                    referenceName + " should be the same, but dim(" + arrName + ") = " + arr.length +
                    " and dim(" + referenceName + ") = " + reference.length);
        }
    }

    @Override
    public Distribution getDistribution() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
