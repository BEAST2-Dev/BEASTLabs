package beast.core.util;

import beast.core.Input;
import beast.core.CalculationNode;
import beast.core.Loggable;
import beast.core.Description;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;

import java.io.PrintStream;

/**
 * @author dkuh004
 *         Date: Sep 14, 2011
 *         Time: 3:54:16 PM
 */

@Description("Class that filters a parameter that needs to be constrained to 0 for some of it's values, determined by startTime and endTime")
public class ParameterConstrainer extends CalculationNode implements Loggable {

    public Input<Tree> m_tree =
            new Input<Tree>("tree", "the phylogenetic tree", Input.Validate.REQUIRED);
    public Input<RealParameter> orig_root =
            new Input<RealParameter>("orig_root", "The origin of infection x0", Input.Validate.REQUIRED);

    public Input<Double> constraintValue =
            new Input<Double>("constraintValue", "The value to be used from startTime to time endTime", Input.Validate.REQUIRED);
    public Input<Double> startTime =
            new Input<Double>("startTime", "The startTime from when to constrain on, (in forward time)", Input.Validate.REQUIRED);
    public Input<Double> endTime =
            new Input<Double>("endTime", "The endTime until when to constrain, (in forward time)", Input.Validate.REQUIRED);


    boolean m_bRecompute = true;

    int dim;
    Double[] parameter;

    // to improve: change to height i.e. backwards time!!!
    double T;
    double youngestTipDate;
    Tree tree;
    int start;
    double end;


    @Override
    public void initAndValidate() throws Exception {

        tree = m_tree.get();
        T = tree.getRoot().getHeight() + orig_root.get().getValue();

        youngestTipDate = -1;
        for (int i = 0; i< tree.getLeafNodeCount(); i++){
            if (tree.getNode(i).getHeight() == 0)
                youngestTipDate = tree.getNode(i).getDate();
        }

        if (startTime.get() < 0 || startTime.get() > youngestTipDate || endTime.get() < 0 || endTime.get() > youngestTipDate){
            throw new Exception("Out of bounds: startTime or endTime is not within (0, T)");
        }
        start = (int) (startTime.get() / dim);
        end = endTime.get();


    }
    public void constrain(Double[] values, int dim){
        parameter = values;

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
            parameter[i] = c;
        }
    }


    public Double[] get(Double[] values, int i){
        constrain(values, i);
        dim = values.length;
        return parameter;
    }

    @Override
    public void init(PrintStream out) throws Exception {
        if (dim == 1) {
            out.print(getID() + "\t");
        } else {
            for (int iValue = 0; iValue < dim; iValue++) {
                out.print(getID() + (iValue + 1) + "\t");
            }
        }
    }

    @Override
    public void log(int i, PrintStream out) {
        for (int iValue = 0; iValue < dim; iValue++) {
            out.print(parameter[iValue] + "\t");
        }
    }

    @Override
    public boolean requiresRecalculation() {
        m_bRecompute = true;
        return true;
    }


    @Override
    public void close(PrintStream printStream) {
        //nothing to do
    }

    public static void main(String[] args){
        
    }
}
