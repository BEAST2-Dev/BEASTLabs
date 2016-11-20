package beast.app.beauti;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beast.app.beauti.BeautiAlignmentProvider;
import beast.app.beauti.BeautiDoc;
import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.Param;
import beast.core.parameter.Parameter;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.sitemodel.SiteModelInterface;
import beast.evolution.tree.TreeDistribution;
import beast.evolution.tree.TreeInterface;
import beast.math.distributions.MRCAPrior;
import beast.util.XMLProducer;

@Description("Compact BEAST analysis specification in NEXUS like format.")
public class CompactAnalysisSpec extends BEASTObject {
	private String spec;
	private BeautiDoc doc;
	private Set<PartitionContext> partitionContext;

	
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
		partitionContext = new LinkedHashSet<>();
		doc = new BeautiDoc();
		doc.beautiConfig = new BeautiConfig();
		doc.beautiConfig.initAndValidate();

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
			doc.loadNewTemplate("Standard.xml");
		}
		String [] strs = cmdsplit(cmd);
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
			if (!template.toLowerCase().endsWith("xml")) {
				template += ".xml";
			}
			doc.loadNewTemplate(template);
		} else if (cmd.toLowerCase().startsWith("import")) {

			// import an alignment form file
			// import [Alignment provider id] <alignment file>;
			if (strs.length < 2) {
				throw new IllegalArgumentException("Command " + cmdCount + ": Expected 'import <alignment file>;' but got " + cmd);
			}
			importData(strs);
		} else if (cmd.toLowerCase().startsWith("partition")) {
			String pattern = strs[1];
			partitionContext.clear();
			for (PartitionContext p : doc.partitionNames) {
				if (p.partition.matches(pattern)) {
					partitionContext.add(new PartitionContext(p.partition, p.siteModel, p.clockModel, p.tree));
				}
			}
		} else if (cmd.toLowerCase().startsWith("link")) {
			if (partitionContext.size() <= 1) {
				throw new IllegalArgumentException("Command " + cmdCount + ": At least two partitions must be selected " + cmd);
			}
			PartitionContext [] contexts = partitionContext.toArray(new PartitionContext[]{});
			GenericTreeLikelihood [] treelikelihood = new GenericTreeLikelihood[contexts.length];
			CompoundDistribution likelihoods = (CompoundDistribution) doc.pluginmap.get("likelihood");

			for (int i = 0; i < partitionContext.size(); i++) {
				String partition = contexts[i].partition;
				for (int j = 0; j < likelihoods.pDistributions.get().size(); j++) {
					GenericTreeLikelihood likelihood = (GenericTreeLikelihood) likelihoods.pDistributions.get().get(i);
					assert (likelihood != null);
					if (likelihood.dataInput.get().getID().equals(partition)) {
						treelikelihood[i] = likelihood;
					}
				}
			}

			switch (strs[1]) {
			case "site" :
				SiteModelInterface sitemodel = treelikelihood[0].siteModelInput.get();
				for (int i = 1; i < contexts.length; i++) {
					treelikelihood[i].siteModelInput.setValue(sitemodel, treelikelihood[i]);
					contexts[i].siteModel = contexts[0].siteModel;
				}
				break;
			case "clock" :
				BranchRateModel clockmodel = treelikelihood[0].branchRateModelInput.get();
				for (int i = 1; i < contexts.length; i++) {
					treelikelihood[i].branchRateModelInput.setValue(clockmodel, treelikelihood[i]);
					contexts[i].clockModel = contexts[0].clockModel;
				}
				break;
			case "tree" :
				TreeInterface tree = treelikelihood[0].treeInput.get();
				for (int i = 1; i < contexts.length; i++) {
					treelikelihood[i].treeInput.setValue(tree, treelikelihood[i]);
					contexts[i].tree = contexts[0].tree;
				}
				break;
			default:
				throw new IllegalArgumentException("Command " + cmdCount + ": expected 'link [site|clock|tree] but got " + cmd);
			}
		} else if (cmd.toLowerCase().startsWith("unlink")) {
			// 
		} else if (cmd.toLowerCase().startsWith("set")) {
			if (strs.length != 4) {
				throw new IllegalArgumentException("Command " + cmdCount + ": expected 'set <id pattern> =  <value>;' but got " + cmd);
			}

			// set <identifier> = <value>;
			String pattern = strs[1];
			String value = strs[3];
			
			
			Map<Input<?>, BEASTInterface> inputMap = getMatchingInputs(pattern + ".*.value");
			if (inputMap.size() == 0) {
				inputMap = getMatchingInputs(pattern);
			}
			for(Input<?> in : inputMap.keySet()) {
				BEASTInterface o = inputMap.get(in);
				in.setValue(value, o);
			}

			if (inputMap.size() == 0) {
				throw new IllegalArgumentException("Command " + cmdCount + ": cannot find suitable match for " + cmd);
			}
		} else {
			// assume this specifies a subtemplate
			// [<id pattern> =]? <SubTemplate>[(param1=value[,param2=value,...])];
			if (strs.length > 3) {
				throw new IllegalArgumentException("Command " + cmdCount + ": expected [<id pattern> =]? <SubTemplate>[(param1=value[,param2=value...])]; but got " + cmd);
			}
			String pattern;
			String subTemplateName;
			if (strs.length == 3) {
				pattern = strs[0];
				if (!strs[1].equals("=")) {
					throw new IllegalArgumentException("Command " + cmdCount + ": expected [<id pattern> =]? <SubTemplate>; but got " + cmd);
				}
				subTemplateName = strs[2];
			} else {
				// match anything
				pattern =".*";
				subTemplateName = strs[0];
			}
			
			// collect parameters
			List<String> param = new ArrayList<>();
			List<String> value = new ArrayList<>();
			if (partitionContext.size() != 1) {
				throw new IllegalArgumentException("Command " + cmdCount + ": partition context does not contain exactly 1 partition but " + partitionContext.size()  + " " + partitionContext.toString());
			}
			
			PartitionContext pc = partitionContext.toArray(new PartitionContext[]{})[0];
			String oldId = pc.partition;
			String id = pc.partition;
 			if (subTemplateName.indexOf('(') > -1) {
 				String parameters = subTemplateName.substring(subTemplateName.indexOf('(')+1, subTemplateName.lastIndexOf(')'));
				String [] x = parameters.split(",");
				for (String s : x) {
					String [] x2 = s.replaceAll("&44;",",").split("=");
					if (x2.length != 2) {
						throw new IllegalArgumentException("Command " + cmdCount + ": expected 'param=value' pair but got " + s + " in \n" +cmd);
					}
					if (x2[0].trim().toLowerCase().equals("id")) {
						id = x2[1].trim();
					} else {
						param.add(x2[0].trim());
						value.add(x2[1].trim());
					}
				}
				subTemplateName = subTemplateName.substring(0, subTemplateName.indexOf('('));
			}
			
			BEASTInterface bo = null;
    		
			pc.partition = id;
            for (BeautiSubTemplate subTemplate : doc.beautiConfig.subTemplates) {
            	if (subTemplate.getID().matches(subTemplateName)) {
            		bo = subTemplate.createSubNet(pc, true);
            		for (int i = 0; i < param.size(); i++) {
            			Input<?> in = bo.getInput(param.get(i));
            			if (in.get() instanceof Parameter.Base) {
            				Parameter.Base<?>  p = (Parameter.Base<?>) in.get();
            				p.valuesInput.setValue(value.get(i), p);
            			} else {
            				bo.setInputValue(param.get(i), value.get(i));
            			}
            		}
            	}
            }
			pc.partition = oldId;

            if (bo == null) {
				throw new IllegalArgumentException("Command " + cmdCount + ": cannot find template '" + subTemplateName + "'");
			}

			Map<Input<?>, BEASTInterface> inputMap = getMatchingInputs(pattern, bo);
			
			if (inputMap.size() == 0) {
				throw new IllegalArgumentException("Command " + cmdCount + ": cannot find suitable match for " + cmd);
			} else {
				doc.scrubAll(false, false);
			}

			for(Input<?> in : inputMap.keySet()) {
				BEASTInterface o = inputMap.get(in);
				if (o instanceof CompoundDistribution && in.getName().equals("distribution") && bo instanceof TreeDistribution) {
					// may need to replace existing distribution
					CompoundDistribution dist = (CompoundDistribution) o;
					Distribution treeDist = null;
					Alignment a = doc.getPartition(bo);
					for (Distribution d : dist.pDistributions.get()) {
						if (d instanceof TreeDistribution && doc.getPartition(d).equals(a)) {
							treeDist = d;
						}
					}
					if (treeDist != null) {
						dist.pDistributions.get().remove(treeDist);
					}
				}
				if (in.get() instanceof Collection<?>) {
					boolean found = false;
					for (Object o2 : (Collection<?>) in.get()) {
						if (o2 == bo) {
							found = true;
						}
					}
					if (!found) {
						in.setValue(bo, o);
					}
				} else {
					in.setValue(bo, o);
				}
			}
			
		}
	}

	
	private String[] cmdsplit(String cmd) {
		if (cmd.indexOf("'") >= 0) {
			List<String> strs = new ArrayList<>();
			StringBuilder b = new StringBuilder();
			boolean markSpaces = false;
			for (int i = 0; i < cmd.length(); i++) {
				char c = cmd.charAt(i);
				if (c == '\'') {
					markSpaces = !markSpaces;
				} else if (Character.isWhitespace(c) && !markSpaces && b.length() > 0) {
					strs.add(b.toString());
					b = new StringBuilder();
				} else if (c == ',' && markSpaces) {
					b.append("&44;");
				} else {
					b.append(c);
				} 
			}
			if (b.length() > 0) {
				strs.add(b.toString());				
			}
			return strs.toArray(new String[]{});
		}
		String [] strs = cmd.split("\\s+");
		return strs;
	}

	private Map<Input<?>, BEASTInterface> getMatchingInputs(String pattern) {
		Map<Input<?>, BEASTInterface> inputMap = new LinkedHashMap<>();
		for (String id : doc.pluginmap.keySet()) {
			BEASTInterface o = doc.pluginmap.get(id);
			for (String name : o.getInputs().keySet()) {
				if ((id + "." + name).matches(pattern)) { // <=== match id + name
					Input<?> in = o.getInputs().get(name);
					testMatch(o, in, inputMap);
				}
			}
		}
		if (inputMap.size() == 0) {
			// no match found -- try matching id only
			for (String id : doc.pluginmap.keySet()) {
				BEASTInterface o = doc.pluginmap.get(id);
				for (String name : o.getInputs().keySet()) {
					if ((id).matches(pattern)) { // <=== match id only
						Input<?> in = o.getInputs().get(name);
						testMatch(o, in, inputMap);
					}
				}
			}
		}
		if (inputMap.size() == 0) {
			// no match found -- try matching input name only
			for (String id : doc.pluginmap.keySet()) {
				BEASTInterface o = doc.pluginmap.get(id);
				for (String name : o.getInputs().keySet()) {
					if ((name).matches(pattern)) { // <=== match name only
						Input<?> in = o.getInputs().get(name);
						testMatch(o, in, inputMap);
					}
				}
			}
		}
		return inputMap;
	}

	private void testMatch(BEASTInterface o, Input<?> in, Map<Input<?>, BEASTInterface> inputMap) {
		if (in.get() instanceof Parameter.Base) {
			BEASTInterface o2 = (BEASTInterface) in.get();
			Input<?> in2 = ((Parameter.Base<?>) o).valuesInput;
			if (in.getType() != null && isAssignableFromString(in.getType())) {
				inputMap.put(in2, o2);
			} else if (in.getType() != null && isAssignableFromString(in.getType())) {
				inputMap.put(in, o);
			}
			return;
		}
		if (in.getType() == null) {
			in.determineClass(o);
		}
		
		if (in.getType() != null && isAssignableFromString(in.getType())) {
			inputMap.put(in, o);
		}
	}

	private boolean isAssignableFromString(Class<?> type) {
		if (String.class.isAssignableFrom(type) ||
				Boolean.class.isAssignableFrom(type) ||
				Number.class.isAssignableFrom(type)) {
			return true;
		}
		return false;
	}

	private void importData(String[] strs) {
		String providerID = "Import Alignment";
		if (strs.length > 2) {
			providerID = ".*" + strs[1];
			for (int i = 2; i < strs.length - 1; i++) {
				providerID += " " + strs[i];
			}
			providerID += ".*";
		}
		
		List<BeautiAlignmentProvider> providerList = doc.beautiConfig.alignmentProvider;
		BeautiAlignmentProvider provider = null;
		for (BeautiAlignmentProvider p : providerList) {
			if (p.getID().matches(providerID)) {
				provider = p;
			}
		}
		if (provider == null) {
			String providers = providerList.get(0).getID();
			for (int i = 1; i < providerList.size(); i++) {
				providers += "," + providerList.get(i).getID();
			}
			throw new IllegalArgumentException("Could not match '" + providerID+"' to one of these providers: " + providers);
		}
		
		//provider.template.setValue(doc.beautiConfig.partitionTemplate.get(), provider);
        List<BEASTInterface> beastObjects = provider.getAlignments(doc, new File[]{new File(strs[strs.length - 1])});
        if (!provider.getClass().equals(BeautiAlignmentProvider.class)) {
            provider.addAlignments(doc, beastObjects);
        }

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
        // set partition context to latest partition
		PartitionContext p = doc.partitionNames.get(doc.partitionNames.size() - 1);
		partitionContext.clear();
		partitionContext.add(new PartitionContext(p.partition, p.siteModel, p.clockModel, p.tree));
	}

	private Map<Input<?>, BEASTInterface> getMatchingInputs(String pattern, BEASTInterface bo) {
		Map<Input<?>, BEASTInterface> inputMap = new LinkedHashMap<>();
		if (bo instanceof Distribution) {
			CompoundDistribution distr = (CompoundDistribution) doc.pluginmap.get("prior");
			inputMap.put(distr.pDistributions, distr);
			return inputMap;
		}
		
		for (String id : doc.pluginmap.keySet()) {
			BEASTInterface o = doc.pluginmap.get(id);
			for (String name : o.getInputs().keySet()) {
				if ((id + "." + name).matches(pattern)) { // <=== match id + name
					Input<?> in = o.getInputs().get(name);
					if (in.getType() != null && in.getType().isAssignableFrom(bo.getClass()) && in.canSetValue(bo, o)) {
						inputMap.put(in, o);
					}
				}
			}
		}
		if (inputMap.size() == 0) {
			// no match found -- try matching id only
			for (String id : doc.pluginmap.keySet()) {
				BEASTInterface o = doc.pluginmap.get(id);
				for (String name : o.getInputs().keySet()) {
					if ((id).matches(pattern)) { // <=== match id only
						Input<?> in = o.getInputs().get(name);
						if (in.getType() != null && in.getType().isAssignableFrom(bo.getClass()) && in.canSetValue(bo, o)) {
							inputMap.put(in, o);
						}
					}
				}
			}
		}
		return inputMap;
	}

	
	
	public static void main(String[] args) {
		String cmds0 = "import ../morph-models/examples/nexus/penguins_dna.nex;" +
				"import Morph ../morph-models/examples/nexus/penguins_morph.nex;";
		
		String cmds = "template Standard;\n" +
				"import Alignment ../beast2/examples/nexus/primate-mtDNA.nex;"
				+ "partition .*;"
				+ "link tree;"
		;

		String cmds1 = "template Standard;\n" +
				"import Alignment ../beast2/examples/nexus/dna.nex;"
				+ "MultiMonoConstraint(id=c1,newick='(Cow,Carp,Human,Whale)');"
				+ "partition dna;"
				+ "MultiMonoConstraint(id=c2,newick='((Cow,Carp),(Human,Whale))');"
//				+ "'Gamma Site Model(gammaCategoryCount=4)';set gammaShape.*estimate = true;"
//				+ "HKY(kappa=3.0);"
//				+ "prior = CoalescentConstantPopulation;"
//				+ "PopSizePrior.*distr = Gamma(alpha=0.1,beta=10.0)"
//				+ "'BEAST Model Test';"
//				+ "RelaxedClockLogNormal;"
//				+ "set birthRate.*upper = 2.5;"
//				+ "set clockRate = 1e-5;"
//				+ "set chainLength = 999999;"
//				+ "set tree.*fileName = dna.trees;"
//				+ "set logEvery = 10000;"
		;
		CompactAnalysisSpec analysis = new CompactAnalysisSpec(cmds);
		analysis.initAndValidate();
		XMLProducer xmlProducer = new XMLProducer();
		MCMC mcmc = (MCMC) analysis.doc.mcmc.get();
		System.out.println(xmlProducer.toXML(mcmc));
	}
	
	
//	static public void main(String args[]) {
//		// create a scanner so we can read the command-line input
//	    Scanner scanner = new Scanner(System.in);
//
//	    //  prompt for the user's name
//
//	    // get their input as a String
//	    while (true) {
//		    System.out.print("> ");
//	    	String username = scanner.next();
//	    	if (username == null) break;
//			System.out.println(username);
//	    }
//	}
}
