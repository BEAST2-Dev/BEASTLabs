package beast.math.distributions;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.evolution.operators.MonoCladesMapping;
import beast.evolution.tree.Node;
import beast.evolution.tree.PrunedTree;

import java.io.PrintStream;
import java.util.*;

@Description("A single distribution which efficiently takes care of a set of MRCA constraints.")
public class MultiMRCAPriors extends MultiMonophyleticConstraint {

    public Input<List<MRCAPrior>> calibrationsInput =
                new Input<List<MRCAPrior>>("distribution", "Set of calibrated nodes", new ArrayList<MRCAPrior>());

    protected int[] nodeToCladeGroup = null;
    protected int prevNodeCount = -1;
    protected int[] ctops = null;
    protected boolean[] ctopParent = null;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    @Override
    protected void parse(String newick) {
        super.parse(newick);
        for( MRCAPrior m : calibrationsInput.get() ) {
            if( ! m.tree.equals(this.tree) ) {
                throw new IllegalArgumentException("All constraints must be on the same tree");
            }
            List<Integer> list = new ArrayList<Integer>();
            for( String taxon : m.taxonsetInput.get().getTaxaNames() ) {
                list.add(indexOf(taxon));
            }
            boolean add = true;
            Set<Integer> slist = new HashSet<>(list);

            // Don't add a taxon list which was already added by the supermethod
            for( List l : taxonIDList ) {
                if( l.size() == slist.size() && slist.containsAll(l) ) {
                    add = false;
                    break;
                }
            }
            // Don't add a taxon list containing the entire tree, to be consistent
            // with the superclass.
            if (list.size() == tree.getLeafNodeCount()) {
		add = false;
	    }
            if( add ) {
                taxonIDList.add(list);
            }
        }
    }

    @Override
    public double calculateLogP() {
    	if (tree instanceof PrunedTree && !((PrunedTree)tree).isValidlyNumbered()) {
            logP = Double.NEGATIVE_INFINITY;
            return logP;
    	}
    	
        double logp = super.calculateLogP();
        if( logp != Double.NEGATIVE_INFINITY ) {
            List<MRCAPrior> mrcaPriors = calibrationsInput.get();

            if( tree.getNodeCount() != prevNodeCount ) {
                nodeToCladeGroup = MonoCladesMapping.setupNodeGroup(tree, this);
                prevNodeCount = tree.getNodeCount();

                final int nCals = mrcaPriors.size();
                ctops = new int[nCals];
                ctopParent = new boolean[nCals];
                int i = 0;
                for( MRCAPrior m : mrcaPriors ) {
                    m.calculateLogP(); // init
                    ctops[i] = m.getCommonAncestor().getNr();
                    ctopParent[i] = m.useOriginate;
                    i += 1;
                }
            }
            for(int k = 0; k < ctops.length; ++k) {
                final int nr = ctops[k];
                Node n = tree.getNode(nr);
                final int ng = nodeToCladeGroup[nr];
                // update clade MRCA if root node changed by operator
                while( !n.isRoot() && ng == nodeToCladeGroup[n.getParent().getNr()] ) {
                    n = n.getParent();
                }
                ctops[k] = n.getNr();
                {
                    boolean ICC = false;
                    if( ICC ) {
                        mrcaPriors.get(k).calculateLogP();
                        assert mrcaPriors.get(k).getCommonAncestor().equals(n);
                    }
                }
                final double MRCATime = ctopParent[k] ? 
                 		  n.isRoot() ? n.getDate() : n.getParent().getDate() 
              			: n.getDate();
                ParametricDistribution dist = mrcaPriors.get(k).dist;
                if( dist != null ) {
                    final double v = dist.logDensity(MRCATime);
                    if( v == Double.NEGATIVE_INFINITY ) {
                        logp = Double.NEGATIVE_INFINITY;
                        break;
                    }
                    logp += v;
                }
            }

        }
        logP = logp;
        return logp;
    }

    @Override
    public void store() {
        super.store();
    }

    @Override
    public void restore() {
        super.restore();
    }

    @Override
    protected boolean requiresRecalculation() {
        return super.requiresRecalculation();
    }


    @Override
    public void init(PrintStream out) {
        final List<MRCAPrior> mrcaPriors = calibrationsInput.get();
        for( MRCAPrior prior : mrcaPriors ) {
            out.append("mrca(" + prior.getID()+")\t");
            if( ! (prior.dist instanceof beast.math.distributions.Uniform) )  {
                out.append("logP(mrca(" + prior.getID()+"))\t");
            }
        }
    }

    @Override
    public void log(long sample, PrintStream out) {
        final List<MRCAPrior> mrcaPriors = calibrationsInput.get();
        for( MRCAPrior prior : mrcaPriors ) {
            prior.calculateLogP();
            out.append(prior.MRCATime+"\t");
            if( ! (prior.dist instanceof beast.math.distributions.Uniform) )  {
               out.append(prior.getCurrentLogP()+"\t");
            }
        }
    }

    @Override
    public void close(PrintStream out) {
    }

    @Override
    public void sample(final State state, final Random random) {
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }
}
