<template>
	<v-dialog
		v-model="open"
		width="500"
		persistent
		@keydown.esc="close"
	>
		<v-form @submit.prevent="addFlow">
			<v-card>
				<v-card-title>{{dialogTitle}}<v-spacer></v-spacer><v-icon @click="close">close</v-icon></v-card-title>
				<v-card-text>
					<v-text-field
						required
						:autofocus="!isEditing"
						:disabled="isEditing"
						label="Flow Name"
						v-model="flowName"
						data-cy="addFlowDialog.flowNameField"
						:error-messages="inputErrors('flowName', 'Flow Name')"
						@input="$v.flowName.$touch()"
						@blur="$v.flowName.$touch()"
					></v-text-field>
					<!-- <v-expansion-panels v-model="advancedState">
						<v-expansion-panel data-cy="addFlowDialog.advancedBtn">
							<v-expansion-panel-header>Advanced</v-expansion-panel-header>
							<v-expansion-panel-content>
								<v-text-field
									label="Description"
									v-model="flowDescription"
									data-cy="addFlowDialog.flowDescriptionField"
								></v-text-field>
								<v-text-field
									required
									label="Batch Size"
									v-model="batchSize"
									data-cy="addFlowDialog.batchSizeField"
									:error-messages="inputErrors('batchSize', 'Batch Size')"
									@input="$v.batchSize.$touch()"
									@blur="$v.batchSize.$touch()"
								></v-text-field>
								<v-text-field
									required
									label="Thread Count"
									v-model="threadCount"
									data-cy="addFlowDialog.threadCountField"
									:error-messages="inputErrors('threadCount', 'Thread Count')"
									@input="$v.threadCount.$touch()"
									@blur="$v.threadCount.$touch()"
								></v-text-field>
							</v-expansion-panel-content>
						</v-expansion-panel>
					</v-expansion-panels> -->
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text color="secondary" @click="close" data-cy="addFlowDialog.cancelBtn">Cancel</v-btn>
					<v-btn :disabled="saveInProgress" type="submit" text color="primary" data-cy="addFlowDialog.saveBtn">Save</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import { mapActions } from 'vuex'
import { required } from 'vuelidate/lib/validators'

function noSpaces(value) {
	return value.match(/^[a-zA-Z0-9_]+$/) !== null
}

export default {
	props: {
		showDialog: { type: Boolean },
		isEditing: { type: Boolean, default: false },
		flow: { type: Object }
	},
	data() {
		return {
			saveInProgress: false,
			advancedState: null,
			isOpen: null,
			flowName: '',
			flowDescription: '',
			batchSize: 100,
			threadCount: 4,
			options: {}
		}
	},
	computed: {
		dialogTitle() {
			return this.isEditing ? 'Edit Flow' : 'Create Flow'
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
		flowName: { required, noSpaces },
		batchSize: { required },
		threadCount: { required }
  },
	mounted() {
		this.updateValues()
	},
	methods: {
		...mapActions({
			getFlow: 'flows/getFlow',
			saveFlow: 'flows/saveFlow'
		}),
		updateValues() {
			this.$v.$reset()
			const flow = this.flow
			if (!flow) {
				this.advancedState = null
				this.flowName = ''
				this.flowDescription = ''
				this.batchSize = 100
				this.threadCount = 4
				this.options = {}
				return
			}
			this.flowName = flow.name
			this.flowDescription = flow.description || ''
			this.batchSize = flow.batchSize || 100
			this.threadCount = flow.threadCount || 4
			this.options = flow.options || {}
		},
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('required') && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('noSpaces') && !this.$v[field].noSpaces && errors.push(`${fieldName} cannot contain spaces. Only letters, numbers, and underscore.`)
			return errors
		},
		addFlow() {
			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			this.saveInProgress = true

			const flow = {
				name: this.flowName,
				description: this.flowDescription,
				batchSize: this.batchSize,
				threadCount: this.threadCount,
				options: this.options
			}

			this.saveFlow(flow)
				.then(() => {
					return this.getFlow(flow.name)
				})
				.then(() => {
					this.open = false
					this.$emit('saved', flow)
					this.$emit('closed')
				})
				.finally(() => {
					this.saveInProgress = false
				})
		},
		close() {
			this.open = false
			this.$emit('closed')
		}
	},
	watch: {
		flow: 'updateValues',
		showDialog: 'updateValues'
	}
}
</script>
