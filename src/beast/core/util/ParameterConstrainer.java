package beast.core.util;


import java.io.PrintStream;
import java.util.Arrays;

import beast.base.inference.CalculationNode;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.tree.Tree;


/**
 * @author dkuh004
 *         Date: Sep 14, 2011
 *         Time: 3:54:16 PM
 */
@Description("Class to constrain a multi-dimensional parameter to have a certain value (like 0) at a certain time range")
public class ParameterConstrainer extends CalculationNode implements Loggable {

    public Input<Tree> m_tree =
            new Input<Tree>("tree", "the phylogenetic tree to provide tree height", Input.Validate.REQUIRED);
    public Input<RealParameter> orig_root =
            new Input<RealParameter>("orig_root", "The origin of infection x0", Input.Validate.REQUIRED);

    public Input<Double> constraintValue =
            new Input<Double>("constraintValue", "The value to be used from startTime to time endTime", Input.Validate.REQUIRED);
    public Input<Double> startTime =
            new Input<Double>("startTime", "The startTime from when to constrain on, (in forward time)", Input.Validate.REQUIRED);
    public Input<Double> endTime =
            new Input<Double>("endTime", "The endTime until when to constrain, (in forward time)", Input.Validate.REQUIRED);

    public Input<RealParameter> m_parameter =
            new Input<RealParameter>("constrainedParameter", "parameter to be constrained", Input.Validate.REQUIRED);

    public Input<RealParameter> baseParameter =
            new Input<RealParameter>("baseParameter", "the unconstrained base parameter", Input.Validate.REQUIRED);


    boolean m_bRecompute = true;

    // nicetohave: change to height i.e. backwards time!!!

    int dim;
    RealParameter parameter;
    double T;
    double youngestTipDate;
    Tree tree;
    int start;
    double end;


    @Override
    public void initAndValidate() {

        dim = m_parameter.get().getDimension();
        tree = m_tree.get();
        T = tree.getRoot().getHeight() + orig_root.get().getValue();

        youngestTipDate = -1;
        for (int i = 0; i< tree.getLeafNodeCount(); i++){
            if (tree.getNode(i).getHeight() == 0)
                youngestTipDate = tree.getNode(i).getDate();
        }

        if (startTime.get() < 0 || startTime.get() > youngestTipDate || endTime.get() < 0 || endTime.get() > youngestTipDate){
            throw new IllegalArgumentException("Out of bounds: startTime or endTime is not within (0, T)");
        }
        start = (int) (startTime.get() / dim);
        end = endTime.get();

       initialConstrain();
    }

    public void initialConstrain() {
        parameter = m_parameter.get();

        Double[] temp = baseParameter.get().getValues();

        tree = m_tree.get();
        T = tree.getRoot().getHeight() + orig_root.get().getValue();

        youngestTipDate = -1;
        for (int i = 0; i< tree.getLeafNodeCount(); i++){
            if (tree.getNode(i).getHeight() == 0)
                youngestTipDate = tree.getNode(i).getDate();
        }


        double tau = T / dim;
        double c = constraintValue.get();

        for(int i = start; i<dim && ((i+1)*tau)<(T-(youngestTipDate-end)); i++){
            temp[i] = c;
        }

        parameter.assignFromWithoutID(new RealParameter(temp));
    }


    public void constrain(){
        parameter = m_parameter.get();

        Double[] temp = baseParameter.get().getValues();

        tree = m_tree.get();
        T = tree.getRoot().getHeight() + orig_root.get().getValue();

        youngestTipDate = -1;
        for (int i = 0; i< tree.getLeafNodeCount(); i++){
            if (tree.getNode(i).getHeight() == 0)
                youngestTipDate = tree.getNode(i).getDate();
        }


        double tau = T / dim;
        double c = constraintValue.get();

        int j = 0;
        for(int i = start; i<dim && ((i+1)*tau)<(T-(youngestTipDate-end)); i++){
            parameter.setValue(i, c);
            j = i;
        }
        for (int i=j+1; i < dim; i++){
            parameter.setValue(i, temp[i]);
        }
    }


    public RealParameter getConstrained(){
        return parameter;
    }

    public void init(PrintStream out) {
        if (dim == 1) {
            out.print(getID() + "\t");
        } else {
            for (int iValue = 0; iValue < dim; iValue++) {
                out.print(getID() + (iValue + 1) + "\t");
            }
        }
    }

    public void log(long i, PrintStream out) {
        for (int iValue = 0; iValue < dim; iValue++) {
            out.print(parameter.getValue(iValue) + "\t");
        }
    }

    @Override
    public boolean requiresRecalculation() {
        m_bRecompute = true;
        return true;
    }


    public void close(PrintStream printStream) {
        //nothing to do
    }

    public static void main(String[] args){

    }
}
