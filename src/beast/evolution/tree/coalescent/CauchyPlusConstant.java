/*
 * Copyright (C) 2012 Tim Vaughan <tgvaughan@gmail.com>
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
package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Population model of the form A/(B*(t-t0)^2 + 1) + C.")
public class CauchyPlusConstant extends PopulationFunction.Abstract {
    
    public Input<RealParameter> AInput = new Input<RealParameter>(
            "A", "Magnitude of Cauchy distribution.", Validate.REQUIRED);
    public Input<RealParameter> BInput = new Input<RealParameter>(
            "B", "Width of Cauchy distribution.", Validate.REQUIRED);
    public Input<RealParameter> t0Input = new Input<RealParameter>(
            "t0", "Centre of Cauchy distribution.", Validate.REQUIRED);
    public Input<RealParameter> CInput = new Input<RealParameter>(
            "C", "Constant population offset value.", Validate.REQUIRED);

    /**
     * Required initAndValidate method.
     */
    @Override
    public void initAndValidate() { }
    
        /**
     * Retrieve list of population model parameter IDs.
     * 
     * @return List of IDs.
     */
    @Override
    public List<String> getParameterIds() {
        List<String> idList = new ArrayList<String>();
        idList.add(AInput.get().getID());
        idList.add(BInput.get().getID());
        idList.add(t0Input.get().getID());
        idList.add(CInput.get().getID());
        
        return Collections.unmodifiableList(idList);
    }
    
    /**
     * Retrieve population size at time t.
     * 
     * @param t
     * @return Population size.
     */
    @Override
    public double getPopSize(double t) {
        
        double A = AInput.get().getValue();
        double B = BInput.get().getValue();
        double t0 = t0Input.get().getValue();
        double C = CInput.get().getValue();

        return A/(B*Math.pow(t-t0, 2.0)+1.0) + C;
    }

    /**
     * Calculate and return result of integral \int_0^t 1/N(s)ds.
     * 
     * @param t
     * @return Result of integral.
     */
    @Override
    public double getIntensity(double t) {
        
        double A = AInput.get().getValue();
        double B = BInput.get().getValue();
        double t0 = t0Input.get().getValue();
        double C = CInput.get().getValue();
        
        if (B==0) {
            return (t-t0)/(A+C);
        }
        
        if (C==0) {
            return (t-B*Math.pow(t0-t,3)/3.0)/A
                    + B*Math.pow(t0,3)/3.0/A;
        }

        return -A*Math.atan(t0*Math.sqrt(B*C/(A + C)))/Math.sqrt(B*Math.pow(C,3)*(A + C))
                + A*Math.atan(Math.sqrt(B*C/(A + C))*(-t + t0))/Math.sqrt(B*Math.pow(C,3)*(A + C))
                + t0/C - (-t + t0)/C;
    }
    

    @Override
    public double getInverseIntensity(double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
