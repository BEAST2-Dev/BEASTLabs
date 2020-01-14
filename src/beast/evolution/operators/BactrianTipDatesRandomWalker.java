package beast.evolution.operators;

import beast.core.Input;
import beast.evolution.tree.Node;
import beast.util.Randomizer;

public class BactrianTipDatesRandomWalker extends TipDatesRandomWalker {
    final public Input<Double> windowSizeInput = new Input<>("m", "standard deviation for Bactrian distribution. "
    		+ "Larger values give more peaked distributions. "
    		+ "The default 0.95 is claimed to be a good choice (Yang 2014, book p.224).", 0.95);

    private double m;

    @Override
    public void initAndValidate() {
        m = windowSizeInput.get();
        if (m <=0 || m >= 1) {
        	throw new IllegalArgumentException("m should be withing the (0,1) range");
        }

    	super.initAndValidate();
    }
	
    public double proposal() {
        // randomly select leaf node
        int i = Randomizer.nextInt(taxonIndices.length);
        Node node = treeInput.get().getNode(taxonIndices[i]);

        double value = node.getHeight();
        double newValue = value + BactrianHelper.getRandomDelta(m, windowSize);

        if (newValue > node.getParent().getHeight()) { // || newValue < 0.0) {
            if (reflectValue) {
                newValue = reflectValue(newValue, 0.0, node.getParent().getHeight());
            } else {
                return Double.NEGATIVE_INFINITY;
            }
        }
        if (newValue == value) {
            // this saves calculating the posterior
            return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newValue);

        return 0.0;
    }
}
