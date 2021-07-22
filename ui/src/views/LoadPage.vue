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
				<v-btn @click="loadFromRDBMS2" color="primary">
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
			insertConfigData:{},
			preJoinConfigData:{},
			schema : userFormSchema,
      formOptions: {
        validateAfterLoad: true,
        validateAfterChanged: true,
        validateAsync: true
      },
			deleteInProgress: false,
			dataSource: null,
			percentComplete: null,
			uploadLabel: null
		}
	},
	validations: {
		dataSource: { required }
	},
	methods: {
		loadFromRDBMS(){
			// eslint-disable-next-line no-unused-vars
			var myPromise = this.readMultiFiles2([this.insertConfig,this.preJoinConfig]).then(results=> {
				// all results in the results array here
				return  R2MConnectAPI.r2m(this.loadModel, results[0], results[1])
			});
		},
		//TODO refactor this code
		//this function creates and resolves promises to read config files in sequence
		//and supply the file contents to the r2m api
		loadFromRDBMS2(){
			var results = [];
			let filePromises = [this.insertConfig,this.preJoinConfig].reduce((p, file) => {
				return p.then(() => {
					new Promise((resolve, reject) => {
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
				}).then(data => {
					// put this result into the results array
					results.push(data);
					});
				}); //reduce creates a chain of promises
			}, Promise.resolve()); //first item in chain is a resolve
			const connection = this.loadModel //without this callbacks lose track of context
			// eslint-disable-next-line no-unused-vars
			let _ = filePromises.then(function() {
				let query = results[1];
				let insert = results[0]
        // make final resolved value be the results array
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
		//https://stackoverflow.com/questions/41906697/how-to-determine-that-all-the-files-have-been-read-and-resolve-a-promise
		readMultiFiles2(files) {
			var results = [];
			files.reduce(async (p, file) => {
				return p.then(() => {
					return this.readConfig(file).then(data => {
					// put this result into the results array
					results.push(data);
					});
				});
			}, Promise.resolve()).then(()=> {
        // make final resolved value be the results array
        return results;
			});
		},
		readMultiFiles(files) {
			return Promise.all(files.map(this.readConfig));
		}
	}
}
</script>

<style scoped>
.vue-form-generator{
  color: #df23d5;
}

</style>
