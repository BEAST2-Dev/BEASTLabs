package beast.evolution.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.SpeciesTreePrior.PopSizeFunction;

@Description("Calculates probability of gene tree conditioned on a species tree (as in *BEAST)")
public class GeneTreeForSpeciesTreeDistribution extends Distribution {
	public Input<Tree> m_speciesTree = new Input<Tree>("speciesTree", "species tree containing the associated gene tree", Validate.REQUIRED);
	public Input<Tree> m_geneTree = new Input<Tree>("geneTree", "gene tree for which to calculate probability conditioned on the species tree", Validate.REQUIRED);
	public Input<List<TaxonSet>> m_taxonSet = new Input<List<TaxonSet>>("taxonset", "set of taxa mapping lineages to species", new ArrayList<TaxonSet>(), Validate.REQUIRED);

	public Input<SpeciesTreePrior> m_popInfo = new Input<SpeciesTreePrior>("speciesTreePrior","defines population function and its parameters", Validate.REQUIRED);
	
	// intervals for each of the species tree branches
	PriorityQueue<Double> [] m_intervals;
	// count nr of lineages at the bottom of species tree branches
	int [] m_nLineages;
	// maps gene tree leaf nodes to species tree leaf nodes
	int [] m_nLineageToSpeciesMap;
	
	PopSizeFunction m_bIsConstantPopFunction;
	RealParameter m_fPopSizesBottom;
	RealParameter m_fPopSizesTop;
	
	@Override 
	public void initAndValidate() throws Exception {
		Node [] nodes = m_geneTree.get().getNodesAsArray();
		int nLineages = m_geneTree.get().getLeafNodeCount();
		Node [] nodes2 = m_speciesTree.get().getNodesAsArray();
		int nSpecies = m_speciesTree.get().getNodeCount();
		
		// reserve memory for priority queues
		m_intervals = new PriorityQueue[nSpecies];
		for (int i = 0; i < nSpecies; i++) {
			m_intervals[i] = new PriorityQueue<Double>();
		}

		// sanity check lineage nodes are all at height=0
		for (int i = 0; i < nLineages; i++) {
			if (nodes[i].getHeight() != 0) {
				throw new Exception("Cannot deal with taxon " + nodes[i].getID() + ", which has non-zero height + " + nodes[i].getHeight());
			}
		}		
		// set up m_nLineageToSpeciesMap 
		m_nLineageToSpeciesMap = new int[nLineages];
		Arrays.fill(m_nLineageToSpeciesMap, -1);
		for (int i = 0; i < nLineages; i++) {
			String sSpeciesID = getSetID(nodes[i].getID());
			if (sSpeciesID == null) {
				throw new Exception("Cannot find species for lineage taxon " + nodes[i].getID());
			}
			for (int iSpecies = 0; iSpecies < nSpecies; iSpecies++) {
				if (sSpeciesID.equals(nodes2[iSpecies].getID())) {
					m_nLineageToSpeciesMap[i] = iSpecies;	
				}
			}
			if (m_nLineageToSpeciesMap[i] < 0) {
				throw new Exception("Cannot find species with name " + sSpeciesID +" in species tree");
			}
		}
		
		// calculate nr of lineages per species 
		m_nLineages = new int[nSpecies];
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].isLeaf()) {
				int iSpecies = m_nLineageToSpeciesMap[nodes[i].getNr()];
				m_nLineages[iSpecies]++;
			}
		}
		
		SpeciesTreePrior popInfo = m_popInfo.get();
		m_bIsConstantPopFunction = popInfo.m_popFunctionInput.get();
		m_fPopSizesBottom = popInfo.m_popSizesBottom.get();
		m_fPopSizesTop = popInfo.m_popSizesTop.get();
	}

	/** find species ID to which the lineage ID belongs according to the TaxonSets **/
	String getSetID(String sLineageID) {
		List<TaxonSet> taxonSets = m_taxonSet.get();
		for (TaxonSet taxonSet : taxonSets) {
			List<Taxon> taxa = taxonSet.m_taxonset.get();
			for (int i = 0; i < taxa.size(); i++) {
				if (taxa.get(i).getID().equals(sLineageID)) {
					return taxonSet.getID();
				}
			}
		}
		return null;
	}

	
	@Override
	public double calculateLogP() {
		logP = 0;
		for (int i = 0; i < m_intervals.length; i++) {
			m_intervals[i].clear();
		}
		Node [] speciesNodes = m_speciesTree.get().getNodesAsArray();
		traverseLineageTree(speciesNodes, m_geneTree.get().getRoot());
		// if the gene tree does not fit the species tree, logP = -infinity by now
		if (logP == 0) {
			traverseSpeciesTree(m_speciesTree.get().getRoot());
		}
		return 0; 
	}
	
	/** calculate contribution to logP for each of the branches of the species tree **/
	private void traverseSpeciesTree(Node node) {
		if (!node.isLeaf()) {
			traverseSpeciesTree(node.m_left);
			traverseSpeciesTree(node.m_right);
		}
		// calculate contribution of a branch in the species tree to the log probability
		int iNode = node.getNr();

		// k, as defined in the paper
		int k = m_intervals[iNode].size();
		double [] fTimes = new double[k+2];
		fTimes[0] = node.getHeight();
		for (int i = 1; i <= k; i++) {
			fTimes[i] = m_intervals[iNode].poll();
		}
		fTimes[k+1] = node.getParent().getHeight(); 
		
		switch (m_bIsConstantPopFunction) {
		case constant:
			calcConstantPopSizeContribution(iNode, fTimes, k);
			break;
		case linear:
			calcLinearPopSizeContribution(iNode, fTimes, k, node);
			break;
		case linear_with_constant_root:
			if (node.isRoot()) {
				calcConstantPopSizeContribution(iNode, fTimes, k);
			} else {
				calcLinearPopSizeContribution(iNode, fTimes, k, node);
			}
			break;
		}
	}
	
	/* the contribution of a branch in the species tree to
	 * the log probability, for constant population function.
	 */
	private void calcConstantPopSizeContribution(int iNode, double[] fTimes, int k) {
		int nLineagesBottom = m_nLineages[iNode];
		double fPopSize = m_fPopSizesBottom.getValue(iNode);
		logP += - k * Math.log(fPopSize);
		for (int i = 0; i <= k; i++) {
			logP += -((nLineagesBottom - i) * (nLineagesBottom-i - 1.0) / 2.0) * (fTimes[i+1]-fTimes[i]) / fPopSize;
		}
	}

	/* the contribution of a branch in the species tree to
	 * the log probability, for linear population function.
	 */
	private void calcLinearPopSizeContribution(int iNode, double[] fTimes, int k, Node node) {
		int nLineagesBottom = m_nLineages[iNode];
		double fPopSizeBottom;
		if (node.isLeaf()) {
			fPopSizeBottom = m_fPopSizesBottom.getValue(iNode);
		} else {
			// use sum of left and right child branches for internal nodes
			fPopSizeBottom = m_fPopSizesTop.getValue(node.m_left.getNr()) + 
				m_fPopSizesTop.getValue(node.m_right.getNr());
		}
		double fPopSizeTop = m_fPopSizesTop.getValue(iNode);
		double a = (fPopSizeTop-fPopSizeBottom)/(fTimes[k]-fTimes[0]);
		double b = fPopSizeBottom; 
		for (int i = 0; i < k; i++) {
			//double fPopSize = fPopSizeBottom + (fPopSizeTop-fPopSizeBottom) * fTimes[i+1]/(fTimes[k]-fTimes[0]);
			double fPopSize = a*(fTimes[i+1]-fTimes[0])+b;
			logP += - Math.log(fPopSize);
		}
		for (int i = 0; i <= k; i++) {
			logP += -((nLineagesBottom - i) * (nLineagesBottom-i - 1.0) / 2.0) * 
				Math.log((a*(fTimes[i+1]-fTimes[0])+b)/(a*(fTimes[i]-fTimes[0])+b)) / a; 
		}
	}

	/** collect intervals for each of the branches of the species tree
	 * as defined by the lineage tree.
	 */
	private int traverseLineageTree(Node[] speciesNodes, Node node) {
		if (node.isLeaf()) {
			return m_nLineageToSpeciesMap[node.getNr()];
		} else {
			int nSpeciesLeft = traverseLineageTree(speciesNodes, node.m_left);
			int nSpeciesRight = traverseLineageTree(speciesNodes, node.m_right);
			double fHeight = node.getHeight();
			
			if (nSpeciesLeft != nSpeciesRight) {
				while (fHeight > speciesNodes[nSpeciesLeft].getHeight()) {
					nSpeciesLeft =  speciesNodes[nSpeciesLeft].getParent().getNr();
					m_nLineages[nSpeciesLeft]++;
				}
				m_intervals[nSpeciesLeft].add(node.getHeight());
			}
			while (fHeight > speciesNodes[nSpeciesRight].getHeight()) {
				nSpeciesRight =  speciesNodes[nSpeciesRight].getParent().getNr();
				m_nLineages[nSpeciesRight]++;
			}
			m_intervals[nSpeciesRight].add(node.getHeight());
			// sanity check
			if (nSpeciesLeft != nSpeciesRight) {
				// if we got here, it means the gene tree does 
				// not fit in the species tree
				logP = Double.NEGATIVE_INFINITY;
			}
			return nSpeciesRight;
		}
	}

	@Override
	public boolean requiresRecalculation() {
		return true;
	}
	
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
	@Override public void sample(State state, Random random) {}
}
