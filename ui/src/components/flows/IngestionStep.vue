<template>
	<v-container>
		<v-text-field
			required
			autofocus
			label="Ingest Path"
			v-model="inputPath"
			data-cy="ingestionStep.inputPath"
			:error-messages="inputErrors('inputPath', 'Input Path')"
			@input="$v.inputPath.$touch()"
			@blur="save"
		></v-text-field>
	</v-container>
</template>

<script>
import { mapState } from 'vuex'
import { required } from 'vuelidate/lib/validators'

export default {
	props: {
		step: {type: Object}
	},
	data() {
		return {
			inputPath: null
		}
	},
	computed: {
		properties() {
			return this.targetEntity ? this.targetEntity.properties.map(p => p.name) : []
		},
		tableData() {
			return this.options.map((o, idx) => ({
				propertyName: o.propertyName || o.propertiesReduce.join(', '),
				matchType: this.getMatchType(o.algorithmRef),
				weight: o.weight,
				raw: o,
				index: idx,
				zip5match9: o.zip5match9,
				zip9match5: o.zip9match5
			}))
		},
		targetEntity() {
			const targetEntityType = this.step.options ? this.step.options.targetEntity: this.step.targetEntityType;
			return this.entities[targetEntityType]
		},
		...mapState({
			entities: state => state.flows.entities
		})
	},
	validations: {
		inputPath: { required }
  },
	methods: {
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('required') && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			return errors
		},
		save() {
			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}
			const newStep = {
				...this.step,
				inputFilePath: this.inputPath,
				fileLocations: {
					...this.step.fileLocations,
					inputFilePath: this.inputPath
				}
			}
			this.$emit('saveStep', newStep)
		},
		updateValues() {
			this.inputPath = this.step.fileLocations.inputFilePath
		}
	},
	mounted() {
		this.updateValues()
	},
	watch: {
		'$route.params': 'updateValues'
	}
}
</script>

<style lang="less" scoped>
span.lbl {
	font-weight: bold;
	margin-right: 10px;
	color: #666;
}

.v-card.options {
	margin-bottom: 2em;
}

.capitalize {
	text-transform: capitalize;
}
</style>
