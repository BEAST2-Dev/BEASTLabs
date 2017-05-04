package beast.app.beauti;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
//import org.antlr.v4.runtime.tree.ParseTreeWalker;

import beast.app.beauti.BeautiConfig;
import beast.app.beauti.BeautiDoc;
import beast.app.beauti.PartitionContext;
import beast.app.beauti.compactanalysis.CABaseListener;
import beast.app.beauti.compactanalysis.CABaseVisitor;
import beast.app.beauti.compactanalysis.CALexer;
import beast.app.beauti.compactanalysis.CAParser;
import beast.app.beauti.compactanalysis.CAParser.ArgContext;
import beast.app.beauti.compactanalysis.CAParser.CasentenceContext;
import beast.app.beauti.compactanalysis.CAParser.FilenameContext;
import beast.app.beauti.compactanalysis.CAParser.IdpatternContext;
import beast.app.beauti.compactanalysis.CAParser.Import_Context;
import beast.app.beauti.compactanalysis.CAParser.KeyContext;
import beast.app.beauti.compactanalysis.CAParser.LinkContext;
import beast.app.beauti.compactanalysis.CAParser.SetContext;
import beast.app.beauti.compactanalysis.CAParser.SubtemplateContext;
import beast.app.beauti.compactanalysis.CAParser.TemplateContext;
import beast.app.beauti.compactanalysis.CAParser.TemplatenameContext;
import beast.app.beauti.compactanalysis.CAParser.UnlinkContext;
import beast.core.BEASTInterface;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.parameter.Parameter;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.sitemodel.SiteModelInterface;
import beast.evolution.tree.TreeDistribution;
import beast.evolution.tree.TreeInterface;
import beast.math.distributions.MRCAPrior;

public class CompactAnalysisByAntlr extends CABaseListener {

	public void enterCAsentence(CasentenceContext ctx) {
		System.out.println(ctx.getText());
	}
	
	
	void parseCA(String CASentence) {
        // Custom parse/lexer error listener
        BaseErrorListener errorListener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                throw new CAParsingException(msg, charPositionInLine, line);
            }
        };

        // Get our lexer
	    CALexer lexer = new CALexer(new ANTLRInputStream(CASentence));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

	    // Get a list of matched tokens
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	 
	    // Pass the tokens to the parser
	    CAParser parser = new CAParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
	 
        ParseTree parseTree = parser.casentence();
//	    // Specify our entry point
//	    CasentenceContext CASentenceContext = parser.casentence();
//	 
//	    // Walk it and attach our listener
//	    ParseTreeWalker walker = new ParseTreeWalker();
//	    AntlrCompactAnalysisListener listener = new AntlrCompactAnalysisListener();
//	    walker.walk(listener, CASentenceContext);


        // Traverse parse tree, constructing BEAST tree along the way
        CAASTVisitor visitor = new CAASTVisitor();

        visitor.visit(parseTree);
	}
	

	public class CAASTVisitor extends CABaseVisitor<BEASTInterface> {
		
		private Set<PartitionContext> partitionContext;
		BeautiDoc doc;
		
		public CAASTVisitor() {
			partitionContext = new LinkedHashSet<>();
			doc = new BeautiDoc();
			doc.beautiConfig = new BeautiConfig();
			doc.beautiConfig.initAndValidate();
		}
		
		@Override
		public BEASTInterface visitTemplate(TemplateContext ctx) {
			TemplatenameContext name = (TemplatenameContext) ctx.children.get(ctx.children.size() - 1);

			String template = name.getText();
			if (!template.toLowerCase().endsWith("xml")) {
				template += ".xml";
			}
			doc.loadNewTemplate(template);
			
			return super.visitTemplate(ctx);
		}
		
		
		@Override
		public BEASTInterface visitPartition(beast.app.beauti.compactanalysis.CAParser.PartitionContext ctx) {
			String pattern = ctx.getChild(1).getText();
			partitionContext.clear();
			for (PartitionContext p : doc.partitionNames) {
				if (p.partition.matches(pattern)) {
					partitionContext.add(new PartitionContext(p.partition, p.siteModel, p.clockModel, p.tree));
				}
			}
			return super.visitPartition(ctx);
		}
		
		@Override
		public BEASTInterface visitImport_(Import_Context ctx) {
			String providerID = "Import Alignment";
			String fileName = null;
			List<String> args = new ArrayList<>();
			if (ctx.getChild(1) instanceof FilenameContext) {
				fileName = ctx.getChild(1).getText(); 
				for (int i = 2; i < ctx.children.size(); i++) {
					if (ctx.getChild(i) instanceof ArgContext) {
						args.add(ctx.getChild(i).getText());
					}
				}
			} else {
				providerID = ctx.getChild(1).getText();
				if (providerID.startsWith("'") && providerID.endsWith("'")) {
					providerID = providerID.substring(1, providerID.length()-1);
				}
				providerID = ".*" + providerID + ".*";
				fileName = ctx.getChild(2).getText();
				for (int i = 3; i < ctx.children.size(); i++) {
					if (ctx.getChild(i) instanceof ArgContext) {
						args.add(ctx.getChild(i).getText());
					}
				}
			}
						
			List<BeautiAlignmentProvider> providerList = doc.beautiConfig.alignmentProvider;
			BeautiAlignmentProvider provider = null;
			for (BeautiAlignmentProvider p : providerList) {
				if (p.getID().matches(providerID)) {
					provider = p;
					break;
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
	        List<BEASTInterface> beastObjects = provider.getAlignments(doc, new File[]{new File(fileName)}, args.toArray(new String[]{}));
//	        if (!provider.getClass().equals(BeautiAlignmentProvider.class)) {
//	            provider.addAlignments(doc, beastObjects);
//	        }

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

			doc.scrubAll(true, false);
			return null;
		}
		
		@Override
		public BEASTInterface visitLink(LinkContext ctx) {
			if (partitionContext.size() <= 1) {
				throw new IllegalArgumentException("Link command : At least two partitions must be selected before '" + ctx.getText() + "'");
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

			switch (ctx.getChild(1).getText()) {
			case "site" :
				SiteModelInterface sitemodel = treelikelihood[0].siteModelInput.get();
				for (int i = 1; i < contexts.length; i++) {
					PartitionContext oldContext = new PartitionContext(treelikelihood[i]);

					SiteModelInterface oldSiteModel = treelikelihood[i].siteModelInput.get();
					for (Object beastObject : BEASTInterface.getOutputs(oldSiteModel).toArray()) { //.toArray(new BEASTInterface[0])) {
						for (Input<?> input : ((BEASTInterface)beastObject).listInputs()) {
							try {
							if (input.get() == oldSiteModel) {
								if (input.getRule() != Input.Validate.REQUIRED) {
									input.setValue(sitemodel /*null*/, (BEASTInterface) beastObject);
								//} else {
									//input.setValue(tree, (BEASTInterface) beastObject);
								}
							} else if (input.get() instanceof List) {
								List list = (List) input.get();
								if (list.contains(oldSiteModel)) { // && input.getRule() != Validate.REQUIRED) {
									list.remove(oldSiteModel);
									if (!list.contains(sitemodel)) {
										list.add(sitemodel);
									}
								}
							}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
					treelikelihood[i].siteModelInput.setValue(sitemodel, treelikelihood[i]);
					contexts[i].siteModel = contexts[0].siteModel;
					repartition(oldContext);
				}
				break;
			case "clock" :
				BranchRateModel clockmodel = treelikelihood[0].branchRateModelInput.get();
				for (int i = 1; i < contexts.length; i++) {
					PartitionContext oldContext = new PartitionContext(treelikelihood[i]);

					BranchRateModel oldClock = treelikelihood[i].branchRateModelInput.get();
					for (Object beastObject : BEASTInterface.getOutputs(oldClock).toArray()) { //.toArray(new BEASTInterface[0])) {
						for (Input<?> input : ((BEASTInterface)beastObject).listInputs()) {
							try {
							if (input.get() == oldClock) {
								if (input.getRule() != Input.Validate.REQUIRED) {
									input.setValue(clockmodel /*null*/, (BEASTInterface) beastObject);
								//} else {
									//input.setValue(tree, (BEASTInterface) beastObject);
								}
							} else if (input.get() instanceof List) {
								List list = (List) input.get();
								if (list.contains(oldClock)) { // && input.getRule() != Validate.REQUIRED) {
									list.remove(oldClock);
									if (!list.contains(clockmodel)) {
										list.add(clockmodel);
									}
								}
							}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
					treelikelihood[i].branchRateModelInput.setValue(clockmodel, treelikelihood[i]);
					contexts[i].clockModel = contexts[0].clockModel;
					repartition(oldContext);
				}
				break;
			case "tree" :
				TreeInterface tree = treelikelihood[0].treeInput.get();
				for (int i = 1; i < contexts.length; i++) {
					PartitionContext oldContext = new PartitionContext(treelikelihood[i]);

					TreeInterface oldTree = treelikelihood[i].treeInput.get();
					treelikelihood[i].treeInput.setValue(tree, treelikelihood[i]);
					contexts[i].tree = contexts[0].tree;
					
	            	// use toArray to prevent ConcurrentModificationException
					for (Object beastObject : BEASTInterface.getOutputs(oldTree).toArray()) { //.toArray(new BEASTInterface[0])) {
						for (Input<?> input : ((BEASTInterface)beastObject).listInputs()) {
							try {
							if (input.get() == oldTree) {
								if (input.getRule() != Input.Validate.REQUIRED) {
									input.setValue(tree/*null*/, (BEASTInterface) beastObject);
								//} else {
									//input.setValue(tree, (BEASTInterface) beastObject);
								}
							} else if (input.get() instanceof List) {
								@SuppressWarnings("unchecked")
								List<TreeInterface> list = (List<TreeInterface>) input.get();
								if (list.contains(oldTree)) { // && input.getRule() != Validate.REQUIRED) {
									list.remove(oldTree);
									if (!list.contains(tree)) {
										list.add(tree);
									}
								}
							}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					repartition(oldContext);

				}
				break;
			default:
				throw new IllegalArgumentException("Command link: expected 'link [site|clock|tree] but got " + ctx.getText());
			}
			doc.determinePartitions();
			doc.scrubAll(true, false);
			return null;
		}
		
		@Override
		public BEASTInterface visitUnlink(UnlinkContext ctx) {
				if (partitionContext.size() <= 1) {
					throw new IllegalArgumentException("Command unlink: At least two partitions must be selected " + ctx.getText());
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

				switch (ctx.getChild(1).getText()) {
				case "site" :
					SiteModelInterface sitemodel = treelikelihood[0].siteModelInput.get();
					for (int i = 1; i < contexts.length; i++) {
						PartitionContext oldContext = new PartitionContext(treelikelihood[i]);
						contexts[i].siteModel = contexts[i].partition;

						SiteModelInterface newSitemodel = (SiteModelInterface) BeautiDoc.deepCopyPlugin((BEASTInterface) sitemodel, treelikelihood[i], (MCMC) doc.mcmc.get(), oldContext, contexts[i], doc, new ArrayList<>());
						treelikelihood[i].siteModelInput.setValue(newSitemodel, treelikelihood[i]);
						repartition(contexts[i]);
					}
					break;
				case "clock" :
					BranchRateModel clockModel = treelikelihood[0].branchRateModelInput.get();
					for (int i = 1; i < contexts.length; i++) {
						PartitionContext oldContext = new PartitionContext(treelikelihood[i]);
						contexts[i].clockModel = contexts[i].partition;

						BranchRateModel newClockmodel = (BranchRateModel) BeautiDoc.deepCopyPlugin((BEASTInterface) clockModel, treelikelihood[i], (MCMC) doc.mcmc.get(), oldContext, contexts[i], doc, new ArrayList<>());
						treelikelihood[i].siteModelInput.setValue(newClockmodel, treelikelihood[i]);
						repartition(contexts[i]);
					}
					break;
				case "tree" :
					TreeInterface tree = treelikelihood[0].treeInput.get();
					for (int i = 1; i < contexts.length; i++) {
						PartitionContext oldContext = new PartitionContext(treelikelihood[i]);
						contexts[i].tree = contexts[i].partition;

						TreeInterface newTree = (TreeInterface) BeautiDoc.deepCopyPlugin((BEASTInterface) tree, treelikelihood[i], (MCMC) doc.mcmc.get(), oldContext, contexts[i], doc, new ArrayList<>());
						treelikelihood[i].treeInput.setValue(newTree, treelikelihood[i]);
						repartition(contexts[i]);
					}
					break;
				default:
					throw new IllegalArgumentException("Command unlink: expected 'unlink [site|clock|tree] but got " + ctx.getText());
				}
				doc.determinePartitions();
				doc.scrubAll(true, false);
				return null;
			}
		
		@Override
		public BEASTInterface visitSet(SetContext ctx) {
			// set <identifier> = <value>;
			String pattern = ctx.getChild(1).getText();
			String value = ctx.getChild(3).getText();;
			
			
			Map<Input<?>, BEASTInterface> inputMap = getMatchingInputs(pattern + ".*.value");
			if (inputMap.size() == 0) {
				inputMap = getMatchingInputs(pattern);
			}
			for(Input<?> in : inputMap.keySet()) {
				BEASTInterface o = inputMap.get(in);
				in.setValue(value, o);
			}

			if (inputMap.size() == 0) {
				throw new IllegalArgumentException("Command set: cannot find suitable match for " + ctx.getText());
			}
			return null;
		}

		@Override
		public BEASTInterface visitSubtemplate(SubtemplateContext ctx) {
			// assume this specifies a subtemplate
			// [<id pattern> =]? <SubTemplate>[(param1=value[,param2=value,...])];
			String pattern;
			String subTemplateName;
			if (ctx.getChild(1) instanceof IdpatternContext) {
				pattern = ctx.getChild(1).getText();
				subTemplateName = ctx.getChild(3).getText();
			} else {
				// match anything
				pattern =".*";
				subTemplateName = ctx.getChild(1).getText();
			}
			
			// collect parameters
			List<String> param = new ArrayList<>();
			List<String> value = new ArrayList<>();
			if (partitionContext.size() != 1) {
				throw new IllegalArgumentException("Command sub: partition context does not contain exactly 1 partition but " + partitionContext.size()  + " " + partitionContext.toString());
			}
			
			PartitionContext pc = partitionContext.toArray(new PartitionContext[]{})[0];
			String oldId = pc.partition;
			String id = pc.partition;

			for (int i = 2; i < ctx.getChildCount(); i++) {
				if (ctx.getChild(i) instanceof KeyContext) {
					String key = ctx.getChild(i).getText();
					String val = ctx.getChild(i + 2).getText();
					
					if (key.trim().toLowerCase().equals("id")) {
						id = val.trim();
					} else {
						param.add(key.trim());
						value.add(val.trim());
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
				throw new IllegalArgumentException("Command sub: cannot find template '" + subTemplateName + "'");
			}

			Map<Input<?>, BEASTInterface> inputMap = getMatchingInputs(pattern, bo);
			
			if (inputMap.size() == 0) {
				throw new IllegalArgumentException("Command sub: cannot find suitable match for " + ctx.getText());
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
			return null;
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

		private void repartition(PartitionContext oldContext) {
			List<BeautiSubTemplate> templates = new ArrayList<>();
			templates.add(doc.beautiConfig.partitionTemplate.get());
			templates.addAll(doc.beautiConfig.subTemplates);
			// keep applying rules till model does not change
			doc.setUpActivePlugins();
			int n;
			do {
				n = doc.posteriorPredecessors.size();
				doc.applyBeautiRules(templates, false, oldContext);
				doc.setUpActivePlugins();
			} while (n != doc.posteriorPredecessors.size());
			doc.determinePartitions();		
		}
	}
	
	
	
	
    public class CAParsingException extends RuntimeException {
        String message;
        Integer characterNum, lineNum;

        /**
         * Create new parsing exception.
         *
         * @param message      Human-readable error message.
         * @param characterNum Character offset of error.
         * @param lineNum      Line offset of error.
         */
        CAParsingException(String message, Integer characterNum, Integer lineNum) {
            this.message = message;
            this.characterNum = characterNum;
            this.lineNum = lineNum;
        }

        /**
         * Create new parsing exception
         *
         * @param message Human-readable error message.
         */
        CAParsingException(String message) {
            this(message, null, null);
        }

        @Override
        public String getMessage() {
            return message + " line " + lineNum + " character " + characterNum;
        }

        /**
         * @return location of error on line.  (May be null for non-lexer errors.)
         */
        public Integer getCharacterNum() {
            return characterNum;
        }

        /**
         * @return line number offset of error. (May be null for non-lexer errors.)
         */
        public Integer getLineNum() {
            return lineNum;
        }
    }

	public static void main(String[] args) {
		String cmds = "template Standard;\n" +
				"import ../beast2/examples/nexus/Primates.nex;"
				+ "partition .*;" 
				+ "link site;";
;
		
		String cmds1 = "template Standard;\n" +
		"import ../beast-geo/examples/nexus/HBV.nex;\n" + 
		"import 'Spherical Geography' ../beast-geo/examples/nexus/HBV_locations.dat(geo,HBV);";
//		"partition dna;\n" +
//		"link clock dna;\n" +
//		"unlink site dna;\n" + 
//		"set gamma = 0.1;\n" + 
//		"sub RelaxedClock;";
//
//		String cmds2 = "template Standar;\nimport ../beast-geo/examples/nexus/HBV.nex;" +
//				"import Spherical Geography ../beast-geo/examples/nexus/HBV_locations.dat(geo,HBV);";
		
		
		CompactAnalysisByAntlr x = new CompactAnalysisByAntlr();
		x.parseCA(cmds);
		System.out.println("Done");

	}
}
