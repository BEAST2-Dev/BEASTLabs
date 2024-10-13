package beastlabs.evolution.likelihood;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import beagle.Beagle;
import beagle.InstanceDetails;
import cern.colt.Arrays;

public class BeagleDebugger implements Beagle {
	Beagle beagle;
	
	public boolean output = false;

	public BeagleDebugger(Beagle beagle, boolean output) {
		this.beagle = beagle;
		this.output = output;
	}
	
	public void finalize() throws Throwable {
		beagle.finalize();
	}


	private PrintStream out;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private static String utf8 = StandardCharsets.UTF_8.name();
	
	private void print() {
		if (output) {
			try {
				String str = baos.toString(utf8);
				str = str.substring(0, str.length()-2) + ");\n"; 
				System.out.println(str);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void print(String str) {
		baos = new ByteArrayOutputStream();
	    try {
			out = new PrintStream(baos, true, utf8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		out.println("/*calling */" + str + "(");
	}

	private void print(String str, int v) {
		out.println(" /*" + str + " = */" + v + ",");
	}

	private void print(String str, double v) {
		out.println(" /*" + str + " = */" + v + ",");
	}

	private void print(String str, int[] v) {
		out.println(" /*" + str + " = */" + (v == null ? "null" : "new int[] {" + Arrays.toString(v).replaceAll("[\\[\\]]","") + "},"));
	}

	private void print(String str, double[] v) {
		out.println(" /*" + str + " = */" + (v == null ? "null" : "new double[] {" + Arrays.toString(v).replaceAll("[\\[\\]]","") + "},"));
	}

	public void setCPUThreadCount(int threadCount) {
		print("setCPUThreadCount");
		print("threadCount", threadCount);

print();
		beagle.setCPUThreadCount(threadCount);
	}

	public void setPatternWeights(final double[] patternWeights) {
		print("setPatternWeights");
		print("patternWeights", patternWeights);

print();
		beagle.setPatternWeights(patternWeights);
	}

	public void setPatternPartitions(int partitionCount, final int[] patternPartitions) {
		print("setPatternPartitions");
		print("partitionCount", partitionCount);
		print("patternPartitions", patternPartitions);

print();
		beagle.setPatternPartitions(partitionCount, patternPartitions);
	}

	public void setTipStates(int tipIndex, final int[] inStates) {
		print("setTipStates");
		print("tipIndex", tipIndex);
		print("inStates", inStates);

print();
		beagle.setTipStates(tipIndex, inStates);
	}

	public void getTipStates(int tipIndex, final int[] outStates) {
		print("getTipStates");
		print("tipIndex", tipIndex);
		print("outStates", outStates);

print();
		beagle.getTipStates(tipIndex, outStates);
	}

	public void setTipPartials(int tipIndex, final double[] inPartials) {
		print("setTipPartials");
		print("tipIndex", tipIndex);
		print("inPartials", inPartials);

print();
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

print();
		beagle.setPartials(bufferIndex, inPartials);
	}

	public void getPartials(int bufferIndex, int scaleIndex, final double[] outPartials) {
		print("getPartials");
		print("bufferIndex", bufferIndex);
		print("scaleIndex", scaleIndex);
		print("outPartials", outPartials);

print();
		beagle.getPartials(bufferIndex, scaleIndex, outPartials);
	}

	public void getLogScaleFactors(int scaleIndex, final double[] outFactors) {
		print("getLogScaleFactors");
		print("scaleIndex", scaleIndex);
		print("outFactors", outFactors);

print();
		beagle.getLogScaleFactors(scaleIndex, outFactors);
	}

	public void setEigenDecomposition(int eigenIndex, final double[] inEigenVectors, final double[] inInverseEigenVectors,
			final double[] inEigenValues) {
		print("setEigenDecomposition");
		print("eigenIndex", eigenIndex);
		print("inEigenVectors", inEigenVectors);
		print("inInverseEigenVectors", inInverseEigenVectors);
		print("inEigenValues", inEigenValues);

print();
		beagle.setEigenDecomposition(eigenIndex, inEigenVectors, inInverseEigenVectors, inEigenValues);
	}

	public void setStateFrequencies(int stateFrequenciesIndex, final double[] stateFrequencies) {
		print("setStateFrequencies");
		print("stateFrequenciesIndex", stateFrequenciesIndex);
		print("stateFrequencies", stateFrequencies);

print();
		beagle.setStateFrequencies(stateFrequenciesIndex, stateFrequencies);
	}

	public void setCategoryWeights(int categoryWeightsIndex, final double[] categoryWeights) {
		print("setCategoryWeights");
		print("categoryWeightsIndex", categoryWeightsIndex);
		print("categoryWeights", categoryWeights);

print();
		beagle.setCategoryWeights(categoryWeightsIndex, categoryWeights);
	}

	public void setCategoryRates(final double[] inCategoryRates) {
		print("setCategoryRates");
		print("inCategoryRates", inCategoryRates);

print();
		beagle.setCategoryRates(inCategoryRates);
	}

	public void setCategoryRatesWithIndex(int categoryRatesIndex, final double[] inCategoryRates) {
		print("setCategoryRatesWithIndex");
		print("categoryRatesIndex", categoryRatesIndex);
		print("inCategoryRates", inCategoryRates);

print();
		beagle.setCategoryRatesWithIndex(categoryRatesIndex, inCategoryRates);
	}

	public void convolveTransitionMatrices(final int[] firstIndices, final int[] secondIndices, final int[] resultIndices,
			int matrixCount) {
		print("convolveTransitionMatrices");
		print("firstIndices", firstIndices);
		print("secondIndices", secondIndices);
		print("resultIndices", resultIndices);
		print("matrixCount", matrixCount);

print();
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
//print();
//		beagle.addTransitionMatrices(firstIndices, secondIndices, resultIndices, matrixCount);
//	}
//
//	public void transposeTransitionMatrices(final int[] inIndices, final int[] outIndices, int matrixCount) {
//		print("transposeTransitionMatrices");
//		print("inIndices", inIndices);
//		print("outIndices", outIndices);
//		print("matrixCount", matrixCount);
//
//print();
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

print();
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

print();
		beagle.updateTransitionMatricesWithMultipleModels(eigenIndices, categoryRateIndices, probabilityIndices,
				firstDerivativeIndices, secondDervativeIndices, edgeLengths, count);
	}

	public void setTransitionMatrix(int matrixIndex, final double[] inMatrix, double paddedValue) {
		print("setTransitionMatrix");
		print("matrixIndex", matrixIndex);
		print("inMatrix", inMatrix);
		print("paddedValue", paddedValue);

print();
		beagle.setTransitionMatrix(matrixIndex, inMatrix, paddedValue);
	}

//	public void setDifferentialMatrix(int matrixIndex, final double[] inMatrix) {
//		print("setDifferentialMatrix");
//		print("matrixIndex", matrixIndex);
//		print("inMatrix", inMatrix);
//
//print();
//		beagle.setDifferentialMatrix(matrixIndex, inMatrix);
//	}

	public void getTransitionMatrix(int matrixIndex, double[] outMatrix) {
		print("getTransitionMatrix");
		print("matrixIndex", matrixIndex);
		print("outMatrix", outMatrix);

print();
		beagle.getTransitionMatrix(matrixIndex, outMatrix);
	}

//	public void updatePrePartials(final int[] operations, int operationCount, int cumulativeScaleIndex) {
//		print("updatePrePartials");
//		print("operations", operations);
//		print("operationCount", operationCount);
//		print("cumulativeScaleIndex", cumulativeScaleIndex);
//
//print();
//		beagle.updatePrePartials(operations, operationCount, cumulativeScaleIndex);
//	}
//
//	public void updatePrePartialsByPartition(final int[] operations, int operationCount) {
//		print("updatePrePartialsByPartition");
//		print("operations", operations);
//		print("operationCount", operationCount);
//
//print();
//		beagle.updatePrePartialsByPartition(operations, operationCount);
//	}
//
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
//print();
//		beagle.calculateEdgeDerivative(postBufferIndices, preBufferIndices, rootBufferIndex, firstDerivativeIndices,
//				secondDerivativeIndices, categoryWeightsIndex, categoryRatesIndex, stateFrequenciesIndex,
//				cumulativeScaleIndices, count, outFirstDerivative, outDiagonalSecondDerivative);
//	}
//
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
//print();
//		beagle.calculateEdgeDifferentials(postBufferIndices, preBufferIndices, derivativeMatrixIndices,
//				categoryWeightsIndices, count, outDerivatives, outSumDerivatives, outSumSquaredDerivatives);
//	}
//
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
//print();
//		beagle.calculateCrossProductDifferentials(postBufferIndices, preBufferIndices, categoryRateIndices,
//				categoryWeightsIndices, edgeLengths, count, outSumDerivatives, outSumSquaredDerivatives);
//	}

	public void updatePartials(final int[] operations, int operationCount, int cumulativeScaleIndex) {
		print("updatePartials");
		print("operations", operations);
		print("operationCount", operationCount);
		print("cumulativeScaleIndex", cumulativeScaleIndex);

print();
		beagle.updatePartials(operations, operationCount, cumulativeScaleIndex);
	}

	public void updatePartialsByPartition(final int[] operations, int operationCount) {
		print("updatePartialsByPartition");
		print("operations", operations);
		print("operationCount", operationCount);

print();
		beagle.updatePartialsByPartition(operations, operationCount);
	}

	public void accumulateScaleFactors(final int[] scaleIndices, final int count, final int cumulativeScaleIndex) {
		print("accumulateScaleFactors");
		print("scaleIndices", scaleIndices);
		print("count", count);
		print("cumulativeScaleIndex", cumulativeScaleIndex);

print();
		beagle.accumulateScaleFactors(scaleIndices, count, cumulativeScaleIndex);
	}

	public void accumulateScaleFactorsByPartition(final int[] scaleIndices, int count, int cumulativeScaleIndex,
			int partitionIndex) {
		print("accumulateScaleFactorsByPartition");
		print("scaleIndices", scaleIndices);
		print("count", count);
		print("cumulativeScaleIndex", cumulativeScaleIndex);
		print("partitionIndex", partitionIndex);

print();
		beagle.accumulateScaleFactorsByPartition(scaleIndices, count, cumulativeScaleIndex, partitionIndex);
	}

	public void removeScaleFactors(final int[] scaleIndices, final int count, final int cumulativeScaleIndex) {
		print("removeScaleFactors");
		print("scaleIndices", scaleIndices);
		print("count", count);
		print("cumulativeScaleIndex", cumulativeScaleIndex);

print();
		beagle.removeScaleFactors(scaleIndices, count, cumulativeScaleIndex);
	}

	public void removeScaleFactorsByPartition(final int[] scaleIndices, final int count, final int cumulativeScaleIndex,
			final int partitionIndex) {
		print("removeScaleFactorsByPartition");
		print("scaleIndices", scaleIndices);
		print("count", count);
		print("cumulativeScaleIndex", cumulativeScaleIndex);
		print("partitionIndex", partitionIndex);

print();
		beagle.removeScaleFactorsByPartition(scaleIndices, count, cumulativeScaleIndex, partitionIndex);
	}

	public void copyScaleFactors(int destScalingIndex, int srcScalingIndex) {
		print("copyScaleFactors");
		print("destScalingIndex", destScalingIndex);
		print("srcScalingIndex", srcScalingIndex);

print();
		beagle.copyScaleFactors(destScalingIndex, srcScalingIndex);
	}

	public void resetScaleFactors(int cumulativeScaleIndex) {
		print("resetScaleFactors");
		print("cumulativeScaleIndex", cumulativeScaleIndex);

print();
		beagle.resetScaleFactors(cumulativeScaleIndex);
	}

	public void resetScaleFactorsByPartition(int cumulativeScaleIndex, int partitionIndex) {
		print("resetScaleFactorsByPartition");
		print("cumulativeScaleIndex", cumulativeScaleIndex);
		print("partitionIndex", partitionIndex);

print();
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

print();
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

print();
		beagle.calculateRootLogLikelihoodsByPartition(bufferIndices, categoryWeightsIndices, stateFrequenciesIndices,
				cumulativeScaleIndices, partitionIndices, partitionCount, count, outSumLogLikelihoodByPartition,
				outSumLogLikelihood);
	}

	public void getSiteLogLikelihoods(double[] outLogLikelihoods) {
		print("getSiteLogLikelihoods");
		print("outLogLikelihoods", outLogLikelihoods);

print();
		beagle.getSiteLogLikelihoods(outLogLikelihoods);
	}

	public InstanceDetails getDetails() {
		print("getDetails");

		return
 beagle.getDetails();
	}
}
