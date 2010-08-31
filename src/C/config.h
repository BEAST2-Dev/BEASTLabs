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

