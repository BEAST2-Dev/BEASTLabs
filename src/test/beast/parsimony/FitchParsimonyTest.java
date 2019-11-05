package test.beast.parsimony;

import java.util.Arrays;

import org.junit.Test;

import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.parsimony.FitchParsimony;
import junit.framework.TestCase;
import test.beast.BEASTTestCase;

public class FitchParsimonyTest extends TestCase {

	/** basic test that parsimony class produces results **/
	@Test
	public void testParsimony() throws Exception {
		Alignment data = BEASTTestCase.getAlignment();
		Tree tree = BEASTTestCase.getTree(data);
		
		FitchParsimony p = new FitchParsimony(data, true);
		double score = p.getScore(tree);
		System.out.println("score = " + score);

		double [] scores = p.getSiteScores(tree);
		System.out.println("scores = " + Arrays.toString(scores));

		for (int i = 6; i < 11; i++) {
			Node node = tree.getNode(i);
			System.out.println(node.toNewick() + " " + Arrays.toString(p.getStates(tree, node)));
		}
	}
}
