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
					<v-text-field
						required
						label="Name"
						v-model="label"
						data-cy="matchThresholdDlg.nameField"
						:error-messages="inputErrors('label', 'Name')"
						@input="$v.label.$touch()"
						@blur="$v.label.$touch()"
					></v-text-field>

					<v-text-field
						required
						label="Weight Threshold"
						v-model="above"
						data-cy="matchThresholdDlg.aboveField"
						:error-messages="inputErrors('above', 'Weight Threshold')"
						@input="$v.above.$touch()"
						@blur="$v.above.$touch()"
					></v-text-field>

					<v-select
						:items="actions"
						label="Action"
						item-text="name"
						item-value="value"
						v-model="action"
						data-cy="matchThresholdDlg.actionField"
						required
						:error-messages="inputErrors('action', 'Action')"
						@input="$v.action.$touch()"
						@blur="$v.action.$touch()"
					></v-select>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn data-cy="matchThresholdDlg.cancelBtn" text color="secondary" @click="close">Cancel</v-btn>
					<v-btn data-cy="matchThresholdDlg.saveBtn" type="submit" text color="primary">Save</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import { required, integer } from 'vuelidate/lib/validators'

export default {
	props: {
		threshold: { type: Object },
		showDialog: { type: Boolean },
		isEditing: { type: Boolean, default: false },
	},
	data() {
		return {
			label: null,
			above: null,
			action: '',
			actions: [
				{
					name: 'Merge',
					value: 'merge'
				},
				{
					name: 'Notify',
					value: 'notify'
				}
			]
		}
	},
	computed: {
		dialogTitle() {
			return this.isEditing ? 'Edit Match Threshold' : 'Add Match Threshold'
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
		label: { required },
		above: { required, integer },
		action: { required }
	},
	mounted() {
	},
	methods: {
		updateValues() {
			this.$v.$reset()
			const threshold = this.threshold
			if (!threshold) {
				this.label = ''
				this.above = ''
				this.action = ''
				return
			}

			this.label = threshold.label
			this.above = threshold.above
			this.action = threshold.action
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

			const threshold = {
				label: this.label,
				above: this.above,
				action: this.action
			}
			this.$emit('save', threshold)
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
