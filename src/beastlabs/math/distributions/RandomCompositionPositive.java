package beastlabs.math.distributions;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.inference.parameter.IntegerParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
@Description("Samples a random k-tuple of positive integers that sum to n.")
public class RandomCompositionPositive extends Distribution {

    final public Input<IntegerParameter> compositionInput = new Input<>("composition", "the k-tuple positive integers.", Input.Validate.REQUIRED);

    final public Input<IntegerParameter> kInput = new Input<>("k", "the size of the random tuple.", Input.Validate.REQUIRED);
    final public Input<IntegerParameter> nInput = new Input<>("n", "the sum of the random tuple.", Input.Validate.REQUIRED);

    @Override
    public void initAndValidate() {
        final int k = kInput.get().getValue();
        final int n = nInput.get().getValue();
        IntegerParameter composition = compositionInput.get();
        if (composition.getDimension() != k)
            throw new IllegalArgumentException("The composition must be size of k (" + k + "), but its dim = " + composition.getDimension());

        int sum = 0;
        for (Integer value : composition.getValues()) {
            if (value <= 0)
                throw new IllegalArgumentException("The integer must > 0, but it is " + value);
            sum += value;
        }
        if (sum != n)
            throw new IllegalArgumentException("The composition must sum to n (" + n + "), but the current sum = " + sum);
    }

    @Override
    public double calculateLogP() {
        logP = 0.0;
        final int k = kInput.get().getValue();
        final int n = nInput.get().getValue();
        IntegerParameter composition = compositionInput.get();
        if (composition.getDimension() != k)
            return Double.NEGATIVE_INFINITY;

        int sum = 0;
        for (Integer value : composition.getValues()) {
            if (value <= 0)
                return Double.NEGATIVE_INFINITY;
            sum += value;
        }
        if (sum != n)
            return Double.NEGATIVE_INFINITY;

        logP = 1.0 / (double) CompositionCounter.countCompositionsPositive(k, n);
        return logP;
    }

    private Integer[] sampleComposition(int k, int n, Random random) {
        List<Integer> bars = new ArrayList<>();
        bars.add(0);
        while (bars.size() < k) {
            int candidate = random.nextInt(n - 1) + 1;
            if (!bars.contains(candidate)) {
                bars.add(candidate);
            }
        }
        bars.add(n);
        Collections.sort(bars);

        Integer[] composition = new Integer[k];
        for (int i = 0; i < composition.length; i++) {
            composition[i] = bars.get(i + 1) - bars.get(i);
        }
        return composition;
    }

    @Override
    public List<String> getArguments() {
        return List.of();
    }

    @Override
    public List<String> getConditions() {
        return List.of();
    }

    @Override
    public void sample(State state, Random random) {
        throw new UnsupportedOperationException();
    }

}
