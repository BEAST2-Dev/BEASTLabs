package beast.evolution.operators;

import beast.core.Input;
import beast.core.Operator;
import beast.core.Input.Validate;
import beast.core.OperatorSchedule;
import beast.core.parameter.IntegerParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;
import beast.util.Randomizer;

public class TreeWithMetaDataRandomWalker extends Operator {
	public Input<Operator> treeoperatorInput = new Input<Operator>("treeoperator","tree operator that changes the tree. " +
			"All changed nodes will have their metadata scaled", Validate.REQUIRED);
	public Input<IntegerParameter> parameterInput = new Input<IntegerParameter>("intparameter", "parameter representing metadata associated with " +
			"the tree that the treeoperator applies to using the indices of the nodes in the tree", Validate.REQUIRED);
    public Input<Integer> windowSizeInput =
            new Input<Integer>("windowSize", "the size of the window both up and down", Validate.REQUIRED);
	
	TreeInterface tree;
	IntegerParameter parameter;
	Operator treeoperator;
	int windowSize;
	
	@Override
	public void initAndValidate() throws Exception {
		treeoperator = treeoperatorInput.get();
		treeoperator.setOperatorSchedule(new OperatorSchedule());
		tree = (TreeInterface) treeoperator.getInput("tree").get();
		parameter = parameterInput.get();
        windowSize = windowSizeInput.get();
	}

	@Override
	public double proposal() {
		double logHastingsRatio = treeoperator.proposal();
		Node [] nodes = tree.getNodesAsArray();
		for (int k = 0; k < nodes.length; k++) {
			if (nodes[k].isDirty() != Tree.IS_CLEAN && !nodes[k].isRoot()) {
		        final int value = parameter.getValue(k);
		        final int newValue = value + Randomizer.nextInt(2 * windowSize + 1) - windowSize;

		        if (newValue < parameter.getLower() || newValue > parameter.getUpper()) {
		            // invalid move, can be rejected immediately
		            return Double.NEGATIVE_INFINITY;
		        }
		        if (newValue == value) {
		            // this saves calculating the posterior
		            return Double.NEGATIVE_INFINITY;
		        }

		        parameter.setValue(k, newValue);
			}
		}
		return logHastingsRatio;
	}
	
	@Override
	public void accept() {
		treeoperator.accept();
		super.accept();
	}

	@Override
	public void reject() {
		treeoperator.reject();
		super.reject();
	}

    @Override
    public void optimize(final double logAlpha) {
        treeoperator.optimize(logAlpha);
    }

}
