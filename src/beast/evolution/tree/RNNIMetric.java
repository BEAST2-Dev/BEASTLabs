package beast.evolution.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;

@Description("Ranked Nearest Neighbour Interchange metric on trees")
public class RNNIMetric extends BEASTObject implements TreeMetric {
	final public Input<TaxonSet> taxonsetInput = new Input<>("taxonset", "taxonset of the trees", Input.Validate.REQUIRED);
	final public Input<Boolean> recursiveInput = new Input<>("recursive", "whether to recurse down the taxon set and take only 'taxon' objects", false);

	private Map<String, Integer> taxonMap = null;
	
	// use reference tree when requesting multiple distances to the same tree
	private TreeInterface referenceTree = null;
	private Integer [] referenceClades; 
			
	// used for bookkeeping when finding MRCA
	private boolean [] nodesTraversed;

	// number of NNIs along path of last calculated distance
	private int nniCount = -1;

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

	public int getNNICount() {
		if (nniCount < 0) {
			throw new RuntimeException("Programmer error: call distance() method before calling getNNICount()");
		}
		return nniCount;
	}

	@Override
	public void initAndValidate() {	
		TaxonSet taxonset = taxonsetInput.get();
		this.taxonMap = new HashMap<>();
		setTaxonMap(taxonset, 0);
	}

	@Override
	public double distance(TreeInterface tree1, TreeInterface tree2) {
		// get clades of tree1 in ranked order
		Integer [] clades1 = getRankedClades(tree1);
		double d1 = distance(clades1, tree1, tree2);
		return d1;
	}
	
	@Override
	public double distance(TreeInterface tree) {
		if (this.referenceTree == null) throw new IllegalArgumentException("Developer error: please provide a reference tree using 'setReference' or use 'distance(t1, t2)' instead");
		referenceClades = getRankedClades(tree);
		return distance(referenceClades, referenceTree, tree);
	}

	@Override
	public void setReference(TreeInterface ref) {
		this.referenceTree = ref;
	}

	
	
	private double distance(Integer[] cladesOther, TreeInterface treeOther, TreeInterface tree) {
		// get clades in ranked order
		Tree treeCopy = new Tree();
		treeCopy.assignFrom((Tree) tree);
		
		Map<String,Integer> map = new HashMap<>();
		for (int i = 0; i < treeCopy.getLeafNodeCount(); i++) {
			Node leaf = treeCopy.getNode(i);
			map.put(leaf.getID(), i);
			leaf.setHeight(-1e-13);
		}

		Integer[] clades = getRankedClades(treeCopy);
		
		// pre-calculate node rankings of other tree
		int [] rank = new int[tree.getNodeCount()];
		Node [] invrank = new Node[clades.length];
		for (int i = 0; i < clades.length; i++) {
			rank[clades[i]] = i;
			invrank[i] = treeCopy.getNode(clades[i]);
		}

		// FindPath algorithm of 
		// Collienne, L., Gavryushkin, A. Computing nearest neighbour interchange distances between ranked phylogenetic trees. J. Math. Biol. 82, 8 (2021). 
		// https://doi.org/10.1007/s00285-021-01567-5
		// only calculates the distance = length of the path, not the path itself
		double d = 0;
		nniCount = 0;
		for (int i = 0; i < cladesOther.length - 1; i++) {
			List<String> cluster = new ArrayList<>();
			Node current = treeOther.getNode(cladesOther[i]);
			getTaxa(current, cluster);
			Node targetNode = mrca(treeCopy, cluster, map);
			int rank0 = rank[targetNode.getNr()];
			for (int j = rank0; j > i; j--) {
				
				// trickle targetnode down to rank i
				if (invrank[j-1].getParent() == targetNode) {
					// perform NNI
					Node node = invrank[j-1];
					Node left = node.getLeft();
					Node right = node.getRight();
					Node child;
					if (nodesTraversed[left.getNr()]) {
						child = left;
					} else {
						child = right;
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
					nniCount++;
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

				// for debugging:
				// checkRank(rank, invrank);
				// System.err.println(d + ": " + invrank[clades.size()-1].toString());
			}				
			// for debugging:
//			Node targetNode2 = mrca(treeCopy, cluster, map);
//			if (targetNode!=targetNode2) {
//				System.err.println("clade not restored");
//			}
		}
		
		// for debugging:
//		String endTree = toSortedNewick(invrank[clades.length-1],new int[1]);
//		String targetTree = toSortedNewick(treeOther.getRoot(), new int[1]);
//		if (!endTree.equals(targetTree)) {
//			System.err.println(targetTree);
//			System.err.println(endTree);
//		}
		return d;
	}
	
	/** for debugging: some sanity checks for rank and invrank arrays **/
	@SuppressWarnings("unused")
	private void checkRank(int[] rank, Node[] invrank) {
		boolean [] used = new boolean[rank.length/2];
		for (int j = rank.length/2+1; j < rank.length; j++) {
			int i = rank[j];
			if (used[i]) {
				System.err.println("Rank error at entry " + i + " " + Arrays.toString(rank));
			}
			used[i] = true;
		}
		
		used = new boolean[rank.length];
		for (Node node : invrank) {
			if (node.getNr() < rank.length/2) {
				System.err.println("invalid node at entry " + node.getNr());
			}
			if (used[node.getNr()]) {
				System.err.println("invrank error at entry " + node.getNr() + " " + Arrays.toString(rank));
			}
			used[node.getNr()] = true;
		}
		
		for (int i = 0; i < invrank.length - 1; i++) {
			if (invrank[i].getHeight() > invrank[i+1].getHeight()) {
				System.err.println("Something wrong with rank");
			}
		}
	}


	/** for debugging: produce newick tree with taxa sorted and without branch lengths or metadata
	 * Note: this assumes taxa orderings in tree are the same as in the taxonset 
	 * **/
	@SuppressWarnings("unused")
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

	public static Integer [] getRankedClades(TreeInterface tree) {
		final Node [] nodes = tree.getNodesAsArray();
		if (nodes == null || nodes[nodes.length-1] == null) {
			if (tree instanceof Tree) {
				((Tree)tree).initArrays();
				// node array is not initialised
				return getRankedClades(tree);
			} else {
				throw new IllegalArgumentException("Don't know how to get node array from tree");				
			}
		}
		int leafNodeCount = tree.getLeafNodeCount();
		int internalNodeCount = tree.getInternalNodeCount();

		final Integer [] clades = new Integer[internalNodeCount];
		for (int i = 0; i < clades.length; i++) {
			clades[i] = leafNodeCount + i; 
		}
		
		Arrays.sort(clades, new Comparator<Integer>() {
			@Override
			public int compare(Integer c1, Integer c2) {
				if (nodes[c1].getHeight() > nodes[c2].getHeight())
					return 1;
				if (nodes[c1].getHeight() < nodes[c2].getHeight())
					return -1;
				// Log.debug.println("Tie break");
				return 0;
			}
		});
		return clades;
	}


	/** return tree at location n on path between tree1 and tree2 **/
	public Tree pathelement(Tree treeOther, Tree tree, int n) {
		Integer [] cladesOther = getRankedClades(treeOther);
	
		// get clades in ranked order
		Tree treeCopy = new Tree();
		treeCopy.assignFrom((Tree) tree);
		
		Map<String,Integer> map = new HashMap<>();
		for (int i = 0; i < treeCopy.getLeafNodeCount(); i++) {
			Node leaf = treeCopy.getNode(i);
			map.put(leaf.getID(), i);
			leaf.setHeight(-1e-13);
		}

		Integer[] clades = getRankedClades(treeCopy);
		
		// pre-calculate node rankings of other tree
		int [] rank = new int[tree.getNodeCount()];
		Node [] invrank = new Node[clades.length];
		for (int i = 0; i < clades.length; i++) {
			rank[clades[i]] = i;
			invrank[i] = treeCopy.getNode(clades[i]);
		}

		// FindPath algorithm of 
		// Collienne, L., Gavryushkin, A. Computing nearest neighbour interchange distances between ranked phylogenetic trees. J. Math. Biol. 82, 8 (2021). 
		// https://doi.org/10.1007/s00285-021-01567-5
		// only calculates the distance = length of the path, not the path itself
		double d = 0;
		nniCount = 0;
		for (int i = 0; i < cladesOther.length - 1 && d < n; i++) {
			List<String> cluster = new ArrayList<>();
			Node current = treeOther.getNode(cladesOther[i]);
			getTaxa(current, cluster);
			Node targetNode = mrca(treeCopy, cluster, map);
			int rank0 = rank[targetNode.getNr()];
			for (int j = rank0; j > i; j--) {
				
				// trickle targetnode down to rank i
				if (invrank[j-1].getParent() == targetNode) {
					// perform NNI
					Node node = invrank[j-1];
					Node left = node.getLeft();
					Node right = node.getRight();
					Node child;
					if (nodesTraversed[left.getNr()]) {
						child = left;
					} else {
						child = right;
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
					nniCount++;
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

				// for debugging:
				// checkRank(rank, invrank);
				// System.err.println(d + ": " + invrank[clades.size()-1].toString());
			}				
		}
		
		Node root = treeCopy.getRoot();
		while (!root.isRoot()) {
			root = root.getParent();
		}
		treeCopy.setRoot(root);
		
		treeCopy.initArrays();
		return treeCopy;
	}	
}
