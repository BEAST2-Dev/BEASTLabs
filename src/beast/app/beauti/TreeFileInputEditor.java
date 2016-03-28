package beast.app.beauti;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import beast.app.draw.InputEditor;
import beast.app.util.XMLFile;
import beast.app.util.Utils;
import beast.core.BEASTInterface;
import beast.core.Input;

public class TreeFileInputEditor extends InputEditor.Base {
	
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
		super.init(input, plugin, itemNr, bExpandOption, bAddButtons);
		m_entry.setText(((File) m_input.get()).getName());
		
		JButton button = new JButton("browse");
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				File defaultFile;
				if (((File) m_input.get()).exists()) {
					defaultFile = (File) m_input.get();
				} else {
					defaultFile = new File(Beauti.g_sDir);
				}
				File file = Utils.getLoadFile(m_input.getTipText(), defaultFile, "tree files", "trees");
				if (file != null) {
					file = new XMLFile(file.getPath());
				}
				try {
					m_entry.setText(file.getName());
					m_input.setValue(file, m_beastObject);
					String path = file.getPath();
					Beauti.g_sDir = path;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		add(button);
	}

	@Override
	protected void setValue(Object o) {
		String file = o.toString();
		if (file.equals("")) {
			return;
		}
		String fileSep = System.getProperty("file.separator");
		String origFile = ((File) m_input.get()).getAbsolutePath();
		if (origFile.indexOf(fileSep) >= 0 && file.indexOf(fileSep) < 0) {
			if (origFile.contains(origFile)) {
				file = origFile.substring(0, origFile.lastIndexOf(fileSep) + 1) + file;
			}
		}
		m_input.setValue(file, m_beastObject);	
   	}
	

}
