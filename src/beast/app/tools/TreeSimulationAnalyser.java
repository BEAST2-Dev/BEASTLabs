package beast.app.tools;

import beast.app.BEASTVersion;
import beast.app.util.Arguments;
import beast.app.util.Version;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeTraceAnalysis;
import beast.util.NexusParser;

import java.io.File;
import java.util.List;

/**
 * TreeSimulationAnalyser, analyse list of trees simulated from a true tree
 *
 * @author Walter Xie
 */
public class TreeSimulationAnalyser extends TreeTraceAnalysis {

    protected double[] branchScoreTreeDistanceList;
    protected int numTrueTree = 0;
    protected int numTopsInCred=0;


    private final static Version version = new BEASTVersion();

    public TreeSimulationAnalyser(Tree trueTree, List<Tree> posteriorTreeList) {
        this(trueTree, posteriorTreeList, DEFAULT_BURN_IN_PERCENTAGE);
    }

    public TreeSimulationAnalyser(Tree trueTree, List<Tree> posteriorTreeList, double burninPercentage) {
        super(posteriorTreeList, burninPercentage);
    }

    protected void calculateBranchScoreTreeDistance() {
        double[] branchScoreTreeDistanceList = new double[topologiesSorted.size()];


    }

    @Override
    protected void analyzeTopologies() {
        super.analyzeTopologies();


        calculateBranchScoreTreeDistance();
    }

    @Override
    protected void calculateCredibleSetFreqs() {





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
            new TreeSimulationAnalyser(trueTree, parser.trees);
        }

    }
}

