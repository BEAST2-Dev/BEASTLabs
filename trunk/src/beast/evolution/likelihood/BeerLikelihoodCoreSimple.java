
/*

 * File BeerLikelihoodCoreSimple.java
 *
 * Copyright (C) 2011 Remco Bouckaert remco@cs.auckland.ac.nz
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

/** likelihood core that collects all operations, than applies Beast1 like calculation 
 */


package beast.evolution.likelihood;

import beast.evolution.sitemodel.SiteModel;


public class BeerLikelihoodCoreSimple extends ExperimentalLikelihoodCore {
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
	//int [] m_iCurrentStates;
	int [] m_iStoredStates;

	/** one number to scale them all */
	double SCALE = 1.05;

	// stack related variables
	final static int OPERATION_SS = 0;
	final static int OPERATION_SP = 1;
	final static int OPERATION_PP = 2;
	int m_nTopOfStack = 0;
	int [] m_nOperation; // #nodes
	int [] m_nNode1;     // #nodes
	int [] m_nNode2;     // #nodes
	int [] m_nNode3;     // #nodes

	/** memory allocation for the root partials **/
    double[] m_fRootPartials;
    /** dealing with proportion of site being invariant **/
    int [] m_iConstantPattern = null;
    double m_fProportianInvariant = 0.0;
    
    /** memory allocation for likelihoods for each of the patterns **/
    double[] m_fPatternLogLikelihoods;      
    int [] m_nPatternWeights;
    
	
	public BeerLikelihoodCoreSimple(int nStateCount) {
		this.m_nStates = nStateCount;
	} // c'tor
	
	
	/**
	 * Calculates partial likelihoods at a node when both children have states.
	 */
	void calcAllMatrixSSP(int iNode1, int iNode2, int iNode3) {
		int [] iStates1 = m_iStates[iNode1];
		int [] iStates2 = m_iStates[iNode2];
		double [] fMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
		double [] fMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
		double [] fPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];
		int v = 0;

		for (int l = 0; l < m_nMatrices; l++) {

			for (int k = 0; k < m_nPatterns; k++) {

				int state1 = iStates1[k];
				int state2 = iStates2[k];

				int w = l * m_nMatrixSize;

                if (state1 < m_nStates && state2 < m_nStates) {

					for (int i = 0; i < m_nStates; i++) {

						fPartials3[v] = fMatrices1[w + state1] * fMatrices2[w + state2];

						v++;
						w += m_nStates;
					}

				} else if (state1 < m_nStates) {
					// child 2 has a gap or unknown state so treat it as unknown

					for (int i = 0; i < m_nStates; i++) {

						fPartials3[v] = fMatrices1[w + state1];

						v++;
						w += m_nStates;
					}
				} else if (state2 < m_nStates) {
					// child 2 has a gap or unknown state so treat it as unknown

					for (int i = 0; i < m_nStates; i++) {

						fPartials3[v] = fMatrices2[w + state2];

						v++;
						w += m_nStates;
					}
				} else {
					// both children have a gap or unknown state so set partials to 1

					for (int j = 0; j < m_nStates; j++) {
						fPartials3[v] = 1.0;
						v++;
					}
				}
			}
		}
	}

	/**
	 * Calculates partial likelihoods at a node when one child has states and one has partials.
	 */
	void calcAllMatrixSPP(int iNode1, int iNode2, int iNode3) {
		int [] iStates1 = m_iStates[iNode1];
		double [] fMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
		double [] fPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];
		double [] fMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
		double [] fPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];

		double sum, tmp;

		int u = 0;
		int v = 0;

		for (int l = 0; l < m_nMatrices; l++) {
			for (int k = 0; k < m_nPatterns; k++) {

				int state1 = iStates1[k];

                int w = l * m_nMatrixSize;

				if (state1 < m_nStates) {


					for (int i = 0; i < m_nStates; i++) {

						tmp = fMatrices1[w + state1];

						sum = 0.0;
						for (int j = 0; j < m_nStates; j++) {
							sum += fMatrices2[w] * fPartials2[v + j];
							w++;
						}

						fPartials3[u] = tmp * sum;
						u++;
					}

					v += m_nStates;
				} else {
					// Child 1 has a gap or unknown state so don't use it

					for (int i = 0; i < m_nStates; i++) {

						sum = 0.0;
						for (int j = 0; j < m_nStates; j++) {
							sum += fMatrices2[w] * fPartials2[v + j];
							w++;
						}

						fPartials3[u] = sum;
						u++;
					}

					v += m_nStates;
				}
			}
		}
	}

	/**
	 * Calculates partial likelihoods at a node when both children have partials.
	 */
	void calcAllMatrixPPP(int iNode1, int iNode2, int iNode3) {
		double [] fPartials1 = m_fPartials[m_iCurrentPartials[iNode1]][iNode1];
		double [] fMatrices1 = m_fMatrices[m_iCurrentMatrices[iNode1]][iNode1];
		double [] fPartials2 = m_fPartials[m_iCurrentPartials[iNode2]][iNode2];
		double [] fMatrices2 = m_fMatrices[m_iCurrentMatrices[iNode2]][iNode2];
		double [] fPartials3 = m_fPartials[m_iCurrentPartials[iNode3]][iNode3];
		double sum1, sum2;

		int u = 0;
		int v = 0;

		for (int l = 0; l < m_nMatrices; l++) {

			for (int k = 0; k < m_nPatterns; k++) {

                int w = l * m_nMatrixSize;

				for (int i = 0; i < m_nStates; i++) {

					sum1 = sum2 = 0.0;

					for (int j = 0; j < m_nStates; j++) {
						sum1 += fMatrices1[w] * fPartials1[v + j];
						sum2 += fMatrices2[w] * fPartials2[v + j];

						w++;
					}

					fPartials3[u] = sum1 * sum2;
					u++;
				}
				v += m_nStates;
			}
		}
	}
	
    /**
     * Calculates partial likelihoods at a node.
     *
     * @param iNode1 the 'child 1' node
     * @param iNode2 the 'child 2' node
     * @param iNode3 the 'parent' node
     */
	@Override
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

	/**
	 * Calculates partial likelihoods at a node when both children have states.
	 */
	private void calculateStatesStatesPruning(int iNode1, int iNode2, int iNode3) {
		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_SS;
		if (m_fPartials[m_iCurrentPartials[iNode1]][iNode1] != null && m_fPartials[m_iCurrentPartials[iNode2]][iNode2] != null) {
			// for handling ambiguities
			m_nOperation[m_nTopOfStack] = OPERATION_PP;
		}
		
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		m_nTopOfStack++;
	}

	/**
	 * Calculates partial likelihoods at a node when one child has states and one has partials.
	 */
	private void calculateStatesPartialsPruning(int iNode1, int iNode2, int iNode3) {
		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_SP;
		if (m_fPartials[m_iCurrentPartials[iNode1]][iNode1] != null && m_fPartials[m_iCurrentPartials[iNode2]][iNode2] != null) {
			// for handling ambiguities
			m_nOperation[m_nTopOfStack] = OPERATION_PP;
		}
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		m_nTopOfStack++;
	}

	/**
	 * Calculates partial likelihoods at a node when both children have partials.
	 */
	private void calculatePartialsPartialsPruning(int iNode1, int iNode2, int iNode3) {
		// prepare the stack
		m_nOperation[m_nTopOfStack] = OPERATION_PP;
		m_nNode1[m_nTopOfStack] = iNode1;
		m_nNode2[m_nTopOfStack] = iNode2;
		m_nNode3[m_nTopOfStack] = iNode3;
		m_nTopOfStack++;
	} // calculatePartialsPartialsPruning
	
	
	@Override
    public void processStack() {
    	for (int iJob = 0; iJob < m_nTopOfStack; iJob++) {
    		int iNode1 = m_nNode1[iJob];
    		int iNode2 = m_nNode2[iJob];
    		int iNode3 = m_nNode3[iJob];

    		switch (m_nOperation[iJob]) {
    		case OPERATION_SS:
    			calcAllMatrixSSP(iNode1, iNode2, iNode3);
    			break;
    		case OPERATION_SP:
    		{
    			calcAllMatrixSPP(iNode1, iNode2, iNode3);
    		}
    			break;
    		case OPERATION_PP:
    		{
    			calcAllMatrixPPP(iNode1, iNode2, iNode3);
    		}
    			break;
    		}
            if (m_bUseScaling) {
                scalePartials(iNode3);
            }
	  	}
    	m_nTopOfStack = 0;
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
	
	/**
	 * Integrates partials across categories.
     * @param fInPartials the array of partials to be integrated
	 * @param fProportions the proportions of sites in each category
	 * @param fOutPartials an array into which the partials will go
	 */
    void integratePartials(int iNode, double[] fProportions, double[] fOutPartials) {
    	processStack();
    	
		double[] fInPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];

		int u = 0;
		int v = 0;
		for (int k = 0; k < m_nPatterns; k++) {

			for (int i = 0; i < m_nStates; i++) {

				fOutPartials[u] = fInPartials[v] * fProportions[0];
				u++;
				v++;
			}
		}


		for (int l = 1; l < m_nMatrices; l++) {
			u = 0;

			for (int k = 0; k < m_nPatterns; k++) {

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
	void calculateLogLikelihoods(double[] fPartials, double[] fFrequencies, double[] fOutLogLikelihoods)
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
	}



    /**
     * initializes partial likelihood arrays.
     *
     * @param nNodeCount           the number of nodes in the tree
     * @param nPatternCount        the number of patterns
     * @param nMatrixCount         the number of matrices (i.e., number of categories)
     * @param bIntegrateCategories whether sites are being integrated over all matrices
     */
    @Override
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

        //m_iCurrentStates = new int[nNodeCount];
        m_iStoredStates = new int[nNodeCount];

        m_iStates = new int[nNodeCount][];

        for (int i = 0; i < nNodeCount; i++) {
            m_fPartials[0][i] = null;
            m_fPartials[1][i] = null;

            m_iStates[i] = null;
        }

        //m_nMatrixSize = (m_nStates+1) * (m_nStates+1);
        m_nMatrixSize = m_nStates * m_nStates;

        m_fMatrices = new double[2][nNodeCount][nMatrixCount * m_nMatrixSize];
        
    	m_nTopOfStack = 0;
    	m_nOperation    = new int[nNodeCount]; // #nodes
    	m_nNode1        = new int[nNodeCount];// #nodes
    	m_nNode2        = new int[nNodeCount];// #nodes
    	m_nNode3        = new int[nNodeCount];// #nodes

        m_fRootPartials = new double[m_nPatterns * m_nStates];
        m_fPatternLogLikelihoods = new double[m_nPatterns];
        m_nPatternWeights  = new int[m_nPatterns];
        return true;
    }

    /**
     * cleans up and deallocates arrays.
     */
    @Override
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
        m_fRootPartials = null;
        m_fPatternLogLikelihoods = null;
        m_nPatternWeights  = null;
    }

    /**
     * Allocates partials for a node
     */
    @Override
    public void createNodePartials(int iNode) {
        this.m_fPartials[0][iNode] = new double[m_nPartialsSize];
        this.m_fPartials[1][iNode] = new double[m_nPartialsSize];
    }

    /**
     * Sets partials for a node
     */
    @Override
    public void setNodePartials(int iNode, double[] fPartials) {
        if (this.m_fPartials[0][iNode] == null) {
            createNodePartials(iNode);
        }
        if (fPartials.length < m_nPartialsSize) {
            int k = 0;
            for (int i = 0; i < m_nMatrices; i++) {
                System.arraycopy(fPartials, 0, this.m_fPartials[0][iNode], k, fPartials.length);
                k += fPartials.length;
            }
        } else {
            System.arraycopy(fPartials, 0, this.m_fPartials[0][iNode], 0, fPartials.length);
        }
    }


    /**
     * Sets states for a node
     */
    @Override
    public void setNodeStates(int iNode, int[] iStates) {

        if (this.m_iStates[iNode] == null) {
            /**
             * Allocates states for a node
             */
            this.m_iStates[iNode] = new int[m_nPatterns];
        }
        System.arraycopy(iStates, 0, this.m_iStates[iNode], 0, m_nPatterns);
    }


    @Override
    public void setNodeMatrixForUpdate(int iNode) {
        m_iCurrentMatrices[iNode] = 1 - m_iCurrentMatrices[iNode];
    }


    /**
     * Sets probability matrix for a node
     */
    @Override
    public void setNodeMatrix(int iNode, int iMatrixIndex, double[] fMatrix) {
        System.arraycopy(fMatrix, 0, m_fMatrices[m_iCurrentMatrices[iNode]][iNode],
                iMatrixIndex * m_nMatrixSize, m_nMatrixSize);
    }


    @Override
    public void setNodePartialsForUpdate(int iNode) {
        m_iCurrentPartials[iNode] = 1 - m_iCurrentPartials[iNode];
    }

    
    @Override
    public void setNodeStatesForUpdate(int iNode) {
    	//m_iCurrentStates[iNode] = 1 - m_iCurrentStates[iNode];
    }



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
    void scalePartials(int iNode) {
    	double [] fPartials = m_fPartials[m_iCurrentPartials[iNode]][iNode];
    	int k = fPartials.length;
    	for (int v = 0; v < k; v++) {
    		fPartials[v] *= SCALE;
    	}
    }

    /**
     * This function returns the scaling factor for that pattern by summing over
     * the log scalings used at each node. If scaling is off then this just returns
     * a 0.
     *
     * @return the log scaling factor
     */
    double getLogScalingFactor(int iPattern) {
    	if (m_bUseScaling) {
    		return -(m_nNodes/2) * Math.log(SCALE);
    	} else {
    		return 0;
    	}
    }

    /**
     * Store current state
     */
    @Override
    public void store() {
        System.arraycopy(m_iCurrentMatrices, 0, m_iStoredMatrices, 0, m_nNodes);
        System.arraycopy(m_iCurrentPartials, 0, m_iStoredPartials, 0, m_nNodes);
        //System.arraycopy(m_iCurrentStates, 0, m_iStoredStates, 0, m_nNodes);
    }
    
    @Override
    public void unstore() {
        System.arraycopy(m_iStoredMatrices, 0, m_iCurrentMatrices, 0, m_nNodes);
        System.arraycopy(m_iStoredPartials, 0, m_iCurrentPartials, 0, m_nNodes);
        //System.arraycopy(m_iStoredStates, 0, m_iCurrentStates, 0, m_nNodes);
    }

    /**
     * Restore the stored state
     */
    @Override
    public void restore() {
        // Rather than copying the stored stuff back, just swap the pointers...
        int[] iTmp1 = m_iCurrentMatrices;
        m_iCurrentMatrices = m_iStoredMatrices;
        m_iStoredMatrices = iTmp1;

        int[] iTmp2 = m_iCurrentPartials;
        m_iCurrentPartials = m_iStoredPartials;
        m_iStoredPartials = iTmp2;

//        int[] iTmp3 = m_iCurrentStates;
//        m_iCurrentStates= m_iStoredStates;
//        m_iStoredStates = iTmp3;
    }

	@Override
	public void setUseScaling(double fScale) {
		SCALE = fScale;
		m_bUseScaling = (fScale != 1.0);
	}

	
	@Override
	public double calcLogP(int iNode, double[] fProportions, double[] fFrequencies) {
		integratePartials(iNode, fProportions, m_fRootPartials);

		if (m_iConstantPattern != null) {
        	// some portion of sites is invariant, so adjust root partials for this
        	for (int i : m_iConstantPattern) {
    			m_fRootPartials[i] += m_fProportianInvariant;
        		System.err.println(i + " " + m_fProportianInvariant + " " + m_fRootPartials[i]);
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
