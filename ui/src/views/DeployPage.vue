<template>
	<div id="DeployContainer">
		<!-- for steps see https://docs.marklogic.com/datahub/projects/deploy-to-cloud-services.html -->
		<h1>Deploy page</h1>
		<div>
			<v-overlay :value="overlay">
				<v-progress-circular indeterminate size="64"></v-progress-circular>
			</v-overlay>
			<ol>
				<li>
					<v-btn color="primary" class="right" v-on:click="delopyDeployGradle">Gradle Deploy to DHS</v-btn>
				</li>

				<li>
					<v-btn color="primary" class="right" v-on:click="deployDeployJava">Java Deploy to DHS</v-btn>
				</li>

				<li>
					<v-btn color="primary" class="right" v-on:click="deployRunIngests">Run Ingestion Steps</v-btn>
				</li>

				<li>
					<v-btn color="primary" class="right" v-on:click="dployRunFlows">Run Other Steps</v-btn>
				</li>
			</ol>
		</div>
	</div>
</template>

<script>
import OSApi from '@/api/OSApi.js';

export default {
	name: 'dhfDeploy',
	data() {
		return {
			dhsGradlePropertiesFile: '',
			dhsGradleProperties: '',
			dhsConfigured: false,
			dhsFlowName: '',
			dhsEntityName: '',
			dhFlows: '',
			dhEntites: '',
			overlay: false
		};
	},
	methods: {
		deployRunFlows: function() {
			this.overlay = true;
			OSApi.dhsRunFlows().then(response => {
				this.overlay = false;
			});
		},
		deployRunIngests: function() {
			this.overlay = true;
			OSApi.runIngestSteps().then(response => {
				this.overlay = false;
			});
		},
		deployDeployGradle: function(e) {
			console.log('In DHS deploy using gradle');
			this.overlay = true;
			OSApi.gradle(' dhsDeploy -PenvironmentName=dhs -i').then(response => {
				// replace dhsDeploy with hubDeploy in 5.2
				this.overlay = false;
			});
		},
		deployDeployJava: function(e) {
			console.log('In DHS deploy using java');
			this.overlay = true;
			OSApi.deployToDHS().then(response => {
				this.overlay = false;
			});
		}		
	}
};
</script>

<style scoped>
#DHSContainer {
	padding-left: 50px;
	padding-right: 50px;
	margin-top: 10px;
}
li {
	padding-top: 6px;
}
</style>
