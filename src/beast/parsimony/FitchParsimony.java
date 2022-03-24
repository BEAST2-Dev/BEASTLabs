/*
 * FitchParsimony.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */

package beast.parsimony;

import beast.base.core.Param;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeUtils;
import beast.parsimony.ParsimonyCriterion;

import java.util.HashMap;
import java.util.Map;

import beast.base.core.BEASTObject;
import beast.base.core.Description;

/**
 * Class for reconstructing characters using Fitch parsimony. This is intended to be much faster
 * than the static methods in the utility "Parsimony" class.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: FitchParsimony.java 604 2007-01-04 20:22:42Z msuchard $
 */
/** ported from jebl to BEAST 2 **/
@Description("Reconstructing characters using Fitch parsimony")
public class FitchParsimony extends BEASTObject implements ParsimonyCriterion {

	private final DataType sequenceType;
	private final int stateCount;
	private boolean gapsAreStates;

	private Map<Integer, boolean[][]> stateSets = new HashMap<>();
	private Map<Integer, int[]> states = new HashMap<>();

	private Tree tree = null;
	private Alignment patterns;

	private boolean hasCalculatedSteps = false;
	private boolean hasRecontructedStates = false;

	private final double[] siteScores;
	
	boolean[][] union;
	boolean[][] intersection;

	public FitchParsimony(
		@Param(name="patterns", description="auto converted jebl2 parameter") Alignment patterns,
		@Param(name="gapsAreStates", description="auto converted jebl2 parameter") Boolean gapsAreStates) {
		if (patterns == null || patterns.getPatternCount() == 0) {
			throw new IllegalArgumentException("The patterns cannot be null or empty");
		}

		this.sequenceType = patterns.getDataType();
		this.gapsAreStates = gapsAreStates;

		if (gapsAreStates) {
			stateCount = sequenceType.getStateCount() + 1;
		} else {
			stateCount = sequenceType.getStateCount();

		}

		this.patterns = patterns;

		this.siteScores = new double[patterns.getPatternCount()];
		
		
		this.union = new boolean[patterns.getPatternCount()][stateCount];
		this.intersection = new boolean[patterns.getPatternCount()][stateCount];
		
	}
	
	
	/**
	 * Flags so that the score will recalculated again next time getSiteScores is called
	 */
	public void reset() {
		hasCalculatedSteps = false;
		//hasRecontructedStates = false;
	}

	
	/**
	 * Calculates the minimum number of siteScores for the parsimony reconstruction of a
	 * a set of character patterns on a tree. This only does the first pass of the
	 * Fitch algorithm so it does not store ancestral state reconstructions.
	 *
	 * @param tree a tree object to reconstruct the characters on
	 * @return number of parsimony siteScores
	 */
	@Override
	public double[] getSiteScores(Tree tree) {

		if (tree == null) {
			throw new IllegalArgumentException("The tree cannot be null");
		}

		if (!(tree instanceof Tree)) {
			throw new IllegalArgumentException("The tree must be an instance of rooted tree");
		}

		if (this.tree == null || this.tree != tree) {
			this.tree = (Tree) tree;

//			if (!Utils.isBinary(this.tree)) {
//				throw new IllegalArgumentException("The Fitch algorithm can only reconstruct ancestral states on binary trees");
//			}

			initialize();
		}

		if (!hasCalculatedSteps) {
			for (int i = 0; i < siteScores.length; i++) {
				siteScores[i] = 0;
			}
			calculateSteps(this.tree);
			hasCalculatedSteps = true;
		}


		return siteScores;
	}

	@Override
	public double getScore(Tree tree) {

		getSiteScores(tree);

		double score = 0;
		for (int i = 0; i < patterns.getPatternCount(); i++) {
			score += siteScores[i] * patterns.getPatternWeight(i);
		}
		return score;
	}

	/**
	 * Returns the reconstructed character states for a given node in the tree. If this method is repeatedly
	 * called with the same tree and patterns then only the first call will reconstruct the states and each
	 * subsequent call will return the stored states.
	 *
	 * @param tree a tree object to reconstruct the characters on
	 * @param node the node of the tree
	 * @return an array containing the reconstructed states for this node
	 */
	@Override
	public int[] getStates(Tree tree, Node node) {

		getSiteScores(tree);

		if (!hasRecontructedStates) {
			reconstructStates(this.tree.getRoot(), null);
			hasRecontructedStates = true;
		}

		return states.get(node.getNr());
	}

	private void initialize() {
		hasCalculatedSteps = false;
		hasRecontructedStates = false;

		for (Node node : tree.getNodesAsArray()) {
			boolean[][] stateSet = new boolean[patterns.getPatternCount()][stateCount];
			stateSets.put(node.getNr(), stateSet);

			int[] stateArray = new int[patterns.getPatternCount()];
			states.put(node.getNr(), stateArray);
		}
	}

	
	
	
	/**
	 * This is the first pass of the Fitch algorithm. This calculates the set of states
	 * at each node and counts the total number of siteScores (the score). If that is all that
	 * is required then the second pass is not necessary.
	 */
	private void calculateSteps(Tree tree) {

		// nodes in pre-order
		final int [] nodes = new int[tree.getNodeCount()];
		
		TreeUtils.preOrderTraversalList(tree, nodes);// Utils.getNodes(tree, tree.getRootNode());

		// Iterate in reverse - post order. State of child is guaranteed to be ready before parent
		for (int k = nodes.length - 1; k >= 0; --k) {
			final Node node = tree.getNode(nodes[k]);
			final boolean[][] nodeStateSet = stateSets.get(node.getNr());

			if (node.isLeaf()) {
				boolean[][] stateSet = stateSets.get(node.getNr());
				int[] stateArray = states.get(node.getNr());

				for (int i = 0; i < patterns.getPatternCount(); ++i) {
					int [] pattern = patterns.getPattern(i);

					//Taxon taxon = tree.getTaxon(node.getNr());
					int index = node.getNr();//taxa.indexOf(taxon);

					//if (index == -1)
					//	throw new IllegalArgumentException("Unknown taxon, " + taxon.getName() + " in tree");

					int state = pattern[index];
					stateArray[i] = state;
					if (gapsAreStates && state < 0) { //.isGap()) {
						stateSet[i][stateCount - 1] = true;
					} else {
						boolean [] stateSet0 = sequenceType.getStateSet(state);
						for (int j = 0; j < stateSet0.length; j++) {
							stateSet[i][j] = stateSet0[j];
						}
					}
				}
			} else {
				boolean first = true;
				for (Node child : node.getChildren()) {
					boolean[][] childStateSet = stateSets.get(child.getNr());
					if (first) {
						for (int i = 0; i < patterns.getPatternCount(); i++) {
							copyOf(childStateSet[i], union[i]);
							copyOf(childStateSet[i], intersection[i]);
						}
						first = false;
					} else {
						for (int i = 0; i < patterns.getPatternCount(); i++) {
							unionOf(union[i], childStateSet[i], union[i]);
							intersectionOf(intersection[i], childStateSet[i], intersection[i]);
						}
					}
				}

				for (int i = 0; i < patterns.getPatternCount(); i++) {
					if (sizeOf(intersection[i]) > 0) {
						copyOf(intersection[i], nodeStateSet[i]);
					} else {
						copyOf(union[i], nodeStateSet[i]);
						siteScores[i]++;
					}
				}
			}
		}
	}


//	private String printState(boolean[][] stateSet) {
//		StringBuffer sb = new StringBuffer();
//		for (int i = 0, n = stateSet.length; i < n; i++) {
//			sb.append("site " + i);
//			for (int j = 0, l = stateSet[i].length; j < l; j++) {
//				sb.append(" " + (stateSet[i][j] ? "T" : "F"));
//			}
//			sb.append("\n");
//		}
//		return sb.toString();
//	}


//	private String printState(boolean[] stateSet) {
//		StringBuffer sb = new StringBuffer();
////			for(int i=0,n=stateSet.length; i<n; i++) {
////		int i = 0;
////				sb.append("site "+i);
//		for (int j = 0, l = stateSet.length; j < l; j++) {
//			sb.append(" " + (stateSet[j] ? "T" : "F"));
//		}
////				sb.append("\n");
////			}
//		return sb.toString();
//	}


	/**
	 * The second pass of the Fitch algorithm. This reconstructs the ancestral states at
	 * each node.
	 *
	 * @param node
	 * @param parentStates
	 */
	private void reconstructStates(Node node, int [] parentStates) {

		if (!node.isLeaf()) {
			boolean[][] nodeStateSet = stateSets.get(node.getNr());
			int [] nodeStates = states.get(node.getNr());

			for (int i = 0; i < patterns.getPatternCount(); i++) {

				if (parentStates != null && nodeStateSet[i][parentStates[i]]) {
					nodeStates[i] = parentStates[i];
				} else {
					int first = firstIndexOf(nodeStateSet[i]);
					nodeStates[i] = first; //sequenceType.getState(first);
				}
			}

			for (Node child : node.getChildren()) {
				reconstructStates(child, nodeStates);
			}
		}
	}

	private static void copyOf(boolean[] s, boolean[] d) {

		for (int i = 0; i < d.length; i++) {
			d[i] = s[i];
		}
	}

	private static void unionOf(boolean[] s1, boolean[] s2, boolean[] d) {

		for (int i = 0; i < d.length; i++) {
			d[i] = s1[i] || s2[i];
		}
	}

	private static void intersectionOf(boolean[] s1, boolean[] s2, boolean[] d) {

		for (int i = 0; i < d.length; i++) {
			d[i] = s1[i] && s2[i];
		}
	}

	private static int firstIndexOf(boolean[] s1) {

		for (int i = 0; i < s1.length; i++) {
			if (s1[i]) {
				return i;
			}
		}
		return -1;
	}

	private static int sizeOf(boolean[] s1) {

		int count = 0;
		for (int i = 0; i < s1.length; i++) {
			if (s1[i]) count += 1;
		}
		return count;
	}

	@Override
	public void initAndValidate() {
		// nothing to do
	}


	public Boolean getGapsAreStates() {
		return gapsAreStates;
	}

	/** should not be used other than by BEAST framework **/
	@Deprecated
	public void setGapsAreStates(Boolean gapsAreStates) {
		this.gapsAreStates = gapsAreStates;
	}

	public Alignment getPatterns() {
		return patterns;
	}

	/** should not be used other than by BEAST framework **/
	@Deprecated
	public void setPatterns(Alignment patterns) {
		this.patterns = patterns;
	}
}