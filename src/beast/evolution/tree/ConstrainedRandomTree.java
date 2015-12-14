package beast.evolution.tree;


import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.MultiMonophyleticConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Description("Random tree with constraints specified by one tree, just like multiple monophyly.")
public class ConstrainedRandomTree extends RandomTree  {
    // The tree in the XML should have a taxon set, since it is not fully initialized at this stage
    public final Input<MultiMonophyleticConstraint> allConstraints = new Input<>("constraints",
                "all constraints as encoded by one unresolved tree.", Input.Validate.REQUIRED);
    @Override
    public void initAndValidate() throws Exception {
    	List<MRCAPrior> cons = getCons();
        List<MRCAPrior> origCons = new ArrayList();
        origCons.addAll(calibrationsInput.get());
        calibrationsInput.setValue(cons, this);
        super.initAndValidate();
        calibrationsInput.get().clear();
        if (origCons.size() > 0)
        	calibrationsInput.setValue(origCons, this);
    }
    
    private List<MRCAPrior> getCons() throws Exception {
        final Tree tree = m_initial.get();
        final MultiMonophyleticConstraint mul = allConstraints.get();
        List<List<String>> allc = mul.getConstraints();

        List<MRCAPrior> cons = new ArrayList<>();
        for( List<String> c : allc ) {
            if (c.size() > 1) {
	            final MRCAPrior m = new MRCAPrior();
	            final List<Taxon> t = new ArrayList<>();
	            for( String s : c ) {
	                t.add(new Taxon(s));
	            }
	            final TaxonSet ts = new TaxonSet();
	            ts.initByName("taxon", t);
	            m.initByName("tree", tree, "taxonset", ts, "monophyletic", true);
	            cons.add(m);
            }
        }
		return cons;
	}

    private void cleanup() {
        final Tree itree = m_initial.get();

        Set outputs1 = itree.getOutputs();
        ArrayList<Object> ox = new ArrayList(outputs1);
        for (Object o : ox) {
            if( ! getOutputs().contains(o) ) {
                itree.getOutputs().remove(o);
            }

        }
        //this.outputs = null;
    }

    @Override
    public void initStateNodes() throws Exception {
    	List<MRCAPrior> cons = getCons();
        List<MRCAPrior> origCons = new ArrayList();
        origCons.addAll(calibrationsInput.get());
        calibrationsInput.setValue(cons, this);
    	super.initStateNodes();
        calibrationsInput.get().clear();
        cleanup();
        if (origCons.size() > 0)
        	calibrationsInput.setValue(origCons, this);
    }
    
}
