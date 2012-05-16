/*
 * Copyright (C) 2012 Tim Vaughan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package beast.evolution.tree;

import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;
import beast.core.Plugin;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeTraceAnalysis;
import java.util.ArrayList;
import java.util.List;

/**
 * Modified logger which analyses a sequence of tree states generated
 * by an MCMC run.
 *
 * @author Tim Vaughan
 */
@Description("Modified logger which analyses a list of tree states generated"
		+ "by an MCMC run.")
public class TreeReport extends Logger {

	public Input<Integer> burninInput = new Input<Integer>("burnin",
			"Number of samples to skip (burn in)");

	public Input<Double> credibleSetProbInput = new Input<Double>(
			"credibleSetProb",
			"Probability cutoff defining credible set of tree topologies.",
			0.95
			);

	public Input<Boolean> silentInput = new Input<Boolean>("silent",
			"Don't display final report.", false);

	Tree treeToTrack;
	List<Tree> treeTrace;

	int m_nEvery = 1;
	int burnin = 0;
	double credibleSetProb = 0.95;
	boolean silent = false;

	TreeTraceAnalysis traceAnalysis;

	@Override
	public void initAndValidate() throws Exception {

		List<Plugin> loggers = m_pLoggers.get();
        final int nLoggers = loggers.size();
        if (nLoggers == 0) {
            throw new Exception("Logger with nothing to log specified");
        }

		if (m_pEvery.get() != null)
			m_nEvery = m_pEvery.get();

		if (burninInput.get() != null)
			burnin = burninInput.get();

		if (credibleSetProbInput.get() != null)
			credibleSetProb = credibleSetProbInput.get();

		if (silentInput.get() != null)
			silent = silentInput.get();

		treeToTrack = (Tree)loggers.get(0);
		treeTrace = new ArrayList<Tree>();

	}

	@Override
	public void init() throws Exception { }

	@Override
	public void log(int nSample) {

		if ((nSample < 0) || (nSample % m_nEvery > 0))
            return;

		treeTrace.add(treeToTrack.copy());
	}

	@Override
	public void close() {

		traceAnalysis = new TreeTraceAnalysis(treeTrace,
				burnin, credibleSetProb);

		if (!silent) {
			System.out.println("\n----- Tree trace analysis -----------------------");
			traceAnalysis.report(System.out);
			System.out.println("-------------------------------------------------");
			System.out.println();
		}
	}

	/**
	 * Obtain completed analysis.
	 * 
	 * @return tree trace analysis.
	 */
	public TreeTraceAnalysis getAnalysis() {
		return traceAnalysis;
	}

}