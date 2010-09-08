package beast.app.draw;

import beast.core.Input;
import beast.core.Plugin;

@SuppressWarnings("serial")
public class GUIPluginEditor extends InputEditor {

	public GUIPluginEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return GUIPlugin.class;
	}
	
    /**
     * construct an editor consisting of
     */
    @Override
    public void init(Input<?> input, Plugin plugin) {
    	 // TODO: implement
    }

} // class GUIPluginEditor
