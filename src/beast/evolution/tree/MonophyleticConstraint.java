package beast.evolution.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;

@Description("Enforces groups of taxa to be monophyletic -- have a common mrca that " +
		"no other taxa have. This can be used as part of the prior.")
public class MonophyleticConstraint extends Distribution {
	public Input<Tree> m_tree = new Input<Tree>("tree","tree to apply the constraint on", Validate.REQUIRED);
	public Input<List<TaxonSet>> m_set = new Input<List<TaxonSet>>("set","set taxa that should be monophyletic", new ArrayList<TaxonSet>(), Validate.REQUIRED);
	public Input<Alignment> m_taxa = new Input<Alignment>("taxa","set taxa to choose from", Validate.REQUIRED);

	
	List<Integer>[] m_nIDs;
	
	@SuppressWarnings("unchecked")
	@Override
	public void initAndValidate() throws Exception {
		List<TaxonSet> taxonsets = m_set.get();
		m_nIDs = new ArrayList[taxonsets.size()];
		List<Sequence> data = m_taxa.get().m_pSequences.get();
		
		for (int i = 0; i < taxonsets.size(); i++) {
			TaxonSet taxonset = taxonsets.get(i);
			m_nIDs[i] = new ArrayList<Integer>();
			List<Taxon> set = taxonset.m_taxonset.get();
			for (Taxon taxon : set) {
				String sLabel = taxon.getID();
				int iTaxon = 0;
				while (!data.get(iTaxon).m_sTaxon.get().equals(sLabel)) {
					iTaxon++;
					if (iTaxon == data.size()) {
						throw new Exception("Unknown taxon (" + sLabel + ") in set (typo?)");
					}
				}
				m_nIDs[i].add(iTaxon);
			}

		}
		
	} // initAndValidate
	
	@Override
    public double calculateLogP() throws Exception {
		for (int i = 0; i < m_nIDs.length; i++) {
			if (!isMonoPhyletic(m_nIDs[i])) {
				return Double.NEGATIVE_INFINITY;
			}
		}
		return 0.0;
	}

	/** Check whether the tree is monophyletic in the set of nodes
	 * represented by m_nIDs.
	 */
	boolean isMonoPhyletic(List<Integer> nIDs) {
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
				return false;
			}
		}
		return true;
	}
	
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
	@Override public void sample(State state, Random random) {}
}
