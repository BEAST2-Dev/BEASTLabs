package beast.evolution.operators;

import java.util.List;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;


/**
 * Implements the Nearest Neighbor Interchange (NNI) operation. This particular
 * implementation assumes explicitly bifurcating trees. It works similar to the
 * Narrow Exchange but with manipulating the height of a node if necessary.
 * Adapted from BEAST1 dr.evomodel.operators.NNI by Sebastian Hoehna 
 */
@Description("Nearest Neighbor Interchange (NNI) operation")
public class NNI extends RestrictedSubtreeSlide {

    private Tree tree = null;

    @Override
    public void initAndValidate() {
        this.tree = treeInput.get();
        super.initAndValidate();
    }
    
    @Override
    public double proposal() {
        final int nNodes = tree.getNodeCount();
        final Node root = tree.getRoot();
        // TODO: handle markClades flag
        final boolean  markClades = markCladesInput.get();


        Node i;
        // 0. determine set of candidate nodes
        if (nrOfTaxa.length > 0) {
        	// we do not want to choose nodes that are constrained
        	List<Node> candidates = getCandidateNodes(tree);
        	for (int j = candidates.size() - 1; j  >= 0; j--) {
        		i = candidates.get(j);
        		if (root == i || i.getParent() == root) {
        			candidates.remove(j);
        		}
        	}
        	if (candidates.size() == 0) {
        		return Double.NEGATIVE_INFINITY;
        	}

	        // get a random node where neither you or your father is the root
	        do {
	            i = candidates.get(Randomizer.nextInt(candidates.size()));
	        } while( root == i || i.getParent() == root );
        } else {
	        do {
              i = tree.getNode(Randomizer.nextInt(nNodes));
	        } while( root == i || i.getParent() == root );
        }

        // get parent node
        final Node iParent = i.getParent();
        // get parent of parent -> grant parent :)
        final Node iGrandParent = iParent.getParent();
        // get left child of grant parent -> uncle
        Node iUncle = iGrandParent.getChild(0);
        // check if uncle == father
        if( iUncle == iParent ) {
            // if so take right child -> sibling of father
            iUncle = iGrandParent.getChild(1);
        }

        // change the height of my father to be randomly between my uncle's
        // heights and my grandfather's height
        // this is necessary for the hastings ratio to do also if the uncle is
        // younger anyway

        final double heightGrandfather = iGrandParent.getHeight();
        final double heightUncle = iUncle.getHeight();
        final double minHeightFather = Math.max(heightUncle, getOtherChild(iParent, i).getHeight());
        final double heightI = i.getHeight();
        final double minHeightReverse = Math.max(heightI, getOtherChild(iParent, i).getHeight());

        double ran;
        do {
            ran = Math.random();
        } while( ran == 0.0 || ran == 1.0 );

        // now calculate the new height for father between the height of the
        // uncle and the grandparent
        final double newHeightFather = minHeightFather + (ran * (heightGrandfather - minHeightFather));
        // set the new height for the father
        iParent.setHeight(newHeightFather);

        // double prForward = 1 / (heightGrandfather - minHeightFather);
        // double prBackward = 1 / (heightGrandfather - minHeightReverse);
        // hastings ratio = backward Prob / forward Prob
        final double hastingsRatio = Math.log((heightGrandfather - minHeightFather) / (heightGrandfather - minHeightReverse));
        // now change the nodes
        exchangeNodes(i, iUncle, iParent, iGrandParent);


        return hastingsRatio;
    }

    /* exchange sub-trees whose root are i and j */
    protected void exchangeNodes(Node i, Node j,
                                 Node iP, Node jP) {
        // precondition iP -> i & jP -> j
        replace(iP, i, j);
        replace(jP, j, i);
        // postcondition iP -> j & iP -> i
    }
}
