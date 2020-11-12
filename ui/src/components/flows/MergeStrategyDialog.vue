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
						autofocus
						required
						label="Name"
						v-model="name"
						data-cy="mergeStrategyDlg.nameField"
						:error-messages="inputErrors('name', 'Name')"
						@input="$v.name.$touch()"
						@blur="$v.name.$touch()"
					></v-text-field>

					<v-text-field
						required
						label="Max Values"
						v-model="maxValues"
						data-cy="mergeStrategyDlg.maxValuesField"
						:error-messages="inputErrors('maxValues', 'Max Values')"
						@input="$v.maxValues.$touch()"
						@blur="$v.maxValues.$touch()"
					></v-text-field>

					<v-text-field
						required
						label="Max Sources"
						v-model="maxSources"
						data-cy="mergeStrategyDlg.maxSourcesField"
						:error-messages="inputErrors('maxSources', 'Max Sources')"
						@input="$v.maxSources.$touch()"
						@blur="$v.maxSources.$touch()"
					></v-text-field>

					<div>
						<span>Source Weights</span>
						<v-btn
							data-cy="mergeStrategyDlg.addSourceWeightBtn"
							color="primary"
							@click="addSourceWeight"
							fab small><v-icon small>add</v-icon></v-btn>
					</div>
					<v-container>
						<v-row v-for="(sw, index) in sourceWeights" :key="index">
							<v-col md="6">
								<v-text-field
									required
									label="Source Name"
									v-model="sw.name"
									data-cy="mergeStrategyDlg.sourceWeightNameField"
									:error-messages="sourceWeightErrors(index, 'name', 'Source Name')"
									@input="$v.sourceWeights.$each.$iter[index].name.$touch()"
									@blur="$v.sourceWeights.$each.$iter[index].name.$touch()"
								></v-text-field>
							</v-col>
							<v-col md="5">
								<v-text-field
									required
									label="Weight"
									v-model="sw.weight"
									data-cy="mergeStrategyDlg.sourceWeightWeightField"
									:error-messages="sourceWeightErrors(index, 'weight', 'Weight')"
									@input="$v.sourceWeights.$each.$iter[index].weight.$touch()"
									@blur="$v.sourceWeights.$each.$iter[index].weight.$touch()"
								></v-text-field>
							</v-col>
							<v-col md="1">
								<v-btn
									data-cy="mergeStrategyDlg.removeSourceWeightBtn"
									color="primary"
									@click="removeSourceWeight(index)"
									fab small><v-icon small>remove</v-icon></v-btn>
							</v-col>
						</v-row>
					</v-container>
					<v-text-field
						required
						label="Length Weight"
						v-model="lengthWeight"
						data-cy="mergeStrategyDlg.lengthWeightField"
						:error-messages="inputErrors('lengthWeight', 'Length Weight')"
						@input="$v.lengthWeight.$touch()"
						@blur="$v.lengthWeight.$touch()"
					></v-text-field>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text color="secondary" @click="close" data-cy="mergeStrategyDlg.cancelBtn">Cancel</v-btn>
					<v-btn type="submit" text color="primary" data-cy="mergeStrategyDlg.saveBtn">Save</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import { mapActions } from 'vuex'
import { required, integer } from 'vuelidate/lib/validators'

function strategyNotInUse(strategy) {
	return (this.strategy && strategy === this.strategy.name)
		|| !this.strategies.includes(strategy)
}

export default {
	props: {
		strategy: { type: Object },
		properties: { type: Array },
		strategies: { type: Array },
		showDialog: { type: Boolean },
		isEditing: { type: Boolean, default: false },
	},
	data() {
		return {
			advancedState: null,
			isOpen: null,
			name: '',
			maxValues: null,
			maxSources: null,
			sourceWeights: [],
			lengthWeight: null
		}
	},
	computed: {
		dialogTitle() {
			return this.isEditing ? 'Edit Merge Strategy' : 'Add Merge Strategy'
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
		name: { required, strategyNotInUse },
		maxValues: { integer },
		maxSources: { integer },
		sourceWeights: {
			$each: {
        name: { required },
				weight: { required, integer }
      }
		},
		lengthWeight: { integer }
  },
	mounted() {
	},
	methods: {
		...mapActions({
			getFlow: 'flows/getFlow'
		}),
		addSourceWeight() {
			this.sourceWeights.push({
				name: null,
				weight: null
			})
		},
		removeSourceWeight(idx) {
			this.sourceWeights.splice(idx, 1)
		},
		updateValues() {
			this.$v.$reset()
			const strategy = this.strategy
			if (!strategy) {
				this.name = ''
				this.maxValues = null
				this.maxSources = null
				this.sourceWeights = []
				this.lengthWeight = null
				return
			}

			this.name = strategy.name
			this.maxValues = strategy.maxValues
			this.maxSources = strategy.maxSources
			this.sourceWeights = strategy.sourceWeights.map(sw => sw.source)
			this.lengthWeight = strategy.length.weight
		},
		sourceWeightErrors(idx, field, fieldName) {
			const errors = []
			if (!this.$v.sourceWeights.$each.$iter[idx][field].$dirty) return errors
			this.$v.sourceWeights.$each.$iter[idx][field].$invalid && this.$v.sourceWeights.$each.$iter[idx][field].$params.required && !this.$v.sourceWeights.$each.$iter[idx][field].required && errors.push(`${fieldName} is required.`)
			this.$v.sourceWeights.$each.$iter[idx][field].$invalid && this.$v.sourceWeights.$each.$iter[idx][field].$params.integer && !this.$v.sourceWeights.$each.$iter[idx][field].integer && errors.push(`${fieldName} must be an integer.`)
			return errors
		},
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.required && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			this.$v[field].$invalid && this.$v[field].$params.integer && !this.$v[field].integer && errors.push(`${fieldName} must be an integer.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('strategyNotInUse') && !this.$v[field].strategyNotInUse && errors.push(`${fieldName} must be unique.`)
			return errors
		},
		save() {
			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			const strategy = {
				...(this.strategy || {}),
				algorithmRef: 'standard',
				length: {
					weight: this.lengthWeight
				},
				maxSources: this.maxSources,
				maxValues: this.maxValues,
				name: this.name,
				sourceWeights: this.sourceWeights.map(sw => ({
					source: {
						...sw
					}
				}))
			}
			this.$emit('save', strategy)
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
