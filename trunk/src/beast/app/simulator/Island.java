package beast.app.simulator;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.Input.Validate;

@Description("defines island/step in Stepping Stone simulations")
public class Island extends Plugin {
	public Input<Double> m_posXInput = new Input<Double>("posx", "x position of the island, used for drawing", Validate.REQUIRED); 
	public Input<Double> m_posYInput = new Input<Double>("posy", "y position of the island, used for drawing", Validate.REQUIRED); 
	public Input<List<Neighbor>> m_neighborsInput = new Input<List<Neighbor>>("neighbor", "list of neighbor islands", new ArrayList<Neighbor>()); 
	public Input<Double> m_initialPropInput = new Input<Double>("initial", "initial proportion of allele l (default 0.5)", 0.5); 

	@Override
	public void initAndValidate() throws Exception {
	}
}
