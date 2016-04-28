package beast.app.beauti;



import beast.app.util.XMLFile;
import beast.core.BEASTInterface;
import beast.core.Input;

public class TreeFileInputEditor extends FileInputEditor {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return XMLFile.class;
	}

	public TreeFileInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		init(input, plugin, itemNr, bExpandOption, bAddButtons, "tree files", "trees");
	}

}
