package beastlabs.app.beauti;

import beast.app.inputeditor.BeautiDoc;
import beastlabs.app.util.TreeFile;

public class XMLFileListInputEditor extends FileListInputEditor {
	private static final long serialVersionUID = 1L;

	public XMLFileListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
    @Override
    public Class<?> baseType() {
		return TreeFile.class;
    }
}
