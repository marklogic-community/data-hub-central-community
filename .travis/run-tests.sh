#!/bin/bash

if [ "${TRAVIS_SECURE_ENV_VARS}" = "true" ] ; then
	project_dir=$(pwd)
	tmp_dir=$(mktemp -d -t ci-XXXXXXXXXX)
	echo "Temp Dir: ${tmp_dir}"
	cd $tmp_dir

	mkdir conceptConnectorModels

	echo "plugins {
		id 'net.saliman.properties' version '1.4.6'
		id 'com.marklogic.ml-data-hub' version '5.2.0'
	}" > build.gradle

	gradle hubInit

	echo "mlUsername=admin
mlPassword=admin
" > gradle-local.properties

	mkdir -p src/main/entity-config/databases/
	gradle mlDeploy -i
	gradle mlLoadModules -i

	cd $project_dir
  echo "./gradlew -DdhfDir=${tmp_dir} -DmodelsDir=${tmp_dir}/conceptConnectorModels -DdhfEnv=local clean test -i"
  ./gradlew -DdhfDir=${tmp_dir} -DmodelsDir=${tmp_dir}/conceptConnectorModels -DdhfEnv=local clean test -i
fi
