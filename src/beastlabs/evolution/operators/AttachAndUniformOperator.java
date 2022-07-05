package beastlabs.evolution.operators;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.util.Randomizer;
import beastlabs.math.distributions.MultiMonophyleticConstraint;

import java.util.*;

@Description("Detach and re-attach a clade as per AttachOperator, and then resample as per UniformOperator only the elements of a multi-dimensional parameter which correspond to branches which were changed.")
public class AttachAndUniformOperator extends AttachOperator {
    final public Input<Parameter<?>> parameterInput = new Input<>("parameter",
            "a real or integer parameter to sample individual values for",
            Validate.REQUIRED, Parameter.class);

    Parameter<?> parameter;
    double lower, upper;
    int lowerIndex, upperIndex;
    private int nNodes;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        parameter = parameterInput.get();
        if (parameter instanceof RealParameter) {
            lower = (Double) parameter.getLower();
            upper = (Double) parameter.getUpper();
        } else if (parameter instanceof IntegerParameter) {
            lowerIndex = (Integer) parameter.getLower();
            upperIndex = (Integer) parameter.getUpper();
        } else {
            throw new IllegalArgumentException("parameter should be a " +
                    "RealParameter or IntergerParameter, not " +
                    parameter.getClass().getName());
        }

        nNodes = useOnly.size();
        for( MRCAPrior m : useOnly ) {
            m.initAndValidate();
        }
        assert nNodes > 0;

        // Check for compatibility between tree and parameter.
        // This assumes that all MRCAPriors come from the same tree, i.e. that
        // the user knows what they are doing.
        final MRCAPrior m = useOnly.get(0);
        m.calculateLogP();
        final int branch_count =
            m.getCommonAncestor().getTree().getNodeCount() - 1;
        if(parameter.getDimension() != branch_count) {
            throw new IllegalArgumentException("Dimensionality of parameter " +
                    "does not equal the number of branches in the tree " +
                    "containing the first useOnly clade.");
        }
    }

    @Override
    public double proposal() {
        final double attachHastings = super.proposal();
        if(attachHastings == Double.NEGATIVE_INFINITY) {
            return Double.NEGATIVE_INFINITY;
        }
        final Tree tree = treeInput.get();
        Node [] nodes = tree.getNodesAsArray();
        for(int index = tree.getLeafNodeCount(); index < nodes.length - 1; index++) {
            if(tree.childrenChanged(index)) {
                // Use node number as index into parameter array
                // Copied from UniformOperator.java
                if (parameter instanceof IntegerParameter) {
                    int newValue = Randomizer.nextInt(upperIndex - lowerIndex + 1) + lowerIndex; // from 0 to n-1, n must > 0,
                    ((IntegerParameter) parameter).setValue(index, newValue);
                } else {
                    double newValue = Randomizer.nextDouble() * (upper - lower) + lower;
                    ((RealParameter) parameter).setValue(index, newValue);
                }
            }
        }

        return 0.0;
    }

}
