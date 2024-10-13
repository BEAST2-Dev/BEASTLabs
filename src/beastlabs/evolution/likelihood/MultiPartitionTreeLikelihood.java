/*
 * MultiPartitionDataLikelihoodDelegate.java
 *
 * Copyright © 2002-2024 the BEAST Development Team
 * http://beast.community/about
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 *
 */

package beastlabs.evolution.likelihood;


import beagle.*;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.evolution.branchratemodel.StrictClockModel;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.likelihood.BeagleTreeLikelihood.PartialsRescalingScheme;
import beast.base.evolution.likelihood.TreeLikelihood.Scaling;
import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.EigenDecomposition;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Distribution;
import beast.base.inference.State;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Adapted from MultiPartitionDataLikelihoodDelegate in BEAST X
 *
 * A DataLikelihoodDelegate that uses BEAGLE 3 to allow for parallelization across multiple data partitions
 *
 * @author Andrew Rambaut
 * @author Marc Suchard
 * @author Guy Baele
 */
@Description("A DataLikelihoodDelegate that uses BEAGLE 3 to allow for parallelization across multiple data partitions")
public class MultiPartitionTreeLikelihood extends Distribution {
	
    final public Input<List<GenericTreeLikelihood>> likelihoodsInput = new Input<>("distribution", "tree likelilhood for each of the partitions", new ArrayList<>(), Validate.REQUIRED);


    boolean needsUpdate;
    	
    private static final boolean COUNT_CALCULATIONS = true; // keep a cumulative total of number of computations
    private static final boolean COUNT_TOTAL_OPERATIONS = true;
    private static final long MAX_UNDERFLOWS_BEFORE_ERROR = 100;

    private static final boolean RESCALING_OFF = false; // a debugging switch
    private static final boolean DEBUG = false;
    static int instanceCount;
    
    
    private double [][] lastKnownFrequencies;
    private double [][] lastKnownCategorieWeights;
    private double [][] lastKnownCategorieRates;
    
    public int matrixUpdateCount;
    public int partialUpdateCount;
    
    public static boolean IS_MULTI_PARTITION_RECOMMENDED() {
        if (!IS_MULTI_PARTITION_COMPATIBLE()) {
            return false;
        }

        String resourceAuto = System.getProperty(RESOURCE_AUTO_PROPERTY);
        if (resourceAuto != null && Boolean.parseBoolean(resourceAuto)) {
            return true;
        }

        fetchBeagleSettings();

        int index = resourceOrder.size() > 0 ? instanceCount % resourceOrder.size() : 0;

        if (resourceOrder.size() > 0 && resourceOrder.get(index) > 0) {
            return true;
        }
        if (requiredOrder.size() > 0 &&
                ((requiredOrder.get(index) & BeagleFlag.PROCESSOR_GPU.getMask()) != 0 ||
                        (requiredOrder.get(index) & BeagleFlag.FRAMEWORK_CUDA.getMask()) != 0 ||
                        (requiredOrder.get(index) & BeagleFlag.FRAMEWORK_OPENCL.getMask()) != 0)) {
            return true;
        }
        if (preferredOrder.size() > 0 &&
                ((preferredOrder.get(index) & BeagleFlag.PROCESSOR_GPU.getMask()) != 0 ||
                        (preferredOrder.get(index) & BeagleFlag.FRAMEWORK_CUDA.getMask()) != 0 ||
                        (preferredOrder.get(index) & BeagleFlag.FRAMEWORK_OPENCL.getMask()) != 0)) {
            return true;
        }

        return false;
    }

    // This property is a comma-delimited list of resource numbers (0 == CPU) to
    // allocate each BEAGLE instance to. If less than the number of instances then
    // will wrap around.
    private static final String RESOURCE_AUTO_PROPERTY = "beagle.resource.auto";
    private static final String RESOURCE_ORDER_PROPERTY = "beagle.resource.order";
    private static final String PREFERRED_FLAGS_PROPERTY = "beagle.preferred.flags";
    private static final String REQUIRED_FLAGS_PROPERTY = "beagle.required.flags";
    private static final String SCALING_PROPERTY = "beagle.scaling";
    private static final String RESCALE_FREQUENCY_PROPERTY = "beagle.rescale";
    private static final String DELAY_SCALING_PROPERTY = "beagle.delay.scaling";
    private static final String EXTRA_BUFFER_COUNT_PROPERTY = "beagle.extra.buffer.count";
    private static final String FORCE_VECTORIZATION = "beagle.force.vectorization";
    private static final String THREAD_COUNT = "beagle.thread.count";


    // Which scheme to use if choice not specified (or 'default' is selected):
    private static final PartialsRescalingScheme DEFAULT_RESCALING_SCHEME = PartialsRescalingScheme.DYNAMIC;

    private static List<Integer> resourceOrder = null;
    private static List<Integer> preferredOrder = null;
    private static List<Integer> requiredOrder = null;
    private static List<String> scalingOrder = null;
    private static List<Integer> extraBufferOrder = null;

    private static void fetchBeagleSettings() {
        // Attempt to get the resource order from the System Property
        if (resourceOrder == null) {
            resourceOrder = parseSystemPropertyIntegerArray(RESOURCE_ORDER_PROPERTY);
        }
        if (preferredOrder == null) {
            preferredOrder = parseSystemPropertyIntegerArray(PREFERRED_FLAGS_PROPERTY);
        }
        if (requiredOrder == null) {
            requiredOrder = parseSystemPropertyIntegerArray(REQUIRED_FLAGS_PROPERTY);
        }
        if (scalingOrder == null) {
            scalingOrder = parseSystemPropertyStringArray(SCALING_PROPERTY);
        }
        if (extraBufferOrder == null) {
            extraBufferOrder = parseSystemPropertyIntegerArray(EXTRA_BUFFER_COUNT_PROPERTY);
        }
    }


    // Default frequency for complete recomputation of scaling factors under the 'dynamic' scheme
    private static final int RESCALE_FREQUENCY = 100;
    private static final int RESCALE_TIMES = 1;

    // count the number of partial likelihood and matrix updates
    public long totalMatrixUpdateCount = 0;
    public long totalPartialsUpdateCount = 0;
    public long totalEvaluationCount = 0;

    private TreeInterface tree;
    private BranchRateModel branchRateModel;
    
	@Override
	public void initAndValidate() {
		List<Alignment> Alignments = new ArrayList<>();
        // List<BranchRateModel> branchModels = new ArrayList<>();
        siteRateModels = new ArrayList<>();
        
        GenericTreeLikelihood tl0 = likelihoodsInput.get().get(0);
        tree = tl0.treeInput.get();
        branchRateModel = tl0.branchRateModelInput.get();
        boolean useAmbiguities = (boolean) tl0.getInput("useAmbiguities").get();
        boolean useTipLikelihoods = (boolean) tl0.getInput("useTipLikelihoods").get();
        rescalingScheme = getRescalingScheme(tl0);

        for (GenericTreeLikelihood tl : likelihoodsInput.get()) {
        	if (tree != tl.treeInput.get()) {
        		throw new IllegalArgumentException("Tree of likelihood " + tl.getID() + " does not match tree in " + likelihoodsInput.get().get(0).getID() +"\n"
        				+ "All likelihoods must share the same tree.");
        	}
        	
        	if (tl.branchRateModelInput.get() != null && branchRateModel != tl.branchRateModelInput.get()) {
        		throw new IllegalArgumentException("Tree of likelihood " + tl.getID() + " does not match branch rate model in " + likelihoodsInput.get().get(0).getID() +"\n"
        				+ "All likelihoods must share the same branch rate model.");
        	}
        	
        	if (useAmbiguities != (boolean)tl.getInput("useAmbiguities").get()) {
        		throw new IllegalArgumentException("All partitions must use ambiguities, or ignore ambiguities, but found a difference between " +
        				tl.getID() + " and " + likelihoodsInput.get().get(0).getID());
        	}
        	if (useTipLikelihoods != (boolean)tl.getInput("useTipLikelihoods").get()) {
        		throw new IllegalArgumentException("All partitions must use tip likelihoods, or ignore tip likelihoods, but found a difference between " +
        				tl.getID() + " and " + likelihoodsInput.get().get(0).getID());
        	}
        	
        	Alignments.add(tl.dataInput.get());
        	//branchModels.add(tl.branchRateModelInput.get());
        	siteRateModels.add((SiteModel) tl.siteModelInput.get());
        	
        	if (!rescalingScheme.equals(getRescalingScheme(tl))) {
        		throw new IllegalArgumentException("Tree of likelihood " + tl.getID() + " does not match scaling scheme of " + likelihoodsInput.get().get(0).getID() +"\n"
        				+ "All scaling must be the same.");
        	}
        }
        
        if (branchRateModel == null) {
        	branchRateModel = new StrictClockModel();
        }
		
        try {
        	initialise(tree, Alignments, /*branchModels,*/ siteRateModels, useAmbiguities, useTipLikelihoods, rescalingScheme, delayRescalingUntilUnderflow);
        } catch (DelegateTypeException e) {
        	e.printStackTrace();
        }
	}

	private PartialsRescalingScheme getRescalingScheme(GenericTreeLikelihood tl0) {
		PartialsRescalingScheme rescalingScheme = DEFAULT_RESCALING_SCHEME;
        String scaling = null;
        try {
        	scaling = tl0.getInputValue("scaling").toString();
        } catch (IllegalArgumentException e) {
        	// tl0 has no input with name "scaling", 
        }
        if (scaling != null && scaling.equals(Scaling.always.toString())) {
        	rescalingScheme = PartialsRescalingScheme.ALWAYS;
        }
        if (scaling != null && scaling.equals(Scaling.none.toString())) {
        	rescalingScheme = PartialsRescalingScheme.NONE;
        }
		return rescalingScheme;
	}

	/**
     * Construct an instance using a list of Alignments, one for each partition. The
     * partitions will share a tree but can have different branchModels and siteRateModels
     * The latter should either have a size of 1 (in which case they are shared across partitions)
     * or equal to Alignments.size() where each partition has a different model.
     *
     * @param tree Used for configuration - shouldn't be watched for changes
     * @param branchModels Specifies a list of branch models for each partition
     * @param Alignments List of Alignments comprising each partition
     * @param siteRateModels A list of siteRateModels for each partition
     * @param useAmbiguities Whether to respect state ambiguities in data
     */
    
    
    public void initialise(TreeInterface tree,
                                                List<Alignment> Alignments,
                                                //List<BranchRateModel> branchModels,
                                                List<SiteModel> siteRateModels,
                                                boolean useAmbiguities,
                                                boolean useTipLikelihoods,
                                                PartialsRescalingScheme rescalingScheme,
                                                boolean delayRescalingUntilUnderflow)
                                                  throws DelegateTypeException {


        //setID(Alignments.get(0).getID());

        this.Alignments = Alignments;
        this.dataType = Alignments.get(0).getDataType();
        stateCount = dataType.getStateCount();

        partitionCount = Alignments.size();
        patternCounts = new int[partitionCount];
        int total = 0;
        int k = 0;
        for (Alignment Alignment : Alignments) {
            assert(Alignment.getDataType().equals(this.dataType));
            patternCounts[k] = Alignment.getPatternCount();
            total += patternCounts[k];
            k++;
        }
        totalPatternCount = total;

        useScaleFactors = new boolean[partitionCount];
        recomputeScaleFactors = new boolean[partitionCount];
        everUnderflowed = new boolean[partitionCount];
        flip = new boolean[partitionCount];
        for (int i = 0; i < partitionCount; i++) {
            flip[i] = true;
        }

        updatePartition = new boolean[partitionCount];
        partitionWasUpdated = new boolean[partitionCount];
        updateAllPartitions = true;

        cachedLogLikelihoodsByPartition = new double[partitionCount];
        storedCachedLogLikelihoodsByPartition = new double[partitionCount];

        // Branch models determine the substitution models per branch. There can be either
        // one per partition or one shared across all partitions
//        assert(branchModels.size() == 1 || (branchModels.size() == Alignments.size()));
//
//        this.branchModels.addAll(branchModels);

        // SiteRateModels determine the rates per category (for site-heterogeneity models).
        // There can be either one per partition or one shared across all partitions
        assert(siteRateModels.size() == 1 || (siteRateModels.size() == Alignments.size()));

        //this.siteRateModels.addAll(siteRateModels);
        this.categoryCount = this.siteRateModels.get(0).getCategoryCount();

        nodeCount = tree.getNodeCount();
        tipCount = tree.getLeafNodeCount();
        internalNodeCount = nodeCount - tipCount;

        branchUpdateIndices = new int[nodeCount];
        branchLengths = new double[nodeCount];
        //changed initialization to account for multiple partitions
        scaleBufferIndices = new int[partitionCount][internalNodeCount];
        storedScaleBufferIndices = new int[partitionCount][internalNodeCount];

        operations = new int[internalNodeCount * Beagle.PARTITION_OPERATION_TUPLE_SIZE * partitionCount];

        rescalingCount = new int[partitionCount];
        rescalingCountInner = new int[partitionCount];
        firstRescaleAttempt = true;

        try {

            int compactPartialsCount = tipCount;
            if (useAmbiguities) {
                // if we are using ambiguities then we don't use tip partials
                compactPartialsCount = 0;
            }

            partialBufferHelper = new BufferIndexHelper[partitionCount];
            scaleBufferHelper = new BufferIndexHelper[partitionCount];
            categoryRateBufferHelper = new BufferIndexHelper[partitionCount];
            eigenBufferHelper = new BufferIndexHelper[partitionCount];
            matrixBufferHelper = new BufferIndexHelper[partitionCount];
            for (int i = 0; i < partitionCount; i++) {
                // one partials buffer for each tip and two for each internal node (for store restore)
                partialBufferHelper[i] = new BufferIndexHelper(nodeCount, tipCount);

                // one scaling buffer for each internal node plus an extra for the accumulation, then doubled for store/restore
                scaleBufferHelper[i] = new BufferIndexHelper(getScaleBufferCount(), 0);

                categoryRateBufferHelper[i] = new BufferIndexHelper(1, 0, i);
                
                eigenBufferHelper[i] = new BufferIndexHelper(1, 0, i);
                
                matrixBufferHelper[i] = new BufferIndexHelper(nodeCount, 0, i);
            }

            int eigenBufferCount = 0;
            int matrixBufferCount = 0;

            // create a substitutionModelDelegate for each branchModel
            int partitionNumber = 0;
            for (SiteModel branchModel : siteRateModels) {
                // HomogenousSubstitutionModelDelegate substitutionModelDelegate = new HomogenousSubstitutionModelDelegate(tree, branchModel, partitionNumber);
                SubstitutionModel.Base substModel = (SubstitutionModel.Base) branchModel.getSubstitutionModel();
                substitutionModels.add(substModel);

                // TODO: deal with substmodels that require more than 1 eigendecomposition (like epoch models)
                eigenBufferCount += 2;  //substitutionModelDelegate.getEigenBufferCount();
                matrixBufferCount += nodeCount * 2; // substitutionModelDelegate.getMatrixBufferCount();

                partitionNumber ++;
            }

            fetchBeagleSettings(); // in case they haven't been set already

            // first set the rescaling scheme to use from the parser
            this.rescalingScheme = rescalingScheme;
            this.delayRescalingUntilUnderflow = delayRescalingUntilUnderflow;

            int[] resourceList = null;
            long preferenceFlags = 0;
            long requirementFlags = 0;

            if (scalingOrder.size() > 0) {
                this.rescalingScheme = PartialsRescalingScheme.parseFromString(
                        scalingOrder.get(instanceCount % scalingOrder.size()));
            }

            if (resourceOrder.size() > 0) {
                // added the zero on the end so that a CPU is selected if requested resource fails
                resourceList = new int[]{resourceOrder.get(instanceCount % resourceOrder.size()), 0};
                if (resourceList[0] > 0) {
                    preferenceFlags |= BeagleFlag.PROCESSOR_GPU.getMask(); // Add preference weight against CPU
                }
            }

            if (preferredOrder.size() > 0) {
                preferenceFlags = preferredOrder.get(instanceCount % preferredOrder.size());
            }

            if (requiredOrder.size() > 0) {
                requirementFlags = requiredOrder.get(instanceCount % requiredOrder.size());
            }

            // Define default behaviour here
            if (this.rescalingScheme == PartialsRescalingScheme.DEFAULT) {
                //if GPU: the default is dynamic scaling in BEAST
                if (resourceList != null && resourceList[0] > 1) {
                    this.rescalingScheme = DEFAULT_RESCALING_SCHEME;
                } else {
                    // if CPU: just run as fast as possible
                    // this.rescalingScheme = PartialsRescalingScheme.NONE;
                    // Dynamic should run as fast as none until first underflow
                    this.rescalingScheme = DEFAULT_RESCALING_SCHEME;
                }
            }

            // to keep behaviour of the delayed scheme (always + delay)...
            if (this.rescalingScheme == PartialsRescalingScheme.DELAYED) {
                this.delayRescalingUntilUnderflow = true;
                this.rescalingScheme = PartialsRescalingScheme.ALWAYS;
            }

            if (this.rescalingScheme == PartialsRescalingScheme.AUTO) {
                // auto scaling not supported for multi-partition BEAGLE3 instances
                preferenceFlags |= BeagleFlag.SCALING_DYNAMIC.getMask();
            } else {
                // preferenceFlags |= BeagleFlag.SCALING_MANUAL.getMask();
            }

            String r = System.getProperty(RESCALE_FREQUENCY_PROPERTY);
            if (r != null) {
                rescalingFrequency = Integer.parseInt(r);
                if (rescalingFrequency < 1) {
                    rescalingFrequency = RESCALE_FREQUENCY;
                }
            }

            String d = System.getProperty(DELAY_SCALING_PROPERTY);
            if (d != null) {
                this.delayRescalingUntilUnderflow = Boolean.parseBoolean(d);
            }

            // I don't think this performance stuff should be here. Perhaps have an intelligent automatic
            // load balancer further up the chain.
//            if (preferenceFlags == 0 && resourceList == null) { // else determine dataset characteristics
//                if (stateCount == 4 && Alignment.getPatternCount() < 10000) // TODO determine good cut-off
//                    preferenceFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
//            }

            boolean forceVectorization = false;
            String vectorizationString = System.getProperty(FORCE_VECTORIZATION);
            if (vectorizationString != null) {
                forceVectorization = true;
            }

            String tc = System.getProperty(THREAD_COUNT);
            if (tc != null) {
                threadCount = Integer.parseInt(tc);
            }

            if (threadCount == 0 || threadCount == 1) {
                preferenceFlags &= ~BeagleFlag.THREADING_CPP.getMask();
                preferenceFlags |= BeagleFlag.THREADING_NONE.getMask();
            } else {
                preferenceFlags &= ~BeagleFlag.THREADING_NONE.getMask();
                preferenceFlags |= BeagleFlag.THREADING_CPP.getMask();
            }

            if (BeagleFlag.VECTOR_SSE.isSet(preferenceFlags) && (stateCount != 4)
                    && !forceVectorization && !IS_ODD_STATE_SSE_FIXED()
            ) {
                // TODO SSE doesn't seem to work for larger state spaces so for now we override the
                // SSE option.
                preferenceFlags &= ~BeagleFlag.VECTOR_SSE.getMask();
                preferenceFlags |= BeagleFlag.VECTOR_NONE.getMask();

                if (stateCount > 4 && this.rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                    this.rescalingScheme = PartialsRescalingScheme.DELAYED;
                }
            }

            if (!BeagleFlag.PRECISION_SINGLE.isSet(preferenceFlags)) {
                // if single precision not explicitly set then prefer double
                preferenceFlags |= BeagleFlag.PRECISION_DOUBLE.getMask();
            }

            if (substitutionModels.get(0).canReturnComplexDiagonalization()) {
                requirementFlags |= BeagleFlag.EIGEN_COMPLEX.getMask();
            }

            if ((resourceList == null &&
                    (BeagleFlag.PROCESSOR_GPU.isSet(preferenceFlags) ||
                            BeagleFlag.FRAMEWORK_CUDA.isSet(preferenceFlags) ||
                            BeagleFlag.FRAMEWORK_OPENCL.isSet(preferenceFlags)))
                    ||
                    (resourceList != null && resourceList[0] > 0)) {
                // non-CPU implementations don't have SSE so remove default preference for SSE
                // when using non-CPU preferences or prioritising non-CPU resource
                preferenceFlags &= ~BeagleFlag.VECTOR_SSE.getMask();
                preferenceFlags &= ~BeagleFlag.THREADING_CPP.getMask();
            }

            // start auto resource selection
            String resourceAuto = System.getProperty(RESOURCE_AUTO_PROPERTY);
            if (resourceAuto != null && Boolean.parseBoolean(resourceAuto)) {

                long benchmarkFlags = 0;

                if (this.rescalingScheme == PartialsRescalingScheme.NONE) {
                    benchmarkFlags =  BeagleBenchmarkFlag.SCALING_NONE.getMask();
                } else if (this.rescalingScheme == PartialsRescalingScheme.ALWAYS) {
                    benchmarkFlags =  BeagleBenchmarkFlag.SCALING_ALWAYS.getMask();
                } else {
                    benchmarkFlags =  BeagleBenchmarkFlag.SCALING_DYNAMIC.getMask();
                }

                Log.warning("\nRunning benchmarks to automatically select fastest BEAGLE resource for analysis... ");

                List<BenchmarkedResourceDetails> benchmarkedResourceDetails =
                        BeagleFactory.getBenchmarkedResourceDetails(
                                tipCount,
                                compactPartialsCount,
                                stateCount,
                                totalPatternCount,
                                categoryCount,
                                resourceList,
                                preferenceFlags,
                                requirementFlags,
                                1, // eigenModelCount,
                                partitionCount,
                                0, // calculateDerivatives,
                                benchmarkFlags);


                Log.warning(" Benchmark results, from fastest to slowest:");

                for (BenchmarkedResourceDetails benchmarkedResource : benchmarkedResourceDetails) {
                    Log.warning(benchmarkedResource.toString());
                }

                long benchedFlags = benchmarkedResourceDetails.get(0).getBenchedFlags();
                if ((benchedFlags & BeagleFlag.FRAMEWORK_CPU.getMask()) != 0) {
                    throw new DelegateTypeException();
                }

                resourceList = new int[]{benchmarkedResourceDetails.get(0).getResourceNumber()};
            }
            // end auto resource selection

            //TODO: check getBufferCount() calls with Daniel
            //TODO: should we multiple getBufferCount() by the number of partitions?
            beagle = BeagleFactory.loadBeagleInstance(
                    tipCount,
                    partialBufferHelper[0].getBufferCount(),
                    compactPartialsCount,
                    stateCount,
                    totalPatternCount,
                    eigenBufferCount,
                    matrixBufferCount,
                    categoryCount,
                    scaleBufferHelper[0].getBufferCount(), // Always allocate; they may become necessary
                    resourceList,
                    preferenceFlags,
                    requirementFlags
            );
            
//            BeagleDebugger debugger = new BeagleDebugger(beagle, true);
//            beagle = debugger;

            InstanceDetails instanceDetails = beagle.getDetails();
            ResourceDetails resourceDetails = null;

            long instanceFlags = instanceDetails.getFlags();
//            if ((instanceFlags & BeagleFlag.FRAMEWORK_CPU.getMask()) != 0) {
//                throw new DelegateTypeException();
//            }

            Log.warning("\nUsing Multi-Partition Data Likelihood Delegate with BEAGLE 3 multi-partition extensions");

//            for (BranchRateModel branchModel : this.branchModels) {
//                addModel(branchModel);
//            }
//
//            for (SiteModel siteRateModel : this.siteRateModels) {
//                assert(siteRateModel.getCategoryCount() == categoryCount);
//                addModel(siteRateModel);
//            }

            if (instanceDetails != null) {
                resourceDetails = BeagleFactory.getResourceDetails(instanceDetails.getResourceNumber());
                if (resourceDetails != null) {
                    StringBuilder sb = new StringBuilder("  Using BEAGLE version: " + BeagleInfo.getVersion() + " resource ");
                    sb.append(resourceDetails.getNumber()).append(": ");
                    sb.append(resourceDetails.getName()).append("\n");
                    if (resourceDetails.getDescription() != null) {
                        String[] description = resourceDetails.getDescription().split("\\|");
                        for (String desc : description) {
                            if (desc.trim().length() > 0) {
                                sb.append("    ").append(desc.trim()).append("\n");
                            }
                        }
                    }
                    sb.append("    with instance flags: ").append(instanceDetails.toString());
                    Log.warning(sb.toString());
                } else {
                    Log.warning("  Error retrieving BEAGLE resource for instance: " + instanceDetails.toString());
                }
            } else {
                Log.warning("  No external BEAGLE resources available, or resource list/requirements not met, using Java implementation");
            }

            if (IS_THREAD_COUNT_COMPATIBLE() && threadCount > 1) {
                beagle.setCPUThreadCount(threadCount);
            }

            patternPartitions = new int[totalPatternCount];
            patternWeights = new double[totalPatternCount];

            int j = 0;
            k = 0;
            for (Alignment Alignment : Alignments) {
                int[] pw = Alignment.getWeights();
                for (int i = 0; i < Alignment.getPatternCount(); i++) {
                    patternPartitions[k] = j;
                    patternWeights[k] = pw[i];
                    k++;
                }
                j++;
            }

            Log.warning("  " + (useAmbiguities ? "Using" : "Ignoring") + " ambiguities in tree likelihood.");
            Log.warning.println("  " + (useTipLikelihoods ? "Using" : "Ignoring") + " character uncertainty in tree likelihood.");
            String patternCountString = "" + Alignments.get(0).getPatternCount();
            for (int i = 1; i < Alignments.size(); i++) {
                patternCountString += ", " + Alignments.get(i).getPatternCount();
            }
            Log.warning("  With " + Alignments.size() + " partitions comprising " + patternCountString + " unique site patterns");

            for (int i = 0; i < tipCount; i++) {
                if (useAmbiguities || useTipLikelihoods) {
                    setPartials(beagle, Alignments, tree.getNode(i));
                } else {
                    setStates(beagle, Alignments, tree.getNode(i));
                }
            }

            beagle.setPatternWeights(patternWeights);
            beagle.setPatternPartitions(partitionCount, patternPartitions);

            String rescaleMessage = "  Using rescaling scheme : " + this.rescalingScheme.getText();
            if (this.rescalingScheme == PartialsRescalingScheme.AUTO) {
                // auto scaling in BEAGLE3 is not supported
                this.rescalingScheme = PartialsRescalingScheme.DYNAMIC;
                rescaleMessage = "  Auto rescaling not supported in BEAGLE v3, using : " + this.rescalingScheme.getText();
            }
            boolean parenthesis = false;
            if (this.rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                rescaleMessage += " (rescaling every " + rescalingFrequency + " evaluations";
                parenthesis = true;
            }
            if (this.delayRescalingUntilUnderflow) {
                rescaleMessage += (parenthesis ? ", " : "(") + "delay rescaling until first overflow";
                parenthesis = true;
            }
            rescaleMessage += (parenthesis ? ")" : "");
            Log.warning(rescaleMessage);

            if (this.rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                for (int i = 0; i < partitionCount; i++) {
                    everUnderflowed[i] = false; // If false, BEAST does not rescale until first under-/over-flow.
                }
            }

            updateSubstitutionModels = new boolean[substitutionModels.size()];
            Arrays.fill(updateSubstitutionModels, true);

            updateSiteRateModels = new boolean[siteRateModels.size()];
            Arrays.fill(updateSiteRateModels, true);

        } catch (Exception mte) {
        	mte.printStackTrace();
            throw new RuntimeException(mte.toString());
        }

        lastKnownFrequencies = new double[siteRateModels.size()][stateCount];
        lastKnownCategorieWeights = new double[siteRateModels.size()][categoryCount];
        lastKnownCategorieRates = new double[siteRateModels.size()*2][categoryCount];

        instanceCount ++;
    }

//    @Override
//    public String getReport() {
//        return null;
//    }

//    @Override
//    public TreeTraversal.TraversalType getOptimalTraversalType() {
//        return TreeTraversal.TraversalType.REVERSE_LEVEL_ORDER;
//    }

    public List<Alignment> getAlignments() {
        return this.Alignments;
    }

//    @Override
    public int getTraitCount() {
        return 1;
    }

//    @Override
    public int getTraitDim() {
        return totalPatternCount;
    }

//    @Override
//    public RateRescalingScheme getRateRescalingScheme() {
//        return RateRescalingScheme.NONE;
//    }

//    private void updateSubstitutionModels(boolean... state) {
//        for (int i = 0; i < updateSubstitutionModels.length; i++) {
//            updateSubstitutionModels[i] = (state.length < 1 || state[0]);
//        }
//    }

//    private void updateSubstitutionModel(BranchRateModel branchModel) {
//        for (int i = 0; i < branchModels.size(); i++) {
//            if (branchModels.get(i) == branchModel) {
//                updateSubstitutionModels[i] = true;
//            }
//        }
//    }

//    private void updateSiteRateModels(boolean... state) {
//        for (int i = 0; i < updateSiteRateModels.length; i++) {
//            updateSiteRateModels[i] = (state.length < 1 || state[0]);
//        }
//    }

//    private void updateSiteRateModel(SiteModel siteRateModel) {
//        for (int i = 0; i < siteRateModels.size(); i++) {
//            if (siteRateModels.get(i) == siteRateModel) {
//                updateSiteRateModels[i] = true;
//            }
//        }
//    }

    private int getScaleBufferCount() {
        return internalNodeCount + 1;
    }

    
    
    
    
    /**
     * set leaf states in likelihood core *
     */
    protected void setStates(Beagle beagle,
            List<Alignment> Alignments,
            Node node) {

    	int[] states = new int[totalPatternCount];
    	int k = 0;
        for (Alignment data : Alignments) {
            int taxonIndex = getTaxonIndex(node.getID(), data);
            for (int i = 0; i < data.getPatternCount(); i++) {
                int code = data.getPattern(taxonIndex, i);
                int[] statesForCode = data.getDataType().getStatesForCode(code);
                if (statesForCode.length==1)
                    states[k++] = statesForCode[0];
                else
                    states[k++] = code; // Causes ambiguous states to be ignored.
            }
        }
        beagle.setTipStates(node.getNr(), states);
    }

    /**
     *
     * @param taxon the taxon name as a string
     * @param data the alignment
     * @return the taxon index of the given taxon name for accessing its sequence data in the given alignment,
     *         or -1 if the taxon is not in the alignment.
     */
    private int getTaxonIndex(String taxon, Alignment data) {
        int taxonIndex = data.getTaxonIndex(taxon);
        if (taxonIndex == -1) {
        	if (taxon.startsWith("'") || taxon.startsWith("\"")) {
                taxonIndex = data.getTaxonIndex(taxon.substring(1, taxon.length() - 1));
            }
            if (taxonIndex == -1) {
            	throw new RuntimeException("Could not find sequence " + taxon + " in the alignment " + data.getID());
            }
        }
        return taxonIndex;
	}

	/**
     * set leaf partials in likelihood core *
     */
    protected void setPartials(Beagle beagle,
            List<Alignment> Alignments,
            Node node) {
        double[] partials = new double[totalPatternCount * stateCount * categoryCount];

        int k = 0;
        for (Alignment data : Alignments) {
            int states = data.getDataType().getStateCount();
            int taxonIndex = getTaxonIndex(node.getID(), data);
            for (int patternIndex_ = 0; patternIndex_ < data.getPatternCount(); patternIndex_++) {                
                double[] tipLikelihoods = data.getTipLikelihoods(taxonIndex,patternIndex_);
                if (tipLikelihoods != null) {
                	for (int state = 0; state < states; state++) {
                		partials[k++] = tipLikelihoods[state];
                	}
                }
                else {
                	int stateCount = data.getPattern(taxonIndex, patternIndex_);
	                boolean[] stateSet = data.getStateSet(stateCount);
	                for (int state = 0; state < states; state++) {
	                	 partials[k++] = (stateSet[state] ? 1.0 : 0.0);                
	                }
                }
            }
        }
        
	    // if there is more than one category then replicate the partials for each
	    int n = totalPatternCount * stateCount;
	    k = n;
	    for (int i = 1; i < categoryCount; i++) {
	        System.arraycopy(partials, 0, partials, k, n);
	        k += n;
	    }

        beagle.setPartials(node.getNr(), partials);
    }
    
    
    
    /**
     * Sets the partials from a sequence in an alignment.
     *
     * @param beagle        beagle
     * @param Alignments  Alignments
     * @param taxonId       taxonId
     * @param nodeIndex     nodeIndex
     */
//    private final void setPartials(Beagle beagle,
//                                   List<Alignment> Alignments,
//                                   String taxonId,
//                                   int nodeIndex) throws TaxonList.MissingTaxonException {
//
//        double[] partials = new double[totalPatternCount * stateCount * categoryCount];
//        int v = 0;
//        for (Alignment Alignment : Alignments) {
//            int sequenceIndex = Alignment.getTaxonIndex(taxonId);
//
//            if (sequenceIndex == -1) {
//                throw new TaxonList.MissingTaxonException("Taxon, " + taxonId +
//                        ", not found in Alignment, " + Alignment.getID());
//            }
//
//            boolean[] stateSet;
//
//            for (int i = 0; i < Alignment.getPatternCount(); i++) {
//
//                int state = Alignment.getPatternState(sequenceIndex, i);
//                stateSet = dataType.getStateSet(state);
//
//                for (int j = 0; j < stateCount; j++) {
//                    if (stateSet[j]) {
//                        partials[v] = 1.0;
//                    } else {
//                        partials[v] = 0.0;
//                    }
//                    v++;
//                }
//            }
//        }
//
//        // if there is more than one category then replicate the partials for each
//        int n = totalPatternCount * stateCount;
//        int k = n;
//        for (int i = 1; i < categoryCount; i++) {
//            System.arraycopy(partials, 0, partials, k, n);
//            k += n;
//        }
//
//        beagle.setPartials(nodeIndex, partials);
//    }

    /**
     * Sets the partials from a sequence in an alignment.
     *
     * @param beagle        beagle
     * @param Alignments  Alignments
     * @param taxonId       taxonId
     * @param nodeIndex     nodeIndex
     */
//    private final void setStates(Beagle beagle,
//                                 List<Alignment> Alignments,
//                                 String taxonId,
//                                 int nodeIndex) throws TaxonList.MissingTaxonException {
//
//        int[] states = new int[totalPatternCount];
//
//        int v = 0;
//        for (Alignment Alignment : Alignments) {
//            int sequenceIndex = Alignment.getTaxonIndex(taxonId);
//
//            if (sequenceIndex == -1) {
//                throw new TaxonList.MissingTaxonException("Taxon, " + taxonId +
//                        ", not found in Alignment, " + Alignment.getID());
//            }
//
//            for (int i = 0; i < Alignment.getPatternCount(); i++) {
//                states[v] = Alignment.getPatternState(sequenceIndex, i);
//                v++;
//            }
//        }
//
//        beagle.setTipStates(nodeIndex, states);
//    }

    
    /**
     * Calculate the log likelihood of the data for the current tree.
     *
     * @return the log likelihood.
     */
//    int x = 0;
    @Override
    public double calculateLogP() {

//    	x++;
    	//System.err.println(x);
//    	if (x > 265) {
//    		((BeagleDebugger)beagle).output = true;
//    	}
        logP = Double.NEGATIVE_INFINITY;
        boolean done = false;
        long underflowCount = 0;

        do {
            // treeTraversalDelegate.dispatchTreeTraversalCollectBranchAndNodeOperations();
        	this.nodeOperations.clear();
        	this.branchOperations.clear();
        	
        	traverseReverseLevelOrder((Tree) tree);
        	
//            final List<BranchOperation> branchOperations = getBranchOperations();
//            final List<NodeOperation> nodeOperations = getNodeOperations();

            if (COUNT_TOTAL_OPERATIONS) {
                //totalMatrixUpdateCount += branchOperations.size();
                //totalOperationCount += nodeOperations.size();
            }

            final Node root = tree.getRoot();

            try {
                logP = calculateLikelihood(branchOperations, nodeOperations, root.getNr());

                done = true;
            } catch (LikelihoodException e) {

                // if there is an underflow, assume delegate will attempt to rescale
                // so flag all nodes to update and return to try again.
                // updateAllNodes();
                tree.getRoot().makeAllDirty(Tree.IS_FILTHY);
                underflowCount++;
            }

        } while (!done && underflowCount < MAX_UNDERFLOWS_BEFORE_ERROR);


        return logP;
    }

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    // @Override
    public double calculateLikelihood(List<BranchOperation> branchOperations, List<NodeOperation> nodeOperations, int rootNodeNumber) throws LikelihoodException {

        boolean throwLikelihoodRescalingException = false;
        if (!initialEvaluation) {
            for (int i = 0; i < partitionCount; i++) {
                if (!this.delayRescalingUntilUnderflow || everUnderflowed[i]) {
                    if (this.rescalingScheme == PartialsRescalingScheme.ALWAYS || this.rescalingScheme == PartialsRescalingScheme.DELAYED) {
                        useScaleFactors[i] = true;
                        recomputeScaleFactors[i] = true;
                    } else if (this.rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                        useScaleFactors[i] = true;

                        if (DEBUG) {
                            System.out.println("rescalingCount["+i+"] = " + rescalingCount[i]);
                        }

                        if (rescalingCount[i] > rescalingFrequency) {
                            if (DEBUG) {
                                System.out.println("rescalingCount > rescalingFrequency");
                            }
                            rescalingCount[i] = 0;
                            rescalingCountInner[i] = 0;
                        }

                        if (DEBUG) {
                            System.out.println("rescalingCountInner = " + rescalingCountInner[i]);
                        }

                        if (rescalingCountInner[i] < RESCALE_TIMES) {
                            if (DEBUG) {
                                System.out.println("rescalingCountInner < RESCALE_TIMES");
                            }

                            recomputeScaleFactors[i] = true;
                            updatePartition[i] = true;

                            rescalingCountInner[i]++;

                            throwLikelihoodRescalingException = true;
                        }
                    }
                }
            }

            if (throwLikelihoodRescalingException) {
                throw new LikelihoodRescalingException();
            }
        }

        if (RESCALING_OFF) { // a debugging switch
            for (int i = 0; i < partitionCount; i++) {
                useScaleFactors[i] = false;
                recomputeScaleFactors[i] = false;
            }
        }

        int k = 0;
        for (SubstitutionModel evolutionaryProcessDelegate : substitutionModels) {
            if (updateSubstitutionModels[k]) {
                // TODO: More efficient to update only the substitution model that changed, instead of all
                // TODO: flip currently assumes 1 substitution model per partition
                updateSubstitutionModels(k, beagle, flip[k]);
                updatePartition[k] = true;
                if (DEBUG) {
                    System.out.println("updateSubstitutionModels, updatePartition["+k+"] = " + updatePartition[k]);
                }
//                updateAllPartitions = false;
                // we are currently assuming a no-category model...
            }
            k++;
        }

        k = 0;
        for (SiteModel siteRateModel : siteRateModels) {
            if (updateSiteRateModels[k]) {
                double[] categoryRates = siteRateModel.getCategoryRates(null);
                if (categoryRates == null) {
                    // If this returns null then there was a numerical error calculating the category rates
                    // (probably a very small alpha) so reject the move.

                    // mark model updates as completed before returning
                	Arrays.fill(updateSubstitutionModels, false);
                    //updateSubstitutionModels(false);
                	Arrays.fill(updateSiteRateModels, false);
                    //updateSiteRateModels(false);

                    return Double.NEGATIVE_INFINITY;
                }

                if (flip[k]) {
                    categoryRateBufferHelper[k].flipOffset(0);
                }

                if (changed(categoryRates, lastKnownCategorieRates[categoryRateBufferHelper[k].getOffsetIndex(0)])) {
                	beagle.setCategoryRatesWithIndex(categoryRateBufferHelper[k].getOffsetIndex(0), categoryRates);
                	System.arraycopy(categoryRates, 0, lastKnownCategorieRates[categoryRateBufferHelper[k].getOffsetIndex(0)], 0, categoryCount);
                }
                updatePartition[k] = true;
                if (DEBUG) {
                    System.out.println("updateSiteRateModels, updatePartition["+k+"] = " + updatePartition[k]);
                }
//                updateAllPartitions = false;
            }
            k++;
        }

        int branchUpdateCount = 0;
        for (BranchOperation op : branchOperations) {
            branchUpdateIndices[branchUpdateCount] = op.getBranchNumber();
            branchLengths[branchUpdateCount] = op.getBranchLength();
            branchUpdateCount++;
        }

        if (branchUpdateCount > 0) {
            // TODO below only applies to homogenous substitution models

            int   [] eigenDecompositionIndices = new int   [branchUpdateCount * partitionCount];
            int   [] categoryRateIndices       = new int   [branchUpdateCount * partitionCount];
            int   [] probabilityIndices        = new int   [branchUpdateCount * partitionCount];
            double[] edgeLengths               = new double[branchUpdateCount * partitionCount];

            matrixUpdateCount = 0;
            int partition = 0;
            for (SubstitutionModel evolutionaryProcessDelegate : substitutionModels) {
                if (updatePartition[partition] || updateAllPartitions) {
                    if (flip[partition]) {
                    	/*evolutionaryProcessDelegate.*/flipTransitionMatrices(branchUpdateIndices,
                                branchUpdateCount, partition);
                    }

                    for (int i = 0; i < branchUpdateCount; i++) {
                        eigenDecompositionIndices[matrixUpdateCount] = eigenBufferHelper[partition].getOffsetIndex(0);
                        	// = evolutionaryProcessDelegate.getEigenIndex(0);
                        categoryRateIndices[matrixUpdateCount] = categoryRateBufferHelper[partition].getOffsetIndex(0);
                        probabilityIndices[matrixUpdateCount] = /*evolutionaryProcessDelegate*/getMatrixIndex(branchUpdateIndices[i], partition);
                        edgeLengths[matrixUpdateCount] = branchLengths[i];
                        matrixUpdateCount++;
                    }
                }
                partition++;
            }

            beagle.updateTransitionMatricesWithMultipleModels(
                    eigenDecompositionIndices,
                    categoryRateIndices,
                    probabilityIndices,
                    null, // firstDerivativeIndices
                    null, // secondDerivativeIndices
                    edgeLengths,
                    matrixUpdateCount);

            if (COUNT_CALCULATIONS) {
                totalMatrixUpdateCount += matrixUpdateCount;
            }

        }

        for (int i = 0; i < partitionCount; i++) {
            if (updatePartition[i] || updateAllPartitions) {

                if (DEBUG) {
                    System.out.println("updatePartition["+i+"] = " + updatePartition[i] + ", updateAllPartitions = " + updateAllPartitions);
                }

                if (flip[i]) {
                    // Flip all the buffers to be written to first...

                    for (NodeOperation op : nodeOperations) {
                        partialBufferHelper[i].flipOffset(op.getNodeNumber());
                    }
                }
            }
        }


        partialUpdateCount = 0;
        k = 0;
        for (NodeOperation op : nodeOperations) {
            int nodeNum = op.getNodeNumber();

            int[] writeScale = new int[partitionCount];
            int[] readScale = new int[partitionCount];

            for (int i = 0; i < partitionCount; i++) {
                if (updatePartition[i] || updateAllPartitions) {
                    if (useScaleFactors[i]) {
                        // get the index of this scaling buffer
                        int n = nodeNum - tipCount;

                        if (recomputeScaleFactors[i]) {
                            // flip the indicator: can take either n or (internalNodeCount + 1) - n
                            scaleBufferHelper[i].flipOffset(n);

                            // store the index
                            scaleBufferIndices[i][n] = scaleBufferHelper[i].getOffsetIndex(n);

                            writeScale[i] = scaleBufferIndices[i][n]; // Write new scaleFactor
                            readScale[i] = Beagle.NONE;

                        } else {
                            writeScale[i] = Beagle.NONE;
                            readScale[i] = scaleBufferIndices[i][n]; // Read existing scaleFactor
                        }

                    } else {

                        writeScale[i] = Beagle.NONE; // Not using scaleFactors
                        readScale[i] = Beagle.NONE;
                    }
                }
            }

            //Example 1: 1 partition with 1 evolutionary model & -beagle_instances 3
            //partition 0 -> model 0
            //partition 1 -> model 0
            //partition 2 -> model 0

            //Example 2: 3 partitions with 3 evolutionary models & -beagle_instances 2
            //partitions 0 & 1 -> model 0
            //partitions 2 & 3 -> model 1
            //partitions 4 & 5 -> model 2

            int mapPartition = partitionCount / substitutionModels.size();

            for (int i = 0; i < partitionCount; i++) {
                if (updatePartition[i] || updateAllPartitions) {

                    SubstitutionModel evolutionaryProcessDelegate = substitutionModels.get(i / (mapPartition));
                    /*if (evolutionaryProcessDelegates.size() == partitionCount) {
                        evolutionaryProcessDelegate = evolutionaryProcessDelegates.get(i);
                    } else {
                        evolutionaryProcessDelegate = evolutionaryProcessDelegates.get(0);
                    }*/

                    operations[k] = partialBufferHelper[i].getOffsetIndex(nodeNum);
                    operations[k + 1] = writeScale[i];
                    operations[k + 2] = readScale[i];
                    operations[k + 3] = partialBufferHelper[i].getOffsetIndex(op.getLeftChild()); // source node 1
                    operations[k + 4] = /*evolutionaryProcessDelegate.*/getMatrixIndex(op.getLeftChild(), i); // source matrix 1
                    operations[k + 5] = partialBufferHelper[i].getOffsetIndex(op.getRightChild()); // source node 2
                    operations[k + 6] = /*evolutionaryProcessDelegate.*/getMatrixIndex(op.getRightChild(), i); // source matrix 2
                    operations[k + 7] = i;
                    //TODO: we don't know the cumulateScaleBufferIndex here yet (see below)
                    operations[k + 8] = Beagle.NONE;

                    if (DEBUG) {
                        if (k == 0 || (k == Beagle.PARTITION_OPERATION_TUPLE_SIZE)) {
                            System.out.println("write = " + writeScale[i] + "; read = " + readScale[i] + "; parent = " + operations[k] + ", k = " + k + ", i = " + i);
                        }

                    }

                    k += Beagle.PARTITION_OPERATION_TUPLE_SIZE;
                    partialUpdateCount++;
                }

            }
        }

        beagle.updatePartialsByPartition(operations, partialUpdateCount);

        if (COUNT_CALCULATIONS) {
            totalEvaluationCount += 1;
            totalPartialsUpdateCount += partialUpdateCount;
        }


        //double[] rootPartials = new double[totalPatternCount * stateCount];
        //beagle.getPartials(rootIndex, 0, rootPartials);

        int[] cumulativeScaleIndices  = new int[partitionCount];
        for (int i = 0; i < partitionCount; i++) {
            cumulativeScaleIndices[i] = Beagle.NONE;
            if (useScaleFactors[i]) {
                if (recomputeScaleFactors[i] && (updatePartition[i] || updateAllPartitions)) {
                    scaleBufferHelper[i].flipOffset(internalNodeCount);
                    cumulativeScaleIndices[i] = scaleBufferHelper[i].getOffsetIndex(internalNodeCount);
                    //TODO: check with Daniel if calling these methods using an iteration can be done more efficiently
                    beagle.resetScaleFactorsByPartition(cumulativeScaleIndices[i], i);
                    beagle.accumulateScaleFactorsByPartition(scaleBufferIndices[i], internalNodeCount, cumulativeScaleIndices[i], i);
                } else {
                    cumulativeScaleIndices[i] = scaleBufferHelper[i].getOffsetIndex(internalNodeCount);
                }
            }
        }

//        double[] scaleFactors = new double[totalPatternCount];
//        beagle.getLogScaleFactors(cumulateScaleBufferIndex, scaleFactors);

        for (int i = 0; i < siteRateModels.size(); i++) {
            double[] categoryWeights = this.siteRateModels.get(i).getCategoryProportions(null);
            if (changed(categoryWeights, lastKnownCategorieWeights[i])) {
            	beagle.setCategoryWeights(i, categoryWeights);
            	System.arraycopy(categoryWeights, 0, lastKnownCategorieWeights[i], 0, categoryWeights.length);
            }

            // This should probably explicitly be the state frequencies for the root node...
            double[] frequencies = substitutionModels.get(i).getFrequencies();
            if (changed(frequencies, lastKnownFrequencies[i])) {
            	beagle.setStateFrequencies(i, frequencies);
            	System.arraycopy(frequencies, 0, lastKnownFrequencies[i], 0, frequencies.length);
            }
        }


        if (DEBUG) {
            for (int i = 0; i < partitionCount; i++) {
                System.out.println("useScaleFactors=" + useScaleFactors[i] + " recomputeScaleFactors=" + recomputeScaleFactors[i] + " (partition: " + i + ")");
            }
        }

                /*System.out.println("partitionCount = " + partitionCount);
                for (int i = 0; i < partitionCount; i++) {
                    System.out.println("partitionIndices[" + i + "] = " + partitionIndices[i]);
                }*/

        int[] partitionIndices = new int[partitionCount];
        int[] rootIndices             = new int[partitionCount];
        int[] categoryWeightsIndices  = new int[partitionCount];
        int[] stateFrequenciesIndices = new int[partitionCount];
        double[] sumLogLikelihoods = new double[1];
        double[] sumLogLikelihoodsByPartition = new double[partitionCount];

        // create a list of partitions that have been updated
        int updatedPartitionCount = 0;
        for (int i = 0; i < partitionCount; i++) {
            if (updatePartition[i] || updateAllPartitions) {
                partitionIndices[updatedPartitionCount] = i;

                rootIndices            [updatedPartitionCount]  = partialBufferHelper[i].getOffsetIndex(rootNodeNumber);
                categoryWeightsIndices [updatedPartitionCount]  = i % siteRateModels.size();
                stateFrequenciesIndices[updatedPartitionCount]  = i % siteRateModels.size();
                cumulativeScaleIndices [updatedPartitionCount]  = cumulativeScaleIndices[i];

                updatedPartitionCount++;
            }
        }


        //TODO: check these arguments with Daniel
        //TODO: partitionIndices needs to be set according to which partitions need updating?
        beagle.calculateRootLogLikelihoodsByPartition(
                rootIndices,
                categoryWeightsIndices,
                stateFrequenciesIndices,
                cumulativeScaleIndices,
                partitionIndices,
                updatedPartitionCount,
                1,
                sumLogLikelihoodsByPartition,
                sumLogLikelihoods);

                /*System.out.println();
                for (int i = 0; i < partitionCount; i++) {
                    System.out.println("partition " + i + " lnL = " + sumLogLikelihoodsByPartition[i]);
                }*/

        // write updated partition likelihoods to the cached array
        for (int i = 0; i < updatedPartitionCount; i++) {
            cachedLogLikelihoodsByPartition[partitionIndices[i]] = sumLogLikelihoodsByPartition[i];

            // clear the global flags
            updatePartition[partitionIndices[i]] = false;
            recomputeScaleFactors[partitionIndices[i]] = false;

            partitionWasUpdated[partitionIndices[i]] = true;
        }

        double tmpLogL = sumLogLikelihoods[0];

        if (DEBUG) {
            for (int i = 0; i < partitionCount; i++) {
                System.out.println("partition " + i + ": " + cachedLogLikelihoodsByPartition[i] +
                        (partitionWasUpdated[i] ? " [updated]" : ""));
            }
        }

        // If these are needed...
//        if (patternLogLikelihoods == null) {
//            patternLogLikelihoods = new double[totalPatternCount];
//        }
//        beagle.getSiteLogLikelihoods(patternLogLikelihoods);

//        updateSubstitutionModels(false);
        Arrays.fill(updateSubstitutionModels, false);
//        updateSiteRateModels(false);
        Arrays.fill(updateSiteRateModels, false);
        updateAllPartitions = true;

        if (Double.isNaN(tmpLogL) || Double.isInfinite(tmpLogL)) {
            // one or more of the updated partitions has underflowed

            if (DEBUG) {
                System.out.println("Double.isNaN(logL) || Double.isInfinite(logL)");
            }

            for (int i = 0; i < updatedPartitionCount; i++) {
                if (Double.isNaN(sumLogLikelihoodsByPartition[i]) || Double.isInfinite(sumLogLikelihoodsByPartition[i])) {
                    everUnderflowed[partitionIndices[i]] = true;
                }
            }

            if (firstRescaleAttempt) {
                if (delayRescalingUntilUnderflow || rescalingScheme == PartialsRescalingScheme.DELAYED) {
                    // show a message but only every 1000 rescales
                    if (rescalingMessageCount % 1000 == 0) {
                        if (rescalingMessageCount > 0) {
                            Log.warning("Underflow calculating likelihood (" + rescalingMessageCount + " messages not shown).");
                        } else {
                            if (getID() != null) {
                                Log.warning("Underflow calculating likelihood. Attempting a rescaling... (" + getID() + ")");
                            } else {
                                Log.warning("Underflow calculating likelihood. Attempting a rescaling...");
                            }
                        }
                    }
                    rescalingMessageCount += 1;
                }

                for (int i = 0; i < updatedPartitionCount; i++) {
                    if (delayRescalingUntilUnderflow || rescalingScheme == PartialsRescalingScheme.DELAYED) {
                        if (Double.isNaN(sumLogLikelihoodsByPartition[i]) || Double.isInfinite(sumLogLikelihoodsByPartition[i])) {
                            useScaleFactors[partitionIndices[i]] = true;
                            recomputeScaleFactors[partitionIndices[i]] = true;
                            updatePartition[partitionIndices[i]] = true;

                            // turn off double buffer flipping so the next call overwrites the
                            // underflowed buffers. Flip will be turned on again in storeState for
                            // next step
                            flip[partitionIndices[i]] = false;

                            updateAllPartitions = false;
                            if (DEBUG) {
                                System.out.println("Double.isNaN(logL) || Double.isInfinite(logL) (partition index: " + partitionIndices[i] + ")");
                            }
                        }

                    }
                }

                firstRescaleAttempt = false;

                throw new LikelihoodUnderflowException();
            }

            return Double.NEGATIVE_INFINITY;
        } else {

            for (int i = 0; i < updatedPartitionCount; i++) {
                if (partitionWasUpdated[partitionIndices[i]]) {
                    if (!this.delayRescalingUntilUnderflow || everUnderflowed[partitionIndices[i]]) {
                        if (this.rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                            if (!initialEvaluation) {
                                rescalingCount[partitionIndices[i]]++;
                            }
                        }
                    }
                    partitionWasUpdated[partitionIndices[i]] = false;
                }

                //TODO: probably better to only switch back those booleans that were actually altered
                recomputeScaleFactors[partitionIndices[i]] = false;
                flip[partitionIndices[i]] = true;
            }

            firstRescaleAttempt = true;
            initialEvaluation = false;
        }

        //********************************************************************
        double logL = 0.0;
        for (double l : cachedLogLikelihoodsByPartition) {
            logL += l;
        }

        return logL;
    }

    /*public void getPartials(int number, double[] partials) {
        int cumulativeBufferIndex = Beagle.NONE;
        // No need to rescale partials
        beagle.getPartials(partialBufferHelper.getOffsetIndex(number), cumulativeBufferIndex, partials);
    }*/

    /*private void setPartials(int number, double[] partials) {
        beagle.setPartials(partialBufferHelper.getOffsetIndex(number), partials);
    }*/

    // @Override
//    public void makeDirty() {
//        updateSiteRateModels();
//        updateSubstitutionModels();
//    }

    
    private boolean changed(double[] array, double[] lastKnown) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] != lastKnown[i]) {
				return true;
			}
		}
		return false;
	}

	private void updateSubstitutionModels(int partition, Beagle beagle, boolean flip) {
        if (flip) {
            eigenBufferHelper[partition].flipOffset(0);
        }
        EigenDecomposition ed = substitutionModels.get(partition).getEigenDecomposition(null);

        beagle.setEigenDecomposition(
                eigenBufferHelper[partition].getOffsetIndex(0),
                ed.getEigenVectors(),
                ed.getInverseEigenVectors(),
                ed.getEigenValues());
		
	}

	private int getMatrixIndex(int branchIndex, int partition) {
		return matrixBufferHelper[partition].getOffsetIndex(branchIndex);
	}

	private void flipTransitionMatrices(int[] branchUpdateIndices, int branchUpdateCount, int partition) {
		for (int i = 0; i < branchUpdateCount; i++) {
			matrixBufferHelper[partition].flipOffset(branchUpdateIndices[i]);
		}
	}

	@Override
    protected boolean requiresRecalculation() {
		needsUpdate = false;
    	  // updateSiteRateModel((SiteModel)model);
          for (int i = 0; i < siteRateModels.size(); i++) {
              updateSiteRateModels[i] = siteRateModels.get(i).isDirtyCalculation();
              needsUpdate |= updateSiteRateModels[i]; 
          }

          
          updateAllPartitions = tree.somethingIsDirty();
          //if (!updateAllPartitions) {
	    	  // updateSubstitutionModel((BranchRateModel)model);
	          for (int i = 0; i < substitutionModels.size(); i++) {
	              updateSubstitutionModels[i] = substitutionModels.get(i).isDirtyCalculation();
	              needsUpdate |= updateSubstitutionModels[i]; 
	          }
          //}
  // Tell TreeDataLikelihood to update all nodes
          
    	return super.requiresRecalculation();
    }
    // @Override
//    protected void handleModelChangedEvent(Model model, Object object, int index) {
//        if (model instanceof SiteModel) {
//            updateSiteRateModel((SiteModel)model);
//        } else if (model instanceof BranchRateModel) {
//            updateSubstitutionModel((BranchRateModel)model);
//        }
//        // Tell TreeDataLikelihood to update all nodes
//        fireModelChanged();
//    }
//
//    @Override
//    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
//
//    }

    /**
     * Stores the additional state other than model components
     */
    @Override
    public void store() {
        for (int i = 0; i < partitionCount; i++) {
            partialBufferHelper[i].storeState();
            categoryRateBufferHelper[i].storeState();
            
            eigenBufferHelper[i].storeState();
            matrixBufferHelper[i].storeState();
        }
//        for (SubstitutionModel evolutionaryProcessDelegate : evolutionaryProcessDelegates) {
//            evolutionaryProcessDelegate.store();
//        }

        for (int i = 0; i < partitionCount; i++) {
            if (useScaleFactors[i] ) { // Only store when actually used
                scaleBufferHelper[i].storeState();
                System.arraycopy(scaleBufferIndices[i], 0, storedScaleBufferIndices[i], 0, scaleBufferIndices[i].length);
                //storedRescalingCount = rescalingCount;
            }

            // turn on double buffering flipping (may have been turned off to enable a rescale)
            flip[i] = true;
        }
        System.arraycopy(cachedLogLikelihoodsByPartition, 0, storedCachedLogLikelihoodsByPartition, 0, cachedLogLikelihoodsByPartition.length);
        super.store();
        needsUpdate = false;
    }

    /**
     * Restore the additional stored state
     */
    @Override
    public void restore() {

        for (int i = 0; i < partitionCount; i++) {
            partialBufferHelper[i].restoreState();
            categoryRateBufferHelper[i].restoreState();
            eigenBufferHelper[i].restoreState();
            matrixBufferHelper[i].restoreState();
        }
//        for (SubstitutionModel evolutionaryProcessDelegate : evolutionaryProcessDelegates) {
//            evolutionaryProcessDelegate.restore();
//        }

        for (int i = 0; i < partitionCount; i++) {
            if (useScaleFactors[i]) {
                scaleBufferHelper[i].restoreState();
                int[] tmp = storedScaleBufferIndices[i];
                storedScaleBufferIndices[i] = scaleBufferIndices[i];
                scaleBufferIndices[i] = tmp;
                //rescalingCount = storedRescalingCount;
            }
        }

        double[] tmp = cachedLogLikelihoodsByPartition;
        cachedLogLikelihoodsByPartition = storedCachedLogLikelihoodsByPartition;
        storedCachedLogLikelihoodsByPartition = tmp;

        super.restore();
        needsUpdate = false;
    }

    // @Override
//    public void setCallback(TreeDataLikelihood treeDataLikelihood) {
//        // Do nothing
//    }

    // @Override
//    public void setComputePostOrderStatisticsOnly(boolean computePostOrderStatistic) {
//        // Do nothing
//    }

    // @Override
//    public boolean providesPostOrderStatisticsOnly() { return false; }

    // @Override
//    public int vectorizeNodeOperations(List<NodeOperation> nodeOperations, int[] operations) {
//        throw new RuntimeException("Not yet implemented");
//    }

    // @Override
//    protected void acceptState() {
//    }

    // **************************************************************
    // INSTANCE PROFILEABLE
    // **************************************************************

    // @Override
//    public long getTotalCalculationCount() {
//        // Can only return one count at the moment so return the number of partials updated
//        return totalPartialsUpdateCount;
//    }

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

    private int nodeCount;
    private int tipCount;
    private int internalNodeCount;

    private int[] branchUpdateIndices;
    private double[] branchLengths;

    //provide per partition buffer indices
    private int[][] scaleBufferIndices;
    private int[][] storedScaleBufferIndices;

    private int[] operations;

    //allow flipping per partition
    private boolean[] flip;
    private BufferIndexHelper[] partialBufferHelper;
    private BufferIndexHelper[] scaleBufferHelper;
    private BufferIndexHelper[] categoryRateBufferHelper;
    
    private BufferIndexHelper[] eigenBufferHelper;
    private BufferIndexHelper[] matrixBufferHelper;

    private PartialsRescalingScheme rescalingScheme;
    private int rescalingFrequency = RESCALE_FREQUENCY;
    private boolean delayRescalingUntilUnderflow = true;

    private int threadCount = -1;

    //allow per partition rescaling
    private boolean[] useScaleFactors;
    private boolean[] recomputeScaleFactors;

    //keep track of underflow on a per partition basis
    private boolean[] everUnderflowed;

    private int[] rescalingCount;
    private int[] rescalingCountInner;

    private boolean firstRescaleAttempt;
    private int rescalingMessageCount = 0;

    // keep track of which partitions need to be updated
    private boolean updatePartition[];
    private boolean updateAllPartitions;
    private boolean partitionWasUpdated[];

    /**
     * the Alignments
     */
    private List<Alignment> Alignments;

    private DataType dataType;

    private int partitionCount;

    /**
     * the pattern weights across all patterns
     */
    private double[] patternWeights;

    /**
     * The partition for each pattern
     */
    private int[] patternPartitions;

    /**
     * the number of patterns for each partition
     */
    private int[] patternCounts;

    /**
     * total number of patterns across all partitions
     */
    private int totalPatternCount;

    /**
     * the number of states in the data
     */
    private int stateCount;

    /**
     * the branch-site model for these sites
     */
    //private List<BranchRateModel> branchModels = new ArrayList<>();

    /**
     * A delegate to handle substitution models on branches
     */
    private List<SubstitutionModel.Base> substitutionModels = new ArrayList<>();

    /**
     * the site model for these sites
     */
    private List<SiteModel> siteRateModels = new ArrayList<>();

    /**
     * the number of rate categories
     */
    private int categoryCount;

    /**
     * the BEAGLE library instance
     */
    private Beagle beagle;

    /**
     * Cached log likelihood for each partition
     */
    private double[] cachedLogLikelihoodsByPartition;
    private double[] storedCachedLogLikelihoodsByPartition;

    /**
     * Flag to specify that the substitution model has changed
     */
    private boolean[] updateSubstitutionModels;

    /**
     * Flag to specify that the site model has changed
     */
    private boolean[] updateSiteRateModels;

    /**
     * Flag to take into account the first likelihood evaluation when initiating the MCMC chain
     */
    private boolean initialEvaluation = true;
    
    
    final class NodeOperation {
        NodeOperation(int nodeNumber, int leftChild, int rightChild) {
            this.nodeNumber = nodeNumber;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }

        public int getNodeNumber() {
            return nodeNumber;
        }

        public int getLeftChild() {
            return leftChild;
        }

        public int getRightChild() {
            return rightChild;
        }

        public String toString() {
            return nodeNumber + "(" + leftChild + "," + rightChild + ")";
        }

        private final int nodeNumber;
        private final int leftChild;
        private final int rightChild;
    }

    final class BranchOperation {
        BranchOperation(int branchNumber, double branchLength) {
            this.branchNumber = branchNumber;
            this.branchLength = branchLength;
        }

        public int getBranchNumber() {
            return branchNumber;
        }

        public double getBranchLength() {
            return branchLength;
        }

        public String toString() {
            return branchNumber + ":" + branchLength;
        }

        private final int branchNumber;
        private final double branchLength;
    }

    @SuppressWarnings("serial")
	class DelegateTypeException extends Exception { }

    @SuppressWarnings("serial")
    class LikelihoodException extends Exception { }

    @SuppressWarnings("serial")
    class LikelihoodUnderflowException extends LikelihoodException { }

    @SuppressWarnings("serial")
    class LikelihoodRescalingException extends LikelihoodException { }

    static boolean IS_MULTI_PARTITION_COMPATIBLE() {
        int[] versionNumbers = BeagleInfo.getVersionNumbers();
        return checkGTEVersion(new int[]{3});
    }
    
    public static boolean IS_ODD_STATE_SSE_FIXED() {
        // SSE for odd state counts fixed in BEAGLE 3.1.3
        return checkGTEVersion(new int[]{3,1,3});
    }

    public static boolean IS_THREAD_COUNT_COMPATIBLE() {
        return checkGTEVersion(new int[]{3,1});
    }

    private static boolean checkGTEVersion(int[] versionNumbers){
        int[] beagleVersionNumbers = BeagleInfo.getVersionNumbers();
        if (versionNumbers.length == 0 || beagleVersionNumbers.length == 0)
            return false;
        for (int i = 0; i < versionNumbers.length && i < beagleVersionNumbers.length; i++){
            if (beagleVersionNumbers[i] > versionNumbers[i])
                return true;
            if (beagleVersionNumbers[i] < versionNumbers[i])
                return false;
        }
        return true;
    }

    public static List<Integer> parseSystemPropertyIntegerArray(String propertyName) {
        List<Integer> order = new ArrayList<>();
        String r = System.getProperty(propertyName);
        if (r != null) {
            String[] parts = r.split(",");
            for (String part : parts) {
                try {
                    int n = Integer.parseInt(part.trim());
                    order.add(n);
                } catch (NumberFormatException nfe) {
                    System.err.println("Invalid entry '" + part + "' in " + propertyName);
                }
            }
        }
        return order;
    }

    public static List<String> parseSystemPropertyStringArray(String propertyName) {

        List<String> order = new ArrayList<>();

        String r = System.getProperty(propertyName);
        if (r != null) {
            String[] parts = r.split(",");
            for (String part : parts) {
                try {
                    String s = part.trim();
                    order.add(s);
                } catch (NumberFormatException nfe) {
                    System.err.println("Invalid entry '" + part + "' in " + propertyName);
                }
            }
        }
        return order;
    }

    /**
     * BufferIndexHelper - helper for double buffering of intermediate computation at nodes.
     *
     * @author Andrew Rambaut
     * @author Marc Suchard
     * @version $Id$
     */
    @SuppressWarnings("serial")
    public class BufferIndexHelper implements Serializable {

        /**
         * @param maxIndexValue the number of possible input values for the index
         * @param minIndexValue the minimum index value to have the mirrored buffers
         */
        public BufferIndexHelper(int maxIndexValue, int minIndexValue) {
            this(maxIndexValue, minIndexValue, 0);
        }

        /**
         * @param maxIndexValue the number of possible input values for the index
         * @param minIndexValue the minimum index value to have the mirrored buffers
         * @param bufferSetNumber provides a total offset of bufferSetNumber * bufferCount
         */
        public BufferIndexHelper(int maxIndexValue, int minIndexValue, int bufferSetNumber) {
            this.minIndexValue = minIndexValue;

            doubleBufferCount = maxIndexValue - minIndexValue;
            indexOffsets = new int[doubleBufferCount];
            storedIndexOffsets = new int[doubleBufferCount];
            indexOffsetsFlipped = new boolean[doubleBufferCount];

            this.constantOffset = bufferSetNumber * getBufferCount();
        }

        public int getBufferCount() {
            return 2 * doubleBufferCount + minIndexValue;
        }

        public void flipOffset(int i) {
            assert(i >= minIndexValue) : "shouldn't be trying to flip the first 'static' indices";

            if (!indexOffsetsFlipped[i - minIndexValue]){ // only flip once before reject / accept
                indexOffsets[i - minIndexValue] = doubleBufferCount - indexOffsets[i - minIndexValue];
                indexOffsetsFlipped[i - minIndexValue] = true;
            }
        }

        public int getOffsetIndex(int i) {
            if (i < minIndexValue) {
                return i + constantOffset;
            }
            return indexOffsets[i - minIndexValue] + i + constantOffset;
        }

        private int getStoredOffsetIndex(int i) {
            assert (i >= minIndexValue);
            return storedIndexOffsets[i - minIndexValue] + i + constantOffset;
        }

        public boolean isSafeUpdate(int i) {
            return getStoredOffsetIndex(i) != getOffsetIndex(i);
        }

        public void storeState() {
            Arrays.fill(indexOffsetsFlipped, false);
            System.arraycopy(indexOffsets, 0, storedIndexOffsets, 0, indexOffsets.length);
        }

        public void restoreState() {
            int[] tmp = storedIndexOffsets;
            storedIndexOffsets = indexOffsets;
            indexOffsets = tmp;
            Arrays.fill(indexOffsetsFlipped, false);
        }

        private final int minIndexValue;
        private final int constantOffset;
        private final int doubleBufferCount;
        private final boolean[] indexOffsetsFlipped;

        private int[] indexOffsets;
        private int[] storedIndexOffsets;

    }//END: class

	@Override
	public List<String> getArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getConditions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sample(State state, Random random) {
		// TODO Auto-generated method stub
		
	}

	
	
    /**
     * Traverse the tree in reverse level order.
     *
     * @param tree tree
     */
    private void traverseReverseLevelOrder(final Tree tree) {

        // create a map of all the operations at each particular level
        Map<Integer, List<NodeOperation>> operationMap = new HashMap<>();

        traverseLevelOrder(tree, tree.getRoot(), 0, operationMap);

        // get the levels as keys in reverse order (they are currently largest towards
        // the tips) and add the operations to the nodeOperation array.
        List<Integer> keyList = new ArrayList<Integer>(operationMap.keySet());
        Collections.sort(keyList, Collections.reverseOrder());

        for (Integer key : keyList) {
            List<NodeOperation> opList = operationMap.get(key);
            for (NodeOperation op : opList) {
                nodeOperations.add(op);
            }
        }
    }

    /**
     * Traverse the tree in level order.
     *
     * @param tree tree
     * @param node node
     * @return boolean
     */
    private boolean traverseLevelOrder(final Tree tree, final Node node,
                                       final int level,
                                       Map<Integer, List<NodeOperation>> operationMap) {
        boolean update = false;

        int nodeNum = node.getNr();

        // First update the transition probability matrix(ices) for this branch
        if (node.getParent() != null && (needsUpdate || node.isDirty()!= Tree.IS_CLEAN)) { //updateNode[nodeNum]) {
            // TODO - at the moment a matrix is updated even if a branch length doesn't change

            // addBranchUpdateOperation(tree, node);
        	double rate = branchRateModel.getRateForBranch(node);
            branchOperations.add(new BranchOperation(node.getNr(), rate * node.getLength()));
                    // computeBranchLength(tree, node)));

            update = true;
        }

        // If the node is internal, update the partial likelihoods.
        if (!node.isLeaf()) {

            // Traverse down the two child nodes incrementing the level (this will give
            // level order but we will reverse these later
            Node child1 = node.getLeft();
            final boolean update1 = traverseLevelOrder(tree, child1, level + 1, operationMap);

            Node child2 = node.getRight();
            final boolean update2 = traverseLevelOrder(tree, child2, level + 1, operationMap);

            // If either child node was updated then update this node too
            if (update1 || update2) {

                List<NodeOperation> ops = operationMap.get(level);
                if (ops == null) {
                    ops = new ArrayList<>();
                    operationMap.put(level, ops);
                }
                ops.add(new NodeOperation(nodeNum, child1.getNr(), child2.getNr()));

                update = true;

            }
        }

        return update;
    }

    /**
     * Add this node to the branchOperation list for updating of the transition probability matrix.
     *
     * @param tree tree
     * @param node node
     */
//    private void addBranchUpdateOperation(final Tree tree, final Node node) {
//        branchOperations.add(new BranchOperation(node.getNr(),
//                computeBranchLength(tree, node)));
//    }

    private final List<BranchOperation> branchOperations = new ArrayList<>();
    private final List<NodeOperation> nodeOperations = new ArrayList<>();

}
