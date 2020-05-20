<template>
	<div id="DHSContainer">
		<!-- for steps see https://docs.marklogic.com/datahub/projects/deploy-to-cloud-services.html -->
		<h1>DHS page</h1>

		<p v-if="dhsConfigured">
			<a href="#" v-on:click.prevent="dhsConfigured = false">Change DHS config</a>
		</p>
		<div v-else>
			<p>To deploy DHS, please:</p>
			<ol>
				<li>
					Access the DHS instance (
					<a href="https://portal.stage.z.marklogicsvc.com/" target="_blank">Azure</a>,
					<a href="https://cloudservices.marklogic.com/login" target="_blank">AWS </a>
					)
				</li>
				<li>
					Copy the Gradle config to your clipboard and paste into the textbox
					below
				</li>
				<li>
					Edit the username and password (the last two lines) and specify an
					account that has both the data-hub-developer and
					data-hub-security-admin roles
				</li>
				<li>
					<v-col sm="9" md="9">
						<v-textarea
							name="gradleProps"
							label="Gradle Properties"
							value
							v-model="dhsGradleProperties"></v-textarea>
					</v-col>
					<v-btn variant="outline-primary" class="right" v-on:click="setProps">Debug set props</v-btn>
				</li>

				<li>
					<v-btn
						color="primary"
						class="right"
						v-on:click="cpGradlePropsToContainer">
						Copy file to container
					</v-btn>
				</li>
			</ol>
		</div>

		<div v-if="dhsConfigured">
			<v-overlay :value="overlay">
				<v-progress-circular indeterminate size="64"></v-progress-circular>
			</v-overlay>
			<ol>
				<li>
					<v-btn color="primary" class="right" v-on:click="dhsDeployGradle">Gradle Deploy to DHS</v-btn>

					<v-btn color="primary" class="right" v-on:click="dhsDeployJava">Java Deploy to DHS</v-btn>
				</li>

				<!-- https://docs.marklogic.com/datahub/flows/run-flow-using-gradle.html -->
				<li>
					<v-btn color="primary" class="right" v-on:click="dhsRunIngests">Run Ingestion Steps</v-btn>
				</li>

				<li>
					<v-btn color="primary" class="right" v-on:click="dhsRunFlows">Run Other Steps</v-btn>
				</li>
			</ol>
		</div>
	</div>
</template>

<script>
import OSApi from '@/api/OSApi.js';

export default {
	name: 'dhsDeploy',
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
	created() {
		// check to see if DHS has been setup
		let configured = OSApi.getDHprojectConfig().then(response => {
			this.dhsConfigured = response.dhsConfigFileExists;

			let strFlows = response.flows; // [Flow1, Flow2] yuk - review passing JSON back from middle tier
			strFlows = strFlows.substring(1);
			strFlows = strFlows.substring(0, strFlows.length - 1);
			this.dhFlows = strFlows.split(',');

			let strEntities = response.entities; // [Flow1, Flow2] yuk
			strEntities = strEntities.substring(1);
			strEntities = strEntities.substring(0, strEntities.length - 1);
			this.dhEntities = strEntities.split(',');
		});
	},
	methods: {
		dhsRunFlows: function() {
			this.overlay = true;
			OSApi.dhsRunFlows().then(response => {
				this.overlay = false;
			});
		},
		dhsRunIngests: function() {
			this.overlay = true;
			OSApi.runIngestSteps().then(response => {
				this.overlay = false;
			});
		},
		dhsDeployGradle: function(e) {
			console.log('In DHS deploy using gradle');
			this.overlay = true;
			OSApi.gradle(' dhsDeploy -PenvironmentName=dhs -i').then(response => {
				// replace dhsDeploy with hubDeploy in 5.2
				this.overlay = false;
			});
		},
		dhsDeployJava: function(e) {
			console.log('In DHS deploy using java');
			this.overlay = true;
			OSApi.deployToDHS().then(response => {
				this.overlay = false;
			});
		},
		cpGradlePropsToContainer: function() {
			console.log('In cpGradlePropsToContainer');

			OSApi.setGradleProperties(this.dhsGradleProperties);
			this.dhsConfigured = true;
		},
		setProps: function() {
			// for debuggubg only - saves me having to access DHS console all the time
			// this is all DHF 5.2 code from Azure
			this.dhsGradleProperties = `mlDHFVersion=5.2

mlHost=YyxYxxZO1.stage.z.marklogicsvc.com
mlIsHostLoadBalancer=true
mlIsProvisionedEnvironment=true
mlStagingAuth=basic
mlFinalAuth=basic
mlJobAuth=basic
mlAppServicesAuthentication=basic
mlJobSimpleSsl=true
mlFinalSimpleSsl=true
mlStagingSimpleSsl=true
mlAdminScheme=https
mlManageScheme=https
mlAppServicesSimpleSsl=true
mlManageSimpleSsl=true
sslFlag=true
mlAppServicesPort=8010

mlFlowOperatorRole=data-hub-operator
mlFlowDeveloperRole=data-hub-developer

mlUsername=[change me]
mlPassword=[change me]
`;

			// AWS
			this.dhsGradleProperties = `mlHost=12dgrma7e.vkunp87wvpv.a.marklogicsvc.com
mlIsHostLoadBalancer=true
mlIsProvisionedEnvironment=true
mlStagingAuth=basic
mlFinalAuth=basic
mlJobAuth=basic
mlAppServicesAuthentication=basic
mlJobSimpleSsl=true
mlFinalSimpleSsl=true
mlStagingSimpleSsl=true
mlAdminScheme=https
mlManageScheme=https
mlAppServicesSimpleSsl=true
mlManageSimpleSsl=true
sslFlag=true

mlStagingAppserverName=data-hub-STAGING
mlStagingPort=8010
mlStagingDbName=data-hub-STAGING
mlStagingForestsPerHost=1

mlAppServicesPort=8010

mlJobAppserverName=data-hub-JOBS
mlJobPort=8013
mlJobDbName=data-hub-JOBS
mlJobForestsPerHost=1

mlModulesDbName=data-hub-MODULES
mlModulePermissions=flowDeveloper,read,flowDeveloper,execute,flowDeveloper,insert,flowDeveloper,update,flowOperator,read,flowOperator,execute,flowOperator,insert,flowOperator,update
mlStagingTriggersDbName=data-hub-staging-TRIGGERS
mlStagingSchemasDbName=data-hub-staging-SCHEMAS

mlFinalTriggersDbName=data-hub-final-TRIGGERS
mlFinalSchemasDbName=data-hub-final-SCHEMAS

mlFinalAppserverName=data-hub-FINAL
mlFinalPort=8011
mlFinalDbName=data-hub-FINAL
mlFinalForestsPerHost=1

mlFlowOperatorRole=flowOperator
mlFlowDeveloperRole=flowDeveloper

mlUsername=dbaldryadmin
mlPassword=MarkLog1c$
mlManageUsername=dbaldryadmin
mlManagePassword=MarkLog1c$
mlFlowOperatorUserName=dbaldryadmin
mlFlowOperatorUserPassword=MarkLog1c$`;
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
