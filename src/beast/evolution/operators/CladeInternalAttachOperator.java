package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.math.distributions.MRCAPrior;
import beast.util.Randomizer;

import java.util.*;

@Description("Variant of AttachOperator which acts on a random subclade of " +
        "one of the clades specified with the 'restricted' parameter.")

public class CladeInternalAttachOperator extends AttachOperator {

    public CladeInternalAttachOperator() {
        useOnlyInput.setRule(Validate.REQUIRED);
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        if(topOnlyInput.get()) {
            throw new RuntimeException("This version of AttachOperator is not compatible with topOnly.");
        }
    }

    @Override
    protected Node getNode(Tree tree, final Node[] post) {
        // Choose one of the provided clades to work within
        int k = Randomizer.nextInt( useOnly.size() );
        final MRCAPrior mrcaPrior = useOnly.get(k);
        final Node node = mrcaPrior.getCommonAncestor();

        // Choose a random node inside this clade
        final List<Node> candidates = node.getAllChildNodes();
        Node n;
        do {
            k = Randomizer.nextInt(candidates.size());
            n = candidates.get(k);
        } while( n.isRoot() || n.getParent().isRoot() );
        return n;
    }

}
