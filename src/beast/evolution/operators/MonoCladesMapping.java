package beast.evolution.operators;


import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.MultiMonophyleticConstraint;

import java.util.HashSet;
import java.util.List;

public class MonoCladesMapping {
    static private boolean internalTest = false;

    // Return a mapping from nodes to their 'constraint group'. Every node has a "most recent monophyletic ancestor" (MRMA), which is the first
    // node that is a root of a monophyletic clade on the path from the node to the root (nodes which are not under any specific constarint have
    // the root as their MRMA, since the root is monophyletic by construction). Two nodes are in the same constraint group if they have the same
    // MRMA.
    //
    // Return a mapping from nodes to their MRMA "index", a number between -1 (the index of the root group) and k, where k is the number of
    // explicit constraints in mc. The mapping is indeed by node number.
    //
    static public int[] setupNodeGroup(Tree tree, final MultiMonophyleticConstraint mc) {
        final int nodeCount = tree.getNodeCount();
        int[] nodeToCladeGroup = new int[nodeCount];
        if( mc != null ) {
            final List<List<String>> constraints = mc.getConstraints();
            HashSet<String> u[] = new HashSet[constraints.size()];

            for(int k = 0; k < constraints.size(); ++k) {
                u[k] = new HashSet<>(constraints.get(k));
            }

            HashSet<Integer> x[] = new HashSet[nodeCount];
            nodeToCladeGroup = new int[nodeCount];

            Node[] post = new Node[nodeCount];
            post = tree.listNodesPostOrder(null, post);
            for( final Node n : post ) {
                final int nr = n.getNr();
                x[nr] = new HashSet<>();
                if( n.isLeaf() ) {
                    final String id = n.getID();
                    for(int k = 0; k < u.length; ++k) {
                        if( u[k].contains(id) ) {
                            x[nr].add(k);
                        }
                    }
                } else {
                    for (int nc = 0; nc < n.getChildCount(); ++nc) {
                        final int cnr = n.getChild(nc).getNr();
                        if( x[nr].isEmpty() ) {
                            x[nr].addAll(x[cnr]);
                        } else {
                            x[nr].retainAll(x[cnr]);
                        }
                    }
                }
                nodeToCladeGroup[nr] = -1;

                if( x[nr].isEmpty() || (x[nr].size() == 1 && x[nr].contains(-1) )) {
                    x[nr].add(-1);
                } else {
                    int sz = nodeToCladeGroup.length + 1;
                    for (Integer i : x[nr]) {
                        if( u[i].size() < sz ) {
                            nodeToCladeGroup[nr] = i;
                            sz = u[i].size();
                        }
                    }
                }
            }

            if( internalTest ) {
                for( final Node n : post ) {
                    if( n.isRoot() ) {
                        continue;
                    }
                    int nr = n.getNr();

                    int z = nodeToCladeGroup[nr];
                    if( z == -1 ) {
                        assert nodeToCladeGroup[n.getParent().getNr()] == -1;
                    } else {
                        final List<Node> cn = n.getAllLeafNodes();
                        for (Node c : cn) {
                            assert u[z].contains(c.getID()) : c.getID();
                        }
                        if( z != nodeToCladeGroup[n.getParent().getNr()] ) {
                            assert u[z].size() == cn.size();
                        }
                    }
                }
            }
        } else {
            for(int k = 0; k < nodeToCladeGroup.length; ++k) {
               nodeToCladeGroup[k] = -1;
            }
        }
        return nodeToCladeGroup;
    }
}
