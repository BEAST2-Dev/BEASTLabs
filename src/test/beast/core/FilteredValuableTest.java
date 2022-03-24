package test.beast.core;


import org.junit.Test;

import beast.core.FilteredValuable;
import beast.base.inference.parameter.RealParameter;



import junit.framework.TestCase;

public class FilteredValuableTest  extends TestCase {
	
    @Test
    public void testFilteredParameter() throws Exception {
    	RealParameter parameter = new RealParameter("0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0 11.0 12.0 13.0 14.0");
    	
    	FilteredValuable oneButLast = new FilteredValuable();
    	oneButLast.parameterInput.setValue(parameter, oneButLast);
    	oneButLast.rangeInput.setValue("-2::2", oneButLast);
    	oneButLast.initAndValidate();
    	//oneButLast.initByName("parameter", parameter, "range", "-2:-2:");
    	assertEquals(1, oneButLast.getDimension());
    	assertEquals(13, oneButLast.getArrayValue(), 1e-100);

    	FilteredValuable last = new FilteredValuable();
    	last.initByName("parameter", parameter, "range", "-1::");
    	assertEquals(1, last.getDimension());
    	assertEquals(14, last.getArrayValue(), 1e-100);

    	
    	FilteredValuable allOdd = new FilteredValuable();
    	allOdd.initByName("parameter", parameter, "range", "2::2");
    	assertEquals(7, allOdd.getDimension());
    	for (int i = 0; i < allOdd.getDimension(); i++) {
    		assertEquals(i * 2 + 1, allOdd.getArrayValue(i), 1e-100);
    	}

    	FilteredValuable allEven = new FilteredValuable();
    	allEven.initByName("parameter", parameter, "range", "1::2");
    	assertEquals(8, allEven.getDimension());
    	for (int i = 0; i < allEven.getDimension(); i++) {
    		assertEquals(i * 2, allEven.getArrayValue(i), 1e-100);
    	}
    	
    }

}
