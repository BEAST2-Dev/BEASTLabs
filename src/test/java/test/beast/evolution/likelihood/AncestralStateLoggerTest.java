package test.beast.evolution.likelihood;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.likelihood.LikelihoodCore;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.substitutionmodel.HKY;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;
import beastlabs.evolution.likelihood.AncestralStateLogger;

/**
 * Tests for AncestralStateLogger under discrete-Gamma rate heterogeneity.
 *
 * The sampling test computes the exact marginal posterior of the states at an
 * internal node independently (outside x inside partials, averaged over rate
 * categories) and checks that the logger's draws reproduce it.
 */
public class AncestralStateLoggerTest {

	/** Exposes the protected likelihood internals needed for the reference calculation. */
	static class ExposedLogger extends AncestralStateLogger {
		LikelihoodCore core() { return likelihoodCore; }
		SiteModel.Base siteModel() { return m_siteModel; }
		double[] freqs() { return substitutionModel.getFrequencies(); }
	}

	private static SiteModel gammaSiteModel(Alignment data, Object proportionInvariant) throws Exception {
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data);
		HKY hky = new HKY();
		hky.initByName("kappa", "38.82974", "frequencies", freqs);
		SiteModel siteModel = new SiteModel();
		if (proportionInvariant == null) {
			siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4, "shape", "0.137064",
					"substModel", hky);
		} else {
			siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4, "shape", "0.137064",
					"proportionInvariant", proportionInvariant, "substModel", hky);
		}
		return siteModel;
	}

	private static TaxonSet taxonSet(String... names) {
		List<Taxon> taxa = new ArrayList<>();
		for (String name : names) {
			taxa.add(new Taxon(name));
		}
		return new TaxonSet(taxa);
	}

	@Test
	public void testFixedProportionInvariantIsRejected() throws Exception {
		Alignment data = TreeLikelihoodTest.getAlignment();
		Tree tree = TreeLikelihoodTest.getTree(data);
		SiteModel siteModel = gammaSiteModel(data, "0.3");

		AncestralStateLogger logger = new AncestralStateLogger();
		// initByName wraps the IllegalArgumentException from initAndValidate in a RuntimeException.
		RuntimeException e = assertThrows(RuntimeException.class, () ->
			logger.initByName("data", data, "tree", tree, "siteModel", siteModel,
					"taxonset", taxonSet("chimp", "bonobo")));
		assertTrue(e.getMessage().contains("invariant"), e.getMessage());
	}

	@Test
	public void testEstimatedProportionInvariantIsRejected() throws Exception {
		Alignment data = TreeLikelihoodTest.getAlignment();
		Tree tree = TreeLikelihoodTest.getTree(data);
		// Starts at 0 but would move during MCMC, so it must be rejected too.
		RealParameter pInv = new RealParameter();
		pInv.initByName("value", "0.0", "lower", 0.0, "upper", 1.0, "estimate", true);
		SiteModel siteModel = gammaSiteModel(data, pInv);

		AncestralStateLogger logger = new AncestralStateLogger();
		RuntimeException e = assertThrows(RuntimeException.class, () ->
			logger.initByName("data", data, "tree", tree, "siteModel", siteModel,
					"taxonset", taxonSet("chimp", "bonobo")));
		assertTrue(e.getMessage().contains("invariant"), e.getMessage());
	}

	@Test
	public void testGammaDrawsMatchExactMarginalAtRoot() throws Exception {
		checkDrawsMatchExactMarginal(new String[] { "human", "chimp", "bonobo", "gorilla", "orangutan", "siamang" });
	}

	@Test
	public void testGammaDrawsMatchExactMarginalBelowRoot() throws Exception {
		// MRCA(human, chimp, bonobo) is three branches below the root, so this
		// exercises the downward pass with the per-site category carried along.
		checkDrawsMatchExactMarginal(new String[] { "human", "chimp", "bonobo" });
	}

	private void checkDrawsMatchExactMarginal(String[] target) throws Exception {
		Randomizer.setSeed(127);
		Alignment data = TreeLikelihoodTest.getAlignment();
		Tree tree = TreeLikelihoodTest.getTree(data);
		SiteModel siteModel = gammaSiteModel(data, null);

		ExposedLogger logger = new ExposedLogger();
		logger.initByName("data", data, "tree", tree, "siteModel", siteModel, "taxonset", taxonSet(target));

		int siteCount = data.getSiteCount();
		int stateCount = 4;
		int draws = 4000;
		int[][] counts = new int[siteCount][stateCount];
		for (int d = 0; d < draws; d++) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			logger.log(d, new PrintStream(bytes));
			String[] fields = bytes.toString().trim().split("\t");
			assertEquals(siteCount, fields.length);
			for (int s = 0; s < siteCount; s++) {
				int state = Integer.parseInt(fields[s]);
				assertTrue(state >= 0 && state < stateCount, "state out of range: " + state);
				counts[s][state]++;
			}
		}

		double[][] expected = exactMarginal(logger, tree, data, new HashSet<>(Arrays.asList(target)));
		double maxDiff = 0;
		for (int s = 0; s < siteCount; s++) {
			double[] p = expected[data.getPatternIndex(s)];
			for (int i = 0; i < stateCount; i++) {
				maxDiff = Math.max(maxDiff, Math.abs((double) counts[s][i] / draws - p[i]));
			}
		}
		// With 4000 draws the standard error of a frequency is at most 0.0079.
		assertTrue(maxDiff < 0.04, "draws deviate from the exact marginal by " + maxDiff);
	}

	/**
	 * Exact per-pattern marginal posterior of the state at MRCA(target):
	 * P(x = i | D) proportional to sum_k w_k * outside_k(i) * inside_k(i).
	 * Uses the partials and transition matrices left in the likelihood core by
	 * the last call to log().
	 */
	private static double[][] exactMarginal(ExposedLogger logger, Tree tree, Alignment data, Set<String> target) {
		LikelihoodCore core = logger.core();
		int patternCount = data.getPatternCount();
		int stateCount = 4;
		int categoryCount = logger.siteModel().getCategoryCount();
		double[] weights = logger.siteModel().getCategoryProportions(tree.getRoot());
		double[] pi = logger.freqs();

		Node mrca = null;
		for (Node node : tree.getNodesAsArray()) {
			Set<String> leaves = new HashSet<>();
			for (Node leaf : node.getAllLeafNodes()) {
				leaves.add(leaf.getID());
			}
			if (node.isLeaf()) {
				leaves.add(node.getID());
			}
			if (leaves.equals(target)) {
				mrca = node;
			}
		}
		assertNotNull(mrca);

		List<Node> path = new ArrayList<>();
		for (Node n = mrca; n != null; n = n.getParent()) {
			path.add(0, n);
		}

		// outside[k][p][i], starting from the root prior.
		double[][][] outside = new double[categoryCount][patternCount][stateCount];
		for (double[][] perCategory : outside) {
			for (double[] perPattern : perCategory) {
				System.arraycopy(pi, 0, perPattern, 0, stateCount);
			}
		}
		for (int step = 1; step < path.size(); step++) {
			Node child = path.get(step);
			Node parent = child.getParent();
			Node sibling = parent.getLeft() == child ? parent.getRight() : parent.getLeft();
			double[][][] next = new double[categoryCount][patternCount][stateCount];
			for (int k = 0; k < categoryCount; k++) {
				double[] pChild = matrix(core, child, k, stateCount);
				double[] pSibling = matrix(core, sibling, k, stateCount);
				for (int p = 0; p < patternCount; p++) {
					double[] lSibling = inside(core, sibling, k, p, patternCount, stateCount, categoryCount);
					for (int s = 0; s < stateCount; s++) {
						double siblingTerm = 0;
						for (int t = 0; t < stateCount; t++) {
							siblingTerm += pSibling[s * stateCount + t] * lSibling[t];
						}
						double factor = outside[k][p][s] * siblingTerm;
						for (int i = 0; i < stateCount; i++) {
							next[k][p][i] += factor * pChild[s * stateCount + i];
						}
					}
				}
			}
			outside = next;
		}

		double[][] marginal = new double[patternCount][stateCount];
		for (int p = 0; p < patternCount; p++) {
			double total = 0;
			for (int k = 0; k < categoryCount; k++) {
				double[] l = inside(core, mrca, k, p, patternCount, stateCount, categoryCount);
				for (int i = 0; i < stateCount; i++) {
					double v = weights[k] * outside[k][p][i] * l[i];
					marginal[p][i] += v;
					total += v;
				}
			}
			for (int i = 0; i < stateCount; i++) {
				marginal[p][i] /= total;
			}
		}
		return marginal;
	}

	private static double[] matrix(LikelihoodCore core, Node node, int k, int stateCount) {
		double[] m = new double[stateCount * stateCount];
		core.getNodeMatrix(node.getNr(), k, m);
		return m;
	}

	/** Inside partials L_k(node, i) for one pattern; leaves use their observed state. */
	private static double[] inside(LikelihoodCore core, Node node, int k, int p,
			int patternCount, int stateCount, int categoryCount) {
		double[] l = new double[stateCount];
		if (node.isLeaf()) {
			int[] states = new int[patternCount];
			core.getNodeStates(node.getNr(), states);
			if (states[p] >= 0 && states[p] < stateCount) {
				l[states[p]] = 1.0;
			} else {
				Arrays.fill(l, 1.0); // gap or ambiguity
			}
		} else {
			double[] partials = new double[patternCount * stateCount * categoryCount];
			core.getNodePartials(node.getNr(), partials);
			System.arraycopy(partials, (k * patternCount + p) * stateCount, l, 0, stateCount);
		}
		return l;
	}
}
