<script>
import axios from 'axios';

const isHosted = process.env.VUE_APP_IS_HOSTED === 'true'
const isTesting = process.env.NODE_ENV === 'test'

export default {
	name:'ExportPage',
	data: ()=> ({
		exportMsg: '',
		exportError: '' ,
		datahub: '',
		entities: [],
		showExportStatus:false,
		requestStatus:"green",
		headers: [
			{
				text: 'Property',
				align: 'start',
				sortable: false,
				value: 'prop',
			},
			{ text: 'Setting', value: 'val' }
		],
		cloud: isHosted && !isTesting
	}),
	computed:{
		getRequestStatus(){return this.requestStatus}
	},
	methods: {
		getDataHubConfig() {
			return axios
			.get('/api/os/getDHprojectConfig/')
			.then(response => {
			console.log('Returning ' + response.data);
			this.datahub= response.data;
			this.parseEntities(response.data);
			return response.data;
			})
			.catch(error => {
				console.error('Error getting DHS config:', error);
				return error;
			});
		},
		async runExports(){
			this.exportMsg = "Exporting entities."
			this.exportError = ""
			this.requestStatus="green"
			this.showExportStatus = true;
			axios.post("/api/export/runExports/")
			.then(response => {
				this.exportMsg =response.statusText
				this.showExportStatus = true;
				console.log("runExports: " + response.data)
				return response.data
			})
			.catch(error => {
				console.error('error:', error);
				this.exportError = error
				this.requestStatus="red"
				this.showExportStatus = true;
				return error;
			});
		},
		async runExport(entityName){
			this.exportMsg = "Exporting " + entityName + "."
			this.exportError = ""
			this.requestStatus="green"
			this.showExportStatus = true;
			return axios.get("/api/os/runExport/", null, {params: {entityName}})
			.then(response => {
				this.exportMsg =response.statusText
				this.requestStatus="green"
				this.showExportStatus = true;
				return response.data
			})
			.catch(error => {
				console.error('error:', error);
				this.exportError = error
				this.requestStatus="red"
				this.showExportStatus = true;
				return error;
			});
		},
		parseEntities(hubConfig){
			//filter out the Entities item from hubConfig
			let ents  = hubConfig.filter(item => item.prop === "Entities");
			//have to parse this into an array- it's a string
			let entStr = JSON.parse(JSON.stringify(ents[0].val));
			//this regex pulls the entity names out of a serilaized string representation
			//and puts them in an array; a great way to come up with this regex is an
			//interactive regex pattern matcher like BBEdit
			// eslint-disable-next-line no-useless-escape
			this.entities = entStr.match(/[^,\s,\",\[][^\,]*[^,\s,]*[^,\",\]]/g);
		},
		handleDataHubTableClick(event){
			console.log(event);
		},
		handleEntityTableClick(entity){
			this.runExport(entity);
			console.log(entity);
		}
	},
	mounted() {
		this.getDataHubConfig();
	}
}

</script>

<template>
	<div id="exportContainer">
		<v-snackbar
			v-model="showExportStatus"
			right
			top
			:color="this.requestStatus"
		>
			{{ exportMsg + " " + exportError }}

			<template v-slot:action="{ attrs }">
				<v-btn
				color="this.requestStatus"
				text
				v-bind="attrs"
				@click="showExportStatus = false"
				>
				Close
				</v-btn>
			</template>
		</v-snackbar>
		<h1>Envision Export Page</h1>
		<fieldset class="col-sm-9" v-if="!cloud">
			<legend>Entities</legend>
				<v-simple-table dense>
				<tbody>
					<tr v-for="entity in entities" :key="entity" class='clickable-row' @click="handleEntityTableClick(entity)">
						<td >{{entity}}</td>
					</tr>
				</tbody>
			</v-simple-table>
			<v-btn color="primary" class="right" v-on:click="runExports" aria-label="Export entities.">Export All</v-btn>
		</fieldset>
		<fieldset class="col-sm-9" v-if="!cloud">
			<legend>Data Hub properties</legend>
			<v-simple-table dense>
				<tbody>
					<tr v-for="dhprop in datahub" :key="dhprop.prop" class='clickable-row' @click="handleDataHubTableClick(dhprop)">
						<td >{{dhprop.prop}}</td>
						<td >{{dhprop.val}}</td>
					</tr>
				</tbody>
			</v-simple-table>
			</fieldset>
	</div>
</template>

<style scoped>
	#exportContainer {
		padding-left: 50px;
		padding-right: 50px;
		margin-top: 10px;
	}
	.right {
		float: right;
		margin-left:10px;
	}
	h1 {
		padding-bottom: 30px;
	}
	.adminItem {
		padding: 10px;
		border : 1px solid black;
		border-radius: 5px;
		margin-bottom: 10px;
	}
	fieldset {
		padding: 10px;
		border : 1px solid black;
		border-radius: 5px;
		margin-bottom: 10px;
	}
	fieldset legend {
		font-weight: bold;
	}
	.success, .error{
		border-radius: 3px;
		padding-left: 5px;
	}
	.code {
		padding-left: 20px;
		font-size: 1.1em;
		color: darkred;
	}
	.clickable-row {
		cursor: pointer;
	}
</style>
