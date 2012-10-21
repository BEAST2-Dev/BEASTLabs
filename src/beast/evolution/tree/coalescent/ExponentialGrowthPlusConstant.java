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
import beast.core.parameter.RealParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Population model of the form A*exp(-B*t) + C")
public class ExponentialGrowthPlusConstant extends PopulationFunction.Abstract {
    
    public Input<RealParameter> AInput = new Input<RealParameter>("A",
            "Parameter A.");
    public Input<RealParameter> BInput = new Input<RealParameter>("B",
            "Parameter B.");
    public Input<RealParameter> CInput = new Input<RealParameter>("C",
            "Parameter C.");

    @Override
    public List<String> getParameterIds() {
        List<String> idList = new ArrayList<String>();
        idList.add(AInput.get().getID());
        idList.add(BInput.get().getID());
        idList.add(CInput.get().getID());
        
        return Collections.unmodifiableList(idList);
    }

    @Override
    public double getPopSize(double t) {
        
        double A = AInput.get().getValue();
        double B = BInput.get().getValue();
        double C = CInput.get().getValue();
        
        if (B==0) {
            return A + C;
        } else {
            return A*Math.exp(-t*B) + C;
        }
    }

    @Override
    public double getIntensity(double t) {

        double A = AInput.get().getValue();
        double B = BInput.get().getValue();
        double C = CInput.get().getValue();
        
        if (B==0) {
            return t/(A + C);
        } else {
            return t/C + (Math.log(Math.exp(-B*t)+C/A) - Math.log(1+C/A))/B/C;
        }
    }
    
    @Override
    public double getIntegral(double start, double finish) {
        
        double A = AInput.get().getValue();
        double B = BInput.get().getValue();
        double C = CInput.get().getValue();
        
        if (B==0) {
            return (finish-start)/(A + C);
        } else {
            return (finish-start)/C + (Math.log(Math.exp(-B*finish)+C/A)-Math.log(Math.exp(-B*start)+C/A))/B/C;
        }
    }

    @Override
    public double getInverseIntensity(double x) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
