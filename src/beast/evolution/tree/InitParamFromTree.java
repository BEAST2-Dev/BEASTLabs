package beast.evolution.tree;


import java.util.ArrayList;
import java.util.List;
import beast.core.Description;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;

@Description("Sets values of a parameter from metadata values associated with a newick tree")
public class InitParamFromTree extends beast.core.Plugin implements StateNodeInitialiser {
	public Input<Tree> m_tree = new Input<Tree>("tree", "tree containing some meta data", Validate.REQUIRED);
	public Input<RealParameter> m_parameter = new Input<RealParameter>("initial","parameter to be initialised", Validate.REQUIRED);
	public Input<String> m_sPattern = new Input<String>("pattern","name of the metadata item to be parsed", Validate.REQUIRED);
	
	@Override
	public void initAndValidate() throws Exception {
		initStateNodes();
	}
	
	@Override
	public void initStateNodes() {
		Node root = m_tree.get().getRoot();
		Double [] fValues = new Double[m_tree.get().getNodeCount()];
		String sPattern = m_sPattern.get();
		traverse(root, fValues, sPattern);
		RealParameter p;
		try {
			p = new RealParameter(fValues);
			m_parameter.get().assignFrom(p);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** traverse tree and pick up meta data values on the way **/
	private void traverse(Node node, Double[] fValues, String sPattern) {
		if (node.m_sMetaData != null) {
            String[] sMetaData = node.m_sMetaData.split(",");
            for (int i = 0; i < sMetaData.length; i++) {
                try {
                    String[] sStrs = sMetaData[i].split("=");
                    if (sStrs.length != 2) {
                        throw new Exception("misformed meta data '" + node.m_sMetaData + "'. Expected name='value' pairs");
                    }
                    if (sStrs[0].equals(sPattern)) {
                    sStrs[1] = sStrs[1].replaceAll("[\"']", "");
            		fValues[node.getNr()] = Double.parseDouble(sStrs[1]);;
                    }
                } catch (Exception e) {
                    System.out.println("Warning 1333: Attempt to parse metadata failed: " + node.m_sMetaData);
                }
            }
        }		
		if (!node.isLeaf()) {
			traverse(node.m_left, fValues, sPattern);
			traverse(node.m_right, fValues, sPattern);
		}
	}

	@Override
	public List<StateNode> getInitialisedStateNodes() {
		List<StateNode> list = new ArrayList<StateNode>();
		list.add(m_parameter.get());
		return list;
	}

}