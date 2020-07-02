<template>
<v-form @submit.prevent="save">
	<v-card light>
		<v-card-title class="primary--text">{{dialogTitle}}</v-card-title>
		<v-container>
			<v-radio-group v-model="type" mandatory row>
				<template v-slot:label>
					<div>Type</div>
				</template>
				<v-radio label="Entity" value="entity"></v-radio>
				<v-radio label="Concept" value="concept"></v-radio>
			</v-radio-group>

			<v-text-field
				ref="entName"
				required
				:error="error"
				:error-messages="errorMsg"
				:label="typeLabel"
				v-model="entityName"
				data-cy="addEntity.entityNameField"
			></v-text-field>
				<v-expansion-panels v-model="advancedState">
				<v-expansion-panel data-cy="addEntity.advancedBtn">
					<v-expansion-panel-header>Advanced</v-expansion-panel-header>
					<v-expansion-panel-content>
						<v-text-field
							v-if = "this.type === 'entity'"
							label="version"
							v-model="version"
							data-cy="addEntity.versionField"
							>
						</v-text-field>
						<v-text-field
							label="IRI"
							v-model="iri"
							:error="errorIRI"
							:error-messages="errorMsgIRI"
							data-cy="addEntity.iriField"
						>
						</v-text-field>
					</v-expansion-panel-content>
				</v-expansion-panel>
			</v-expansion-panels>
		</v-container>

		<v-card-actions>
			<v-spacer></v-spacer>
			<v-btn text color="secondary" @click="cancel">Cancel</v-btn>
			<v-btn data-cy="addEntity.createEntityButton" type="submit" text color="primary">Create</v-btn>
		</v-card-actions>
	</v-card>
</v-form>
</template>

<script>
const BASE_URI_REGEX = /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\.-]+)+[\w\-\._~:/?#[\]@!\$&'\(\)\*\+,;=.]+(\/)$/
export default {
	props: {
		existingEntityNames: {type: Array}
	},
	data: () => ({
		advancedState: null,
		iri: "http://marklogic.envision.com/",
		version: "0.0.1",
		type: 'entity',
		entityName: null,
		error: false,
		errorMsg: null,
		errorIRI: false,
		errorMsgIRI: null,
		rules: [
			value => !!value || 'Required.'
		],
	}),
	computed: {
		typeLabel() {
			return (this.type === 'entity') ? 'Entity Name' : 'Concept Name'
		},
		dialogTitle() {
			return (this.type === 'entity') ? 'Add New Entity' : 'Add New Concept'
		}
	},
	methods: {
		reset() {
			setTimeout(() => {
				this.$refs.entName.focus()
			}, 1);
			this.type = 'entity'
			this.error = false
			this.errorMsg = []
			this.errorIRI = false
			this.errorMsgIRI = []
			this.entityName = null
			this.advancedState = null
			this.iri = "http://marklogic.envision.com/"
			this.version = "0.0.1"
		},
		save() {
			if (!this.entityName || this.entityName.length === 0) {
				this.error = true
				this.errorMsg = ['Entity name is required']
				return
			}
			if (this.entityName && this.entityName.match(/^[a-zA-Z0-9_]+$/) == null) {
				this.error = true
				this.errorMsg = [`${this.typeLabel} cannot contain spaces. Only letters, numbers, and underscore`]
				return
			}
			if (!(this.iri && BASE_URI_REGEX.test(this.iri))) {
				this.errorIRI = true
				this.errorMsgIRI = ['A valid IRI is required, e.g. http://marklogic.envision.com/']
				return
			}
			if (this.existingEntityNames.indexOf(this.entityName.toLowerCase()) >= 0) {
				this.error = true
				this.errorMsg = ['Entity already exists']
				return
			}
			this.$emit('save', { type: this.type, name: this.entityName, iri: this.iri, version: this.version})
		},
		cancel() {
			this.$emit('cancel')
		}
	}
}
</script>
