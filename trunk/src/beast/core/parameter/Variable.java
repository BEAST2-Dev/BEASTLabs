/*
* File Variable.java
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
package beast.core.parameter;

import beast.core.*;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


@Description("A variable represents a value in the state space that can be changed " +
        "by operators.")
public class Variable extends StateNode {
    public Input<java.lang.Integer> m_nDimension =
            new Input<java.lang.Integer>("dimension", "dimension of the variable (default 1)", 1);

    /**
     * constructors *
     */
    public Variable() {
    }

    @Override
    public void initAndValidate() throws Exception {
        m_bIsDirty = new boolean[m_nDimension.get()];
    }


    /**
     * upper & lower bound *
     */
    protected double m_fUpper;
    protected double m_fLower;

    /**
     * the actual values of this parameter, implemented as array of double
     */
    protected double [] m_fValues;
    
    /**
     * isDirty flags for individual elements in high dimensional parameters
     */
    protected boolean[] m_bIsDirty;

    /** check whether the iParam-th element has changed **/
    public boolean isDirty(int iParam) {
        return m_bIsDirty[iParam];
    }

    @Override
    public void setEverythingDirty(final boolean isDirty) {
    	setSomethingIsDirty(isDirty);
    	Arrays.fill(m_bIsDirty, isDirty);
	}

    /*
     * various setters & getters *
     */
    @Override public double getArrayValue() {return (double) m_fValues[0];}
    @Override public double getArrayValue(int iValue) {return (double) m_fValues[iValue];};
    public double getLower() {
        return m_fLower;
    }

    public void setLower(double fLower) {
        m_fLower = fLower;
    }
    
    public double getUpper() {
        return m_fUpper;
    }

    public void setUpper(double fUpper) {
        m_fUpper = fUpper;
    }

    public int getDimension() {
        return m_fValues.length;
    }

    public void setBounds(double fLower, double fUpper) {
        m_fLower = fLower;
        m_fUpper = fUpper;
    }

    public double getValue() {
        return m_fValues[0];
    }
    public int getIntValue() {
        return (int) m_fValues[0];
    }
    public boolean getBooltValue() {
        return (m_fValues[0] != 0.0);
    }

    public double getValue(int iParam) {
        return m_fValues[iParam];
    }
    public int getIntValue(int iParam) {
        return (int) m_fValues[iParam];
    }
    public boolean getBoolValue(int iParam) {
        return m_fValues[iParam] != 0.0;
    }

    public double[] getValues() {
        return Arrays.copyOf(m_fValues, m_fValues.length);
    }

    public void setValue(double fValue) {
        m_fValues[0] = fValue;
        m_bIsDirty[0] = true;
    }
    public void setIntValue(int nValue) {
        m_fValues[0] = nValue;
        m_bIsDirty[0] = true;
    }
    public void setBoolValue(Boolean bValue) {
        m_fValues[0] = (bValue ? 1 : 0);
        m_bIsDirty[0] = true;
    }

    public void setValue(int iParam, double fValue) {
        m_fValues[iParam] = fValue;
        m_bIsDirty[iParam] = true;
    }
    public void setIntValue(int iParam, int nValue) {
        m_fValues[iParam] = nValue;
        m_bIsDirty[iParam] = true;
    }
    public void setBoolValue(int iParam, boolean bValue) {
        m_fValues[iParam] = (bValue ? 1 : 0);
        m_bIsDirty[iParam] = true;
    }

	/** StateNode override methods follow **/
    @Override
	public int scale(double fScale) throws Exception {
    	for (int i = 0; i < m_fValues.length; i++) {
    		m_fValues[i] *= fScale;
    		if (m_fValues[i] < m_fLower || m_fValues[i] > m_fUpper) {
    			throw new Exception("parameter scaled our of range");
    		}
    	}
		return m_fValues.length;
	}

    /** Note that changing toString means fromXML needs to be changed as well,
     * since it parses the output of toString back into a parameter.
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append(m_sID + "[" +  m_fValues.length +"] ");
        buf.append("(" + m_fLower + "," + m_fUpper + "): ");
        for(double value : m_fValues) {
            buf.append(value).append(" ");
        }
        return buf.toString();
    }
    
    @Override
    public void fromXML(Node node) {
    	NamedNodeMap atts = node.getAttributes();
    	setID(atts.getNamedItem("id").getNodeValue());
    	String sStr = node.getTextContent();
    	Pattern pattern = Pattern.compile(".*\\[(.*)\\].*\\((.*),(.*)\\): (.*) ");
		Matcher matcher = pattern.matcher(sStr);
		matcher.matches();
		String sDimension = matcher.group(1);
		String sLower = matcher.group(2);
		String sUpper = matcher.group(3);
		String sValuesAsString = matcher.group(4);
    	String [] sValues = sValuesAsString.split(" ");
    	setLower(Double.parseDouble(sLower));
    	setUpper(Double.parseDouble(sUpper));
    	m_fValues = new double[Integer.parseInt(sDimension)];
    	for (int i = 0; i < sValues.length; i++) {
    		m_fValues[i] = Double.parseDouble(sValues[i]);
    	}
    }

    @Override
    public Variable copy() {
    	try {
			Variable copy = (Variable) this.clone();
	        copy.m_fValues = m_fValues.clone();
	        copy.m_bIsDirty = new boolean[m_fValues.length];
	        return copy;
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	return null;
    }

    @Override
    public void assignTo(StateNode other) {
        Variable copy = (Variable) other;
        copy.setID(getID());
        copy.index = index;
        copy.m_fValues = m_fValues.clone();
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        copy.m_bIsDirty = new boolean[m_fValues.length];
    }

    @Override
    public void assignFrom(StateNode other) {
        Variable source = (Variable) other;
        setID(source.getID());
        m_fValues = source.m_fValues.clone();
        m_fLower = source.m_fLower;
        m_fUpper = source.m_fUpper;
        m_bIsDirty = new boolean[source.m_fValues.length];
    }

    @Override
    public void assignFromFragile(StateNode other) {
        Variable source = (Variable) other;
        System.arraycopy(source.m_fValues, 0, m_fValues, 0, m_fValues.length);
        Arrays.fill(m_bIsDirty, false);
    }

    /**
     * Loggable interface implementation follows 
     */
    @Override
    public void init(PrintStream out) throws Exception {
        final int nValues = getDimension();
        if (nValues == 1) {
            out.print(getID() + "\t");
        } else {
            for (int iValue = 0; iValue < nValues; iValue++) {
                out.print(getID() + iValue + "\t");
            }
        }
    }

	@Override
	public void log(int nSample, PrintStream out) {
        RealParameter var = (RealParameter) getCurrent();
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
	}

	@Override
    public void close(PrintStream out) {
        // nothing to do
    }

} // class Parameter
