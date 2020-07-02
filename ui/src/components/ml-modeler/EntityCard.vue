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
				<v-simple-table v-if="entities[entity].properties.length > 0">
					<thead>
						<tr>
							<th class="id-col"></th>
							<th class="primary--text">Name</th>
							<th class="primary--text">Type</th>
							<th class="primary--text">Action</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="prop in entities[entity].properties" :key="prop.name">
							<td class="id-col" data-cy="entityPickList.entityPropertyId">
								<span v-if="entities[entity].idField === prop.name">id</span>
							</td>
							<td data-cy="entityPickList.entityPropertyName">{{prop.name}}</td>
							<td data-cy="entityPickList.entityPropertyType">{{prop.type}}{{prop.isArray ? '[]' : ''}}</td>
							<td class="action">
								<edit-property-menu
									:prop="Object.assign({}, prop)"
									:entityName="entity"
									:existingProperties="entities[entity].properties"
									@save="btnSaveProperty(entity, $event)">
									<button data-cy="entityPickList.editPropertyBtn" class="fa fa-pencil" aria-label="Edit Property" />
								</edit-property-menu>
								<button
									data-cy="entityPickList.deletePropertyBtn"
									class="fa fa-trash"
									v-on:click="btnDeleteProperties(entity, prop.name)"
									aria-label="Delete property"></button>
							</td>
						</tr>
					</tbody>
				</v-simple-table>
				<p class="grey--text darken-4" v-else>No properties</p>
				<edit-property-menu
					:existingProperties="entities[entity].properties"
					:entityName="entity"
					@save="btnSaveProperty(entity, $event)">
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
				<v-simple-table v-if="edges[entity] && edges[entity].length > 0">
					<thead>
						<tr>
							<th class="primary--text">Description</th>
							<th class="primary--text">To entity</th>
							<th class="primary--text">Cardinality</th>
							<th class="primary--text">Action</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="edge in edges[entity]" :key="edge.id">
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
										@save="btnSaveExistingEdge(entity, edge.id, $event)"
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
					v-model="relationshipsPopover[entity]"
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
						:visible="relationshipsPopover[entity] == true"
						:ref="'add_relationship_' + entity"
						:nodes="nodes"
						:relationship="Object.assign({from: entity})"
						:existingRelNames="edgeIds"
						adding="true"
						@save="btnSaveNewEdge(entity, $event)"
						@cancel="cancelRelationshipsPopover(entity)"
					></edit-relationship>
				</v-menu>
			</v-tab-item>
			<v-tab-item class="info-tab">
				<v-select
					:items="entities[entity].properties"
					item-text="name"
					item-value="name"
					label="Entity Identifier"
					v-model="entities[entity].idField"
					@change="btnUpdateModel(entity)"
					outlined
				></v-select>
				<v-select
					:items="entities[entity].properties"
					item-text="name"
					item-value="name"
					label="Entity Label"
					v-model="entities[entity].labelField"
					@change="btnUpdateModel(entity)"
					outlined
				></v-select>
			</v-tab-item>
		</v-tabs-items>
	</v-card>
</template>

<script>
import EditRelationship from '@/components/EditRelationship.vue';
import EditPropertyMenu from '@/components/EditPropertyMenu.vue'

export default {
	name: 'entity-card',
	props: {
		entity: {type: String},
		entities: {type: Object},
		edges: {type: Object},
		nodes: {type: Array},
		edgeIds: {type: Array},
		activeTab: {type: Number}
	},
	components: {
		EditRelationship,
		EditPropertyMenu
	},
	data() {
		return {
			relationshipsPopover: {},
			whichTab: null
		}
	},
	watch: {
		activeTab(newVal) {
			this.whichTab = newVal
		}
	},
	mounted() {
		this.whichTab = this.activeTab || 0
	},
	methods: {
		cancelRelationshipsPopover(id) {
			this.$set(this.relationshipsPopover, id, false)
		},
		btnSaveProperty(item, {oldProp, newProp}) {
			let entity = this.entities[item]
			if (oldProp) {
				entity.properties = entity.properties.filter(p => p._propId !== oldProp._propId)
			}
			entity.properties.push(newProp)
			this.btnUpdateModel(item)
		},
		btnUpdateModel() {
			this.$emit('updateModel')
		},
		btnDeleteProperties(item, propName){
			this.$emit ("deleteProperties", {item, propName})
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
	}
}

</style>
