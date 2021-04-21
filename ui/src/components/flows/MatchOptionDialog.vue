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

					<template v-if="matchType === 'reduce'">
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
						required
						integer
						label="Weight"
						v-model="weight"
						data-cy="matchOptionDlg.weightField"
						:error-messages="inputErrors('weight', 'Weight')"
						@input="$v.weight.$touch()"
						@blur="$v.weight.$touch()"
					></v-text-field>

					<template v-if="matchType === 'synonym'">
						<v-text-field
							required
							label="Thesaurus URI"
							v-model="thesaurusURI"
							data-cy="matchOptionDlg.thesaurusUriField"
							:error-messages="inputErrors('thesaurusURI', 'Thesaurus URI')"
							@input="$v.thesaurusURI.$touch()"
							@blur="$v.thesaurusURI.$touch()"
						></v-text-field>
					</template>

					<template v-if="matchType === 'doubleMetaphone'">
						<v-text-field
							required
							label="Dictionary URI"
							v-model="dictionaryURI"
							data-cy="matchOptionDlg.dictionaryUriField"
							:error-messages="inputErrors('dictionaryURI', 'Dictionary URI')"
							@input="$v.dictionaryURI.$touch()"
							@blur="$v.dictionaryURI.$touch()"
						></v-text-field>
						<v-text-field
							required
							label="Distance Threshold"
							v-model="distanceThreshold"
							data-cy="matchOptionDlg.distanceThresholdField"
							:error-messages="inputErrors('distanceThreshold', 'Distance Threshold')"
							@input="$v.distanceThreshold.$touch()"
							@blur="$v.distanceThreshold.$touch()"
						></v-text-field>
					</template>
					<template v-if="matchType === 'custom'">
						<v-text-field
							label="Module Namespace"
							v-model="algorithmModuleNamespace"
							data-cy="matchOptionDlg.algorithmModuleNamespaceField"
							:error-messages="inputErrors('algorithmModuleNamespace', 'Module Namespace')"
							@input="$v.algorithmModuleNamespace.$touch()"
							@blur="$v.algorithmModuleNamespace.$touch()"
						></v-text-field>
						<v-text-field
							required
							label="Module Path"
							v-model="algorithmModulePath"
							data-cy="matchOptionDlg.algorithmModulePathField"
							:error-messages="inputErrors('algorithmModulePath', 'Module Path')"
							@input="$v.algorithmModulePath.$touch()"
							@blur="$v.algorithmModulePath.$touch()"
						></v-text-field>
						<v-text-field
							required
							label="Function Name"
							v-model="algorithmFunction"
							data-cy="matchOptionDlg.algorithmFunctionField"
							:error-messages="inputErrors('algorithmFunction', 'Function Name')"
							@input="$v.algorithmFunction.$touch()"
							@blur="$v.algorithmFunction.$touch()"
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
			dictionaryURI: null,
			distanceThreshold: 100,
			thesaurusURI: null,
			algorithmModuleNamespace: null,
			algorithmModulePath: null,
			algorithmFunction: null,
			matchTypes: [
				{
					name: 'Exact',
					value: 'exact'
				},
		    {
					name: 'Synonym',
			 		value: 'synonym'
			  },
			  {
					name: 'Double Metaphone',
			 		value: 'doubleMetaphone'
			  },
			 {
					name: 'Zip',
					value: 'zip'
			 },
			 {
				 	name: 'Reduce',
					value: 'reduce'
			 },
				{
					name: 'Custom',
					value: 'custom'
				}
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
		if (this.matchType === 'reduce') {
			vals.propertiesReduce = { required }
		}
		else {
			vals.propertyName = { required }
		}

		vals.weight = { required, integer }
		return vals
  },
	mounted() {
		this.matchType = this.matchTypes[0].value
	},
	methods: {
		updateValues() {
			this.$v.$reset()
			const option = this.option
			if (!option) {
				this.advancedState = null
				this.matchType = ''
				this.propertyName = ''
				this.propertiesReduce = []
				this.weight = null
				return
			}

			this.reduce = option.reduce
			this.matchType = this.reduce ? 'reduce': option.matchRules[0].matchType
			this.propertyName = option.matchRules[0].entityPropertyPath
			this.propertiesReduce = option.reduce ? option.matchRules.map((rule) => rule.entityPropertyPath): [];
			this.weight = option.weight
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
			let reduce = this.matchType === 'reduce';
			const matchRuleset = {
				reduce,
				weight: this.weight
			}
			if (reduce) {
				matchRuleset.matchRules = this.propertiesReduce.map((prop) => {return { entityPropertyPath: prop, matchType: 'exact', options: {} }})
			} else {
				const matchRule = { entityPropertyPath: this.propertyName, matchType: this.matchType, options: {} }
				matchRuleset.matchRules = [ matchRule ]
				switch (this.matchType) {
					case "doubleMetaphone":
						matchRule.options.dictionaryURI = this.dictionaryURI
						matchRule.options.distanceThreshold = this.distanceThreshold
						break;
					case "synonym":
						matchRule.options.thesaurusURI = this.thesaurusURI
						break;
					case "custom":
						matchRule.options.dictionaryURI = this.dictionaryURI
						matchRule.options.distanceThreshold = this.distanceThreshold
						break;
				}
			}
			let prefix = reduce ? 'Reduce: ' : '';
			let rulesetTitle = matchRuleset.matchRules.map((rule) => `${rule.entityPropertyPath} - ${rule.matchType}`).join(', ');
			matchRuleset.name = `${prefix}${rulesetTitle}`;
			this.$emit('save', matchRuleset)
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
