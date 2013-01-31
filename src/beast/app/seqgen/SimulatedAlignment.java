package beast.app.seqgen;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.datatype.DataType;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beast.util.XMLProducer;
import java.io.PrintStream;
import java.util.List;

/**
 * @author remco@cs.waikato.ac.nz
 */
@Description("An alignment containing sequences randomly generated using a"
        + "given site model down a given tree.")
public class SimulatedAlignment extends Alignment {
    public Input<Alignment> m_data = new Input<Alignment>("data", "alignment data which specifies datatype and taxa of the beast.tree", Validate.REQUIRED);
    public Input<Tree> m_treeInput = new Input<Tree>("tree", "phylogenetic beast.tree with sequence data in the leafs", Validate.REQUIRED);
    public Input<SiteModel.Base> m_pSiteModelInput = new Input<SiteModel.Base>("siteModel", "site model for leafs in the beast.tree", Validate.REQUIRED);
    public Input<BranchRateModel.Base> m_pBranchRateModelInput = new Input<BranchRateModel.Base>("branchRateModel",
            "A model describing the rates on the branches of the beast.tree.");
    public Input<Integer> m_sequenceLengthInput = new Input<Integer>("sequencelength", "nr of samples to generate (default 1000).", 1000);
    public Input<String> m_outputFileNameInput = new Input<String>(
            "outputFileName",
            "If provided, simulated alignment is additionally written to this file.");    

    /**
     * nr of samples to generate *
     */
    protected int m_sequenceLength;
    /**
     * tree used for generating samples *
     */
    protected Tree m_tree;
    /**
     * site model used for generating samples *
     */
    protected SiteModel.Base m_siteModel;
    /**
     * branch rate model used for generating samples *
     */
    protected BranchRateModel m_branchRateModel;
    /**
     * nr of categories in site model *
     */
    int m_categoryCount;
    /**
     * nr of states in site model *
     */
    int m_stateCount;

    /**
     * name of output file *
     */
    String m_outputFileName;    

    /**
     * an array used to transfer transition probabilities
     */
    protected double[][] m_probabilities;
    
    public SimulatedAlignment() {
        
        // Override the sequence input requirement.
        m_pSequences.setRule(Validate.OPTIONAL);
    }

    @Override
    public void initAndValidate() throws Exception {
        m_tree = m_treeInput.get();
        m_siteModel = m_pSiteModelInput.get();
        m_branchRateModel = m_pBranchRateModelInput.get();
        m_sequenceLength = m_sequenceLengthInput.get();
        m_stateCount = m_data.get().getMaxStateCount();
        m_categoryCount = m_siteModel.getCategoryCount();
        m_probabilities = new double[m_categoryCount][m_stateCount * m_stateCount];
        m_outputFileName = m_outputFileNameInput.get();
        
        m_pSequences.get().clear();
        
        simulate();        
        
        // Write simulated alignment to disk if requested:
        if (m_outputFileName != null) {
            PrintStream pstream = new PrintStream(m_outputFileName);
            pstream.println(new XMLProducer().toRawXML(this));
        }
        
        super.initAndValidate();
    }

    /**
     * Convert integer representation of sequence into a Sequence
     *
     * @param seq  integer representation of the sequence
     * @param node used to determine taxon for sequence
     * @return Sequence
     * @throws Exception
     */
    Sequence intArray2Sequence(int[] seq, Node node) throws Exception {
        DataType dataType = m_data.get().getDataType();
        String sSeq = dataType.state2string(seq);
//    	StringBuilder sSeq = new StringBuilder();
//    	String sMap = m_data.get().getMap();
//    	if (sMap != null) {
//    		for (int i  = 0; i < m_sequenceLength; i++) {
//    			sSeq.append(sMap.charAt(seq[i]));
//    		}
//    	} else {
//    		for (int i  = 0; i < m_sequenceLength-1; i++) {
//    			sSeq.append(seq[i] + ",");
//    		}
//			sSeq.append(seq[m_sequenceLength-1] + "");
//    	}
        List<Sequence> taxa = m_data.get().m_pSequences.get();
        String sTaxon = taxa.get(node.getNr()).m_sTaxon.get();
        return new Sequence(sTaxon, sSeq.toString());
    } // intArray2Sequence

    /**
     * perform the actual sequence generation
     *
     * @return alignment containing randomly generated sequences for the nodes in the
     *         leaves of the tree
     * @throws Exception
     */
    public void simulate() throws Exception {
        Node root = m_tree.getRoot();


        double[] categoryProbs = m_siteModel.getCategoryProportions(root);
        int[] category = new int[m_sequenceLength];
        for (int i = 0; i < m_sequenceLength; i++) {
            category[i] = Randomizer.randomChoicePDF(categoryProbs);
        }

        double[] frequencies = m_siteModel.getSubstitutionModel().getFrequencies();
        int[] seq = new int[m_sequenceLength];
        for (int i = 0; i < m_sequenceLength; i++) {
            seq[i] = Randomizer.randomChoicePDF(frequencies);
        }


        //alignment.setDataType(m_siteModel.getFrequencyModel().getDataType());

        traverse(root, seq, category);

    } // simulate

    /**
     * recursively walk through the tree top down, and add sequence to alignment whenever
     * a leave node is reached.
     *
     * @param node           reference to the current node, for which we visit all children
     * @param parentSequence randomly generated sequence of the parent node
     * @param category       array of categories for each of the sites
     * @param alignment
     * @throws Exception
     */
    void traverse(Node node, int[] parentSequence, int[] category) throws Exception {
        for (int iChild = 0; iChild < 2; iChild++) {
            Node child = (iChild == 0 ? node.getLeft() : node.getRight());
            for (int i = 0; i < m_categoryCount; i++) {
                getTransitionProbabilities(m_tree, child, i, m_probabilities[i]);
            }

            int[] seq = new int[m_sequenceLength];
            double[] cProb = new double[m_stateCount];
            for (int i = 0; i < m_sequenceLength; i++) {
                System.arraycopy(m_probabilities[category[i]], parentSequence[i] * m_stateCount, cProb, 0, m_stateCount);
                seq[i] = Randomizer.randomChoicePDF(cProb);
            }

            if (child.isLeaf()) {
                m_pSequences.setValue(intArray2Sequence(seq, child), this);
            } else {
                traverse(child, seq, category);
            }
        }
    } // traverse

    /**
     * get transition probability matrix for particular rate category *
     */
    void getTransitionProbabilities(Tree tree, Node node, int rateCategory, double[] probs) {

        Node parent = node.getParent();
        double branchRate = (m_branchRateModel == null ? 1.0 : m_branchRateModel.getRateForBranch(node));
        branchRate *= m_siteModel.getRateForCategory(rateCategory, node);

        // Get the operational time of the branch
        //final double branchTime = branchRate * (parent.getHeight() - node.getHeight());

        //if (branchTime < 0.0) {
        //    throw new RuntimeException("Negative branch length: " + branchTime);
        //}

        //double branchLength = m_siteModel.getRateForCategory(rateCategory) * branchTime;

//        // TODO Hack until SiteRateModel issue is resolved
//        if (m_siteModel.getSubstitutionModel() instanceof SubstitutionEpochModel) {
//            ((SubstitutionEpochModel)m_siteModel.getSubstitutionModel()).getTransitionProbabilities(tree.getNodeHeight(node),
//                    tree.getNodeHeight(parent),branchLength, probs);
//            return;
//        }
        //m_siteModel.getSubstitutionModel().getTransitionProbabilities(branchLength, probs);
        m_siteModel.getSubstitutionModel().getTransitionProbabilities(node, parent.getHeight(), node.getHeight(), branchRate, probs);

    } // getTransitionProbabilities


} // class SequenceAlignment

