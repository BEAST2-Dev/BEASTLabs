package beast.app.simulator;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;

import beast.app.beauti.BeautiDoc;
import beast.app.draw.InputEditor;
import beast.app.draw.ListInputEditor;
import beast.app.draw.PluginPanel;
import beast.core.Input;
import beast.core.Plugin;

public class NeighborInputEditor extends ListInputEditor {
	public NeighborInputEditor(BeautiDoc doc) {
		super(doc);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> baseType() {
		return Neighbor.class;
	}

	@Override
    protected void addPluginItem(Box itemBox, Plugin plugin) {
    	Neighbor neighbor = (Neighbor) plugin;
    	try {
    		InputEditor inputEditor = PluginPanel.createInputEditor(neighbor.m_neighborsInput, plugin, true, ExpandOption.FALSE, ButtonStatus.ALL, this, doc);
    		inputEditor.addValidationListener(this);
			itemBox.add((Component) inputEditor);

			inputEditor = PluginPanel.createInputEditor(neighbor.m_migrationRateInput, plugin, true, ExpandOption.FALSE, ButtonStatus.ALL, this, doc);
			itemBox.add((Component) inputEditor);
    		inputEditor.addValidationListener(this);

			itemBox.add(Box.createVerticalGlue());
    	} catch (Exception e) {
			e.printStackTrace();
		}

    }

	@Override
    public List<Plugin> pluginSelector(Input<?> input, Plugin parent, List<String> sTabuList) {
        Plugin plugin = new Neighbor();
        PluginPanel.addPluginToMap(plugin, null);
    	List<Plugin> selectedPlugins = new ArrayList<Plugin>();
    	selectedPlugins.add(plugin);
		return selectedPlugins;
	}
}
