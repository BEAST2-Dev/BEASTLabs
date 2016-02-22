/*
* File HKY.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.evolution.substitutionmodel;


import java.util.Arrays;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.substitutionmodel.HKY;
import beast.evolution.tree.Node;



@Description("Lazy version of HKY85 substitution model of nucleotide evolution.")
public final class LazyHKY extends HKY {
	enum RelaxationMode {
        exponential, gamma, inverse_gamma
    };
    public Input<RelaxationMode> m_modeInput = new Input<RelaxationMode>("mode", "form of the  prior distribution used for relaxation " +
            "This can be " + Arrays.toString(RelaxationMode.values()) + " (default 'exponential')", RelaxationMode.exponential, RelaxationMode.values());
    public Input<RealParameter> m_theta = new Input<RealParameter>("theta", "shape parameter, ignored with exponential prior");

    
    // shadows the input
    RelaxationMode m_relaxationMode = RelaxationMode.exponential;

	
	@Override
    public void initAndValidate() {
		m_relaxationMode = m_modeInput.get();
		if (m_relaxationMode != RelaxationMode.exponential && m_theta.get() == null) {
			throw new IllegalArgumentException("theta parameter should be specified if mode is " + m_relaxationMode);
		}
		super.initAndValidate();
    } // initAndValidate

    
    @Override
    public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
      	double distance = (fStartTime - fEndTime) * fRate;

        if (updateMatrix) {
            setupMatrix();
        }

        final double xx = beta * distance;
        
        
        double bbR = Math.exp(xx * A_R);
        double bbY = Math.exp(xx * A_Y);
        double aa = Math.exp(xx);

        double fTheta = (m_theta.get() == null ? 1.0 : m_theta.get().getValue());

    	switch (m_relaxationMode) {
    	case exponential:
            bbR = 1.0/(-xx * A_R + 1.0);
            bbY = 1.0/(-xx * A_Y + 1.0);
            aa =  1.0/(-xx + 1.0);
    		break;
    	case gamma:
            bbR = 1.0/Math.pow(-xx * A_R + 1.0, 1.0/fTheta);
            bbY = 1.0/Math.pow(-xx * A_Y + 1.0, 1.0/fTheta);
            aa =  1.0/Math.pow(-xx + 1.0, 1.0/fTheta);
    		break;
    	case inverse_gamma:
//    		temp = 2.0 * Math.pow(distance * fTheta * Eval[i], (fTheta+1.0)/2.0);
//    		temp *= BesselK(fTheta + 1.0, 2.0 * Math.sqrt(distance * fTheta * Eval[i]), 1);
//    		temp *= Math.exp(GammaFunction.lnGamma(fTheta + 1.0));
    		break;
    	}

        
        final double oneminusa = 1 - aa;

        final double t1Aaa = (tab1A * aa);
        matrix[0] = freqA + t1Aaa + (tab2A * bbR);

        matrix[1] = freqC * oneminusa;
        final double t1Gaa = (tab1G * aa);
        matrix[2] = freqG + t1Gaa - (tab3G * bbR);
        matrix[3] = freqT * oneminusa;

        matrix[4] = freqA * oneminusa;
        final double t1Caa = (tab1C * aa);
        matrix[5] = freqC + t1Caa + (tab2C * bbY);
        matrix[6] = freqG * oneminusa;
        final double t1Taa = (tab1T * aa);
        matrix[7] = freqT + t1Taa - (tab3T * bbY);

        matrix[8] = freqA + t1Aaa - (tab3A * bbR);
        matrix[9] = matrix[1];
        matrix[10] = freqG + t1Gaa + (tab2G * bbR);
        matrix[11] = matrix[3];

        matrix[12] = matrix[4];
        matrix[13] = freqC + t1Caa - (tab3C * bbY);
        matrix[14] = matrix[6];
        matrix[15] = freqT + t1Taa + (tab2T * bbY);
    } // getTransitionProbabilities

}