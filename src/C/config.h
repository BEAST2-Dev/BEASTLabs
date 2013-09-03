#define BLOCK_SIZE 16

#ifdef DOUBLE_PRECISION
#define REAL            double
#define jREAL           jdouble
#define jREALArray      jdoubleArray

#else
#define REAL            float
#define jREAL           jfloat
#define jREALArray      jfloatArray
#endif

#define GET_JDOUBLE_ARRY      (jdouble *)env->GetPrimitiveArrayCritical
#define GET_JINT_ARRY         (jint *)env->GetPrimitiveArrayCritical
//#define GET_JDOUBLE_ARRY      env->GetDoubleArrayElements
//#define GET_JINT_ARRY         env->GetIntArrayElements
#define RELEASE_JDOUBLE_ARRAY env->ReleasePrimitiveArrayCritical
#define RELEASE_JINT_ARRAY    env->ReleasePrimitiveArrayCritical
//#define RELEASE_JDOUBLE_ARRAY env->ReleaseDoubleArrayElements
//#define RELEASE_JINT_ARRAY    env->ReleaseIntArrayElements

bool initDevice(int nDevice);
void releaseDevice();
bool allocateGPU(int nPartialsSize, int nStatesSize,int nMatricesSize,int nPatterns, int nRootPartialsSize, int nStates);
void memtest();

void setPatternWeightsGPU(int * nPatterWeights, int nPatters);
void setProportionInvariantGPU(double m_fProportionInvariant, int * iConstantPatterns, int nConstantPatterns, int nPatters);
void setNodePartialsGPU(int iNode, double * fPartials, int nPartialsSize, int nPartialsSizeGPU);
void setNodeStatesGPU(int iNode, int * iStates, int nStateSize, int nStateSizeGPU);
void setNodeMatrixGPU(int iNode, double * fMatrix, int iOffset, int nMatrixSize);


void calcAllMatrixSSPGPU(int iNode1, int iNode2, int iNode3, int * iCurrentPartials, int * iCurrentMatrices,
						const int nPatterns, const int nStates, const int nMatrices, const int nMatrixSize,
						const int nPartialsSizeGPU, const int nTotalPartialsSizeGPU,
						const int nStatesSizeGPU, const int nTotalStatesSizeGPU, int nTotalMatrixSizeGPU, double fScale);
void calcAllMatrixSPPGPU(int iNode1, int iNode2, int iNode3, int * iCurrentPartials, int * iCurrentMatrices,
						const int nPatterns, const int nStates, const int nMatrices, const int nMatrixSize,
						const int nPartialsSizeGPU, const int nTotalPartialsSizeGPU,
						const int nStatesSizeGPU, const int nTotalStatesSizeGPU, int nTotalMatrixSizeGPU, double fScale);
void calcAllMatrixPPPGPU(int iNode1, int iNode2, int iNode3, int * iCurrentPartials, int * iCurrentMatrices,
						const int nPatterns, const int nStates, const int nMatrices, const int nMatrixSize,
						const int nPartialsSizeGPU, const int nTotalPartialsSizeGPU, int nTotalMatrixSizeGPU, double fScale);

void getNodePartialsGPU(int iNode, double * fPartials, int nPartialsSize, int nPartialsSizeGPU, int * iCurrentPartials, int nTotalPartialsSizeGPU);
void getPatternLogLikelihoodsGPU(double * fPatternLogLikelihoods, int nPatterns);

double integratePartialsGPU(const int iNode, const double * fProportions, const double * fFrequencies,
			const int nPatterns, const int nStates, const int nMatrices,
    		const int * iCurrentPartials,	const int nPartialsSizeGPU, const int nTotalPartialsSizeGPU);
