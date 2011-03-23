package beast.evolution.substitutionmodel;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.substitutionmodel.SubstitutionModel.Base;
import beast.evolution.tree.Node;

@Description("A substitution model that can change at various threshold dates.")
public class EpochSubstitutionModel extends Base {
	public Input<List<SubstitutionModel>> m_models = new Input<List<SubstitutionModel>>("model","substitution models that apply for certain time intervals", new ArrayList<SubstitutionModel>());
	public Input<RealParameter> m_epochDates = new Input<RealParameter>("epochDates","list of threshold dates. " +
			"The list indicates the dates at which substitution models are switched.", Validate.REQUIRED);

	/** shadows m_models **/
	SubstitutionModel [] m_substitutionModels;
	
	@Override
	public void initAndValidate() throws Exception {
		m_substitutionModels = new SubstitutionModel[m_models.get().size()];
		int i = 0;
		for (SubstitutionModel model : m_models.get()) {
			m_substitutionModels[i++] = model;
		}
		// ensure the number of epoch dates is one less than the nr of models
		if (m_substitutionModels.length != m_epochDates.get().getDimension()+1) {
			throw new Exception("The number of epoch dates should be one less than the number of substitution models");
		}
	}
	
	@Override
	public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
		/** threshold dates **/
		Double [] fEpochDates = m_epochDates.get().getValues();
		/** find start substitution model **/
		int iStart = fEpochDates.length ;
		while (iStart > 0 && fEpochDates[iStart- 1] > fStartTime) {
			iStart--;
		}
		if (iStart == 0 || fEpochDates[iStart- 1] <= fEndTime) {
			// start and end time fall in a single epoch
			m_substitutionModels[iStart].getTransitionProbabilities(node, fStartTime, fEndTime, fRate, matrix);
			return;
		}
		
		double [] tmp = new double[matrix.length];
		m_substitutionModels[iStart].getTransitionProbabilities(node, fStartTime, fEpochDates[iStart-1], fRate, matrix);
		int iEnd = iStart - 1;
		int nStates = (int) (Math.sqrt(matrix.length)+0.1);
		
		while (iEnd > 0 && fEpochDates[iEnd- 1] > fEndTime) {
			// work through epochs that are completely overlapped by the time interval 
			m_substitutionModels[iEnd].getTransitionProbabilities(node, fEpochDates[iEnd], fEpochDates[iEnd-1], fRate, tmp);
			// matrix multiplication
			multiply(matrix, tmp, nStates);
			iEnd--;
		}

		// process last bit of the branch
		m_substitutionModels[iEnd].getTransitionProbabilities(node, fEpochDates[iEnd], fEndTime, fRate, tmp);
		// matrix multiplication
		multiply(matrix, tmp, nStates);
	}

	
	/** matrix multiplication A = A times B **/
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
	    System.arraycopy(C, 0, A, 0, C.length);
	}
		
		
	@Override
	public EigenDecomposition getEigenDecomposition(Node node) {
		// cannot return EigenDecomposition for this substitution model
		return null;
	}


	@Override
	public boolean canHandleDataType(DataType dataType) throws Exception {
		if (m_substitutionModels != null) {
			return m_substitutionModels[0].canHandleDataType(dataType);
		}
		return true;
	}
} // class EpochSubstitutionModel
