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
				<v-btn @click="loadFromRDBMS('insertConfigChoose')" color="primary">
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
		loadFromRDBMS() {
			var preJoinConfig;
			this.readAsBinaryString(this.preJoinConfig).then (result=>{preJoinConfig = result});
			var insertConfig;
			this.readAsBinaryString(this.insertConfig).then (result=>{insertConfig = result});
			R2MConnectAPI.r2m(this.loadModel, preJoinConfig, insertConfig)
		},
		preJoinChoose(files) {
			this.preJoinConfig = files.length === 1 ? files[0] : null
		},
		insertConfigChoose(files) {
			this.insertConfig = files.length === 1 ? files[0] : null
		},
		readAsBinaryString : async(file)=> {
			let result = await new Promise((resolve) => {
			let fileReader = new FileReader();
			fileReader.onload = (e) => resolve(fileReader.result);
			fileReader.readAsBinaryString(file);
			});
			return result;
		}
	}
}
</script>

<style scoped>
.vue-form-generator{
  color: #df23d5;
}

</style>
