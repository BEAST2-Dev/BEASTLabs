
/*
 * File BeerLikelihoodCore.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST2.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

/** likelihood core that uses a cache mechanism so that only local
 * patterns for leaves need to be calculated.
 * Takes GORED trees in account
 * Switches off cache mechanism if it does not help.
 */


package beastlabs.evolution.likelihood;

public class BeerLikelihoodCoreNative extends ExperimentalLikelihoodCore {
	int m_nPatterns;
	int m_nStates;
	int m_nNodes;
	double SCALE = 1;
	

    long m_pBEER = 0;

    native long createCppBEERObject(int nStateCount);

    public BeerLikelihoodCoreNative(int nStateCount) {
        System.loadLibrary("BEER");
    	m_nStates = nStateCount;
        m_pBEER = createCppBEERObject(nStateCount);
    } // c'tor

	@Override
	public boolean initialize(int nNodeCount, int nPatternCount, int nMatrixCount, boolean bIntegrateCategories, boolean bUseAmbiguities) {
		m_nPatterns = nPatternCount;
		m_nNodes = nNodeCount;
		return initializeC(m_pBEER, nNodeCount, nPatternCount, nMatrixCount, bIntegrateCategories);
	}
	native boolean initializeC(long pBeer, int nNodeCount, int nPatternCount, int nMatrixCount, boolean bIntegrateCategories);

	@Override
	public void finalize() throws Throwable {
		finalizeC(m_pBEER);
	}
	native void finalizeC(long pBeer);

	@Override
	public void createNodePartials(int iNode) {
		createNodePartialsC(m_pBEER, iNode);
	}
	native void createNodePartialsC(long pBeer, int iNode);

	@Override
	public void setNodePartialsForUpdate(int iNode) {
		setNodePartialsForUpdateC(m_pBEER, iNode);
	}
	native void setNodePartialsForUpdateC(long pBeer, int iNode);

	@Override
    public void setNodeStatesForUpdate(int iNode) {
		setNodeStatesForUpdateC(m_pBEER, iNode);
    }
	native void setNodeStatesForUpdateC(long m_pBEER, int iNode);
	
	@Override
	public void setNodeStates(int iNode, int[] iStates) {
		setNodeStatesC(m_pBEER, iNode, iStates);
	}
	native void setNodeStatesC(long pBeer, int iNode, int[] iStates);

	@Override
	public void setNodeMatrixForUpdate(int iNode) {
		setNodeMatrixForUpdateC(m_pBEER, iNode);
	}
	native void setNodeMatrixForUpdateC(long pBeer, int iNode);

	@Override
	public void setNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix) {
		setNodeMatrixC(m_pBEER, iNode, iMatrixIndex, fMatrix);
	}
	native void setNodeMatrixC(long pBeer, int iNode, int iMatrixIndex, double[] fMatrix);

//	@Override
//	public void setPaddedNodeMatrices(int iNode, double[] fMatrix) {
//		setPaddedMatricesC(m_pBEER, iNode, fMatrix);
//	}
//	native void setPaddedMatricesC(long pBeer, int iNode, double[] fMatrix);
	
	
	@Override
	public void setUseScaling(double fScale) {
		m_bUseScaling = (fScale != 1.0);
		SCALE = fScale;
		setUseScalingC(m_pBEER, fScale);
	}
	native void setUseScalingC(long pBeer, double fScale);
	
	@Override
	public void calculatePartials(int iNode1, int iNode2, int iNode3) {
		calculatePartialsC(m_pBEER, iNode1, iNode2, iNode3);
	}
	native void calculatePartialsC(long pBeer, int iNode1, int iNode2, int iNode3);

	public void integratePartials(int iNode, double[] fProportions, double[] fOutPartials) {
		integratePartialsC(m_pBEER, iNode, fProportions, fOutPartials);
	}
	native void integratePartialsC(long pBeer, int iNode, double[] fProportions, double[] fOutPartials);
	
	public void calculateLogLikelihoods(double[] fPartials,	double[] fFrequencies, double[] fOutLogLikelihoods) {
        int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {

            double sum = 0.0;
			for (int i = 0; i < m_nStates; i++) {
				sum += fFrequencies[i] * fPartials[v];
				v++;
			}
            fOutLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
		}
	}
    public double getLogScalingFactor(int iPattern) {
    	if (m_bUseScaling) {
    		return -(m_nNodes/2) * Math.log(SCALE);
    	} else {
    		return 0;
    	}
    }
    
	@Override
	public void store() {
		storeC(m_pBEER);
	}
	native void storeC(long pBeer);

	@Override
	public void unstore() {
		unstoreC(m_pBEER);
	}
	native void unstoreC(long pBeer);

	@Override
	public void restore() {
		restoreC(m_pBEER);
	}
	native void restoreC(long pBeer);


	@Override
	public void setNodePartials(int iNode, double[] fPartials) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double calcLogP(int iNode, double[] fProportions, double[] fFrequencies) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPatternWeights(int[] nPatterWeights) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProportionInvariant(double fProportianInvariant, int[] iConstantPatterns) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getPatternLogLikelihoods(double[] fPatternLogLikelihoods) {
		// TODO Auto-generated method stub
		
	}


} // class BeerLikelihoodCoreMixed
