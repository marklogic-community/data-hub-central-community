<script>
import axios from 'axios';

const isHosted = process.env.VUE_APP_IS_HOSTED === 'true'
const isTesting = process.env.NODE_ENV === 'test'

export default {
	name:'ExportPage',
	data: ()=> ({
		exportMsg: '',
		exportError: '' ,
		entityMsg: '',
		entityError: '',
		datahub: '',
		entities: '',
		showEntityStatus:false,
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

	methods: {
		getDataHubConfig() {
			return axios
			.get('/api/os/getDHprojectConfig/')
			.then(response => {
			console.log('Returning ' + response.data);
			this.datahub= response.data;
			this.entities= parseEntities(response.data);
			return response.data;
			})
			.catch(error => {
				console.error('Error getting DHS config:', error);
				return error;
			});
		},
		getEntityNames() {
			return axios
			.get('/api/os/getEntityNames/')
			.then(response => {
				console.log('Returning ' + response.data);
				this.entities=response.data;
				return response.data;
			})
			.catch(error => {
				console.error('Error getting flows:', error);
				return error;
			});
		},
		async runExports(){
			this.exportMsg = "Running exports."
			this.exportError = ""
			axios.post("/api/os/runExports/")
			.then(response => {
				this.entityMsg =response.statusText
				return response.data
			})
			.catch(error => {
				console.error('error:', error);
				this.exportError = error
				return error;
			});
		},
		async runExport(entityName){
			this.exportMsg = "Exporting " + entityName + "."
			this.exportError = ""
			this.showExportStatus = true;
			axios.post("/api/os/runExport/", null, {params: {entityName}})
			.then(response => {
				this.exportMsg =response.statusText
				this.showExportStatus = true;
				return response.data
			})
			.catch(error => {
				console.error('error:', error);
				this.exportError = error
				this.showExportStatus = true;
				return error;
			});
		},
		parseEntities(hubConfig){
			hubConfig.filter(item => item.prop === "Entities")
				.array.forEach(item => {
					this.entities = item.value
				});
		},
		handleDataHubTableClick(event){
			console.log(event);
		}},
	mounted() {
		this.getDataHubConfig();
		this.getEntityNames();
	}
}

</script>

<template>
	<div id="exportContainer">
		<v-snackbar
			v-model="showEntityStatus"
			right
			top
		>
			{{ entityMsg + " " + entityError }}

			<template v-slot:action="{ attrs }">
				<v-btn
				color="red"
				text
				v-bind="attrs"
				@click="snackbar = false"
				>
				Close
				</v-btn>
			</template>
		</v-snackbar>
		<h1>Envision Export Page</h1>
		<fieldset class="col-sm-9" v-if="!cloud">
			<legend>Etities</legend>
				<v-simple-table dense>
				<tbody>
					<tr v-for="entity in entities" :key="entity" class='clickable-row' @click="handleEntityTableClick(dhprop)">
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
