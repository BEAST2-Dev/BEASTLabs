/*
 * FitchParsimony.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */

package beastlabs.parsimony;

import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeUtils;
import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;

/**
 * Class for reconstructing characters using Fitch parsimony. This is intended to be much faster
 * than the static methods in the utility "Parsimony" class.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: FitchParsimony.java 604 2007-01-04 20:22:42Z msuchard $
 */
/** ported from jebl to BEAST 2 **/
/** optimised for bit operations **/
@Description("Reconstructing characters using Fitch parsimony")
public class FitchParsimony32 extends BEASTObject implements ParsimonyCriterion {

    final public Input<Alignment> dataInput = new Input<>("data", "sequence data for the beast.tree", Validate.REQUIRED);
    final public Input<Boolean> gapsAreStatesInput = new Input<>("gapsAreState", "whether to consider a gap as a seperate state", false);

    private DataType sequenceType;
	private int stateCount, patternCount;
	private boolean gapsAreStates;

	private int[][] stateSets;
	private int[][] states;

	private Tree tree = null;
	private Alignment patterns;

	private boolean hasCalculatedSteps = false;
	private boolean hasRecontructedStates = false;

	private double[] siteScores;
	
	int [] union;
	int [] intersection;
	int mask;

	public FitchParsimony32() {
		
	}
	
	public FitchParsimony32(
		Alignment patterns,
		Boolean gapsAreStates) {
		if (patterns == null || patterns.getPatternCount() == 0) {
			throw new IllegalArgumentException("The patterns cannot be null or empty");
		}
		initByName("data", patterns, "gapsAreState", gapsAreStates);
	}
	
	@Override
	public void initAndValidate() {
		
		this.patterns = dataInput.get();
		
		this.sequenceType = patterns.getDataType();

		this.gapsAreStates = gapsAreStatesInput.get();;

		if (gapsAreStates) {
			stateCount = sequenceType.getStateCount() + 1;
		} else {
			stateCount = sequenceType.getStateCount();
		}
				
		if (stateCount > 32) {
			throw new IllegalArgumentException("At most 32 states can be handled by this implementation");
		}
		
		mask = 0;
		for (int i = 0; i < stateCount; i++) {
			mask |= (1<<i);
		}
		
		this.siteScores = new double[patterns.getPatternCount()];
		
		patternCount = patterns.getPatternCount();
		int taxonCount = patterns.getTaxonCount();
		int nodeCount = taxonCount * 2 - 1;
		this.union = new int[patternCount];
		this.intersection = new int[patternCount];
		
		
		states = new int[nodeCount][patternCount];
		stateSets = new int[nodeCount][patternCount];
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
	public int[] getStates(Tree tree, Node node) {

		getSiteScores(tree);

		if (!hasRecontructedStates) {
			reconstructStates(this.tree.getRoot(), null);
			hasRecontructedStates = true;
		}

		return states[node.getNr()];
	}

	private void initialize() {
		hasCalculatedSteps = false;
		hasRecontructedStates = false;
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
			final int[] nodeStateSet = stateSets[node.getNr()];

			if (node.isLeaf()) {
				int[] stateSet = stateSets[node.getNr()];
				int[] stateArray = states[node.getNr()];

				for (int i = 0; i < patterns.getPatternCount(); ++i) {
					int [] pattern = patterns.getPattern(i);

					//Taxon taxon = tree.getTaxon(node.getNr());
					int index = node.getNr();//taxa.indexOf(taxon);

					//if (index == -1)
					//	throw new IllegalArgumentException("Unknown taxon, " + taxon.getName() + " in tree");

					int state = pattern[index];
					stateArray[i] = state;
					if (state < 0) { //.isGap()) {
						if (gapsAreStates) {
							stateSet[i] = 1<<(stateCount-1);
						} else {
							stateSet[i] = mask;
						}
					} else {
						stateSet[i] = (1 << state);
					}
				}
			} else {
				boolean first = true;
				for (Node child : node.getChildren()) {
					int[] childStateSet = stateSets[child.getNr()];
					if (first) {
						System.arraycopy(childStateSet, 0, union, 0, patternCount);
						System.arraycopy(childStateSet, 0, intersection, 0, patternCount);
						first = false;
					} else {
						for (int i = 0; i < patterns.getPatternCount(); i++) {
							union[i] |= childStateSet[i];
							intersection[i] &= childStateSet[i];
						}
					}
				}

				for (int i = 0; i < patterns.getPatternCount(); i++) {
					if (Integer.bitCount(intersection[i] & mask) > 0) {
						nodeStateSet[i] = intersection[i];
					} else {
						nodeStateSet[i] = union[i];
						siteScores[i]++;
					}
				}
			}
		}
	}


	/**
	 * The second pass of the Fitch algorithm. This reconstructs the ancestral states at
	 * each node.
	 *
	 * @param node
	 * @param parentStates
	 */
	private void reconstructStates(Node node, int [] parentStates) {

		if (!node.isLeaf()) {
			int [] nodeStateSet = stateSets[node.getNr()];
			int [] nodeStates = states[node.getNr()];

			for (int i = 0; i < patterns.getPatternCount(); i++) {

				if (parentStates != null && (nodeStateSet[i] & (1<<parentStates[i])) > 0) {
					nodeStates[i] = parentStates[i];
				} else {
					int first = Integer.numberOfTrailingZeros(nodeStateSet[i]);
					nodeStates[i] = first; 
				}
			}

			for (Node child : node.getChildren()) {
				reconstructStates(child, nodeStates);
			}
		}
	}

}