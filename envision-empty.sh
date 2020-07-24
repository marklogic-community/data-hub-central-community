#!/bin/bash
# Environment variable set as first argument when running script
MLENV=$1
#java -DdhfEnv=$MLENV -jar envision-1.0.3.jar

#./gradlew -DdhfDir=/Users/frubino/projects/hr-bare-bones/datahub -DmodelsDir=/Users/frubino/projects/hr-bare-bones/conceptConnectorModels -DdhfEnv=$MLENV bootrun
#--debug-jvm
#java -jar ./middle-tier/build/libs/envision-1.0.3.jar --server.port=9004 --dhfDir=/Users/frubino/projects/empty-data-hub/datahub --modelsDir=/Users/frubino/projects/empty-data-hub/conceptConnectorModels
#gradle -Dserver.port=9004 -DdhfDir=/Users/frubino/projects/empty-data-hub/datahub -DmodelsDir=/Users/frubino/projects/empty-data-hub/conceptConnectorModels -DdhfEnv=$MLENV bootRun
gradle -Dserver.port=9004 -DdhfDir=/Users/frubino/projects/empty-data-hub/datahub -DmodelsDir=/Users/frubino/projects/empty-data-hub/conceptConnectorModels -DdhfEnv=$MLENV bootRun