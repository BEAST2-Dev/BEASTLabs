package beast.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.core.Description;
import beast.core.DataNode;
import beast.core.Distribution;
import beast.core.BEASTObject;
import beast.core.util.CompoundDistribution;



@Description("Represents one of more likelihoods which represent the " +
		"probability of some data (e.g. a sequence alignment), possibly conditioned on " +
		"one or more DataNodes (e.g. a Tree or set of Parameters). " +
		"The class checks that Data has not defined more than one likelihood.")
public class Likelihood extends CompoundDistribution {
	
	@Override
    public void initAndValidate() throws Exception {
        // Check no DataNode has two likelihoods defined for it
		// first, find all state nodes that have any references
		List<Distribution> likelihoods = pDistributions.get();
		int nLikelihoods = likelihoods.size();
		Set<DataNode> [] DataNodesPerLikelihood = new HashSet[nLikelihoods];
		int k = 0;
		for (Distribution distribution : likelihoods) {
			DataNodesPerLikelihood[k] = new HashSet<DataNode>();
			collectDataNodes(distribution, DataNodesPerLikelihood[k]);
			// sanity check
			if (DataNodesPerLikelihood[k].size() == 0) {
				throw new Exception("Likelihood (id=" + likelihoods.get(k).getID() + ") does not cover any DataNode. " +
						"This indicates the model is not valid.");
			}
		}
		
		// second, determine whether state nodes can be partitioned
		// For every prior, at least one extra DataNode needs to be considered
		Set<DataNode> DataNodes = new HashSet<DataNode>();
		DataNodes.addAll(DataNodesPerLikelihood[0]);
		for (k = 1; k < nLikelihoods; k++) {
			if (DataNodes.containsAll(DataNodesPerLikelihood[k])) {
				throw new Exception("Likelihood (id=" + likelihoods.get(k).getID() + ") does not cover a DataNode exclusively. " +
						"This indicates the model is not valid.");
			}
			DataNodes.addAll(DataNodesPerLikelihood[k]);
		}
		
    } // initAndValidate

	void collectDataNodes(BEASTInterface plugin, Set<DataNode> DataNodes) throws Exception {
		for (BEASTInterface o : plugin.listActivePlugins()) {
			if (o instanceof DataNode) {
				DataNodes.add((DataNode) o);
			} else {
				collectDataNodes(o, DataNodes);
			}
		}
	} // collectDataNodes
	
	
}
