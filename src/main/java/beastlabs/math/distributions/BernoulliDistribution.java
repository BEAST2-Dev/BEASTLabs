package beastlabs.math.distributions;

import beast.base.core.Description;
import beast.base.inference.Distribution;
import beast.base.core.Input;
import beast.base.inference.State;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;

import java.util.List;
import java.util.Random;

/**
 * @author Alexei Drummond
 */
@Description("Bernoulli distribution, used as prior or likelihood." +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class BernoulliDistribution extends Distribution {

    final public Input<RealVectorParam<? extends UnitInterval>> pInput = new Input<>("p", "probability p parameter. Must be either " +
            "size 1 for iid trials, or the same dimension as trials parameter if inhomogeneous bernoulli process.", Input.Validate.REQUIRED);
    final public Input<BoolVectorParam> trialsInput = new Input<>("parameter", "the results of a series of bernoulli trials.");

    final public Input<IntScalarParam<? extends NonNegativeInt>> minSuccessesInput = new Input<>("minSuccesses",
            "Optional condition: the minimum number of ones in the boolean array.");

    public double calculateLogP() {
        logP = 0.0;

        BoolVectorParam trials = trialsInput.get();
        RealVectorParam<? extends UnitInterval> p = pInput.get();
        IntScalarParam<? extends NonNegativeInt> minSuccesses = minSuccessesInput.get();

        // for efficiency split the two options
        if (p.size() == 1) {
            double prob = p.get(0);
            double logProb = Math.log(prob);
            double log1MinusProb = Math.log(1.0-prob);

            for (int i = 0; i < trials.size(); i++) {
                logP += trials.get(i) ? logProb : log1MinusProb;
            }

        } else {
            // reject if < minSuccesses
            if (minSuccesses != null && hammingWeight(trials) < minSuccesses.get())
                return Double.NEGATIVE_INFINITY;

            for (int i = 0; i < trials.size(); i++) {
                double prob = p.get(i);
                logP += Math.log(trials.get(i) ? prob : 1.0 - prob);
            }

        }
        return logP;
    }

    private int hammingWeight(BoolVectorParam trials) {
        int sum = 0;
        for (int i = 0; i < trials.size(); i++)
            if (trials.get(i)) sum += 1;
        return sum;
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(State state, Random random) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initAndValidate() {
        if (pInput.get().size() != 1 && pInput.get().size() != trialsInput.get().size()) {
            throw new RuntimeException("p parameter must be size 1 or the same size as trials parameter but it was dimension " + pInput.get().size());
        }
    }
}
