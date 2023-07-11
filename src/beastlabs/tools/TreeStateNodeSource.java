package beastlabs.tools;

import java.io.IOException;
import java.util.List;

import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.evolution.tree.Tree;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beastfx.app.treeannotator.TreeAnnotator;
import beastfx.app.treeannotator.TreeAnnotator.MemoryFriendlyTreeSet;
import beastfx.app.util.TreeFile;

public class TreeStateNodeSource extends BEASTObject implements StateNodeSource {
	final public Input<TreeFile> fileInput = new Input<>("tree", "Tree log containing trees from the posterior.", new TreeFile("[[none]]"));
	final public Input<String> srcIdInput = new Input<>("treeID", "id of the tree that should be initialised from the tree file");
    final public Input<State> stateInput = new Input<>("state", "elements of the state space");

    
	private MemoryFriendlyTreeSet treefile;
	private Tree tree;

	@Override
	public void initAndValidate() {
		try {
			treefile  = new TreeAnnotator().new MemoryFriendlyTreeSet(fileInput.get().getPath(), 0);
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage());
		}

		tree = null;
		List<StateNode> sns = stateInput.get().stateNodeInput.get();
		String treeID = srcIdInput.get();
		for (StateNode sn : sns) {
			if (sn.getID().equals(treeID)) {
				tree = (Tree) sn;
				break;
			}
		}
		if (tree == null) {
			throw new IllegalArgumentException("could not find tree with id = " + treeID);
		}
	}

	@Override
	public void initStateNodes(int i) {
		
		Tree newtree = null;
		try {
			newtree = treefile.next();
		} catch (IOException e) {
			throw new RuntimeException("Expected more trees : " + e.getMessage());
		}
		tree.assignFrom(newtree);
	}

}
