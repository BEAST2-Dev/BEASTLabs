package beast.evolution.operators;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.util.Randomizer;

@Description("Operator for scaling only the root of a tree")
public class RootHeightScaleOperator extends beast.base.evolution.operator.TreeOperator {
    public Input<Double> m_pScaleFactor = new Input<Double>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);

    double m_fScaleFactor;
    
	@Override
	public void initAndValidate() {
        m_fScaleFactor = m_pScaleFactor.get();
    }
	
	@Override
	public double proposal() {
        final double scale = m_fScaleFactor + (Randomizer.nextDouble() * ((1.0 / m_fScaleFactor) - m_fScaleFactor));
        double hastingsRatio = -Math.log(scale);
        Node root = treeInput.get().getRoot();
        double fNewHeight = root.getHeight() * scale;
        if (fNewHeight < Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
        	return Double.NEGATIVE_INFINITY;
        }
        root.setHeight(fNewHeight);
        return hastingsRatio;
	}

    @Override
    public void optimize(double logAlpha) {
        double fDelta = calcDelta(logAlpha);
        fDelta += Math.log(1.0 / m_fScaleFactor - 1.0);
        m_fScaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
    }
}
