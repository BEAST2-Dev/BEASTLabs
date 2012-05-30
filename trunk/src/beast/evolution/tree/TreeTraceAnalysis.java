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

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import java.io.OutputStream;
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
 */
public class TreeTraceAnalysis {

	private List<Tree> treeList;
	private double credSetProbability;
	private int burnin;
	private int totalTrees, totalTreesUsed;

	private Map<String, Integer> topologyCounts;
	private List<String> topologiesSorted;
	private List<String> credibleSet;
	private List<Integer> credibleSetFreqs;

	private int credibleSetTotalFreq;

	public TreeTraceAnalysis(List<Tree> rawTreeList, double burninPercentage,
			double credSetProbability) {

		// Remove burnin from trace:
		treeList = new ArrayList<Tree>();
		for (int i=0; i<rawTreeList.size(); i++) {
			if (i<burnin)
				continue;

			treeList.add(rawTreeList.get(i));
		}

		// Record original list length and burnin for report:
		this.burnin = (int)(rawTreeList.size()*burninPercentage/100.0);
		this.totalTrees = rawTreeList.size();
		this.totalTreesUsed = this.totalTrees-this.burnin;

		// Assemble credible set:
		this.credSetProbability = credSetProbability;
		analyzeTopologies();
	}

	/**
	 * Generate report summarising analysis.
	 * 
	 * @param oStream Print stream to write output to.
	 */
	public void report(PrintStream oStream) {

		oStream.println("burnin = " + String.valueOf(burnin));
		oStream.println("total trees used (total - burnin) = "
				+ String.valueOf(totalTreesUsed));

		oStream.print("\n" + String.valueOf(100*credSetProbability)
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
	private void analyzeTopologies() {

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

		credibleSetTotalFreq = 0;
		int totalFreq = treeList.size();

		credibleSet = new ArrayList<String>();
		credibleSetFreqs = new ArrayList<Integer>();

		for (String topo : topologiesSorted) {
			credibleSet.add(topo);
			credibleSetFreqs.add(topologyCounts.get(topo));

			credibleSetTotalFreq += topologyCounts.get(topo);
			if (credibleSetTotalFreq>credSetProbability*totalFreq)
				break;
		}
	}

	/**
	 * Recursive function for constructing a Newick tree representation
	 * in the given buffer.
	 * 
	 * @param node
	 * @return 
	 */
	private String uniqueNewick(Node node) {

		if (node.isLeaf()) {
			return String.valueOf(node.getNr());
		} else {
			StringBuilder builder = new StringBuilder("(");

			List<String> subTrees = new ArrayList<String>();
			for (int i=0; i<node.getChildCount(); i++)
				subTrees.add(uniqueNewick(node.getChild(i)));

			Collections.sort(subTrees);

			for (int i=0; i<subTrees.size(); i++) {
				builder.append(subTrees.get(i));
				if (i<subTrees.size()-1)
					builder.append(",");
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

}