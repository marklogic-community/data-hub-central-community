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
				<div>Pre-join configuration: {{preJoinConfig === {}?'':preJoinConfig}}</div>
			</v-col>
	</v-row>
	<v-row justify="center">
			<v-col cols="6">
				<file-upload @upload="insertConfigChoose" :dropZoneLabel="'Drop a JSON file containing your Marklogic database insert specification here'">
				</file-upload>
				<div>Insert into Marklogic configuration: {{insertConfig === {}?'':insertConfig}}</div>
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
import uploadApi from '@/api/UploadApi'
import flowsApi from '@/api/FlowsApi'
import FileUpload from '@/components/FileUpload'
import DeleteDataConfirm from '@/components/DeleteDataConfirm'
import axios from 'axios'
import UploadCollectionDialog from '../components/UploadCollectionDialog.vue'
import VueFormGenerator from 'vue-form-generator'
import 'vue-form-generator/dist/vfg-core.css'  // optional full css additions
import 'vue-form-generator/dist/vfg.css'  // optional full css additions
import userFormSchema from '../forms/userFormSchema2'
import R2MConnectAPI from '@/api/R2MConnectApi'

export default {
	name: 'LoadPage',
	components: {
		FileUpload,
		DeleteDataConfirm,
		UploadCollectionDialog,
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
			sampleData: [
				{
					collection: 'MyDataSource.csv',
					count: 125
				},
				{
					collection: 'MyOtherDataSource.csv',
					count: 32
				}
			],
			stagingData: [],
			dataSource: null,
			percentComplete: null,
			uploadLabel: null
		}
	},
	validations: {
		dataSource: { required }
	},
	methods: {
		updateLoadDetails(){},
		loadFromRDBMS(){
			var sourceConfig = {name:"sourceConfig"}
			var mlConfig = {name:"mlConfig"}
			R2MConnectAPI.r2m(this.preJoinConfig, sourceConfig, this.insertConfig, mlConfig)
		},
		choosePJConfigFile(myEvent){
			this.chooseFileInput = document.createElement('input');
			this.chooseFileInput.id = 'envision-config-file-chooser'
			this.chooseFileInput.type = 'file'
			this.chooseFileInput.multiple = true
			this.chooseFileInput.addEventListener('change', () => {
				this.$emit(myEvent, this.chooseFileInput.files)
			})
			this.chooseFileInput.click()
		},
		preJoinChoose(files) {
			this.preJoinConfig = files.length === 1 ? files[0].name : null

		},
		insertConfigChoose(files) {
			this.insertConfig = files.length === 1 ? files[0].name : null
		},
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$params.required && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			return errors
		},
		refreshInfo() {
			flowsApi.getNewStepInfo().then(info => {
				this.stagingData = info.collections.staging
			})
		},
		async removeAllData() {
			this.deleteInProgress = true
			await axios.post("/api/system/deleteCollection", { database: 'staging', collections: this.allCollections })
			this.deleteInProgress = false
			this.stagingData = []
		},
		async removeData(collection) {
			this.deleteInProgress = true
			await axios.post("/api/system/deleteCollection", { database: 'staging', collections: [collection] })
			this.deleteInProgress = false
			this.stagingData = this.stagingData.filter(c => c.collection !== collection)
		}
	},
	mounted() {
		this.$ws.subscribe('/topic/status', tick => {
			const msg = tick.body
			if (msg.percentComplete >= 100) {
				this.refreshInfo()
			}
		})
		this.refreshInfo()
	}
}
</script>

<style scoped>
.vue-form-generator{
  color: #df23d5;
}

</style>
