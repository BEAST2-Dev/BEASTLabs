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
package beast.math.distributions;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;

/**
 * @author Denise Kuehnert, based on Tim Vaughan's ExcludablePrior
 */
@Description("Just as with Prior, produces log probability of the parameter x. "
        + "This variant however allows one to explicitly exclude individual "
        + "elements of multidimensional parameters from the result using indexes that can be set to true, rather than requiring a TRUE/FALSe entry for each parameter index.")
public class ExcludablePriorIndex extends Prior {

    public Input<BooleanParameter> xIncludeInput = new Input<BooleanParameter>(
            "xInclude", "Array of true/false values specifying which elements"
            + " of x to include");

    public Input<IntegerParameter> xIncludeIntegerInput = new Input<IntegerParameter>(
            "xIncludeInteger", "List of array indices to include "
            + " of x to include", Validate.XOR, xIncludeInput);


    IntegerParameter xIncludeInteger;

    @Override
    public void initAndValidate() {
        super.initAndValidate();

        Function x = m_x.get();
        if (x instanceof RealParameter || x instanceof IntegerParameter) {
            if (xIncludeInput.get()!=null){
                if (x.getDimension() != xIncludeInput.get().getDimension())
                    throw new IllegalArgumentException("Length of xInclude does "
                            + "not match length of x.");
            }
            else {
                xIncludeInteger = xIncludeIntegerInput.get();
                if (x.getDimension() <= xIncludeInteger.getValue(xIncludeInteger.getDimension()-1)) // assuming xIncludeInteger is sorted!! todo: check!
                    throw new IllegalArgumentException("xIncludeInteger should be sorted and the highest value has to be lower than the parameter length");


            }
        }
    }

    @Override
    public double calculateLogP() {
        Function x = m_x.get();
        if (x instanceof RealParameter || x instanceof IntegerParameter) {
            // test that parameter is inside its bounds
            double l = 0.0;
            double h = 0.0;
            if (x instanceof RealParameter) {
                l = ((RealParameter) x).getLower();
                h = ((RealParameter) x).getUpper();
            } else {
                l = ((IntegerParameter) x).getLower();
                h = ((IntegerParameter) x).getUpper();
            }
            if (xIncludeInput.get()!=null){
                for (int i = 0; i < x.getDimension(); i++) {
                    if (!xIncludeInput.get().getValue(i))
                        continue;
                    double value = x.getArrayValue(i);
                    if (value < l || value > h) {
                        return Double.NEGATIVE_INFINITY;
                    }
                }
            }
            else {
                xIncludeInteger = xIncludeIntegerInput.get();

                for (int i = 0; i < xIncludeInteger.getDimension(); i++) {
                    double value = x.getArrayValue(xIncludeInteger.getValue(i));
                    if (value < l || value > h) {
                        return Double.NEGATIVE_INFINITY;
                    }
                }

            }

            // Inline modified version of ParametricDistribution.calcLogP()
            final double fOffset = dist.offsetInput.get();
            logP = 0;
            if (xIncludeInput.get()!=null) {
                for (int i = 0; i < x.getDimension(); i++) {
                    if (!xIncludeInput.get().getValue(i))
                        continue;
                    final double fX = x.getArrayValue(i) - fOffset;
                    logP += dist.logDensity(fX);
                }
            }
            else {
                for (int i = 0; i < xIncludeInteger.getDimension(); i++) {
                    final double fX = x.getArrayValue(xIncludeInteger.getValue(i)) - fOffset;
                    logP += dist.logDensity(fX);
                }
            }
        }

        return logP;
    }

}
