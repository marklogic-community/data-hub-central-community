<template>
	<v-dialog
		v-model="dialog"
		width="650"
		persistent
		@keydown.esc="cancel">
		<v-form @submit.prevent="save">
			<v-card>
				<v-card-title>Name Your Data Source
					<v-spacer/>
					<v-icon @click="cancel">close</v-icon>
				</v-card-title>
				<v-card-text>
					<p>Give your data source a name so you can refer to it easily later.</p>
					<v-text-field
						autofocus
						required
						:error-messages="inputErrors('collection', 'Collection')"
						@input="$v.collection.$touch()"
						@blur="$v.collection.$touch()"
						v-model="collection"
						data-cy="uploadCollection.collectionName"
					></v-text-field>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text color="secondary" @click="cancel" data-cy="uploadCollectionDlg.cancelBtn">Cancel</v-btn>
					<v-btn type="submit" text color="primary" data-cy="uploadCollectionDlg.saveBtn">Save</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import { required } from 'vuelidate/lib/validators'

function noSpaces(value) {
	return value.match(/^[^\s]+$/) !== null
}

export default {
	props: {
		showDialog: { type: Boolean }
	},
	data() {
		return {
			collection: null,
			dialog: false,
			resolve: null,
			reject: null
		}
	},
	validations: {
		collection: { required, noSpaces },
  },
	methods: {
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('required') && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('noSpaces') && !this.$v[field].noSpaces && errors.push(`${fieldName} cannot contain spaces. Only letters, numbers, and underscore.`)
			return errors
		},
		open(collection) {
			this.$v.$reset()
			this.collection = collection || null
			this.dialog = true
			return new Promise((resolve, reject) => {
				this.resolve = resolve
				this.reject = reject
			})
		},
		save() {
			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			this.resolve(this.collection)
			this.dialog = false
		},
		cancel() {
			this.reject()
			this.dialog = false
		}
	}
}
</script>

<style lang="less" scoped>
/deep/ .v-dialog {
	display: flex;
	flex-direction: column;
	overflow: hidden;

	.v-card {
		display: flex;
		height: 100%;
		flex-direction: column;
		flex: 1;
		overflow: hidden;
	}
	.v-card__text {
		overflow-y: auto;
		height: 100%;
	}
}
</style>
