<template>
	<v-container>
		<v-layout row>
			<v-flex md12 class="text-center">
				<h1>Load From a Relational Database</h1>
			</v-flex>
		</v-layout>

	<vue-form-generator tag="div" :schema="schema" :model="loadModel" :options="formOptions"></vue-form-generator>
	<v-row justify="center">
			<v-col cols="6">
				<file-upload @upload="preJoinChoose" :dropZoneLabel="'Drop a JSON file containing your source database pre-join specification here'">
				</file-upload>
				<div>Pre-join configuration: {{preJoinConfig === {}?'':preJoinConfig.name}}</div>
			</v-col>
	</v-row>
	<v-row justify="center">
			<v-col cols="6">
				<file-upload @upload="insertConfigChoose" :dropZoneLabel="'Drop a JSON file containing your Marklogic database insert specification here'">
				</file-upload>
				<div>Insert into Marklogic configuration: {{insertConfig === {}?'':insertConfig.name}}</div>
			</v-col>
	</v-row>
	<v-row justify="center">
			<v-col cols="6">
				<v-btn @click="loadFromRDBMS" color="primary">
				Load
				</v-btn>
		</v-col>
	</v-row>
	</v-container>
</template>

<script>
import { required } from 'vuelidate/lib/validators'
import FileUpload from '@/components/FileUpload'
import VueFormGenerator from 'vue-form-generator'
import 'vue-form-generator/dist/vfg-core.css'  // optional full css additions
import 'vue-form-generator/dist/vfg.css'  // optional full css additions
import userFormSchema from '../forms/userFormSchema2'
import R2MConnectAPI from '@/api/R2MConnectApi'

export default {
	name: 'LoadPage',
	components: {
		FileUpload,
		"vue-form-generator": VueFormGenerator.component
	},
	props: ['type'],
	data() {
		return {
			loadModel: {
				connectionString:'jdbc:oracle:thin:@localhost:1527:OraDoc',
				username:'admin',
				password:'admin',
				numThreads: 5,
				batchSize: 10
			},
			insertConfig:{},
			preJoinConfig:{},
			schema : userFormSchema,
      formOptions: {
        validateAfterLoad: true,
        validateAfterChanged: true,
        validateAsync: true
      },
		}
	},
	validations: {
		dataSource: { required }
	},
	methods: {
		loadFromRDBMS(){
			const connection = this.loadModel //without this callbacks lose track of context
			// eslint-disable-next-line no-unused-vars
			var myPromise = this.readMultiFiles([this.insertConfig,this.preJoinConfig]).then(results=> {
				// all results in the results array here
				//it's more trouble to guarantee a sequence than sniff the results
				//TODO make this one object, or move from file-oriented settings
				let merged = Object.assign(...results);
				const query = results[0].hasOwnProperty("query") ? results[0] : results[1]
				const insert = results[0].hasOwnProperty("entityName") ? results[0] : results[1]
				return R2MConnectAPI.r2m(connection, query, insert)
			});
		},
		preJoinChoose(files) {
			this.preJoinConfig = files.length === 1 ? files[0] : null
		},
		insertConfigChoose(files) {
			this.insertConfig = files.length === 1 ? files[0] : null
		},
		//return FileReader Promise
		async readConfig(file) {
			return new Promise((resolve, reject) => {
					const reader = new FileReader();
					reader.onload = async (e) => {
						try {
							console.log('Loaded ' + file.name);
							resolve(JSON.parse(e.target.result))
						} catch (err) {
							reject(err);
						}
					};
					reader.onerror = (error) => {
						reject(error);
					};
					reader.readAsText(file);
				})
		},
		readMultiFiles(files) {
			return Promise.all(files.map(this.readConfig));
		},
		mounted() {
			this.$ws.subscribe('/topic/status', tick => {
				const msg = tick.body
				if (msg.percentComplete >= 100) {
					this.refreshInfo()
				}
			})
			this.refreshInfo()
		},
		refreshInfo() {

		}
	}
}
</script>

<style scoped>

.group-one-class legend {
  color: #824082;
  font-weight: bold;
  font-size: 1.6em;
  position: relative;
  display: block;
  width: 100%;
  float: left;
  padding-left: 15px;
  padding-right: 15px;
  margin-bottom: 1em;
  border-bottom: 1px solid #824082;
}
</style>
