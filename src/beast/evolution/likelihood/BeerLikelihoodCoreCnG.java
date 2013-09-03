
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


package beast.evolution.likelihood;

import java.util.Arrays;

import beast.evolution.sitemodel.SiteModel;





public class BeerLikelihoodCoreCnG extends ExperimentalLikelihoodCore {
	/** various counts **/
	protected int m_nStates;
    protected int m_nNodes;
    protected int m_nPatterns;
    protected int m_nPartialsSize;
    protected int m_nMatrixSize;
    protected int m_nMatrices;

    /** flag to indicate whether to integrate over site categories (as defined by the SiteModel) */
    protected boolean m_bIntegrateCategories;

    protected double[][][] m_fPartials; // 2 x #nodes x (#patterns*#states*#matrices)

    protected int[][] m_iStates; // #nodes x #patterns

    protected double[][][] m_fMatrices; // 2 x #nodes x matrix size

    protected int[] m_iCurrentMatrices; // # nodes
    protected int[] m_iStoredMatrices;  // # nodes
    protected int[] m_iCurrentPartials; // # nodes
    protected int[] m_iStoredPartials;  // # nodes

    // used to store/restore state
	int [] m_iCurrentStates;
	int [] m_iStoredStates;

	/** one number to scale them all */
	double SCALE = 1.05;
//    protected double[][][] m_fScalingFactors; // 2 x #nodes x #patterns



    
    protected int[][] m_nNrOfID; // 2 x #nodes
    protected int[][][] m_nID; // 2 x #nodes x #patterns
    /** contains [0,1,2,...#pattenrs-1], used as default m_nID content **/
    int [] DEFAULT_ID; 
    /** memory for building up cache index **/
    int [] DEFAULT_MAP; 


	// stack related variables
	final static int OPERATION_SS = 0;
	final static int OPERATION_SP = 1;
	final static int OPERATION_PP = 2;
	int m_nTopOfStack = 0;
	int [] m_nOperation; // #nodes
	int [] m_nNode1;     // #nodes
	int [] m_nNode2;     // #nodes
	int [] m_nNode3;     // #nodes
	int [][][] m_nStates1; // 2 x #nodes x #patterns
	int [][][] m_nStates2; // 2 x #nodes x #patterns
	int [][] m_nStackStates1; // #nodes x * pointer to m_nStates, or DEFAULT_ID
	int [][] m_nStackStates2; // #nodes x * pointer to m_nStates, or DEFAULT_ID
	
	/** memory allocation for the root partials **/
    double[] m_fRootPartials;
    /** dealing with proportion of site being invariant **/
    int [] m_iConstantPattern = null;
    double m_fProportianInvariant = 0.0;
    
    /** memory allocation for likelihoods for each of the patterns **/
    double[] m_fPatternLogLikelihoods;      
    int [] m_nPatternWeights;
    
    public BeerLikelihoodCoreCnG(int nStateCount) {
		this.m_nStates = nStateCount;
	} // c'tor

	void calcAllMatrixSSP(int nNrOfID, int [] pStates1, int [] pStates2, double [] pfMatrices1, double [] pfMatrices2, double [] pfPartials3) {
		int v = 0;
		for (int i = 0; i < nNrOfID; i ++) {
			for (int l = 0; l < m_nMatrices; l++) {
				int w = l * m_nMatrixSize;
				v = m_nStates * (i*m_nMatrices + l);
				calcSSP(pStates1[i], pStates2[i], pfMatrices1, pfMatrices2, pfPartials3, w, v);
			}
		}
	}

	void calcSSP(int state1, int state2, double [] pfMatrices1, double [] pfMatrices2, double [] pfPartials3, int w, int v) {
		if (state1 < m_nStates) {
			if (state2 < m_nStates) {
				for (int i = 0; i < m_nStates; i++) {
					pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
					v++;
					w += m_nStates;
				}
			} else {
				for (int i = 0; i < m_nStates; i++) {
					pfPartials3[v] = pfMatrices1[w + state1];
					v++;
					w += m_nStates;
				}
			}
		} else {
			if (state2 < m_nStates) {
				for (int i = 0; i < m_nStates; i++) {
					pfPartials3[v] = pfMatrices2[w + state1];
					v++;
					w += m_nStates;
				}
			} else {
				for (int i = 0; i < m_nStates; i++) {
					pfPartials3[v] = 1;
					v++;
					//w += m_nStates+1;
				}
			}
				
		}
		//return v;
	}
	
	void calcAllMatrixSPP(int nNrOfID, int [] pStates1, int [] pStates2, double [] pfMatrices1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3) {
		int u = 0;
		for (int i = 0; i < nNrOfID; i ++) {
		for (int l = 0; l < m_nMatrices; l++) {
            int w = l * m_nMatrixSize;
			int v = (l  + pStates2[i] * m_nMatrices) * m_nStates;
				u = m_nStates * (i * m_nMatrices + l);
				calcSPP(pStates1[i], pfMatrices1, pfMatrices2, pfPartials2, pfPartials3, w, v, u);
			}
		}
	}

	void calcAllMatrixSPP2(int [] pStates1, double [] pfMatrices1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3) {
		int u = 0;
		int v = 0;
		for (int i = 0; i < m_nPatterns; i ++) {
			int w = 0;
			//v =  i*m_nMatrixCount * m_nStates;
			for (int l = 0; l < m_nMatrices; l++) {
				//int v = (l  + i * m_nMatrixCount) * m_nStates;
				//u = m_nStates * i * m_nMatrixCount;
				calcSPP(pStates1[i], pfMatrices1, pfMatrices2, pfPartials2, pfPartials3, w, v, u);
	            w += m_nMatrixSize;
	            v += m_nStates;
	            u += m_nStates;
			}
			//u += m_nStates * m_nMatrixCount;
		}
	}
	
	void calcSPP(int state1, double [] pfMatrices1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3, int w, int v, int u) {
		if (state1 < m_nStates) {
			double tmp, sum;
			for (int i = 0; i < m_nStates; i++) {
				tmp = pfMatrices1[w + state1];
				sum = 0.0;
				for (int j = 0; j < m_nStates; j++) {
					sum += pfMatrices2[w] * pfPartials2[v + j];
					w++;
				}
				//w++;
				pfPartials3[u] = tmp * sum;
				u++;
			}
		} else {
			double sum;
			for (int i = 0; i < m_nStates; i++) {
				sum = 0.0;
				for (int j = 0; j < m_nStates; j++) {
					sum += pfMatrices2[w] * pfPartials2[v + j];
					w++;
				}
				//w++;
				pfPartials3[u] = sum;
				u++;
			}
		}
		//return u;
			
	}

	void calcAllMatrixPPP(int nNrOfID, int [] pStates1, int [] pStates2, double [] pfMatrices1, double [] pfPartials1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3) {
		int u = 0;
		for (int i = 0; i < nNrOfID; i ++) {
			int w = 0;
			for (int l = 0; l < m_nMatrices; l++) {
				//int w = l * m_nMatrixSize;
				int v1 = (l + pStates1[i] * m_nMatrices) * m_nStates;
				int v2 = (l + pStates2[i] * m_nMatrices) * m_nStates;
				//u = m_nStates * i * m_nMatrixCount;
				calcPPP(pfMatrices1, pfPartials1, pfMatrices2, pfPartials2, pfPartials3, w, v1, v2, u);
				w += m_nMatrixSize;
				u += m_nStates;
			}
			//u += m_nStates * m_nMatrixCount;
		}
	}
	
	void calcAllMatrixPPP2(double [] pfMatrices1, double [] pfPartials1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3) {
		int u = 0;
		int v = 0;
		for (int i = 0; i < m_nPatterns; i++) {
			int w = 0;
			for (int l = 0; l < m_nMatrices; l++) {
				//int w = l * m_nMatrixSize;
				//int v = (l + i * m_nMatrixCount) * m_nStates;
				//int v2 = (l + i * m_nMatrixCount) * m_nStates;
				//u = m_nStates * i * m_nMatrixCount;
				calcPPP2(pfMatrices1, pfPartials1, pfMatrices2, pfPartials2, pfPartials3, w, v, u);
	            v += m_nStates;
				w += m_nMatrixSize;
				u += m_nStates;
			}
			//u += m_nStates * m_nMatrixCount;
		}
	}

	void calcPPP(double [] pfMatrices1, double [] pfPartials1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3, int w, int v1, int v2, int u) {
		double sum1, sum2;
		for (int i = 0; i < m_nStates; i++) {
			sum1=0;
			sum2=0;
			for (int j = 0; j < m_nStates; j++) {
				sum1 += pfMatrices1[w] * pfPartials1[v1 + j];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + j];
				w++;
			}
			//w++;
			pfPartials3[u] = sum1 * sum2;
			u++;
		}
		//return u;
	}
	
	void calcPPP2(double [] pfMatrices1, double [] pfPartials1, double [] pfMatrices2, double [] pfPartials2, double [] pfPartials3, int w, int v, int u) {
		double sum1, sum2;
		for (int i = 0; i < m_nStates; i++) {
			sum1=0;
			sum2=0;
			for (int j = 0; j < m_nStates; j++) {
				sum1 += pfMatrices1[w] * pfPartials1[v + j];
				sum2 += pfMatrices2[w] * pfPartials2[v + j];
				w++;
			}
			//w++;
			pfPartials3[u] = sum1 * sum2;
			u++;
		}
		//return u;
	}
	
    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNode1 the 'child 1' node
     * @param iNode2 the 'child 2' node
     * @param iNode3 the 'parent' node
     */
    public void calculatePartials(int iNode1, int iNode2, int iNode3) {
        if (m_iStates[iNode1] != null) {
            if (m_iStates[iNode2] != null) {
                calculateStatesStatesPruning(iNode1, iNode2, iNode3);
            } else {
                calculateStatesPartialsPruning(iNode1, iNode2, iNode3);
            }
        } else {
            if (m_iStates[iNode2] != null) {
                calculateStatesPartialsPruning(iNode2, iNode1, iNode3);
            } else {
                calculatePartialsPartialsPruning(iNode1, iNode2, iNode3);
            }
        }

    }

	int [] initIDMap(int iNode, int nMaxID1, int nMaxID2) {
		m_nNrOfID[m_iCurrentStates[iNode]][iNode] = 0;
		//int [] nIDMap = new int[nMaxID1*nMaxID2];
		int [] nIDMap = DEFAULT_MAP;
		Arrays.fill (nIDMap, 0, nMaxID1*nMaxID2, -1);
		return nIDMap; 
	} // initIDMap

	/**
	 * Calculates partial likelihoods at a node when both children have states.
	 */
	protected void calculateStatesStatesPruning(int iNode1, int iNode2, int iNode3) {
		int[] iStates1 = m_iStates[iNode1]; 
		int[] iStates2 = m_iStates[iNode2]; 
		int [] nID3 = m_nID[m_iCurrentStates[iNode3]][iNode3];

		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_SS;
		if (m_fPartials[m_iCurrentPartials[iNode1]][iNode1] != null && m_fPartials[m_iCurrentPartials[iNode2]][iNode2] != null) {
			// for handling ambiguities
			m_nOperation[m_nTopOfStack] = OPERATION_PP;
		}
		
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		int [] pStates1 = m_nStates1[m_iCurrentStates[iNode3]][iNode3];
		int [] pStates2 = m_nStates2[m_iCurrentStates[iNode3]][iNode3];
		m_nStackStates1[m_nTopOfStack] = pStates1;
		m_nStackStates2[m_nTopOfStack] = pStates2;
		m_nTopOfStack++;

		// recalc state indices if necessary
		if (m_nNrOfID[m_iCurrentStates[iNode3]][iNode3] == 0) {
			int [] nIDMap = initIDMap(iNode3, m_nStates+1, m_nStates+1);
			int nBase = m_nStates+1;
			int nNrOfID = 0;
			for (int k = 0; k < m_nPatterns; k++) {
				int state1 = iStates1[k];
				int state2 = iStates2[k];
				if (nIDMap[state1 + nBase * state2]<0) {
					pStates1[nNrOfID] = state1;
					pStates2[nNrOfID] = state2;
					nIDMap[state1 + nBase * state2] = nNrOfID++;
				}
				nID3[k] = nIDMap[state1 + nBase * state2];
			}
			m_nNrOfID[m_iCurrentStates[iNode3]][iNode3] = nNrOfID;
		}
	}

	/**
	 * Calculates partial likelihoods at a node when one child has states and one has partials.
	 */
	protected void calculateStatesPartialsPruning(int iNode1, int iNode2, int iNode3) {
		int [] iStates1 = m_iStates[iNode1]; 
		int [] nID2 = m_nID[m_iCurrentStates[iNode2]][iNode2];
		int [] nID3 = m_nID[m_iCurrentStates[iNode3]][iNode3];

		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_SP;
		if (m_fPartials[m_iCurrentPartials[iNode1]][iNode1] != null && m_fPartials[m_iCurrentPartials[iNode2]][iNode2] != null) {
			// for handling ambiguities
			m_nOperation[m_nTopOfStack] = OPERATION_PP;
		}
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		int [] pStates1 = m_nStates1[m_iCurrentStates[iNode3]][iNode3];
		int [] pStates2 = m_nStates2[m_iCurrentStates[iNode3]][iNode3];
		m_nStackStates1[m_nTopOfStack] = pStates1;
		m_nStackStates2[m_nTopOfStack] = pStates2;
		m_nTopOfStack++;

		// recalc state indices if necessary
		if (m_nNrOfID[m_iCurrentStates[iNode3]][iNode3] == 0) {
			
			if (m_nNrOfID[m_iCurrentStates[iNode2]][iNode2] == m_nPatterns) {
				System.arraycopy(iStates1, 0, pStates1, 0, m_nPatterns);
				System.arraycopy(nID2, 0, pStates2, 0, m_nPatterns);
				System.arraycopy(DEFAULT_ID, 0, nID3, 0, m_nPatterns);

				m_nStackStates2[m_nTopOfStack-1] = DEFAULT_ID;
				m_nNrOfID[m_iCurrentStates[iNode3]][iNode3] = m_nPatterns;
				return;
			}
			
			int [] nIDMap = initIDMap(iNode3, m_nStates+1, m_nNrOfID[m_iCurrentStates[iNode2]][iNode2]);
			int nBase = m_nStates+1;
			int nNrOfID = 0;
			for (int k = 0; k < m_nPatterns; k++) {
				int state1 = iStates1[k];
				int state2 = nID2[k];
				if (nIDMap[state1 + nBase * state2]<0) {
					pStates1[nNrOfID] = state1;
					pStates2[nNrOfID] = state2;
					nIDMap[state1 + nBase * state2] = nNrOfID++;
				}
				nID3[k] = nIDMap[state1 + nBase * state2];
			}
			m_nNrOfID[m_iCurrentStates[iNode3]][iNode3] = nNrOfID;
		}
	}

	
//	void initPartialsLeave(int iNode) {
//		System.err.println("RRB: this is really inefficient and code should be replaced!!");
//		m_nNrOfID[m_iCurrentStates[iNode]][iNode] = 0;
//		for (int k = 0; k < m_nPatterns; k++) {
//			m_nID[m_iCurrentStates[iNode]][iNode][k] = k;
//			m_nNrOfID[m_iCurrentStates[iNode]][iNode]++;
//		}
//	} // initPartialsLeave

	/**
	 * Calculates partial likelihoods at a node when both children have partials.
	 */
	protected void calculatePartialsPartialsPruning(int iNode1, int iNode2, int iNode3) {
		int i1 = m_iCurrentStates[iNode1]; 
		int i2 = m_iCurrentStates[iNode2]; 
		int i3 = m_iCurrentStates[iNode3]; 
		
		int [] nID1 = m_nID[i1][iNode1];
		int [] nID2 = m_nID[i2][iNode2];
		int [] nID3 = m_nID[i3][iNode3];

		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_PP;
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		int [] pStates1 = m_nStates1[i3][iNode3];
		int [] pStates2 = m_nStates2[i3][iNode3];
		m_nStackStates1[m_nTopOfStack] = pStates1;
		m_nStackStates2[m_nTopOfStack] = pStates2;
		m_nTopOfStack++;

		// recalc state indices if necessary
		if (m_nNrOfID[i3][iNode3] == 0) {

			if (m_nNrOfID[i1][iNode1] == m_nPatterns &&
					m_nNrOfID[i2][iNode2] == m_nPatterns) {
				System.arraycopy(nID1, 0, pStates1, 0, m_nNrOfID[i1][iNode1]);
				System.arraycopy(nID2, 0, pStates2, 0, m_nNrOfID[i2][iNode2]);
				System.arraycopy(DEFAULT_ID, 0, nID3, 0, m_nPatterns);

				if (m_nNrOfID[i1][iNode1] == m_nPatterns) {
					m_nStackStates1[m_nTopOfStack-1] = DEFAULT_ID;
				}
				if (m_nNrOfID[i2][iNode2] == m_nPatterns) {
					m_nStackStates2[m_nTopOfStack-1] = DEFAULT_ID;
				}
				m_nNrOfID[i3][iNode3] = m_nPatterns;
				return;
			}
			
			
//			if (m_nNrOfID[i1][iNode1] == 0) {
//				initPartialsLeave(iNode1);    
//
//			}
//			if (m_nNrOfID[i2][iNode2] == 0) {
//				initPartialsLeave(iNode2);
//			}
	
			int [] nIDMap = initIDMap(iNode3, m_nNrOfID[i1][iNode1], m_nNrOfID[i2][iNode2]);
			int nBase = m_nNrOfID[i1][iNode1];
			m_nNrOfID[i3][iNode3] = calcPPPInner(nID1, nID2, nID3, nBase, nIDMap, pStates1, pStates2, iNode3);
		}
	} // calculatePartialsPartialsPruning

	int calcPPPInner(int [] nID1, int [] nID2, int [] nID3, int nBase, int [] nIDMap, int [] pStates1, int [] pStates2, int iNode3) {
		int nNrOfID = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			int state1 = nID1[k];
			int state2 = nID2[k];
			if (nIDMap[state1 + nBase * state2]<0) {
				pStates1[nNrOfID] = state1;
				pStates2[nNrOfID] = state2;
				nIDMap[state1 + nBase * state2] = nNrOfID++; 
			}
			nID3[k] = nIDMap[state1 + nBase * state2];
		}
		return nNrOfID;
	}
	
	void processNodeFromStack(int iJob) {
		int iNode1 = m_nNode1[iJob];
		int iNode2 = m_nNode2[iJob];
		int iNode3 = m_nNode3[iJob];
		int [] pStates1 = m_nStackStates1[iJob];//m_nStates1[m_iCurrentStates[iNode3]][iNode3];
		int [] pStates2 = m_nStackStates2[iJob];//m_nStates2[m_iCurrentStates[iNode3]][iNode3];
		
		double [] pfMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
		double [] pfMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
		double [] pfPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];

		switch (m_nOperation[iJob]) {
		case OPERATION_SS:
			//v=0;
			calcAllMatrixSSP(m_nNrOfID[m_iCurrentStates[iNode3]][iNode3], pStates1, pStates2, pfMatrices1, pfMatrices2, pfPartials3);
			break;
		case OPERATION_SP:
		{
			double [] pfPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];  
			if (pStates2 != DEFAULT_ID) {
				calcAllMatrixSPP(m_nNrOfID[m_iCurrentStates[iNode3]][iNode3], pStates1, pStates2, pfMatrices1, pfMatrices2, pfPartials2, pfPartials3);
			} else {
				calcAllMatrixSPP2(pStates1, pfMatrices1, pfMatrices2, pfPartials2, pfPartials3);
			}
		}
			break;
		case OPERATION_PP:
		{
			double [] pfPartials1 = m_fPartials[m_iCurrentPartials[iNode1]][iNode1];
			double [] pfPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];
			if (pStates1 != DEFAULT_ID || pStates2 != DEFAULT_ID) {
				calcAllMatrixPPP(m_nNrOfID[m_iCurrentStates[iNode3]][iNode3], pStates1, pStates2, pfMatrices1, pfPartials1, pfMatrices2, pfPartials2, pfPartials3);
			} else {
				calcAllMatrixPPP2(pfMatrices1, pfPartials1, pfMatrices2, pfPartials2, pfPartials3);
			}
		}
			break;
		}
        if (m_bUseScaling) {
            scalePartials(iNode3);
        }
	} // processNodeFromStack
	
	
	
	
	/**
	 * Integrates partials across categories.
     * @param fInPartials the array of partials to be integrated
	 * @param fProportions the proportions of sites in each category
	 * @param fOutPartials an array into which the partials will go
	 */
	@Override
    public void processStack() {
    	for (int iJob = 0; iJob < m_nTopOfStack; iJob++) {
	  		processNodeFromStack(iJob);
	  	}

    	m_nTopOfStack = 0;
    }

    public void integratePartials(int iNode, double[] fProportions, double[] fOutPartials) {
    	processStack();
    	
		double[] fInPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
		int [] nID = m_nID[m_iCurrentStates[iNode]][iNode];
		int u = 0;
		int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			v = 0*m_nStates + nID[k]*m_nStates * m_nMatrices;
			for (int i = 0; i < m_nStates; i++) {
				fOutPartials[u] = fInPartials[v] * fProportions[0];
				u++;
				v++;
			}
		}


		for (int l = 1; l < m_nMatrices; l++) {
			u=0;
			for (int k = 0; k < m_nPatterns; k++) {
				v = l*m_nStates + nID[k]*m_nStates * m_nMatrices;
				for (int i = 0; i < m_nStates; i++) {
					fOutPartials[u] += fInPartials[v] * fProportions[l];
					u++;
					v++;
				}
			}
		}
	}

	/**
	 * Calculates pattern log likelihoods at a node.
	 * @param fPartials the partials used to calculate the likelihoods
	 * @param fFrequencies an array of state frequencies
	 * @param fOutLogLikelihoods an array into which the likelihoods will go
	 */
	public void calculateLogLikelihoods(double[] fPartials, double[] fFrequencies, double[] fOutLogLikelihoods)
	{
        int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {

            double sum = 0.0;
			for (int i = 0; i < m_nStates; i++) {
				sum += fFrequencies[i] * fPartials[v];
				v++;
			}
            fOutLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
		}
//		if (m_bUseScaling) {
//			double fSum = 0;
//			for (int k = 0; k < m_nPatterns; k++) {
//				fSum += fOutLogLikelihoods[k];
//			}
//			if (fSum - getLogScalingFactor(0) * m_nPatterns > 0) {
//				SCALE *= 0.99;
//			} else {
//				SCALE *= 1.01;
//			}
//		}
//		System.err.print(SCALE+" ");
	}



    /**
     * initializes partial likelihood arrays.
     *
     * @param nNodeCount           the number of nodes in the tree
     * @param nPatternCount        the number of patterns
     * @param nMatrixCount         the number of matrices (i.e., number of categories)
     * @param bIntegrateCategories whether sites are being integrated over all matrices
     */
    public boolean initialize(int nNodeCount, int nPatternCount, int nMatrixCount, boolean bIntegrateCategories, boolean bUseAmbiguities) {

        this.m_nNodes = nNodeCount;
        this.m_nPatterns = nPatternCount;
        this.m_nMatrices = nMatrixCount;

        this.m_bIntegrateCategories = bIntegrateCategories;

        if (bIntegrateCategories) {
            m_nPartialsSize = nPatternCount * m_nStates * nMatrixCount;
        } else {
            m_nPartialsSize = nPatternCount * m_nStates;
        }

        m_fPartials = new double[2][nNodeCount][];

        m_iCurrentMatrices = new int[nNodeCount];
        m_iStoredMatrices = new int[nNodeCount];

        m_iCurrentPartials = new int[nNodeCount];
        m_iStoredPartials = new int[nNodeCount];

        m_iCurrentStates = new int[nNodeCount];
        m_iStoredStates = new int[nNodeCount];

        m_iStates = new int[nNodeCount][];

        for (int i = 0; i < nNodeCount; i++) {
            m_fPartials[0][i] = null;
            m_fPartials[1][i] = null;

            m_iStates[i] = null;
        }

        m_nMatrixSize = (m_nStates+1) * (m_nStates+1);

        m_fMatrices = new double[2][nNodeCount][nMatrixCount * m_nMatrixSize];

        m_nNrOfID = new int[2][nNodeCount];
        m_nID = new int[2][nNodeCount][nPatternCount];
        //m_nIDMap =  new int[nNodeCount][][];
    	//m_pStates1 = new int[m_nPatterns];
    	//m_pStates2 = new int[m_nPatterns];
        
    	m_nTopOfStack = 0;
    	m_nOperation    = new int[nNodeCount]; // #nodes
    	m_nNode1        = new int[nNodeCount];// #nodes
    	m_nNode2        = new int[nNodeCount];// #nodes
    	m_nNode3        = new int[nNodeCount];// #nodes
    	m_nStates1      = new int[2][nNodeCount][nPatternCount];// #nodes x #patterns
    	m_nStates2      = new int[2][nNodeCount][nPatternCount];// #nodes x #patterns
    	m_nStackStates1 = new int[nNodeCount][];
    	m_nStackStates2 = new int[nNodeCount][];
//        if (m_nMatrixCount == 1) {
//        	m_innerLoopCalculator = new InnerLoopCalculatorSingleMatrix();
//        } else {
//        	m_innerLoopCalculator = new InnerLoopCalculator();
//        }
    	DEFAULT_ID = new int[m_nPatterns];
    	for (int k = 0; k < m_nPatterns; k++) {
    		DEFAULT_ID[k] = k;
    	}
    	DEFAULT_MAP = new int[m_nPatterns * m_nPatterns];
        
    	m_fRootPartials = new double[m_nPatterns * m_nStates];
        m_fPatternLogLikelihoods = new double[m_nPatterns];
        m_nPatternWeights  = new int[m_nPatterns];
        
        return true;
    }

    /**
     * cleans up and deallocates arrays.
     */
    public void finalize() throws java.lang.Throwable  {
        m_nNodes = 0;
        m_nPatterns = 0;
        m_nMatrices = 0;

        m_fPartials = null;
        m_iCurrentPartials = null;
        m_iStoredPartials = null;
        m_iStates = null;
        m_fMatrices = null;
        m_iCurrentMatrices = null;
        m_iStoredMatrices = null;

//        m_fScalingFactors = null;
    }

//    public boolean getUseScaling() {return m_bUseScaling;}
//    
//    public void setUseScaling(boolean bUseScaling) {
//        m_bUseScaling = bUseScaling;
////        if (bUseScaling) {
////            m_fScalingFactors = new double[2][m_nNodes][m_nPatterns];
////        }
//    }

    /**
     * Allocates partials for a node
     */
    public void createNodePartials(int iNode) {
        this.m_fPartials[0][iNode] = new double[m_nPartialsSize];
        this.m_fPartials[1][iNode] = new double[m_nPartialsSize];
    }

    /**
     * Sets partials for a node
     */
    public void setNodePartials(int iNode, double[] fPartials) {
    	m_nNrOfID[m_iCurrentStates[iNode]][iNode] = 0;
    	
        if (this.m_fPartials[0][iNode] == null) {
            createNodePartials(iNode);
        }
        //if (fPartials.length < m_nPartialsSize) {
            for (int k = 0; k < m_nPatterns; k++) {
            	for (int l = 0; l < m_nMatrices; l++) {
    				int u = l*m_nStates + k*m_nStates * m_nMatrices;
    				int v = k*m_nStates;
            		for (int i = 0; i < m_nStates; i++) {
            			m_fPartials[0][iNode][u] = fPartials[v];
            			u++;
            			v++;
            		}
            	}
            }
                //System.arraycopy(fPartials, 0, this.m_fPartials[0][iNode], k, fPartials.length);
                //k += fPartials.length;
        //} else {
        //	System.err.println("RRB: should not get here, following line is invalid");
        //   System.arraycopy(fPartials, 0, this.m_fPartials[0][iNode], 0, fPartials.length);
        //}
    }

    /**
     * Allocates states for a node
     */
    public void createNodeStates(int iNode) {

        this.m_iStates[iNode] = new int[m_nPatterns];
    }

    /**
     * Sets states for a node
     */
    public void setNodeStates(int iNode, int[] iStates) {

        if (this.m_iStates[iNode] == null) {
            createNodeStates(iNode);
        }
        System.arraycopy(iStates, 0, this.m_iStates[iNode], 0, m_nPatterns);
        for (int i = 0; i < m_nPatterns; i++) {
        	if (iStates[i] < m_nStates) {
        		m_iStates[iNode][i] = iStates[i];
        	} else {
        		m_iStates[iNode][i] = m_nStates;
        	}
        }
    }

    /**
     * Gets states for a node
     */
    public void getNodeStates(int iNode, int[] iStates) {
        System.arraycopy(this.m_iStates[iNode], 0, iStates, 0, m_nPatterns);
    }

    public void setNodeMatrixForUpdate(int iNode) {
        m_iCurrentMatrices[iNode] = 1 - m_iCurrentMatrices[iNode];

    }


    /**
     * Sets probability matrix for a node
     */
    public void setNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix) {
    	double [] fMatrix2 = m_fMatrices[m_iCurrentMatrices[iNode]][iNode];
    	System.arraycopy(fMatrix, 0, fMatrix2, iMatrixIndex * m_nMatrixSize, fMatrix.length);
//    	double [] fMatrix2 = m_fMatrices[m_iCurrentMatrices[iNode]][iNode];
//    	for (int i = 0; i < m_nStates; i++) {
//	        System.arraycopy(fMatrix, i*m_nStates, fMatrix2,
//	                iMatrixIndex * m_nMatrixSize+i*(m_nStates+1), m_nStates);
//	        fMatrix2[iMatrixIndex * m_nMatrixSize+i*(m_nStates+1)+m_nStates]=1.0;
//    	}
//    	for (int i = 0; i < m_nStates+1; i++) {
//    		fMatrix2[iMatrixIndex * m_nMatrixSize+m_nStates*(m_nStates+1)+i]=1.0;
//    	}
    }
//    @Override
//    public void setPaddedNodeMatrices(int iNode, double[] fMatrix) {
//    	double [] fMatrix2 = m_fMatrices[m_iCurrentMatrices[iNode]][iNode];
//    	System.arraycopy(fMatrix, 0, fMatrix2, 0, fMatrix.length);
//    }
    /**
     * Gets probability matrix for a node
     */
    public void getNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix) {
    	System.err.println("getNodeMatrix call is invalid");
        System.arraycopy(m_fMatrices[m_iCurrentMatrices[iNode]][iNode],
                iMatrixIndex * m_nMatrixSize, fMatrix, 0, m_nMatrixSize);
    }

    public void setNodePartialsForUpdate(int iNode) {
        m_iCurrentPartials[iNode] = 1 - m_iCurrentPartials[iNode];
    }

    
    public void setNodeStatesForUpdate(int iNode) {
    	m_iCurrentStates[iNode] = 1 - m_iCurrentStates[iNode];
    	m_nNrOfID[m_iCurrentStates[iNode]][iNode] = 0;
    }
    
    /**
     * Sets the currently updating node partials for node nodeIndex. This may
     * need to repeatedly copy the partials for the different category partitions
     */
    public void setCurrentNodePartials(int iNode, double[] fPartials) {
        //if (fPartials.length < m_nPartialsSize) {
        //    int k = 0;
        //    for (int i = 0; i < m_nMatrixCount; i++) {
        //        System.arraycopy(fPartials, 0, this.m_fPartials[m_iCurrentPartialsIndices[iNode]][iNode], k, fPartials.length);
        //        k += fPartials.length;
        //    }
        //} else {
        //    System.arraycopy(fPartials, 0, this.m_fPartials[m_iCurrentPartialsIndices[iNode]][iNode], 0, fPartials.length);
        //}
        for (int k = 0; k < m_nPatterns; k++) {
        	for (int l = 0; l < m_nMatrices; l++) {
				int u = l*m_nStates + k*m_nStates * m_nMatrices;
				int v = k*m_nStates;
        		for (int i = 0; i < m_nStates; i++) {
        			m_fPartials[m_iCurrentPartials[iNode]][iNode][u] = fPartials[v];
        			u++;
        			v++;
        		}
        	}
        }
    }


    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNode1 the 'child 1' node
     * @param iNode2 the 'child 2' node
     * @param iNode3 the 'parent' node
     * @param iMatrixMap  a map of which matrix to use for each pattern (can be null if integrating over categories)
     */
  public void calculatePartials(int iNode1, int iNode2, int iNode3, int[] iMatrixMap) {
	  System.err.println("calculatePartials(int, int, int ,int[]) not implemented yet");
  }
//    public void calculatePartials(int iNode1, int iNode2, int iNode3, int[] iMatrixMap) {
//        if (m_iStates[iNode1] != null) {
//            if (m_iStates[iNode2] != null) {
//                calculateStatesStatesPruning(
//                        m_iStates[iNode1], m_fMatrices[m_iCurrentMatricesIndices[iNode1]][iNode1],
//                        m_iStates[iNode2], m_fMatrices[m_iCurrentMatricesIndices[iNode2]][iNode2],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode3]][iNode3], iMatrixMap);
//            } else {
//                calculateStatesPartialsPruning(
//                        m_iStates[iNode1], m_fMatrices[m_iCurrentMatricesIndices[iNode1]][iNode1],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode2]][iNode2], m_fMatrices[m_iCurrentMatricesIndices[iNode2]][iNode2],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode3]][iNode3], iMatrixMap);
//            }
//        } else {
//            if (m_iStates[iNode2] != null) {
//                calculateStatesPartialsPruning(
//                        m_iStates[iNode2], m_fMatrices[m_iCurrentMatricesIndices[iNode2]][iNode2],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode1]][iNode1], m_fMatrices[m_iCurrentMatricesIndices[iNode1]][iNode1],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode3]][iNode3], iMatrixMap);
//            } else {
//                calculatePartialsPartialsPruning(
//                        m_fPartials[m_iCurrentPartialsIndices[iNode1]][iNode1], m_fMatrices[m_iCurrentMatricesIndices[iNode1]][iNode1],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode2]][iNode2], m_fMatrices[m_iCurrentMatricesIndices[iNode2]][iNode2],
//                        m_fPartials[m_iCurrentPartialsIndices[iNode3]][iNode3], iMatrixMap);
//            }
//        }
//
//        if (m_bUseScaling) {
//            scalePartials(iNode3);
//        }
//    }





    /**
     * Scale the partials at a given node. This uses a scaling suggested by Ziheng Yang in
     * Yang (2000) J. Mol. Evol. 51: 423-432
     * <p/>
     * This function looks over the partial likelihoods for each state at each pattern
     * and finds the largest. If this is less than the scalingThreshold (currently set
     * to 1E-40) then it rescales the partials for that pattern by dividing by this number
     * (i.e., normalizing to between 0, 1). It then stores the log of this scaling.
     * This is called for every internal node after the partials are calculated so provides
     * most of the performance hit. Ziheng suggests only doing this on a proportion of nodes
     * but this sounded like a headache to organize (and he doesn't use the threshold idea
     * which improves the performance quite a bit).
     *
     * @param iNode
     */
    protected void scalePartials(int iNode) {
    	double [] fPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
    	int k = m_nNrOfID[m_iCurrentStates[iNode]][iNode] * m_nMatrices * m_nStates;
    	for (int v = 0; v < k; v++) {
    		fPartials[v] *= SCALE;
    	}
    	
//        int v = 0;
//        for (int i = 0; i < m_nNrOfID[m_iCurrentStates[iNode]][iNode]; i++) {
//            for (int k = 0; k < m_nMatrixCount; k++) {
//                for (int j = 0; j < m_nStates; j++) {
//                	fPartials[v] *= SCALE;
//                	v++;
//                }
//            }
//        }
        
//        int u = 0;
//    	double [] fScaleFactor = m_fScalingFactors[m_iCurrentPartials[iNode]][iNode]; 
//        for (int i = 0; i < m_nNrOfID[m_iCurrentStates[iNode]][iNode]; i++) {
//            double scaleFactor = 0.0;
//            int v = u;
//            for (int k = 0; k < m_nMatrixCount; k++) {
//                for (int j = 0; j < m_nStates; j++) {
//                    if (fPartials[v] > scaleFactor) {
//                        scaleFactor = fPartials[v];
//                    }
//                    v++;
//                }
//                v += (m_nPatterns - 1) * m_nStates;
//            }
//
//            if (scaleFactor < m_fScalingThreshold) {
//
//                v = u;
//                for (int k = 0; k < m_nMatrixCount; k++) {
//                    for (int j = 0; j < m_nStates; j++) {
//                    	fPartials[v] /= scaleFactor;
//                        v++;
//                    }
//                    v += (m_nPatterns - 1) * m_nStates;
//                }
//                fScaleFactor[i] = Math.log(scaleFactor);
//
//            } else {
//            	fScaleFactor[i] = 0.0;
//            }
//            u += m_nStates;
//        }
    }

    /**
     * This function returns the scaling factor for that pattern by summing over
     * the log scalings used at each node. If scaling is off then this just returns
     * a 0.
     *
     * @return the log scaling factor
     */
    public double getLogScalingFactor(int iPattern) {
    	if (m_bUseScaling) {
    		return -(m_nNodes/2) * Math.log(SCALE);
    	} else {
    		return 0;
    	}
//        double logScalingFactor = 0.0;
//        if (m_bUseScaling) {
//            for (int i = 0; i < m_nNodes; i++) {
//                logScalingFactor += m_fScalingFactors[m_iCurrentPartials[i]][i][iPattern];
//            }
//        }
//        return logScalingFactor;
    }

    /**
     * Gets the partials for a particular node.
     *
     * @param iNode   the node
     * @param fOutPartials an array into which the partials will go
     */
    public void getPartials(int iNode, double[] fOutPartials) {
        double[] partials1 = m_fPartials[m_iCurrentPartials[iNode]][iNode];

        System.arraycopy(partials1, 0, fOutPartials, 0, m_nPartialsSize);
    }

    /**
     * Store current state
     */
    public void store() {
        System.arraycopy(m_iCurrentMatrices, 0, m_iStoredMatrices, 0, m_nNodes);
        System.arraycopy(m_iCurrentPartials, 0, m_iStoredPartials, 0, m_nNodes);
        System.arraycopy(m_iCurrentStates, 0, m_iStoredStates, 0, m_nNodes);
    }
    
    public void unstore() {
        System.arraycopy(m_iStoredMatrices, 0, m_iCurrentMatrices, 0, m_nNodes);
        System.arraycopy(m_iStoredPartials, 0, m_iCurrentPartials, 0, m_nNodes);
        System.arraycopy(m_iStoredStates, 0, m_iCurrentStates, 0, m_nNodes);
    }

    /**
     * Restore the stored state
     */
    public void restore() {
        // Rather than copying the stored stuff back, just swap the pointers...
        int[] iTmp1 = m_iCurrentMatrices;
        m_iCurrentMatrices = m_iStoredMatrices;
        m_iStoredMatrices = iTmp1;

        int[] iTmp2 = m_iCurrentPartials;
        m_iCurrentPartials = m_iStoredPartials;
        m_iStoredPartials = iTmp2;

        int[] iTmp3 = m_iCurrentStates;
        m_iCurrentStates= m_iStoredStates;
        m_iStoredStates = iTmp3;
    }

	@Override
	public void setUseScaling(double fScale) {
		SCALE = fScale;
		m_bUseScaling = (fScale != 1.0);
	}

//    @Override
//    LikelihoodCore feelsGood() {
//    	int nDefaults = 0;
//    	for (int i = 0; i < 2; i++) {
//    		int [] nCounts = m_nNrOfID[i];
//        	for (int j = m_nNodes/2; j < m_nNodes; j++) {
//        		nDefaults += nCounts[j];
//        	}
//    	}
//		System.err.println("total: " +nDefaults + " #patterns=" + m_nPartialsSize + " #nodes="+m_nNodes + " " + (nDefaults+0.0)/(m_nPatterns * m_nNodes));
//		// savings: \propto (m_nPatterns * m_nNodes/2 - nDefaults/2) * m_nMatrixCount * m_nMatrixSize
//		// costs: \propto m_nPatterns * m_nNodes
//		System.err.println((m_nPatterns * m_nNodes/2 - nDefaults/2) * m_nMatrixCount * m_nMatrixSize +"<"+ m_nPatterns * m_nNodes * 10);
//    	if ((m_nPatterns * m_nNodes/2 - nDefaults/2) * m_nMatrixCount * m_nMatrixSize < m_nPatterns * m_nNodes * 10) {
//    		return getAlternativeCore();
//    	}
//    	return null;
//    }
//    LikelihoodCore getAlternativeCore() {
//    	return new BeerLikelihoodCoreJava(m_nStates);
//    }



    public void calcRootPsuedoRootPartials(double[] fFrequencies, int iNode, double [] fPseudoPartials) {
		int u = 0;
		double [] fInPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
		for (int k = 0; k < m_nPatterns; k++) {
			for (int l = 0; l < m_nMatrices; l++) {
				for (int i = 0; i < m_nStates; i++) {
					fPseudoPartials[u] = fInPartials[u] * fFrequencies[i];
					u++;
				}
			}
		}
    }

    public void calcNodePsuedoRootPartials(double[] fInPseudoPartials, int iNode, double [] fOutPseudoPartials) {
		double [] fPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
		int [] nID = m_nID[m_iCurrentStates[iNode]][iNode];
		
		int u = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			int n = nID[k] * m_nMatrices * m_nStates;
			for (int l = 0; l < m_nMatrices; l++) {
				for (int i = 0; i < m_nStates; i++) {
					fOutPseudoPartials[u] = fInPseudoPartials[u] * fPartials[n];
					u++;
					n++;
				}
			}
		}
	}
    

    public void calcPsuedoRootPartials(double [] fParentPseudoPartials, int iNode, double [] fPseudoPartials) {
		int v = 0;
		int u = 0;
		double [] fMatrices = m_fMatrices[m_iCurrentMatrices[iNode]][iNode];
		for (int k = 0; k < m_nPatterns; k++) {
			for (int l = 0; l < m_nMatrices; l++) {
				for (int i = 0; i < m_nStates; i++) {
					int w = l * m_nMatrixSize;
					double fSum = 0;
					for (int j = 0; j < m_nStates; j++) {
					      fSum += fParentPseudoPartials[u+j] * fMatrices[w];
					      w++;
					}
					fPseudoPartials[v] = fSum;
					v++;
				}
				u += m_nStates;
			}
		}
    }


    void integratePartialsP(double [] fInPartials, double [] fProportions, double [] m_fRootPartials) {
		int u = 0;
		int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			v = 0*m_nStates + k*m_nStates * m_nMatrices;
			for (int i = 0; i < m_nStates; i++) {
				m_fRootPartials[u] = fInPartials[v] * fProportions[0];
				u++;
				v++;
			}
		}
		
		for (int l = 1; l < m_nMatrices; l++) {
			u = 0;
			v = 0;
			for (int k = 0; k < m_nPatterns; k++) {
				v = l*m_nStates + k*m_nStates * m_nMatrices;
				for (int i = 0; i < m_nStates; i++) {
					m_fRootPartials[u] = fInPartials[v] * fProportions[0];
					u++;
					v++;
				}
			}
		}
    } // integratePartials

	/**
	 * Calculates pattern log likelihoods at a node.
	 * @param fPartials the partials used to calculate the likelihoods
	 * @param fFrequencies an array of state frequencies
	 * @param fOutLogLikelihoods an array into which the likelihoods will go
	 */
	public void calculateLogLikelihoodsP(double[] fPartials,double[] fOutLogLikelihoods)
	{
        int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {
            double sum = 0.0;
			for (int i = 0; i < m_nStates; i++) {
				sum += fPartials[v];
				v++;
			}
            fOutLogLikelihoods[k] = Math.log(sum) + getLogScalingFactor(k);
		}
	}


	@Override
	public void setPatternWeights(int [] nPatterWeights) {
		System.arraycopy(nPatterWeights, 0, m_nPatternWeights, 0, m_nPatterns);
	}
	
	@Override
	public void setProportionInvariant(double fProportianInvariant, int [] iConstantPatterns) {
		m_fProportianInvariant = fProportianInvariant;
		m_iConstantPattern = new int[iConstantPatterns.length];
		System.arraycopy(iConstantPatterns, 0, m_iConstantPattern, 0, iConstantPatterns.length);
	}
	
	@Override
	public void getPatternLogLikelihoods(double [] fPatternLogLikelihoods) {
		System.arraycopy(m_fPatternLogLikelihoods, 0, fPatternLogLikelihoods, 0, m_nPatterns);
	}

	@Override
	public double calcLogP(int iNode, double[] fProportions, double[] fFrequencies) {
		integratePartials(iNode, fProportions, m_fRootPartials);

		if (m_iConstantPattern != null) {
        	// some portion of sites is invariant, so adjust root partials for this
        	for (int i : m_iConstantPattern) {
    			m_fRootPartials[i] += m_fProportianInvariant;
        	}
        }

        calculateLogLikelihoods(m_fRootPartials, fFrequencies, m_fPatternLogLikelihoods);

        double fLogP = 0.0;
        for (int i = 0; i < m_nPatterns; i++) {
            fLogP += m_fPatternLogLikelihoods[i] * m_nPatternWeights[i];
        }
        return fLogP;
	}
} // class BeerLikelihoodCoreMixed
