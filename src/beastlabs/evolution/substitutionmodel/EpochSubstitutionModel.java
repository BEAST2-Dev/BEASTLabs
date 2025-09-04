package beastlabs.evolution.substitutionmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.substitutionmodel.EigenDecomposition;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.substitutionmodel.SubstitutionModel.Base;
import beast.base.evolution.tree.Node;



@Description("A substitution model that can change at various threshold dates.")
public class EpochSubstitutionModel extends Base {
	public Input<List<SubstitutionModel>> m_models = new Input<List<SubstitutionModel>>("model","substitution models that apply for certain time intervals", new ArrayList<SubstitutionModel>());
	public Input<RealParameter> m_epochDates = new Input<RealParameter>("epochDates","list of threshold dates. " +
			"The list indicates the dates at which substitution models are switched.", Validate.REQUIRED);

	/** shadows m_models **/
	SubstitutionModel [] m_substitutionModels;
	
	@Override
	public void initAndValidate() {
    	super.initAndValidate();
		m_substitutionModels = new SubstitutionModel[m_models.get().size()];
		int i = 0;
		for (SubstitutionModel model : m_models.get()) {
			m_substitutionModels[i++] = model;
		}
		// ensure the number of epoch dates is one less than the nr of models
		if (m_substitutionModels.length != m_epochDates.get().getDimension()+1) {
			throw new IllegalArgumentException("The number of epoch dates ("+m_epochDates.get().getDimension()+") "
					+ "should be one less than the number of substitution models (" + m_substitutionModels.length + ")");
		}
		
		// sanity check
		int stateCount = getFrequencies().length;
		for (SubstitutionModel s : m_substitutionModels) {
			if (s.getFrequencies().length != stateCount) {
				throw new IllegalArgumentException("Frequencies should all be the same length ("+stateCount+") but found " + s.getFrequencies().length + " in " + ((BEASTInterface)s).getID());
			}
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
		Arrays.fill(tmp, 1.0);
		m_substitutionModels[iStart].getTransitionProbabilities(node, fStartTime, fEpochDates[iStart-1], fRate, matrix);
		int iEnd = iStart - 1;
		int nStates = (int) (Math.sqrt(matrix.length)+0.1) - 1;
		
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
		Arrays.fill(C, 1.0);

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
	public boolean canHandleDataType(DataType dataType) {
		if (m_substitutionModels != null) {
			return m_substitutionModels[0].canHandleDataType(dataType);
		}
		return true;
	}
} // class EpochSubstitutionModel
