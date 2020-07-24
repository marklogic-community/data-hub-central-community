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
			<v-autocomplete
				:items="dataTypeWithArray"
				data-cy="editProperty.dataType"
				item-text="text"
				item-value="value"
				label="Data Type"
				v-model="type"
				:menu-props="{ 'content-class': 'menuDataType'}"
			></v-autocomplete>
			<v-autocomplete
				v-show="isArray"
				:items="dataTypes"
				data-cy="editProperty.arrayDataType"
				item-text="text"
				item-value="value"
				label="Array Data Type"
				v-model="arrayType"
				:menu-props="{ 'content-class': 'menuDataTypeArray'}"
			></v-autocomplete>
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
									v-if="primaryKey == '' || (prop && primaryKey == prop.name) "
									data-cy="prop.isPrimaryKey"
									v-model="isPrimaryKey"
									label="Primary Key"
								></v-checkbox>
								<v-checkbox
									v-else
									data-cy="prop.isPrimaryKey"
									:label="'Change Primary Key from ' + primaryKey"
									v-model="isPrimaryKey"
									v-on:click="removeOldPK"
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
			<v-btn data-cy="editProperty.cancelBtn"text color="secondary" @click="cancel">Cancel</v-btn>
			<v-btn data-cy="editProperty.createBtn" type="submit" text color="primary">Save</v-btn>
		</v-card-actions>
	</v-card>
</v-form>
</template>

<script>
import uuidv4 from 'uuid/v4';

export default {
	props: {
		entityName: {type: String},
		adding: {type: String},
		existingProperties: {type: Array},
		prop: {type: Object},
		propertyName: {type: String},
		visible: {type: Boolean}
	},
	computed: {
		isNew() {
			return !(this.prop)
		},
		isArray() {
			return this.type === 'array'
		},
		dataTypes() {
			return this.rawDataTypes.map(t => {
				return {
					text: t,
					value: t
				}
			})
		},
		dataTypeWithArray() {
			return [{ text: "array", value: "array" }].concat(this.dataTypes)
		},
		primaryKey() { 
			// feature/issue-12 - check to see if any prop is defined as a primary key
			if (this.existingProperties) {
				for (var i=0; i<this.existingProperties.length; i++) {
					if ( this.existingProperties[i].isPrimaryKey) {
						return this.existingProperties[i].name
					}
				}
			}
			return ""
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
		arrayType: 'string',
		rawDataTypes: [
			'string', 'decimal', 'date', 'dateTime', 'integer', 'boolean',
			'anyURI', 'base64Binary', 'byte',
			'dayTimeDuration',
			'double', 'duration', 'float', 'gDay', 'gMonth',
			'gMonthDay', 'gYear', 'gYearMonth', 'hexBinary',
			'int', 'long', 'negativeInteger',
			'nonNegativeInteger', 'nonPositiveInteger',
			'positiveInteger','short', 'time',
			'unsignedByte', 'unsignedInt', 'unsignedLong',
			'unsignedShort', 'yearMonthDuration', 'iri'
		],
		isRequired: false,
		isPii: false,
		isPrimaryKey: false,
		isElementRangeIndex: false,
		isRangeIndex: false,
		isWordLexicon: false
	}),
	methods: {
		removeOldPK(){
			if (this.existingProperties) {
				for (var i=0; i<this.existingProperties.length; i++) {
					if (this.existingProperties[i].name != this.name ) {
						this.existingProperties[i].isPrimaryKey = false
					}	
				}
			}
			this.isPrimaryKey = true
		},
		reset() {
			this.advancedState = 0
			this.name = null
			this.type = 'string'
			this.error = false
			this.errorMsg = null
			this.updateValues()
		},
		updateValues() {
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
					this.type = 'array'
					this.arrayType = this.prop.type
				}
				else {
					this.type = this.prop.type
				}
			}
			else {
				// feature/issue-12 - reset all values
				this.type = 'string'
				this.name = ""
				this.isRequired = false
				this.isPii = false
				this.isPrimaryKey = false
				this.isElementRangeIndex = false
				this.isRangeIndex = false
				this.isWordLexicon = false
			}

		},
		save() {
			if (!this.name || this.name.length === 0) {
				this.error = true
				this.errorMsg = ['Property name is required']
				return
			}
			if (this.name === this.entityName) {
				this.error = true
				this.errorMsg = ['Property name cannot be the same as the Entity Name']
				return
			}

			if (this.name && this.name.match(/^[a-zA-Z0-9_]+$/) == null) {
				this.error = true
				this.errorMsg = ['Property name cannot contain spaces. Only letters, numbers, and underscore']
				return
			}
			if (this.prop && this.prop._propId && this.existingProperties && this.existingProperties.find(p => p._propId !== this.prop._propId && p.name === this.name)) {
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
