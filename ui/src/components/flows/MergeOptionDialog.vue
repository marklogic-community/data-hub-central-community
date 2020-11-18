<template>
	<v-dialog
		v-model="open"
		width="500"
		persistent
		@keydown.esc="close">
		<v-form @submit.prevent="save">
			<v-card>
				<v-card-title>{{dialogTitle}}<v-spacer></v-spacer><v-icon @click="close">close</v-icon></v-card-title>
				<v-card-text>
					<v-select
						:items="properties"
						label="Property to Merge"
						v-model="propertyName"
						data-cy="mergeOptionDlg.propertyNameField"
						:disabled="isEditing"
						required
						:error-messages="inputErrors('propertyName', 'Property to Merge')"
						@input="$v.propertyName.$touch()"
						@blur="$v.propertyName.$touch()"
					></v-select>

					<v-select
						:items="strategies"
						label="Strategy"
						v-model="strategy"
						data-cy="mergeOptionDlg.strategyField"
						:disabled="isEditing"
						no-data-text="Create a non-default Strategy first."
						required
						:error-messages="inputErrors('strategy', 'Strategy')"
						@input="$v.strategy.$touch()"
						@blur="$v.strategy.$touch()"
					></v-select>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text color="secondary" @click="close" data-cy="mergeOptionDlg.cancelBtn">Cancel</v-btn>
					<v-btn type="submit" text color="primary" data-cy="mergeOptionDlg.saveBtn">Save</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import { mapActions } from 'vuex'
import { required } from 'vuelidate/lib/validators'

export default {
	props: {
		option: { type: Object },
		properties: { type: Array },
		strategies: { type: Array },
		showDialog: { type: Boolean },
		isEditing: { type: Boolean, default: false },
	},
	data() {
		return {
			advancedState: null,
			isOpen: null,
			propertyName: '',
			strategy: '',
		}
	},
	computed: {
		dialogTitle() {
			return this.isEditing ? 'Edit Merge Option' : 'Add Merge Option'
		},
		open: {
			get() {
				return this.isOpen || this.showDialog
			},
			set(val) {
				this.isOpen = val
			}
		}
	},
	validations: {
		propertyName: { required },
		strategy: { required }
  },
	mounted() {
	},
	methods: {
		...mapActions({
			getFlow: 'flows/getFlow'
		}),
		updateValues() {
			this.$v.$reset()
			const option = this.option
			if (!option) {
				this.propertyName = ''
				this.strategy = ''
				return
			}

			this.propertyName = option.propertyName
			this.strategy = option.strategy
		},
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			this.$v[field].$invalid && !this.$v[field].integer && errors.push(`${fieldName} must be an integer.`)
			return errors
		},
		save() {
			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			const option = {
				...(this.option || {}),
				propertyName: this.propertyName,
				algorithmRef: 'standard',
				length: {
					weight: null
				},
				sourceWeights: [],
				strategy: this.strategy
			}
			this.$emit('save', option)
			this.close()
		},
		close() {
			this.open = false
			this.$emit('closed')
		},
	},
	watch: {
		option: 'updateValues',
		showDialog: 'updateValues'
	}
}
</script>
