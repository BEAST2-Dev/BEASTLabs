package beast.evolution.tree;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.TaxonSet;

@Description("Ranked Nearest Neghbour Interchange metric on trees")
public class RNNIMetric extends BEASTObject implements TreeMetric {
	final public Input<TaxonSet> taxonInput = new Input<>("taxonset", "taxonset of the trees", Input.Validate.REQUIRED);
	final public Input<Boolean> recursiveInput = new Input<>("recursive", "whether to recurse down the taxon set and take only 'taxon' objects", false);

	Map<String, Integer> taxonMap = null;
	TreeInterface referenceTree = null;
	
	public RNNIMetric() {
		this.taxonMap = null;
	}
	
	
	public RNNIMetric(TaxonSet taxonset) {
		this.taxonMap = new HashMap<>();
		this.setTaxonMap(taxonset, 0);
	}
	
	
	public RNNIMetric(String [] taxaNames) {
		if (taxaNames == null) return;
		this.taxonMap = new HashMap<>();

		// Create taxon to int mapping
		int i = 0;
		for (String taxon : taxaNames) {
			taxonMap.put(taxon, i);
			i++;
		}
	}

	private int setTaxonMap(TaxonSet taxonset, int i) {
		
		if (taxonset == null) return i;

		// Create taxon to int mapping
		for (String taxon : taxonset.getTaxaNames()) {
			if (recursiveInput.get() && taxonset.getTaxon(taxon) instanceof TaxonSet) {
				i = setTaxonMap((TaxonSet)taxonset.getTaxon(taxon), i);
			}else {
				taxonMap.put(taxon, i);
				i++;
			}
		}
		
		return i;
	}

	@Override
	public void initAndValidate() {	
		TaxonSet taxonset = taxonInput.get();
		this.taxonMap = new HashMap<>();
		setTaxonMap(taxonset, 0);
	}

	
	Map<Clade, Node> nodeToCladeMap;
	
	@Override
	public double distance(TreeInterface tree1, TreeInterface tree2) {
		nodeToCladeMap = new HashMap<>();
		List<Clade> clades1 = getRankedClades(tree1); 
		List<Clade> clades2 = getRankedClades(tree2);
		int [] rank2 = new int[clades2.size()];
		int n = tree2.getLeafNodeCount();
		for (int i = 0; i < clades2.size(); i++) {
			Node node = nodeToCladeMap.get(clades2.get(i));
			rank2[node.getNr() - n] = i; 
		}
		
		double d = 0;
		for (int i = 0; i < clades1.size()-1; i++) {
			Node node2 = mrca(tree2, clades1.get(i));
			int rank = rank2[node2.getNr() - n];
			if (rank > i) {
				d += rank - i;
			}
		}
		return d;
	}
	

	private Node mrca(TreeInterface tree2, Clade clade) {
		int size = clade.getSize();
		Node [] nodes = new Node[size];
		int j = 0, k = 0;
		BitSet bits = clade.getBits();
		for (int i = 0; i < size; i++) {
			while (!bits.get(j)) {
				j++;
			}
			nodes[k++] = tree2.getNode(j);
		}
		return getCommonAncestor(tree2, nodes);
	}
	
	boolean [] nodesTraversed;
	int nseen;

    protected Node getCommonAncestor(Node n1, Node n2) {
        // assert n1.getTree() == n2.getTree();
        if( ! nodesTraversed[n1.getNr()] ) {
            nodesTraversed[n1.getNr()] = true;
            nseen += 1;
        }
        if( ! nodesTraversed[n2.getNr()] ) {
            nodesTraversed[n2.getNr()] = true;
            nseen += 1;
        }
        while (n1 != n2) {
	        double h1 = n1.getHeight();
	        double h2 = n2.getHeight();
	        if ( h1 < h2 ) {
	            n1 = n1.getParent();
	            if( ! nodesTraversed[n1.getNr()] ) {
	                nodesTraversed[n1.getNr()] = true;
	                nseen += 1;
	            }
	        } else if( h2 < h1 ) {
	            n2 = n2.getParent();
	            if( ! nodesTraversed[n2.getNr()] ) {
	                nodesTraversed[n2.getNr()] = true;
	                nseen += 1;
	            }
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
                    n = n1 = n.getParent();
                } else {
                    n = n2 = n.getParent();
                }
	            if( ! nodesTraversed[n.getNr()] ) {
	                nodesTraversed[n.getNr()] = true;
	                nseen += 1;
	            } 
	        }
        }
        return n1;
    }

    private Node getCommonAncestor(TreeInterface tree, Node [] nodes) {
        nodesTraversed = new boolean[tree.getNodeCount()];
        nseen = 0;
        Node cur = nodes[0];

        for (int k = 1; k < nodes.length; ++k) {
            cur = getCommonAncestor(cur, nodes[k]);
        }
        return cur;
    }

	private List<Clade> getRankedClades(TreeInterface tree) {
		List<Clade> clades = new ArrayList<>();
		getClades(taxonMap, tree.getRoot(), clades, null);
		Collections.sort(clades, (c1,c2)->{
			if (c1.getHeight() > c2.getHeight())
					return 1;
			if (c1.getHeight() < c2.getHeight())
				return -1;
			return 0;
		});
		return clades;
	}


	@Override
	public double distance(TreeInterface tree) {
		if (this.referenceTree == null) throw new IllegalArgumentException("Developer error: please provide a reference tree using 'setReference' or use 'distance(t1, t2)' instead");
		return distance(this.referenceTree, tree);
	}

	@Override
	public void setReference(TreeInterface ref) {
		this.referenceTree = ref;
	}
	
	
    private void getClades(Map<String, Integer> taxonMap,
            Node node, List<Clade> clades, BitSet bits) {

    	BitSet bits2 = new BitSet();

    	if (node.isLeaf()) {

    		int index = taxonMap.get(node.getID());
    		bits2.set(index);

    	} else {

    		for (Node child : node.getChildren()) {
    			getClades(taxonMap, child, clades, bits2);
    		}
    		Clade clade = new Clade(bits2, node.getHeight()); 
    		nodeToCladeMap.put(clade, node);
    		clades.add(clade);
    	}

    	if (bits != null) {
    		bits.or(bits2);
    	}
    }


}
