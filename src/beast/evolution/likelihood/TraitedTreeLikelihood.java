package beast.evolution.likelihood;

import java.util.Arrays;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.branchratemodel.StrictClockModel;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.Node;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;

@Description("Performs peeling algorithm over a tree using a trait as values for " +
		"tips instead of a sequence")
public class TraitedTreeLikelihood extends TreeLikelihood {
	public Input<DataType.Base> m_dataTypeInput = new Input<DataType.Base>("dataType", "data type of the trait", Validate.REQUIRED);
	public Input<TraitSet> m_traitSet = new Input<TraitSet>("traitSet", "set of traits associated with tips", Validate.REQUIRED);
	
	int m_nPatterns;
	
	public TraitedTreeLikelihood() {
		m_data.setRule(Validate.OPTIONAL);
	}
	
	@Override
	public void initAndValidate() throws Exception {
    	// sanity check: alignment should have same #taxa as tree
//    	if (m_traitSet.get().getNrTaxa() != m_tree.get().getLeafNodeCount()) {
//    		throw new Exception("The number of nodes in the tree does not match the number of sequences");
//    	}
    	m_beagle = null;
    	
        int nodeCount = m_tree.get().getNodeCount();
        m_siteModel = m_pSiteModel.get();


        int nStateCount = m_dataTypeInput.get().getStateCount();
        
        m_siteModel.setDataType(m_dataTypeInput.get());
        m_substitutionModel = m_siteModel.m_pSubstModel.get();

        if (m_pBranchRateModel.get() != null) {
        	m_branchRateModel = m_pBranchRateModel.get();
        } else {
            m_branchRateModel = new StrictClockModel();
        }
    	m_branchLengths = new double[nodeCount];
    	m_StoredBranchLengths = new double[nodeCount];
    	
        m_nPatterns = 1;
        if (nStateCount == 4) {
            m_likelihoodCore = new BeerLikelihoodCore4();
        } else {
            m_likelihoodCore = new BeerLikelihoodCore(nStateCount);
        }
        System.out.println("TreeLikelihood uses " + m_likelihoodCore.getClass().getName());

        m_fProportionInvariant = m_siteModel.getProportianInvariant();
        m_siteModel.setPropInvariantIsCategory(false);
        if (m_fProportionInvariant > 0) {
        	calcConstantPatternIndices(m_nPatterns, nStateCount);
        }
        

        // initialise core
        m_likelihoodCore.initialize(
                nodeCount,
                m_nPatterns,
                m_siteModel.getCategoryCount(),
                true, m_useAmbiguities.get()
        );

        int extNodeCount = nodeCount / 2 + 1;
        int intNodeCount = nodeCount / 2;

        if (m_useAmbiguities.get()) {
        	setTraitPartials(m_tree.get().getRoot(), m_nPatterns);
        } else {
        	setTraitStates(m_tree.get().getRoot(), m_nPatterns);
        }
        m_nHasDirt = Tree.IS_FILTHY;
        for (int i = 0; i < intNodeCount; i++) {
            m_likelihoodCore.createNodePartials(extNodeCount + i);
        }
        
        
        m_fPatternLogLikelihoods = new double[m_nPatterns];
        m_fRootPartials = new double[m_nPatterns * nStateCount];
        m_nMatrixSize = (nStateCount +1)* (nStateCount+1);
        m_fProbabilities = new double[(nStateCount +1)* (nStateCount+1)];
        Arrays.fill(m_fProbabilities, 1.0);

        m_bAscertainedSitePatterns = false;
	}
	
    /** set leaf states in likelihood core **/
    void setTraitStates(Node node, int patternCount) throws Exception {
        if (node.isLeaf()) {
            int[] states = new int[patternCount];
    		DataType dataType = m_dataTypeInput.get();
        	String sValue = m_traitSet.get().getStringValue(node.getNr());
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
            m_likelihoodCore.setNodeStates(node.getNr(), states);

        } else {
        	setTraitStates(node.m_left, patternCount);
            setTraitStates(node.m_right, patternCount);
        }
    }
	
	
    void setTraitPartials(Node node, int patternCount) throws Exception {
        if (node.isLeaf()) {
    		DataType dataType = m_dataTypeInput.get();
        	int nStates = dataType.getStateCount();
            double[] partials = new double[patternCount * nStates];

        	String sValue = m_traitSet.get().getStringValue(node.getNr());
        	List<Integer> iStates = dataType.string2state(sValue);

        	int k = 0;
            for (int iPattern = 0; iPattern < patternCount; iPattern++) {
            	int nState = iStates.get(iPattern);
            	boolean [] stateSet = dataType.getStateSet(nState);
        		for (int iState = 0; iState < nStates; iState++) {
        			partials[k++] = (stateSet[iState] ? 1.0 : 0.0);
            	}
            }
            m_likelihoodCore.setNodePartials(node.getNr(), partials);

        } else {
        	setTraitPartials(node.m_left, patternCount);
        	setTraitPartials(node.m_right, patternCount);
        }
    }
	
    @Override
    void calcLogP() throws Exception {
        logP = 0.0;
        for (int i = 0; i < m_nPatterns; i++) {
            logP += m_fPatternLogLikelihoods[i];
        }
    }

    @Override
    protected boolean requiresRecalculation() {
        m_nHasDirt = Tree.IS_CLEAN;

        if (m_siteModel.isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_DIRTY;
            return true;
        }
        if (m_branchRateModel != null && m_branchRateModel.isDirtyCalculation()) {
            m_nHasDirt = Tree.IS_DIRTY;
            return true;
        }
        return m_tree.get().somethingIsDirty();
    }
}
