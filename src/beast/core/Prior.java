package beast.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.core.util.CompoundDistribution;

@Description("Represents a list of prior distribution over a StateNode, " +
		"each potentially conditioned on one or more other StateNodes. " +
		"The class checks that no StateNode has more than one prior defined " +
		"for it.")
public class Prior extends CompoundDistribution {

	@Override
    public void initAndValidate() {
        // Check no StateNode has two priors defined for it
		// first, find all state nodes that have any references
		List<Distribution> priors = pDistributions.get();
		int nPriors = priors.size();
		Set<StateNode> [] stateNodesPerPrior = new HashSet[nPriors];
		int k = 0;
		for (Distribution distribution : priors) {
			stateNodesPerPrior[k] = new HashSet<StateNode>();
			collectStateNodes(distribution, stateNodesPerPrior[k]);
			// sanity check
			if (stateNodesPerPrior[k].size() == 0) {
				throw new IllegalArgumentException("Prior (id=" + priors.get(k).getID() + ") does not cover any StateNode. " +
						"This indicates the model is not valid.");
			}
		}
		
		// second, determine whether state nodes can be partitioned
		// For every prior, at least one extra StateNode needs to be considered
		Set<StateNode> stateNodes = new HashSet<StateNode>();
		stateNodes.addAll(stateNodesPerPrior[0]);
		for (k = 1; k < nPriors; k++) {
			if (stateNodes.containsAll(stateNodesPerPrior[k])) {
				throw new IllegalArgumentException("Prior (id=" + priors.get(k).getID() + ") does not cover a StateNode exclusively. " +
						"This indicates the model is not valid.");
			}
			stateNodes.addAll(stateNodesPerPrior[k]);
		}
		
    } // initAndValidate

	void collectStateNodes(BEASTInterface plugin, Set<StateNode> stateNodes) {
		for (BEASTInterface o : plugin.listActiveBEASTObjects()) {
			if (o instanceof StateNode) {
				stateNodes.add((StateNode) o);
			} else {
				collectStateNodes(o, stateNodes);
			}
		}
	} // collectStateNodes

}
