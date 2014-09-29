package beast.evolution.tree;


import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.MultiMonophyleticConstraint;

import java.util.ArrayList;
import java.util.List;

@Description("Random tree with constraints specified by one tree, just like multiple monophyly.")
public class ConstrainedRandomTree extends RandomTree  {
    // The tree in the XML should have a taxon set, since it is not fully initialized at this stage
    public final Input<MultiMonophyleticConstraint> allConstraints = new Input<>("constraints",
                "all constraints as encoded by one unresolved tree.", Input.Validate.REQUIRED);
    @Override
    public void initAndValidate() throws Exception {
        final Tree tree = m_initial.get();

//        final MultiMonophyleticConstraint mul = new  MultiMonophyleticConstraint();
//        mul.initByName("newick", allConstraints.get(), "tree", tree);
        final MultiMonophyleticConstraint mul = allConstraints.get();
        List<List<String>> allc = mul.getConstraints();

        List<MRCAPrior> cons = new ArrayList<>();
        for( List<String> c : allc ) {
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
        calibrationsInput.setValue(cons, this);
        super.initAndValidate();
    }
}
