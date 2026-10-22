package beastlabs.util;




import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Vector;
import beast.base.parser.JSONProducer;
import beast.base.core.Log;
import beast.base.core.Loggable;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
//import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

@Description("Base class for Script-BEAST interoperation")
public class Script extends CalculationNode implements Loggable, beast.base.core.Function {
    public Input<String> scriptInput = new Input<String>("value", "Script script needed for the calculations. " +
    		"It assumes there is a function f defined, which returns a single number or array of numbers.");
    public Input<String> expressionInput = new Input<String>("expression", "expression representing the calculations", Validate.XOR, scriptInput);
    public Input<List<CalculationNode>> functionInput = new Input<>("x", "Parameters needed for the calculations: " +
            "a Tree, a Function (e.g. RealParameter), or a BEAST 3 Scalar or Vector (e.g. RealScalarParam, RealVectorParam)",
            new ArrayList<CalculationNode>());
    public Input<String> argNames = new Input<>("argnames", "names of arguments used in expression (space delimited)," +
            " order as given by XML");

    enum Engine {JavaScript, python, jruby, groovy};
    public Input<Engine> engineInput = new Input<Engine>("engine", "Script needed for the calculations (one of "+ Arrays.toString(Engine.values()) + " default Javascript)", Engine.JavaScript, Engine.values());

    ScriptEngine engine;
    Invocable inv;

    boolean isUpToDate = false;
    boolean isScript = true; // otherwise it is an expression
    double [] value;

    @Override public void initAndValidate() {
        for (CalculationNode x : functionInput.get()) {
            if (!(x instanceof Tree || x instanceof Function || x instanceof Scalar || x instanceof Vector)) {
                throw new IllegalArgumentException("Input x '" + x.getID() + "' of type " + x.getClass().getName() +
                        " is not supported: expected a Tree, Function, Scalar or Vector");
            }
        }

        // create a JavaScript engine
        engine = getScriptEngine(engineInput.get().name());
        if (engine == null) {
            throw new IllegalArgumentException("No " + engineInput.get() + " script engine found. " +
                    "BEASTLabs ships nashorn-core and ASM in its lib directory, but nashorn also needs the JDK module " +
                    "jdk.dynalink, which must be added to the BEAST launcher's --add-modules.");
        }
        value = new double[1];
        value[0] = Double.NaN;

        Object o = null;
        if (scriptInput.get() != null && scriptInput.get().trim().length() > 0) { 
	        try {
				o = engine.eval(scriptInput.get());
			} catch (ScriptException e) {
				throw new IllegalArgumentException(e);
			}
        } else {
        	StringBuilder f = new StringBuilder();
        	// create function with argument list
        	f.append("with (Math) { function f(");
            if( argNames.get() != null ) {
                String[] args = argNames.get().split("\\s+");
                f.append(args[0]);
                for (String a : Arrays.copyOfRange(args, 1, args.length) ) {
                    f.append(", ").append(a);
                }
            } else {
                for (CalculationNode x : functionInput.get()) {
                    f.append(x.getID());
                    f.append(", ");
                }

                if( functionInput.get().size() > 0 ) {
                    // eat up trailing comma
                    f.deleteCharAt(f.length() - 2);
                }
            }
        	f.append(") { return ");
        	f.append(expressionInput.get());
        	f.append(";}\n}");
        	Log.info.println(f);
        	try {
        		o = engine.eval(f.toString());
			} catch (ScriptException e) {
				throw new IllegalArgumentException(e);
			}

        }
//        if (o instanceof NativeArray) {
//            value = new double[((NativeArray)o).size()];
//            for (int i = 0; i < value.length; i++) {
//                value[i] = Double.NaN;
//            }
//        }
        //initEngine();
        inv = (Invocable) engine;
        
    }




    /**
     * Find a script engine by name. The default ScriptEngineManager searches the thread context
     * class loader, which cannot see engines in BEAST's package ModuleLayers, so search this
     * module's layer and its parent layers instead (BEAST may load the engine jars of this package
     * into a parent layer), falling back to this class's loader when not running as a named module.
     */
    private static ScriptEngine getScriptEngine(String name) {
        ModuleLayer layer = Script.class.getModule().getLayer();
        if (layer != null) {
            for (ScriptEngineFactory factory : ServiceLoader.load(layer, ScriptEngineFactory.class)) {
                if (factory.getNames().contains(name)) {
                    return factory.getScriptEngine();
                }
            }
        }
        return new ScriptEngineManager(Script.class.getClassLoader()).getEngineByName(name);
    }

    /** convert a BEAST 3 Scalar or Vector element (Double, Integer or Boolean) to a number for the script **/
    private static double toDouble(Object v) {
        if (v instanceof Boolean b) {
            return b ? 1.0 : 0.0;
        }
        return ((Number) v).doubleValue();
    }

    private void calc() {
        //Bindings bind = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        Object [] args = new Object[functionInput.get().size()];
        int k = 0;
        for (CalculationNode x : functionInput.get()) {
            //String name = var.nameInput.get();
            Object _value = null;
            //beast.base.core.Function f = var.functionInput.get();
            // Tree is also a Function, so test it first
            if (x instanceof Tree) {
                Tree tree = (Tree) x;
                JSONProducer p = new JSONProducer();
                String treeString = toJSON(tree.getRoot());
                _value = "{" + treeString + "}";
            } else if (x instanceof Function f) {
                if (f.getDimension() > 1) {
                    Double [] a = new Double[f.getDimension()];
                    for (int i = 0;i < f.getDimension(); i++) {
                        a[i] = f.getArrayValue(i);
                    }
                    _value = a;
                } else {
                    _value = f.getArrayValue();
                }
            } else if (x instanceof Scalar<?, ?> s) {
                // BEAST 3 scalar, e.g. RealScalarParam, IntScalarParam or BoolScalarParam
                _value = toDouble(s.get());
            } else if (x instanceof Vector<?, ?> v) {
                // BEAST 3 vector, e.g. RealVectorParam; a single value is passed as a number, as for a Function
                List<?> elements = v.getElements();
                if (elements.size() > 1) {
                    Double [] a = new Double[elements.size()];
                    for (int i = 0; i < a.length; i++) {
                        a[i] = toDouble(elements.get(i));
                    }
                    _value = a;
                } else {
                    _value = toDouble(elements.get(0));
                }
            }
            args[k++] = _value;
            //bind.put(name, _value);
        }

        
        try {
       		Object o = inv.invokeFunction("f", args);

            if (o instanceof ScriptObjectMirror) {
                value = new double[((ScriptObjectMirror)o).values().size()];
                for (int i = 0; i < value.length; i++) {
                    // getSlot for array index: standalone nashorn's get(Object) only accepts String keys
                    this.value[i] = Double.parseDouble(((ScriptObjectMirror)o).getSlot(i).toString());
                }
            } else {
                this.value[0] = Double.parseDouble(o.toString());
            }
        } catch (NoSuchMethodException | ScriptException e) {
            this.value[0] = Double.NaN;
            Log.err.println(e.getMessage());
        }
        isUpToDate = true;
    }

//    private void initEngine() {
//        StringBuilder buf = new StringBuilder();
//        for (Var var : functionInput.get()) {
//            buf.append(var.nameInput.get());
//            buf.append("=");
//            beast.base.core.Function f = var.functionInput.get();
//            if (f instanceof Tree) {
//                Tree tree = (Tree) f;
//                JSONProducer p = new JSONProducer();
//                String treeString = p.toJSON(tree);
//                buf.append(treeString);
//            } else if (f.getDimension() > 1) {
//                buf.append('[');
//                for (int i = 0;i < f.getDimension()- 1; i++) {
//                    buf.append(f.getArrayValue(i));
//                    buf.append(',');
//                }
//                buf.append(f.getArrayValue(f.getDimension()- 1));
//                buf.append(']');
//            } else {
//                buf.append(f.getArrayValue());
//            }
//            if (engineInput.get() == Engine.JavaScript) {
//                buf.append(';');
//            }
//            buf.append('\n');
//        }
//        buf.append(scriptInput.get());
//        String formula = buf.toString();
//
//        try {
//            Object o = engine.eval(formula);
////            o = engine.get("a");
////            NativeArray as = (NativeArray) o;
////            System.out.println(as);
////            for (int i = 0; i < as.size(); i++) {
////                o = as.get(i);
////            }
//            //value = Double.parseDouble(o.toString());
//            value = Double.NaN;
//        } catch (javax.script.ScriptException es) {
//            Log.err.println(es.getMessage());
//            value = Double.NaN;
//        }
////        isUpToDate = true;
//    }

    private String toJSON(Node node) {
        StringBuilder bf = new StringBuilder();
        bf.append("height: " + node.getHeight());
        bf.append(",nr:" + node.getNr());
        if (!node.isLeaf()) {
            bf.append(",children:{");
            for (int i = 0; i < node.getChildCount(); i++) {
                bf.append(toJSON(node.getChild(i)));
                if (i < node.getChildCount() - 1) {
                    bf.append(",");
                }
            }
            bf.append("}");
        } else {
            //bf.append(",id:" + node.getID());
        }
        return bf.toString();
    }




    @Override public double getArrayValue() {
        if (!isUpToDate) {
            calc();
        }
        return value[0];
    }

    @Override public double getArrayValue(int iDim) {
        if (!isUpToDate) {
            calc();
        }
        return value[iDim];
    }

    @Override public int getDimension() {
        // the dimension is only known after f is evaluated, since f may return an array
        if (!isUpToDate) {
            calc();
        }
        return value.length;
    }


    @Override protected void store() {
        isUpToDate = false;
        super.store();
    }

    @Override protected void restore() {
        isUpToDate = false;
        super.restore();
    }

    @Override protected boolean requiresRecalculation() {
        // we only get here if at least one input has changed, so always recalculate
        isUpToDate = false;
        return true;
    }


    public static void main(String[] args) throws Exception {
      RealVectorParam<Real> a = new RealVectorParam<>();
      a.initByName("value", "1.0 3.0");
      a.setID("a");
      RealVectorParam<Real> b = new RealVectorParam<>();
      b.initByName("value", "4.0");
      b.setID("b");
      Script jsBEAST = new Script();
//      jsBEAST.initByName("expression", "3 * sin(a[0]) + log(a[1]) * b", "x", a, "x", b);
      jsBEAST.initByName("expression", "a * b", "x", a, "x", b);
      System.out.println(jsBEAST.expressionInput.get() + "  = " + jsBEAST.getArrayValue());

      jsBEAST.requiresRecalculation();
      a.initByName("value", "3.0 4.0");
      b.initByName("value", "-4.0");
      System.out.println(jsBEAST.expressionInput.get() + "  = " + jsBEAST.getArrayValue());
      
      
      jsBEAST = new Script();
      a = new RealVectorParam<>();
      a.initByName("value", "1.0");
      jsBEAST.initByName("value", 
    		  "function fac(x) {\n" +
    		  "		if (x <= 1) {return 1;}\n\n" +
    		  "		return x * fac(x-1);\n"+
    		  "}\n"+
    		  "function f(a) {return fac(a);}\n"
    		  , "x", a);
      System.out.println(jsBEAST.scriptInput.get() + " = " + jsBEAST.getArrayValue());

      jsBEAST.requiresRecalculation();
      a.initByName("value", "5");
      System.out.println(jsBEAST.scriptInput.get() + " = " + jsBEAST.getArrayValue());

      
      
    	
//    	String tree = "(((A:1.0,B:1.0):1.0,C:2.0);";
//        TreeParser newickTree = new TreeParser(tree, false, false, true, 1);
//        ScriptEngineManager factory = new ScriptEngineManager();
//        ScriptEngine engine = factory.getEngineByName("JavaScript");
//        Object o = engine.eval("var tree = eval(\"({'height':'3.0'})\"); tree.height;");
//        System.out.println(o);

//        jsBEAST = new JSBEAST();
//        String script = "function f(t) {var tree = eval(\"(\"+t+\")\"); tree.height}";
//        jsBEAST.initByName("value", script, "x", newickTree);
//
//        System.out.println("f = " + jsBEAST.getArrayValue());


    }
    
//    void x() throws Exception {
//        RealParameter a = new RealParameter("1.0 3.0");
//        RealParameter b = new RealParameter("4.0");
//        Var v = new Var();
//        v.initByName("varname","a", "value", a);
//
//        Var v2 = new Var();
//        v2.initByName("varname","b", "value", b);
//
//        JSBEAST jsBEAST = new JSBEAST();
//        jsBEAST.initByName("value", "function f(a,b) {a[0] = 3*a[1]; return +a[0] + a[1] * b;}", "x", a, "x", b);
//        System.out.println("a[0] + a[1] * b = " + jsBEAST.getArrayValue());
//
//        jsBEAST.requiresRecalculation();
//        a.initByName("value", "3.0 4.0");
//        b.initByName("value", "-4.0");
//        System.out.println("a * 3 = " + jsBEAST.getArrayValue());
//    }


    // Loggable implementation
    @Override
    public void init(final PrintStream out) {
        int dim = getDimension();
        if (dim == 1)
            out.print(this.getID() + "\t");
        else
            for (int i = 0; i < dim; i++)
                out.print(this.getID() + "_" + i + "\t");
    }

    @Override
    public void log(final long nSample, final PrintStream out) {
    	isUpToDate = false;
        int dim = getDimension();
        for (int i = 0; i < dim; i++)
            out.print(getArrayValue(i) + "\t");
    }

    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }
    
}
