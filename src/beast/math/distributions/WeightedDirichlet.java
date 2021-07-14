package beast.math.distributions;

import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;

/**
 * @author Alexei Drummond
 */
@Description("Weighted Dirichlet distribution.")
public class WeightedDirichlet extends ParametricDistribution {
    final public Input<RealParameter> alphaInput = new Input<>("alpha", "coefficients of the scaled Dirichlet distribution", Validate.REQUIRED);
    final public Input<IntegerParameter> weightsInput = new Input<>("weights", "weights of the scaled Dirichlet distribution", Validate.REQUIRED);

    @Override
    public void initAndValidate() {
    }

    @Override
    public Distribution getDistribution() {
        return null;
    }

    class DirichletImpl implements ContinuousDistribution {
        Double[] m_fAlpha;

        void setAlpha(Double[] alpha) {
            m_fAlpha = alpha;
        }

        @Override
        public double cumulativeProbability(double x) throws MathException {
            throw new MathException("Not implemented yet");
        }

        @Override
        public double cumulativeProbability(double x0, double x1) throws MathException {
            throw new MathException("Not implemented yet");
        }

        @Override
        public double inverseCumulativeProbability(double p) throws MathException {
            throw new MathException("Not implemented yet");
        }

        @Override
        public double density(double x) {
            return Double.NaN;
        }

        @Override
        public double logDensity(double x) {
            return Double.NaN;
        }

    } // class DirichletImpl


    @Override
    public double calcLogP(Function pX) {

//        Double[] alpha = alphaInput.get().getValues();
//        if (alphaInput.get().getDimension() != pX.getDimension()) {
//            throw new IllegalArgumentException("Dimensions of alpha and x should be the same, but dim(alpha)=" + alphaInput.get().getDimension()
//                    + " and dim(x)=" + pX.getDimension());
//        }
//        double[] zstar = pX.getDoubleValues();
//
//        double[] z = new double[zstar.length];
//
//        double sum = 0.0;
//        for (int i = 0; i < zstar.length; i++) {
//            z[i] = zstar[i];
//            sum += z[i];
//        }
//        for (int i = 0; i < zstar.length; i++) {
//            z[i] /= sum;
//        }
//
//        double logP = 0;
//        double sumAlpha = 0;
//        for (int i = 0; i < pX.getDimension(); i++) {
//            logP += (alpha[i] - 1) * Math.log(z[i]);
//            logP -= org.apache.commons.math.special.Gamma.logGamma(alpha[i]);
//            sumAlpha += alpha[i];
//        }
//        logP += org.apache.commons.math.special.Gamma.logGamma(sumAlpha);
//
//        return logP;


        double[] zstar = pX.getDoubleValues();
        for (int i =0; i < zstar.length; i++) {
            if (zstar[i] < 0) return Double.NEGATIVE_INFINITY;
        }

        return 0.0;
    }

    @Override
    public Double[][] sample(int size) {
        int dim = alphaInput.get().getDimension();
        Double[][] samples = new Double[size][];
        for (int i = 0; i < size; i++) {
            Double[] dirichletSample = new Double[dim];
            double sum = 0.0;
            for (int j = 0; j < dim; j++) {
                dirichletSample[j] = Randomizer.nextGamma(alphaInput.get().getValue(j), 1.0);
                sum += dirichletSample[j] * weightsInput.get().getValue(j);
            }
            for (int j = 0; j < dim; j++) {
                dirichletSample[j] = dirichletSample[j] / sum;
            }
            samples[i] = dirichletSample;
        }
        return samples;
    }
}
