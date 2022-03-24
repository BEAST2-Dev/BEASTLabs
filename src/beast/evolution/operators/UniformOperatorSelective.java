/*
* File UniformOperatorSelective.java
*
* Copyright (C) 2018 Luke Maurits <luke@maurits.id.au>
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/

package beast.evolution.operators;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Operator;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.util.Randomizer;

import java.util.ArrayList;
import java.util.List;


@Description("Assign all elements of a multi-dimensional parameter " +
        "corresponding to the nodes of a particular clade to a uniformly" +
        "selected value in their ranges.")
public class UniformOperatorSelective extends Operator {
    final public Input<Parameter<?>> parameterInput = new Input<>("parameter",
            "a real or integer parameter to sample individual values for",
            Validate.REQUIRED, Parameter.class);
    public Input<List<MRCAPrior>> useOnlyInput =
            new Input<>("restricted", "Use only those nodes (MRCAs of some " +
            "clades)", new ArrayList<>(), Input.Validate.REQUIRED);

    Parameter<?> parameter;
    double lower, upper;
    int lowerIndex, upperIndex;
    private List<MRCAPrior> useOnly = null;
    private int nNodes;

    // empty constructor to facilitate construction by XML + initAndValidate
    public UniformOperatorSelective() {
    }

    @Override
    public void initAndValidate() {
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

        useOnly = useOnlyInput.get();
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

    /**
     * change the parameter and return the hastings ratio.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
     * should not be accepted *
     */
    @Override
    public double proposal() {

        for( MRCAPrior mrcaPrior : useOnly ) {
            // Get the common ancestor of this clade
            mrcaPrior.calculateLogP();
            Node mrca = mrcaPrior.getCommonAncestor();
            // Get all descendant nodes
            List<Node> nodesInClade = new ArrayList<>();
            mrca.getAllChildNodes(nodesInClade);
            // Resample parameter for each descendant node
            for(Node node : nodesInClade) {
                // Use node number as index into parameter array
                int index = node.getNr();
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
