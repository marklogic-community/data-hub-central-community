<template>
	<v-container>
		<v-layout row>
			<v-flex md12 class="text-center">
				<h1>Load From a Relational Database</h1>
			</v-flex>
		</v-layout>
	<v-alert type="error" v-show="hasR2MError" v-cloak>Error loading data!</v-alert>
	<vue-form-generator tag="div" :schema="schema" :model="loadModel" :options="formOptions"></vue-form-generator>
	<v-row justify="center">
			<v-col cols="6">
				<file-upload @upload="preJoinChoose" :dropZoneLabel="'Drop a JSON file containing your source database pre-join specification here'">
				</file-upload>
				<div>Pre-join configuration: <span v-bind:class="preJoinConfigStyle" >{{preJoinConfig? preJoinConfig.name:'please choose a pre-join specification'}}</span></div>
			</v-col>
	</v-row>
	<v-row justify="center">
			<v-col cols="6">
				<file-upload @upload="insertConfigChoose" :dropZoneLabel="'Drop a JSON file containing your Marklogic database insert specification here'">
				</file-upload>
				<div>Insert into Marklogic configuration: <span v-bind:class="insertConfigStyle" >{{insertConfig? insertConfig.name :'please choose a Marklogic database insert specification'}}</span></div>
			</v-col>
	</v-row>
	<v-row justify="center">
			<v-col cols="6">
				<v-btn @click="loadFromRDBMS" color="primary" :disabled='isDisabled'>
				Load
				</v-btn>
		</v-col>
	</v-row>
	</v-container>
</template>

<script>
import { required } from 'vuelidate/lib/validators'
import FileUpload from '@/components/FileUpload'
//scoped css unexpectedly has no effect on vue-form-generator
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
			insertConfig:null,
			preJoinConfig:null,
			hasR2MError:false,
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
			this.hasR2MError = false
			const connection = this.loadModel //without this callbacks lose track of context
			// eslint-disable-next-line no-unused-vars
			var myPromise = this.readMultiFiles([this.insertConfig,this.preJoinConfig]).then(results=> {
				// all results in the results array here
				//it's more trouble to guarantee a sequence than sniff the results
				//TODO make this one object, or move from file-oriented settings
				if(results.length === 2){
					//let merged = Object.assign(...results);
					const query = results[0].hasOwnProperty("query") ? results[0] : (results[1].hasOwnProperty("query") ? results[1] : null )
					const insert = results[0].hasOwnProperty("entityName") ? results[0] : (results[1].hasOwnProperty("entityName") ? results[1] : null )
					R2MConnectAPI.r2m(connection, query, insert)
				}else{
					//report error
					this.hasR2MError = true
				}
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
	},
	computed: {
		isDisabled: function(){
			return !this.insertConfig  ||
			!this.preJoinConfig ||
			!this.loadModel.connectionString ||
			!this.loadModel.username ||
			!this.loadModel.password ||
			!this.loadModel.numThreads ||
			!this.loadModel.batchSize
    },
		insertConfigStyle: function () {
    return {
			'config-provided': this.insertConfig,
			'text-danger': !this.insertConfig
			}
		},
		preJoinConfigStyle: function () {
    return {
			'config-provided': this.preJoinConfig,
			'text-danger': !this.preJoinConfig
			}
		},
	}
}
</script>

<style lang="less">

.text-danger{
	color: gray;
	font-style: italic;
}
.config-provided{
	color: green;
	font-style: normal;
}
.vue-form-generator > div{
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    flex-grow: 1;
  }

  .group-one-class .form-group{
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding: 0 2%;
    width: 50%;
  }

  .group-one-class .field-wrap, .wrapper{
    width: 100%;
  }

  .group-one-class .dropList{
    z-index: 10;
    background-color: #FFF;
    position: relative;
    width: 40%;
    top: 5px;
    right: 12px;
  }

  .group-one-class legend{
    margin: 10px 0 20px 18px;
    font-size: 16px;
    font-weight: bold;
    text-align: left;
  }

  .group-one-class .hint{
    font-size: 10px;
    font-style: italic;
    color: purple;
  }

  .group-one-class .help-block{
    color: red;
  }
</style>
