package beast.util;




import java.io.PrintStream;
import java.util.*;

import javax.script.*;

import beast.core.*;
import beast.core.Input.*;
import beast.core.parameter.RealParameter;
import beast.core.util.*;
import beast.evolution.tree.*;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.objects.NativeArray;

@Description("Base class for Script-BEAST interoperation")
public class Script extends CalculationNode implements Loggable, beast.core.Function {
    public Input<String> scriptInput = new Input<String>("value", "Script script needed for the calculations. " +
    		"It assumes there is a function f defined, which returns a single number or array of numbers.");
    public Input<String> expressionInput = new Input<String>("expression", "expression representing the calculations", Validate.XOR, scriptInput);
    public Input<List<beast.core.Function>> functionInput = new Input<List<beast.core.Function>>("x", "Parameters needed for the calculations", new ArrayList<beast.core.Function>());
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
        // create a script engine manager
        ScriptEngineManager factory = new ScriptEngineManager();
        // create a JavaScript engine
        engine = factory.getEngineByName(engineInput.get().name());
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
                for (Function x : functionInput.get()) {
                    f.append(((BEASTObject) x).getID());
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




    private void calc() {
        //Bindings bind = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        Object [] args = new Object[functionInput.get().size()];
        int k = 0;
        for (beast.core.Function f : functionInput.get()) {
            //String name = var.nameInput.get();
            Object _value = null;
            //beast.core.Function f = var.functionInput.get();
            if (f instanceof Tree) {
                Tree tree = (Tree) f;
                JSONProducer p = new JSONProducer();
                String treeString = toJSON(tree.getRoot());
                _value = "{" + treeString + "}";
            } else if (f.getDimension() > 1) {
                Double [] a = new Double[f.getDimension()];
                for (int i = 0;i < f.getDimension(); i++) {
                    a[i] = f.getArrayValue(i);
                }
                _value = a;
            } else {
                _value = f.getArrayValue();
            }
            args[k++] = _value;
            //bind.put(name, _value);
        }

        
        try {
       		Object o = inv.invokeFunction("f", args);

            if (o instanceof ScriptObjectMirror) {
                value = new double[((ScriptObjectMirror)o).values().size()];
                for (int i = 0; i < value.length; i++) {
                    this.value[i] = Double.parseDouble(((ScriptObjectMirror)o).get(i).toString());
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
//            beast.core.Function f = var.functionInput.get();
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

    @Override public int getDimension() {return value.length;}


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
      RealParameter a = new RealParameter("1.0 3.0");
      a.setID("a");
      RealParameter b = new RealParameter("4.0");
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
      a = new RealParameter("1.0");
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
        if (value.length == 1)
            out.print(this.getID() + "\t");
        else
            for (int i = 0; i < value.length; i++)
                out.print(this.getID() + "_" + i + "\t");
    }

    @Override
    public void log(final long nSample, final PrintStream out) {
    	isUpToDate = false;
        for (int i = 0; i < value.length; i++)
            out.print(getArrayValue(i) + "\t");
    }

    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }
    
}
