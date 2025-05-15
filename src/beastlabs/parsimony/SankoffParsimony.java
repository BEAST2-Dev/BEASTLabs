/*
 * SankoffParsimony.java
 *
 * Copyright © 2002-2024 the BEAST Development Team
 * http://beast.community/about
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
 *
 */

package beastlabs.parsimony;

import java.util.Set;
import java.util.TreeSet;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;

import java.util.Iterator;

/**
 * Class for reconstructing characters using the Sankoff generalized parsimony methods. This will be
 * slower than the Fitch algorithm but it allows Weighted Parsimony.
 *
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class SankoffParsimony implements ParsimonyCriterion {

    private final int stateCount;

    private int[][] stateSets;
    private double[][][] nodeScores;
    private int[][] nodeStates;

    private Tree tree = null;
    private final Alignment patterns;
    private final double[][] costMatrix;

    private final boolean compressStates = true;

    private boolean hasCalculatedSteps = false;
    private boolean hasRecontructedStates = false;

    private final double[] siteScores;

    public SankoffParsimony(Alignment patterns) {
        if (patterns == null) {
            throw new IllegalArgumentException("The patterns cannot be null");
        }
        stateCount = patterns.getDataType().getStateCount();
        this.costMatrix = new double[stateCount][stateCount];
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                if (i == j) {
                    costMatrix[i][j] = 0.0;
                } else {
                    costMatrix[i][j] = 1.0;
                }
            }
        }

        this.patterns = patterns;
        this.siteScores = new double[patterns.getPatternCount()];
    }

    public SankoffParsimony(Alignment patterns, double[][] costMatrix) {
        if (patterns == null) {
            throw new IllegalArgumentException("The patterns cannot be null");
        }
        stateCount = patterns.getDataType().getStateCount();
        if (costMatrix.length != stateCount || costMatrix[0].length != stateCount) {
            throw new IllegalArgumentException("The cost matrix is of the wrong dimension: expecting " + stateCount + " square");
        }
        this.costMatrix = costMatrix;

        this.patterns = patterns;
        this.siteScores = new double[patterns.getPatternCount()];
    }

    /**
     * Calculates the minimum number of siteScores for the parsimony reconstruction of a
     * a set of character patterns on a tree.
     * @param tree a tree object to reconstruct the characters on
     * @return number of parsimony siteScores
     */
    public double[] getSiteScores(Tree tree) {

        if (tree == null) {
            throw new IllegalArgumentException("The tree cannot be null");
        }

        if (this.tree == null || this.tree != tree) {
            this.tree = tree;

            initialize();
        }

        if (!hasCalculatedSteps) {
            calculateSteps(tree, tree.getRoot(), patterns);
            if (compressStates) {
                for (int i = 0; i < siteScores.length; i++) {
                    double[] Sr = nodeScores[tree.getRoot().getNr()][i];
                    siteScores[i] = minScore(Sr, stateSets[i]);
                }
            } else {
                for (int i = 0; i < siteScores.length; i++) {
                    double[] Sr = nodeScores[tree.getRoot().getNr()][i];
                    siteScores[i] = minScore(Sr);
                }
            }
            hasCalculatedSteps = true;
        }


        return siteScores;
    }

    public double getScore(Tree tree) {

        getSiteScores(tree);

        double score = 0;

        for (int i = 0; i < patterns.getPatternCount(); i++) {
            score += siteScores[i] * patterns.getPatternWeight(i);
        }
        return score;
    }

    /**
     * Returns the reconstructed character nodeStates for a given node in the tree. If this method is repeatedly
     * called with the same tree and patterns then only the first call will reconstruct the nodeStates and each
     * subsequent call will return the stored nodeStates.
     * @param tree a tree object to reconstruct the characters on
     * @param node the node of the tree
     * @return an array containing the reconstructed nodeStates for this node
     */
    public int[] getStates(Tree tree, Node node) {

        getSiteScores(tree);

        if (!hasRecontructedStates) {
            if (compressStates) {
                for (int i = 0; i < patterns.getPatternCount(); i++) {
                    nodeStates[tree.getRoot().getNr()][i] = minState(nodeScores[tree.getRoot().getNr()][i], stateSets[i]);
                }
            } else {
                for (int i = 0; i < patterns.getPatternCount(); i++) {
                    nodeStates[tree.getRoot().getNr()][i] = minState(nodeScores[tree.getRoot().getNr()][i]);
                }
            }
            reconstructStates(tree, tree.getRoot(), nodeStates[tree.getRoot().getNr()]);
            hasRecontructedStates = true;
        }

        return nodeStates[node.getNr()];
    }

    private void initialize() {
        hasCalculatedSteps = false;
        hasRecontructedStates = false;

        if (compressStates) {
            stateSets = new int[patterns.getPatternCount()][];
        }

        nodeScores = new double[tree.getNodeCount()][patterns.getPatternCount()][];
        nodeStates = new int[tree.getNodeCount()][patterns.getPatternCount()];

        for (int i = 0; i < patterns.getPatternCount(); i++) {
            int[] pattern = patterns.getPattern(i);

            if (compressStates) {
                Set observedStates = new TreeSet();
                for (int j = 0; j < pattern.length; j++) {
                    boolean[] stateSet = patterns.getDataType().getStateSet(pattern[j]);
                    for (int k = 0; k < stateSet.length; k++) {
                        if (stateSet[k]) {
                            observedStates.add(new Integer(k));
                        }
                    }
                }

                stateSets[i] = new int[observedStates.size()];

                Iterator iter = observedStates.iterator();
                int j = 0;
                while (iter.hasNext()) {
                    stateSets[i][j] = ((Integer)iter.next()).intValue();
                    j++;
                }
            }

            for (int j = 0; j < tree.getLeafNodeCount(); j++) {
                Node node = tree.getNode(j);
                int state = pattern[patterns.getTaxonIndex(node.getID())];
                boolean[] stateSet = patterns.getDataType().getStateSet(state);

                nodeScores[j][i] = new double[stateCount];
                for (int k = 0; k < stateCount; k++) {
                    if (stateSet[k]) {
                        nodeScores[j][i][k] = 0.0;
                    } else {
                        nodeScores[j][i][k] = Double.POSITIVE_INFINITY;
                    }
                }
            }

            for (int j = 0; j < tree.getInternalNodeCount(); j++) {
                nodeScores[j + tree.getLeafNodeCount()][i] = new double[stateCount];
            }

        }
    }

    /**
     * This is the first pass of the Fitch algorithm. This calculates the set of nodeStates
     * at each node and counts the total number of siteScores (the score). If that is all that
     * is required then the second pass is not necessary.
     * @param tree
     * @param node
     * @param patterns
     */
    private void calculateSteps(Tree tree, Node node, Alignment patterns) {

        if (!node.isLeaf()) {

            for (int i = 0; i < node.getChildCount(); i++) {
                calculateSteps(tree, node.getChild( i), patterns);
            }

            if (compressStates) {
                for (int i = 0; i < patterns.getPatternCount(); i++) {
                    double[] Sc = nodeScores[node.getChild( 0).getNr()][i];
                    double[] Sa = nodeScores[node.getNr()][i];

                    int[] set = stateSets[i];
                    for (int k = 0; k < set.length; k++) {
                        Sa[set[k]] = minCost(k, Sc, costMatrix, set);
                    }

                    for (int j = 1; j < node.getChildCount(); j++) {
                        Sc = nodeScores[node.getChild( j).getNr()][i];
                        for (int k = 0; k < set.length; k++) {
                            Sa[set[k]] += minCost(k, Sc, costMatrix, set);
                        }

                    }


                }
            } else {
                for (int i = 0; i < patterns.getPatternCount(); i++) {
                    double[] Sc = nodeScores[node.getChild( 0).getNr()][i];
                    double[] Sa = nodeScores[node.getNr()][i];

                    for (int k = 0; k < stateCount; k++) {
                        Sa[k] = minCost(k, Sc, costMatrix);
                    }

                    for (int j = 1; j < node.getChildCount(); j++) {
                        Sc = nodeScores[node.getChild( j).getNr()][i];
                        for (int k = 0; k < stateCount; k++) {
                            Sa[k] += minCost(k, Sc, costMatrix);
                        }

                    }


                }

            }
        }
    }

    /**
     * The second pass of the algorithm. This reconstructs the ancestral nodeStates at
     * each node.
     * @param tree
     * @param node
     * @param parentStates
     */
    private void reconstructStates(Tree tree, Node node, int[] parentStates) {

        for (int i = 0; i < patterns.getPatternCount(); i++) {

            double[] Sa = nodeScores[node.getNr()][i];

            if (compressStates) {
                int[] set = stateSets[i];

                int minState = set[0];
                double minCost = Sa[minState] + costMatrix[parentStates[i]][minState];

                for (int j = 1; j < set.length; j++) {
                    double c = Sa[set[j]] + costMatrix[parentStates[i]][set[j]];
                    if (c < minCost) {
                        minState = set[j];
                        minCost = c;
                    }
                }
                nodeStates[node.getNr()][i] = minState;
            } else {
                int minState = 0;
                double minCost = Sa[minState] + costMatrix[parentStates[i]][minState];

                for (int j = 1; j < Sa.length; j++) {
                    double c = Sa[j] + costMatrix[parentStates[i]][j];
                    if (c < minCost) {
                        minState = j;
                        minCost = c;
                    }
                }
                nodeStates[node.getNr()][i] = minState;
            }

        }

        for (int i = 0; i < node.getChildCount(); i++) {
            reconstructStates(tree, node.getChild( i), nodeStates[node.getNr()]);
        }

    }


    private int minState(double[] s1) {

        int minState = 0;

        for (int j = 1; j < s1.length; j++) {
            if (s1[j] < s1[minState]) minState = j;
        }
        return minState;
    }

    private double minScore(double[] s1) {

        double minScore = s1[0];

        for (int j = 1; j < s1.length; j++) {
            if (s1[j] < minScore) minScore = s1[j];
        }
        return minScore;
    }

    private double minCost(int i, double[] s1, double[][] costMatrix) {

        double[] costRow = costMatrix[i];
        double minCost = costRow[0] + s1[0];

        for (int j = 1; j < s1.length; j++) {
            double cost = costRow[j] + s1[j];
            if (cost < minCost) minCost = cost;
        }
        return minCost;
    }

    private int minState(double[] s1, int[] set) {

        int minState = set[0];

        for (int j = 1; j < set.length; j++) {
            if (s1[set[j]] < s1[minState]) minState = set[j];
        }
        return minState;
    }

    private double minScore(double[] s1, int[] set) {

        double minScore = s1[set[0]];

        for (int j = 1; j < set.length; j++) {
            if (s1[set[j]] < minScore) minScore = s1[set[j]];
        }
        return minScore;
    }

    private double minCost(int i, double[] s1, double[][] costMatrix, int[] set) {

        double[] costRow = costMatrix[set[i]];
        double minCost = costRow[set[0]] + s1[set[0]];

        for (int j = 1; j < set.length; j++) {
            double cost = costRow[set[j]] + s1[set[j]];
            if (cost < minCost) minCost = cost;
        }
        return minCost;
    }

}