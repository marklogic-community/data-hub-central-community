<template>
<v-form @submit.prevent="save" v-if="newRelationship">
	<v-card>
		<v-card-title class="primary--text">{{adding ? 'Add' : 'Edit'}} a Relationship</v-card-title>
		<v-container>
			<v-text-field
				disabled
				label="From"
				v-model="newRelationship.from"
			></v-text-field>
			<v-text-field
				ref="desc"
				label="Description"
				:error="descError"
				:error-messages="descErrorMsg"
				required
				v-model="newRelationship.label"
			></v-text-field>
			<v-select
				:items="nodes"
				item-text="label"
				item-value="id"
				label="Target"
				v-model="newRelationship.to"
				:disabled="entityLocked"
				:error="targetError"
				:error-messages="targetErrorMsg"
				required
			></v-select>
			<v-card outlined>
				<v-card-text v-if="hasConcept">
					<v-select
						v-if="fromEntity && fromEntity.type === 'entity'"
						:items="fromEntity.properties"
						label="Property"
						item-text="name"
						item-value="value"
						v-model="newRelationship.keyTo"
						required
					></v-select>
					<v-select
						v-if="targetEntity && targetEntity.type === 'entity'"
						:items="targetEntity.properties"
						label="Property"
						item-text="name"
						item-value="value"
						v-model="newRelationship.keyFrom"
						required
					></v-select>
				</v-card-text>
				<v-card-text v-else>
      		<div>Linking</div>
					<v-row align="center">
						<v-col class="d-flex" cols="12" sm="5">
							<v-select
								v-if="fromEntity && (!hasConcept || fromEntity.type !== 'entity')"
								:items="getLinkItems(fromEntity, targetEntity)"
								:label="getLinkLabel(fromEntity)"
								item-text="name"
								item-value="value"
								v-model="newRelationship.keyFrom"
								:disabled="hasConcept && fromEntity.type === 'entity'"
								required
							></v-select>
						</v-col>
						<v-col class="text-center d-block" cols="12" sm="2">
							&lt;=&gt;
						</v-col>
						<v-col class="d-flex" cols="12" sm="5">
							<v-select
								v-if="targetEntity"
								:items="getLinkItems(targetEntity, fromEntity)"
								:label="getLinkLabel(targetEntity)"
								item-text="name"
								item-value="value"
								v-model="newRelationship.keyTo"
								:disabled="hasConcept && targetEntity.type === 'entity'"
								required
							></v-select>
						</v-col>
					</v-row>
					<v-alert dense color="error" v-if="linkError">{{linkError}}</v-alert>
				</v-card-text>
			</v-card>
			<v-radio-group v-model="newRelationship.cardinality" mandatory row v-if="!hasConcept">
				<template v-slot:label>
					<div>Cardinality</div>
				</template>
				<v-radio label="1:1" value="1:1"></v-radio>
				<v-radio label="1:Many" value="1:Many"></v-radio>
			</v-radio-group>
		</v-container>
		<v-card-actions>
			<v-spacer></v-spacer>
			<v-btn text color="secondary" @click="cancel">Cancel</v-btn>
			<v-btn type="submit" text color="primary">Save</v-btn>
		</v-card-actions>
	</v-card>
</v-form>
</template>

<script>
export default {
	name: 'edit-relationship',
	props: {
		adding: {type: String},
		relationship: {type: Object},
		nodes: {type: Array},
		existingRelNames: {type: Array},
		entityLocked: {type: Boolean},
		visible: {type: Boolean}
	},
	computed: {
		fromEntity() {
			return this.getEntity(this.newRelationship.from)
		},
		targetEntity() {
			return this.getEntity(this.newRelationship.to)
		},
		selectedProperties() {
			const entity = this.nodes.find(n => n.id === this.newRelationship.tdeSource)
			if (entity) {
				const name = entity.label
				return entity.properties.map(p => {
					return {
						name: `${name}.${p.name}`,
						value: p.name
					}
				})
			}
			return []
		},
		hasConcept() {
			return (this.fromEntity ? this.fromEntity.type === 'concept' : false) ||
			(this.targetEntity ? this.targetEntity.type === 'concept' : false)
		}
	},
	data: () => ({
		newRelationship: null,
		descError: false,
		descErrorMsg: null,
		targetError: false,
		targetErrorMsg: null,
		linkError: null
	}),
	watch: {
		relationship(newVal) {
			this.updateRelationship(newVal)
		},
		newRelationship: {
			handler(newVal) {
				this.updateToFrom()
			},
			deep: true
		},
		visible(newVal) {
			if (newVal) {
				this.reset()
			}
		}
	},
	mounted() {
		this.updateRelationship(this.relationship)
	},
	methods: {
		getLinkLabel(entity) {
			if (entity.type === 'entity') {
				return entity.label
			}
			return `${entity.label} value`
		},
		getLinkItems(entity, otherEntity) {
			if (entity.type === 'entity') {
				return entity.properties
			}
			else if (otherEntity) {
				return otherEntity.properties
			}
			return []
		},
		getEntity(id) {
			return id && this.nodes && this.nodes.find(n => n.id === id)
		},
		reset() {
			setTimeout(() => {
				this.$refs.desc.focus()
			}, 250)
			this.updateRelationship(this.relationship)
			this.descError = false
			this.descErrorMsg = null
			this.linkError = null
			this.targetError = false
			this.targetErrorMsg = null
		},
		validate() {
			if (!(this.newRelationship && this.newRelationship.label)) {
				this.descError = true
				this.descErrorMsg = ['Description is required']
				return false
			}
			if ((this.newRelationship && this.newRelationship.label) && this.newRelationship.label.match(/^[a-zA-Z0-9_]+$/) == null) {
				this.descError = true
				this.descErrorMsg = ['Description cannot contain spaces. Only letters, numbers, and underscore']
				return false
			}
			if (!this.targetEntity) {
				this.targetError = true
				this.targetErrorMsg = ['Target Entity is required']
				return false
			}
			if (!this.hasConcept) {
				return this.newRelationship.keyFrom === this.fromEntity.idField ||
					this.newRelationship.keyTo === this.targetEntity.idField
			}
			return true
		},
		save(e) {
			if (this.validate()) {
				this.$emit('save', this.newRelationship)
			}
			else {
				this.linkError = 'One Must be an id'
			}
		},
		cancel() {
			this.$emit('cancel')
		},
		updateRelationship(newVal) {
			const rel = JSON.parse(JSON.stringify(newVal))

			if (!rel.to && this.nodes) {
				rel.to = this.nodes[0].id
			}

			this.newRelationship = rel
			if (this.adding && !this.entityLocked) {
				this.newRelationship.to = null
			}
			if (this.newRelationship.from) {
				this.newRelationship.from = this.newRelationship.from.toLowerCase()
			}
			if (this.newRelationship.to) {
				this.newRelationship.to = this.newRelationship.to.toLowerCase()
			}
			this.updateToFrom()
		},
		updateToFrom() {
			if (this.hasConcept) {
				if (this.fromEntity.type === 'entity') {
					this.newRelationship.keyFrom = this.fromEntity.idField
				}
				else if (this.targetEntity && this.targetEntity.type === 'entity') {
					this.newRelationship.keyTo = this.targetEntity.idField
				}
			}
		}
	}
}
</script>
