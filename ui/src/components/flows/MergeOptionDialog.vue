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
						v-model="entityPropertyPath"
						data-cy="mergeOptionDlg.propertyNameField"
						:disabled="isEditing"
						required
						:error-messages="inputErrors('entityPropertyPath', 'Property to Merge')"
						@input="$v.entityPropertyPath.$touch()"
						@blur="$v.entityPropertyPath.$touch()"
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
			entityPropertyPath: '',
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
		entityPropertyPath: { required },
		strategy: { required }
  },
	mounted() {
	},
	methods: {
		updateValues() {
			this.$v.$reset()
			const option = this.option
			if (!option) {
				this.entityPropertyPath = ''
				this.strategy = ''
				return
			}

			this.entityPropertyPath = option.entityPropertyPath
			this.strategy = option.mergeStrategyName
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
			const raw = this.option ? this.option.raw : {}
			const option = {
				...(raw || {}),
				entityPropertyPath: this.entityPropertyPath,
				mergeType: this.strategy ? 'strategy' : 'property-specific',
				mergeStrategyName: this.strategy
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
