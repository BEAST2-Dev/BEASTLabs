package beast.evolution.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.util.Log;
import beast.evolution.alignment.TaxonSet;

@Description("Ranked Nearest Neighbour Interchange metric on trees")
public class RNNIMetric extends BEASTObject implements TreeMetric {
	final public Input<TaxonSet> taxonsetInput = new Input<>("taxonset", "taxonset of the trees", Input.Validate.REQUIRED);
	final public Input<Boolean> recursiveInput = new Input<>("recursive", "whether to recurse down the taxon set and take only 'taxon' objects", false);

	private Map<String, Integer> taxonMap = null;
	// maps leaf node index from tree2 to taxon index from taxonset
	private int [] reverseMap2;
	
	// use reference tree when requesting multiple distances to the same tree
	private TreeInterface referenceTree = null;
	private List<Clade> referenceTreeClades;

	// used for bookkeeping when finding MRCA
	private boolean [] nodesTraversed;

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
		TaxonSet taxonset = taxonsetInput.get();
		this.taxonMap = new HashMap<>();
		setTaxonMap(taxonset, 0);
	}

	@Override
	public double distance(TreeInterface tree1, TreeInterface tree2) {
		reverseMap2 = new int[tree2.getLeafNodeCount()];

		// get clades of tree1 in ranked order
		List<Clade> clades1 = getRankedClades(tree1);
		double d1 = distance(clades1, tree1, tree2);
		
		reverseMap2 = new int[tree2.getLeafNodeCount()];

		// get clades of tree2 in ranked order
		List<Clade> clades2 = getRankedClades(tree2);
		double d2 = distance(clades2, tree2, tree1);
		return Math.min(d1, d2);
	}
	
	@Override
	public double distance(TreeInterface tree) {
		if (this.referenceTree == null) throw new IllegalArgumentException("Developer error: please provide a reference tree using 'setReference' or use 'distance(t1, t2)' instead");
		// TODO: uncomment following line once code is robustified 
		// return distance(this.referenceTreeClades, referenceTree, tree);
		return distance(referenceTree, tree);
	}

	@Override
	public void setReference(TreeInterface ref) {
		this.referenceTree = ref;
		this.referenceTreeClades = getRankedClades(referenceTree);
	}

	
	
	private double distance(List<Clade> cladesOther, TreeInterface treeOtherX, TreeInterface tree) {
		// get clades in ranked order
		Tree treeCopy = new Tree();
		treeCopy.assignFrom((Tree) tree);
		Map<String,Integer> map = new HashMap<>();
		for (int i = 0; i < treeCopy.getLeafNodeCount(); i++) {
			map.put(treeCopy.getNode(i).getID(), i);
		}
		List<Clade> clades = getRankedClades(treeCopy);
		Map<Node,Clade> map2 = new HashMap<>();
		for (Clade c : clades) {
			map2.put(c.getNode(),c);
		}
		for (Node leaf : treeCopy.getExternalNodes()) {
			BitSet bits = new BitSet();
			bits.set(taxonMap.get(leaf.getID()));
			map2.put(leaf, new Clade(bits, leaf.getHeight(), leaf));
		}

		
		
		// pre-calculate node rankings of other tree
		int [] rank = new int[tree.getNodeCount()];
		Node [] invrank = new Node[clades.size()];
		for (int i = 0; i < clades.size(); i++) {
			Node node = clades.get(i).getNode();
			rank[node.getNr()] = i;
			invrank[i] = node;
		}

		// FindPath algorithm of 
		// Collienne, L., Gavryushkin, A. Computing nearest neighbour interchange distances between ranked phylogenetic trees. J. Math. Biol. 82, 8 (2021). 
		// https://doi.org/10.1007/s00285-021-01567-5
		// only calculates the distance = length of the path, not the path itself
		double d = 0;
		for (int i = 0; i < cladesOther.size() - 1; i++) {
			List<String> cluster = new ArrayList<>();
			Node current = cladesOther.get(i).getNode();
			getTaxa(current, cluster);
			Node targetNode = mrca(treeCopy, cluster, map);
			int rank0 = rank[targetNode.getNr()];
			for (int j = rank0; j > i; j--) {
				
				// trickle targetnode down to rank i
				if (invrank[j-1].getParent() == targetNode) {
					// perform NNI
					BitSet currentBits = cladesOther.get(i).getBits();
					Node node = invrank[j-1];
					Node left = node.getLeft();
					Node right = node.getRight();
					Node child;
					if (left.isLeaf()) {
						// current clade contains at least 2 taxa, so left cannot contain cluster, and therefore we want the right to remain
						if (currentBits.get(taxonMap.get(left.getID()))) {
							child = left;
						} else {
							child = right;
						}
					} else if (right.isLeaf()) {
						// mirror situation of left.isLeaf
						if (currentBits.get(taxonMap.get(right.getID()))) {
							child = right;
						} else {
							child = left;
						}
					} else {
						// check cluster is subset of left, if so take left, otherwise take right
//						if (currentBits.intersects(clades.get(rank[left.getNr()]).getBits())) {
						if (currentBits.intersects(map2.get(left).getBits())) {
							child = left;
						} else {
							child = right;
						}
					}
					
					Node gp = targetNode.getParent();
					if (gp != null) {
						gp.removeChild(targetNode);
						gp.addChild(node);
					}
					node.removeChild(child);
					node.addChild(targetNode);					
					node.setParent(gp);
					
					targetNode.removeChild(node);
					targetNode.addChild(child);
					targetNode.setParent(node);
					
					// update bit sets
					setBits(targetNode, map2, rank);
					setBits(node, map2, rank);
					
					if (current.getChildCount() != 2) {
						int h = 3;
						h++;
					}
					if (targetNode.getChildCount() != 2) {
						int h = 3;
						h++;
					}
					if (gp != null && gp.getChildCount() != 2) {
						int h = 3;
						h++;
					}
				}
				d++;

				// update rank/invrank arrays
				Node tmp = invrank[j-1];
				Node tmp2 = invrank[j];
				double h1 = tmp.getHeight();
				double h2 = tmp2.getHeight();
				rank[tmp2.getNr()] = j-1;
				rank[tmp.getNr()] = j;
				invrank[j-1] = tmp2;
				invrank[j] = tmp;
				tmp2.setHeight(h1);
				tmp.setHeight(h2);
				
				// System.err.println(d + ": " + invrank[clades.size()-1].toString());
			}				
		}
		
		String endTree = toSortedNewick(invrank[clades.size()-1],new int[1]);
		String targetTree = toSortedNewick(treeOtherX.getRoot(), new int[1]);
		if (!endTree.equals(targetTree)) {
			System.err.println(targetTree);
			System.err.println(endTree);
			// return Double.NEGATIVE_INFINITY;
		}
		return d;
	}
	

	private String toSortedNewick(Node node, int[] maxNodeInClade) {
        StringBuilder buf = new StringBuilder();

        if (!node.isLeaf()) {

            if (node.getChildCount() <= 2) {
                // Computationally cheap method for special case of <=2 children

                buf.append("(");
                String child1 = toSortedNewick(node.getChild(0), maxNodeInClade);
                int child1Index = maxNodeInClade[0];
                if (node.getChildCount() > 1) {
                    String child2 = toSortedNewick(node.getChild(1), maxNodeInClade);
                    int child2Index = maxNodeInClade[0];
                    if (child1Index > child2Index) {
                        buf.append(child2);
                        buf.append(",");
                        buf.append(child1);
                    } else {
                        buf.append(child1);
                        buf.append(",");
                        buf.append(child2);
                        maxNodeInClade[0] = child1Index;
                    }
                } else {
                    buf.append(child1);
                }
                buf.append(")");
            }

        } else {
            maxNodeInClade[0] = node.getNr();
            buf.append(node.getID());
        }

        return buf.toString();
	}


	private void setBits(Node targetNode, Map<Node,Clade> map2, /*List<Clade> clades,*/ int[] rank) {
		BitSet parentSet = map2.get(targetNode).getBits();
		parentSet.clear();
		BitSet leftSet = map2.get(targetNode.getLeft()).getBits();
		BitSet rightSet = map2.get(targetNode.getRight()).getBits();
		
		parentSet.or(leftSet);
		parentSet.or(rightSet);		
	}

	private void getTaxa(Node node, List<String> cluster) {
		if (node.isLeaf()) {
			cluster.add(node.getID());
		} else {
			for (Node child : node.getChildren()) {
				getTaxa(child, cluster);
			}
		}
	}
	
	private Node mrca(TreeInterface tree, List<String> cluster, Map<String,Integer> map) {
		int size = cluster.size();
		Node [] nodes = new Node[size];
		int k = 0;
		for (String taxon : cluster) {
			nodes[k++] = tree.getNode(map.get(taxon));
		}
		return getCommonAncestor(tree, nodes);
	}

    protected Node getCommonAncestor(Node n1, Node n2) {
        // assert n1.getTree() == n2.getTree();
        if( ! nodesTraversed[n1.getNr()] ) {
            nodesTraversed[n1.getNr()] = true;
        }
        if( ! nodesTraversed[n2.getNr()] ) {
            nodesTraversed[n2.getNr()] = true;
        }
        while (n1 != n2) {
	        double h1 = n1.getHeight();
	        double h2 = n2.getHeight();
	        if ( h1 < h2 ) {
	            n1 = n1.getParent();
	            if( ! nodesTraversed[n1.getNr()] ) {
	                nodesTraversed[n1.getNr()] = true;
	            }
	        } else if( h2 < h1 ) {
	            n2 = n2.getParent();
	            if( ! nodesTraversed[n2.getNr()] ) {
	                nodesTraversed[n2.getNr()] = true;
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
	            } 
	        }
        }
        return n1;
    }

    private Node getCommonAncestor(TreeInterface tree, Node [] nodes) {
        nodesTraversed = new boolean[tree.getNodeCount()];
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
			Log.debug.println("Tie break");
			return 0;
		});
		return clades;
	}	
	
    private void getClades(Map<String, Integer> taxonMap,
            Node node, List<Clade> clades, BitSet bits) {

    	BitSet bits2 = new BitSet();

    	if (node.isLeaf()) {

    		int index = taxonMap.get(node.getID());
    		reverseMap2[index] = node.getNr();
    		bits2.set(index);

    	} else {

    		for (Node child : node.getChildren()) {
    			getClades(taxonMap, child, clades, bits2);
    		}
    		Clade clade = new Clade(bits2, node.getHeight(), node); 
    		clades.add(clade);
    	}

    	if (bits != null) {
    		bits.or(bits2);
    	}
    }


}
