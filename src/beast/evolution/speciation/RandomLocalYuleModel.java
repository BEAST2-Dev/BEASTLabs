package beast.evolution.speciation;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */

@Description("A speciation model of a Yule process whose rate of birth changes at different points in the tree.")
public class RandomLocalYuleModel extends UltrametricSpeciationModel {
    public Input<RealParameter> birthRatesParameter = new Input<RealParameter>("birthRates",
            "birth rates parameter, lambda in birth/death model", Validate.REQUIRED); //TODO lambda or lambda - mu
    public Input<BooleanParameter> indicatorsParameter = new Input<BooleanParameter>( "indicators",
            "the indicators associated with nodes in the tree for sampling of individual rate of birth changes among branches.",
            Validate.REQUIRED);
    public Input<RealParameter> meanRateParameter = new Input<RealParameter>("meanRate",
            "an optional parameter to set the mean rate of birth across the whole tree", Validate.REQUIRED);
    public Input<Boolean> ratesAreMultipliersInput =
            new Input<Boolean>("ratesAreMultipliers", "birth rates are multipliers (default false)", false);
//    public Input<Integer> mfDigitsInput = new Input<Integer>("mfDigits", "maximum fraction digits (default 4)", 4);
    public Input<Tree> treeInput =
            new Input<Tree>("tree", "the tree this random local yule model is associated with.", Input.Validate.REQUIRED);
    
    protected Tree tree;
    protected double[] birthRates;
    protected boolean ratesAreMultipliers;
    private boolean calculateAllBirthRates = false;
//    private NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
    
    @Override
    public void initAndValidate() throws Exception {
//        super.initAndValidate();
        tree = treeInput.get();
        this.birthRates = new double[tree.getNodeCount()];
        
        RealParameter rates = birthRatesParameter.get();
        if (rates.lowerValueInput.get() == null || rates.lowerValueInput.get() < 0.0) {
            rates.setLower(0.0);
        }
        if (rates.upperValueInput.get() == null || rates.upperValueInput.get() < 0.0) {
            rates.setUpper(Double.MAX_VALUE);
        }
        if (rates.getDimension() != tree.getNodeCount() - 1) {
            System.out.println("RandomLocalYuleModel::Setting dimension of birth rates to " + (tree.getNodeCount() - 1));
            rates.setDimension(tree.getNodeCount() - 1);
        }
        
        BooleanParameter indicators = indicatorsParameter.get();
        if (indicators.getDimension() != tree.getNodeCount() - 1) {
            System.out.println("RandomLocalYuleModel::Setting dimension of indicators to " + (tree.getNodeCount() - 1));
            indicators.setDimension(tree.getNodeCount() - 1);
        }
        
        this.ratesAreMultipliers = ratesAreMultipliersInput.get();
//        format.setMaximumFractionDigits(mfDigitsInput.get());
    }
   
    @Override
    public boolean includeExternalNodesInLikelihoodCalculation() {        
        return true;
    }
    
    @Override
    protected double logTreeProbability(int taxonCount) {
     // calculate all nodes birth rates
        calculateAllBirthRates = true;

        return 0.0;
    }

    @Override
    protected double logNodeProbability(Node node, int taxonCount) {
        
        if (calculateAllBirthRates) {
            RealParameter rates = birthRatesParameter.get();
            BooleanParameter indicators = indicatorsParameter.get();

            calculateBirthRates(tree.getRoot(), 0.0, indicators, rates);
            calculateAllBirthRates = false;
        }

        if (node.isRoot()) {
            return 0.0;
            
        } else {
            double lambda = birthRates[node.getNr()];
            double branchLength = node.getParent().getHeight() - node.getHeight();
            double logP = -lambda * branchLength;

            if (node.isLeaf()) logP += Math.log(lambda);

            return logP;
        }
    }      

    /**
     * This is a recursive function that does the work of
     * calculating the branch birth rates across the tree
     * taking into account the indicator variables.
     * @param node the node
     * @param rate the birth rate of the parent node
     * @param indicators indicators parameter
     * @param rates birth rates parameter
     */
    private void calculateBirthRates(Node node, double rate, BooleanParameter indicators, RealParameter rates) {
        
        int nodeNumber = node.getNr();

        if (node.isRoot()) {
            rate = meanRateParameter.get().getValue();
        } else {
            if (indicators.getValue(nodeNumber)) {
                if (ratesAreMultipliers) {
                    rate *= rates.getValue(nodeNumber);
                } else {
                    rate = rates.getValue(nodeNumber);
                }
            }
        }
        birthRates[nodeNumber] = rate;

        if (!node.isLeaf()) {
            calculateBirthRates(node.m_left, rate, indicators, rates);
            calculateBirthRates(node.m_right, rate, indicators, rates);
        }

    }

//    /**
//     * @param tree the tree
//     * @param node the node to retrieve the birth rate of
//     * @return the birth rate of the given node;
//     */
//    private double getBirthRate(TreeModel tree, NodeRef node) {
//
//        double birthRate;
//        if (!tree.isRoot(node)) {
//
//            double parentRate = getBirthRate(tree, tree.getParent(node));
//            if (isVariableSelected(tree, node)) {
//                birthRate = getVariable(tree, node);
//                if (birthRatesAreMultipliers) {
//                    birthRate *= parentRate;
//                } else {
//                    throw new RuntimeException("Rates must be multipliers in current implementation! " +
//                            "Otherwise root rate might be ignored");
//                }
//            } else {
//                birthRate = parentRate;
//            }
//        } else {
//            birthRate = meanRate.getParameterValue(0);
//        }
//        return birthRate;
//    }
}
