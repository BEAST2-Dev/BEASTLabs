package beastlabs.evolution.likelihood;

import java.util.Arrays;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.evolution.branchratemodel.StrictClockModel;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.likelihood.BeerLikelihoodCore;
import beast.base.evolution.likelihood.BeerLikelihoodCore4;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.Tree;



@Description("Performs peeling algorithm over a tree using a trait as values for " +
		"tips instead of a sequence")
public class TraitedTreeLikelihood extends TreeLikelihood {
	public Input<DataType.Base> m_dataTypeInput = new Input<DataType.Base>("dataType", "data type of the trait", Validate.REQUIRED);
	public Input<TraitSet> m_traitSet = new Input<TraitSet>("traitSet", "set of traits associated with tips", Validate.REQUIRED);
	
	int m_nPatterns;
	
	public TraitedTreeLikelihood() {
		dataInput.setRule(Validate.OPTIONAL);
	}
	
	@Override
	public void initAndValidate() {
    	// sanity check: alignment should have same #taxa as tree
//    	if (m_traitSet.get().getNrTaxa() != m_tree.get().getLeafNodeCount()) {
//    		throw new Exception("The number of nodes in the tree does not match the number of sequences");
//    	}
    	beagle = null;
    	
        int nodeCount = treeInput.get().getNodeCount();
        if (!(siteModelInput.get() instanceof SiteModel.Base)) {
        	throw new IllegalArgumentException ("siteModel input should be of type SiteModel.Base");
        }
        m_siteModel = (SiteModel.Base) siteModelInput.get();


        int nStateCount = m_dataTypeInput.get().getStateCount();
        
        m_siteModel.setDataType(m_dataTypeInput.get());
        substitutionModel = m_siteModel.substModelInput.get();

        if (branchRateModelInput.get() != null) {
        	branchRateModel = branchRateModelInput.get();
        } else {
            branchRateModel = new StrictClockModel();
        }
    	m_branchLengths = new double[nodeCount];
    	storedBranchLengths = new double[nodeCount];
    	
        m_nPatterns = 1;
        if (nStateCount == 4) {
            likelihoodCore = new BeerLikelihoodCore4();
        } else {
            likelihoodCore = new BeerLikelihoodCore(nStateCount);
        }
        Log.info.println("TreeLikelihood uses " + likelihoodCore.getClass().getName());

        proportionInvariant = m_siteModel.getProportionInvariant();
        m_siteModel.setPropInvariantIsCategory(false);
        if (proportionInvariant > 0) {
        	calcConstantPatternIndices(m_nPatterns, nStateCount);
        }
        

        // initialise core
        likelihoodCore.initialize(
                nodeCount,
                m_nPatterns,
                m_siteModel.getCategoryCount(),
                true, m_useAmbiguities.get()
        );

        int extNodeCount = nodeCount / 2 + 1;
        int intNodeCount = nodeCount / 2;

        if (m_useAmbiguities.get()) {
        	setTraitPartials(treeInput.get().getRoot(), m_nPatterns);
        } else {
        	setTraitStates(treeInput.get().getRoot(), m_nPatterns);
        }
        hasDirt = Tree.IS_FILTHY;
        for (int i = 0; i < intNodeCount; i++) {
            likelihoodCore.createNodePartials(extNodeCount + i);
        }
        
        
        patternLogLikelihoods = new double[m_nPatterns];
        m_fRootPartials = new double[m_nPatterns * nStateCount];
        matrixSize = (nStateCount +1)* (nStateCount+1);
        probabilities = new double[(nStateCount +1)* (nStateCount+1)];
        Arrays.fill(probabilities, 1.0);

        useAscertainedSitePatterns = false;
	}
	
    /** set leaf states in likelihood core **/
    void setTraitStates(Node node, int patternCount) {
        if (node.isLeaf()) {
            int[] states = new int[patternCount];
    		DataType dataType = m_dataTypeInput.get();
        	String sValue = m_traitSet.get().getStringValue(node.getNr());
        	if (sValue == null) {
        		throw new IllegalArgumentException("Trait not specified for " + node.getID());
        	}
        	List<Integer> iStates = dataType.string2state(sValue);
        	for (int iPattern = 0; iPattern < patternCount; iPattern++) {
            	int nState = iStates.get(iPattern);
        		if (!dataType.isAmbiguousState(nState)) {
                	int [] stateSet = dataType.getStatesForCode(nState);
               		states[iPattern] = stateSet[0];
        		} else {
        			states[iPattern] = dataType.getStateCount();
        		}
            }
            likelihoodCore.setNodeStates(node.getNr(), states);

        } else {
        	setTraitStates(node.getLeft(), patternCount);
            setTraitStates(node.getRight(), patternCount);
        }
    }
	
	
    void setTraitPartials(Node node, int patternCount) {
        if (node.isLeaf()) {
    		DataType dataType = m_dataTypeInput.get();
        	int nStates = dataType.getStateCount();
            double[] partials = new double[patternCount * nStates];

        	String sValue = m_traitSet.get().getStringValue(node.getNr());
        	if (sValue == null) {
        		throw new IllegalArgumentException("Trait not specified for " + node.getID());
        	}
        	List<Integer> iStates = dataType.string2state(sValue);

        	int k = 0;
            for (int iPattern = 0; iPattern < patternCount; iPattern++) {
            	int nState = iStates.get(iPattern);
            	boolean [] stateSet = dataType.getStateSet(nState);
        		for (int iState = 0; iState < nStates; iState++) {
        			partials[k++] = (stateSet[iState] ? 1.0 : 0.0);
            	}
            }
            likelihoodCore.setNodePartials(node.getNr(), partials);

        } else {
        	setTraitPartials(node.getLeft(), patternCount);
        	setTraitPartials(node.getRight(), patternCount);
        }
    }
	
    @Override
	protected void calcLogP() {
        logP = 0.0;
        for (int i = 0; i < m_nPatterns; i++) {
            logP += patternLogLikelihoods[i];
        }
    }

    @Override
    protected boolean requiresRecalculation() {
        hasDirt = Tree.IS_CLEAN;

        if (m_siteModel.isDirtyCalculation()) {
            hasDirt = Tree.IS_DIRTY;
            return true;
        }
        if (branchRateModel != null && branchRateModel.isDirtyCalculation()) {
            hasDirt = Tree.IS_DIRTY;
            return true;
        }
        return treeInput.get().somethingIsDirty();
    }
}
