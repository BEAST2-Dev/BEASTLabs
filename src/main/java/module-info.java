open module beast.labs {
    requires beast.base;
    requires beast.pkgmgmt;
    requires java.xml;
    requires java.scripting;
    requires beagle;
    requires org.openjdk.nashorn;
    requires org.apache.commons.statistics.distribution;
    requires org.apache.commons.numbers.gamma;
    requires static beast.fx;
    requires static javafx.controls;

    exports beastlabs.app.beauti;
    exports beastlabs.core;
    exports beastlabs.core.parameter;
    exports beastlabs.core.util;
    exports beastlabs.evolution.alignment;
    exports beastlabs.evolution.branchratemodel;
    exports beastlabs.evolution.likelihood;
    exports beastlabs.evolution.operators;
    exports beastlabs.evolution.sitemodel;
    exports beastlabs.evolution.speciation;
    exports beastlabs.evolution.substitutionmodel;
    exports beastlabs.evolution.taxonomy;
    exports beastlabs.evolution.tree;
    exports beastlabs.evolution.tree.coalescent;
    exports beastlabs.inference;
    exports beastlabs.math.distributions;
    exports beastlabs.parsimony;
    exports beastlabs.prevalence;
    exports beastlabs.tools;
    exports beastlabs.util;

    provides beast.base.core.BEASTInterface with
        beastlabs.core.DataNode,
        beastlabs.core.FilteredValuable,
        beastlabs.core.GridSearch,
        beastlabs.core.Likelihood,
        beastlabs.core.MCMC2,
        beastlabs.core.Posterior,
        beastlabs.core.parameter.CompoundRealParameter,
        beastlabs.core.parameter.NormalisedRealParameter,
        beastlabs.core.util.LoggableSum,
        beastlabs.core.util.ParameterConstrainer,
        beastlabs.core.util.Slice,
        beastlabs.evolution.alignment.PrunedAlignment,
        beastlabs.evolution.branchratemodel.PrunedRelaxedClockModel,
        beastlabs.evolution.likelihood.AncestralStateLogger,
        beastlabs.evolution.likelihood.ExperimentalTreeLikelihood,
        beastlabs.evolution.likelihood.MultiPartitionTreeLikelihood,
        beastlabs.evolution.likelihood.SelfTuningCompoundDistribution,
        beastlabs.evolution.likelihood.SelfTuningMCMC,
        beastlabs.evolution.likelihood.SupertreeLikelihood,
        beastlabs.evolution.likelihood.TraitedTreeLikelihood,
        beastlabs.evolution.operators.AttachAndUniformOperator,
        beastlabs.evolution.operators.AttachOperator,
        beastlabs.evolution.operators.CladeInternalAttachOperator,
        beastlabs.evolution.operators.CombinedOperator,
        beastlabs.evolution.operators.CompoundOperator,
        beastlabs.evolution.operators.MultiCladeAttachOperator,
        beastlabs.evolution.operators.NNI,
        beastlabs.evolution.operators.RestrictedSubtreeSlide,
        beastlabs.evolution.operators.RootHeightScaleOperator,
        beastlabs.evolution.operators.SPR,
        beastlabs.evolution.operators.TreeWithMetaDataRandomWalker,
        beastlabs.evolution.operators.UniformOperatorSelective,
        beastlabs.evolution.operators.UniformSelective,
        beastlabs.evolution.sitemodel.SiteModelGI,
        beastlabs.evolution.speciation.RandomLocalYuleModel,
        beastlabs.evolution.substitutionmodel.CladeSubstitutionModel,
        beastlabs.evolution.substitutionmodel.EmpiricalAAModelFromFile,
        beastlabs.evolution.substitutionmodel.EpochSubstitutionModel,
        beastlabs.evolution.substitutionmodel.GeneralLazySubstitutionModel,
        beastlabs.evolution.substitutionmodel.LazyHKY,
        beastlabs.evolution.tree.ConstrainedClusterTree,
        beastlabs.evolution.tree.ConstrainedRandomTree,
        beastlabs.evolution.tree.InitParamFromTree,
        beastlabs.evolution.tree.MonophyleticConstraint,
        beastlabs.evolution.tree.PrunedTree,
        beastlabs.evolution.tree.RNNIMetric,
        beastlabs.evolution.tree.RobinsonsFouldMetric,
        beastlabs.evolution.tree.SimpleConstrainedRandomTree,
        beastlabs.evolution.tree.SimplePrunedTree,
        beastlabs.evolution.tree.SimpleRandomTree,
        beastlabs.evolution.tree.TreeDistanceLogger,
        beastlabs.evolution.tree.coalescent.CauchyPlusConstant,
        beastlabs.evolution.tree.coalescent.ExponentialGrowthPlusConstant,
        beastlabs.evolution.tree.coalescent.StructuredCoalescentTree,
        beastlabs.inference.ConvergableMCMC,
        beastlabs.inference.HeatedMCMC,
        beastlabs.inference.IndependentMCMC,
        beastlabs.inference.MCMCMC,
        beastlabs.inference.MCMCParticle,
        beastlabs.inference.MCMCParticleAsync,
        beastlabs.inference.ML,
        beastlabs.inference.MultiMCMC,
        beastlabs.inference.ParticleFilter,
        beastlabs.inference.SimulatedAnnealing,
        beastlabs.inference.TreeStoreLogger,
        beastlabs.math.distributions.BernoulliDistribution,
        beastlabs.math.distributions.BetaRange,
        beastlabs.math.distributions.ExcludablePrior,
        beastlabs.math.distributions.ExcludablePriorIndex,
        beastlabs.math.distributions.GammaOneP,
        beastlabs.math.distributions.MRCAPriorWithRogues,
        beastlabs.math.distributions.MixtureDistribution,
        beastlabs.math.distributions.MonoPoints,
        beastlabs.math.distributions.MultiMRCAPriors,
        beastlabs.math.distributions.MultiMonophyleticConstraint,
        beastlabs.math.distributions.RandomCompositionPositive,
        beastlabs.math.distributions.SingleParamGamma,
        beastlabs.math.distributions.WeibullDistribution,
        beastlabs.math.distributions.WeightedDirichlet,
        beastlabs.parsimony.FitchParsimony32,
        beastlabs.parsimony.FitchParsimony64,
        beastlabs.prevalence.AddOperator,
        beastlabs.prevalence.DelOperator,
        beastlabs.prevalence.MoveOperator,
        beastlabs.prevalence.PrevalenceLikelihood,
        beastlabs.prevalence.PrevalenceList,
        beastlabs.prevalence.SubtreeSlide,
        beastlabs.prevalence.TreeForPrevalenceLikelihood,
        beastlabs.prevalence.TreeScaleOperator,
        beastlabs.prevalence.Uniform,
        beastlabs.prevalence.WilsonBalding,
        beastlabs.tools.PostHocAnalyser,
        beastlabs.tools.TraceStateNodeSource,
        beastlabs.tools.TreeStateNodeSource,
        beastlabs.util.Script,
        beastlabs.util.TimeLogger;
}
