package beast.evolution.operators;

import beast.core.Description;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;


/**
 * This is an implementation of the Subtree Prune and Regraft (SPR) operator for
 * trees. It assumes explicitely bifurcating rooted trees. 
 * 
 * Regrafted trees will attach at the same height they got pruned from.
 * Adapted from BEAST1 dr.evomodel.operators.FNPR by Sebastian Hoehna 
 */
@Description("Subtree Prune Regraft move that attaches a subtree at the same height it got pruned from")
public class SPR extends TreeOperator {

    private Tree tree = null;

    @Override
    public void initAndValidate() throws Exception {
        tree = treeInput.get();
    }
    
    @Override
    public double proposal()  {
        Node grandfather, brother;
        double heightFather;

        final int nNodes = tree.getNodeCount();
        final Node root = tree.getRoot();

        Node i;

        int MAX_TRIES = 1000;

        for (int tries = 0; tries < MAX_TRIES; ++tries) {
           // get a random node whose father is not the root - otherwise
           // the operation is not possible
           do {
              i = tree.getNode(Randomizer.nextInt(nNodes));
           } while (root == i || i.getParent() == root);

           // int childIndex = (MathUtils.nextDouble() >= 0.5 ? 1 : 0);
           // int otherChildIndex = 1 - childIndex;
           // NodeRef iOtherChild = tree.getChild(i, otherChildIndex);

           Node father = i.getParent();
           grandfather = father.getParent();
           brother = getOtherChild(father, i);
           heightFather = father.getHeight();

           // NodeRef newChild = getRandomNode(possibleChilds, iFather);
           Node newChild = tree.getNode(Randomizer.nextInt(nNodes));

           if (newChild.getHeight() < heightFather
                 && root != newChild
                 && newChild.getParent().getHeight() > heightFather
                 && newChild != father
                 && newChild.getParent() != father) {
              Node newGrandfather = newChild.getParent();


              // Prune
              father.removeChild(brother);
              grandfather.removeChild(father);
              grandfather.addChild(brother);

              // Regraft
              newGrandfather.removeChild(newChild);
              father.addChild(newChild);
              newGrandfather.addChild(father);

              return 0.0;
           }
        }

        // failed
        return Double.NEGATIVE_INFINITY;
     }


}
