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
	/** Eigen decomposition corresponding to m_fQ **/
	EigenDecomposition [] m_eigenDecompositon;
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
		frequenciesInput.setRule(Validate.OPTIONAL);
		m_rates.setRule(Validate.OPTIONAL);
	}
	
	@Override
	public void initAndValidate() throws Exception {
		SubstitutionModel model = m_default.get();
		double[] fFreqs = model.getFrequencies();
		m_nStates = fFreqs.length;
		//m_fQ = new double[m_tree.get().getNodeCount()][];//m_nStates * m_nStates];
		m_fP = new double[m_tree.get().getNodeCount()][m_nStates * m_nStates];
		
		m_fFreqs = new double[m_nStates][];//m_nStates];

		m_fFreqs = new double[m_nStates][m_nStates];
		m_fQ = new double[m_nStates][m_nStates * m_nStates];
		m_eigenDecompositon = new EigenDecomposition[m_nStates];
		
		m_nStateOfEpoch = new int[m_tree.get().getNodeCount()];
        eigenSystem = new DefaultEigenSystem(m_nStates);
        m_rateMatrix = new double[m_nStates][m_nStates];
        relativeRates = new double[m_nStates * (m_nStates-1)];
        storedRelativeRates = new double[m_nStates * (m_nStates-1)];
        
        // set root frequencies
        Frequencies freqs = new AARSFrequencies();
        RealParameter rootFrequencies = new RealParameter(new Double[]{1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0});
        freqs.frequencies.setValue(rootFrequencies, freqs);
        freqs.initAndValidate();
        m_frequencies = freqs;
		//m_fDepths = new double[m_tree.get().getNodeCount()];


        // initialise first epoch from WAG
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
		

//		double [] x = { -1.0343, 0.0156, 0.1738, 0.0342, 0.1552, 0.0087, 0.0144, 0.0162, 0.0111, 0.3791, 0.0847, 0.0077, 0.0111, 0.0029, 0.0193, 0.0189, 0.0250, 0.0111, 0.0401, 0.0053,
//				 0.0133, -0.4741, 0.1227, 0.0330, 0.0053, 0.0494, 0.0111, 0.0933, 0.0257, 0.0015, 0.0138, 0.0440, 0.0121, 0.0061, 0.0059, 0.0232, 0.0019, 0.0037, 0.0034, 0.0048,
//				 0.1422, 0.1179, -1.0644, 0.0919, 0.0343, 0.0422, 0.0658, 0.2343, 0.0243, 0.0094, 0.1294, 0.0199, 0.0334, 0.0077, 0.0198, 0.0562, 0.0081, 0.0085, 0.0174, 0.0016,
//				 0.0417, 0.0473, 0.1371, -1.1785, 0.0133, 0.3522, 0.0312, 0.0490, 0.0193, 0.0062, 0.0502, 0.0370, 0.2009, 0.0139, 0.0004, 0.1603, 0.0031, 0.0069, 0.0061, 0.0023,
//				 0.1276, 0.0051, 0.0345, 0.0090, -0.6918, 0.0048, 0.0190, 0.0240, 0.0219, 0.1537, 0.0199, 0.0051, 0.0319, 0.0122, 0.0074, 0.0160, 0.0813, 0.0141, 0.0947, 0.0096,
//				 0.0108, 0.0721, 0.0640, 0.3585, 0.0073, -0.9430, 0.0194, 0.0745, 0.0065, 0.0019, 0.0229, 0.2122, 0.0227, 0.0227, 0.0006, 0.0298, 0.0018, 0.0115, 0.0020, 0.0019,
//				 0.0223, 0.0203, 0.1246, 0.0396, 0.0358, 0.0242, -0.5770, 0.1122, 0.0299, 0.0048, 0.0485, 0.0076, 0.0343, 0.0170, 0.0021, 0.0345, 0.0062, 0.0076, 0.0033, 0.0020,
//				 0.0165, 0.1117, 0.2920, 0.0409, 0.0297, 0.0611, 0.0738, -1.3266, 0.0538, 0.0155, 0.2671, 0.1554, 0.0378, 0.0181, 0.0272, 0.0600, 0.0210, 0.0278, 0.0096, 0.0075,
//				 0.0179, 0.0487, 0.0478, 0.0255, 0.0429, 0.0084, 0.0311, 0.0851, -0.9284, 0.0091, 0.0338, 0.0248, 0.1115, 0.0522, 0.0102, 0.3319, 0.0039, 0.0135, 0.0133, 0.0167,
//				 0.5545, 0.0025, 0.0167, 0.0074, 0.2734, 0.0022, 0.0046, 0.0222, 0.0082, -1.1750, 0.0890, 0.0217, 0.0042, 0.0034, 0.0033, 0.0201, 0.0407, 0.0148, 0.0830, 0.0031,
//				 0.0984, 0.0188, 0.1837, 0.0478, 0.0282, 0.0214, 0.0364, 0.3044, 0.0244, 0.0707, -1.1005, 0.0794, 0.0315, 0.0116, 0.0099, 0.0860, 0.0066, 0.0103, 0.0296, 0.0016,
//				 0.0139, 0.0937, 0.0442, 0.0550, 0.0113, 0.3097, 0.0089, 0.2763, 0.0279, 0.0269, 0.1239, -1.3839, 0.0567, 0.0967, 0.0051, 0.1868, 0.0037, 0.0383, 0.0039, 0.0010,
//				 0.0214, 0.0275, 0.0787, 0.3176, 0.0750, 0.0352, 0.0427, 0.0715, 0.1335, 0.0055, 0.0523, 0.0603, -1.3147, 0.1049, 0.0019, 0.2416, 0.0038, 0.0080, 0.0301, 0.0031,
//				 0.0084, 0.0208, 0.0275, 0.0331, 0.0431, 0.0531, 0.0319, 0.0515, 0.0940, 0.0067, 0.0289, 0.1546, 0.1577, -0.9455, 0.0048, 0.0552, 0.0261, 0.1366, 0.0079, 0.0038,
//				 0.0710, 0.0255, 0.0890, 0.0012, 0.0331, 0.0017, 0.0050, 0.0979, 0.0232, 0.0082, 0.0313, 0.0104, 0.0036, 0.0061, -0.4644, 0.0046, 0.0153, 0.0192, 0.0076, 0.0103,
//				 0.0217, 0.0311, 0.0785, 0.1500, 0.0222, 0.0274, 0.0255, 0.0672, 0.2353, 0.0157, 0.0846, 0.1177, 0.1431, 0.0218, 0.0014, -1.0715, 0.0034, 0.0047, 0.0182, 0.0020,
//				 0.0461, 0.0042, 0.0182, 0.0047, 0.1823, 0.0027, 0.0074, 0.0380, 0.0045, 0.0513, 0.0105, 0.0038, 0.0037, 0.0166, 0.0077, 0.0055, -0.6800, 0.2277, 0.0232, 0.0220,
//				 0.0223, 0.0086, 0.0209, 0.0114, 0.0344, 0.0186, 0.0099, 0.0547, 0.0168, 0.0204, 0.0178, 0.0425, 0.0084, 0.0946, 0.0105, 0.0083, 0.2481, -0.6920, 0.0084, 0.0358,
//				 0.1459, 0.0145, 0.0774, 0.0183, 0.4185, 0.0059, 0.0078, 0.0343, 0.0300, 0.2063, 0.0925, 0.0077, 0.0568, 0.0099, 0.0075, 0.0580, 0.0458, 0.0151, -1.2597, 0.0074,
//				 0.0259, 0.0281, 0.0098, 0.0091, 0.0574, 0.0074, 0.0064, 0.0364, 0.0512, 0.0103, 0.0068, 0.0028, 0.0079, 0.0064, 0.0138, 0.0085, 0.0588, 0.0877, 0.0101, -0.4447};
//		Aminoacid dataType = new Aminoacid();
//		String sCodeMap = dataType.getCodeMap();
//		int [] nCodeMap = new int[dataType.getStateCount()];
//		String sEncoding = "VGAELDPSRITNQHCKFYMW";
//		for (int i = 0; i < dataType.getStateCount(); i++) {
//			nCodeMap[i] = sEncoding.indexOf(sCodeMap.charAt(i));
//		}
//		k = 0;
//		for (int i = 0; i < m_nStates; i++) {
//			int u = nCodeMap[i];
//			for (int j = 0; j < m_nStates; j++) {
//				int v = nCodeMap[j];
//				Q[k++] = x[u*m_nStates + v];
//			}
//		}		
		System.arraycopy(Q, 0, m_fQ[0], 0, Q.length);
		System.arraycopy(fFreqs, 0, m_fFreqs[0], 0, fFreqs.length);
	}
	
	void update() {
		Tree tree = m_tree.get();
		// calculate epochs
		Node[] nodes = tree.getNodesAsArray();
		
		NodeComparator comparator = new NodeComparator();

		// at this point, V (= internalnodes) contains nodes sorted by height
		List<Node> V = new ArrayList<Node>();
		// start with any of the leaf nodes
		V.add(tree.getNode(0));

		calcStates(tree.getRoot(), V);
		Collections.sort(V, comparator);

		//m_fQ[0] = new double[m_nStates * m_nStates];
		//m_fFreqs[0] = new double[m_nStates];
		for (int v = 1; v < V.size(); v++) {
			Node node = V.get(v);
			int iStateLeft = m_nStateOfEpoch[node.getLeft().getNr()];
			int iStateRight = m_nStateOfEpoch[node.getRight().getNr()];
			int i = Math.min(iStateLeft, iStateRight);
			int j = Math.max(iStateLeft, iStateRight);

			// We assume every node is a split node
			assert(i!=j);// if i!=j then this node is not a split node. 
			
			// update frequencies
			//m_fFreqs[node.getNr()] = new double[m_nStates];
			double[] fNewFreqs = m_fFreqs[v];
			double[] fOldFreqs = m_fFreqs[v-1];
			double[] fNewQ = m_fQ[v];
			double[] fOldQ = m_fQ[v-1];

			
			
//			m_fFreqs[node.getNr()] = new double[m_nStates];
//			m_fQ[node.getNr()] = new double[m_nStates * m_nStates];
//			double[] fNewFreqs = m_fFreqs[node.getNr()];
//			double[] fOldFreqs = null;
//			double[] fNewQ = m_fQ[node.getNr()];
//			double[] fOldQ = null;
//			if (iStateLeft < iStateRight) {
//				if (node.m_left.isLeaf()) {
//					fOldFreqs = m_fFreqs[0];
//					fOldQ = m_fQ[0];
//				} else {
//					fOldFreqs = m_fFreqs[node.m_left.getNr()];
//					fOldQ = m_fQ[node.m_left.getNr()];
//				}
//			} else {
//				if (node.m_right.isLeaf()) {
//					fOldFreqs = m_fFreqs[0];
//					fOldQ = m_fQ[0];
//				} else {
//					fOldFreqs = m_fFreqs[node.m_right.getNr()];
//					fOldQ = m_fQ[node.m_right.getNr()];
//				}
//			}
					
			System.arraycopy(fOldFreqs, 0, fNewFreqs, 0, fOldFreqs.length);
			fNewFreqs[i] = fOldFreqs[i] + fOldFreqs[j];
			fNewFreqs[j] = 0.0;

			// update rate matrix Q
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
			double fSum = -fNewQ[i * m_nStates + i];
			for (int k = 0; k < m_nStates; k++) {
				fSum += fNewQ[i * m_nStates + k];
			}
			fNewQ[i * m_nStates + i] = -fSum;
		}

//		DecimalFormat df = new DecimalFormat("##.####");
//		for (int i = 0; i < m_nStates; i++) {
//			System.out.println(i);
//			System.out.println(Arrays.toString(m_fFreqs[i]));
//			int k = 0;
//			for (int j = 0; j < m_fQ[i].length; j++) {
//				if (Math.abs(m_fQ[i][j])>1e-6) {
//					System.out.print(df.format(m_fQ[i][j]) + "\t");
//					k++;
//					if (k % (m_nStates - i) == 0) {
//						System.out.println();
//					}
//				}
//			}
//			System.out.println();
//			//System.out.println(Arrays.toString(m_fQ[i]));
//		}
		
		/** calc eigen decomposition for the epochs **/
		for (int i = 0; i < m_nStates-1; i++) {
			//System.err.println(i);
			m_eigenDecompositon[i] = calcEigenDecomposition(m_fQ[i]);
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
				// handle negative values of iFrom and/or iTo
				if (iFrom < 0) {
					iFrom = -iFrom-1;
				}
				if (iTo < 0) {
					iTo = -iTo-1;
				}
				double d = node.getHeight();
				for (int k = iFrom; k <= Math.min(iTo, m_nStates - 2); k++) {
					Node u = V.get(k);
					Node v = V.get(k + 1);
					
					// A. t = min(d − d[k], d − depth(u))
					double t = Math.min(parent.getHeight() - d, v.getHeight() - d);
					if (t > 0) {
						
						// B. P = exp(Q[k] ∗ t) ∗ P
	//					((AARSFrequencies)frequencies.get()).setFreqs(m_fFreqs[u.getNr()]);
	//					exponentiate(m_fQ[u.getNr()], fP, t);
						((AARSFrequencies)m_frequencies).setFreqs(m_fFreqs[k]);
						//exponentiate(m_fQ[k], fP, t);
						exponentiate(m_eigenDecompositon[k], fP, t);
						
						// C. let i be the state of V [k] and let j be the state of its child that is different from	i.
						int iStateLeft = m_nStateOfEpoch[v.getLeft().getNr()];
						int iStateRight = m_nStateOfEpoch[v.getRight().getNr()];
						int i = Math.min(iStateLeft, iStateRight);
						int j = Math.max(iStateLeft, iStateRight);
						
	
						// D. replace row i of P with the sum of π[k + 1]i /π[k]i times row i 
						// and π[k + 1]j /π[k]i times row j.
						
						// TODO: note maybe these freqs should be swapped
	//					double[] fNewFreqs = m_fFreqs[v.getNr()];
	//					double[] fOldFreqs = m_fFreqs[u.getNr()];
						double[] fNewFreqs = m_fFreqs[k+1];
						double[] fOldFreqs = m_fFreqs[k];
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
		}
		double [] defaultFreqs = {1.0,0.0,0.0,0.0,0.0,
				                  0.0,0.0,0.0,0.0,0.0,
				                  0.0,0.0,0.0,0.0,0.0,
				                  0.0,0.0,0.0,0.0,0.0
		};
		((AARSFrequencies)m_frequencies).setFreqs(defaultFreqs);
		m_bRecalc = false;
	}

	
	 EigenDecomposition calcEigenDecomposition(double [] fQ) {
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
//		    // bring in frequencies
//	        for (int i = 0; i < m_nStates; i++) {
//	            for (int j = i + 1; j < m_nStates; j++) {
//	            	m_rateMatrix[i][j] *= fFreqs[j];
//	            	m_rateMatrix[j][i] *= fFreqs[i];
//	            }
//	        }
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

	        return eigenSystem.decomposeMatrix(m_rateMatrix);
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
        exponentiate(eigenDecomposition, fP, fT);
	}
	
    private void exponentiate(EigenDecomposition eigenDecomposition, double[] fP, double fT) {

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
        
        //multiply(expQt, fP, m_nStates);
        multiply2(expQt, fP, m_nStates);
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

	/** matrix multiplication B = A times B **/
	void multiply2(double [] B, double [] A, int n){
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
			int iStateLeft = calcStates(node.getLeft(), V);
			int iStateRight = calcStates(node.getRight(), V);
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
	
	public double [] getFrequencies(Node node) {
		int i = m_nStateOfEpoch[node.getNr()];
		return m_fFreqs[i];
	}
	
} // class AARSSubstitutionModel
