package beast.app.simulator;

import beast.app.draw.PluginPanel;
import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.Input.Validate;

@Description("defines neighborhood relation in Stepping Stone simulations")
public class Neighbor extends Plugin {
	public Input<String> m_neighborsInput = new Input<String>("island", "name of neighbor island", Validate.REQUIRED); 
	public Input<Double> m_migrationRateInput = new Input<Double>("rate", "rate of migration to neighbor island", Validate.REQUIRED);
	
    public boolean canSetIsland(Object o) throws Exception {
    	String sName = (String) o;
    	if (PluginPanel.g_plugins != null && PluginPanel.g_plugins.size() > 0) {
    		if (!PluginPanel.g_plugins.containsKey(sName)) {
        		return false;
    		}
    		Plugin plugin = PluginPanel.g_plugins.get(sName);
    		if (!(plugin instanceof Island)) {
    			return false;
    		}
    	}
    	return true;
    }

	@Override
	public void initAndValidate() throws Exception {
	}
}
