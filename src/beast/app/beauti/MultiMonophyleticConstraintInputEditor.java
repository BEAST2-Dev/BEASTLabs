package beast.app.beauti;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import beast.app.draw.BEASTObjectInputEditor;
import beast.app.draw.InputEditor;
import beast.app.draw.SmallButton;
import beast.app.draw.StringInputEditor;
import beast.core.BEASTInterface;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.StateNode;
import beast.core.util.Log;
import beast.evolution.tree.Tree;
import beast.math.distributions.MultiMonophyleticConstraint;

public class MultiMonophyleticConstraintInputEditor extends BEASTObjectInputEditor implements PriorProvider {
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return MultiMonophyleticConstraint.class;
	}
	

	public MultiMonophyleticConstraintInputEditor() {super(null);}
	public MultiMonophyleticConstraintInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
			boolean addButtons) {
		super.init(input, beastObject, itemNr, isExpandOption, addButtons);

		JButton deleteButton = new SmallButton("-", true);
        deleteButton.setToolTipText("Delete this multiple monophyletic constraint");
        deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Log.warning.println("Trying to delete a multiple monophyletic constraint");
				List<?> list = (List<?>) m_input.get();
				MultiMonophyleticConstraint prior = (MultiMonophyleticConstraint) list.get(itemNr);
				doc.disconnect(prior, "prior", "distribution");
				doc.unregisterPlugin(prior);
				refreshPanel();
			}        	
        });
        add(deleteButton);
        
	}
	
	@Override
	public List<Distribution> createDistribution(BeautiDoc doc) {
		this.doc = doc;
		List<Distribution> list = new ArrayList<>();
		MultiMonophyleticConstraint prior = new MultiMonophyleticConstraint();
        try {
        	
            List<Tree> trees = new ArrayList<>();
            getDoc().scrubAll(true, false);
            State state = (State) doc.pluginmap.get("state");
            for (StateNode node : state.stateNodeInput.get()) {
                if (node instanceof Tree) { // && ((Tree) node).m_initial.get() != null) {
                    trees.add((Tree) node);
                }
            }
            int treeIndex = 0;
            if (trees.size() > 1) {
                String[] treeIDs = new String[trees.size()];
                for (int j = 0; j < treeIDs.length; j++) {
                    treeIDs[j] = trees.get(j).getID();
                }
                treeIndex = JOptionPane.showOptionDialog(null, "Select a tree", "MRCA selector",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        treeIDs, trees.get(0));
            }
            if (treeIndex < 0) {
            	return list;
            }
            prior.treeInput.setValue(trees.get(treeIndex), prior);
            String id = trees.get(treeIndex).getID();
            if (id.contains(".t:")) {
            	id = id.substring(id.indexOf(".t:"));
            }
            prior.setID("MultiMonophyleticConstraint" + id);
            prior.isBinaryInput.setValue(false, prior);
        } catch (Exception e) {
        	return list;
        }
		
		list.add(prior);
		return list;
	}

	@Override
	public String getDescription() {
		return "Multiple monophyletic constraint";
	}

	
	public InputEditor createTreeEditor() {
		// suppress tree input editing 
		return new StringInputEditor(doc) {
			@Override
			public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
					boolean addButtons) {
				addInputLabel();
			}
			
		};
	}
	
	public InputEditor createNewickEditor() {
		StringInputEditor e = new StringInputEditor(doc);
		e.init(((MultiMonophyleticConstraint)m_beastObject).newickInput, m_beastObject, -1, ExpandOption.TRUE, true);
        // increase size of newick text editor
        for (int i = 0; i < e.getComponentCount(); i++) {
        	if (e.getComponent(i) instanceof JTextField) {
        		((JTextField)e.getComponent(i)).setColumns(50);
        		System.err.println("Got one!");
        	}
        }
        return e;
	}
}
