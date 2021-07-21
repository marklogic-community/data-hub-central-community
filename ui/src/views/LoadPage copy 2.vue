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
	computed: {
		uploading() {
			return this.percentComplete !== null && this.percentComplete < 100
		},
		tableData() {
			return (this.stagingData.length > 0) ? this.stagingData : this.sampleData
		},
		allCollections() {
			return (this.stagingData.length > 0) ? this.stagingData.map(d => d.collection) : []
		}
	},
	data() {
		return {
			loadModel: {
				srcConnection:'',
				srcUser:'',
				srcPassword:''
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
		loadFromRDBMS(evt){
			this.readMultiFiles([this.insertConfig,this.preJoinConfig]).then(results=> {
				// all results in the results array here
				return  R2MConnectAPI.r2m(this.loadModel, results[0], results[1])
			});
		},
		loadFromRDBMS2(evt){
			try{
				this.readPreJoinConfig().then(result=> {
					console.log(result)
			}).then(result=> {
				//try{
				//const insertConfigData = await this.readConfig(this.insertConfig)
				//const preJoinConfigData = await this.readPreJoinConfig()
					return  R2MConnectAPI.r2m(this.loadModel, result, result)
				})
			}catch (ex) {
				console.log(ex);
      }
		},
		preJoinChoose(files) {
			this.preJoinConfig = files.length === 1 ? files[0] : null
		},
		insertConfigChoose(files) {
			this.insertConfig = files.length === 1 ? files[0] : null
		},
		async readConfig(file) {
			return new Promise((resolve, reject) => {
					const reader = new FileReader();
					reader.onload = async (e) => {
						try {
							console.log('Loaded ' + file.name);
							resolve(e.target.result)
						} catch (err) {
							reject(err);
						}
					};
					reader.onerror = (error) => {
						reject(error);
					};
					reader.readAsDataURL(file);
				})
		},
		readConfig2(file) {
			const filePromise = (file) => {
				return new Promise((resolve, reject) => {
					const reader = new FileReader();
					reader.onload = async (e) => {
						try {
							console.log('Loaded ' + file.name);
							resolve(e.target.result)
						} catch (err) {
							reject(err);
						}
					};
					reader.onerror = (error) => {
						reject(error);
					};
					reader.readAsDataURL(file);
				}
			)};
			//const fileData = await Promise.all([filePromise]);
			//console.log('Loaded ' + file.name);
			//return fileData;
			return filePromise;
		},
		async readPreJoinConfig() {
			const file = this.preJoinConfig
			const filePromise = (file) => {
				return new Promise((resolve, reject) => {
					const reader = new FileReader();
					reader.onload = async (e) => {
						try {
							console.log('Loaded ' + file.name);
							resolve(e.target.result)
						} catch (err) {
							reject(err);
						}
					};
					reader.onerror = (error) => {
						reject(error);
					};
					reader.readAsDataURL(file);
				}
			)};
			this.preJoinConfigData = Promise.all([filePromise]);
			console.log('Loaded ' + this.preJoinConfig.name);
			return this.preJoinConfigData;
		},
		//https://stackoverflow.com/questions/41906697/how-to-determine-that-all-the-files-have-been-read-and-resolve-a-promise
		readMultiFiles(files) {
			var results = [];
			files.reduce((p, file) => {
				return p.then(() => {
					return this.readConfig(file).then(function(data) {
					// put this result into the results array
					results.push(data);
					});
				});
			}, Promise.resolve()).then(function() {
        // make final resolved value be the results array
        return results;
			});
		}
	}
}
</script>

<style scoped>
.vue-form-generator{
  color: #df23d5;
}

</style>

  function newFunction(file) {
    console.log("Reading file: "+file.name)
  }
