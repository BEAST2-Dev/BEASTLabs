package beastlabs.math.distributions;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.TensorDistribution;
import beast.base.spec.type.RealVector;
import beast.base.util.Randomizer;
import org.apache.commons.numbers.gamma.LogGamma;

import java.util.Arrays;
import java.util.List;

/**
 * WeightedDirichlet includes an optional input: the expected mean, which defaults to 1.
 * During both sampling and inference, the values drawn from the distribution are scaled to maintain this expected mean.
 * @author Alexei Drummond
 * @author Walter Xie
 */
@Description("Weighted Dirichlet distribution that scales dimensions by weight, where the values are scaled to maintain the expected mean (default to 1).")
public class WeightedDirichlet extends TensorDistribution<RealVector<PositiveReal>, Double> {
    final public Input<RealVector<? extends Real>> alphaInput = new Input<>("alpha", "coefficients of the Dirichlet distribution", Validate.REQUIRED);
    final public Input<RealVector<? extends Real>> weightsInput = new Input<>("weights", "weights of the scaled Dirichlet distribution", Validate.REQUIRED);
    // optional
    final public Input<Double> meanInput = new Input<>("mean",
            "the expected weighted mean of the values, default to 1", 1.0);

    // the expectedMean is default to 1, mostly used for 3 relative rates to fix the mean to 1
    private double expectedMean = 1.0;

    public WeightedDirichlet() {}

    public WeightedDirichlet(RealVector<PositiveReal> param, RealVector<? extends Real> alpha, RealVector<? extends Real> weights) {
        try {
            initByName("param", param, "alpha", alpha, "weights", weights);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e);
        }
    }

    @Override
    public void initAndValidate() {
        expectedMean = meanInput.get();
        if (weightsInput.get().size() != alphaInput.get().size()) {
            throw new IllegalArgumentException("Dimensions of alpha and weights should be the same, but dim(alphaInput)=" + alphaInput.get().size()
                    + " and dim(weights)=" + weightsInput.get().size());
        }
        super.initAndValidate();
    }

    private static double[] toArray(RealVector<?> v) {
        double[] a = new double[v.size()];
        for (int i = 0; i < a.length; i++) a[i] = v.get(i);
        return a;
    }

    @Override
    public void refresh() {
        // alpha and weights are read fresh in calcLogP
    }

    @Override
    protected double calcLogP(Double... value) {
        return calcLogP(Arrays.asList(value));
    }

    @Override
    public double calculateLogP() {
        logP = calcLogP(param.getElements());
        return logP;
    }

    private double calcLogP(List<Double> x) {
        final double[] alpha = toArray(alphaInput.get());
        final double[] weights = toArray(weightsInput.get());
        final int dim = x.size();

        if (alpha.length != dim)
            throw new IllegalArgumentException("Dimensions of alpha and values should be the same");
        if (weights.length != dim)
            throw new IllegalArgumentException("Dimensions of weights and values should be the same");

        double weightSum = 0.0;
        for (int i = 0; i < dim; i++)
            weightSum += weights[i];

        // Normalize weights to be scale-invariant
        double[] weightsNorm = new double[dim];
        double weightsNormSumX = 0.0;
        for (int i = 0; i < dim; i++) {
            weightsNorm[i] = weights[i] * dim / weightSum;
            weightsNormSumX += x.get(i) * weightsNorm[i];
        }

        if (Math.abs(weightsNormSumX / dim - expectedMean) > 1e-6) {
            throw new RuntimeException("The weighted mean of values (" + (weightsNormSumX / dim) +
                    ") must be same as the expected mean of values (" + expectedMean + ") !");
        }

        double sumLgWeightsNorm = 0.0;
        double logP = 0.0;
        double sumAlpha = 0.0;
        for (int i = 0; i < dim; i++) {
            double y = (x.get(i) * weightsNorm[i]) / weightsNormSumX;
            logP += (alpha[i] - 1) * Math.log(y);
            logP -= LogGamma.value(alpha[i]);
            sumAlpha += alpha[i];
            sumLgWeightsNorm += Math.log(weightsNorm[i]);
        }
        logP += LogGamma.value(sumAlpha);

        // Jacobian term
        double logJacobian = sumLgWeightsNorm - dim * Math.log(weightsNormSumX);
        return logP + logJacobian;
    }

    @Override
    public List<Double> sample() {
        final double[] alpha = toArray(alphaInput.get());
        final double[] weights = toArray(weightsInput.get());
        final int dim = alpha.length;

        double weightSum = 0.0;
        for (int j = 0; j < dim; j++)
            weightSum += weights[j];

        Double[] x = new Double[dim];
        double sum = 0.0;
        for (int j = 0; j < dim; j++) {
            x[j] = Randomizer.nextGamma(alpha[j], 1.0);
            sum += x[j];
        }
        for (int j = 0; j < dim; j++) {
            x[j] = x[j] / sum;
        }
        for (int j = 0; j < dim; j++) {
            x[j] = expectedMean * x[j] * weightSum / weights[j];
        }

        double weightedMeanX = getWeightedMean(Arrays.stream(x)
                .mapToDouble(Double::doubleValue).toArray(), weights);
        if (Math.abs(weightedMeanX - expectedMean) > 1e-6)
            throw new RuntimeException("The weighted mean of values (" + weightedMeanX +
                    ") differs significantly from the expected weighted mean of values (" + expectedMean + ") !");

        return List.of(x);
    }

    @Override
    public Double getLowerBoundOfParameter() {
        return 0.0;
    }

    @Override
    public Double getUpperBoundOfParameter() {
        return Double.POSITIVE_INFINITY;
    }

    public static double getWeightedMean(double[] samples, double[] weights) {
        if (weights.length != samples.length)
            throw new IllegalArgumentException("Dimensions of weights and values should be the same");
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
}
