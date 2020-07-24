#!/bin/bash
# Environment variable set as first argument when running script
MLENV=$1
#java -DdhfEnv=$MLENV -jar envision-1.0.3.jar

#./gradlew -DdhfDir=/Users/frubino/projects/hr-bare-bones/datahub -DmodelsDir=/Users/frubino/projects/hr-bare-bones/conceptConnectorModels -DdhfEnv=$MLENV bootrun
#--debug-jvm
./gradlew -DdhfDir=/Users/frubino/projects/contact-tracing/datahub -DmodelsDir=/Users/frubino/projects/contact-tracing/datahub/conceptConnectorModels -DdhfEnv=$MLENV bootrun
