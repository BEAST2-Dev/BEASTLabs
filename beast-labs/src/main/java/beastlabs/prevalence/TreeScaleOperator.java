/*
* File ScaleOperator.java
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
package beastlabs.prevalence;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;

@Description("Scales a complete beast.tree.")
public class TreeScaleOperator extends TreeOperator {

    public Input<Double> m_pScaleFactor = new Input<Double>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
    // shadows input
    double m_fScaleFactor;
    public Input<Boolean> m_pScaleAll =
            new Input<Boolean>("scaleAll", "if true, all elements of a parameter (not beast.tree) are scaled, otherwise one is randomly selected",
                    false);
    public Input<Boolean> m_pScaleAllIndependently =
            new Input<Boolean>("scaleAllIndependently", "if true, all elements of a parameter (not beast.tree) are scaled with " +
                    "a different factor, otherwise a single factor is used", false);

    public Input<Integer> m_pDegreesOfFreedom = new Input<Integer>("degreesOfFreedom", "Degrees of freedom used in ...", 1);

    
    @Override
    public void initAndValidate() {
        m_fScaleFactor = m_pScaleFactor.get();
    }


    /** override this for proposals,
	 * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted **/
    @Override
    public double proposal() {
    	try {
	        double d = Randomizer.nextDouble();
	        double scale = (m_fScaleFactor + (d * ((1.0 / m_fScaleFactor) - m_fScaleFactor)));
	        
	    	Tree tree = m_tree.get(); 
	        // scale the beast.tree
	    	int nInternalNodes = tree.scale(scale);

	    	PrevalenceList list = m_list.get();
	    	// scale only internal nodes in the list
	    	// nNodesScaled is the number of nodes that have been scaled
	    	int nNodesScaled = list.scale(scale);
	        // TODO: check the HastingsRatio, since the prevalence list is assumed to be unchanged here
	        return Math.log(scale) * (nInternalNodes - 2);
    	} catch (Exception e) {
			return Double.NEGATIVE_INFINITY;
		}
    }


    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(double logAlpha) {
        double fDelta = calcDelta(logAlpha);
        fDelta += Math.log(1.0 / m_fScaleFactor - 1.0);
        m_fScaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
    }

} // class ScaleOperator
