package beast.app.tools;

import beast.app.BEASTVersion;
import beast.app.util.Arguments;
import beast.app.util.Version;
import beast.evolution.tree.BranchScoreMetric;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeTraceAnalysis;
import beast.util.FrequencySet;
import beast.util.NexusParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * TreeSimulationAnalyser, analyse list of trees simulated from a true tree
 *
 * @author Walter Xie
 */
public class TreeSimulationAnalyser extends TreeTraceAnalysis {

    protected final Tree trueTree;
    protected List<Double> branchScoreMetricList;
    protected int numTrueTree = 0;
    protected int numTopsInCred=0;


    private final static Version version = new BEASTVersion();

    public TreeSimulationAnalyser(Tree trueTree, List<Tree> posteriorTreeList) {
        this(trueTree, posteriorTreeList, DEFAULT_BURN_IN_FRACTION);
    }

    public TreeSimulationAnalyser(Tree trueTree, List<Tree> posteriorTreeList, double burninFraction) {
        super(posteriorTreeList, burninFraction);
        this.trueTree = trueTree;
    }

    @Override
    public void analyze(double credSetProbability) {
        // set credSetProbability
        topologiesFrequencySet = new FrequencySet<String>(credSetProbability);
        branchScoreMetricList = new ArrayList<Double>();
        BranchScoreMetric branchScoreMetric = new BranchScoreMetric();
        String trueTopology = uniqueNewick(trueTree.getRoot());

        for (Tree tree : treeInTrace) {
            String topology = uniqueNewick(tree.getRoot());
            topologiesFrequencySet.add(topology, 1);

//            double branchScore = branchScoreMetric.getMetric(trueTree, tree); TODO
//            branchScoreMetricList.add(branchScore);
        }

        credibleSet = topologiesFrequencySet.getCredibleSet(trueTopology);

        numTrueTree = topologiesFrequencySet.getFrequency(trueTopology);
        numTopsInCred = credibleSet.credibleSetList.size();
    }

    public static void centreLine(String line, int pageWidth) {
        int n = pageWidth - line.length();
        int n1 = n / 2;
        for (int i = 0; i < n1; i++) {
            System.out.print(" ");
        }
        System.out.println(line);
    }

    public static void printTitle() {
        System.out.println();
        centreLine("BEAST " + version.getVersionString() + ", " + version.getDateString(), 60);
        centreLine("Tree Simulation Analyser", 60);
        for (String creditLine : version.getCredits()) {
            centreLine(creditLine, 60);
        }
        System.out.println();

    }

    public static void printUsage(Arguments arguments) {
        arguments.printUsage("TreeSimulationAnalyser", "[<input-file-name>]");
        System.exit(0);
    }

    //Main method
    public static void main(String[] args) {

        File trueTreesLog = new File("");
        NexusParser parser = new NexusParser();
        try {
            parser.parseFile(trueTreesLog);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Tree> trueTreeList = parser.trees;

        File posteriorTreesLog = new File("");
        parser = new NexusParser();
        try {
            parser.parseFile(posteriorTreesLog);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Tree trueTree : trueTreeList) {
            TreeSimulationAnalyser treeSimulationAnalyser = new TreeSimulationAnalyser(trueTree, parser.trees);
            treeSimulationAnalyser.analyze();
        }

    }
}


