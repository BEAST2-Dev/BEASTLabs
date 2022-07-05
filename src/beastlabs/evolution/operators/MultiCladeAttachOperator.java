package beastlabs.evolution.operators;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.MRCAPrior;
import beast.base.util.Randomizer;

import java.util.*;

@Description("Variant of AttachOperator which acts on the parent or  " +
        "grandparent of one of the clades specified with the 'restricted' " +
        "parameter, to move groups of 2, 3 or 4 tightly related families " +
        "around as a unit.")

public class MultiCladeAttachOperator extends AttachOperator {

    public MultiCladeAttachOperator() {
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
        // Choose one of the provided clades to work within, but avoid any
        // clade whose parent is the root of the tree
        Node node;
        do {
            int k = Randomizer.nextInt( useOnly.size() );
            final MRCAPrior mrcaPrior = useOnly.get(k);
            node = mrcaPrior.getCommonAncestor();
        } while(node.getParent().isRoot());

        // Go up a step or two
        node = node.getParent();    // Always go up one step
        if(!node.getParent().isRoot() && Randomizer.nextBoolean()) {
            node = node.getParent();    // Sometimes go up two
        }
        return node;
    }

}
