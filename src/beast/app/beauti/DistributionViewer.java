package beast.app.beauti;


import java.awt.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.WindowConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.app.beauti.BeautiDoc;
import beast.app.beauti.BeautiSubTemplate;
import beast.app.beauti.PartitionContext;
import beast.app.draw.InputEditor;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;
import beast.math.distributions.Uniform;
import beast.util.XMLParserException;

public class DistributionViewer {

	BeautiDoc doc;
	
		public DistributionViewer() throws XMLParserException, SAXException, IOException, ParserConfigurationException {
			doc = new BeautiDoc();
			doc.parseArgs(new String[]{});
			
            JFrame frame = new JFrame("Parametric distribution");
            // start with prior with uniform(0,1) distribution
            final Prior prior = new Prior();
            Uniform uniform = new Uniform();
            uniform.setID("Uniform.0");
            uniform.initByName("lower","0.0","upper","1.0");
            prior.initByName("x", new RealParameter("0.0"), "distr", uniform);
            prior.setID("Parametric.Distribution");
            
            // create panel with parametric distribution viewer
            refreshDistributionPanel(frame, prior);

            frame.setSize(800, 400);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }

		private void refreshDistributionPanel(JFrame frame, Prior prior) {
			// clear frame content in case it is refreshed after change of distribution  
			JRootPane p = (JRootPane) frame.getComponent(0);
			JLayeredPane p1 = (JLayeredPane) p.getComponent(1);
			JPanel p2 = (JPanel) p1.getComponent(0);
			p2.removeAll();
			
            final JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			
            // add combobox with parametric distributions
            List<BeautiSubTemplate> availableBEASTObjects = doc.getInputEditorFactory().getAvailableTemplates(prior.distInput, prior, null, doc);
            JComboBox<BeautiSubTemplate> comboBox = new JComboBox<BeautiSubTemplate>(availableBEASTObjects.toArray(new BeautiSubTemplate[]{}));
            panel.add(comboBox);
            
            String id = prior.distInput.get().getID();
            id = id.substring(0, id.indexOf('.'));
            for (BeautiSubTemplate template : availableBEASTObjects) {
                if (template.classInput.get() != null && template.shortClassName.equals(id)) {
                    comboBox.setSelectedItem(template);
                }
            }
            
            try {
            	// add parametric input editor
                final InputEditor editor = doc.inputEditorFactory.createInputEditor(prior.distInput, prior, doc);
                panel.add((Component) editor);
                comboBox.addActionListener(e -> {
                    @SuppressWarnings("unchecked")
        			JComboBox<BeautiSubTemplate> comboBox1 = (JComboBox<BeautiSubTemplate>) e.getSource();

                    BeautiSubTemplate template = (BeautiSubTemplate) comboBox1.getSelectedItem();
                    try {
                        template.createSubNet(new PartitionContext(), prior, prior.distInput, true);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    refreshDistributionPanel(frame, prior);
                });
			} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
            
            // add close button
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> {
            	frame.setVisible(false);
            	frame.dispose();
            });
            panel.add(closeButton);
			
            frame.add(panel);
            frame.revalidate();
		}

    public static void main(String[] args) {
		try {
			new DistributionViewer();
		} catch (XMLParserException | SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

}
