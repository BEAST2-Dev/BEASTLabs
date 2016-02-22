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

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;

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

	public Input<Double> burninPercentageInput = new Input<Double>("burninPercentage",
			"PERCENTAGE of samples to skip (burn in)");

	public Input<Double> credibleSetPercentageInput = new Input<Double>(
			"credibleSetPercentage",
			"Probability cutoff defining credible set of tree topologies.",
			95.0
			);

	public Input<Boolean> silentInput = new Input<Boolean>("silent",
			"Don't display final report.", false);

	Tree treeToTrack;
	List<Tree> treeTrace;

	int m_nEvery = 1;
	double burninPercentage = 10.0;
	double credibleSetPercentage = 95.0;
	boolean silent = false;

	TreeTraceAnalysis traceAnalysis;

	@Override
	public void initAndValidate() {

		List<BEASTObject> loggers = loggersInput.get();
        final int nLoggers = loggers.size();
        if (nLoggers == 0) {
            throw new IllegalArgumentException("Logger with nothing to log specified");
        }

		if (everyInput.get() != null)
			m_nEvery = everyInput.get();

		if (burninPercentageInput.get() != null)
			burninPercentage = burninPercentageInput.get();

		if (credibleSetPercentageInput.get() != null)
			credibleSetPercentage = credibleSetPercentageInput.get();

		if (silentInput.get() != null)
			silent = silentInput.get();

		treeToTrack = (Tree)loggers.get(0);
		treeTrace = new ArrayList<Tree>();

	}

	@Override
	public void init() { }

	@Override
	public void log(int nSample) {

		if ((nSample < 0) || (nSample % m_nEvery > 0))
            return;

		treeTrace.add(treeToTrack.copy());
	}

	@Override
	public void close() {

		traceAnalysis = new TreeTraceAnalysis(treeTrace, burninPercentage/100);
        traceAnalysis.analyze(credibleSetPercentage);

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
