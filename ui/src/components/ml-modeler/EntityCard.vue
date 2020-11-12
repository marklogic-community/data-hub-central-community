<template>
	<v-card>
		<v-tabs
			light
			v-model="whichTab">
			<v-tab class="tab" ripple>Properties</v-tab>
			<v-tab class="tab" ripple>Relationships</v-tab>
			<v-tab class="tab" ripple>Info</v-tab>
		</v-tabs>
		<v-tabs-items v-model="whichTab">
			<v-tab-item>
				<v-simple-table v-if="properties.length > 0">
					<thead>
						<tr>
							<th class="id-col"></th>
							<th class="primary--text">Name</th>
							<th class="primary--text">Type</th>
							<th class="primary--text">Action</th>
						</tr>
					</thead>
					<draggable v-bind="dragOptions" v-model="properties" tag="tbody" class="properties">
						<tr v-for="prop in properties" :key="prop.name" data-cy="entityPickList.propertyRow" :name="prop.name">
							<!-- td class="id-col" data-cy="entityPickList.entityPropertyId">
								<span v-if="currentEntity.idField === prop.name">id</span>
							</td -->

							<td class="id-col" data-cy="entityPickList.entityPropertyId">
								<span v-if="currentEntity.idField === prop.name" title="ID field">id</span>
								<edit-property-menu
									:prop="Object.assign({}, prop)"
									:entityName="currentEntity.entityName"
									:existingProperties="properties"
									@save="btnSaveProperty">
										<span v-if="prop.isPii" title="PII">pii</span>
										<span v-if="prop.isPrimaryKey" title="Primary Key">key</span>
										<span v-if="prop.isRequired" title="Required">req</span>
										<span v-if="prop.isElementRangeIndex || prop.isWordLexicon || prop.isRangeIndex" title="Advanced options">...</span>

									</edit-property-menu>



							</td>
							<td data-cy="entityPickList.entityPropertyName">{{prop.name}}</td>
							<td data-cy="entityPickList.entityPropertyType">{{prop.type}}{{prop.isArray ? '[]' : ''}}</td>
							<td class="action">
								<edit-property-menu
									:prop="Object.assign({}, prop)"
									:entityName="currentEntity.entityName"
									:existingProperties="properties"
									@save="btnSaveProperty">
									<button data-cy="entityPickList.editPropertyBtn" class="fa fa-pencil" aria-label="Edit Property" />
								</edit-property-menu>
								<button
									data-cy="entityPickList.deletePropertyBtn"
									class="fa fa-trash"
									v-on:click="btnDeleteProperties(currentEntity, prop.name)"
									aria-label="Delete property"></button>
							</td>
						</tr>
					</draggable>
				</v-simple-table>
				<p class="grey--text darken-4" v-else>No properties</p>
				<edit-property-menu
					v-if="currentEntity"
					:existingProperties="properties"
					:entityName="currentEntity.entityName"
					@save="btnSaveProperty">
					<v-btn
						color="primary"
						fab
						dark
						data-cy="entityPickList.addPropertyBtn"
					>
						<v-icon dark>add</v-icon>
					</v-btn>
				</edit-property-menu>
			</v-tab-item>
			<v-tab-item>
				<v-simple-table v-if="edges && edges.length > 0">
					<thead>
						<tr>
							<th class="primary--text">Description</th>
							<th class="primary--text">To entity</th>
							<th class="primary--text">Cardinality</th>
							<th class="primary--text">Action</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="edge in edges" :key="edge.id">
							<td>{{edge.label}}</td>
							<td>{{edge.to}}</td>
							<td>{{edge.cardinality}}</td>
							<td class="action">
								<v-menu
									:close-on-content-click="false"
									:nudge-width="300"
									v-model="relationshipsPopover[edge.id]"
									offset-x
								>
									<template v-slot:activator="{ on }">
										<button v-on="on" class="fa fa-pencil"
										aria-label="Edit relationship" />
									</template>
									<edit-relationship
										:visible="relationshipsPopover[edge.id] == true"
										:nodes="nodes"
										:relationship="edge"
										@save="btnSaveExistingEdge(currentEntity, edge.id, $event)"
										@cancel="cancelRelationshipsPopover(edge.id)"
									></edit-relationship>
								</v-menu>
								<button v-on:click="btnDeleteEdge(edge.id)" class="fa fa-trash"
										aria-label="Delete relationship" />
							</td>
						</tr>
					</tbody>
				</v-simple-table>
				<p class="grey--text darken-4" v-else>No relationships</p>
				<v-menu
					:close-on-content-click="false"
					:nudge-width="300"
					v-model="relationshipsPopover[currentEntity]"
					offset-x
				>
					<template v-slot:activator="{ on }">
						<v-btn
							color="primary"
							fab
							dark
							v-on="on"
						>
							<v-icon dark>add</v-icon>
						</v-btn>
					</template>
					<edit-relationship
						v-if="currentEntity"
						:visible="relationshipsPopover[currentEntity] == true"
						:ref="'add_relationship_' + currentEntity"
						:nodes="nodes"
						:relationship="Object.assign({from: currentEntity.id})"
						:existingRelNames="edgeIds"
						adding="true"
						@save="btnSaveNewEdge(currentEntity, $event)"
						@cancel="cancelRelationshipsPopover(currentEntity)"
					></edit-relationship>
				</v-menu>
			</v-tab-item>
			<v-tab-item v-if="currentEntity" class="info-tab">
				<v-select
					:items="properties"
					item-text="name"
					item-value="name"
					label="Entity Identifier"
					v-model="currentEntity.idField"
					@change="save()"
					outlined
				></v-select>
				<v-select
					:items="properties"
					item-text="name"
					item-value="name"
					label="Entity Label"
					v-model="currentEntity.labelField"
					@change="save()"
					outlined
				></v-select>
				<v-text-field
					outlined
					required
					color="primary"
					label="Version"
					v-model="version"
					data-cy="infoPane.version"
				></v-text-field>
				<v-text-field
					outlined
					required
					color="primary"
					label="IRI"
					v-model="baseUri"
					data-cy="infoPane.baseUri"
					:error="errorIRI"
					:error-messages="errorMsgIRI"
				></v-text-field>
			</v-tab-item>
		</v-tabs-items>
	</v-card>
</template>

<script>
import draggable from 'vuedraggable'
import EditRelationship from '@/components/EditRelationship.vue';
import EditPropertyMenu from '@/components/EditPropertyMenu.vue'

const BASE_URI_REGEX = /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w.-]+)+[\w\-._~:/?#[\]@!$&'()*+,;=.]+(\/)$/

export default {
	props: {
		entity: {type: Object},
		edges: {type: Array},
		nodes: {type: Array},
		edgeIds: {type: Array},
		activeTab: {type: Number}
	},
	components: {
		draggable,
		EditRelationship,
		EditPropertyMenu
	},
	computed: {
		properties: {
			get() {
				return this.currentEntity ? this.currentEntity.properties : []
			},
			set(props) {
				this.currentEntity.properties = props
				this.save()
			}
		},
		baseUri: {
			get() {
				return this.currentEntity.baseUri || 'http://marklogic.envision.com/'
			},
			set(val) {
				if (val && BASE_URI_REGEX.test(val)) {
					this.errorIRI = false
					this.errorMsgIRI = []
					this.currentEntity.baseUri = val
					this.save()
				}
				else {
					this.errorIRI = true
					this.errorMsgIRI = ['A valid IRI is required, e.g. http://marklogic.envision.com/']
				}
			}
		},
		version: {
			get() {
				return this.currentEntity.version || '0.0.1'
			},
			set(val) {
				this.currentEntity.version = val
			}
		}
	},
	data() {
		return {
			currentEntity: null,
			relationshipsPopover: {},
			whichTab: null,
			errorIRI: false,
			errorMsgIRI: null,
			dragOptions: {
				animation: 200,
			}
		}
	},
	watch: {
		activeTab(newVal) {
			this.whichTab = newVal
		},
		entity() {
			this.updateValues()
		}
	},
	mounted() {
		this.whichTab = this.activeTab || 0
		this.updateValues()
	},
	methods: {
		updateValues() {
			this.currentEntity = JSON.parse(JSON.stringify(this.entity))
		},
		cancelRelationshipsPopover(id) {
			this.$set(this.relationshipsPopover, id, false)
		},
		btnSaveProperty({oldProp, newProp}) {
			const idx = this.currentEntity.properties.findIndex(p => p._propId === (oldProp || {})._propId)
			if (idx >= 0) {
				this.$set(this.currentEntity.properties, idx, newProp)
			}
			else {
				this.currentEntity.properties.push(newProp)
			}
			console.log(this.currentEntity.properties)
			this.save()
		},
		save() {
			this.$emit('updated', this.currentEntity)
		},
		btnDeleteProperties(entity, propName){
			this.$emit ("deleteProperties", {entity, propName})
		},
		btnSaveNewEdge(item, relInfo) {
			this.$set(this.relationshipsPopover, item, false)
			this.$emit('saveEdge', {item, relInfo})
		},
		btnSaveExistingEdge(item, edgeId, relInfo) {
			this.$set(this.relationshipsPopover, edgeId, false)
			this.$emit('saveEdge', {item, relInfo})
		},
		btnDeleteEdge(edge) {
			this.$emit('deleteEdge', edge)
		},
	}
}
</script>

<style lang="less" scoped>
.v-window-item--active {
	padding: 10px;
}

::-webkit-scrollbar {
	-webkit-appearance: none;
	width: 7px;
}
::-webkit-scrollbar-thumb {
	border-radius: 4px;
	background-color: rgba(255,255,255,.5);
	-webkit-box-shadow: 0 0 1px rgba(255,255,255,.5);
	box-shadow: 0 0 1px rgba(255,255,255,.5);
}

.fa-pencil {
	margin-right: 10px;
}
.tab:hover {
  background-color:  #E1E1EF;
}

.small-btn {
	width: 30px;
	height: 30px;
}
.id-col {
	padding: 0px;
	span {
		font-weight: bold;
		font-size: 8px;
		border: 1px solid darkblue;
    padding: 4px;
    border-radius: 10px;
    background-color: lightblue;
	margin-right: 2px;
	}
}

</style>
