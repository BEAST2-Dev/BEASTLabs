package beast.evolution.tree;


import java.io.PrintStream;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;


@Description("Logger to report statistics of a tree")
public class TreeDistanceLogger extends CalculationNode implements Loggable, Function {
	
    final public Input<Tree> treeInput = new Input<>("tree", "Tree to report height for.", Validate.REQUIRED);
    final public Input<TreeMetric> metricInput = new Input<>("metric", "Tree distance metric (default: Robinson Foulds).");
    final public Input<Tree> referenceInput = new Input<>("ref", "Reference tree to calculate distances from (default: the initial tree).");
    
   // final public Input<Alignment> alignmentInput = new Input<>("alignment", "Alignment for computing initial tree (only required if reference tree is not provided)");
    
    //new RobinsonsFouldMetric

    TreeMetric metric;
    Tree referenceTree;
    
    

    @Override
    public void initAndValidate() {
    	
    	
    	// Distance metric
    	if (metricInput.get() == null) {
    		this.metric = new RobinsonsFouldMetric(treeInput.get().getTaxonset());
    	}else {
    		this.metric = metricInput.get();
    	}
    	
    	
    	// Reference tree
    	if (referenceInput.get() == null) {
    		//if (alignmentInput.get() == null) throw new IllegalArgumentException("Please provide either a reference tree or an alignment");
    		Node root = treeInput.get().getRoot().copy();
    		this.referenceTree = new Tree(root);
    		//this.referenceTree.initByName("initial", this.treeInput.get(), 
    		//							"clusterType", "neighborjoining",
    		//							"taxa", alignmentInput.get());
    	}else {
    		this.referenceTree = referenceInput.get();
    	}
        
    	
    }

    @Override
    public void init(PrintStream out) {
        out.print(this.getID() + ".treeDistance\t");
    }

    @Override
    public void log(long sample, PrintStream out) {
        final Tree tree = treeInput.get();
        out.print(getDistance(tree) + "\t");
    }

    /**
     * Return distances between 'tree' and the reference tree
     * @param tree
     * @return
     */
    public double getDistance(Tree tree) {
    	return this.metric.distance(this.referenceTree, tree);
	}

	@Override
    public void close(PrintStream out) {
        // nothing to do
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public double getArrayValue() {
        return this.getDistance(treeInput.get());
    }

    @Override
    public double getArrayValue(int dim) {
    	return this.getArrayValue();
    }
}
