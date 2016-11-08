package test.beast.evolution.operators;


import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import beast.app.beauti.BeautiDoc;
import beast.core.Operator;
import beast.core.OperatorSchedule;
import beast.core.State;
import beast.core.StateNode;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.CompoundOperator;
import beast.evolution.operators.DeltaExchangeOperator;
import beast.evolution.operators.ScaleOperator;
import junit.framework.TestCase;

public class CompoundOperatorTest extends TestCase {
	
	@Test
	public void testCompoundOperator() {
		DeltaExchangeOperator operator1 = new DeltaExchangeOperator();
		operator1.setID("deltaOperator");
		RealParameter parameter = new RealParameter(new Double[] {1., 1., 1., 1.});
		operator1.initByName("parameter", parameter, "weight", 1.0);

		ScaleOperator operator2 = new ScaleOperator(); 
		operator2.setID("scaleOperator");
		operator2.initByName("parameter", parameter, "weight", 3.0);

		CompoundOperator co = new CompoundOperator();
		co.setID("compoundOperator");
		co.initByName("operator", operator1, "operator", operator2, "weight", 1.0);

		State state = new State();
		state.initByName("stateNode", parameter);
		state.initialise();
		

		OperatorSchedule schedule = new OperatorSchedule();
		schedule.addOperator(co);
		schedule.initAndValidate();
		
		Operator o = schedule.selectOperator();
		assertEquals(o.getClass(), CompoundOperator.class);
		
		// test proposal, accept, optimize
		int scaleOperatorCount0 = 0;
		for (int i = 0; i < 1000; i++) {
			o.proposal();
			if (((CompoundOperator)o).lastOperator instanceof ScaleOperator) {
				scaleOperatorCount0++;
			}
			o.accept();
			o.optimize(1);
		}
		assertTrue(scaleOperatorCount0 > 700 && scaleOperatorCount0 < 800);
		
		// test store & restore
		try {
			String stateFile = "/tmp/x.state";
			if ((new File(stateFile)).exists()) {
				(new File(stateFile)).delete();
			}
			state.setStateFileName(stateFile);
			state.storeToFile(123);
			schedule.setStateFileName(stateFile);
			schedule.storeToFile();
			schedule.restoreFromFile();

			String s = BeautiDoc.load(stateFile);
			matches(s,"\"accept\":" + scaleOperatorCount0 + ",\"reject\":" + 0 + ",");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// test proposal, reject, optimize
		int scaleOperatorCount = 0;
		for (int i = 0; i < 1000; i++) {
			o.proposal();
			if (((CompoundOperator)o).lastOperator instanceof ScaleOperator) {
				scaleOperatorCount++;
			}
			o.reject();
			o.optimize(1);
		}
		assertTrue(scaleOperatorCount > 700 && scaleOperatorCount < 800);

		// test store & restore
		try {
			String stateFile = "/tmp/x.state2";
			if ((new File(stateFile)).exists()) {
				(new File(stateFile)).delete();
			}
			state.setStateFileName(stateFile);
			state.storeToFile(123);
			schedule.setStateFileName(stateFile);
			schedule.storeToFile();			
			String s = BeautiDoc.load(stateFile);
			matches(s,"\"accept\":" + scaleOperatorCount0 + ",\"reject\":" + scaleOperatorCount + ",");
			matches(s,"\"accept\":" + (1000-scaleOperatorCount0) + ",\"reject\":" + (1000 -scaleOperatorCount) + ",");
			
			stateFile = "/tmp/x.state";
			schedule.setStateFileName(stateFile);
			schedule.restoreFromFile();
			s = BeautiDoc.load(stateFile);
			matches(s,"\"accept\":" + scaleOperatorCount0 + ",\"reject\":" + 0 + ",");
			matches(s,"\"accept\":" + (1000-scaleOperatorCount0) + ",\"reject\":" + 0 + ",");
			

			stateFile = "/tmp/x.state2";
			if ((new File(stateFile)).exists()) {
				(new File(stateFile)).delete();
			}
			state.setStateFileName(stateFile);
			state.storeToFile(123);
			schedule.setStateFileName(stateFile);
			schedule.storeToFile();
			s = BeautiDoc.load(stateFile);
			matches(s,"\"accept\":" + scaleOperatorCount0 + ",\"reject\":" + 0 + ",");
			matches(s,"\"accept\":" + (1000-scaleOperatorCount0) + ",\"reject\":" + 0 + ",");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// test listStateNodes
		List<StateNode> list = co.listStateNodes();
		assertEquals(2,list.size());
		assertEquals(parameter, list.get(0));
		assertEquals(parameter, list.get(1));
		
	}

	private void matches(String s, String match) {
		int i = s.indexOf(match);
		assertTrue(i > 0);
	}

}
