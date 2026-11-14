package test.beast.util;

import beast.base.core.BEASTInterface;
import beast.base.inference.CalculationNode;
import beast.base.inference.parameter.RealParameter;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.pkgmgmt.BEASTClassLoader;
import beastlabs.util.Script;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptTest {

    @Test
    public void testScriptServiceDiscovery() throws Exception {
        Set<String> beastObjects = BEASTClassLoader.loadService(BEASTInterface.class);
        assertTrue(beastObjects.contains(Script.class.getName()), "Script should be provided as BEASTInterface service");
        assertNotNull(BEASTClassLoader.forName(Script.class.getName()), "Script should be loadable");
    }

    /** Script needs a JavaScript engine: nashorn and its ASM dependencies on the module path */
    @Test
    public void testScriptEngineDiscovery() {
        RealScalarParam<Real> x = new RealScalarParam<>();
        x.initByName("value", "4.0");
        x.setID("x");

        Script script = new Script();
        script.initByName("expression", "1 / x", "x", x);
        assertEquals(0.25, script.getArrayValue(), 1e-12);
    }

    @Test
    public void testScalarInput() {
        RealScalarParam<PositiveReal> kappa = new RealScalarParam<>();
        kappa.initByName("value", "2.0", "domain", PositiveReal.INSTANCE);
        kappa.setID("kappa");

        Script script = new Script();
        script.initByName("expression", "1 / kappa", "x", kappa);
        assertEquals(0.5, script.getArrayValue(), 1e-12);
    }

    @Test
    public void testVectorInput() {
        RealVectorParam<Real> a = new RealVectorParam<>();
        a.initByName("value", "1.0 3.0");
        a.setID("a");

        Script script = new Script();
        script.initByName("value", "function f(a) {return [a[0] + a[1], a[0] * a[1]];}", "x", a);
        // getDimension() must evaluate f to know the dimension, before any value is read
        assertEquals(2, script.getDimension());
        assertEquals(4.0, script.getArrayValue(0), 1e-12);
        assertEquals(3.0, script.getArrayValue(1), 1e-12);
    }

    /** the log header must have one column per element of an array result */
    @Test
    public void testLoggerColumns() {
        RealVectorParam<Real> a = new RealVectorParam<>();
        a.initByName("value", "1.0 3.0");
        a.setID("a");

        Script script = new Script();
        script.initByName("value", "function f(a) {return [a[0] + a[1], a[0] * a[1]];}", "x", a);
        script.setID("s");

        ByteArrayOutputStream header = new ByteArrayOutputStream();
        script.init(new PrintStream(header));
        assertEquals("s_0\ts_1\t", header.toString());

        ByteArrayOutputStream row = new ByteArrayOutputStream();
        script.log(0, new PrintStream(row));
        assertEquals("4.0\t3.0\t", row.toString());
    }

    /** old Function and BEAST 3 Scalar/Vector inputs in one script, passed to f in XML order */
    @Test
    public void testMixedInputs() {
        RealParameter b = new RealParameter("4.0");
        b.setID("b");
        RealVectorParam<Real> a = new RealVectorParam<>();
        a.initByName("value", "1.0 3.0");
        a.setID("a");
        RealScalarParam<Real> c = new RealScalarParam<>();
        c.initByName("value", "0.5");
        c.setID("c");

        Script script = new Script();
        script.initByName("expression", "3 * sin(a[0]) + log(a[1]) * b + c", "x", a, "x", b, "x", c);
        assertEquals(3 * Math.sin(1.0) + Math.log(3.0) * 4.0 + 0.5, script.getArrayValue(), 1e-12);
    }

    @Test
    public void testUnsupportedInput() {
        CalculationNode node = new CalculationNode() {
            @Override
            public void initAndValidate() {
            }
        };
        node.setID("node");

        Script script = new Script();
        // initByName wraps the IllegalArgumentException from initAndValidate
        RuntimeException e = assertThrows(RuntimeException.class, () -> script.initByName("expression", "node", "x", node));
        assertTrue(e.getMessage().contains("is not supported"), e.getMessage());
    }
}
