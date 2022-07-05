package beastlabs.evolution.operators;


import java.lang.reflect.Array;
import java.util.*;

import beast.pkgmgmt.Arguments;
import beastlabs.math.distributions.MultiMonophyleticConstraint;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.operator.SubtreeSlide;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;

@Description("Subtree-slide operator that only considers part of the tree defined by "
		+ "the predecessors of a set of clades.")
public class RestrictedSubtreeSlide extends SubtreeSlide {
	public Input<List<TaxonSet>> cladesInput = new Input<List<TaxonSet>>("clade", "defines predecessor of clade to be considered by Subtree-slide operation",
            new ArrayList<TaxonSet>());

    public final Input<MultiMonophyleticConstraint> cladesSetInput = new Input<>("clades", "all clades encoded by one unresolved tree.", null,
            Input.Validate.OPTIONAL);
    
    public final Input<Boolean> topLevelOnlyInput = new Input<Boolean>("topLevelOnly", "use only constraints that are not nested", false); 

    // array of flags to indicate which taxa are in the set
    boolean[][] isInTaxaSet;
    int[] nrOfTaxa;
    // array of indices of taxa
    int[][] taxonIndex;
    
    @Override
    public void initAndValidate() {
    	try {
	    	Tree tree = treeInput.get();
	        final List<String> sTaxaNames = new ArrayList<String>();
            Collections.addAll(sTaxaNames, tree.getTaxaNames());

            final List<List<String>> constraints = cladesSetInput.get() != null ? cladesSetInput.get().getConstraints() : null;
            if (topLevelOnlyInput.get() && constraints != null) {
            	// remove nested constraints
            	for (int i = 0; i < constraints.size(); i++) {
            		List<String> c1 = constraints.get(i);
                	for (int j = 0; j < constraints.size(); j++) {
                		if (i != j) {
	                		List<String> c2 = constraints.get(j);
	                		if (isNested(c1, c2)) {
	                			c1.clear();
	                		}
                		}
                	}            		
            	}
            	for (int i = constraints.size() - 1; i >= 0 ; i--) {
            		if (constraints.get(i).size() == 0) {
            			constraints.remove(i);
            		}
            	}            	
            }
            
	        final int n = cladesInput.get().size() + (constraints != null ? constraints.size() : 0);

	        isInTaxaSet = new boolean[n][];
	        nrOfTaxa = new int[n];
	        taxonIndex = new int[n][];
	        		
	        int j = 0;
	        for (TaxonSet clade : cladesInput.get()) {
		        // determine nr of taxa in taxon set
		        List<String> set = clade.asStringList();
		        nrOfTaxa[j] = set.size();
	
		        // determine which taxa are in the set
		        taxonIndex[j] = new int[nrOfTaxa[j]];
	            isInTaxaSet[j] = new boolean[sTaxaNames.size()];
	            int k = 0;
	            for (final String sTaxon : set) {
	                final int iTaxon = sTaxaNames.indexOf(sTaxon);
	                if (iTaxon < 0) {
	                    throw new Exception("Cannot find taxon " + sTaxon + " in data");
	                }
	                if (isInTaxaSet[j][iTaxon]) {
	                    throw new Exception("Taxon " + sTaxon + " is defined multiple times, while they should be unique");
	                }
	                isInTaxaSet[j][iTaxon] = true;
	                taxonIndex[j][k++] = iTaxon;
	            }
		        j++;
	        }

            if( constraints != null ) {
                for (final List<String> set : constraints) {
                    nrOfTaxa[j] = set.size();

                    // determine which taxa are in the set
                    taxonIndex[j] = new int[nrOfTaxa[j]];
                    isInTaxaSet[j] = new boolean[sTaxaNames.size()];
                    int k = 0;
                    for (final String sTaxon : set) {
                        final int iTaxon = sTaxaNames.indexOf(sTaxon);
                        if( iTaxon < 0 ) {
                            throw new Exception("Cannot find taxon " + sTaxon + " in data");
                        }
                        if( isInTaxaSet[j][iTaxon] ) {
                            throw new Exception("Taxon " + sTaxon + " is defined multiple times, while they should be unique");
                        }
                        isInTaxaSet[j][iTaxon] = true;
                        taxonIndex[j][k++] = iTaxon;
                    }
                    j++;
                }
            }

            super.initAndValidate();
    	} catch (Exception e) {
    		throw new RuntimeException(e.getMessage());
    	}
    }

	private boolean isNested(List<String> c1, List<String> c2) {
		if (c1.size() == 0 || c2.size() == 0) {
			return false;
		}
		for (String c : c1) {
			if (c2.indexOf(c) == -1) {
				return false;
			}
		}
		return true;
	}

	@Override
	public double proposal() {
        final Tree tree = treeInput.get();
        // 0. determine set of candidate nodes
        List<Node> candidates = getCandidateNodes(tree);
        if (candidates.size() <= 1) {
        	return Double.NEGATIVE_INFINITY;
        }

        double logq;

        Node i;

        // 1. choose a random node avoiding root
        do {
            i = candidates.get(Randomizer.nextInt(candidates.size()));
        } while (i.isRoot());

        final Node iP = i.getParent();
        final Node CiP = getOtherChild(iP, i);
        final Node PiP = iP.getParent();

        // 2. choose a delta to move
        final double delta = getDelta();
        final double oldHeight = iP.getHeight();
        final double newHeight = oldHeight + delta;

        // 3. if the move is up
        if (delta > 0) {

            // 3.1 if the topology will change
            if (PiP != null && PiP.getHeight() < newHeight) {
                // find new parent
                Node newParent = PiP;
                Node newChild = iP;
                while (newParent.getHeight() < newHeight) {
                    newChild = newParent;
                    newParent = newParent.getParent();
                    if (newParent == null) break;
                }


                // 3.1.1 if creating a new root
                if (newChild.isRoot()) {
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);

                    iP.setParent(null);
                    tree.setRoot(iP);

                }
                // 3.1.2 no new root
                else {
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);
                    replace(newParent, newChild, iP);
                }

                iP.setHeight(newHeight);

                // 3.1.3 count the hypothetical sources of this destination.
                final int possibleSources = intersectingEdges(newChild, oldHeight, null);
                //System.out.println("possible sources = " + possibleSources);

                logq = -Math.log(possibleSources);

            } else {
                // just change the node height
                iP.setHeight(newHeight);
                logq = 0.0;
            }
        }
        // 4 if we are sliding the subtree down.
        else {

            // 4.0 is it a valid move?
            if (i.getHeight() > newHeight) {
                return Double.NEGATIVE_INFINITY;
            }

            // 4.1 will the move change the topology
            if (CiP.getHeight() > newHeight) {

                final List<Node> newChildren = new ArrayList<Node>();
                final int possibleDestinations = intersectingEdges(CiP, newHeight, newChildren);

                // if no valid destinations then return a failure
                if (newChildren.size() == 0) {
                    return Double.NEGATIVE_INFINITY;
                }

                // pick a random parent/child destination edge uniformly from options
                final int childIndex = Randomizer.nextInt(newChildren.size());
                final Node newChild = newChildren.get(childIndex);
                final Node newParent = newChild.getParent();
                
                // only allow the parent to be in the candidate set
                // we don't want to slide further down, otherwisw the HR is incorrect;
                // there is no way to move back to the original situation using this operator if we slide down below our candidate set
                if (!candidates.contains(newParent)) {
                	return Double.NEGATIVE_INFINITY;
                }


                // 4.1.1 if iP was root
                if (iP.isRoot()) {
                    // new root is CiP
                    replace(iP, CiP, newChild);
                    replace(newParent, newChild, iP);

                    CiP.setParent(null);
                    tree.setRoot(CiP);

                } else {
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);
                    replace(newParent, newChild, iP);
                }

                iP.setHeight(newHeight);

                logq = Math.log(possibleDestinations);
            } else {
                iP.setHeight(newHeight);
                logq = 0.0;
            }
        }
        return logq;
	}

    private int prevNodeCount = -1;
    private int[] nodeToCladeGroup = null;
   // private int[] fixedCands = null;
    private int[] ctops = null;

    private void initForCandidates(Tree tree) {
        Set<Integer> groups = new HashSet<>();
        groups.add(-1);
        List<Node> tops = new ArrayList<>();
        for( int k = 0; k < nodeToCladeGroup.length; ++k ) {
            Node n = tree.getNode(k);
            if( ! n.isLeaf() ) {
                int g = nodeToCladeGroup[k];
                if( g != -1 ) {
                    assert !n.isRoot();
                    Node p = n.getParent();
                    if( g != nodeToCladeGroup[p.getNr()] ) {
                        groups.add(nodeToCladeGroup[p.getNr()]);
                        tops.add(n);
                    }
                }
            }
        }
//        List<Integer> canIDs = new ArrayList<>();
//        for( int k = 0; k < nodeToCladeGroup.length; ++k ) {
//            Node n = tree.getNode(k);
//            if( ! n.isLeaf() ) {
//                int g = nodeToCladeGroup[k];
//                if( groups.contains(g) ) {
//                   canIDs.add(k);
//                }
//            }
//        }
//        {
//            // wonders of Java
//            fixedCands = new int[canIDs.size()];
//            int k = 0;
//            for( int i : canIDs ) {
//              fixedCands[k] = i;
//                k += 1;
//            }
//        }
        {
            int[] c = new int[tops.size()];
            int k = 0;
            for (Node n : tops) {
                if( !groups.contains(nodeToCladeGroup[n.getNr()]) ) {
                    c[k] = n.getNr();
                    ++k;
                }
            }
            ctops = new int[k];
            System.arraycopy(c,0,ctops,0,k);
        }
    }

	protected List<Node> getCandidateNodes(Tree tree) {
        List<Node> list = new ArrayList<Node>();
        if( tree.getNodeCount() != prevNodeCount ) {
            nodeToCladeGroup = MonoCladesMapping.setupNodeGroup(tree, cladesSetInput.get());
            prevNodeCount = tree.getNodeCount();
            if( !topLevelOnlyInput.get() ) {
                initForCandidates(tree);
            }
        }

        if( topLevelOnlyInput.get() ) {
            for (int k = 0; k < nodeToCladeGroup.length; ++k) {
                if( nodeToCladeGroup[k] == -1 ) {
                    list.add(tree.getNode(k));
                }
            }
        } else {
            Set<Node> candidates = new HashSet<Node>();
            for(int k = 0; k < ctops.length; ++k) {
                int nr = ctops[k];
                Node n = tree.getNode(nr);
                while( nodeToCladeGroup[nr] == nodeToCladeGroup[n.getParent().getNr()]) {
                    n = n.getParent();
                }
                ctops[k] = n.getNr();

                while( n != null && !candidates.contains(n) ) {
                    candidates.add(n);
                    n = n.getParent();
                }
            }
            if( false ) {  // ICC
                 Set<Node> candidates1 = new HashSet<Node>();
                for (int i = 0; i < nrOfTaxa.length; i++) {
                    calcPredecessorsOfClade(tree.getRoot(), new int[1], isInTaxaSet[i], nrOfTaxa[i], candidates1);
                }
                final boolean b = candidates.equals(candidates1);
                assert b;
            }
            list.addAll(candidates);
        }

        return list;
    }
	
    int calcPredecessorsOfClade(final Node node, final int[] nTaxonCount, boolean[] isInTaxaSet, int nrOfTaxa, Set<Node> candidates) {
        if (node.isLeaf()) {
            nTaxonCount[0]++;
            if (isInTaxaSet[node.getNr()]) {
                return 1;
            } else {
                return 0;
            }
        } else {
            int iTaxons = calcPredecessorsOfClade(node.getLeft(), nTaxonCount, isInTaxaSet, nrOfTaxa, candidates);
            final int nLeftTaxa = nTaxonCount[0];
            nTaxonCount[0] = 0;
            if (node.getRight() != null) {
                iTaxons += calcPredecessorsOfClade(node.getRight(), nTaxonCount, isInTaxaSet, nrOfTaxa, candidates);
                final int nRightTaxa = nTaxonCount[0];
                nTaxonCount[0] = nLeftTaxa + nRightTaxa;
                if (iTaxons >= nrOfTaxa) {
                	candidates.add(node);
                    return iTaxons + 1;
                }
//                if (iTaxons == nrOfTaxa) {
//                    return iTaxons + 1;
//                }
            }
            return iTaxons;
        }
    }

    private double getDelta() {
        if (!gaussianInput.get()) {
            return (Randomizer.nextDouble() * size) - (size / 2.0);
        } else {
            return Randomizer.nextGaussian() * size;
        }
    }

    private int intersectingEdges(Node node, double height, List<Node> directChildren) {
        final Node parent = node.getParent();

        if (parent.getHeight() < height) return 0;

        if (node.getHeight() < height) {
            if (directChildren != null) directChildren.add(node);
            return 1;
        }

        if (node.isLeaf()) {
            // TODO: verify that this makes sense
            return 0;
        } else {
            final int count = intersectingEdges(node.getLeft(), height, directChildren) +
                    intersectingEdges(node.getRight(), height, directChildren);
            return count;
        }
    }
}
