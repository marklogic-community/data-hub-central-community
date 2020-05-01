<template>
<v-form @submit.prevent="save">
	<v-card>
		<v-card-title class="primary--text">{{isNew ? 'Add' : 'Edit'}} Property</v-card-title>
		<v-container>
			<v-text-field
				ref="propName"
				data-cy="editProperty.propName"
				color="primary"
				required
				:error="error"
				:error-messages="errorMsg"
				label="Property Name"
				v-model="name"
			></v-text-field>
			<v-select
				:items="dataTypeWithArray"
				data-cy="editProperty.dataType"
				item-text="text"
				item-value="value"
				label="Data Type"
				v-model="type"
			></v-select>
			<v-select
				v-show="isArray"
				:items="dataTypes"
				data-cy="editProperty.arrayDataType"
				item-text="text"
				item-value="value"
				label="Array Data Type"
				v-model="arrayType"
			></v-select>
			<v-expansion-panels v-model="advancedState">
				<v-expansion-panel data-cy="editProperty.advancedBtn">
					<v-expansion-panel-header>Advanced</v-expansion-panel-header>
					<v-expansion-panel-content>
						<v-layout row>
							<v-flex md6>
								<v-checkbox
									data-cy="prop.isElementRangeIndex"
									v-model="isElementRangeIndex"
									label="Element Range Index"
								></v-checkbox>
								<v-checkbox
									data-cy="prop.isRangeIndex"
									v-model="isRangeIndex"
									label="Path Range Index"
								></v-checkbox>
								<v-checkbox
									data-cy="prop.isPii"
									v-model="isPii"
									label="PII"
								></v-checkbox>
							</v-flex>
							<v-flex md6>
								<v-checkbox
									data-cy="prop.isPrimaryKey"
									v-model="isPrimaryKey"
									label="Primary Key"
								></v-checkbox>
								<v-checkbox
									data-cy="prop.isRequired"
									v-model="isRequired"
									label="Required"
								></v-checkbox>
								<v-checkbox
									data-cy="prop.isWordLexicon"
									v-model="isWordLexicon"
									label="Word Lexicon"
								></v-checkbox>
							</v-flex>
						</v-layout>
					</v-expansion-panel-content>
				</v-expansion-panel>
			</v-expansion-panels>

		</v-container>
		<v-card-actions>
			<v-spacer></v-spacer>
			<v-btn text color="secondary" @click="cancel">Cancel</v-btn>
			<v-btn data-cy="editProperty.createBtn" type="submit" text color="primary">Save</v-btn>
		</v-card-actions>
	</v-card>
</v-form>
</template>

<script>
import uuidv4 from 'uuid/v4';

export default {
	props: {
		adding: {type: String},
		existingProperties: {type: Array},
		prop: {type: Object},
		propertyName: {type: String},
		visible: {type: Boolean}
	},
	computed: {
		isNew() {
			return !(!!this.prop)
		},
		isArray() {
			return this.type === 'Array'
		},
		dataTypeWithArray() {
			return this.dataTypes.concat([{ text: "Array", value: "Array" }])
		}
	},
	data: () => ({
		advancedState: 0,
		error: false,
		errorMsg: null,
		rules: [
			value => !!value || 'Required.'
		],
		name: null,
		type: null,
		arrayType: 'String',
		dataTypes: [
			{ text: "Boolean", value: "Boolean" },
			{ text: "String", value: "String" },
			{ text: "Integer", value: "Integer" },
			{ text: "Decimal", value: "Decimal" },
			{ text: "Date", value: "Date" }
		],
		isRequired: false,
		isPii: false,
		isPrimaryKey: false,
		isElementRangeIndex: false,
		isRangeIndex: false,
		isWordLexicon: false
	}),
	methods: {
		reset() {
			this.advancedState = 0
			this.name = null
			this.type = 'String'
			this.error = false
			this.errorMsg = null
			this.updateValues()
		},
		updateValues() {
			console.log('updateValues')
			setTimeout(() => {
				this.$refs.propName.focus()
			}, 250);
			this.$nextTick(() => {
				this.advancedState = null
			})

			if (this.prop) {
				this.name = this.prop.name
				this.isRequired = this.prop.isRequired
				this.isPii = this.prop.isPii
				this.isPrimaryKey = this.prop.isPrimaryKey
				this.isElementRangeIndex = this.prop.isElementRangeIndex
				this.isRangeIndex = this.prop.isRangeIndex
				this.isWordLexicon = this.prop.isWordLexicon

				if (this.prop.isArray) {
					this.type = 'Array'
					this.arrayType = this.prop.type
				}
				else {
					this.type = this.prop.type
				}
			}
			else {
				this.type = 'String'
			}
		},
		save() {
			if (!this.name || this.name.length === 0) {
				this.error = true
				this.errorMsg = ['Property name is required']
				return
			}
			if (this.name && this.name.match(/^[a-zA-Z0-9_]+$/) == null) {
				this.error = true
				this.errorMsg = ['Property name cannot contain spaces. Only letters, numbers, and underscore']
				return
			}
			if (this.prop && this.prop._propId && this.existingProperties && this.existingProperties.find(p => p._propId !== this.prop._propId && p.name === this.prop.name)) {
				this.error = true
				this.errorMsg = ['Property already exists']
				return
			}
			this.$emit('save', {
				_propId: (this.prop && this.prop._propId) || uuidv4(),
				name: this.name,
				type: this.isArray ? this.arrayType : this.type,
				isArray: this.isArray,
				isRequired: this.isRequired,
				isPii: this.isPii,
				isPrimaryKey: this.isPrimaryKey,
				isElementRangeIndex: this.isElementRangeIndex,
				isRangeIndex: this.isRangeIndex,
				isWordLexicon: this.isWordLexicon
			})
		},
		cancel() {
			this.$emit('cancel')
		}
	},
	mounted: function() {
		this.updateValues()
	},
	watch: {
		visible(newVal) {
			if (newVal) {
				this.reset()
			}
		}
	}
}
</script>
