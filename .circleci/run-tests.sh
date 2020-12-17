#!/bin/bash

project_dir=$(pwd)
tmp_dir=$(mktemp -d -t ci-XXXXXXXXXX)
echo "Temp Dir: ${tmp_dir}"
cd $tmp_dir

mkdir conceptConnectorModels

echo "plugins {
	id 'net.saliman.properties' version '1.4.6'
	id 'com.marklogic.ml-data-hub' version '5.2.5'
}" > build.gradle

gradle hubInit

echo "mlUsername=admin
mlPassword=admin
" > gradle-local.properties

mkdir -p src/main/entity-config/databases/

echo "{
\"lang\" : \"zxx\",
\"path-namespace\" : [ {
	\"prefix\" : \"es\",
	\"namespace-uri\" : \"http://marklogic.com/entity-services\"
} ],
\"range-element-index\" : [
],
\"database-name\" : \"%%mlFinalDbName%%\"
}
"  > src/main/entity-config/databases/final-database.json
gradle mlDeploy -i
gradle mlLoadModules -i

cd $project_dir
echo "./gradlew -DdhfDir=${tmp_dir} -DmodelsDir=${tmp_dir}/conceptConnectorModels -DdhfEnv=local clean test -i"
./gradlew -DdhfDir=${tmp_dir} -DmodelsDir=${tmp_dir}/conceptConnectorModels -DdhfEnv=local clean test -i
