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
						:items="matchTypes"
						label="Match Type"
						item-text="name"
						item-value="value"
						v-model="matchType"
						data-cy="matchOptionDlg.matchTypeField"
						:disabled="isEditing"
						required
						:error-messages="inputErrors('matchType', 'Match Type')"
						@input="$v.matchType.$touch()"
						@blur="$v.matchType.$touch()"
					></v-select>

					<template v-if="matchType === 'standard-reduction'">
						<v-select
							chips
							multiple
							:items="properties"
							label="Properties to Match"
							v-model="propertiesReduce"
							data-cy="matchOptionDlg.propertiesReduceField"
							:disabled="isEditing"
							required
							:error-messages="inputErrors('propertiesReduce', 'Properties to Match')"
							@input="$v.propertiesReduce.$touch()"
							@blur="$v.propertiesReduce.$touch()"
						></v-select>
					</template>
					<v-select
						v-else
						:items="properties"
						label="Property to Match"
						v-model="propertyName"
						data-cy="matchOptionDlg.propertyNameField"
						:disabled="isEditing"
						required
						:error-messages="inputErrors('propertyName', 'Property to Match')"
						@input="$v.propertyName.$touch()"
						@blur="$v.propertyName.$touch()"
					></v-select>

					<v-text-field
						v-if="matchType !== 'zip-match'"
						required
						label="Weight"
						v-model="weight"
						data-cy="matchOptionDlg.weightField"
						:error-messages="inputErrors('weight', 'Weight')"
						@input="$v.weight.$touch()"
						@blur="$v.weight.$touch()"
					></v-text-field>

					<!-- <template v-if="matchType === 'thesaurus'">
					</template>

					<template v-if="matchType === 'double-metaphone'">
					</template> -->

					<template v-if="matchType === 'zip-match'">
						<v-text-field
							required
							label="5-vs-9 Match Weight"
							v-model="zip5match9"
							data-cy="matchOptionDlg.zip5match9Field"
							:error-messages="inputErrors('zip5match9', '5-vs-9 Match Weight')"
							@input="$v.zip5match9.$touch()"
							@blur="$v.zip5match9.$touch()"
						></v-text-field>

						<v-text-field
							required
							label="9-vs-5 Match Weight"
							v-model="zip9match5"
							data-cy="matchOptionDlg.zip9match5Field"
							:error-messages="inputErrors('zip9match5', '9-vs-5 Match Weight')"
							@input="$v.zip9match5.$touch()"
							@blur="$v.zip9match5.$touch()"
						></v-text-field>
					</template>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text color="secondary" @click="close" data-cy="matchOptionDlg.cancelBtn">Cancel</v-btn>
					<v-btn type="submit" text color="primary" data-cy="matchOptionDlg.saveBtn">Save</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import { mapActions } from 'vuex'
import { required, integer } from 'vuelidate/lib/validators'

export default {
	props: {
		option: { type: Object },
		properties: { type: Array },
		showDialog: { type: Boolean },
		isEditing: { type: Boolean, default: false },
	},
	data() {
		return {
			advancedState: null,
			isOpen: null,
			matchType: '',
			propertyName: '',
			propertiesReduce: [],
			weight: null,
			zip5match9: null,
			zip9match5: null,

			matchTypes: [
				{
					name: 'Exact',
					value: 'exact'
				},
				// {
				// 	name: 'Synonym',
				// 	value: 'thesaurus'
				// },
				// {
				// 	name: 'Double Metaphone',
				// 	value: 'double-metaphone'
				// },
				{
					name: 'Zip',
					value: 'zip-match'
				},
				{
					name: 'Reduce',
					value: 'standard-reduction'
				}
				// ,
				// {
				// 	name: 'Custom',
				// 	value: 'custom'
				// }
			]
		}
	},
	computed: {
		dialogTitle() {
			return this.isEditing ? 'Edit Match Option' : 'Add Match Option'
		},
		collections() {
			return this.database ? (this.allCollections[this.database.toLowerCase()] || []) : []
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
	validations() {
		let vals = {
			matchType: { required },
		}
		if (this.matchType === 'standard-reduction') {
			vals.propertiesReduce = { required }
		}
		else {
			vals.propertyName = { required }
		}

		if (this.matchType === 'zip-match') {
			vals.zip5match9 = { required, integer }
			vals.zip9match5 = { required, integer }
		}
		else {
			vals.weight = { required, integer }
		}
		return vals
  },
	mounted() {
		this.matchType = this.matchTypes[0].value
	},
	methods: {
		...mapActions({
			getFlow: 'flows/getFlow'
		}),
		updateValues() {
			this.$v.$reset()
			const option = this.option
			if (!option) {
				this.advancedState = null
				this.matchType = ''
				this.propertyName = ''
				this.propertiesReduce = []
				this.weight = null
				this.zip5match9 = null
				this.zip9match5 = null
				return
			}

			this.matchType = option.algorithmRef || 'exact'
			this.propertyName = option.propertyName
			this.propertiesReduce = option.propertiesReduce
			this.weight = option.weight
			this.zip5match9 = option.zip5match9
			this.zip9match5 = option.zip9match5
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
				algorithmRef: (this.matchType !== 'exact') ? this.matchType : null,
				propertyName: (this.matchType !== 'standard-reduction') ? this.propertyName : null,
				propertiesReduce: this.propertiesReduce,
				weight: (this.matchType !== 'zip-match') ? this.weight : null,
				zip5match9: (this.matchType === 'zip-match') ? this.zip5match9 : null,
				zip9match5: (this.matchType === 'zip-match') ? this.zip9match5 : null
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
