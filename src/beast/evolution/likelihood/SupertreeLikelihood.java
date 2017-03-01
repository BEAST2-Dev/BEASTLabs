package beast.evolution.likelihood;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;
import beast.evolution.tree.TreeMetric;

import java.util.List;
import java.util.Random;

/**
 * @author Alexei Drummond
 */
@Description("A supertree likelihood for a set of subtrees.")
public class SupertreeLikelihood extends Distribution {


    final public Input<List<Tree>> dataInput = new Input<>("data", "sub trees", Input.Validate.REQUIRED);

    final public Input<TreeInterface> treeInput = new Input<>("tree", "the super tree to be estimated.", Input.Validate.REQUIRED);

    final public Input<Double> betaInput = new Input<>("beta", "the beta function defining the rate of probability fall off with tree distance.", Input.Validate.REQUIRED);

    final public Input<TreeMetric> treeMetricInput = new Input<>("treeMetric", "the tree metric to use for the tree distances in the likelihood function.", Input.Validate.REQUIRED);


    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(State state, Random random) {
    }

    public double calculateLogP() {

        List<Tree> subtrees = dataInput.get();
        double beta = betaInput.get();
        TreeMetric treeMetric = treeMetricInput.get();
        TreeInterface tree = treeInput.get();


        double logP = 0;

        for (Tree subtree : subtrees) {

            logP += -beta * treeMetric.distance(subtree, tree);
        }

        return logP;
    }
}
