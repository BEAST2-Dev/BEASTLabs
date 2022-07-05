package beastlabs.evolution.branchratemodel;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
import beastlabs.evolution.tree.PrunedTree;
import beast.base.evolution.tree.Node;
import beast.base.evolution.branchratemodel.*;
import beast.base.evolution.tree.TreeInterface;

@Description("Tree containing a subset of nodes from another tree")
public class PrunedRelaxedClockModel extends BranchRateModel.Base {
    // We can't use a generic BranchRateModel here, since there is no unifying API for getting which rates changed
    public Input<UCRelaxedClockModel>  baseRates = new Input<UCRelaxedClockModel>("rates", "clock model used on original tree", Input.Validate.REQUIRED);
    public Input<PrunedTree>  tree = new Input<PrunedTree>("tree", "source tree to be pruned", Input.Validate.REQUIRED);

    private PrunedTree ptree; // = tree.get();
    private UCRelaxedClockModel rates; // = baseRates.get();
    private TreeInterface btree; // = ptree.m_tree.get();
    private boolean normed = false;

    @Override
    public void initAndValidate() {
        ptree = tree.get();
        ptree.keepTrack();

        rates = baseRates.get();
        btree = ptree.m_tree.get();
        normed = rates.normalizeInput.get();
    }

    @Override
    public double getRateForBranch(Node node) {
        if (node.isRoot()) {
            // root has no rate
            return 1;
        }

        // node mapped to base node X
        // parent mapped to real Y
        // we need the rate and branch length on the path, get avg weighted rate and divide by total branch.

        double branch = node.getLength();

        final int b = ptree.baseNode(node.getNr());
        final int e = ptree.baseNode(node.getParent().getNr());

        double tot = 0; //r1 * b1.getLength();
        Node x = btree.getNode(b);
        do {
            tot += x.getLength() * rates.getRateForBranch(x);
            x = x.getParent();                                          assert ( x != null );

        } while ( x.getNr() != e );

        return tot/branch;
    }

    int countTot = 0;
    int countRQ = 0;

    protected boolean requiresRecalculation() {
        countTot += 1;
        boolean r = rq();
        countRQ += r ? 1 : 0;
        //System.out.println("PRC = " + r);
        return r;
    }

    private boolean rq() {
        if (! (rates.getMeanRate() instanceof RealParameter) )
            throw new UnsupportedOperationException("meanRate has to be RealParameter !");

        // before tree, since tree might not be valid if only rates changed
        if( rates.getDistribution().isDirtyCalculation() || ((RealParameter) rates.getMeanRate()).somethingIsDirty() ) {
            return true;
        }

        if( ptree.somethingIsDirty() || normed ) {
            return true;
        }

//        if( nodesSet.isEmpty() ) {
//            setOfBaseNodes();
//        }

        if( ptree.anyPathChange() )  {
            return true;
        }

        // rates distribution changed, global!!

        if( ! rates.isDirtyCalculation() ) {
            // base tree changed - pruned tree unchanged - no rates change
            // assert false;
            return false;
        }

        // set can change
        final int dim = rates.getCategories().getDimension();                           assert dim + 1 == btree.getNodeCount();
        final int nRoot = btree.getRoot().getNr();
        for(int k = 0; k < dim; ++k) {
            if( rates.getCategories().isDirty(k) ) {
                if( ptree.onPath(k == nRoot ? dim - 1 : k) ) {
                    return true;
                }
            }
        }

        return false;
    }

//    private Set<Integer> nodesSet = new HashSet<Integer>();
//
//    private void setOfBaseNodes() {
//        Set<Integer> a = new HashSet<Integer>();
//
//        for(int i = 0; i < ptree.getNodeCount(); ++i) {
//            a.add(ptree.baseNode(i));
//        }
//
//        for(int i : a ) {
//            Node node = btree.getNode(i);
//            node = node.getParent();
//            while( ! a.contains(node.getNr()) )  {
//              a.add(node.getNr());
//              node = node.getParent();
//            }
//        }
//        nodesSet = a;
//    }
}
