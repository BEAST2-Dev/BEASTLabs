package beast.math.distributions;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operators.MonoCladesMapping;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeInterface;

import java.io.PrintStream;
import java.util.*;

@Description("Helper class for specifiying a set of monophyletic clades. This beast object is passed as an argument to another beast object, and " +
        "provides the node id's of the clade roots upon request.The clades are collected from the various arguments and merged. Note that the mrca " +
        "logger ignores the 'useOriginate' directive by design.")
public class MonoPoints extends BEASTObject implements Loggable {

    public Input<List<MRCAPrior>> cladesInput = new Input<List<MRCAPrior>>("clades", "The set of clades represented as MRCAPriors", new ArrayList<MRCAPrior>());
    public Input<MultiMRCAPriors> multiInput = new Input<MultiMRCAPriors>("multiclades", "Set of clades represented as MultiMRCAPriors");
    public final Input<MultiMonophyleticConstraint> constraintsInput = new Input<>("constraints",
            "all constraints as encoded by one unresolved tree.");

    int[] topsNR;
    String[] cnames;
    TreeInterface tree;
    int[] nodeToCladeGroup;

    Node getCommonAncestor(Node n1, Node n2) {
        while (n1 != n2) {
	        double h1 = n1.getHeight();
	        double h2 = n2.getHeight();
	        if ( h1 < h2 ) {
	            n1 = n1.getParent();
	        } else if( h2 < h1 ) {
	            n2 = n2.getParent();
	        } else {
	            //zero length branches hell
	            Node n;
	            double b1 = n1.getLength();
	            double b2 = n2.getLength();
	            if( b1 > 0 ) {
	                n = n2;
	            } else { // b1 == 0
	                if( b2 > 0 ) {
	                    n = n1;
	                } else {
	                    // both 0
	                    n = n1;
	                    while( n != null && n != n2 ) {
	                        n = n.getParent();
	                    }
	                    if( n == n2 ) {
	                        // n2 is an ancestor of n1
	                        n = n1;
	                    } else {
	                        // always safe to advance n2
	                        n = n2;
	                    }
	                }
	            }
	            if( n == n1 ) {
                    n1 = n.getParent();
                } else {
                    n2 = n.getParent();
                }
	        }
        }
        return n1;
    }

    // A lightweight version for finding the most recent common ancestor of a group of taxa.
    // return the node-ref of the MRCA.

    // would be nice to use nodeRef's, but they are not preserved :(
    Node getCommonAncestor(TreeInterface tree, List<String> clade, Map<String,Integer> taxonToNR) {
        Node cur = tree.getNode(taxonToNR.get(clade.get(0)));

        for (int k = 1; k < clade.size(); ++k) {
            cur = getCommonAncestor(cur, tree.getNode(taxonToNR.get(clade.get(k))));
        }
        return cur;
    }

    @Override
    public void initAndValidate() {
        tree = null;
        List<List<String>> clades = new ArrayList<>();
        List<String> names = new ArrayList<>();
        final List<MRCAPrior> mrcaPriors = new ArrayList<>();
        List<MRCAPrior> priors = cladesInput.get();
        if( priors != null ) {
            mrcaPriors.addAll(priors);
        }

        MultiMRCAPriors multi = multiInput.get();
        if( multi != null ) {
            priors = multi.calibrationsInput.get();
            if( priors != null ) {
                mrcaPriors.addAll(priors);
            }
        }

        for (MRCAPrior p : mrcaPriors) {
            if( tree == null ) {
                tree = p.tree;
            } else {
                if( tree != p.tree ) {
                    throw new IllegalArgumentException("Conflicting trees");
                }
            }
            final TaxonSet taxonSet = p.taxonsetInput.get();
            clades.add(taxonSet.asStringList());
            names.add(p.getID());
        }

        final MultiMonophyleticConstraint constraints = constraintsInput.get();
        if( constraints != null ) {
            if( constraints.tree != tree ) {
               throw new IllegalArgumentException("Conflicting trees");
            }
            final List<List<String>> c = constraints.getConstraints();
            clades.addAll(c);
            for(int i = 0; i < c.size(); ++i) {
                names.add(null);
            }
        }

        if( tree == null ) {
           throw new IllegalArgumentException("nothing to do");
        }

        nodeToCladeGroup = MonoCladesMapping.setupNodeGroup(tree, clades);
        Map<String,Integer> taxonToNR = new HashMap<>();
        for( Node n : tree.getExternalNodes() ) {
            taxonToNR.put(n.getID(), n.getNr());
        }
        Node[] tops = new Node[clades.size()];
        for(int k = 0; k < clades.size(); ++k) {
           tops[k] = getCommonAncestor(tree, clades.get(k), taxonToNR);
        }
        Map<Integer,Integer> topNR2clade = new HashMap<>();
        for(int k = 0; k < clades.size(); ++k) {
            final Node n = tops[k];
            if( n.isRoot() ) {
                continue;
            }

            if( names.get(k) == null ) {
                final int prn = n.getParent().getNr();
                if( nodeToCladeGroup[prn] != -1 ) {
                    // un-named internal - ignored
                    continue;
                }
            }
            int nr = n.getNr();
            if( topNR2clade.containsKey(nr) ) {
                if( names.get(k) == null ) {
                    // unnamed duplicate - keep other
                    continue;
                }
                int cIndex = topNR2clade.get(nr);
                if( names.get(cIndex) == null ) {
                   topNR2clade.put(nr, k);
                } else {
                    // both named. WTF
                    continue;
                }
            } else {
              topNR2clade.put(nr, k);
            }
        }
        topsNR = new int[topNR2clade.size()];
        cnames = new String[topsNR.length];
        int n = 0;
        for( Map.Entry<Integer,Integer> n2c : topNR2clade.entrySet() ) {
            int k = n2c.getValue();
            topsNR[n] = n2c.getKey();
            cnames[n] = names.get(k);
            n += 1;
        }
    }

    public int[] getTops() {
        for(int k = 0; k < topsNR.length; ++k ) {
            int nr = topsNR[k];
            Node n = tree.getNode(nr);
            while( true ) {
                final Node np = n.getParent(); assert np != null;
                if( nodeToCladeGroup[np.getNr()] != nodeToCladeGroup[nr] ) {
                    break;
                }
                n = np;
                nr = n.getNr();
            }
            topsNR[k] = nr;
        }
        return topsNR;
    }

    public String[] getNames() {
        return cnames;
    }

    public TreeInterface getTree() { return  tree; }

    @Override
    public void init(PrintStream out) {
        for ( String name : cnames ) {
 			out.append("mrca(" + name + ")\t");
 		}
 	//	super.init(out);
    }

    @Override
    public void log(int sample, PrintStream out) {
        int[] tops = getTops();
        for (int k = 0; k < tops.length; ++k) {
            int taxonNr = tops[k];
            out.append(tree.getNode(taxonNr).getHeight()  +"\t");
        }
    }

    @Override
    public void close(PrintStream out) {
    }
}
