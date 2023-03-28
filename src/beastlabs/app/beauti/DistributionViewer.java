package beastlabs.app.beauti;


import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.BeautiSubTemplate;
import beast.base.parser.PartitionContext;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.StateNode;
import beast.base.inference.distribution.Prior;
import beast.base.inference.distribution.Uniform;
import beast.base.parser.XMLParserException;

public class DistributionViewer extends javafx.application.Application {

	BeautiDoc doc;
	
	public DistributionViewer() throws XMLParserException, SAXException, IOException, ParserConfigurationException {
		doc = new BeautiDoc();
		doc.parseArgs(new String[]{});
		doc.beautiConfig.suppressBEASTObjects.add(RealParameter.class.getName() + ".estimate");
		doc.beautiConfig.suppressBEASTObjects.add(StateNode.class.getName() + ".estimate");
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
        final Prior prior = new Prior();
        Uniform uniform = new Uniform();
        uniform.setID("Uniform.0");
        uniform.initByName("lower","0.0","upper","1.0");
        RealParameter parameter = new RealParameter("0.0");
        parameter.setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        prior.initByName("x", parameter, "distr", uniform);
        prior.setID("Parametric.Distribution");

        // add combobox with parametric distributions
		VBox box = FXUtils.newVBox();
        List<BeautiSubTemplate> availableBEASTObjects = doc.getInputEditorFactory().getAvailableTemplates(prior.distInput, prior, null, doc);
        ComboBox<BeautiSubTemplate> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(availableBEASTObjects.toArray(new BeautiSubTemplate[]{}));
        
        box.getChildren().add(comboBox);
        
        final InputEditor editor = doc.inputEditorFactory.createInputEditor(prior.distInput, prior, doc);
        box.getChildren().add((Node)editor);
        comboBox.getSelectionModel().select(availableBEASTObjects.get(0));
        
        comboBox.setOnAction(e -> {
            @SuppressWarnings("unchecked")
			ComboBox<BeautiSubTemplate> comboBox1 = (ComboBox<BeautiSubTemplate>) e.getSource();

            BeautiSubTemplate template = (BeautiSubTemplate) comboBox1.getSelectionModel().getSelectedItem();
            try {
                template.createSubNet(new PartitionContext(), prior, prior.distInput, true);
                final InputEditor editor2 = doc.inputEditorFactory.createInputEditor(prior.distInput, prior, doc);
                box.getChildren().remove(1);
                box.getChildren().add((Node) editor2);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        
        primaryStage.setScene(new Scene(box, 1024, 650));
        primaryStage.show();		
	}

    public static void main(String[] args) {
		launch(DistributionViewer.class, args);
	}

}
