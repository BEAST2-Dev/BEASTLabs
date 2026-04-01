package beastlabs.evolution.tree;


import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.inference.StateNodeInitialiser;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;


@Description("Sets values of a parameter from metadata values associated with a newick tree")
public class InitParamFromTree extends beast.base.core.BEASTObject implements StateNodeInitialiser {
	public Input<Tree> m_tree = new Input<Tree>("tree", "tree containing some meta data", Validate.REQUIRED);
	public Input<RealVectorParam<? extends Real>> m_parameter = new Input<>("initial","parameter to be initialised", Validate.REQUIRED);
	public Input<String> m_sPattern = new Input<String>("pattern","name of the metadata item to be parsed", Validate.REQUIRED);
	
	@Override
	public void initAndValidate() {
		initStateNodes();
	}
	
	@Override
	public void initStateNodes() {
		Node root = m_tree.get().getRoot();
		double [] fValues = new double[m_tree.get().getNodeCount()];
		String sPattern = m_sPattern.get();
		traverse(root, fValues, sPattern);
		RealVectorParam<Real> p = new RealVectorParam<>();
		p.initByName("value", fValues);
		m_parameter.get().assignFrom(p);
	}

	/** traverse tree and pick up meta data values on the way **/
	private void traverse(Node node, double[] fValues, String sPattern) {
		if (node.metaDataString != null) {
            String[] sMetaData = node.metaDataString.split(",");
            for (int i = 0; i < sMetaData.length; i++) {
                try {
                    String[] sStrs = sMetaData[i].split("=");
                    if (sStrs.length != 2) {
                        throw new Exception("misformed meta data '" + node.metaDataString + "'. Expected name='value' pairs");
                    }
                    if (sStrs[0].equals(sPattern)) {
                    sStrs[1] = sStrs[1].replaceAll("[\"']", "");
            		fValues[node.getNr()] = Double.parseDouble(sStrs[1]);;
                    }
                } catch (Exception e) {
                    System.out.println("Warning 1333: Attempt to parse metadata failed: " + node.metaDataString);
                }
            }
        }		
		if (!node.isLeaf()) {
			traverse(node.getLeft(), fValues, sPattern);
			traverse(node.getRight(), fValues, sPattern);
		}
	}

	@Override
	public void getInitialisedStateNodes(List<StateNode> list) {
		list.add(m_parameter.get());
	}

}
