package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.MultiMonophyleticConstraint;
import beast.util.Randomizer;

import java.util.*;

@Description("Detach a clade and re-attach it at the same height somewhere else. A BEAST object can guide the moves by providing a " +
        "distance between clades via the DistanceProvider interface. Clades with lower distance should be more closely related and therefore the " +
        "move more likely to get accepted.")
public class AttachOperator extends TreeOperator {
    enum Method {
        DISTANCE("distance"),
        SQRT("sqrt"),
        SQR("sqr");

        Method(final String name) {
            this.ename = name;
        }

        public String toString() {
            return ename;
        }

        private final String ename;
    }

    // Provide weights for attach operator. Weight is a clade based statistic that is based only on the properties associated with tips and not on
    // the clade topology. To be useful the distance between clades should reflect in some way the distance between those two groups, i.e. it
    // should be some kind of a "mean" statistic for tip values.

    public interface DistanceProvider {
        // Data associated with tip or node
        interface Data {}

        // Return a mapping from taxon name to its associated data for all taxa
        Map<String,Data> init(Set<String> taxa);

       // Data combine(Data info1, Data info2);

        // return a new 'empty' entry
        Data empty();

        // clear an existing data entry
        void clear(Data d);

        // combine 'data' and 'with' and store the result back to 'data'
        void update(Data info, Data with);

        // distance between two summaries
        double dist(Data info1, Data info2);
    };

    public Input<DistanceProvider> weightsInput = new Input<>("weights", "Provide distances between clades (data, not tree based)", null, Input
            .Validate.OPTIONAL);

    public Input<Boolean> tipsOnlyInput = new Input<Boolean>("tipsOnly", "Move only nodes attached to tips.", false);

    public Input<Method> initMethod = new Input<Method>("method", "Sqrt takes square root of distance (default distance)",
            Method.DISTANCE, Method.values());

    public Input<Boolean> topOnlyInput = new Input<Boolean>("topOnly", "Consider only nodes not under any monophyly constraint.", false);

    public final Input<MultiMonophyleticConstraint> constraintsInput =
              new Input<>("constraints", "Respect clade constrainted, i.e make no moves which violate some constraint.",
                      null, Input.Validate.OPTIONAL);

    private DistanceProvider weightProvider;

    // Node (by index) to the index of their "clade group".
    // All nodes in the same clade group are descendants of the root of one monophyly constraint, with no other constraints on the path between
    // node and root. Unconstrained nodes have a clade group with index -1.
    private int[] nodeToCladeGroup = null;

    private boolean internalTest = false;

    //private Method method;

    DistanceProvider.Data weights[];

    @Override
    public void initAndValidate() {
        assert ! markCladesInput.get();  // need to implement SOON!!

        final Tree tree = treeInput.get();
        weightProvider = weightsInput.get();
        if( weightProvider == null ) {
            weightProvider = new DistanceProvider() {
                @Override
                public Map<String, Data> init(Set<String> taxa) {
                    HashMap<String, Data> m = new HashMap<String, Data>();
                    for( String s : taxa ) {
                        m.put(s, empty());
                    }
                    return m;
                }

                class Data1 implements Data {};
                @Override
                public Data empty() {
                    return new Data1();
                }

                @Override
                public void clear(Data d) {}

                @Override
                public void update(Data info, Data with) {}

                @Override
                public double dist(Data info1, Data info2) {
                    return 1;
                }
            };
        }

        final int nc = tree.getNodeCount();
        weights = new DistanceProvider.Data[nc];
        final Map<String, DistanceProvider.Data> init = weightProvider.init(new HashSet<String>(tree.getTaxonset().asStringList()));
        for( Node tip : tree.getExternalNodes() ) {
            weights[tip.getNr()] = init.get(tip.getID());
        }
        for(int i = 0; i < nc; ++i) {
            final Node n = tree.getNode(i);
            if( !n.isLeaf() ) {
              weights[n.getNr()] = weightProvider.empty();
            }
        }

        //method =  initMethod.get();
    }

    private int[] setupNodeGroup(Tree tree) {
        final MultiMonophyleticConstraint mc = constraintsInput.get();
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
        }
        return nodeToCladeGroup;
    }

    private Node getNode(Tree tree, final Node[] post) {
        boolean topOnly = topOnlyInput.get();

        if( tipsOnlyInput.get() ) {
            assert nodeToCladeGroup == null; // implement later
            assert ! topOnly;

            final List<Node> tips = tree.getExternalNodes();
            final int nTip = Randomizer.nextInt(tree.getLeafNodeCount());
            return tips.get(nTip);
        }

        Node n;
        do {
            final int k = Randomizer.nextInt(post.length);
            n = post[k];
        } while( n.isRoot() || n.getParent().isRoot() ||
                ( topOnly && nodeToCladeGroup[n.getParent().getNr()] != -1 )   ||
                (nodeToCladeGroup != null &&
                        nodeToCladeGroup[n.getParent().getNr()] != nodeToCladeGroup[n.getParent().getParent().getNr()]) );
        return n;
    }

    private int ncheck = 1000;
    private int prevNodeCount = -1;

    @Override
    public double proposal() {
        final Tree tree = treeInput.get(this);
        Node[] post = new Node[tree.getNodeCount()];
        post = tree.listNodesPostOrder(null, post);

        if (tree.getNodeCount() != prevNodeCount) {
            nodeToCladeGroup = setupNodeGroup(tree);
        	prevNodeCount = tree.getNodeCount();
        } else {
            if( ncheck == 0 && internalTest ) {
                int[] xxx = setupNodeGroup(tree);
                for (Node n : post) {
                    if( xxx[n.getNr()] != nodeToCladeGroup[n.getNr()] ) {
                        String dd = n.toNewick()
                                + ' ' + n.getNr() + ' ' + xxx[n.getNr()] + ' ' + nodeToCladeGroup[n.getNr()];
                        assert false : dd;
                    }
                }
                ncheck = 1001;
            }
            ncheck -= 1;
        }

        List<Integer> cans = new ArrayList<>(post.length);
        Node node;
        {
            int ntries = 1000;
            do {
                ntries -= 1;

                node = getNode(tree, post);
                final int nodeNr = node.getNr();
                final Node parent = node.getParent();
                final int nParent = parent.getNr();

                assert !(parent.isRoot());

                final double nodeParentHeight = parent.getHeight();

                //int[] w = new int[weights.length];
                for (final Node n : post) {
                    if( n.getHeight() > nodeParentHeight ) {
                        continue;
                    }
                    assert !n.isRoot();

                    final int nr = n.getNr();

                    if( n.isLeaf() ) {
                        assert weights[n.getNr()] != null : "" + n.getNr();
                    } else {
                        DistanceProvider.Data d1 = weights[nr];
                        weightProvider.clear(d1);
                        for (int nc = 0; nc < n.getChildCount(); ++nc) {
                            final Node c = n.getChild(nc);
                            weightProvider.update(d1, weights[c.getNr()]);
                        }

                    }

                    if( nr == nParent ) { // store in parent location sans son to be detached
                        DistanceProvider.Data d1 = weights[nr];
                        weightProvider.clear(d1);
                        for (int nc = 0; nc < n.getChildCount(); ++nc) {
                            final Node c = n.getChild(nc);
                            final int cnr = c.getNr();
                            if( cnr == nodeNr ) { // skip node
                                continue;
                            }
                            weightProvider.update(d1, weights[cnr]);
                        }
                    } else if( nr != nodeNr ) {   // node itself is not a candidate
                        Node p = n.getParent();
                        if( p.getHeight() > nodeParentHeight &&
                                (nodeToCladeGroup == null || nodeToCladeGroup[p.getNr()] == nodeToCladeGroup[nParent]) ) {
                            cans.add(nr);
                        }
                    }
                }
            } while (cans.size() == 0 && ntries > 0);
        }
        final int nodeNr = node.getNr();
        final Node parent = node.getParent();
        final int nParent = parent.getNr();

        assert !(parent.isRoot());

        final double nodeParentHeight = parent.getHeight();

        if( cans.size() == 0 ) {
           return Double.NEGATIVE_INFINITY;
        }

        if( cans.size() == 1 ) {
            final Node n = tree.getNode(cans.get(0));

            assert n.getHeight() < nodeParentHeight;
            assert n.getParent().getHeight() >= nodeParentHeight;
            assert n.getNr() != node.getNr();
            assert parent.getNr() != n.getNr();
            assert parent.getNr() != n.getParent().getNr();

            reAttach(node, n);
            return 0;
        }

        double[] w1 = new double[cans.size()];
        for(int k = 0; k < cans.size(); ++k) {
            final int nr = cans.get(k);
            final double d = weightProvider.dist(weights[nodeNr], weights[nr]);

            assert d > 0 : "" + nodeNr + " " + nr + "" + d;
            w1[k] = 1 / d;
        }

        double tot = 0;
        for (double x : w1) {
            tot += x;
        }

        final double wx = 1/weightProvider.dist(weights[nodeNr], weights[nParent]);
        final double atot = tot + wx;

        double[] w2 = new double[cans.size()];
        double s = 0;
        for(int k = 0; k < w1.length; ++k) {
            s += w1[k];
            w2[k] = s/tot;
        }
        final int i = Randomizer.randomChoice(w2);
        assert( 0 <= i && i < cans.size() );

        Node n = tree.getNode(cans.get(i));
        assert n.getHeight() < nodeParentHeight;
        assert n.getParent().getHeight() >= nodeParentHeight;
        assert n.getNr() != node.getNr();
        assert parent.getNr() != n.getNr();
        assert parent.getNr() != n.getParent().getNr();

        reAttach(node, n);

        final double pi = w2[i] - (i > 0 ? w2[i - 1] : 0);
        final double wtot = atot - pi * tot;
        final double px = wx / wtot;
        return Math.log(px / pi);
    }

//    private double dist(double[] doubles, double[] doubles1) {
//        double s = 0;
//        for(int k = 0; k < doubles.length; ++k) {
//            double x = (doubles[k] - doubles1[k]);
//            s += x*x;
//        }
//        s = (s == 0) ? 1e-8 : s;
//        switch (method) {
//            case DISTANCE: break;
//            case SQRT: s = Math.sqrt(s); break;
//            case SQR: s =  s * s; break;
//        }
//        return s;
//    }

    private void detach(Node x, Node xP) {
        // remove xP from sons of xPP
        // go over other sons of xP, make them sons of xPP instead of xP
        //
        // This leaves xP floating with x as a single child
        Node xPP = xP.getParent();
        xPP.removeChild(xP);
        final List<Node> children = new ArrayList<Node>(xP.getChildren());
        for( Node c : children ) {
            if( x.getNr() != c.getNr() ) {
                xP.removeChild(c);
                xPP.addChild(c);
                c.makeDirty(Tree.IS_FILTHY);
            }
        }
        xPP.makeDirty(Tree.IS_FILTHY);

    }

    private void makeSon(Node n, Node son, Node instead) {
        n.removeChild(instead);
        son.addChild(instead);
        n.addChild(son);

        n.makeDirty(Tree.IS_FILTHY);
        son.makeDirty(Tree.IS_FILTHY);
    }

    void reAttach(Node moving, Node newSibling) {
        // move node 'moving' so that after the move it is a sibling of 'newSibling'

        Node movingP = moving.getParent();
        detach(moving, movingP);
        Node newSiblingP = newSibling.getParent();
        makeSon(newSiblingP, movingP, newSibling);
    }
}
