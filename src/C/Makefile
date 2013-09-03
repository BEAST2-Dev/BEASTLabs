CUDA_INSTALL_PATH:=/usr/local/cuda
CUDA_SDK_PATH:=/home/remco//NVIDIA_GPU_Computing_SDK
JAVA_HOME:=/opt/java
ACML_PATH:=/home/remco/acml/gfortran64

CPP:=g++
CC:=gcc
#CC:=nvcc 
NVCC:=$(CUDA_INSTALL_PATH)/bin/nvcc

CFLAGS  := $(DEBUGFLAGS) -I$(CUDA_INSTALL_PATH)/include -I$(CUDA_SDK_PATH)/C/common/inc -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -arch sm_13 -DDOUBLE_PRECISION 
CPPFLAGS:= $(DEBUGFLAGS) -O3 -Wall -ftree-vectorize -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(CUDA_INSTALL_PATH)/include -I$(ACML_PATH)/include -DDOUBLE_PRECISION
#LDFLAGS := -L$(CUDA_INSTALL_PATH)/lib -lstdc++ $(ACML_PATH)/lib/libacml_mv.a -lgfortran -lm -lc -lcudart
LDFLAGS := -lstdc++ -lm -lc
LOBJ    := BeerLikelihoodCoreSimpleNative.o BeerLikelihoodCoreNative.o ThreadedLikelihoodCoreNative.o BEER.o BEERSimple.o ThreadedBEER.o
DEPS    := beast_evolution_likelihood_BeerLikelihoodCoreNative.h beast_evolution_likelihood_BeerLikelihoodCoreSimpleNative.h 

GPULDFLAGS := -L$(CUDA_INSTALL_PATH)/lib64 -L$(CUDA_INSTALL_PATH)/lib -lpthread -lcublas -lcudart -lstdc++ -lm -lc
GPULOBJ    := BeerLikelihoodCoreSimpleGPU.o BEERSimpleGPU.o
GPIDEPS    := beast_evolution_likelihood_BeerLikelihoodCoreSimpleGPU.h



native: $(LOBJ) $(GPULOBJ)
	$(CC) -O3 -shared -lpthread -lm \
		-o libBEER.so \
		$(LOBJ) \
		$(LDFLAGS) -L ../../lib 
	cp libBEER.so ../../lib/libBEER.so
	$(CC) -O3 -shared  -lm \
		-o libBEERGPU.so -lstdc++ -lm -lc\
		ThreadedLikelihoodCoreNative.o ThreadedBEER.o \
		-L ../../lib 
	cp libBEERGPU.so ../../lib/libBEERGPU.so

#		-Wl,-soname,libBEER.so 
all: $(LOBJ) $(GPULOBJ)
	$(CC) -O3 -shared -lpthread -lm \
		-o libBEER.so \
		$(LOBJ) \
		$(LDFLAGS) -L ../../lib 
	cp libBEER.so ../../lib/libBEER.so
	$(NVCC) -Xcompiler -fPIC $(CFLAGS) -O3 --use_fast_math -c -o BEERGPU.o BEERGPU.cu
	$(NVCC) -O3 -shared  -lm \
		-o libBEERGPU.so\
		 BEERGPU.o \
		$(GPULOBJ) \
		$(GPULDFLAGS) -L ../../lib 
	cp libBEERGPU.so ../../lib/libBEERGPU.so

jni:	
	javah -classpath ../../../beastii/build -jni beast.evolution.likelihood.ThreadedLikelihoodCoreNative
	javah -classpath ../../../beastii/build -jni beast.evolution.likelihood.BeerLikelihoodCoreNative
	javah -classpath ../../../beastii/build -jni beast.evolution.likelihood.BeerLikelihoodCoreSimpleNative
	javah -classpath ../../../beastii/build -jni beast.evolution.likelihood.BeerLikelihoodCoreSimpleGPU

%.o: %.cpp
	$(CPP) -fPIC -c -o $@ $< $(CPPFLAGS)
 
clean:
	rm -f $(LOBJ) ../../lib/*.o ../../lib/libBEER.so *.o libBEER.so *.linkinfo *.sibling *.sibling.vcg harness a.out beertest

%.o: %.cu
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	nvcc --compiler-options -fPIC \
		-Xcompiler $(CFLAGS) \
		-deviceemu \
		-c -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


test: beertest.o BEER.o
	gcc -o beertest \
		 BEER.o \
		 beertest.o \
		 -lcudart -L$(CUDA_INSTALL_PATH)/lib

#		BEER.cpp \
		 
