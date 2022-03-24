package beast.evolution.speciation;

import beast.base.core.Description;
import beast.base.evolution.speciation.SpeciesTreeDistribution;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;

@Description("Experimental code with unknown use. If you know, fill this in.")
public abstract class UltrametricSpeciationModel extends SpeciesTreeDistribution {

    public UltrametricSpeciationModel() {
        super();
    }

    @Override
    public double calculateTreeLogLikelihood(final TreeInterface tree) {        
        final int taxonCount = tree.getLeafNodeCount();
            
        double logL = logTreeProbability(taxonCount);
    
        final Node [] nodes = tree.getNodesAsArray();
        for (int i = taxonCount; i < nodes.length; i++) { // exclude tips
            assert ( ! nodes[i].isLeaf() );
            logL += logNodeProbability(nodes[i], taxonCount);
        }
        
        if (includeExternalNodesInLikelihoodCalculation()) { 
            for (int i = 0; i < taxonCount; i++) { // + logL of tips
                logL += logNodeProbability(nodes[i], taxonCount);
            }
        }
    
        return logL;
    }

    /** calculate contribution of the tree to the log likelihood
     *
     * @param taxonCount     
     * @return
     **/
    protected abstract double logTreeProbability(final int taxonCount);

    /** contribution of a single node to the log likelihood     
    *
     * @param node     
     * @param taxonCount
     * @return
     **/
    protected abstract double logNodeProbability(Node node, int taxonCount);
    
    /**
     * @return true if calls to logNodeProbability for terminal nodes (tips) are required
     */
    public abstract boolean includeExternalNodesInLikelihoodCalculation();


}