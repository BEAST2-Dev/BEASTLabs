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
    requires static beast.test.utils;

    exports beastlabs.core;
    exports beastlabs.core.parameter;
    exports beastlabs.core.util;
    exports beastlabs.evolution.alignment;
    exports beastlabs.evolution.branchratemodel;
    exports beastlabs.evolution.likelihood;
    exports beastlabs.evolution.operators;
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
        beastlabs.core.Posterior,
        beastlabs.core.util.LoggableSum,
        beastlabs.core.util.ParameterConstrainer,
        beastlabs.core.util.Slice,
        beastlabs.evolution.likelihood.ExperimentalTreeLikelihood,
        beastlabs.evolution.likelihood.MultiPartitionTreeLikelihood,
        beastlabs.evolution.likelihood.SelfTuningCompoundDistribution,
        beastlabs.evolution.likelihood.SupertreeLikelihood,
        beastlabs.evolution.operators.TreeWithMetaDataRandomWalker,
        beastlabs.evolution.operators.UniformOperatorSelective,
        beastlabs.evolution.tree.MonophyleticConstraint,
        beastlabs.evolution.tree.RNNIMetric,
        beastlabs.evolution.tree.RobinsonsFouldMetric,
        beastlabs.evolution.tree.TreeDistanceLogger,
        beastlabs.math.distributions.BernoulliDistribution,
        beastlabs.math.distributions.MixtureDistribution,
        beastlabs.math.distributions.MonoPoints,
        beastlabs.math.distributions.MultiMonophyleticConstraint,
        beastlabs.math.distributions.RandomCompositionPositive,
        beastlabs.parsimony.FitchParsimony32,
        beastlabs.parsimony.FitchParsimony64,
        beastlabs.prevalence.AddOperator,
        beastlabs.prevalence.DelOperator,
        beastlabs.prevalence.MoveOperator,
        beastlabs.prevalence.PrevalenceLikelihood,
        beastlabs.prevalence.TreeForPrevalenceLikelihood,
        beastlabs.tools.TraceStateNodeSource,
        beastlabs.tools.TreeStateNodeSource,
        beastlabs.util.TimeLogger;
}
