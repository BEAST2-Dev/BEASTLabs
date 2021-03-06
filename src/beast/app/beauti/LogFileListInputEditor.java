package beast.app.beauti;

import beast.app.util.TreeFile;

public class LogFileListInputEditor extends FileListInputEditor {
	private static final long serialVersionUID = 1L;

	public LogFileListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
    @Override
    public Class<?> baseType() {
		return TreeFile.class;
    }
}
