package beast.evolution.likelihood;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.util.Randomizer;


@Description("Logs internal states sampled from the distribution at the MRCA of a set of taxa")
public class AncestralStateLogger extends TreeLikelihood implements Loggable {
	public Input<TaxonSet> taxonsetInput = new Input<>("taxonset", "set of taxa defining a clade. The MRCA node of the clade is logged", Validate.REQUIRED);
	public Input<String> valueInput = new Input<>("value", "space delimited set of labels, one for each site in the alignment. Used as site label in the log file.");
	public Input<Boolean> logParentInput = new Input<>("logParent", "flag to indicate the parent value should be logged", false);
	
    // array of flags to indicate which taxa are in the set
    Set<String> isInTaxaSet = new LinkedHashSet<>();
    Node MRCA;
	int [] parentSample;
    boolean logParent;
    
    @Override
	public void initAndValidate() {
		// ensure we do not use BEAGLE
        boolean forceJava = Boolean.valueOf(System.getProperty("java.only"));
        System.setProperty("java.only", "true");
		super.initAndValidate();
        System.setProperty("java.only", "" + forceJava);
        
        String values = valueInput.get();
		if (values != null && values.trim().length() > 0) {
			// use values as labels
			values = values.trim().replaceAll("\\s+", "\t");
			String [] strs = values.split("\t");
			if (strs.length != dataInput.get().getSiteCount()) {
				throw new IllegalArgumentException("Number of labels (" + strs.length + ") does not match amountof data (" + dataInput.get().getSiteCount() +")");
			}
		}
		
// print out duplicates
//        boolean headerShown = false;
//        Alignment data = dataInput.get();
//        for (int i = 0; i < data.getPatternCount(); i++) {
//        	if (data.getPatternWeight(i) > 1) {
//        		int [] pattern = data.getPattern(i);
//        		int k = 0;
//        		for (int j : pattern) {
//        			if (j == 1) {k++;}
//        		}
//        		if (k >= 1) {
//        			if (!headerShown) {
//        				System.out.println("Pattern with multiple occurances that are not (singletons/lexemes)");
//        			}
//        			System.out.print(getID() + " (");
//            		for (int j = 0; j < pattern.length; j++) {
//            			if (pattern[j] == 1) {
//            				System.out.print(data.getTaxaNames().get(j) + " ");
//            			}            			
//            		}
//            		System.out.print(")");
//        			for (int j = 0; j < data.getSiteCount(); j++) {
//        				if (data.getPatternIndex(j) == i) {
//        					System.out.print("site " + j + " ");
//        				}
//        			}
//        			System.out.println();//"(contains " + k+ " 1s):" + Arrays.toString(pattern));
//        		}
//        	}
//        }

	
        isInTaxaSet.clear();
        List<String> taxaNames = dataInput.get().getTaxaNames();
        List<String> set = taxonsetInput.get().asStringList();
        for (final String sTaxon : set) {
            final int iTaxon = taxaNames.indexOf(sTaxon);
            if (iTaxon < 0) {
                throw new IllegalArgumentException("Cannot find taxon " + sTaxon + " in data");
            }
            if (isInTaxaSet.contains(sTaxon)) {
                throw new IllegalArgumentException("Taxon " + sTaxon + " is defined multiple times, while they should be unique");
            }
            isInTaxaSet.add(sTaxon);
        }
        
        logParent = logParentInput.get();
        if (logParent && taxaNames.size() == set.size()) {
        	throw new RuntimeException("Cannot log parent of the root; either choose a different clade, or set logParent flag to false");
        }
	}
	

	@Override
	public void init(PrintStream out) {
		String values = valueInput.get();
		if (values != null && values.trim().length() > 0) {
			// use values as labels
			values = values.trim().replaceAll("\\s+", "\t");
			out.append(values);
			out.append("\t");
		} else {
			int siteCount = dataInput.get().getSiteCount();
			for (int i = 0; i < siteCount; i++) {
				out.append("site"+i+"\t");
			}
		}
	}
	
	@Override
	public void log(int nSample, PrintStream out) {
		try {
			// force fresh recalculation of likelihood at this stage
			Arrays.fill(m_branchLengths, 0);
			calculateLogP();
			
			// determine the MRCA node we are going to log
            calcMRCA(treeInput.get().getRoot(), new int[1]);
            
            // sample states
            int [] sample = sample(MRCA);

            // generate output
			for (int i = 0; i < sample.length; i++) {
				if (logParent) {
					out.append(parentSample[i] + "");
				}
				out.append(sample[i] + "\t");
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** traverse to the root
	 * then, sample root values, and propagate back to the MRCA
	 * along the path that goes between root and MRCA
	 * @return sample
	 */
	private int[] sample(Node node) {
        int siteCount = dataInput.get().getSiteCount();
        int stateCount = dataInput.get().getMaxStateCount();
        int [] sample = new int[siteCount];

		if (node.isRoot()) {
			if (beagle != null) {
				throw new RuntimeException("BEAGLE is not supported yet");
				// m_fRootPartials = beagle.m_fRootPartials;
			}
			
			double [] p = new double[stateCount];
			
			for (int i = 0; i < sample.length; i++) {
				int offset = stateCount * dataInput.get().getPatternIndex(i);
				for (int j = 0; j < stateCount; j++) {
					p[j] = m_fRootPartials[offset + j];
				}
				sample[i] = Randomizer.randomChoicePDF(p);
			}
			
		} else {
			parentSample = sample(node.getParent());
			
			double [] p = new double[stateCount];
			double [] partials = new double[dataInput.get().getPatternCount() * stateCount * m_siteModel.getCategoryCount()];
			
			if (m_siteModel.getCategoryCount() != 1) {
				throw new RuntimeException("Gamma rate heterogeneity or proportion invariant is not supported yet");
			}
			if (beagle != null) {
				throw new RuntimeException("BEAGLE is not supported yet");
				// beagle.beagle.getPartials(arg0, arg1, arg2);
        		// getTransitionMatrix(nodeNum, probabilities);
			} else {
				likelihoodCore.getNodeMatrix(node.getNr(), 0, probabilities);
			}

			if (node.isLeaf()) { 
				if (!m_useAmbiguities.get()) {
					// leaf node values come mainly from the states.
					// only ambiguous sites are sampled
					
					int [] states = new int [dataInput.get().getPatternCount() * m_siteModel.getCategoryCount()];
					if (beagle != null) {
						throw new RuntimeException("BEAGLE is not supported yet");
						// beagle.beagle.getPartials(arg0, arg1, arg2);
		        		// getTransitionMatrix(nodeNum, probabilities);
					} else {
						likelihoodCore.getNodeStates(node.getNr(), states);
					}
					
		            for (int j = 0; j < sample.length; j++) {
		            	int childIndex = dataInput.get().getPatternIndex(j);
		            	if (states[childIndex] >= 0 && states[childIndex] < stateCount) {
		            		// copy state, if it is not ambiguous
		            		sample[j] = states[childIndex];
		            	} else {
		            		sample[j] = -1;
		            	}
		            }
				} else {
					// useAmbiguities == true
					// sample conditioned on child partials
					likelihoodCore.getNodePartials(node.getNr(), partials);

					// sample using transition matrix and parent states
		            for (int j = 0; j < sample.length; j++) {
		                int parentIndex = parentSample[j] * stateCount;
		                int childIndex = dataInput.get().getPatternIndex(j) * stateCount;
		
		                for (int i = 0; i < stateCount; i++) {
		                    p[i] = partials[childIndex + i] * probabilities[parentIndex + i];
		                }
		
						sample[j] = Randomizer.randomChoicePDF(p);
		            }
				}
			} else {
				likelihoodCore.getNodePartials(node.getNr(), partials);

				// sample using transition matrix and parent states
	            for (int j = 0; j < sample.length; j++) {
	                int parentIndex = parentSample[j] * stateCount;
	                int childIndex = dataInput.get().getPatternIndex(j) * stateCount;
	
	                for (int i = 0; i < stateCount; i++) {
	                    p[i] = partials[childIndex + i] * probabilities[parentIndex + i];
	                }
	
					sample[j] = Randomizer.randomChoicePDF(p);
	            }
            }
		}
		return sample;
	}

	@Override
	public void close(PrintStream out) {
		// nothing to do
	}
	
	
    int calcMRCA(final Node node, final int[] nTaxonCount) {
        if (node.isLeaf()) {
            nTaxonCount[0]++;
            if (isInTaxaSet.contains(node.getID())) {
                if (isInTaxaSet.size() == 1) {
                	MRCA = node;
                    return 2;
                }
                return 1;
            } else {
                return 0;
            }
        } else {
            int taxonCount = calcMRCA(node.getLeft(), nTaxonCount);
            final int nLeftTaxa = nTaxonCount[0];
            nTaxonCount[0] = 0;
            if (node.getRight() != null) {
                taxonCount += calcMRCA(node.getRight(), nTaxonCount);
                final int nRightTaxa = nTaxonCount[0];
                nTaxonCount[0] = nLeftTaxa + nRightTaxa;
                if (taxonCount == isInTaxaSet.size()) {
                	MRCA = node;
                    return taxonCount + 1;
                }
            }
            return taxonCount;
        }
    }

	
}
