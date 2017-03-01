package beast.evolution.tree;

/**
 * @author Alexei Drummond
 */
public interface TreeMetric {

    /**
     * Distance between two trees that may have different taxa sets but must have an overlap of taxa.
     * @param tree1
     * @param tree2
     * @return
     */
    public double distance(TreeInterface tree1, TreeInterface tree2);
}
