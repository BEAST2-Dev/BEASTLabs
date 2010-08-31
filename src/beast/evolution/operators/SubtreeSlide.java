/*
* File SubtreeSlide.java
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
 * SubtreeSlideOperator.java
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
import beast.util.Randomizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the subtree slide move.
 */
@Description("Moves the height of an internal node along the branch. " +
        "If it moves up, it can exceed the root and become a new root. " +
        "If it moves down, it may need to make a choise which branch to " +
        "slide down into.")
public class SubtreeSlide extends TreeOperator {

//    public Input<Tree> m_tree = new Input<Tree>("beast.tree", "beast.tree on which the subtree slide operator is applied");
    public Input<Double> m_size = new Input<Double>("size", "size of the slide, default 1.0", 1.0);
    // shadows m_size
    double m_fSize;
    public Input<Boolean> m_gaussian = new Input<Boolean>("gaussian", "Gaussian (=true=default) or uniform delta", true);
//    public Input<Boolean> m_swapInRandomRate= new Input<Boolean>("swapInRandomRate","swapInRandomRate???", new Boolean(true));
//    public Input<Boolean> m_swapInRandomTrait= new Input<Boolean>("swapInRandomTrait","swapInRandomTrait???", new Boolean(true));
//    public Input<Boolean> m_scaledDirichletBranches= new Input<Boolean>("scaledDirichletBranches","scaledDirichletBranches???", new Boolean(true));

    @Override
    public void initAndValidate() {
        m_fSize = m_size.get();
    }

    /**
     * Do a probabilistic subtree slide move.
     *
	 * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted **/
    @Override
    public double proposal() {
        Tree tree = m_tree.get(this);//(Tree) state.getStateNode(m_tree);

        //calculateHeightsFromLengths(beast.tree);

        double logq;

        Node i;

        // 1. choose a random node avoiding root
        do {
            i = tree.getNode(Randomizer.nextInt(tree.getNodeCount()));
        } while (i.isRoot());

        final Node iP = i.getParent();
        final Node CiP = getOtherChild(iP, i);
        final Node PiP = iP.getParent();

        // 2. choose a delta to move
        final double delta = getDelta();
        final double oldHeight = iP.getHeight();
        final double newHeight = oldHeight + delta;

        // 3. if the move is up
        if (delta > 0) {

            // 3.1 if the topology will change
            if (PiP != null && PiP.getHeight() < newHeight) {
                // find new parent
                Node newParent = PiP;
                Node newChild = iP;
                while (newParent.getHeight() < newHeight) {
                    newChild = newParent;
                    newParent = newParent.getParent();
                    if (newParent == null) break;
                }


                // 3.1.1 if creating a new root
                if (newChild.isRoot()) {
                	//if (true) return Double.NEGATIVE_INFINITY;
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);

//                	beast.tree.removeChild(iP, CiP);
//                    beast.tree.removeChild(PiP, iP);
//                    beast.tree.addChild(iP, newChild);
//                    beast.tree.addChild(PiP, CiP);

                    iP.setParent(null);
                    tree.setRoot(iP);
                    //System.err.println("Creating new root!");

//                    if (beast.tree.hasNodeTraits()) {
//                        // **********************************************
//                        // swap traits and rates so that root keeps it trait and rate values
//                        // **********************************************
//
//                        beast.tree.swapAllTraits(newChild, iP);
//
//                    }

//                    if (beast.tree.hasRates()) {
//                        final double rootNodeRate = beast.tree.getNodeRate(newChild);
//                        beast.tree.setNodeRate(newChild, beast.tree.getNodeRate(iP));
//                        beast.tree.setNodeRate(iP, rootNodeRate);
//                    }

                    // **********************************************

                }
                // 3.1.2 no new root
                else {
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);
                    replace(newParent, newChild, iP);

//                    beast.tree.removeChild(iP, CiP);
//                    beast.tree.removeChild(PiP, iP);
//                    beast.tree.removeChild(newParent, newChild);
//                    beast.tree.addChild(iP, newChild);
//                    beast.tree.addChild(PiP, CiP);
//                    beast.tree.addChild(newParent, iP);
                    //System.err.println("No new root!");
                }

                iP.setHeight(newHeight);

                // 3.1.3 count the hypothetical sources of this destination.
                final int possibleSources = intersectingEdges(newChild, oldHeight, null);
                //System.out.println("possible sources = " + possibleSources);

                logq = -Math.log(possibleSources);

            } else {
                // just change the node height
                iP.setHeight(newHeight);
                logq = 0.0;
            }
        }
        // 4 if we are sliding the subtree down.
        else {

            // 4.0 is it a valid move?
            if (i.getHeight() > newHeight) {
                return Double.NEGATIVE_INFINITY;
            }

            // 4.1 will the move change the topology
            if (CiP.getHeight() > newHeight) {

                List<Node> newChildren = new ArrayList<Node>();
                final int possibleDestinations = intersectingEdges(CiP, newHeight, newChildren);

                // if no valid destinations then return a failure
                if (newChildren.size() == 0) {
                    return Double.NEGATIVE_INFINITY;
                }

                // pick a random parent/child destination edge uniformly from options
                final int childIndex = Randomizer.nextInt(newChildren.size());
                Node newChild = newChildren.get(childIndex);
                Node newParent = newChild.getParent();


                // 4.1.1 if iP was root
                if (iP.isRoot()) {
                    // new root is CiP
                    replace(iP, CiP, newChild);
                    replace(newParent, newChild, iP);

                    //beast.tree.removeChild(iP, CiP);
                    //beast.tree.removeChild(newParent, newChild);
                    //beast.tree.addChild(iP, newChild);
                    //beast.tree.addChild(newParent, iP);
                    CiP.setParent(null);
                    tree.setRoot(CiP);

//                    if (beast.tree.hasNodeTraits()) {
//                        // **********************************************
//                        // swap traits and rates, so that root keeps it trait and rate values
//                        // **********************************************
//
//                        beast.tree.swapAllTraits(iP, CiP);
//
//                    }
//getHeight()
//                    if (beast.tree.hasRates()) {
//                        final double rootNodeRate = beast.tree.getNodeRate(iP);
//                        beast.tree.setNodeRate(iP, beast.tree.getNodeRate(CiP));
//                        beast.tree.setNodeRate(CiP, rootNodeRate);
//                    }

                    // **********************************************

                    //System.err.println("DOWN: Creating new root!");
                } else {
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);
                    replace(newParent, newChild, iP);

                    //beast.tree.removeChild(iP, CiP);
                    //beast.tree.removeChild(PiP, iP);
                    //beast.tree.removeChild(newParent, newChild);
                    //beast.tree.addChild(iP, newChild);
                    //beast.tree.addChild(PiP, CiP);
                    //beast.tree.addChild(newParent, iP);
                    //System.err.println("DOWN: no new root!");
                }

                iP.setHeight(newHeight);

                logq = Math.log(possibleDestinations);
            } else {
                iP.setHeight(newHeight);
                logq = 0.0;
            }
        }

//        if (swapInRandomRate) {
//            final Node j = beast.tree.getNode(Randomizer.nextInt(beast.tree.getNodeCount()));
//            if (j != i) {
//                final double tmp = beast.tree.getNodeRate(i);
//                beast.tree.setNodeRate(i, beast.tree.getNodeRate(j));
//                beast.tree.setNodeRate(j, tmp);
//            }
//
//        }
//
//        if (swapInRandomTrait) {
//            final Node j = beast.tree.getNode(Randomizer.nextInt(beast.tree.getNodeCount()));
//            if (j != i) {
//
//                beast.tree.swapAllTraits(i, j);
//
////                final double tmp = beast.tree.getNodeTrait(i, TRAIT);
////                beast.tree.setNodeTrait(i, TRAIT, beast.tree.getNodeTrait(j, TRAIT));
////                beast.tree.setNodeTrait(j, TRAIT, tmp);
//            }
//
//        }

//        if (logq == Double.NEGATIVE_INFINITY) throw new Exception("invalid slide");

//        if (scaledDirichletBranches) {
//            if (oldTreeHeight != beast.tree.getRoot().getHeight())
//                throw new Exception("Temporarily disabled."); // TODO calculate Hastings ratio
//        }

        //beast.tree.getRoot().setLength(0);
        //setLengthsFromHeights(beast.tree.getRoot());
        return logq;
    }

    private double getDelta() {
        if (!m_gaussian.get()) {
            return (Randomizer.nextDouble() * m_fSize) - (m_fSize / 2.0);
        } else {
            return Randomizer.nextGaussian() * m_fSize;
        }
    }

    private int intersectingEdges(Node node, double height, List<Node> directChildren) { 
        final Node parent = node.getParent();

        if (parent.getHeight() < height) return 0;

        if (node.getHeight() < height) {
            if (directChildren != null) directChildren.add(node);
            return 1;
        }

        if (node.isLeaf()) {
            // TODO: verify that this makes sense
            return 0;
        } else {
            int count = intersectingEdges(node.m_left, height, directChildren) +
                    intersectingEdges(node.m_right, height, directChildren);
            return count;
        }
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(double logAlpha) {
        double fDelta = calcDelta(logAlpha);
        fDelta += Math.log(m_fSize);
        m_fSize = Math.exp(fDelta);
    }
}
