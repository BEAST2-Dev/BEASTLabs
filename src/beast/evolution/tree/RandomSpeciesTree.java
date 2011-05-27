package beast.evolution.tree;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;

@Description("Initialises a species tree, consisting of a tree and a parameter indicating "
		+ "the height of the root branch")
public class RandomSpeciesTree extends RandomTree {
	public Input<RealParameter> m_rootBranchHeight = new Input<RealParameter>("rootBranchHeight",
			"parameter indicating height of the root branch", Validate.REQUIRED);

	@Override
	public void initAndValidate() throws Exception {
		super.initAndValidate();
	}

	@Override
	public void initStateNodes() {
		super.initStateNodes();
		double fTreeHeight = m_initial.get().getRoot().getHeight();
		double fRootBranchHeight = fTreeHeight + Randomizer.nextDouble() * fTreeHeight;
		try {
			m_rootBranchHeight.get().initByName("value", fRootBranchHeight + "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<StateNode> getInitialisedStateNodes() {
		List<StateNode> stateNodes = super.getInitialisedStateNodes();
		stateNodes.add(m_rootBranchHeight.get());
		return stateNodes;
	}
}
