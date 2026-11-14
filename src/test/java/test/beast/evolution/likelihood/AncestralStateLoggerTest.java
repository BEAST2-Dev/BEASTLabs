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
import beast.base.evolution.datatype.DataType;
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
 * Tests for AncestralStateLogger under discrete-Gamma rate heterogeneity and a
 * proportion of invariant sites.
 *
 * The sampling tests compute the exact marginal posterior of the states at a
 * node independently (outside x inside partials averaged over the Gamma
 * categories, plus the invariant class, in which every node keeps the root
 * state) and check that the logger's draws reproduce it. The same reference
 * calculation is checked against BEAST's own tree likelihood.
 */
public class AncestralStateLoggerTest {

	static final String[] ALL_TAXA = { "human", "chimp", "bonobo", "gorilla", "orangutan", "siamang" };
	/** Three branches below the root: exercises the downward pass. */
	static final String[] HUMAN_CHIMP_BONOBO = { "human", "chimp", "bonobo" };

	/** Exposes the protected likelihood internals needed for the reference calculation. */
	static class ExposedLogger extends AncestralStateLogger {
		LikelihoodCore core() { return likelihoodCore; }
		SiteModel.Base siteModel() { return m_siteModel; }
		double[] freqs() { return substitutionModel.getFrequencies(); }
	}

	/** HKY, optionally with 4 Gamma categories and/or a proportion of invariant sites. */
	private static SiteModel siteModel(Alignment data, boolean gamma, Object proportionInvariant) throws Exception {
		return siteModel(data, gamma, proportionInvariant, "1.0");
	}

	/** As above; a mutationRate above 1 lengthens every branch, which makes the
	 * invariant class easier to tell apart from slow Gamma categories. */
	private static SiteModel siteModel(Alignment data, boolean gamma, Object proportionInvariant,
			String mutationRate) throws Exception {
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data);
		HKY hky = new HKY();
		hky.initByName("kappa", "38.82974", "frequencies", freqs);
		List<Object> args = new ArrayList<>(Arrays.asList("mutationRate", mutationRate, "substModel", hky));
		if (gamma) {
			args.addAll(Arrays.asList("gammaCategoryCount", 4, "shape", "0.137064"));
		}
		if (proportionInvariant != null) {
			args.addAll(Arrays.asList("proportionInvariant", proportionInvariant));
		}
		SiteModel siteModel = new SiteModel();
		siteModel.initByName(args.toArray());
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
	public void testGammaDrawsMatchExactMarginalAtRoot() throws Exception {
		checkDrawsMatchExactMarginal(true, null, "1.0", ALL_TAXA);
	}

	@Test
	public void testGammaDrawsMatchExactMarginalBelowRoot() throws Exception {
		checkDrawsMatchExactMarginal(true, null, "1.0", HUMAN_CHIMP_BONOBO);
	}

	@Test
	public void testGammaInvariantDrawsMatchExactMarginalAtRoot() throws Exception {
		checkDrawsMatchExactMarginal(true, "0.5", "5.0", ALL_TAXA);
	}

	@Test
	public void testGammaInvariantDrawsMatchExactMarginalBelowRoot() throws Exception {
		checkDrawsMatchExactMarginal(true, "0.5", "5.0", HUMAN_CHIMP_BONOBO);
	}

	@Test
	public void testInvariantOnlyDrawsMatchExactMarginalBelowRoot() throws Exception {
		checkDrawsMatchExactMarginal(false, "0.5", "5.0", HUMAN_CHIMP_BONOBO);
	}

	@Test
	public void testEstimatedProportionInvariantStartingAtZero() throws Exception {
		// TreeLikelihood only builds the constant-pattern list when pInv > 0 at
		// start-up; the logger must build it itself so pInv can move later.
		Alignment data = TreeLikelihoodTest.getAlignment();
		Tree tree = TreeLikelihoodTest.getTree(data);
		RealParameter pInv = new RealParameter();
		pInv.initByName("value", "0.0", "lower", 0.0, "upper", 1.0, "estimate", true);
		SiteModel siteModel = siteModel(data, true, pInv);

		ExposedLogger logger = new ExposedLogger();
		logger.initByName("data", data, "tree", tree, "siteModel", siteModel,
				"taxonset", taxonSet(HUMAN_CHIMP_BONOBO));
		assertNotNull(logger.getConstantPattern());
		assertFalse(logger.getConstantPattern().isEmpty());

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		logger.log(0, new PrintStream(bytes));
		assertEquals(data.getSiteCount(), bytes.toString().trim().split("\t").length);
	}

	private void checkDrawsMatchExactMarginal(boolean gamma, Object pInv, String mutationRate, String[] target) throws Exception {
		Randomizer.setSeed(127);
		Alignment data = TreeLikelihoodTest.getAlignment();
		Tree tree = TreeLikelihoodTest.getTree(data);
		SiteModel siteModel = siteModel(data, gamma, pInv, mutationRate);

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

		Reference ref = new Reference(logger, tree, data);
		// The reference model must be the model BEAST evaluates.
		assertEquals(logger.calculateLogP(), ref.logLikelihood(), 1e-8, "reference log-likelihood differs from BEAST's");

		double[][] expected = ref.marginal(new HashSet<>(Arrays.asList(target)));
		double maxDiff = 0;
		for (int s = 0; s < siteCount; s++) {
			double[] p = expected[data.getPatternIndex(s)];
			for (int i = 0; i < stateCount; i++) {
				maxDiff = Math.max(maxDiff, Math.abs((double) counts[s][i] / draws - p[i]));
			}
		}
		System.out.println("AncestralStateLoggerTest gamma=" + gamma + " pInv=" + pInv + " mutationRate=" + mutationRate
				+ " target=" + target.length + " taxa: max deviation " + maxDiff);
		// With 4000 draws the standard error of a frequency is at most 0.0079.
		assertTrue(maxDiff < 0.04, "draws deviate from the exact marginal by " + maxDiff);
	}

	/**
	 * Independent reference calculation from the partials and transition
	 * matrices left in the likelihood core by the last call to log().
	 * Model: Gamma categories with weights w_k (summing to 1 - pInv) plus an
	 * invariant class with weight pInv in which every node has the root state.
	 */
	static class Reference {
		final LikelihoodCore core;
		final Tree tree;
		final Alignment data;
		final int patternCount, stateCount = 4, categoryCount;
		final double[] weights, pi;
		final double pInv;
		/** compatible[p][i]: every taxon at pattern p allows state i. */
		final boolean[][] compatible;

		Reference(ExposedLogger logger, Tree tree, Alignment data) {
			this.core = logger.core();
			this.tree = tree;
			this.data = data;
			this.patternCount = data.getPatternCount();
			this.categoryCount = logger.siteModel().getCategoryCount();
			this.weights = logger.siteModel().getCategoryProportions(tree.getRoot());
			this.pi = logger.freqs();
			this.pInv = logger.siteModel().getProportionInvariant();
			assertEquals(1.0, Arrays.stream(weights).sum() + pInv, 1e-12, "category weights + pInv must sum to 1");

			DataType dataType = data.getDataType();
			compatible = new boolean[patternCount][stateCount];
			for (int p = 0; p < patternCount; p++) {
				Arrays.fill(compatible[p], true);
				for (int code : data.getPattern(p)) {
					boolean[] allowed = new boolean[stateCount];
					for (int state : dataType.getStatesForCode(code)) {
						allowed[state] = true;
					}
					for (int i = 0; i < stateCount; i++) {
						compatible[p][i] &= allowed[i];
					}
				}
			}
		}

		double logLikelihood() {
			double logL = 0;
			for (int p = 0; p < patternCount; p++) {
				double site = 0;
				for (int k = 0; k < categoryCount; k++) {
					double[] l = inside(tree.getRoot(), k, p);
					for (int i = 0; i < stateCount; i++) {
						site += weights[k] * pi[i] * l[i];
					}
				}
				for (int i = 0; i < stateCount; i++) {
					site += compatible[p][i] ? pInv * pi[i] : 0;
				}
				logL += data.getPatternWeight(p) * Math.log(site);
			}
			return logL;
		}

		/** P(x = i | D) at MRCA(target), per pattern. */
		double[][] marginal(Set<String> target) {
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
					double[] pChild = matrix(child, k);
					double[] pSibling = matrix(sibling, k);
					for (int p = 0; p < patternCount; p++) {
						double[] lSibling = inside(sibling, k, p);
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
				for (int i = 0; i < stateCount; i++) {
					double v = compatible[p][i] ? pInv * pi[i] : 0;
					for (int k = 0; k < categoryCount; k++) {
						v += weights[k] * outside[k][p][i] * inside(mrca, k, p)[i];
					}
					marginal[p][i] = v;
					total += v;
				}
				for (int i = 0; i < stateCount; i++) {
					marginal[p][i] /= total;
				}
			}
			return marginal;
		}

		double[] matrix(Node node, int k) {
			double[] m = new double[stateCount * stateCount];
			core.getNodeMatrix(node.getNr(), k, m);
			return m;
		}

		/** Inside partials L_k(node, i) for one pattern; leaves use their observed state. */
		double[] inside(Node node, int k, int p) {
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
}
