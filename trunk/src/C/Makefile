CUDA_INSTALL_PATH:=/usr/local/cuda
CUDA_SDK_PATH:=/home/remco/NVIDIA_CUDA_SDK
JAVA_HOME:=/opt/java
ACML_PATH:=/home/remco/acml/gfortran64

CPP:=g++
CC:=gcc
#CC:=nvcc 


CFLAGS  := $(DEBUGFLAGS) -g -I$(CUDA_INSTALL_PATH)/include -arch sm_13 -DDOUBLE_PRECISION 
CPPFLAGS:= $(DEBUGFLAGS) -O3 -Wall -ftree-vectorize -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(CUDA_INSTALL_PATH)/include -I$(ACML_PATH)/include -DDOUBLE_PRECISION
#LDFLAGS := -L$(CUDA_INSTALL_PATH)/lib -lpthread -lcublas -lcudart -lstdc++ -lm -lc
#LDFLAGS := -L$(CUDA_INSTALL_PATH)/lib -lstdc++ $(ACML_PATH)/lib/libacml_mv.a -lgfortran -lm -lc -lcudart
LDFLAGS := -lstdc++ -lm -lc
LOBJ    := BeerLikelihoodCoreNative.o BEER.o 
DEPS    := beast_evolution_likelihood_BeerLikelihoodCoreNative.h 


#		-Wl,-soname,libBEER.so 
all: $(LOBJ)
	$(CC) -O3 -shared  -lm \
		-o libBEER.so \
		$(LOBJ) \
		$(LDFLAGS) -L ../../lib 
	cp libBEER.so ../../lib/libBEER.so

jni:	
	javah -classpath ../../../beast2/build -jni beast.evolution.likelihood.BeerLikelihoodCoreNative
#	javah -classpath ../../bin -jni dr.evomodel.treelikelihood.BeerLikelihoodCoreCpp

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
		 
