package beastlabs.math.distributions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.Node;

@Description("Allow some rogue taxa to enter a monophyletic constraint")
public class MRCAPriorWithRogues extends MRCAPrior {
	final public Input<TaxonSet> rogueTaxonSetInput = new Input<>("rogues", "set of taxa that may or may not part of the by "
			+ "monophyly constrained clade", Validate.REQUIRED);
	
	boolean [] isRogue;
	
	// used to count nr of rogues under MRCA
	int nrogues;
	
	@Override
	public void initAndValidate() {
		if (taxonsetInput.get() == null) {
			throw new IllegalArgumentException("taxonset input must be specified");
		}
		if (!isMonophyleticInput.get()) {
			throw new IllegalArgumentException("monophyletic input must be true");
		}
		super.initAndValidate();
	}
	
    // A lightweight version for finding the most recent common ancestor of a group of taxa.
    // return the node-ref of the MRCA.

    // would be nice to use nodeRef's, but they are not preserved :(
    @Override
    public Node getCommonAncestor() {
        Node cur = tree.getNode(taxonIndex[0]);
        for (int k = 1; k < taxonIndex.length; ++k) {
            cur = getCommonAncestor(cur, tree.getNode(taxonIndex[k]));
        }
        nseen = 0;
        collectRogues(cur);
        return cur;
    }
    
    private void collectRogues(Node node) {
    	nseen++;
    	if (isRogue[node.getNr()]) {
    		nrogues++;
    	}
    	for (Node child : node.getChildren()) {
    		collectRogues(child);
    	}
	}

	@Override
    public double calculateLogP() {
    	if (!initialised) {
    		initialise();
    	}
    	Node m;
    	if (taxonIndex.length == 1) {
    		isMonophyletic = true;
    		m = tree.getNode(taxonIndex[0]);
    	} else {
            nodesTraversed = new boolean[tree.getNodeCount()];
            nseen = 0;
            nrogues = 0;
        	m = getCommonAncestor();
            isMonophyletic = (nseen - nrogues * 2 == 2 * taxonIndex.length - 1);
    	}
    	if (useOriginate) {
    		if (!m.isRoot()) {
    			MRCATime = m.getParent().getDate();
    		} else {
    			MRCATime = m.getDate();
    		}
    	} else {
    		MRCATime = m.getDate();
    	}
    	
    	logP = 0;
        if (isMonophyleticInput.get() && !isMonophyletic) {
    		logP = Double.NEGATIVE_INFINITY;
    		return Double.NEGATIVE_INFINITY;
        }
        if (dist != null) {
            logP = dist.logDensity(MRCATime); // - dist.offsetInput.get());
        }
        return logP;
    }
    
    protected void initialise() {
		super.initialise();

		// determine which taxa are in the rogue set
        List<String> set = null;
        set = rogueTaxonSetInput.get().asStringList();

        final List<String> taxaNames = new ArrayList<>();
        for (final String taxon : tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }

        isRogue = new boolean[tree.getNodeCount()];
        for (final String taxon : set) {
            final int taxonIndex_ = taxaNames.indexOf(taxon);
            if (taxonIndex_ < 0) {
                throw new RuntimeException("Cannot find rogue taxon " + taxon + " in data");
            }
            if (isRogue[taxonIndex_]) {
                throw new RuntimeException("Rogue taxon " + taxon + " is defined multiple times, while they should be unique");
            }
            isRogue[taxonIndex_] = true;
        }
    }

}
