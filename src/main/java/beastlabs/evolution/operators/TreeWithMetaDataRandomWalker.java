package beastlabs.evolution.operators;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.core.Input.Validate;
import beast.base.inference.OperatorSchedule;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;
import beast.base.util.Randomizer;

@Description("Operator that uses a standard TreeOperator to change the topology of the tree, "
		+ "then changes metadata (such as rate categories for relaxed clock) associated with "
		+ "only those nodes in the tree that are changed due to the TreeOperator.")
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
	public void initAndValidate() {
		treeoperator = treeoperatorInput.get();
		treeoperator.setOperatorSchedule(new OperatorSchedule());
		parameter = parameterInput.get();
        windowSize = windowSizeInput.get();
        tree = (TreeInterface) treeoperator.getInput("tree").get();
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
    
    @Override
    public void setOperatorSchedule(OperatorSchedule operatorSchedule) {
    	super.setOperatorSchedule(operatorSchedule);
		treeoperator.setOperatorSchedule(operatorSchedule);
    }

}
