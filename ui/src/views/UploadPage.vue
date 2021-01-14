<template>
	<v-container>
		<v-layout row>
			<v-flex md12 class="text-center">
				<h1>Upload Data</h1>
			</v-flex>
		</v-layout>
		<v-row justify="center">
			<v-col cols="6">
				<file-upload @upload="uploadFiles">
				</file-upload>
				<template v-if="uploading">
					<h3 class="text-center">{{uploadLabel}}...</h3>
					<v-progress-linear :value="percentComplete"></v-progress-linear>
				</template>
			</v-col>
		</v-row>
		<v-layout row>
			<v-flex md12 class="text-center">
				<h1>My Data</h1>
			</v-flex>
		</v-layout>
		<v-row justify="center">
			<v-col cols="6" class="data-container">
				<div class="text-center sample" v-if="(stagingData.length <= 0)">
					Uploaded data will appear here.
				</div>

				<div class="sample-text" v-if="(stagingData.length <= 0)">
					SAMPLE
				</div>
				<v-simple-table :class="(stagingData.length > 0) ? 'active': 'sample'">
					<thead>
						<tr>
							<th>Data Source</th>
							<th>Count</th>
							<th data-cy="manageSources.deleteAll">
								<delete-data-confirm
								tooltip="Delete All Data Sources"
								message="Do you really want to delete all Data Sources?"
								:disabled="(stagingData.length <= 0)"
								:collection="''"
								:deleteInProgress="deleteInProgress"
								@deleted="removeAllData($event)"/>
							</th>
						</tr>
					</thead>
					<tbody data-cy="manageSources.table">
						<tr v-for="data of tableData" :key="data.collection">
							<td>{{data.collection}}</td>
							<td>{{data.count}}</td>
							<td>
								<delete-data-confirm
									tooltip="Delete Data Source"
									message="Do you really want to delete this data source?"
									:disabled="(stagingData.length <= 0)"
									:collection="data.collection"
									:deleteInProgress="deleteInProgress"
									@deleted="removeData($event)"/>
							</td>
						</tr>
					</tbody>
				</v-simple-table>
			</v-col>
		</v-row>
		<upload-collection-dialog ref="uploadCollectionDlg" />
	</v-container>
</template>

<script>
import { required } from 'vuelidate/lib/validators'
import uploadApi from '@/api/UploadApi'
import flowsApi from '@/api/FlowsApi'
import FileUpload from '@/components/FileUpload'
import DeleteDataConfirm from '@/components/DeleteDataConfirm'
import axios from 'axios';
import UploadCollectionDialog from '../components/UploadCollectionDialog.vue'

export default {
	name: 'UploadPage',
	components: {
		FileUpload,
		DeleteDataConfirm,
		UploadCollectionDialog
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
		uploadFiles(files) {
			const collection = files.length === 1 ? files[0].name : null
			this.$refs.uploadCollectionDlg.open(collection).then(({collection, database}) => {
				if (collection) {
					this.percentComplete = 0
					uploadApi.upload({collection, database}, files, (progressEvent) => {
						this.percentComplete = Math.round((progressEvent.loaded * 100) / progressEvent.total)
						if (this.percentComplete >= 100) {
							this.percentComplete = null
						}
					})
				}
			})
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

<style lang="less" scoped>
.dropzone {
	margin-top: 2em;
	margin-bottom: 2em;
}

.alert-enter {
	opacity: 0;
}

.alert-leave-active {
	opacity: 0;
}

.alert-enter .alert-container,
.alert-leave-active .alert-container {
	-webkit-transform: scale(1.1);
	transform: scale(1.1);
}

div.sample {
	color: #555;
}

.theme--light.v-data-table.sample > .v-data-table__wrapper > table > tbody > tr:hover:not(.v-data-table__expanded__content):not(.v-data-table__empty-wrapper) {
	background: none;
}
.v-data-table.sample {
	td,th {
		color: #ccc !important;
	}
	/deep/ .v-btn--icon {
		color: #ccc;
	}
	color: #ccc;
}

.data-container {
	position: relative;
}

.sample-text {
	position: absolute;
	display: flex;
	flex: 1 1;
	vertical-align: middle;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	font-size: 60px;
	transform: rotate(-30deg);
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	text-align: center;
	color: #ccc;
	-webkit-touch-callout: none; /* iOS Safari */
	-webkit-user-select: none; /* Safari */
	-khtml-user-select: none; /* Konqueror HTML */
	-moz-user-select: none; /* Old versions of Firefox */
	-ms-user-select: none; /* Internet Explorer/Edge */
	user-select: none;
}
</style>
