package beastlabs.evolution.tree;


import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beastlabs.math.distributions.MultiMonophyleticConstraint;

import java.util.ArrayList;
import java.util.List;

@Description("Random tree with constraints specified by one tree, just like multiple monophyly.")
public class SimpleConstrainedRandomTree extends SimpleRandomTree  {
    // The tree in the XML should have a taxon set, since it is not fully initialized at this stage
    public final Input<List<MultiMonophyleticConstraint>> allConstraints = new Input<>("constraints",
                "all constraints as encoded by one unresolved tree.",  new ArrayList<MultiMonophyleticConstraint>(), Input.Validate.REQUIRED);

    @Override
    public void initAndValidate() {
    	if (beastfx.app.beauti.Beauti.isInBeauti()) {
    		return;
    	}
        List<MRCAPrior> cons = getCons();
        List<MRCAPrior> origCons = new ArrayList<>();
        origCons.addAll(calibrationsInput.get());
        calibrationsInput.setValue(cons, this);
        super.initAndValidate();
        calibrationsInput.get().clear();
        clearup();
        if (origCons.size() > 0) {
            calibrationsInput.setValue(origCons, this);
        }
    }

    private ArrayList<Object> outputs = null;

    private List<MRCAPrior> getCons() {
        final Tree tree = m_initial.get();
        outputs = new ArrayList<>(tree.getOutputs());
        final List<MultiMonophyleticConstraint> allmul = allConstraints.get();

        List<MRCAPrior> cons = new ArrayList<>();
        for( final MultiMonophyleticConstraint mul : allmul ) {
            List<List<String>> allc = mul.getConstraints();
            for (List<String> c : allc) {
                if( c.size() > 1 ) {
                    final MRCAPrior m = new MRCAPrior();
                    final List<Taxon> t = new ArrayList<>();
                    for (String s : c) {
                        t.add(new Taxon(s));
                    }
                    final TaxonSet ts = new TaxonSet();
                    ts.initByName("taxon", t);
                    m.initByName("tree", tree, "taxonset", ts, "monophyletic", true);
                    cons.add(m);
                }
            }
        }
		return cons;
	}

    private void clearup() {
        final Tree itree = m_initial.get();

        ArrayList<BEASTInterface> ox = new ArrayList<>(itree.getOutputs());
        for (BEASTInterface o : ox) {
            if( !outputs.contains(o) ) {
                itree.getOutputs().remove(o);
            }

        }
        outputs = null;
    }

	@Override
    public void initStateNodes() {
    	List<MRCAPrior> cons = getCons();
        List<MRCAPrior> origCons = new ArrayList<>();
        origCons.addAll(calibrationsInput.get());
        calibrationsInput.setValue(cons, this);
    	super.initStateNodes();
        calibrationsInput.get().clear();
        
        int n = countNodes(getRoot());
        System.err.println("\n\n#leafs = " + n + " " + getLeafNodeCount() + "\n\n");
        Tree init = (Tree) m_initial.get();
        System.err.println("\n\n#leafs = " + countNodes(init.getRoot()) + " " + init.getLeafNodeCount() + " " + init.getRoot().getHeight() + "\n\n");
        
        clearup();
        if (origCons.size() > 0)
        	calibrationsInput.setValue(origCons, this);
    }

	private int countNodes(Node node) {
		if (node.isLeaf()) {
			return 1;
		}
		int n = 0;
		for (Node c : node.getChildren()) {
			n += countNodes(c);
		}
		return n;
	}
}
