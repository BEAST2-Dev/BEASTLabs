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
		m_fQ = new double[m_tree.get().getNodeCount()][m_nStates * m_nStates];
		m_fP = new double[m_tree.get().getNodeCount()][m_nStates * m_nStates];
		
		m_fFreqs = new double[m_tree.get().getNodeCount()][m_nStates];
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
		double[] Q = model.getRateMatrix(null);
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
		Collections.sort(V, comparator);
		calcStates(tree.getRoot(), V);

		
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
			double[] fNewFreqs = m_fFreqs[node.getNr()];
			double[] fOldFreqs = (iStateLeft < iStateRight ? m_fFreqs[node.m_left.getNr()] : m_fFreqs[node.m_right
					.getNr()]);
			System.arraycopy(fOldFreqs, 0, fNewFreqs, 0, fOldFreqs.length);
			fNewFreqs[i] = fOldFreqs[i] + fOldFreqs[j];
			fNewFreqs[j] = 0.0;

			// update rate matrix Q
			double[] fNewQ = m_fQ[node.getNr()];
			double[] fOldQ = (iStateLeft < iStateRight ? m_fQ[node.m_left.getNr()] : m_fQ[node.m_right.getNr()]);
			System.arraycopy(fOldQ, 0, fNewQ, 0, fOldQ.length);
			// zero out row and column j
			for (int k = 0; k < m_nStates; k++) {
				fNewQ[j * m_nStates + k] = 0.0;
				fNewQ[k * m_nStates + j] = 0.0;
			}
			// update entries for character i
			for (int k = 0; k < m_nStates; k++) {
				if (k != i && k != j) {
					fNewQ[i * m_nStates + k] = (fOldFreqs[i] * fOldQ[i * m_nStates + k] + fOldFreqs[j]
							* fOldQ[j * m_nStates + k])
							/ fNewFreqs[i];
					fNewQ[k * m_nStates + i] = fOldQ[k * m_nStates + i] + fOldQ[k * m_nStates + j];
				}
			}
			// update diagonal for character i, ensure the row sums to zero
//			double fSum = -fNewQ[i * m_nStateCount + i];
//			for (int k = 0; k < m_nStateCount; k++) {
//				fSum += fNewQ[i * m_nStateCount + k];
//			}
//			fNewQ[i * m_nStateCount + i] = -fSum;
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
				for (int k = iFrom; k < iTo; k++) {
					Node u = V.get(k);
					// A. t = min(d − d[k], d − depth(u))
					double t = Math.min(d - u.getHeight(), parent.getHeight() - d);
					
					// B. P = exp(Q[k] ∗ t) ∗ P
					((AARSFrequencies)frequencies.get()).setFreqs(m_fFreqs[u.getNr()]);
					exponentiate(m_fQ[u.getNr()], fP, t);
					
					// C. let i be the state of V [k] and let j be the state of its child that is different from	i.
					int iStateLeft = m_nStateOfEpoch[u.m_left.getNr()];
					int iStateRight = m_nStateOfEpoch[u.m_right.getNr()];
					int i = Math.min(iStateLeft, iStateRight);
					int j = Math.max(iStateLeft, iStateRight);

					// D. replace row i of P with the sum of π[k + 1]i /π[k]i times row i 
					// and π[k + 1]j /π[k]i times row j.
					
					// TODO: note maybe these freqs should be swapped
					double[] fNewFreqs = m_fFreqs[node.getNr()];
					double[] fOldFreqs = m_fFreqs[parent.getNr()];
					for (int iCol = 0; iCol < m_nStates; iCol++) {
						fP[i*m_nStates + iCol] =  fOldFreqs[i] * fP[i*m_nStates + iCol]/ fNewFreqs[i] + 
							fOldFreqs[j] * fP[j*m_nStates + iCol]/ fNewFreqs[j];
					}
					
					// E. zero row j of P and set Pjj = 1.
					for (int iCol = 0; iCol < m_nStates; iCol++) {
						fP[j*m_nStates + iCol] = 0;
					}
					fP[j*m_nStates + j] = 1.0;
				
					// F. d = d − t.
					d = d - t;
				}
				
			}
		}
		m_bRecalc = false;
	}

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

    	setupRateMatrix();
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
		System.arraycopy(m_fP[node.getNr()], 0, matrix, 0, matrix.length);
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
		super.store();
	}
	@Override
	public void restore() {
		m_bRecalc = true;
		super.restore();
	}
	
	
} // class PetersSubstitutionModel
