package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.substitutionmodel.SubstitutionModel.Base;
import beast.evolution.tree.Node;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;

@Description("Substitution model for Peter Wills problem")
public class PetersSubstitutionModel extends Base {
	public Input<TraitSet> m_traits = new Input<TraitSet>("trait","integer labeling of taxa, starting at zero", Validate.REQUIRED); 
	public Input<SubstitutionModel> m_default = new Input<SubstitutionModel>("defaultModel","The not yet transformed substitution model", Validate.REQUIRED);
	public Input<Tree> m_tree = new Input<Tree>("tree","tree to calculate substition model for", Validate.REQUIRED);

	/** rate matrices, one for each node in the tree **/
	double [][] m_fQ;
	double [][] m_fFreqs;
	int m_nStateCount;
	int [] m_nStates;
	double [] m_fDepths;
	/** flag to indicate the matrices are up to date **/
	boolean m_bRecalc;

	@Override
	public void initAndValidate() {
		SubstitutionModel model = m_default.get();
		double [] Q = model.getRateMatrix(null);
		double [] fFreqs = model.getFrequencies();
		m_nStateCount = fFreqs.length;
		m_fQ = new double[m_tree.get().getNodeCount()][m_nStateCount * m_nStateCount];
		m_fFreqs = new double[m_tree.get().getNodeCount()][m_nStateCount];
		m_nStates = new int[m_tree.get().getNodeCount()];
		m_fDepths = new double[m_tree.get().getNodeCount()];

		calcStates(m_tree.get().getRoot(), Q, fFreqs);
		calcFreqs();
		m_bRecalc = false;
	}
	
	int calcStates(Node node, double [] Q, double [] fFreqs) {
		m_fDepths[node.getNr()] = node.getHeight();
		if (node.isLeaf()) {
			m_nStates[node.getNr()] = (int) m_traits.get().getValue(node.getNr());
			System.arraycopy(Q, 0, m_fQ[node.getNr()], 0, Q.length);
			System.arraycopy(fFreqs, 0, m_fFreqs[node.getNr()], 0, fFreqs.length);
		} else {
			int iStateLeft = calcStates(node.m_left, Q, fFreqs);
			int iStateRight = calcStates(node.m_right, Q, fFreqs);
			int i = Math.min(iStateLeft, iStateRight);
			int j = Math.max(iStateLeft, iStateRight);
			
			// update frequencies
			double [] fNewFreqs = m_fFreqs[node.getNr()];
			double [] fOldFreqs = (iStateLeft < iStateRight ? m_fFreqs[node.m_left.getNr()] : m_fFreqs[node.m_right.getNr()]);
			System.arraycopy(fOldFreqs, 0, fNewFreqs, 0, fOldFreqs.length);
			fNewFreqs[i] = fOldFreqs[i] + fOldFreqs[j];
			fNewFreqs[j] = 0.0; 
			
			// update rate matrix Q
			double [] fNewQ = m_fQ[node.getNr()];
			double [] fOldQ = (iStateLeft < iStateRight ? m_fQ[node.m_left.getNr()] : m_fQ[node.m_right.getNr()]);
			System.arraycopy(fOldQ, 0, fNewQ, 0, fOldQ.length);
			// zero out row and column j
			for (int k = 0; k < m_nStateCount; k++) {
				fNewQ[k*m_nStateCount+j] = 0.0;
				fNewQ[j*m_nStateCount+k] = 0.0;
			}
			// update entries for character i
			for (int k = 0; k < m_nStateCount; k++) {
				if (k != i && k != j) {
					fNewQ[i*m_nStateCount+k] = (fOldFreqs[i] * fOldQ[i*m_nStateCount+k] + fOldFreqs[j] * fOldQ[j*m_nStateCount+k])/fNewFreqs[i];
					fNewQ[k*m_nStateCount+i] = fOldQ[k*m_nStateCount+i] + fOldQ[k*m_nStateCount+j];
				}
			}
			// update diagonal for character i, ensure the row sums to zero
			double fSum = -fNewQ[i*m_nStateCount+i];
			for (int k = 0; k < m_nStateCount; k++) {
				fSum += fNewQ[i*m_nStateCount+k];
			}
			fNewQ[i*m_nStateCount+i] = -fSum;
			
			m_nStates[node.getNr()] = Math.min(iStateLeft, iStateRight);
		}
		return m_nStates[node.getNr()];
	}
	
	
	void calcFreqs() {
		for (int i = 0; i < m_nStateCount; i++) {
			
		}
	}
	
	
	@Override
	public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
		// TODO Auto-generated method stub

	}

	@Override
	public EigenDecomposition getEigenDecomposition(Node node) {
		// cannot return EigenDecomposition for this substitution model
		return null;
	}

} // class PetersSubstitutionModel
