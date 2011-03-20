package beast.evolution.substitutionmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;

@Description("Substitution model for Peter Wills problem")
public class AARSSubstitutionModel extends GeneralSubstitutionModel {
	public Input<TraitSet> m_traits = new Input<TraitSet>("trait", "integer labeling of taxa, starting at zero",
			Validate.REQUIRED);
	public Input<SubstitutionModel> m_default = new Input<SubstitutionModel>("defaultModel",
			"The not yet transformed substitution model", Validate.REQUIRED);
	public Input<Tree> m_tree = new Input<Tree>("tree", "tree to calculate substition model for", Validate.REQUIRED);

	/** rate matrices, one for each epoch **/
	double[][] m_fQ;
	/** frequencies, one per epoch **/
	double[][] m_fFreqs;
	/** transition probability matrices, one for each node **/
	double[][] m_fP;
	/** state of an epoch **/
	int[] m_nStateOfEpoch;
	//double[] m_fDepths;
	/** flag to indicate the matrices are up to date **/
	boolean m_bRecalc;

	
	public AARSSubstitutionModel() {
		frequencies.setRule(Validate.OPTIONAL);
		m_rates.setRule(Validate.OPTIONAL);
	}
	
	@Override
	public void initAndValidate() throws Exception {
		SubstitutionModel model = m_default.get();
		double[] fFreqs = model.getFrequencies();
		m_nStates = fFreqs.length;
		m_fQ = new double[m_tree.get().getNodeCount()][];//m_nStates * m_nStates];
		m_fP = new double[m_tree.get().getNodeCount()][m_nStates * m_nStates];
		
		m_fFreqs = new double[m_tree.get().getNodeCount()][];//m_nStates];
		m_nStateOfEpoch = new int[m_tree.get().getNodeCount()];
        eigenSystem = new DefaultEigenSystem(m_nStates);
        m_rateMatrix = new double[m_nStates][m_nStates];
        relativeRates = new double[m_nStates * (m_nStates-1)];
        storedRelativeRates = new double[m_nStates * (m_nStates-1)];
        
        // set root frequencies
        Frequencies freqs = new AARSFrequencies();
        RealParameter rootFrequencies = new RealParameter("1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0");
        freqs.frequencies.setValue(rootFrequencies, freqs);
        freqs.initAndValidate();
        frequencies.setValue(freqs, this);
		//m_fDepths = new double[m_tree.get().getNodeCount()];
	}
	
	void update() {
		SubstitutionModel model = m_default.get();
		double[] fFreqs = model.getFrequencies();
		
		// todo: move this to initAndValidate()
		((GeneralSubstitutionModel)model).setupRelativeRates();
		((GeneralSubstitutionModel)model).setupRateMatrix();
		double [][] matrix = ((GeneralSubstitutionModel)model).getRateMatrix();
		double[] Q = new double[matrix.length * matrix.length];
		int k = 0;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				Q[k++] = matrix[i][j];
			}			
		}
		
		
		
		
		
		Tree tree = m_tree.get();
		// calculate epochs
		Node[] nodes = tree.getNodesAsArray();
		
//		Node[] V = new Node[tree.getInternalNodeCount()+1];
//		V[0] = tree.getNode(0);// any leaf will do
//		for (int i = 1; i < V.length; i++) {
//			V[i] = nodes[i + tree.getLeafNodeCount()];
//		}
		NodeComparator comparator = new NodeComparator();
//		Arrays.sort(V, comparator);
		// at this point, V (= internalnodes) contains nodes sorted by height
		List<Node> V = new ArrayList<Node>();
		// start with any of the leaf nodes
		V.add(tree.getNode(0));

		calcStates(tree.getRoot(), V);
		Collections.sort(V, comparator);

		m_fQ[0] = new double[m_nStates * m_nStates];
		m_fFreqs[0] = new double[m_nStates];
		System.arraycopy(Q, 0, m_fQ[0], 0, Q.length);
		System.arraycopy(fFreqs, 0, m_fFreqs[0], 0, fFreqs.length);
		for (int v = 1; v < V.size(); v++) {
			Node node = V.get(v);
			int iStateLeft = m_nStateOfEpoch[node.m_left.getNr()];
			int iStateRight = m_nStateOfEpoch[node.m_right.getNr()];
			int i = Math.min(iStateLeft, iStateRight);
			int j = Math.max(iStateLeft, iStateRight);

			// We assume every node is a split node
			assert(i!=j);// if i!=j then this node is not a split node. 
			
			// update frequencies
			m_fFreqs[node.getNr()] = new double[m_nStates];
			m_fQ[node.getNr()] = new double[m_nStates * m_nStates];
			double[] fNewFreqs = m_fFreqs[node.getNr()];
			double[] fOldFreqs = null;
			double[] fNewQ = m_fQ[node.getNr()];
			double[] fOldQ = null;
			if (iStateLeft < iStateRight) {
				if (node.m_left.isLeaf()) {
					fOldFreqs = m_fFreqs[0];
					fOldQ = m_fQ[0];
				} else {
					fOldFreqs = m_fFreqs[node.m_left.getNr()];
					fOldQ = m_fQ[node.m_left.getNr()];
				}
			} else {
				if (node.m_right.isLeaf()) {
					fOldFreqs = m_fFreqs[0];
					fOldQ = m_fQ[0];
				} else {
					fOldFreqs = m_fFreqs[node.m_right.getNr()];
					fOldQ = m_fQ[node.m_right.getNr()];
				}
			}
					
			System.arraycopy(fOldFreqs, 0, fNewFreqs, 0, fOldFreqs.length);
			fNewFreqs[i] = fOldFreqs[i] + fOldFreqs[j];
			fNewFreqs[j] = 0.0;

			// update rate matrix Q
			System.arraycopy(fOldQ, 0, fNewQ, 0, fOldQ.length);
			// zero out row and column j
			for ( k = 0; k < m_nStates; k++) {
				fNewQ[j * m_nStates + k] = 0.0;
				fNewQ[k * m_nStates + j] = 0.0;
			}
			// update entries for character i
			for (k = 0; k < m_nStates; k++) {
				if (k != i && k != j) {
					fNewQ[i * m_nStates + k] = (fOldFreqs[i] * fOldQ[i * m_nStates + k] + fOldFreqs[j]
							* fOldQ[j * m_nStates + k])
							/ fNewFreqs[i];
					fNewQ[k * m_nStates + i] = fOldQ[k * m_nStates + i] + fOldQ[k * m_nStates + j];
				}
			}
			// update diagonal for character i, ensure the row sums to zero
			double fSum = -fNewQ[i * m_nStates + i];
			for (k = 0; k < m_nStates; k++) {
				fSum += fNewQ[i * m_nStates + k];
			}
			fNewQ[i * m_nStates + i] = -fSum;
		}

		
		
		/** calc transition probability matrices, one for each node **/
		for (int iNode = 0; iNode < nodes.length; iNode++) {
			Node node = nodes[iNode];
			if (!node.isRoot()) {
				double [] fP = m_fP[node.getNr()];
				// set fP to identity matrix
				Arrays.fill(fP, 0.0);
				for (int j = 0; j < m_nStates; j++) {
					fP[j*m_nStates + j] = 1.0;
				}

				Node parent = node.getParent();
				
				int iFrom = Collections.binarySearch(V, node, comparator);
				int iTo = Collections.binarySearch(V, parent, comparator);
				// TODO: handle negative values of iFrom and/or iTo
				double d = node.getHeight();
				for (k = iFrom; k < iTo; k++) {
					Node u = V.get(k);
					Node v = V.get(k + 1);
					
					// A. t = min(d − d[k], d − depth(u))
					double t = Math.min(parent.getHeight() - d, v.getHeight() - d);
					
					// B. P = exp(Q[k] ∗ t) ∗ P
					((AARSFrequencies)frequencies.get()).setFreqs(m_fFreqs[u.getNr()]);
					exponentiate(m_fQ[u.getNr()], fP, t);
					
					// C. let i be the state of V [k] and let j be the state of its child that is different from	i.
					int iStateLeft = m_nStateOfEpoch[v.m_left.getNr()];
					int iStateRight = m_nStateOfEpoch[v.m_right.getNr()];
					int i = Math.min(iStateLeft, iStateRight);
					int j = Math.max(iStateLeft, iStateRight);

					// D. replace row i of P with the sum of π[k + 1]i /π[k]i times row i 
					// and π[k + 1]j /π[k]i times row j.
					
					// TODO: note maybe these freqs should be swapped
					double[] fNewFreqs = m_fFreqs[v.getNr()];
					double[] fOldFreqs = m_fFreqs[u.getNr()];
					for (int iCol = 0; iCol < m_nStates; iCol++) {
						fP[i*m_nStates + iCol] =  fOldFreqs[i] * fP[i*m_nStates + iCol]/ fNewFreqs[i] + 
							fOldFreqs[j] * fP[j*m_nStates + iCol]/ fNewFreqs[i];
					}
					
					// E. zero row j of P and set Pjj = 1.
					for (int iCol = 0; iCol < m_nStates; iCol++) {
						fP[j*m_nStates + iCol] = 0;
					}
					fP[j*m_nStates + j] = 1.0;
				
					// F. d = d − t.
					d = d + t;
				}
				
			}
		}
		double [] defaultFreqs = {1.0,0.0,0.0,0.0,0.0,
				                  0.0,0.0,0.0,0.0,0.0,
				                  0.0,0.0,0.0,0.0,0.0,
				                  0.0,0.0,0.0,0.0,0.0
		};
		((AARSFrequencies)frequencies.get()).setFreqs(defaultFreqs);
		m_bRecalc = false;
	}

	

	
	
	
//    /**
//     * Calculate matrix exponential of a square matrix.
//     *
//     * A scaled Pade approximation algorithm is used.
//     * The algorithm has been directly translated from Golub & Van Loan "Matrix Computations",
//     * algorithm 11.3.1. Special Horner techniques from 11.2 are also used to minimize the number
//     * of matrix multiplications.
//     *
//     * @param A square matrix
//     * @return matrix exponential of A
//     */
//    public static double [] expm(double [] A, int n) {
//        // constants for pade approximation
//        final double c0 = 1.0;
//        final double c1 = 0.5;
//        final double c2 = 0.12;
//        final double c3 = 0.01833333333333333;
//        final double c4 = 0.0019927536231884053;
//        final double c5 = 1.630434782608695E-4;
//        final double c6 = 1.0351966873706E-5;
//        final double c7 = 5.175983436853E-7;
//        final double c8 = 2.0431513566525E-8;
//        final double c9 = 6.306022705717593E-10;
//        final double c10 = 1.4837700484041396E-11;
//        final double c11 = 2.5291534915979653E-13;
//        final double c12 = 2.8101705462199615E-15;
//        final double c13 = 1.5440497506703084E-17;
//
//        int j = Math.max(0, 1 + (int) Math.floor(Math.log(normmax(A)) / Math.log(2)));
//        double [] As = div(A, (double) Math.pow(2, j)); // scaled version of A
//    
//        // calculate D and N using special Horner techniques
//        double [] As_2 = mmul(As, As);
//        double [] As_4 = mmul(As_2, As_2);
//        double [] As_6 = mmul(As_4, As_2);
//        // U = c0*I + c2*A^2 + c4*A^4 + (c6*I + c8*A^2 + c10*A^4 + c12*A^6)*A^6
//        double [] U = DoubleMatrix.eye(n).muli(c0).addi(As_2.mul(c2)).addi(As_4.mul(c4)).addi(
//                DoubleMatrix.eye(n).muli(c6).addi(As_2.mul(c8)).addi(As_4.mul(c10)).addi(As_6.mul(c12)).mmuli(As_6));
//        // V = c1*I + c3*A^2 + c5*A^4 + (c7*I + c9*A^2 + c11*A^4 + c13*A^6)*A^6
//        double [] V = DoubleMatrix.eye(n).muli(c1).addi(As_2.mul(c3)).addi(As_4.mul(c5)).addi(
//                DoubleMatrix.eye(n).muli(c7).addi(As_2.mul(c9)).addi(As_4.mul(c11)).addi(As_6.mul(c13)).mmuli(As_6));
//
//        double [] AV = mmuli(As, V);
//        double [] N = add(U, AV);
//        double [] D = subi(U, AV);
//
//        // solve DF = N for F
//        double [] F = Solve.solve(D, N);
//
//        // now square j times
//        for (int k = 0; k < j; k++) {
//            mmuli(F, F);
//        }
//
//        return F;
//    }

	
//	final static double MIN_STEP = 0.00001;
//
//	void exponentiate_ode(double[] fQ, double[] fP, double t) {
//		//#define MIN_STEP 0.0001
//		//#define MIN_STEP 0.00035
//			// determine step size h, such that h is at most MIN_STEP
//			double h = MIN_STEP;
//			int nSteps;
//			if (h > t) {
//				h = t;
//				nSteps = 1;
//			} else {
//				nSteps = (int)(t/h) + 1;
//				h = t / nSteps;
//			}
//
//			int n = fQ.length;
//			int m = (int) Math.sqrt(n);
//			double [] k1 = new double[n];
//			double [] k2 = new double[n];
//			double [] k3 = new double[n];
//			double [] k4 = new double[n];
//			double [] tmp = new double[n];
//			for (int iStep = 0; iStep < nSteps; iStep++) {
//				//k1 = hAyj
//				multiply(fQ, fP, k1, m);
//				for (int i = 0; i < n; i++) {k1[i] = h * k1[i];}
//
//				// k2 = hA(yj + 1/2 k1 )
//				for (int i = 0; i < n; i++) {tmp[i] =  fP[i]+k1[i]/2.0;}
//				multiply(fQ, tmp, k2, m);
//				for (int i = 0; i < n; i++) {k2[i] = h * k2[i];}
//
//				// k3 = hA(yj + 1/2 k2 )
//				for (int i = 0; i < n; i++) {tmp[i] =  fP[i]+k2[i]/2.0;}
//				multiply(fQ, tmp, k3, m);
//				for (int i = 0; i < n; i++) {k3[i] = h * k3[i];}
//
//				// k4 = hA(yj + k3 )
//				for (int i = 0; i < n; i++) {tmp[i] =  fP[i]+k3[i];}
//				multiply(fQ, tmp, k4, m);
//				for (int i = 0; i < n; i++) {k4[i] = h * k4[i];}
//
//				// yj+1 = yj + 1/6 k1 + 1/3 k2 + 1/3 k3 + 1/6 k4 ,
//				for (int i = 0; i < n; i++) {fP[i] = fP[i] + k1[i]/6.0 + k2[i]/3.0 + k3[i]/3.0 + k4[i]/6.0;}
//			}
//		} // ode_expmv	
//	
//	/** matrix multiplication B = A times B **/
//	void multiply(double [] A, double [] B, double [] C, int n){
//		double[] Bcolj = new double[n];
//	    for (int j = 0; j < n; j++) {
//	      for (int k = 0; k < n; k++) {
//	        Bcolj[k] = B[k*n+j];
//	      }
//	      for (int i = 0; i < n; i++) {
//	        double s = 0;
//	        for (int k = 0; k < n; k++) {
//	          s += A[i * n +k]*Bcolj[k];
//	        }
//	        C[i*n+j] = s;
//	      }
//	    }
//	}
	
	
	/** calculate P = exp(Qt)*P **/
	private void exponentiate(double [] fQ, double[] fP, double fT) {
		// set up relative rates
		//System.arraycopy(fQ, 0, relativeRates, 0, m_nStateCount * (m_nStateCount-1));
		for (int i = 0; i < m_nStates; i++) {
			for (int j = i+1; j < m_nStates - 1; j++) {
				relativeRates[i*(m_nStates -1)+ j] = fQ[i*m_nStates + j];
			    relativeRates[j*(m_nStates -1)+ i] = fQ[j*m_nStates + i];
			}
		}

	    for (int i = 0; i < m_nStates; i++) {
	    	m_rateMatrix[i][i] = 0;
		    for (int j = 0; j < i; j++) {
		    	m_rateMatrix[i][j] = fQ[i*m_nStates + j];
		    }
		    for (int j = i+1; j < m_nStates; j++) {
		    	m_rateMatrix[i][j] = fQ[i*m_nStates + j];
		    }
	    }
//	    // bring in frequencies
//        for (int i = 0; i < m_nStates; i++) {
//            for (int j = i + 1; j < m_nStates; j++) {
//            	m_rateMatrix[i][j] *= fFreqs[j];
//            	m_rateMatrix[j][i] *= fFreqs[i];
//            }
//        }
        // set up diagonal
        for (int i = 0; i < m_nStates; i++) {
            double fSum = 0.0;
            for (int j = 0; j < m_nStates; j++) {
                if (i != j)
                    fSum += m_rateMatrix[i][j];
            }
            m_rateMatrix[i][i] = -fSum;
        }
        // normalise rate matrix to one expected substitution per unit time
        double fSubst = 0.0;
        for (int i = 0; i < m_nStates; i++)
            fSubst += -m_rateMatrix[i][i]; // * fFreqs[i];

        for (int i = 0; i < m_nStates; i++) {
            for (int j = 0; j < m_nStates; j++) {
            	m_rateMatrix[i][j] = m_rateMatrix[i][j] / fSubst;
            }
        }        

        eigenDecomposition = eigenSystem.decomposeMatrix(m_rateMatrix);
    	double[] iexp = new double[m_nStates * m_nStates];
        // Eigen vectors
        double[] Evec = eigenDecomposition.getEigenVectors();
        // inverse Eigen vectors
        double[] Ievc = eigenDecomposition.getInverseEigenVectors();
        // Eigen values
        double[] Eval = eigenDecomposition.getEigenValues();
        for (int i = 0; i < m_nStates; i++) {
            double temp = Math.exp(fT * Eval[i]);
            for (int j = 0; j < m_nStates; j++) {
                iexp[i * m_nStates + j] = Ievc[i * m_nStates + j] * temp;
            }
        }

        int u = 0;
        double [] expQt = new double[fP.length]; 
        for (int i = 0; i < m_nStates; i++) {
            for (int j = 0; j < m_nStates; j++) {
                double temp = 0.0;
                for (int k = 0; k < m_nStates; k++) {
                    temp += Evec[i * m_nStates + k] * iexp[k * m_nStates + j];
                }

                expQt[u] = Math.abs(temp);
                u++;
            }	
        }
        
        multiply(expQt, fP, m_nStates);
	}

	
	/** matrix multiplication B = A times B **/
	void multiply(double [] A, double [] B, int n){
		double [] C = new double[A.length];

		double[] Bcolj = new double[n];
	    for (int j = 0; j < n; j++) {
	      for (int k = 0; k < n; k++) {
	        Bcolj[k] = B[k*n+j];
	      }
	      for (int i = 0; i < n; i++) {
	        double s = 0;
	        for (int k = 0; k < n; k++) {
	          s += A[i * n +k]*Bcolj[k];
	        }
	        C[i*n+j] = s;
	      }
	    }
	    System.arraycopy(C, 0, B, 0, C.length);
	}
	
	
	/** class to sort internal nodes by height **/
	class NodeComparator implements Comparator<Node> {
		@Override
		public int compare(Node o1, Node o2) {
			return (int) Math.signum(o1.getHeight() - o2.getHeight());
		}
	}
	
	/** calculate states, 
	 * The state of the node is the smallest index of a trna at or below that node. Hence
	• The state of the root is 0.
	• If two siblings have the same state then all leaves below them also have the same state.
	 **/
	int calcStates(Node node, List<Node> V) {
		//m_fDepths[node.getNr()] = node.getHeight();
		if (node.isLeaf()) {
			m_nStateOfEpoch[node.getNr()] = (int) m_traits.get().getValue(node.getNr());
		} else {
			int iStateLeft = calcStates(node.m_left, V);
			int iStateRight = calcStates(node.m_right, V);
			m_nStateOfEpoch[node.getNr()] = Math.min(iStateLeft, iStateRight);
			if (iStateLeft != iStateRight) {
				V.add(node);
			}
		}
		return m_nStateOfEpoch[node.getNr()];
	}

	@Override
	public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
		if (m_bRecalc) {
			update();
		}
		System.arraycopy(m_fP[node.getNr()], 0, matrix, 0, m_nStates * m_nStates);
	}

	@Override
	public EigenDecomposition getEigenDecomposition(Node node) {
		// cannot return EigenDecomposition for this substitution model
		return null;
	}

    /** CalculationNode methods **/
	@Override
	public boolean requiresRecalculation() {
		m_bRecalc = true;
		return true;
	}
	@Override
	public void store() {
		m_bRecalc = true;
		//super.store();
	}
	@Override
	public void restore() {
		m_bRecalc = true;
		//super.restore();
	}
	
	
} // class PetersSubstitutionModel
