#/bin/sh
export H=/home/rrb/workspace/beastii
java -cp $H/build:$H/lib/commons-math-2.0.jar beast.app.BeastMCMC $*
#time java -agentlib:hprof=cpu=samples -cp build/:lib/commons-math-2.0.jar beast.app.BeastMCMC $*

