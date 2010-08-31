#include "beast_evolution_likelihood_BeerLikelihoodCoreNative.h"
#include "config.h"
#include "BEER.h"
#include <stdio.h>

#define PRINT(X) fprintf(stderr,"init X\n");
#define PRINT2(X) fprintf(stderr,"exit X\n");

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    createCppBEERObject
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_createCppBEERObject
  (JNIEnv * env, jobject obj, jint nStateCount) {
	jlong p = 0;

	if (nStateCount == 4) {
		BEER4 * pBEER4 = new BEER4();
		p = reinterpret_cast<jlong>(pBEER4);
	} else {
		BEER * pBEER = new BEER(nStateCount);
		p = reinterpret_cast<jlong>(pBEER);
	}
	return p;
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    initializeC
 * Signature: (JIIIZ)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_initializeC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint nNodeCount, jint nPatternCount, jint nMatrixCount, jboolean bIntegrateCategories) {
PRINT(init);
	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
	pBEER->initialize(nNodeCount, nPatternCount, nMatrixCount, bIntegrateCategories);
PRINT2(init);
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    finalizeC
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_finalizeC
  (JNIEnv * env, jobject obj, jlong jpBEER) {
    BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->finalize();
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    createNodePartialsC
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_createNodePartialsC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode) {
    BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->createNodePartials(iNode);
}


/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    setNodePartialsForUpdateC
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_setNodePartialsForUpdateC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode) {
    BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->setNodePartialsForUpdate(iNode);
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    setNodeStatesForUpdateC
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_setNodeStatesForUpdateC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode) {
    BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->setNodeStatesForUpdate(iNode);
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    setNodeStatesC
 * Signature: (JI[I)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_setNodeStatesC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode, jintArray jiNodeStates) {

	jint * iNodeStates = GET_JINT_ARRY(jiNodeStates, NULL);

	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->setNodeStates(iNode, iNodeStates);

    RELEASE_JINT_ARRAY(jiNodeStates, iNodeStates, JNI_ABORT);
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    setNodeMatrixForUpdateC
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_setNodeMatrixForUpdateC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode) {
    BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->setNodeMatrixForUpdate(iNode);
}


/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    setNodeMatrixC
 * Signature: (JII[D)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_setNodeMatrixC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode, jint iMatrixIndex, jdoubleArray jfMatrix) {
	jdouble * fMatrix = GET_JDOUBLE_ARRY(jfMatrix, NULL);

	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->setNodeMatrix(iNode, iMatrixIndex, fMatrix);

    RELEASE_JDOUBLE_ARRAY(jfMatrix, fMatrix, JNI_ABORT);
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    setPaddedMatricesC
 * Signature: (JI[D)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_setPaddedMatricesC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode, jdoubleArray jfMatrix) {
	jdouble * fMatrix = GET_JDOUBLE_ARRY(jfMatrix, NULL);

	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->setPaddedMatrices(iNode, fMatrix);

    RELEASE_JDOUBLE_ARRAY(jfMatrix, fMatrix, JNI_ABORT);
}


/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    setUseScalingC
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_setUseScalingC
  (JNIEnv * env, jobject obj, jlong jpBEER, jdouble fScale) {
	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->setUseScaling(fScale);

}


/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    calculatePartialsC
 * Signature: (JIII)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_calculatePartialsC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode1, jint iNode2, jint iNode3) {
	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->calculatePartials(iNode1, iNode2, iNode3);
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    integratePartialsC
 * Signature: (JI[D[D)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_integratePartialsC
  (JNIEnv * env, jobject obj, jlong jpBEER, jint iNode, jdoubleArray jfProportions, jdoubleArray jfOutPartials) {
    jdouble * fProportions = GET_JDOUBLE_ARRY(jfProportions, NULL);
    jdouble * fOutPartials = GET_JDOUBLE_ARRY(jfOutPartials, NULL);

    BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->integratePartials(iNode, fProportions, fOutPartials);

	RELEASE_JDOUBLE_ARRAY(jfProportions, fProportions, JNI_ABORT);
	RELEASE_JDOUBLE_ARRAY(jfOutPartials, fOutPartials, JNI_ABORT);
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    calculateLogLikelihoodsC
 * Signature: (J[D[D[D)V
 */
//JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_calculateLogLikelihoodsC
//  (JNIEnv * env, jobject obj, jlong jpBEER, jdoubleArray, jdoubleArray, jdoubleArray);

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    storeC
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_storeC
  (JNIEnv * env, jobject obj, jlong jpBEER) {
	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->store();
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    unstoreC
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_unstoreC
  (JNIEnv * env, jobject obj, jlong jpBEER) {
	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->unstore();
}

/*
 * Class:     beast_evolution_likelihood_BeerLikelihoodCoreNative
 * Method:    restoreC
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_beast_evolution_likelihood_BeerLikelihoodCoreNative_restoreC
  (JNIEnv * env, jobject obj, jlong jpBEER) {
	BEER * pBEER = reinterpret_cast<BEER *>(jpBEER);
    pBEER->restore();
}

