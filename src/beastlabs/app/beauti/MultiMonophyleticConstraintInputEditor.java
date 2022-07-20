package beastlabs.app.beauti;

import java.util.ArrayList;
import java.util.List;


import beastfx.app.beauti.PriorProvider;
import beastfx.app.inputeditor.BEASTObjectInputEditor;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.inputeditor.SmallButton;
import beastfx.app.inputeditor.StringInputEditor;
import beastfx.app.util.Alert;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beastlabs.math.distributions.MultiMonophyleticConstraint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;

public class MultiMonophyleticConstraintInputEditor extends BEASTObjectInputEditor implements PriorProvider {

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

		Button deleteButton = new SmallButton("-", true);
        deleteButton.setTooltip(new Tooltip("Delete this multiple monophyletic constraint"));
        deleteButton.setOnAction(e -> {
				Log.warning.println("Trying to delete a multiple monophyletic constraint");
				List<?> list = (List<?>) m_input.get();
				MultiMonophyleticConstraint prior = (MultiMonophyleticConstraint) list.get(itemNr);
				doc.disconnect(prior, "prior", "distribution");
				doc.unregisterPlugin(prior);
				refreshPanel();
			});
        pane.getChildren().add(deleteButton);        
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
            Tree tree  = null;
            if (trees.size() > 1) {
                String[] treeIDs = new String[trees.size()];
                for (int j = 0; j < treeIDs.length; j++) {
                    treeIDs[j] = trees.get(j).getID();
                }
                tree = (Tree) Alert.showInputDialog(null, "Select a tree", "MRCA selector", Alert.QUESTION_MESSAGE, null, treeIDs, trees.get(0));
            }
            if (tree == null) {
            	return list;
            }
            prior.treeInput.setValue(tree, prior);
            String id = tree.getID();
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

	
	/** assumes args are Newick string, Tree partition (if any) */
	@Override
	public List<Distribution> createDistribution(BeautiDoc doc, List<Object> args) {
		this.doc = doc;
		List<Distribution> list = new ArrayList<>();

		MultiMonophyleticConstraint prior = new MultiMonophyleticConstraint();
        getDoc().scrubAll(true, false);

        if (args.size() <= 1) {
            getDoc().scrubAll(true, false);
            State state = (State) doc.pluginmap.get("state");
            for (StateNode node : state.stateNodeInput.get()) {
                if (node instanceof Tree) { 
    	            prior.treeInput.setValue(node, prior);
    	            break;
                }
            }
        } else {
        	Object tree = doc.pluginmap.get("Tree.t:" + args.get(1));
            prior.treeInput.setValue(tree, prior);
        }

        String newick = args.get(0).toString().trim();
        if (newick.charAt(0) == '"' || newick.charAt(0) == '\'') {
        	newick = newick.substring(1, newick.length() - 1);
        }
        prior.newickInput.setValue(newick, prior);
        
        String id = prior.treeInput.get().getID();
        if (id.contains(".t:")) {
        	id = id.substring(id.indexOf(".t:"));
        }
        prior.setID("MultiMonophyleticConstraint" + id);
        prior.isBinaryInput.setValue(false, prior);
		
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
		for (Node n : e.getChildren()) {
			if (n instanceof Pane) {
				Pane pane = (Pane) n;
		        for (Node n2 : pane.getChildren()) {
		        	if (n2 instanceof TextField) {
		        		((TextField)n2).setMinWidth(200);
		        		System.err.println("Got one!");
		        	}
		        }
			}
		}
        return e;
	}
}
