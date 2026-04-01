package beastlabs.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.parameter.Parameter;
import beastfx.app.treeannotator.TreeAnnotator;
import beastfx.app.treeannotator.TreeAnnotator.MemoryFriendlyTreeSet;
import beastfx.app.util.TreeFile;

@Description("Source of trees for post hoc analysis")
public class TreeStateNodeSource extends BEASTObject implements StateNodeSource {
	final public Input<TreeFile> fileInput = new Input<>("treefile", "Tree log containing trees from the posterior.", new TreeFile("[[none]]"));
	final public Input<String> srcIdInput = new Input<>("treeID", "id of the tree that should be initialised from the tree file");
    final public Input<State> stateInput = new Input<>("state", "elements of the state space");
	final public Input<String> metaSrcIdInput = new Input<>("value", "comma delimited string of map enties. "
			+ "Each entry maps a meta data label to a state node entry. "
			+ "For example BranchRates.c:dna=rate");

    
	private MemoryFriendlyTreeSet treefile;
	private Tree tree;
	private String [] taxa;
	private int [] taxonmapping;
	private List<StateNode> metaDataStateNodes;
	private List<String> metaDataLabels;

	@Override
	public void initAndValidate() {
		try {
			treefile  = new TreeAnnotator().new MemoryFriendlyTreeSet(fileInput.get().getPath(), 0);
			treefile.reset();
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

		taxa = new String[tree.getLeafNodeCount()];
		for (int i = 0; i < taxa.length; i++) {
			taxa[i] = tree.getNode(i).getID();
		}
		
		String meta = metaSrcIdInput.get();
		if (meta != null && meta.trim().length() > 0) {
			String [] strs = meta.split(",");
			metaDataStateNodes = new ArrayList<>();
			metaDataLabels = new ArrayList<>();
			
			for (String str : strs) {
				String [] strs2 = str.split("=");
				if (strs2.length != 2) {
					throw new IllegalArgumentException("value attribute should be comma separated list of stateNodeId=metaDataLable pairs");
				}
				String id = strs2[0].trim();
				boolean found = false;
				for (StateNode s : stateInput.get().stateNodeInput.get()) {
					if (s.getID().equals(id)) {
						if (!(s instanceof Parameter<?>)) {
							throw new IllegalArgumentException("Expected a parameter statenode (id=" + s.getID() + ")");
						}
						metaDataStateNodes.add(s);
						found = true;
						break;
					}
				}
				if (!found) {
					throw new IllegalArgumentException("Could not find statenode with id=" + id);
				}
				metaDataLabels.add(strs2[1].trim());	
			}
		}

		Log.info(getID() + " initialised");
	}

	@Override
	public void initStateNodes(int i) {
		Tree newtree = null;
		try {
			newtree = treefile.next();
		} catch (IOException e) {
			throw new RuntimeException("Expected more trees : " + e.getMessage());
		}

		if (taxonmapping == null) {
			taxonmapping = new int[taxa.length*2-1];
			for (int k = 0; k < taxa.length; k++) {
				taxonmapping[k] = indexOf(newtree.getNode(k).getID(), taxa);
			}
			for (int k = taxa.length; k < taxonmapping.length; k++) {
				taxonmapping[k] = k;
			}			
		}

		for (int k = 0; k < taxa.length; k++) {
			newtree.getNode(k).setNr(taxonmapping[k]);
		}
		
		tree.assignFrom(newtree);
		
		if (metaDataStateNodes != null) {
			Node [] nodes = tree.getNodesAsArray();
			for (int j = 0; j < nodes.length; j++) {
				for (int k = 0; k < metaDataLabels.size(); k++) {
					Parameter sn = (Parameter) metaDataStateNodes.get(k);
					int index = nodes[j].getNr();
					if (index < sn.getDimension()) {
						Object o = nodes[j].getMetaData(metaDataLabels.get(k));
						sn.setValue(index, o);
					}
				}
			}
		}
	}


	private int indexOf(String id, String[] taxa2) {
		for (int i = 0; i < taxa2.length; i++) {
			if (taxa2[i].equals(id)) {
				return i;
			}
		}
		throw new RuntimeException("tree from file " + fileInput.get().getName() + " contains a taxon ("+ id +")that is not in the target tree");
	}

}
