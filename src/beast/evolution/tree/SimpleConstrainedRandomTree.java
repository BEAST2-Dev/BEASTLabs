package beast.evolution.tree;


import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.MultiMonophyleticConstraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Description("Random tree with constraints specified by one tree, just like multiple monophyly.")
public class SimpleConstrainedRandomTree extends SimpleRandomTree  {
    // The tree in the XML should have a taxon set, since it is not fully initialized at this stage
    public final Input<List<MultiMonophyleticConstraint>> allConstraints = new Input<>("constraints",
                "all constraints as encoded by one unresolved tree.",  new ArrayList<MultiMonophyleticConstraint>(),
            Input.Validate.REQUIRED);


    @Override
    public void initAndValidate() {
        //final Tree itree = m_initial.get();

        List<MRCAPrior> cons = getCons();
        List<MRCAPrior> origCons = new ArrayList();
        origCons.addAll(calibrationsInput.get());
        calibrationsInput.setValue(cons, this);
        super.initAndValidate();
        calibrationsInput.get().clear();
        clearup();
        if (origCons.size() > 0)
        	calibrationsInput.setValue(origCons, this);
//        if (m_initial.get() != null) {
//       	  initStateNodes();
//        }
    }

    private ArrayList<Object> outputs = null;

    private List<MRCAPrior> getCons() {
        final Tree tree = m_initial.get();
        outputs = new ArrayList(tree.getOutputs());
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

        ArrayList<Object> ox = new ArrayList(itree.getOutputs());
        for (Object o : ox) {
            if( !outputs.contains(o) ) {
                itree.getOutputs().remove(o);
            }

        }
        outputs = null;
    }

	@Override
    public void initStateNodes() {
    	List<MRCAPrior> cons = getCons();
        List<MRCAPrior> origCons = new ArrayList();
        origCons.addAll(calibrationsInput.get());
        calibrationsInput.setValue(cons, this);
    	super.initStateNodes();
        calibrationsInput.get().clear();
        clearup();
        if (origCons.size() > 0)
        	calibrationsInput.setValue(origCons, this);
    }
}
