package beast.app.beauti;

import java.io.File;
import java.io.IOException;
import java.util.List;

import beast.app.beauti.BeautiAlignmentProvider;
import beast.app.beauti.BeautiDoc;
import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Param;
import beast.evolution.alignment.Alignment;
import beast.math.distributions.MRCAPrior;

@Description("Compact BEAST analysis specification in NEXUS like format.")
public class CompactAnalysisSpec extends BEASTObject {
	private String spec;
	private BeautiDoc doc;

	
	public CompactAnalysisSpec(@Param(name="value", description="specification of the analysis") String spec) {
		this.spec = spec;
	}
	
	public String getValue() {
		return spec;
	}

	public void setValue(String spec) {
		this.spec = spec;
	}

	@Override
	public void initAndValidate() {
		doc = new BeautiDoc();
		doc.beautiConfig = new BeautiConfig();
		doc.beautiConfig.initAndValidate();
		doc.beautiConfig.setDoc(doc);


		String [] cmds = spec.split(";");
		int cmdCount = 1;
		try {
			for (String cmd : cmds) {
				processCommand(cmd.trim(), cmdCount++);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

	private void processCommand(String cmd, int cmdCount) throws IOException {
		if (cmdCount == 1 && !cmd.toLowerCase().startsWith("template")) {
			doc.processTemplate("Standard.xml");
		}
		String [] strs = cmd.split("=");
		for (int i = 0; i < strs.length; i++) {
			strs[i] = strs[i].trim();
		}

		if (cmd.toLowerCase().startsWith("template")) {
			
			// set template -- must be at start of file
			// template=<template name>;
			if (strs.length != 2) {
				throw new IllegalArgumentException("Command " + cmdCount + ": Expected 'template=<template name>;' but got " + cmd);
			}
			if (cmdCount != 1) {
				throw new IllegalArgumentException("Command " + cmdCount + ": 'template=<template name>;' can only be at the start, not at command " + cmdCount);
			}
			String template = strs[1];
			doc.processTemplate(template);
		} else if (cmd.toLowerCase().startsWith("import")) {

			// import an alignment form file
			// import=<alignment file>;
			if (strs.length != 2) {
				throw new IllegalArgumentException("Command " + cmdCount + ": Expected 'import=<alignment file>;' but got " + cmd);
			}
			BeautiAlignmentProvider provider = new BeautiAlignmentProvider();
	        List<BEASTInterface> beastObjects = provider.getAlignments(doc, new File[]{new File(strs[1])});
	        if (beastObjects != null) {
		        for (BEASTInterface o : beastObjects) {
		        	if (o instanceof Alignment) {
		        		try {
		        			BeautiDoc.createTaxonSet((Alignment) o, doc);
		        		} catch(Exception ex) {
		        			ex.printStackTrace();
		        		}
		        	}
		        }
	        }

            doc.connectModel();
            doc.fireDocHasChanged();
            
	        if (beastObjects != null) {
		        for (BEASTInterface o : beastObjects) {
		        	if (o instanceof MRCAPrior) {
	        			doc.addMRCAPrior((MRCAPrior) o);
		        	}
		        }
	        }
		} else if (cmd.toLowerCase().startsWith("partition")) {
			// 
		} else if (cmd.toLowerCase().startsWith("link")) {
			// 
		} else if (cmd.toLowerCase().startsWith("unlink")) {
			// 
		} else if (cmd.toLowerCase().startsWith("set")) {
			// set <identifier> = <value>;
		} else {
			// assume this specifies a subtemplate
			// [<id pattern> =]? <SubTemplate>;
			if (strs.length > 2) {
				throw new IllegalArgumentException("Command " + cmdCount + ": expected [<id pattern> =]? <SubTemplate>; but got " + cmd);
			}
			String pattern;
			String subTemplateName;
			if (strs.length == 2) {
				pattern = strs[0];
				subTemplateName = strs[1];
			} else {
				// match anything
				pattern =".*";
				subTemplateName = strs[0];
			}
			
			BEASTInterface bo = null;
            for (BeautiSubTemplate subTemplate : doc.beautiConfig.subTemplates) {
            	if (subTemplate.getID().equals(subTemplateName)) {
            		bo = subTemplate.createSubNet(new PartitionContext(), true);
            	}
            }
			if (bo == null) {
				throw new IllegalArgumentException("Command " + cmdCount + ": cannot find template '" + subTemplateName + "'");
			}

			boolean matchFound = false; 
			for (String id : doc.pluginmap.keySet()) {
				BEASTInterface o = doc.pluginmap.get(id);
				for (String name : o.getInputs().keySet()) {
					if ((id + "." + name).matches(pattern)) {
						Input<?> in = o.getInputs().get(name);
						if (in.canSetValue(bo, o)) {
							in.setValue(bo, o);
							matchFound = true;
						}
					}
				}
			}
			if (!matchFound) {
				throw new IllegalArgumentException("Command " + cmdCount + ": cannot find suitable match for " + cmd);
			}
			
		}
	}

}
