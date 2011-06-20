package beast.evolution.substitutionmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Substitution model dependent on clades. For each clade a substitution model is specified. " +
		"For nodes that do not fit in a clade a default substitution model is used.")
public class CladeSubstitutionModel extends SubstitutionModel.Base {
	public Input<SubstitutionModel> m_default = new Input<SubstitutionModel>("defaultModel","Default substitution model, that is, the model that applies when none of the other do", Validate.REQUIRED);
	public Input<Tree> m_tree = new Input<Tree>("tree", "tree for which to produce substitution models", Validate.REQUIRED);
	public Input<List<SubstitutionModel>> m_clademodel = new Input<List<SubstitutionModel>>("cladeModel","substitution model for each of the clades specified", new ArrayList<SubstitutionModel>());
	public Input<List<TaxonSet>> m_clades = new Input<List<TaxonSet>>("clades","set of taxa forming a clade and any branch in the clade will get a clade substitution model", new ArrayList<TaxonSet>());
	public Input<Alignment> m_taxa = new Input<Alignment>("taxa","set taxa to choose from", Validate.REQUIRED);

	/** contain default model (position 0) and clade models **/
	SubstitutionModel [] m_substitutionModels;
	List<Integer>[] m_nLeafNrs;
	
	/** maps node to a substitution model **/
	int [] m_nodeToModelMap;
	int [] m_storedNodeToModelMap;
	/** flag to indicate the nodeToModelMap is uptodate **/
	boolean m_bRecalc;

	
	public CladeSubstitutionModel() {
		frequenciesInput.setRule(Validate.OPTIONAL);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void initAndValidate() throws Exception{
    	super.initAndValidate();
		if (m_clades.get().size() != m_clademodel.get().size()) {
			throw new Exception("The number of clades should match the number of (non-default) substitution models");
		}
		
		m_substitutionModels = new SubstitutionModel[1+m_clademodel.get().size()];
		m_substitutionModels[0] = m_default.get();
		for (int i = 0; i < m_clademodel.get().size(); i++) {
			m_substitutionModels[i+1] = m_clademodel.get().get(i);
		}
		
		m_nLeafNrs = new List[m_clademodel.get().size()];
		
		// find node numbers for clades
		List<Sequence> data = m_taxa.get().m_pSequences.get();
		for (int i = 0; i < m_clades.get().size(); i++) {
			List<Taxon> set = m_clades.get().get(i).m_taxonset.get();
			m_nLeafNrs[i] = new ArrayList<Integer>();
			for (Taxon taxon : set) {
				String sLabel = taxon.getID();
				int iTaxon = 0;
				while (!data.get(iTaxon).m_sTaxon.get().equals(sLabel)) {
					iTaxon++;
					if (iTaxon == data.size()) {
						throw new Exception("Unknown taxon (" + sLabel + ") in clade number " + (i+1));
					}
				}
				m_nLeafNrs[i].add(iTaxon);
			}
		}
		
		m_nodeToModelMap = new int[m_tree.get().getNodeCount()];
		m_storedNodeToModelMap = new int[m_tree.get().getNodeCount()];
		calcNodeToModelMap(m_tree.get().getRoot());
		m_bRecalc = false;
	} // initAndValidate
	
    @Override
	public double[] getFrequencies() {
    	return m_default.get().getFrequencies();
	}

    @Override
	public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
		if (m_bRecalc) {
			calcNodeToModelMap(m_tree.get().getRoot());
			m_bRecalc = false;
		}
		int iModel = m_nodeToModelMap[node.getNr()];
		m_substitutionModels[iModel].getTransitionProbabilities(node, fStartTime, fEndTime, fRate, matrix);
	} // getTransitionProbabilities

	private void calcNodeToModelMap(Node node) {
		Arrays.fill(m_nodeToModelMap, 0);
		// process models one by one
		int nModelNr = 1;
		for (List<Integer> nIDs : m_nLeafNrs) {
			// label leafs
			for (Integer nID : nIDs) {
				m_nodeToModelMap[nID] = nModelNr;
			}
			labelInternalNodes(nIDs, nModelNr);
			nModelNr++;
		}
	} // calcNodeToModelMap
	
	void labelInternalNodes(List<Integer> nIDs, int nModelNr) {
		Tree tree = m_tree.get();
		Node [] nodes = tree.getNodesAsArray();
		// mark all leaf nodes as being part of the clade
		boolean [] isInClade = new boolean[nodes.length];
		for (Integer iID : nIDs) {
			isInClade[iID] = true;
		}
		Set<Integer> set = new HashSet<Integer>();
		set.addAll(nIDs);
		while (set.size() > 1) {
			// Find a node in the set that has a parent node with both
			// children in the clade constructed so far.
			// If no such node exists, the tree is not monophyletic
			Integer iNextNode = -1;
			for (Integer i : set) {
				Node node = nodes[i];
				Node parent = node.getParent();
				if (parent != null && isInClade[parent.m_left.getNr()] && isInClade[parent.m_right.getNr()]) {
					iNextNode = i;
					m_nodeToModelMap[parent.getNr()] = nModelNr;
					break;
				}
			}
			if (iNextNode >= 0) {
				// remove children from candidate set, and add parent
				Node parent = nodes[iNextNode].getParent();
				isInClade[parent.getNr()] = true;
				set.add(parent.getNr());
				set.remove(parent.m_left.getNr());
				set.remove(parent.m_right.getNr());
			} else {
				// no candidate found; this tree is not monophyletic
				return;
			}
		}
	} // labelInternalNodes
	
	
    /** CalculationNode methods **/
	@Override
	public boolean requiresRecalculation() {
		if (m_tree.isDirty()) {
			m_bRecalc = true;
		}
		for (SubstitutionModel model : m_substitutionModels) {
			if (((SubstitutionModel.Base) model).isDirtyCalculation()) {
				return true;
			}
		}
		return m_bRecalc;
	}

	@Override
	public void restore() {
		int [] tmp = m_nodeToModelMap;
		m_nodeToModelMap = m_storedNodeToModelMap;
		m_storedNodeToModelMap = tmp;
		super.restore();
	}

	@Override
	public void store() {
		System.arraycopy(m_nodeToModelMap, 0, m_storedNodeToModelMap, 0, m_nodeToModelMap.length);
		super.store();
	}
	
	
	@Override
	public EigenDecomposition getEigenDecomposition(Node node) {
		// cannot return EigenDecomposition for this substitution model
		return null;
	}

	@Override
	public boolean canHandleDataType(DataType dataType) throws Exception {
		if (m_substitutionModels != null) {
			return m_substitutionModels[0].canHandleDataType(dataType);
		}
		return true;
	}

} // class CladeSubstitutionModel
