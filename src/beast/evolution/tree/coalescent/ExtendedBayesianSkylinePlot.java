package beast.evolution.tree.coalescent;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;

// RRB: for a Beast1 implementation, see VariableDemographicModel
// for StartBeast, implement 
// dr.evomodel.coalescent.MultiLociTreeSet
// dr.evomodel.speciation.SpeciesTreeModel
// dr.evomodel.operators.TreeNodeSlide
@Description("An effective population size function based on coalecent times from a set of trees.")
public class ExtendedBayesianSkylinePlot extends PopulationFunction.Abstract {
        public Input<List<TreeIntervals>> m_trees = new Input<List<TreeIntervals>>("treeIntervals","Intervals for trees used to calculate x-axis of the population function", new ArrayList<TreeIntervals>(), Validate.REQUIRED);
        public Input<RealParameter> m_populationSize = new Input<RealParameter>("popSize","population size used for y-axis", Validate.REQUIRED);
        public Input<BooleanParameter> m_indicators = new Input<BooleanParameter>("indicators", "selects which of the population sizes are being used", Validate.REQUIRED);
        public Input<String> m_populationFunctionType = new Input<String>("populationFunctionType", "type of population function, must be one of 'constant' or 'linear' (default)", "linear");
        
        final static int LINEAR = 0, CONSTANT = 1;
        int m_popFunctionType = LINEAR; // todo, fix up
        boolean m_bRecompute = true;
        
        @Override
        public void initAndValidate() throws Exception {
            // set sizes of parameters
        	int nDimension = 1;//???
        	RealParameter popSize = new RealParameter((double) m_populationSize.get().getValue(), 0.0, Double.MAX_VALUE, nDimension);
        	m_populationSize.get().assignFrom(popSize);

        	nDimension = 1;//???
        	BooleanParameter indicators = new BooleanParameter(false, nDimension);
        	m_indicators.get().assignFrom(indicators);
            // set m_popFunctionType
        	if (m_populationFunctionType.get() == null || m_populationFunctionType.get().equals("linear")) {
        		m_popFunctionType = LINEAR;
        	} else if (m_populationFunctionType.get().equals("constant")) {
        		m_popFunctionType = CONSTANT;
        	} else {
        		throw new Exception("populationFunctionType should be 'linear' or 'constant' not '" + m_populationFunctionType.get() + "'");
        	}
        }

        
        
        
//        @Override
//        public double getArgument(int n) {
//                // TODO Auto-generated method stub
//                return 0;
//        }
//
//        @Override
//        public String getArgumentName(int n) {
//                // TODO Auto-generated method stub
//                return null;
//        }
//
//        @Override
//        public PopulationFunction getCopy() {
//                // TODO Auto-generated method stub
//                return null;
//        }

        @Override
        public double getIntensity(double t) {
                if (m_bRecompute) {
                        //computesomething();
                        m_bRecompute = false;
                }
                return 0;//precomputedvalue;
        }

        @Override
        public double getInverseIntensity(double x) {
                // TODO Auto-generated method stub
                return 0;
        }

//        @Override
//        public double getLowerBound(int n) {
//                // TODO Auto-generated method stub
//                return 0;
//        }
//
//        @Override
//        public int getNumArguments() {
//                // TODO Auto-generated method stub
//                return 0;
//        }

        @Override
        public List<String> getParameterIds() {
                // TODO Auto-generated method stub
                return null;
        }

        @Override
        public double getPopSize(double t) {
                // TODO Auto-generated method stub
                return 0;
        }

//        @Override
//        public double getUpperBound(int n) {
//                // TODO Auto-generated method stub
//                return 0;
//        }
//
//        @Override
//        public void setArgument(int n, double value) {
//                // TODO Auto-generated method stub
//                
//        }

    @Override
    protected boolean requiresRecalculation() {
        m_bRecompute = false;
        for (TreeIntervals tree : m_trees.get()) {
                if (tree.isDirtyCalculation()) {
                        m_bRecompute = true;
                        break;
                }
        }
        
        if (m_populationSize.isDirty()) {
                        m_bRecompute = true;
        }
        
        if (m_indicators.isDirty()) {
                        m_bRecompute = true;
        }
        
        return m_bRecompute;
    }       
    
    @Override
    public void store() {
            super.store();
    }
    
    @Override
    public void restore() {
            super.restore();
    }
    
    
} // class ExtendedBayesianSkylinePlot
