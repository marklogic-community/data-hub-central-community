<template>
	<v-container>
		<v-layout row>
			<v-flex md12 class="text-center">
				<h1>Load Data From RDBMS</h1>
			</v-flex>
		</v-layout>
	<vue-form-generator :schema="schema" :model="loadModel" :options="formOptions"></vue-form-generator>
	<v-row justify="center">
			<v-col cols="6">
				<v-btn @click="choosePJConfigFile('preJoinChoose')" color="primary">
				Pre-Join Config File
				</v-btn>
		</v-col>
	</v-row>
	<v-row justify="center">
			<v-col cols="6">
				<v-btn @click="choosePJConfigFile('insertConfigChoose')" color="primary">
				Insert Config File
				</v-btn>
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
        id: 1,
        name: 'John Doe',
        password: 'J0hnD03!x4',
        skills: ['Javascript', 'VueJS'],
        email: 'john.doe@gmail.com',
        status: true
      },
			schema : userFormSchema,
      schema2: {
        fields: [
          {
            type: 'input',
            inputType: 'text',
            label: 'ID (disabled text field)',
            model: 'id',
            readonly: true,
            disabled: true
          },
          {
            type: 'input',
            inputType: 'text',
            label: 'Name',
            model: 'name',
            placeholder: 'Your name',
            featured: true,
            required: true
          },
          {
            type: 'input',
            inputType: 'password',
            label: 'Password',
            model: 'password',
            min: 6,
            required: true,
            hint: 'Minimum 6 characters',
            validator: VueFormGenerator.validators.string
          },
          {
            type: 'select',
            label: 'Skills',
            model: 'skills',
            values: ['Javascript', 'VueJS', 'CSS3', 'HTML5']
          },
          {
            type: 'input',
            inputType: 'email',
            label: 'E-mail',
            model: 'email',
            placeholder: 'User\'s e-mail address'
          },
          {
            type: 'checkbox',
            label: 'Status',
            model: 'status',
            default: true
          }
        ]
      },
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
		loadFromRDBMS(){},
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
.vue-form-generator > div
{
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    flex-grow: 1;
  }

  .form-group{
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding: 0 2%;
    width: 50%;
  }

  .field-wrap, .wrapper{
    width: 100%;
  }

  .dropList{
    z-index: 10;
    background-color: #FFF;
    position: relative;
    width: 40%;
    top: 5px;
    right: 12px;
  }

  legend{
    margin: 10px 0 20px 18px;
    font-size: 16px;
    font-weight: bold;
    text-align: left;
		color: purple;
  }

  .hint{
    font-size: 10px;
    font-style: italic;
    color: purple;
  }

  .help-block{
    color: red;
  }
</style>
