<template>
	<v-card>
		<v-tabs
			light
			v-model="activeTab">
			<v-tab class="tab" ripple>Relationships</v-tab>
		</v-tabs>
		<v-tabs-items v-model="activeTab">
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
									v-model="editRelationshipsPopover[edge.id]"
									offset-x
								>
									<template v-slot:activator="{ on }">
										<button v-on="on" class="fa fa-pencil"
										aria-label="Edit relationship" />
									</template>
									<edit-relationship
										:ref="'edit_relationship_' + entity.label"
										:nodes="entities"
										:relationship="Object.assign({}, edge)"
										@save="btnSaveEdge(entity, $event)"
										@cancel="editRelationshipsPopover[edge.id] = false"
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
					v-model="relationshipsPopover[entity.label]"
					offset-x>
					<template v-slot:activator="{ on }">
						<v-btn
							color="primary"
							fab
							dark
							v-on="on">
							<v-icon dark>add</v-icon>
						</v-btn>
					</template>
					<edit-relationship
						:ref="'add_relationship_' + entity.label"
						:nodes="entities"
						:relationship="Object.assign({from: entity.id})"
						:existingRelNames="edgeIds"
						adding="true"
						@save="btnSaveEdge(entity, $event)"
						@cancel="relationshipsPopover[entity.label] = false"
					></edit-relationship>
				</v-menu>
			</v-tab-item>
		</v-tabs-items>
	</v-card>
</template>

<script>
import EditRelationship from '@/components/EditRelationship.vue';

export default {
	props: {
		entity: {type: Object},
		edges: {type: Array},
		entities: {type: Array},
		edgeIds: {type: Array}
	},
	components: {
		EditRelationship
	},
	data() {
		return {
			activeTab: null,
			propertiesPopover: {},
			editPropertiesPopover: {},
			relationshipsPopover: {},
			editRelationshipsPopover: {},
		}
	},
	computed: {},
	methods: {
		btnSaveEdge(item, relInfo) {
			this.relationshipsPopover[item] = false
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

</style>
