package beast.math.distributions;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Prior over set of taxa, useful for defining multiple monophyletic constraints using a newick format")
public class MultiMonophyleticConstraint extends Distribution {
    public final Input<Tree> treeInput = new Input<Tree>("tree", "the tree containing the taxon set", Validate.REQUIRED);
    //public final Input<TaxonSet> taxonsetInput = new Input<TaxonSet>("taxonset", "set of taxa for which prior information is available");
    public final Input<String> newickInput = new Input<String>("newick", "the tree constraints specified as newick tree using polytopes, "
	    		+ "e.g. ((human,chimp,bonobo),gorilla) specifies two monophyletic constraints,"
	    		+ "one for human,chimp,bonobo' and one for 'human,chimp,bonobo,gorilla'", Validate.REQUIRED);
    
    public final Input<Boolean> isBinaryInput = new Input<>("isBinary", "flag to indicate tree is a binary tree instead of a polytopy (faster)", true);

    
    /** Constraints are encoded as a list of taxon numbers for each constraint
     * TaxonIDList contains a list of constraints
     */
    List<List<Integer>> taxonIDList;

    Tree tree;
    String[] taxaList;

    // indexed by node number, the (smallest monophyletic) clade this leaf belongs to (0 .. #clades). -1 for free taxa.
    int[] leafCladeAssignments;
    // cladeSize[i] is the size (number of taxa) in the i'th clade.
    int[] cladeSize;
    // cladeParent[i] is the clade assignment of the embedding monophyletic clade (-1 for none)
    int[] cladeParent;

    @Override
    public void initAndValidate() throws Exception {
        taxonIDList = new ArrayList<List<Integer>>();
        tree = treeInput.get();
        taxaList = tree.getTaxonset().asStringList().toArray(new String[]{});

        parse(newickInput.get());

        // setup internal arrays for monophyly check
        leafCladeAssignments = new int[tree.getNodeCount()];
        Arrays.fill(leafCladeAssignments, -1);

        cladeSize = new int[taxonIDList.size()];
        cladeParent = new int[taxonIDList.size()];
        Arrays.fill(cladeParent, -1);

        Node [] nodes = tree.getNodesAsArray();
        for(int k = 0; k < taxonIDList.size(); ++k ) {
            cladeSize[k] = taxonIDList.get(k).size();
        }

        for(int k = 0; k < taxonIDList.size(); ++k ) {
            final List<Integer> t = taxonIDList.get(k);
            for( int n : t ) {
                final int i = nodes[n].getNr();
                if( leafCladeAssignments[i] == -1 ) {
                    leafCladeAssignments[i] = k;
                } else {
                    // nesting situation k and leafCladeAssignments[i]; keep the assignment to the smaller clade
                    if( cladeSize[leafCladeAssignments[i]] > cladeSize[k] ) {
                        // [i] is parent of k
                        leafCladeAssignments[i] = k;
                    }
                }
            }
        }
        // slow but who cares - one time only
        for(int k = 0; k < taxonIDList.size(); ++k ) {
            final List<Integer> tk = taxonIDList.get(k);
            for(int i = 0; i < k; ++i ) {
                final List<Integer> ti = taxonIDList.get(i);
                for( int n : ti ) {
                    if( tk.contains(n) ) {
                        updateCladeParent(i, k);
                        break;
                    }
                }
            }
        }
    }

    private void updateCladeParent(int subClade, int clade) {
        if( cladeSize[subClade] > cladeSize[clade] ) {
            int tmp = subClade;
            subClade = clade;
            clade = tmp;
        }

        if( cladeParent[subClade] != clade ) {
            if( cladeParent[subClade] == -1 ||
                    cladeSize[cladeParent[subClade]] > cladeSize[clade] ) {
                cladeParent[subClade] = clade;
            }
        }
    }

	/** extract clades from Newick string,
	 * and add constraints for all internal nodes (except the root if it contains all taxa)
	 **/
	private void parse(String newick) {
		// get rid of initial and trailing spaces
		newick = newick.trim();
		// remove comments
		newick = newick.replaceAll(".*\\[[^\\]]*\\].*", "");
		// remove branch lengths
		newick = newick.replaceAll(":[^,\\(\\)]*", "");
		Pattern pattern = Pattern.compile("\\(([^\\(\\)]*)\\)");
		Matcher m = pattern.matcher(newick);
		
		//List<String> taxaList = taxonsetInput.get().asStringList();
		while (m.find()) {
			String group = m.group();
			String [] taxa = group.substring(1,group.length()-1).split(",");
			List<Integer> list = new ArrayList<Integer>();
			for (String taxon : taxa) {
				taxon = taxon.trim();
				if (taxon.length() > 0) {
					int i = indexOf(taxon);
					if (i == -1 && (taxon.startsWith("'") || taxon.startsWith("\""))) {
						i = indexOf(taxon.substring(1, taxon.length() - 1));

					}
                    if (i == -1) {
                        throw new RuntimeException("Cannot find taxon " + taxon + "  in taxon list");
                    }
					list.add(i);
				}
			}
			if (list.size() < tree.getLeafNodeCount()) {
				taxonIDList.add(list);
				//Log.trace.println("Constraining " + group);// + " " + Arrays.toString(list.toArray()));			
			}
			newick = newick.replaceFirst("\\(([^\\(\\)]*)\\)", ",$1,");
			newick = newick.replaceAll("([\\(,]),", "$1");
			newick = newick.replaceAll(",\\)", ")");
			m = pattern.matcher(newick);
		}
	}


    private int indexOf(String taxon) {
		for (int k = 0; k < taxaList.length; k++) {
			if (taxon.equals(taxaList[k])) {
				return k;
			}
		}
		return -1;
	}

	@Override
    public double calculateLogP() throws Exception {
        final boolean mono1 = isBinaryInput.get() ? isMonoJH() : isMonoJHNonBinary();
        if( false ) assert mono1 == isMonoRB();   // assert is expensive. isMonoJH replaces the much slower isMonoRB

        logP = mono1 ? 0 : Double.NEGATIVE_INFINITY;
        return logP;
    }


    private boolean isMonoRB() {
        for (List<Integer> list : taxonIDList) {
            if( !isMonophyletic(list) ) {
                return false;
            }
        }
        return true;
    }

    /** walk up the tree from the nodes, to see whether a clade is monophyletic
     * After initialising steps from each of the child nodes,
     * a step is made from any internal nodes if there are two 
     * paths to an internal node. Iff the clade is monophyletic, 
     * this process halts after n-1 steps where n is the nr of taxa
     * in the clade.
     * NB assumes that the tree is binary **/
	private boolean isMonophyletic(List<Integer> list) {
		int [] parentcount = new int[tree.getNodeCount()];
		Node [] nodes = tree.getNodesAsArray();
		
		// mark parents of leaf nodes
		Set<Integer> list2 = new HashSet<Integer>();
		int k = 0;
		for (int i : list) {
			int parentNr = nodes[i].getParent().getNr();
			parentcount[parentNr]++;
			if (parentcount[parentNr] == 2) {
				list2.add(parentNr);
				k++;
			}
		}
		// mark parents of nodes that have two children marked
		while (list2.size() > 0) {
			Set<Integer> list3 = new HashSet<Integer>();
			for (int i : list2) {
				int parentNr = nodes[i].getParent().getNr();
				parentcount[parentNr]++;
				if (parentcount[parentNr] == 2) {
					list3.add(parentNr);
					k++;
				}
			}
			list2 = list3;
		}
        return k == list.size() - 1;
    }

    /**
     *  Check all monophyly constraints simultaneously in one (post-order) pass.
     *  A monophyly violation is detected when a subtree contains taxa from two incompatible clades,
     *  and then the function immediately returns false.
     *  Each taxon is assigned a clade index/assignment, the smallest clade it belongs to.
     *  A clade count is calculated for each internal node, that is the number of taxa in the current clade it is
     *  embedding in. The node inherits the clade index/assignment from its children. When the root of a clade is
     *  reached, i.e. when the count is equal to the known size of the clade, its clade assignment is updated to be the
     *  assignment of its parent (if any).
     *
     * @return  true if all monophyly constraints are met, false otherwise
     */
    private boolean isMonoJH() {
        // this traversal is shared between all calling code in current cycle.
        Node[] post = tree.listNodesPostOrder(null, null);
        final int nNodes = post.length;
        // per tree node, number of taxa in current monophyletic clade
        int nodeCladeSize[] = new int[nNodes];
        // per tree node, clade index (or assignment) of taxa in this sub tree.
        int[] nodeClade = new int[nNodes];

        for (final Node n : post) {
            final int nr = n.getNr();
            if( n.isLeaf() ) {
                nodeClade[nr] = leafCladeAssignments[nr];
                if( nodeClade[nr] >= 0 ) {
                    nodeCladeSize[nr] = 1;
                    // a leaf in a clade of its own is ipso facto completed.
                    if( cladeSize[nodeClade[nr]] == 1 ) {
                        nodeClade[nr] = cladeParent[nodeClade[nr]];
                    }
                }
            } else {
                final int lnr = n.getLeft().getNr();
                final int l = nodeClade[lnr];
                final int rnr = n.getRight().getNr();
                final int r = nodeClade[rnr];

                if( l != r ) {
                    // A node with mixed taxa (from two mono clades or one clade and free taxa)
                    return false;
                }

                nodeClade[nr] = l;

                if( l != -1 ) {
                    nodeCladeSize[nr] = nodeCladeSize[lnr] + nodeCladeSize[rnr];
                    if( nodeCladeSize[nr] == cladeSize[l] ) {
                        // clade root: set its assignment to parent, if any.
                        nodeClade[nr] = cladeParent[l];
                    }
                }
            }
        }
        return true;
    }
    private boolean isMonoJHNonBinary() {
        // this traversal is shared between all calling code in current cycle.
        Node[] post = tree.listNodesPostOrder(null, null);
        final int nNodes = post.length;
        // per tree node, number of taxa in current monophyletic clade
        int nodeCladeSize[] = new int[nNodes];
        // per tree node, clade index (or assignment) of taxa in this sub tree.
        int[] nodeClade = new int[nNodes];

        for (final Node n : post) {
        	if (n != null) {
	            final int nr = n.getNr();
	            if( n.isLeaf() ) {
	                nodeClade[nr] = leafCladeAssignments[nr];
	                if( nodeClade[nr] >= 0 ) {
	                    nodeCladeSize[nr] = 1;
	                    // a leaf in a clade of its own is ipso facto completed.
	                    if( cladeSize[nodeClade[nr]] == 1 ) {
	                        nodeClade[nr] = cladeParent[nodeClade[nr]];
	                    }
	                }
	            } else {
	                final int lnr = n.getLeft().getNr();
	                final int l = nodeClade[lnr];
	                if (l != -1) {
	                	nodeCladeSize[nr] = nodeCladeSize[lnr];
	                }
	                for (int i = 1; i < n.getChildCount(); i++) {
		                final int rnr = n.getChild(i).getNr();
		                final int r = nodeClade[rnr];
		
		                if( l != r ) {
		                    // A node with mixed taxa (from two mono clades or one clade and free taxa)
		                    return false;
		                }
		
		                nodeClade[nr] = l;
		
		                if( l != -1 ) {
		                	nodeCladeSize[nr] += nodeCladeSize[rnr];
		                }
	                }
	                if (l != -1 ) {
		                if (nodeCladeSize[nr] == cladeSize[l] ) {
		                    // clade root: set its assignment to parent, if any.
		                    nodeClade[nr] = cladeParent[l];
		                }
	                }
	            }
        	}
        }
        return true;
    }

    public int getMonoNodes(boolean[] nodes) {

        // this traversal is shared between all calling code in current cycle.
        Node[] post = tree.listNodesPostOrder(null, null);
        final int nNodes = post.length;

        assert nNodes == nodes.length;
        Arrays.fill(nodes, false);

        // per tree node, number of taxa in current monophyletic clade
        int nodeCladeSize[] = new int[nNodes];
        // per tree node, clade index (or assignment) of taxa in this sub tree.
        int[] nodeClade = new int[nNodes];
        int nMono = 0;
        for (final Node n : post) {
            final int nr = n.getNr();
            if( n.isLeaf() ) {
                nodeClade[nr] = leafCladeAssignments[nr];
                if( nodeClade[nr] >= 0 ) {
                    nodeCladeSize[nr] = 1;
                    // a leaf in a clade of its own is ipso facto completed.
                    if( cladeSize[nodeClade[nr]] == 1 ) {
                        nodeClade[nr] = cladeParent[nodeClade[nr]];
                    }
                }
            } else {
                final int lnr = n.getLeft().getNr();
                final int l = nodeClade[lnr];
                final int rnr = n.getRight().getNr();
                final int r = nodeClade[rnr];

                if( l != r ) {
                    // A node with mixed taxa (from two mono clades or one clade and free taxa)
                    return -1;
                }

                nodeClade[nr] = l;

                if( l != -1 ) {
                    nodeCladeSize[nr] = nodeCladeSize[lnr] + nodeCladeSize[rnr];
                    if( nodeCladeSize[nr] == cladeSize[l] ) {
                        nodes[n.getNr()] = true;
                        nMono += 1;
                        // clade root: set its assignment to parent, if any.
                        nodeClade[nr] = cladeParent[l];
                    }
                }
            }
        }
        return nMono;
    }

	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
	@Override public void sample(State state, Random random) {}

    /** Return constraints as a collection of clade tip sets (tips taxa as strings) **/

    public List<List<String>> getConstraints() {
        List<List<String>> allc = new ArrayList<List<String>>();
        for (List<Integer> list : taxonIDList) {
            List<String> tax = new ArrayList<>(list.size());
            for( int i : list ) {
                tax.add(taxaList[i]);
            }
            allc.add(tax);
        }
        return allc;
    }
}
