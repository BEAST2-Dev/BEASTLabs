package beast.evolution.tree;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.Gamma;

@Description("Species tree prior for *BEAST analysis")
public class SpeciesTreePrior extends Distribution {
	public Input<Tree> m_speciesTree = new Input<Tree>("speciesTree", "species tree containing the associated gene tree", Validate.REQUIRED);

	protected enum PopSizeFunction {constant, linear, linear_with_constant_root}
	public Input<PopSizeFunction> m_popFunctionInput = new Input<PopSizeFunction>("popFunction", "Population function. " +
	           "This can be " + Arrays.toString(PopSizeFunction.values()) + " (default 'constant')", PopSizeFunction.constant, PopSizeFunction.values());

	public Input<RealParameter> m_popSizesBottom = new Input<RealParameter>("bottomPopSize","population size parameter for populations at the bottom of a branch. " +
			"For linear population function, this is the same at the top of the branch.", Validate.REQUIRED);
	public Input<RealParameter> m_popSizesTop = new Input<RealParameter>("topPopSize","population size parameter at the top of a branch. " +
			"Ignored for constant population function, but required for linear population function.");
	
	public Input<RealParameter> m_gammaParameter = new Input<RealParameter>("gammaParameter","shape parameter of the gamma distribution", Validate.REQUIRED);

	public Input<RealParameter> m_rootHeightParameter = new Input<RealParameter>("rootBranchHeight","height of the node above the root, representing the root branch", Validate.REQUIRED);
	public Input<List<TaxonSet>> m_taxonSet = new Input<List<TaxonSet>>("taxonset", "set of taxa mapping lineages to species", new ArrayList<TaxonSet>(), Validate.REQUIRED);
	
	
	PopSizeFunction m_popFunction;
	RealParameter m_fPopSizesBottom;
	RealParameter m_fPopSizesTop;

	Gamma m_gamma2Prior;
	Gamma m_gamma4Prior;
	
	@Override 
	public void initAndValidate() throws Exception {
		m_popFunction = m_popFunctionInput.get();
		m_fPopSizesBottom = m_popSizesBottom.get();
		m_fPopSizesTop = m_popSizesTop.get();
		
		// set up sizes of population functions
		int nSpecies = m_speciesTree.get().getLeafNodeCount();
		int nNodes = m_speciesTree.get().getNodeCount();
		switch (m_popFunction) {
		case constant:
			m_fPopSizesBottom.setDimension(nNodes);
			break;
		case linear:
		case linear_with_constant_root:
			m_fPopSizesBottom.setDimension(nSpecies);
			m_fPopSizesTop.setDimension(nNodes);
			break;
		}

		// bottom prior = Gamma(2,Psi)
		m_gamma2Prior = new Gamma();
		m_gamma2Prior.m_beta.setValue(m_gammaParameter, m_gamma2Prior);

		// top prior = Gamma(4,Psi)
		m_gamma4Prior = new Gamma();
		RealParameter parameter = new RealParameter("4");
		m_gamma4Prior.m_alpha.setValue(parameter, m_gamma4Prior);
		m_gamma4Prior.m_beta.setValue(m_gammaParameter, m_gamma4Prior);
		
		if (m_popFunction != PopSizeFunction.constant && m_gamma4Prior == null) {
			throw new Exception("Top prior must be specified when population function is not constant");
		}
	}	
	
	@Override
	public double calculateLogP() {
		logP = 0;
		// make sure the root branch length is positive
		if (m_rootHeightParameter.get().getValue() < m_speciesTree.get().getRoot().getHeight()) {
			logP = Double.NEGATIVE_INFINITY;
			return logP;
		}
		
		Node [] speciesNodes = m_speciesTree.get().getNodesAsArray();
		try {
		switch (m_popFunction) 
		{
		case constant:
			// constant pop size function
			logP += m_gamma2Prior.calcLogP(m_fPopSizesBottom);
//			for (int i = 0; i < speciesNodes.length; i++) {
//				double fPopSize = m_fPopSizesBottom.getValue(i);
//				logP += m_bottomPrior.logDensity(fPopSize); 
//			}
			break;
		case linear:
			// linear pop size function
			logP += m_gamma4Prior.calcLogP(m_fPopSizesBottom);
			logP += m_gamma2Prior.calcLogP(m_fPopSizesTop);

//			for (int i = 0; i < speciesNodes.length; i++) {
//				Node node = speciesNodes[i];
//				double fPopSizeBottom;
//				if (node.isLeaf()) {
//					// Gamma(4, fPsi) prior 
//					fPopSizeBottom = m_fPopSizesBottom.getValue(i);
//					logP += m_gamma2Prior.logDensity(fPopSizeBottom); 
//				}				
//				double fPopSizeTop = m_fPopSizesTop.getValue(i);
//				logP += m_gamma4Prior.logDensity(fPopSizeTop); 
//			}
			break;
		case linear_with_constant_root:
			logP += m_gamma4Prior.calcLogP(m_fPopSizesBottom);
			logP += m_gamma2Prior.calcLogP(m_fPopSizesTop);
			int iRoot = m_speciesTree.get().getRoot().getNr();
			double fPopSize = m_fPopSizesTop.getValue(iRoot);
			logP -= m_gamma2Prior.logDensity(fPopSize); 
			
//			for (int i = 0; i < speciesNodes.length; i++) {
//				Node node = speciesNodes[i];
//				if (node.isLeaf()) {
//					double fPopSizeBottom = m_fPopSizesBottom.getValue(i);
//					logP += m_gamma2Prior.logDensity(fPopSizeBottom); 
//				}				
//				if (!node.isRoot()) {
//					double fPopSizeTop = m_fPopSizesTop.getValue(i);
//					logP += m_gamma4Prior.logDensity(fPopSizeTop); 
//				}
//			}
			break;
		}
		} catch (Exception e) {
			// exceptions can be thrown by the gamma priors
			e.printStackTrace();
			return Double.NEGATIVE_INFINITY; 
		}
		return logP;
	}
	
	
	
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
	@Override public void sample(State state, Random random) {}
}
