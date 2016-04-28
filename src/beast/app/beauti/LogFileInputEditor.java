package beast.app.beauti;


import beast.app.util.LogFile;
import beast.core.BEASTInterface;
import beast.core.Input;

public class LogFileInputEditor extends FileInputEditor {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return LogFile.class;
	}

	public LogFileInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		init(input, plugin, itemNr, bExpandOption, bAddButtons, "trace files", "log");
	}

}
