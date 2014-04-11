/*
 * Copyright (C) 2012 Tim Vaughan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package beast.evolution.tree;


import beast.util.NexusParser;

import java.io.File;
import java.io.PrintStream;
import java.util.*;


// TODO: Calculate mean node heights for trees in credible set.

/**
 * Partial re-implementation of TreeTraceAnalysis from BEAST 1.
 *
 * Represents an analysis of a list of trees obtained either directly
 * from a logger or from a trace file.  Currently only the 95% credible
 * set of tree topologies is calculated.
 *
 * @author Tim Vaughan
 * @author Walter Xie
 */
public class TreeTraceAnalysis {

    public static final double DEFAULT_CRED_SET = 0.95;
    public static final double DEFAULT_BURN_IN_PERCENTAGE = 0.1;

    protected List<Tree> treeList;
    protected double credSetProbability;
    protected int burnin;
    protected int totalTrees, totalTreesUsed;

    protected Map<String, Integer> topologyCounts;
    protected List<String> topologiesSorted;
    protected List<String> credibleSet;
    protected List<Integer> credibleSetFreqs;

    protected int credibleSetTotalFreq;


    public TreeTraceAnalysis() {   }

    public TreeTraceAnalysis(List<Tree> rawTreeList) {
        this(rawTreeList, DEFAULT_BURN_IN_PERCENTAGE);
    }

    public TreeTraceAnalysis(List<Tree> rawTreeList, double burninPercentage) {
        this(rawTreeList, burninPercentage, DEFAULT_CRED_SET);
    }

    public TreeTraceAnalysis(List<Tree> rawTreeList, double burninPercentage, double credSetProbability) {
        this.totalTrees = rawTreeList.size();
        this.burnin = getBurnIn(totalTrees, burninPercentage);
        this.totalTreesUsed = this.totalTrees-this.burnin;

        // Remove burnin from trace:
        treeList = getSubListOfTrees(rawTreeList, burnin, totalTrees);

        // Assemble credible set:
        this.credSetProbability = credSetProbability;

        analyzeTopologies();
        calculateCredibleSetFreqs();
    }

    public static int getBurnIn(int total, double burninPercentage) {
        // Record original list length and burnin for report:
        int burnin = (int)(total * burninPercentage / 100.0);
        assert burnin < total;
        return burnin;
    }

    /**
     * used to remove burn in
     * @param rawTreeList
     * @param start
     * @param end
     * @return
     */
    public static List<Tree> getSubListOfTrees(List<Tree> rawTreeList, int start, int end) {
        return new ArrayList<Tree>(rawTreeList.subList(start, end));
    }

    public void reportShort(PrintStream oStream) {
        oStream.println("burnin = " + String.valueOf(burnin));
        oStream.println("total trees used (total - burnin) = "
                + String.valueOf(totalTreesUsed));
    }

    /**
     * Generate report summarising analysis.
     *
     * @param oStream Print stream to write output to.
     */
    public void report(PrintStream oStream) {
        reportShort(oStream);

        oStream.print("\n" + String.valueOf(credSetProbability*100)
                + "% credible set");

        oStream.println(" (" + String.valueOf(credibleSet.size())
                + " unique tree topologies, "
                + String.valueOf(credibleSetTotalFreq)
                + " trees in total)");

        oStream.println("Count\tPercent\tRunning\tTree");
        double runningPercent = 0;
        for (int i=0; i<credibleSet.size(); i++) {
            double percent = 100.0*credibleSetFreqs.get(i)/(totalTrees-burnin);
            runningPercent += percent;

            oStream.print(credibleSetFreqs.get(i) + "\t");
            oStream.format("%.2f%%\t", percent);
            oStream.format("%.2f%%\t", runningPercent);
            oStream.println(credibleSet.get(i));
        }
    }

    /**
     * Analyse tree topologies.
     */
    protected void analyzeTopologies() {
        topologyCounts = new HashMap<String, Integer>();
        topologiesSorted = new ArrayList<String>();

        for (Tree tree : treeList) {
            String topology = uniqueNewick(tree.getRoot());
            if (topologyCounts.containsKey(topology))
                topologyCounts.put(topology, topologyCounts.get(topology)+1);
            else {
                topologyCounts.put(topology, 1);
                topologiesSorted.add(topology);
            }

        }

        Collections.sort(topologiesSorted, new Comparator<String>(){
            public int compare(String top1, String top2) {
                return topologyCounts.get(top2) - topologyCounts.get(top1);
            }
        });
    }

    protected void calculateCredibleSetFreqs() {
        credibleSetTotalFreq = 0;
        int totalFreq = treeList.size();

        credibleSet = new ArrayList<String>();
        credibleSetFreqs = new ArrayList<Integer>();

        for (String topo : topologiesSorted) {
            credibleSetTotalFreq += topologyCounts.get(topo);
            if (credibleSetTotalFreq>credSetProbability*totalFreq)
                break;
            credibleSet.add(topo);
            credibleSetFreqs.add(topologyCounts.get(topo));
        }
    }

    /**
     * Recursive function for constructing a Newick tree representation
     * in the given buffer.
     *
     * @param node
     * @return
     */
    protected String uniqueNewick(Node node) {
        if (node.isLeaf()) {
            return String.valueOf(node.getNr());
        } else {
            StringBuilder builder = new StringBuilder("(");

            List<String> subTrees = new ArrayList<String>();
            for (int i=0; i<node.getChildCount(); i++) {
                subTrees.add(uniqueNewick(node.getChild(i)));
            }

            Collections.sort(subTrees);

            for (int i=0; i<subTrees.size(); i++) {
                builder.append(subTrees.get(i));
                if (i<subTrees.size()-1) {
                    builder.append(",");
                }
            }
            builder.append(")");

            return builder.toString();
        }
    }

    /**
     * Obtain credible set of tree topologies
     *
     * @return List of tree topologies as Newick-formatted strings.
     */
    public List<String> getCredibleSet() {
        return credibleSet;
    }

    /**
     * Obtain frequencies with which members of the credible set appeared
     * in the original tree list.
     *
     * @return  List of absolute topology frequencies.
     */
    public List<Integer> getCredibleSetFreqs() {
        return credibleSetFreqs;
    }

    /**
     * Obtain total number of trees analysed (excluding burnin).
     *
     * @return Number of trees analysed.
     */
    public int getTotalTreesUsed() {
        return totalTreesUsed;
    }
    public Map<String,Integer> getTopologyCounts() {
        return topologyCounts;
    }

    public static void main(String[] args) {
        try {
            NexusParser parser = new NexusParser();
            parser.parseFile(new File(args[0]));
            TreeTraceAnalysis analysis = new TreeTraceAnalysis(parser.trees);
            analysis.report(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}