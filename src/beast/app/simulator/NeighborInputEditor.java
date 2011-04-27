package beast.app.simulator;

import java.util.List;

import javax.swing.Box;

import beast.app.draw.InputEditor;
import beast.app.draw.ListInputEditor;
import beast.app.draw.PluginPanel;
import beast.core.Input;
import beast.core.Plugin;

public class NeighborInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> baseType() {
		return Neighbor.class;
	}
    
	@Override
    protected void addPluginItem(Box itemBox, Plugin plugin) {
    	Neighbor neighbor = (Neighbor) plugin;
    	try {
    		InputEditor inputEditor = PluginPanel.createInputEditor(neighbor.m_neighborsInput, plugin, true, EXPAND.FALSE, BUTTONSTATUS.ALL, this);
    		inputEditor.addValidationListener(this);
			itemBox.add(inputEditor);

			inputEditor = PluginPanel.createInputEditor(neighbor.m_migrationRateInput, plugin, true, EXPAND.FALSE, BUTTONSTATUS.ALL, this); 
			itemBox.add(inputEditor);
    		inputEditor.addValidationListener(this);
    		
			itemBox.add(Box.createVerticalGlue());
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }

	@Override
    public Plugin pluginSelector(Input<?> input, Plugin parent, List<String> sTabuList) {
        Plugin plugin = new Neighbor();
        PluginPanel.addPluginToMap(plugin);
		return plugin; 
	}
}
