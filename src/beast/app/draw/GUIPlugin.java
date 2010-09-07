package beast.app.draw;


import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.Input.Validate;

@Description("Plugin to assemble GUI components so that other Plugins can be pre-configured " +
		"and manipulated together through a Panel.")
public class GUIPlugin extends Plugin {
	public Input<Plugin> m_plugin = new Input<Plugin>("plugin", "Plugin that we want to manipulate", Validate.REQUIRED);
	public Input<String> m_spec = new Input<String>("value", "Comma separated list of inputs to be shown in GUI.");
	public Input<String> m_conditions = new Input<String>("condition", "One or more conditions for which this GUIPlugin is applicable " +
			"This is a comma separated string of any of the following conditions to be checked: " +
			"statecount, type, ??"); // perhaps this should be a list of 'condition' Plugins?

	
	List<Input<?>> m_inputs;
	
	@Override
	public void initAndValidate() throws Exception {
		m_inputs = new ArrayList<Input<?>>();
		// parse specification, and collect relevant inputs 
		String [] sSpecs = m_spec.get().split(",");
		for (String sSpec : sSpecs) {
			Plugin plugin = this;
			while (sSpec.contains(".")) {
				String sInput = sSpec.substring(0, sSpec.indexOf('.'));
				plugin = (Plugin) plugin.getInput(sInput).get();
				sSpec = sSpec.substring(sSpec.indexOf('.') + 1);
			}
			Input<?> input = plugin.getInput(sSpec);
			m_inputs.add(input);
		}
		// parse conditions
	} // initAndValidate

	
	
	public boolean checkConditions(Class<?> pluginClass) {
		if (!(m_plugin.get().getClass().isAssignableFrom(pluginClass))) {
			// wrong type
			return false;
		}
		// check other conditions
		// TODO....
		return true;
	}
	
	
	public void addGUIComponenets(Box editor) {
		for (Input input : m_inputs) {
			editor.add(editor);
		}
	} // addGUIComponenets
	
	
} // class GUIPlugin
