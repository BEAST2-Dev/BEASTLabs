
#include "BEER.h"
#include <string.h>
#include <math.h>
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

void BEER::arraycopy(double * src, double * dest, int nLength) {
        memcpy(dest, src, nLength * sizeof(double));
} // arraycopy

/* copy array of length nLenght ints from source src + nSrcOffset to destination dest + nDestOffset*/
void BEER::arraycopy(int * src, int * dest, int nLength) {
        memcpy(dest, src, nLength * sizeof(int));
} // arraycopy

void BEER::arraycopy(double * src, int nSrcOffset, double * dest, int nDestOffset, int nLength) {
        memcpy(&dest[nDestOffset], &src[nSrcOffset], nLength * sizeof(double));
} // arraycopy

/* copy array of length nLenght ints from source src + nSrcOffset to destination dest + nDestOffset*/
void BEER::arraycopy(int * src, int nSrcOffset, int * dest, int nDestOffset, int nLength) {
        memcpy(&dest[nDestOffset], &src[nSrcOffset], nLength * sizeof(int));
} // arraycopy

/* allocate int array of nSize elements and initialize to zero */
int * BEER::newint(int nSize) {
        int * p = new int[nSize];
        memset(p,0,nSize*sizeof(int));
        return p;
} // newInt

/* allocate double array of nSize elements and initialize to zero */
double * BEER::newdouble(int nSize) {
        double * p = new double[nSize];
        memset(p,0,nSize*sizeof(double));
        return p;
} // newDouble


	void BEER::calcAllMatrixSSP(int nNrOfID, int * pStates1, int * pStates2, double * pfMatrices1, double * pfMatrices2, double * pfPartials3) {
		int v = 0;
		for (int i = 0; i < nNrOfID; i ++) {
			for (int l = 0; l < m_nMatrixCount; l++) {
				int w = l * m_nMatrixSize;
				v = m_nStates * (i*m_nMatrixCount + l);
				calcSSP(pStates1[i], pStates2[i], pfMatrices1, pfMatrices2, pfPartials3, w, v);
			}
		}
	}

	void BEER::calcSSP(int state1, int state2, double * pfMatrices1, double * pfMatrices2, double * pfPartials3, int w, int v) {
		for (int i = 0; i < m_nStates; i++) {
			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;
		}
		//return v;
	}
	
	void BEER::calcAllMatrixSPP(int nNrOfID, int * pStates1, int * pStates2, double * pfMatrices1, double * pfMatrices2, double * pfPartials2, double * pfPartials3) {
		int u = 0;
		for (int i = 0; i < nNrOfID; i ++) {
		for (int l = 0; l < m_nMatrixCount; l++) {
            int w = l * m_nMatrixSize;
			int v = (l  + pStates2[i] * m_nMatrixCount) * m_nStates;
				u = m_nStates * (i * m_nMatrixCount + l);
				calcSPP(pStates1[i], pfMatrices1, pfMatrices2, pfPartials2, pfPartials3, w, v, u);
			}
		}
	}

	void BEER::calcAllMatrixSPP2(int * pStates1, double * pfMatrices1, double * pfMatrices2, double * pfPartials2, double * pfPartials3) {
		int u = 0;
		int v = 0;
		for (int i = 0; i < m_nPatterns; i ++) {
			int w = 0;
			//v =  i*m_nMatrixCount * m_nStates;
			for (int l = 0; l < m_nMatrixCount; l++) {
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
	
	void BEER::calcSPP(int state1, double * pfMatrices1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v, int u) {
		double tmp, sum;
		for (int i = 0; i < m_nStates; i++) {
			tmp = pfMatrices1[w + state1];
			sum = 0.0;
			for (int j = 0; j < m_nStates; j++) {
				sum += pfMatrices2[w] * pfPartials2[v + j];
				w++;
			}
			w++;
			pfPartials3[u] = tmp * sum;
			u++;
		}
		//return u;
	}

	void BEER::calcAllMatrixPPP(int nNrOfID, int * pStates1, int * pStates2, double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3) {
		int u = 0;
		for (int i = 0; i < nNrOfID; i ++) {
			int w = 0;
			for (int l = 0; l < m_nMatrixCount; l++) {
				//int w = l * m_nMatrixSize;
				int v1 = (l + pStates1[i] * m_nMatrixCount) * m_nStates;
				int v2 = (l + pStates2[i] * m_nMatrixCount) * m_nStates;
				//u = m_nStates * i * m_nMatrixCount;
				calcPPP(pfMatrices1, pfPartials1, pfMatrices2, pfPartials2, pfPartials3, w, v1, v2, u);
				w += m_nMatrixSize;
				u += m_nStates;
			}
			//u += m_nStates * m_nMatrixCount;
		}
	}
	
	void BEER::calcAllMatrixPPP2(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3) {
		int u = 0;
		int v = 0;
		for (int i = 0; i < m_nPatterns; i++) {
			int w = 0;
			for (int l = 0; l < m_nMatrixCount; l++) {
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

	void BEER::calcPPP(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v1, int v2, int u) {
		double sum1, sum2;
		for (int i = 0; i < m_nStates; i++) {
			sum1=0;
			sum2=0;
			for (int j = 0; j < m_nStates; j++) {
				sum1 += pfMatrices1[w] * pfPartials1[v1 + j];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + j];
				w++;
			}
			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;
		}
		//return u;
	}
	
	void BEER::calcPPP2(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v, int u) {
		double sum1, sum2;
		for (int i = 0; i < m_nStates; i++) {
			sum1=0;
			sum2=0;
			for (int j = 0; j < m_nStates; j++) {
				sum1 += pfMatrices1[w] * pfPartials1[v + j];
				sum2 += pfMatrices2[w] * pfPartials2[v + j];
				w++;
			}
			w++;
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
    void BEER::calculatePartials(int iNode1, int iNode2, int iNode3) {
        if (m_iStates[iNode1] != NULL) {
            if (m_iStates[iNode2] != NULL) {
                calculateStatesStatesPruning(iNode1, iNode2, iNode3);
            } else {
                calculateStatesPartialsPruning(iNode1, iNode2, iNode3);
            }
        } else {
            if (m_iStates[iNode2] != NULL) {
                calculateStatesPartialsPruning(iNode2, iNode1, iNode3);
            } else {
                calculatePartialsPartialsPruning(iNode1, iNode2, iNode3);
            }
        }

    }

	int * BEER::initIDMap(int iNode, int nMaxID1, int nMaxID2) {
		m_nNrOfID[m_iCurrentStates[iNode]][iNode] = 0;
		//int * nIDMap = new int[nMaxID1*nMaxID2];
		int * nIDMap = DEFAULT_MAP;
		//Arrays.fill (nIDMap, 0, nMaxID1*nMaxID2, -1);
        memset(nIDMap, 0xFF, (nMaxID1*nMaxID2) *sizeof(int));
		return nIDMap; 
	} // initIDMap

	/**
	 * Calculates partial likelihoods at a node when both children have states.
	 */
	 void BEER::calculateStatesStatesPruning(int iNode1, int iNode2, int iNode3) {
		int* iStates1 = m_iStates[iNode1]; 
		int* iStates2 = m_iStates[iNode2]; 
		int * nID3 = m_nID[m_iCurrentStates[iNode3]][iNode3];

		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_SS;
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		int * pStates1 = m_nStates1[m_iCurrentStates[iNode3]][iNode3];
		int * pStates2 = m_nStates2[m_iCurrentStates[iNode3]][iNode3];
		m_nStackStates1[m_nTopOfStack] = pStates1;
		m_nStackStates2[m_nTopOfStack] = pStates2;
		m_nTopOfStack++;

		// recalc state indices if necessary
		if (m_nNrOfID[m_iCurrentStates[iNode3]][iNode3] == 0) {
			int * nIDMap = initIDMap(iNode3, m_nStates+1, m_nStates+1);
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
	void BEER::calculateStatesPartialsPruning(int iNode1, int iNode2, int iNode3) {
		int * iStates1 = m_iStates[iNode1]; 
		int * nID2 = m_nID[m_iCurrentStates[iNode2]][iNode2];
		int * nID3 = m_nID[m_iCurrentStates[iNode3]][iNode3];

		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_SP;
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		int * pStates1 = m_nStates1[m_iCurrentStates[iNode3]][iNode3];
		int * pStates2 = m_nStates2[m_iCurrentStates[iNode3]][iNode3];
		m_nStackStates1[m_nTopOfStack] = pStates1;
		m_nStackStates2[m_nTopOfStack] = pStates2;
		m_nTopOfStack++;

		// recalc state indices if necessary
		if (m_nNrOfID[m_iCurrentStates[iNode3]][iNode3] == 0) {
			
			if (m_nNrOfID[m_iCurrentStates[iNode2]][iNode2] == m_nPatterns) {
				arraycopy(iStates1, pStates1, m_nPatterns);
				arraycopy(nID2, pStates2, m_nPatterns);
				arraycopy(DEFAULT_ID, nID3, m_nPatterns);

				m_nStackStates2[m_nTopOfStack-1] = DEFAULT_ID;
				m_nNrOfID[m_iCurrentStates[iNode3]][iNode3] = m_nPatterns;
				return;
			}
			
			int * nIDMap = initIDMap(iNode3, m_nStates+1, m_nNrOfID[m_iCurrentStates[iNode2]][iNode2]);
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
	void BEER::calculatePartialsPartialsPruning(int iNode1, int iNode2, int iNode3) {
		int i1 = m_iCurrentStates[iNode1]; 
		int i2 = m_iCurrentStates[iNode2]; 
		int i3 = m_iCurrentStates[iNode3]; 
		
		int * nID1 = m_nID[i1][iNode1];
		int * nID2 = m_nID[i2][iNode2];
		int * nID3 = m_nID[i3][iNode3];

		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_PP;
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		int * pStates1 = m_nStates1[i3][iNode3];
		int * pStates2 = m_nStates2[i3][iNode3];
		m_nStackStates1[m_nTopOfStack] = pStates1;
		m_nStackStates2[m_nTopOfStack] = pStates2;
		m_nTopOfStack++;

		// recalc state indices if necessary
		if (m_nNrOfID[i3][iNode3] == 0) {

			if (m_nNrOfID[i1][iNode1] == m_nPatterns &&
					m_nNrOfID[i2][iNode2] == m_nPatterns) {
				arraycopy(nID1, pStates1, m_nNrOfID[i1][iNode1]);
				arraycopy(nID2, pStates2, m_nNrOfID[i2][iNode2]);
				arraycopy(DEFAULT_ID, nID3, m_nPatterns);

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
	
			int * nIDMap = initIDMap(iNode3, m_nNrOfID[i1][iNode1], m_nNrOfID[i2][iNode2]);
			int nBase = m_nNrOfID[i1][iNode1];
			m_nNrOfID[i3][iNode3] = calcPPPInner(nID1, nID2, nID3, nBase, nIDMap, pStates1, pStates2, iNode3);
		}
	} // calculatePartialsPartialsPruning

	int BEER::calcPPPInner(int * nID1, int * nID2, int * nID3, int nBase, int * nIDMap, int * pStates1, int * pStates2, int iNode3) {
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
	
	void BEER::processNodeFromStack(int iJob) {
		int iNode1 = m_nNode1[iJob];
		int iNode2 = m_nNode2[iJob];
		int iNode3 = m_nNode3[iJob];
		int * pStates1 = m_nStackStates1[iJob];//m_nStates1[m_iCurrentStates[iNode3]][iNode3];
		int * pStates2 = m_nStackStates2[iJob];//m_nStates2[m_iCurrentStates[iNode3]][iNode3];
		
		double * pfMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
		double * pfMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
		double * pfPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];

		switch (m_nOperation[iJob]) {
		case OPERATION_SS:
			//v=0;
			calcAllMatrixSSP(m_nNrOfID[m_iCurrentStates[iNode3]][iNode3], pStates1, pStates2, pfMatrices1, pfMatrices2, pfPartials3);
			break;
		case OPERATION_SP:
		{
			double * pfPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];  
			if (pStates2 != DEFAULT_ID) {
				calcAllMatrixSPP(m_nNrOfID[m_iCurrentStates[iNode3]][iNode3], pStates1, pStates2, pfMatrices1, pfMatrices2, pfPartials2, pfPartials3);
			} else {
				calcAllMatrixSPP2(pStates1, pfMatrices1, pfMatrices2, pfPartials2, pfPartials3);
			}
		}
			break;
		case OPERATION_PP:
		{
			double * pfPartials1 = m_fPartials[m_iCurrentPartials[iNode1]][iNode1];
			double * pfPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];
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
    void BEER::integratePartials(int iNode, double* fProportions, double* fOutPartials) {

    	for (int iJob = 0; iJob < m_nTopOfStack; iJob++) {
	  		processNodeFromStack(iJob);
	  	}

    	m_nTopOfStack = 0;
    	
    	
		double* fInPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
		int * nID = m_nID[m_iCurrentStates[iNode]][iNode];
		int u = 0;
		int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {
			v = 0*m_nStates + nID[k]*m_nStates * m_nMatrixCount;
			for (int i = 0; i < m_nStates; i++) {
				fOutPartials[u] = fInPartials[v] * fProportions[0];
				u++;
				v++;
			}
		}


		for (int l = 1; l < m_nMatrixCount; l++) {
			u=0;
			for (int k = 0; k < m_nPatterns; k++) {
				v = l*m_nStates + nID[k]*m_nStates * m_nMatrixCount;
				for (int i = 0; i < m_nStates; i++) {
					fOutPartials[u] += fInPartials[v] * fProportions[l];
					u++;
					v++;
				}
			}
		}

//for (int i = 0; i < m_nPatterns; i++) {
//    for (int j = 0; j < m_nStates; j++) {
//        fprintf(stderr,"%f ",fOutPartials[i*m_nStates + j]);
//    }
//}
//fprintf(stderr,"\n");
//
//for (int i  =0; i < m_nNodes; i++) {
//    fprintf(stderr,"%d %d %d %d %d %d\n",m_iStoredMatrices[i], m_iCurrentMatrices[i], m_iStoredPartials[i], m_iCurrentPartials[i], m_iStoredStates[i], m_iCurrentStates[i]);
//}

	}

	/**
	 * Calculates pattern log likelihoods at a node.
	 * @param fPartials the partials used to calculate the likelihoods
	 * @param fFrequencies an array of state frequencies
	 * @param fOutLogLikelihoods an array into which the likelihoods will go
	 */
/*
	void BEER::calculateLogLikelihoods(double* fPartials, double* fFrequencies, double* fOutLogLikelihoods)
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
*/


    /**
     * initializes partial likelihood arrays.
     *
     * @param nNodeCount           the number of nodes in the tree
     * @param nPatternCount        the number of patterns
     * @param nMatrixCount         the number of matrices (i.e., number of categories)
     * @param bIntegrateCategories whether sites are being integrated over all matrices
     */
    void BEER::initialize(int nNodeCount, int nPatternCount, int nMatrixCount, bool bIntegrateCategories) {
//fprintf(stderr,"initialize %d %d %d %d\n", nNodeCount, nPatternCount, nMatrixCount, bIntegrateCategories);
        m_nNodes = nNodeCount;
        m_nPatterns = nPatternCount;
        m_nMatrixCount = nMatrixCount;

        m_bIntegrateCategories = bIntegrateCategories;

        if (bIntegrateCategories) {
            m_nPartialsSize = nPatternCount * m_nStates * nMatrixCount;
        } else {
            m_nPartialsSize = nPatternCount * m_nStates;
        }

        m_fPartials = new double**[2];
        m_fPartials[0] = new double*[nNodeCount];
        m_fPartials[1] = new double*[nNodeCount];


        m_iCurrentMatrices = newint(nNodeCount);
        m_iStoredMatrices = newint(nNodeCount);

        m_iCurrentPartials = newint(nNodeCount);
        m_iStoredPartials = newint(nNodeCount);

        m_iCurrentStates = newint(nNodeCount);
        m_iStoredStates = newint(nNodeCount);

        m_iStates = new int*[nNodeCount];

        for (int i = 0; i < nNodeCount; i++) {
            m_fPartials[0][i] = NULL;
            m_fPartials[1][i] = NULL;

            m_iStates[i] = NULL;
        }

        m_nMatrixSize = (m_nStates+1) * (m_nStates+1);

        //m_fMatrices = newdouble[2][nNodeCount][nMatrixCount * m_nMatrixSize);
        m_fMatrices = new double**[2];
        m_fMatrices[0] = new double*[nNodeCount]; 
        m_fMatrices[1] = new double*[nNodeCount];
        for (int i = 0; i < nNodeCount; i++) {
            m_fMatrices[0][i] = newdouble(nMatrixCount * m_nMatrixSize);
            m_fMatrices[1][i] = newdouble(nMatrixCount * m_nMatrixSize);
        }

        m_nNrOfID = new int*[2];
        m_nNrOfID[0] = newint(nNodeCount);
        m_nNrOfID[1] = newint(nNodeCount);
        m_nID = new int**[2];
        m_nID[0] = new int*[nNodeCount];
        m_nID[1] = new int*[nNodeCount];
        for (int i = 0; i < nNodeCount; i++) {
            m_nID[0][i] = newint(nPatternCount);
            m_nID[1][i] = newint(nPatternCount);
        }

        //m_nIDMap =  new int[nNodeCount]**;
    	//m_pStates1 = new int[m_nPatterns);
    	//m_pStates2 = new int[m_nPatterns);
        
    	m_nTopOfStack = 0;
    	m_nOperation    = newint(nNodeCount); // #nodes
    	m_nNode1        = newint(nNodeCount);// #nodes
    	m_nNode2        = newint(nNodeCount);// #nodes
    	m_nNode3        = newint(nNodeCount);// #nodes

    	m_nStates1      = new int **[2];
    	m_nStates1[0]   = new int *[nNodeCount];
    	m_nStates1[1]   = new int *[nNodeCount];
        for (int i = 0; i < nNodeCount; i++) {
            m_nStates1[0][i] = newint(nPatternCount);
            m_nStates1[1][i] = newint(nPatternCount);
        }
    	m_nStates2      = new int **[2];
    	m_nStates2[0]   = new int *[nNodeCount];
    	m_nStates2[1]   = new int *[nNodeCount];
        for (int i = 0; i < nNodeCount; i++) {
            m_nStates2[0][i] = newint(nPatternCount);
            m_nStates2[1][i] = newint(nPatternCount);
        }
    	m_nStackStates1 = new int*[nNodeCount];
    	m_nStackStates2 = new int*[nNodeCount];

    	DEFAULT_ID = newint(m_nPatterns);
    	for (int k = 0; k < m_nPatterns; k++) {
    		DEFAULT_ID[k] = k;
    	}
    	DEFAULT_MAP = newint(m_nPatterns * m_nPatterns);
    }

    /**
     * cleans up and deallocates arrays.
     */
    void BEER::finalize() {
        // TODO: clean up, now it's leaving a sizeable memory leak
        m_nNodes = 0;
        m_nPatterns = 0;
        m_nMatrixCount = 0;

        m_fPartials = NULL;
        m_iCurrentPartials = NULL;
        m_iStoredPartials = NULL;
        m_iStates = NULL;
        m_fMatrices = NULL;
        m_iCurrentMatrices = NULL;
        m_iStoredMatrices = NULL;

//        m_fScalingFactors = NULL;
    }

//    public bool getUseScaling() {return m_bUseScaling;}
//    
//    public void setUseScaling(bool bUseScaling) {
//        m_bUseScaling = bUseScaling;
////        if (bUseScaling) {
////            m_fScalingFactors = new double[2][m_nNodes][m_nPatterns];
////        }
//    }

    /**
     * Allocates partials for a node
     */

    void BEER::createNodePartials(int iNode) {
        m_fPartials[0][iNode] = newdouble(m_nPartialsSize);
        m_fPartials[1][iNode] = newdouble(m_nPartialsSize);
    }

    /**
     * Sets partials for a node
     */
/*
    void BEER::setNodePartials(int iNode, double* fPartials) {
    	m_nNrOfID[m_iCurrentStates[iNode]][iNode] = 0;
    	
        if (m_fPartials[0][iNode] == NULL) {
            createNodePartials(iNode);
        }
        //if (fPartials.length < m_nPartialsSize) {
            for (int k = 0; k < m_nPatterns; k++) {
            	for (int l = 0; l < m_nMatrixCount; l++) {
    				int u = l*m_nStates + k*m_nStates * m_nMatrixCount;
    				int v = k*m_nStates;
            		for (int i = 0; i < m_nStates; i++) {
            			m_fPartials[0][iNode][u] = fPartials[v];
            			u++;
            			v++;
            		}
            	}
            }
                //System.arraycopy(fPartials, 0, m_fPartials[0][iNode], k, fPartials.length);
                //k += fPartials.length;
        //} else {
        //	System.err.println("RRB: should not get here, following line is invalid");
        //   System.arraycopy(fPartials, 0, m_fPartials[0][iNode], 0, fPartials.length);
        //}
    }
*/

    /**
     * Allocates states for a node
     */
    void BEER::createNodeStates(int iNode) {

        m_iStates[iNode] = newint(m_nPatterns);
    }

    /**
     * Sets states for a node
     */
    void BEER::setNodeStates(int iNode, int* iStates) {

        if (m_iStates[iNode] == NULL) {
            createNodeStates(iNode);
        }
        arraycopy(iStates, m_iStates[iNode], m_nPatterns);
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
//    void BEER::getNodeStates(int iNode, int* iStates) {
//        arraycopy(m_iStates[iNode], 0, iStates, 0, m_nPatterns);
//    }

    void BEER::setNodeMatrixForUpdate(int iNode) {
        m_iCurrentMatrices[iNode] = 1 - m_iCurrentMatrices[iNode];

    }


    /**
     * Sets probability matrix for a node
     */
    void BEER::setNodeMatrix(int iNode, int iMatrixIndex, double* fMatrix) {
    	for (int i = 0; i < m_nStates; i++) {
	        arraycopy(fMatrix, i*m_nStates, m_fMatrices[m_iCurrentMatrices[iNode]][iNode],
	                iMatrixIndex * m_nMatrixSize+i*(m_nStates+1), m_nStates);
	        m_fMatrices[m_iCurrentMatrices[iNode]][iNode][iMatrixIndex * m_nMatrixSize+i*(m_nStates+1)+m_nStates]=1.0;
    	}
    	for (int i = 0; i < m_nStates+1; i++) {
	        m_fMatrices[m_iCurrentMatrices[iNode]][iNode][iMatrixIndex * m_nMatrixSize+m_nStates*(m_nStates+1)+i]=1.0;
    	}
    }

    void BEER::setPaddedMatrices(int iNode, double * fMatrix) {
        arraycopy(fMatrix, m_fMatrices[m_iCurrentMatrices[iNode]][iNode], m_nMatrixSize * m_nMatrixCount);
    }


    /**
     * Gets probability matrix for a node
     */
//    void BEER::getNodeMatrix(int iNode, int iMatrixIndex, double* fMatrix) {
//    	err.println("getNodeMatrix call is invalid");
//        arraycopy(m_fMatrices[m_iCurrentMatrices[iNode]][iNode],
//                iMatrixIndex * m_nMatrixSize, fMatrix, 0, m_nMatrixSize);
//    }

    void BEER::setNodePartialsForUpdate(int iNode) {
        m_iCurrentPartials[iNode] = 1 - m_iCurrentPartials[iNode];
    }

    
    void BEER::setNodeStatesForUpdate(int iNode) {
    	m_iCurrentStates[iNode] = 1 - m_iCurrentStates[iNode];
    	m_nNrOfID[m_iCurrentStates[iNode]][iNode] = 0;
    }
    
    /**
     * Sets the currently updating node partials for node nodeIndex. This may
     * need to repeatedly copy the partials for the different category partitions
     */
/*
    void BEER::setCurrentNodePartials(int iNode, double* fPartials) {
        //if (fPartials.length < m_nPartialsSize) {
        //    int k = 0;
        //    for (int i = 0; i < m_nMatrixCount; i++) {
        //        System.arraycopy(fPartials, 0, m_fPartials[m_iCurrentPartialsIndices[iNode]][iNode], k, fPartials.length);
        //        k += fPartials.length;
        //    }
        //} else {
        //    System.arraycopy(fPartials, 0, m_fPartials[m_iCurrentPartialsIndices[iNode]][iNode], 0, fPartials.length);
        //}
        for (int k = 0; k < m_nPatterns; k++) {
        	for (int l = 0; l < m_nMatrixCount; l++) {
				int u = l*m_nStates + k*m_nStates * m_nMatrixCount;
				int v = k*m_nStates;
        		for (int i = 0; i < m_nStates; i++) {
        			m_fPartials[m_iCurrentPartials[iNode]][iNode][u] = fPartials[v];
        			u++;
        			v++;
        		}
        	}
        }
    }
*/

    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNode1 the 'child 1' node
     * @param iNode2 the 'child 2' node
     * @param iNode3 the 'parent' node
     * @param iMatrixMap  a map of which matrix to use for each pattern (can be NULL if integrating over categories)
     */
//  void BEER::calculatePartials(int iNode1, int iNode2, int iNode3, int* iMatrixMap) {
//	  System.err.println("calculatePartials(int, int, int ,int*) not implemented yet");
//  }
//    public void calculatePartials(int iNode1, int iNode2, int iNode3, int* iMatrixMap) {
//        if (m_iStates[iNode1] != NULL) {
//            if (m_iStates[iNode2] != NULL) {
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
//            if (m_iStates[iNode2] != NULL) {
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
    void BEER::scalePartials(int iNode) {
    	double * fPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
    	int k = m_nNrOfID[m_iCurrentStates[iNode]][iNode] * m_nMatrixCount * m_nStates;
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
//    	double * fScaleFactor = m_fScalingFactors[m_iCurrentPartials[iNode]][iNode]; 
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
    double BEER::getLogScalingFactor(int iPattern) {
    	if (m_bUseScaling) {
    		return -(m_nNodes/2) * log(SCALE);
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
//    void BEER::getPartials(int iNode, double* fOutPartials) {
//        double* partials1 = m_fPartials[m_iCurrentPartials[iNode]][iNode];
//
//       arraycopy(partials1, 0, fOutPartials, 0, m_nPartialsSize);
//    }

    /**
     * Store current state
     */
    void BEER::store() {
        arraycopy(m_iCurrentMatrices, m_iStoredMatrices, m_nNodes);
        arraycopy(m_iCurrentPartials, m_iStoredPartials, m_nNodes);
        arraycopy(m_iCurrentStates, m_iStoredStates, m_nNodes);
    }
    
    void BEER::unstore() {
        arraycopy(m_iStoredMatrices, m_iCurrentMatrices, m_nNodes);
        arraycopy(m_iStoredPartials, m_iCurrentPartials, m_nNodes);
        arraycopy(m_iStoredStates, m_iCurrentStates, m_nNodes);
    }

    /**
     * Restore the stored state
     */
    void BEER::restore() {
        // Rather than copying the stored stuff back, just swap the pointers...
        int* iTmp1 = m_iCurrentMatrices;
        m_iCurrentMatrices = m_iStoredMatrices;
        m_iStoredMatrices = iTmp1;

        int* iTmp2 = m_iCurrentPartials;
        m_iCurrentPartials = m_iStoredPartials;
        m_iStoredPartials = iTmp2;

        int* iTmp3 = m_iCurrentStates;
        m_iCurrentStates= m_iStoredStates;
        m_iStoredStates = iTmp3;
    }

	void BEER::setUseScaling(double fScale) {
		SCALE = fScale;
		m_bUseScaling = (fScale != 1.0);
	}

//    @Override
//    LikelihoodCore feelsGood() {
//    	int nDefaults = 0;
//    	for (int i = 0; i < 2; i++) {
//    		int * nCounts = m_nNrOfID[i];
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
//    	return NULL;
//    }
//    LikelihoodCore getAlternativeCore() {
//    	return new BeerLikelihoodCoreJava(m_nStates);
//    }
//} // class BeerLikelihoodCoreMixed
























	void BEER4::calcSSP(int state1, int state2, double * pfMatrices1, double * pfMatrices2, double * pfPartials3, int w, int v) {
			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			v++;
			w += m_nStates+1;

			pfPartials3[v] = pfMatrices1[w + state1] * pfMatrices2[w + state2];
			//v++;
			w += m_nStates+1;
			//return v;
	}
	
	void BEER4::calcSPP(int state1, double * pfMatrices1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v, int u) {
		double tmp, sum;
			tmp = pfMatrices1[w + state1];
			sum = 0.0;
				sum += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = tmp * sum;
			u++;

			tmp = pfMatrices1[w + state1];
			sum = 0.0;
				sum += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = tmp * sum;
			u++;

			tmp = pfMatrices1[w + state1];
			sum = 0.0;
				sum += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = tmp * sum;
			u++;

			tmp = pfMatrices1[w + state1];
			sum = 0.0;
				sum += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = tmp * sum;
			//u++;
			
		//v += m_nStates;
		//return u;
	}

	void BEER4::calcPPP(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v1, int v2, int u) {
		double sum1, sum2;
			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 3];
				w++;
			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 3];
				w++;
			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;
			
			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 3];
				w++;
			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v1 + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v2 + 3];
				w++;
			w++;
			pfPartials3[u] = sum1 * sum2;
			//u++;
			//return u;
	}
	
	void BEER4::calcPPP2(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v, int u) {
		double sum1, sum2;
			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;
			
			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = sum1 * sum2;
			u++;

			sum1=0;
			sum2=0;
				sum1 += pfMatrices1[w] * pfPartials1[v + 0];
				sum2 += pfMatrices2[w] * pfPartials2[v + 0];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 1];
				sum2 += pfMatrices2[w] * pfPartials2[v + 1];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 2];
				sum2 += pfMatrices2[w] * pfPartials2[v + 2];
				w++;
				sum1 += pfMatrices1[w] * pfPartials1[v + 3];
				sum2 += pfMatrices2[w] * pfPartials2[v + 3];
				w++;
			w++;
			pfPartials3[u] = sum1 * sum2;
			//u++;
			//return u;
	}

