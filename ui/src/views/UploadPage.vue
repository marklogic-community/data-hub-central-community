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
	</v-container>
</template>

<script>
import { required } from 'vuelidate/lib/validators'
import uploadApi from '@/api/UploadApi'
import FileUpload from '@/components/FileUpload'

export default {
	name: 'UploadPage',
	components: {
		FileUpload
	},
	props: ['type'],
	computed: {
		uploading() {
			return this.percentComplete !== null && this.percentComplete < 100
		}
	},
	data() {
		return {
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
			for (let i = 0; i < files.length; i++) {
				const file = files[i]
				this.uploadLabel = `Uploading ${file.name}`
				this.percentComplete = 0
				uploadApi.upload(file, (progressEvent) => {
					this.percentComplete = Math.round((progressEvent.loaded * 100) / progressEvent.total)
					if (this.percentComplete >= 100) {
						this.percentComplete = null
					}
				})
			}
		},
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$params.required && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			return errors
		}
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
</style>
