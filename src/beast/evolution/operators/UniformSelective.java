/*
* File Uniform.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
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
/*
 * UniformOperator.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
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

import beast.core.Description;
import beast.core.Input;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.MRCAPrior;
import beast.util.Randomizer;

import java.util.ArrayList;
import java.util.List;


@Description("Select one specific internal node from a list and move node height uniformly in interval " +
        "restricted by the nodes parent and children.")
public class UniformSelective extends TreeOperator {
    public Input<List<MRCAPrior>> useOnlyInput =
            new Input<>("restricted", "Use only those nodes (MRCAs of some clades)", new ArrayList<>(), Input.Validate.REQUIRED);

    private List<MRCAPrior> useOnly = null;
    private int nNodes;

    // empty constructor to facilitate construction by XML + initAndValidate
    public UniformSelective() {
    }

    public UniformSelective(Tree tree) {
        try {
            initByName(treeInput.getName(), tree);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException("Failed to construct Uniform Tree Operator.");
        }
    }

    @Override
    public void initAndValidate() {
        useOnly = useOnlyInput.get();
        nNodes = useOnly.size();
        for( MRCAPrior m : useOnly ) {
            m.initAndValidate();
        }
        assert nNodes > 0;
    }

    /**
     * change the parameter and return the hastings ratio.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {
        final int k = nNodes > 1 ? Randomizer.nextInt( nNodes ) : 0;
        final MRCAPrior mrcaPrior = useOnly.get(k);
        mrcaPrior.calculateLogP();
        final Node node = mrcaPrior.getCommonAncestor();

        if( node.isRoot() || node.isLeaf() ) {
          return Double.NEGATIVE_INFINITY;
        }

        //final Tree tree = treeInput.get(this);

        final double upper = node.getParent().getHeight();
        final double lower = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        final double newValue = (Randomizer.nextDouble() * (upper - lower)) + lower;
        node.setHeight(newValue);

        return 0.0;
    }
}
