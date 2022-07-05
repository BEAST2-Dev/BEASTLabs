package beastlabs.app.beauti;



import java.io.File;

import beast.app.inputeditor.BeautiDoc;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beastlabs.app.util.XMLFile;

public class XMLFileInputEditor extends FileInputEditor {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return XMLFile.class;
	}

	public XMLFileInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		init(input, plugin, itemNr, bExpandOption, bAddButtons, "BEAST XML files", "xml");
	}
	
	protected File newFile(File file) {
		return new XMLFile(file.getPath());
	}

}
