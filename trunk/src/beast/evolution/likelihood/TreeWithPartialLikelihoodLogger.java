package beast.evolution.likelihood;

import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Input.Validate;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.AARSSubstitutionModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Logs partial likelihood at all internal nodes in the tree with the tree. " +
		"The")
public class TreeWithPartialLikelihoodLogger extends Plugin implements Loggable {

	public Input<TreeLikelihood> likelihoodInput = new Input<TreeLikelihood>("likelihood",
			"tree likelihood used to calc partials", Validate.REQUIRED);

	TreeLikelihood likelihood;
	SiteModel.Base siteModel;
	SubstitutionModel substModel;
	Alignment data;
	Tree tree;
	double [] partials;
	double [] probs;
	int [] patternweight;
	int stateCount;
	int patternCount;
	int categoryCount;

	public void initAndValidate() throws Exception {
		likelihood = likelihoodInput.get();
		tree = likelihood.m_tree.get();
		siteModel = likelihood.m_pSiteModel.get();
		substModel = siteModel.m_pSubstModel.get();
		data = likelihood.m_data.get();
        stateCount = data.getDataType().getStateCount();
        patternCount = data.getPatternCount();
        categoryCount = siteModel.getCategoryCount();
        if (categoryCount > 1) {
        	throw new Exception("not implemented yet for categoryCount > 1");
        }
		partials = new double[stateCount * patternCount * categoryCount];
		probs = new double[patternCount];
		patternweight = data.getWeights();
	};

	@Override
	public void init(PrintStream out) throws Exception {
		tree.init(out);
	}

	@Override
	public void log(int nSample, PrintStream out) {
        out.print("tree STATE_" + nSample + " = ");
		out.append(toNewick(tree.getRoot()));
        out.print(";");
	}

	String toNewick(Node node) {
		StringBuffer buf = new StringBuffer();
		if (node.getLeft() != null) {
			buf.append("(");
			buf.append(toNewick(node.getLeft()));
			if (node.getRight() != null) {
				buf.append(',');
				buf.append(toNewick(node.getRight()));
			}
			buf.append(")");
		} else {
			buf.append(node.getNr() + 1);
		}
		buf.append("[&partialLogL=");
		buf.append(partialLogL(node));
		buf.append(']');
		buf.append(":").append(node.getLength());
		return buf.toString();
	}

	private double partialLogL(Node node) {
		if (node.isLeaf()) {
			return 0;
		}
		double [] freqs = substModel.getFrequencies();
		if (substModel instanceof AARSSubstitutionModel) {
			AARSSubstitutionModel AARSmodel = (AARSSubstitutionModel) substModel;
			freqs = AARSmodel.getFrequencies(node);
		}
		
		likelihood.m_likelihoodCore.getNodePartials(node.getNr(), partials);

		// TODO: implement for categoryCount > 1
		int k = 0;
		for (int i = 0; i < patternCount; i++) {
			double p = 0;
			for (int j = 0; j < stateCount; j++) {
				p += freqs[j] * partials[k++];
			}
			probs[i] = p;
		}
		
		double logP = 0;
		for (int i = 0; i < patternCount; i++) {
			logP += Math.log(probs[i]) * patternweight[i];
		}		
		return logP;
	}

	@Override
	public void close(PrintStream out) {
		tree.close(out);
	}
}
