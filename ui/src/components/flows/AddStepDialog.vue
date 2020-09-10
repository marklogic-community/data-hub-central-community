<template>
	<v-dialog
		v-model="open"
		width="500"
		persistent
		@keydown.esc="close"
	>
		<v-form @submit.prevent="addStep">
			<v-card>
				<v-card-title>{{dialogTitle}}<v-spacer></v-spacer><v-icon @click="close">close</v-icon></v-card-title>
				<v-card-text>
					<v-text-field
						required
						:autofocus="!isEditing"
						:disabled="isEditing"
						label="Step Name"
						v-model="stepName"
						data-cy="addStepDialog.stepNameField"
						:error-messages="inputErrors('stepName', 'Step Name')"
						@input="$v.stepName.$touch()"
						@blur="$v.stepName.$touch()"
					></v-text-field>
					<v-select
						:items="stepTypes"
						label="Step Type"
						item-text="name"
						item-value="value"
						v-model="stepType"
						:disabled="isEditing"
						data-cy="addStepDialog.stepTypeField"
						required
						:error-messages="inputErrors('stepType', 'Step Type')"
						@input="$v.stepType.$touch()"
						@blur="$v.stepType.$touch()"
					></v-select>
					<v-select
						:items="entities"
						label="Entity Type"
						v-model="entityName"
						:disabled="isEditing"
						data-cy="addStepDialog.entityTypeField"
						required
						:error-messages="inputErrors('stepType', 'Step Type')"
						@input="$v.entityName.$touch()"
						@blur="$v.entityName.$touch()"
					></v-select>
					<v-select
						:items="collections"
						label="Data Source"
						v-model="sourceCollection"
						data-cy="addStepDialog.dataSourceField"
						required
						:error-messages="inputErrors('sourceCollection', 'Data Source')"
						@change="$v.sourceCollection.$touch()"
						@blur="$v.sourceCollection.$touch()"
					></v-select>
					<v-expansion-panels v-model="advancedState">
						<v-expansion-panel data-cy="addStepDialog.advancedBtn">
							<v-expansion-panel-header>Advanced</v-expansion-panel-header>
							<v-expansion-panel-content>
								<v-text-field
									label="Description"
									v-model="stepDescription"
									data-cy="addStepDialog.stepDescField"
								></v-text-field>
								<!--
									disable for now to make it simpler
									<v-select
									:items="databases"
									item-text="name"
									label="Source Database"
									v-model="sourceDatabase"
									data-cy="addStepDialog.sourceDatabaseField"
									required
									:error-messages="inputErrors('sourceDatabase', 'Source Database')"
									@input="$v.sourceDatabase.$touch()"
									@blur="$v.sourceDatabase.$touch()"
								></v-select>
								<v-select
									:items="databases"
									item-text="name"
									label="Target Database"
									v-model="targetDatabase"
									data-cy="addStepDialog.targetDatabaseField"
									required
									:error-messages="inputErrors('targetDatabase', 'Target Database')"
									@input="$v.targetDatabase.$touch()"
									@blur="$v.targetDatabase.$touch()"
								></v-select> -->
								<v-select
									:items="dataFormats"
									label="Output Format"
									v-model="outputFormat"
									data-cy="addStepDialog.dataFormatField"
									required
									:error-messages="inputErrors('outputFormat', 'Output Format')"
									@input="$v.outputFormat.$touch()"
									@blur="$v.outputFormat.$touch()"
								></v-select>

							</v-expansion-panel-content>
						</v-expansion-panel>
					</v-expansion-panels>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text color="secondary" @click="close" data-cy="addStepDialog.cancelBtn">Cancel</v-btn>
					<v-btn :disabled="saveInProgress" type="submit" text color="primary" data-cy="addStepDialog.saveBtn">Save</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import _ from 'lodash'
import { mapState, mapActions } from 'vuex'
import flowsApi from '@/api/FlowsApi'
import { required } from 'vuelidate/lib/validators'

function noSpaces(value) {
	return value.match(/^[a-zA-Z0-9_]+$/) !== null
}

export default {
	props: {
		flowName: { type: String },
		// entities: { type: Array },
		showDialog: { type: Boolean },
		isEditing: { type: Boolean, default: false },
		step: { type: Object }
	},
	data() {
		return {
			saveInProgress: false,
			advancedState: null,
			isOpen: null,
			stepName: '',
			stepType: '',
			entityName: '',
			stepDescription: '',
			sourceCollection: '',
			outputFormat: 'json',
			sourceDatabase: 'Staging',
			targetDatabase: 'Final',

			allCollections: {},
			allDatabases: {},

			databases: ['Staging', 'Final'],
			stepTypes: [
				{
					name: 'Mapping',
					value: 'MAPPING'
				},
				{
					name: 'Matching',
					value: 'MATCHING'
				},
				{
					name: 'Merging',
					value: 'MERGING'
				}
				// {
				// 	name: 'Pipes',
				// 	value: 'CUSTOM'
				// }
			],
			dataFormats: ['json', 'xml']
		}
	},
	computed: {
		dialogTitle() {
			return this.isEditing ? 'Edit Step' : 'Create Step'
		},
		collections() {
			return this.sourceDatabase ? (this.allCollections[this.sourceDatabase.toLowerCase()] || []) : []
		},
		open: {
			get() {
				return this.isOpen || this.showDialog
			},
			set(val) {
				this.isOpen = val
			}
		},
		...mapState({
			entities: state => Object.keys(state.flows.entities)
		})
	},
	validations: {
    stepName: { required, noSpaces },
		stepType: { required },
		entityName: { required },
		sourceCollection: { required },
		sourceDatabase: { required },
		targetDatabase: { required },
		outputFormat: { required }
  },
	mounted() {
		this.stepType = this.stepTypes[0].value
		flowsApi.getNewStepInfo().then(info => {
			this.allCollections = info.collections,
			this.allDatabases = info.databases
			this.updateValues()
		})
	},
	methods: {
		...mapActions({
			getFlow: 'flows/getFlow'
		}),
		updateValues() {
			this.$v.$reset()
			const step = this.step
			if (!step) {
				this.advancedState = null
				this.stepName = ''
				this.stepType = ''
				this.entityName = ''
				this.stepDescription = ''
				this.sourceCollection = ''
				this.outputFormat = 'json'
				this.sourceDatabase = 'Staging'
				this.targetDatabase = 'Final'
				return
			}
			this.stepName = step.name
			this.stepType = step.stepDefinitionType
			this.entityName = step.options.targetEntity
			this.stepDescription = step.description
			this.sourceCollection = step.options.sourceCollection
			this.outputFormat = step.options.outputFormat
			this.sourceDatabase = _.capitalize(_.findKey(this.allDatabases, (db) => db === step.options.sourceDatabase))
			this.targetDatabase = _.capitalize(_.findKey(this.allDatabases, (db) => db === step.options.targetDatabase))
		},
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('required') && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('noSpaces') && !this.$v[field].noSpaces && errors.push(`${fieldName} cannot contain spaces. Only letters, numbers, and underscore.`)
			return errors
		},
		addStep() {
			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			this.saveInProgress = true

			const step = {
				name: this.stepName,
				description: this.stepDescription,
				options: {
					additionalCollections: [],
					targetEntity: this.entityName,
					sourceDatabase : this.allDatabases[this.sourceDatabase.toLowerCase()],
					targetDatabase : this.allDatabases[this.targetDatabase.toLowerCase()],
					collections : [this.entityName],
					sourceCollection : this.sourceCollection,
					sourceQuery: `cts.collectionQuery(["${this.sourceCollection}"])`,
					permissions: 'data-hub-operator,read,data-hub-operator,update',
					outputFormat: this.outputFormat
				},
				customHook: {
					module: '',
					parameters: {},
					user: '',
					runBefore: false
				},
				retryLimit: 0,
				batchSize: 100,
				threadCount: 4,
				stepDefinitionName: 'entity-services-mapping',
				stepDefinitionType: this.stepType
			}

			if (this.stepType === 'MAPPING') {
				step.options.mapping = {
          name : `${this.flowName}-${this.stepName}`,
          version : 1
				}
				// default to use our custom uri remapper hook. it will
				// allow 2 steps to run against the same input doc
				step.customHook.module = '/envision/customHooks/uriRemapper.sjs'
				step.stepDefinitionName = 'entity-services-mapping'
			}
			else if (this.stepType === 'MATCHING') {
				step.options = Object.assign(step.options, {
					matchOptions: {
						propertyDefs: { property: [] },
						algorithms: { algorithm: [] },
						collections: { content: [] },
						scoring: {
							add: [],
							expand: [],
							reduce: []
						},
						actions: { action: [] },
						thresholds: { threshold: [] },
						tuning: { maxScan: 200 }
					}
				})
				step.stepDefinitionName = 'default-matching'
			}
			else if (this.stepType === 'MERGING') {
				step.options = Object.assign(step.options, {
					matchOptions: '',
					mergeOptions: {
						propertyDefs: {
							properties: [],
							namespaces: {}
						},
						algorithms: {
							stdAlgorithm: { timestamp: {} },
							custom: [],
							collections: {}
						},
						mergeStrategies: [],
						merging: [
							{
								algorithmRef: "standard",
								sourceWeights: [],
								default: true
							}
						]
					}
				})
				step.stepDefinitionName = 'default-merging'
			}
			flowsApi.createStep(this.flowName, step)
				.then(() => {
					return this.getFlow()
				})
				.then(() => {
					this.open = false
					this.$emit('saved', step)
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
		step: 'updateValues',
		showDialog: 'updateValues',
		stepType() {
			if (this.stepType == 'MATCHING' || this.stepType === 'MERGING') {
				this.sourceDatabase = 'Final'
			}
			else {
				this.sourceDatabase = 'Staging'
			}
		}
	}
}
</script>
