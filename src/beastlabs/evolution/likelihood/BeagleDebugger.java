package beastlabs.evolution.likelihood;

import java.io.Serializable;
import beagle.Beagle;
import beagle.InstanceDetails;
import beast.base.evolution.likelihood.BeagleTreeLikelihood;
import cern.colt.Arrays;

public class BeagleDebugger implements Beagle {
	Beagle beagle;

	public BeagleDebugger(Beagle beagle) {
		this.beagle = beagle;
	}
	
	public void finalize() throws Throwable {
		beagle.finalize();
	}

	private void print(String str) {
		System.out.println("calling " + str + "()");
	}

	private void print(String str, int v) {
		System.out.println(" " + str + " = " + v);
	}

	private void print(String str, double v) {
		System.out.println(" " + str + " = " + v);
	}

	private void print(String str, int[] v) {
		System.out.println(" " + str + " = " + (v == null ? "null" : Arrays.toString(v)));
	}

	private void print(String str, double[] v) {
		System.out.println(" " + str + " = " + (v == null ? "null" : Arrays.toString(v)));
	}

	public void setCPUThreadCount(int threadCount) {
		print("setCPUThreadCount");
		print("threadCount", threadCount);

		beagle.setCPUThreadCount(threadCount);
	}

	public void setPatternWeights(final double[] patternWeights) {
		print("setPatternWeights");
		print("patternWeights", patternWeights);

		beagle.setPatternWeights(patternWeights);
	}

	public void setPatternPartitions(int partitionCount, final int[] patternPartitions) {
		print("setPatternPartitions");
		print("partitionCount", partitionCount);
		print("patternPartitions", patternPartitions);

		beagle.setPatternPartitions(partitionCount, patternPartitions);
	}

	public void setTipStates(int tipIndex, final int[] inStates) {
		print("setTipStates");
		print("tipIndex", tipIndex);
		print("inStates", inStates);

		beagle.setTipStates(tipIndex, inStates);
	}

	public void getTipStates(int tipIndex, final int[] outStates) {
		print("getTipStates");
		print("tipIndex", tipIndex);
		print("outStates", outStates);

		beagle.getTipStates(tipIndex, outStates);
	}

	public void setTipPartials(int tipIndex, final double[] inPartials) {
		print("setTipPartials");
		print("tipIndex", tipIndex);
		print("inPartials", inPartials);

		beagle.setTipPartials(tipIndex, inPartials);
	}

//	public void setRootPrePartials(            final int[] inbufferIndices,
//            final int[] instateFrequenciesIndices,
//            int count
//    )
//{
//print("setRootPrePartials");
//print("inbufferIndices",inbufferIndices);
//print("instateFrequenciesIndices",instateFrequenciesIndices);
//print("count",count);
//
//beagle.setRootPrePartials(inbufferIndices,
//instateFrequenciesIndices,
//count
//);
//}

	public void setPartials(int bufferIndex, final double[] inPartials) {
		print("setPartials");
		print("bufferIndex", bufferIndex);
		print("inPartials", inPartials);

		beagle.setPartials(bufferIndex, inPartials);
	}

	public void getPartials(int bufferIndex, int scaleIndex, final double[] outPartials) {
		print("getPartials");
		print("bufferIndex", bufferIndex);
		print("scaleIndex", scaleIndex);
		print("outPartials", outPartials);

		beagle.getPartials(bufferIndex, scaleIndex, outPartials);
	}

	public void getLogScaleFactors(int scaleIndex, final double[] outFactors) {
		print("getLogScaleFactors");
		print("scaleIndex", scaleIndex);
		print("outFactors", outFactors);

		beagle.getLogScaleFactors(scaleIndex, outFactors);
	}

	public void setEigenDecomposition(int eigenIndex, final double[] inEigenVectors, final double[] inInverseEigenVectors,
			final double[] inEigenValues) {
		print("setEigenDecomposition");
		print("eigenIndex", eigenIndex);
		print("inEigenVectors", inEigenVectors);
		print("inInverseEigenVectors", inInverseEigenVectors);
		print("inEigenValues", inEigenValues);

		beagle.setEigenDecomposition(eigenIndex, inEigenVectors, inInverseEigenVectors, inEigenValues);
	}

	public void setStateFrequencies(int stateFrequenciesIndex, final double[] stateFrequencies) {
		print("setStateFrequencies");
		print("stateFrequenciesIndex", stateFrequenciesIndex);
		print("stateFrequencies", stateFrequencies);

		beagle.setStateFrequencies(stateFrequenciesIndex, stateFrequencies);
	}

	public void setCategoryWeights(int categoryWeightsIndex, final double[] categoryWeights) {
		print("setCategoryWeights");
		print("categoryWeightsIndex", categoryWeightsIndex);
		print("categoryWeights", categoryWeights);

		beagle.setCategoryWeights(categoryWeightsIndex, categoryWeights);
	}

	public void setCategoryRates(final double[] inCategoryRates) {
		print("setCategoryRates");
		print("inCategoryRates", inCategoryRates);

		beagle.setCategoryRates(inCategoryRates);
	}

	public void setCategoryRatesWithIndex(int categoryRatesIndex, final double[] inCategoryRates) {
		print("setCategoryRatesWithIndex");
		print("categoryRatesIndex", categoryRatesIndex);
		print("inCategoryRates", inCategoryRates);

		beagle.setCategoryRatesWithIndex(categoryRatesIndex, inCategoryRates);
	}

	public void convolveTransitionMatrices(final int[] firstIndices, final int[] secondIndices, final int[] resultIndices,
			int matrixCount) {
		print("convolveTransitionMatrices");
		print("firstIndices", firstIndices);
		print("secondIndices", secondIndices);
		print("resultIndices", resultIndices);
		print("matrixCount", matrixCount);

		beagle.convolveTransitionMatrices(firstIndices, secondIndices, resultIndices, matrixCount);
	}

//	public void addTransitionMatrices(final int[] firstIndices, final int[] secondIndices, final int[] resultIndices,
//			int matrixCount) {
//		print("addTransitionMatrices");
//		print("firstIndices", firstIndices);
//		print("secondIndices", secondIndices);
//		print("resultIndices", resultIndices);
//		print("matrixCount", matrixCount);
//
//		beagle.addTransitionMatrices(firstIndices, secondIndices, resultIndices, matrixCount);
//	}
//
//	public void transposeTransitionMatrices(final int[] inIndices, final int[] outIndices, int matrixCount) {
//		print("transposeTransitionMatrices");
//		print("inIndices", inIndices);
//		print("outIndices", outIndices);
//		print("matrixCount", matrixCount);
//
//		beagle.transposeTransitionMatrices(inIndices, outIndices, matrixCount);
//	}

	public void updateTransitionMatrices(int eigenIndex, final int[] probabilityIndices, final int[] firstDerivativeIndices,
			final int[] secondDervativeIndices, final double[] edgeLengths, int count) {
		print("updateTransitionMatrices");
		print("eigenIndex", eigenIndex);
		print("probabilityIndices", probabilityIndices);
		print("firstDerivativeIndices", firstDerivativeIndices);
		print("secondDervativeIndices", secondDervativeIndices);
		print("edgeLengths", edgeLengths);
		print("count", count);

		beagle.updateTransitionMatrices(eigenIndex, probabilityIndices, firstDerivativeIndices, secondDervativeIndices,
				edgeLengths, count);
	}

	public void updateTransitionMatricesWithMultipleModels(final int[] eigenIndices, final int[] categoryRateIndices,
			final int[] probabilityIndices, final int[] firstDerivativeIndices, final int[] secondDervativeIndices,
			final double[] edgeLengths, int count) {
		print("updateTransitionMatricesWithMultipleModels");
		print("eigenIndices", eigenIndices);
		print("categoryRateIndices", categoryRateIndices);
		print("probabilityIndices", probabilityIndices);
		print("firstDerivativeIndices", firstDerivativeIndices);
		print("secondDervativeIndices", secondDervativeIndices);
		print("edgeLengths", edgeLengths);
		print("count", count);

		beagle.updateTransitionMatricesWithMultipleModels(eigenIndices, categoryRateIndices, probabilityIndices,
				firstDerivativeIndices, secondDervativeIndices, edgeLengths, count);
	}

	public void setTransitionMatrix(int matrixIndex, final double[] inMatrix, double paddedValue) {
		print("setTransitionMatrix");
		print("matrixIndex", matrixIndex);
		print("inMatrix", inMatrix);
		print("paddedValue", paddedValue);

		beagle.setTransitionMatrix(matrixIndex, inMatrix, paddedValue);
	}

//	public void setDifferentialMatrix(int matrixIndex, final double[] inMatrix) {
//		print("setDifferentialMatrix");
//		print("matrixIndex", matrixIndex);
//		print("inMatrix", inMatrix);
//
//		beagle.setDifferentialMatrix(matrixIndex, inMatrix);
//	}

	public void getTransitionMatrix(int matrixIndex, double[] outMatrix) {
		print("getTransitionMatrix");
		print("matrixIndex", matrixIndex);
		print("outMatrix", outMatrix);

		beagle.getTransitionMatrix(matrixIndex, outMatrix);
	}

//	public void updatePrePartials(final int[] operations, int operationCount, int cumulativeScaleIndex) {
//		print("updatePrePartials");
//		print("operations", operations);
//		print("operationCount", operationCount);
//		print("cumulativeScaleIndex", cumulativeScaleIndex);
//
//		beagle.updatePrePartials(operations, operationCount, cumulativeScaleIndex);
//	}

//	public void updatePrePartialsByPartition(final int[] operations, int operationCount) {
//		print("updatePrePartialsByPartition");
//		print("operations", operations);
//		print("operationCount", operationCount);
//
//		beagle.updatePrePartialsByPartition(operations, operationCount);
//	}

//	public void calculateEdgeDerivative(final int[] postBufferIndices, final int[] preBufferIndices, final int rootBufferIndex,
//			final int[] firstDerivativeIndices, final int[] secondDerivativeIndices, final int categoryWeightsIndex,
//			final int categoryRatesIndex, final int stateFrequenciesIndex, final int[] cumulativeScaleIndices,
//			int count, double[] outFirstDerivative, double[] outDiagonalSecondDerivative) {
//		print("calculateEdgeDerivative");
//		print("postBufferIndices", postBufferIndices);
//		print("preBufferIndices", preBufferIndices);
//		print("rootBufferIndex", rootBufferIndex);
//		print("firstDerivativeIndices", firstDerivativeIndices);
//		print("secondDerivativeIndices", secondDerivativeIndices);
//		print("categoryWeightsIndex", categoryWeightsIndex);
//		print("categoryRatesIndex", categoryRatesIndex);
//		print("stateFrequenciesIndex", stateFrequenciesIndex);
//		print("cumulativeScaleIndices", cumulativeScaleIndices);
//		print("count", count);
//		print("outFirstDerivative", outFirstDerivative);
//		print("outDiagonalSecondDerivative", outDiagonalSecondDerivative);
//
//		beagle.calculateEdgeDerivative(postBufferIndices, preBufferIndices, rootBufferIndex, firstDerivativeIndices,
//				secondDerivativeIndices, categoryWeightsIndex, categoryRatesIndex, stateFrequenciesIndex,
//				cumulativeScaleIndices, count, outFirstDerivative, outDiagonalSecondDerivative);
//	}

//	public void calculateEdgeDifferentials(final int[] postBufferIndices, final int[] preBufferIndices,
//			final int[] derivativeMatrixIndices, final int[] categoryWeightsIndices, int count, double[] outDerivatives,
//			double[] outSumDerivatives, double[] outSumSquaredDerivatives) {
//		print("calculateEdgeDifferentials");
//		print("postBufferIndices", postBufferIndices);
//		print("preBufferIndices", preBufferIndices);
//		print("derivativeMatrixIndices", derivativeMatrixIndices);
//		print("categoryWeightsIndices", categoryWeightsIndices);
//		print("count", count);
//		print("outDerivatives", outDerivatives);
//		print("outSumDerivatives", outSumDerivatives);
//		print("outSumSquaredDerivatives", outSumSquaredDerivatives);
//
//		beagle.calculateEdgeDifferentials(postBufferIndices, preBufferIndices, derivativeMatrixIndices,
//				categoryWeightsIndices, count, outDerivatives, outSumDerivatives, outSumSquaredDerivatives);
//	}

//	public void calculateCrossProductDifferentials(final int[] postBufferIndices, final int[] preBufferIndices,
//			final int[] categoryRateIndices, final int[] categoryWeightsIndices, final double[] edgeLengths, int count,
//			double[] outSumDerivatives, double[] outSumSquaredDerivatives) {
//		print("calculateCrossProductDifferentials");
//		print("postBufferIndices", postBufferIndices);
//		print("preBufferIndices", preBufferIndices);
//		print("categoryRateIndices", categoryRateIndices);
//		print("categoryWeightsIndices", categoryWeightsIndices);
//		print("edgeLengths", edgeLengths);
//		print("count", count);
//		print("outSumDerivatives", outSumDerivatives);
//		print("outSumSquaredDerivatives", outSumSquaredDerivatives);
//
//		beagle.calculateCrossProductDifferentials(postBufferIndices, preBufferIndices, categoryRateIndices,
//				categoryWeightsIndices, edgeLengths, count, outSumDerivatives, outSumSquaredDerivatives);
//	}

	public void updatePartials(final int[] operations, int operationCount, int cumulativeScaleIndex) {
		print("updatePartials");
		print("operations", operations);
		print("operationCount", operationCount);
		print("cumulativeScaleIndex", cumulativeScaleIndex);

		beagle.updatePartials(operations, operationCount, cumulativeScaleIndex);
	}

	public void updatePartialsByPartition(final int[] operations, int operationCount) {
		print("updatePartialsByPartition");
		print("operations", operations);
		print("operationCount", operationCount);

		beagle.updatePartialsByPartition(operations, operationCount);
	}

	public void accumulateScaleFactors(final int[] scaleIndices, final int count, final int cumulativeScaleIndex) {
		print("accumulateScaleFactors");
		print("scaleIndices", scaleIndices);
		print("count", count);
		print("cumulativeScaleIndex", cumulativeScaleIndex);

		beagle.accumulateScaleFactors(scaleIndices, count, cumulativeScaleIndex);
	}

	public void accumulateScaleFactorsByPartition(final int[] scaleIndices, int count, int cumulativeScaleIndex,
			int partitionIndex) {
		print("accumulateScaleFactorsByPartition");
		print("scaleIndices", scaleIndices);
		print("count", count);
		print("cumulativeScaleIndex", cumulativeScaleIndex);
		print("partitionIndex", partitionIndex);

		beagle.accumulateScaleFactorsByPartition(scaleIndices, count, cumulativeScaleIndex, partitionIndex);
	}

	public void removeScaleFactors(final int[] scaleIndices, final int count, final int cumulativeScaleIndex) {
		print("removeScaleFactors");
		print("scaleIndices", scaleIndices);
		print("count", count);
		print("cumulativeScaleIndex", cumulativeScaleIndex);

		beagle.removeScaleFactors(scaleIndices, count, cumulativeScaleIndex);
	}

	public void removeScaleFactorsByPartition(final int[] scaleIndices, final int count, final int cumulativeScaleIndex,
			final int partitionIndex) {
		print("removeScaleFactorsByPartition");
		print("scaleIndices", scaleIndices);
		print("count", count);
		print("cumulativeScaleIndex", cumulativeScaleIndex);
		print("partitionIndex", partitionIndex);

		beagle.removeScaleFactorsByPartition(scaleIndices, count, cumulativeScaleIndex, partitionIndex);
	}

	public void copyScaleFactors(int destScalingIndex, int srcScalingIndex) {
		print("copyScaleFactors");
		print("destScalingIndex", destScalingIndex);
		print("srcScalingIndex", srcScalingIndex);

		beagle.copyScaleFactors(destScalingIndex, srcScalingIndex);
	}

	public void resetScaleFactors(int cumulativeScaleIndex) {
		print("resetScaleFactors");
		print("cumulativeScaleIndex", cumulativeScaleIndex);

		beagle.resetScaleFactors(cumulativeScaleIndex);
	}

	public void resetScaleFactorsByPartition(int cumulativeScaleIndex, int partitionIndex) {
		print("resetScaleFactorsByPartition");
		print("cumulativeScaleIndex", cumulativeScaleIndex);
		print("partitionIndex", partitionIndex);

		beagle.resetScaleFactorsByPartition(cumulativeScaleIndex, partitionIndex);
	}

	public void calculateRootLogLikelihoods(int[] bufferIndices, int[] categoryWeightsIndices, int[] stateFrequenciesIndices,
			int[] cumulativeScaleIndices, int count, double[] outSumLogLikelihood) {
		print("calculateRootLogLikelihoods");
		print("bufferIndices", bufferIndices);
		print("categoryWeightsIndices", categoryWeightsIndices);
		print("stateFrequenciesIndices", stateFrequenciesIndices);
		print("cumulativeScaleIndices", cumulativeScaleIndices);
		print("count", count);
		print("outSumLogLikelihood", outSumLogLikelihood);

		beagle.calculateRootLogLikelihoods(bufferIndices, categoryWeightsIndices, stateFrequenciesIndices,
				cumulativeScaleIndices, count, outSumLogLikelihood);
	}

	public void calculateRootLogLikelihoodsByPartition(int[] bufferIndices, int[] categoryWeightsIndices,
			int[] stateFrequenciesIndices, int[] cumulativeScaleIndices, int[] partitionIndices, int partitionCount,
			int count, double[] outSumLogLikelihoodByPartition, double[] outSumLogLikelihood) {
		print("calculateRootLogLikelihoodsByPartition");
		print("bufferIndices", bufferIndices);
		print("categoryWeightsIndices", categoryWeightsIndices);
		print("stateFrequenciesIndices", stateFrequenciesIndices);
		print("cumulativeScaleIndices", cumulativeScaleIndices);
		print("partitionIndices", partitionIndices);
		print("partitionCount", partitionCount);
		print("count", count);
		print("outSumLogLikelihoodByPartition", outSumLogLikelihoodByPartition);
		print("outSumLogLikelihood", outSumLogLikelihood);

		beagle.calculateRootLogLikelihoodsByPartition(bufferIndices, categoryWeightsIndices, stateFrequenciesIndices,
				cumulativeScaleIndices, partitionIndices, partitionCount, count, outSumLogLikelihoodByPartition,
				outSumLogLikelihood);
	}

	public void getSiteLogLikelihoods(double[] outLogLikelihoods) {
		print("getSiteLogLikelihoods");
		print("outLogLikelihoods", outLogLikelihoods);

		beagle.getSiteLogLikelihoods(outLogLikelihoods);
	}

	public InstanceDetails getDetails() {
		print("getDetails");

		return beagle.getDetails();
	}
}
