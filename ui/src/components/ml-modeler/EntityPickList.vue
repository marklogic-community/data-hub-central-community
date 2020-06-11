<template>
	<v-layout fill-height>
		<v-flex xs12 fill-height class="relparent">
			<v-card dark class="white--text abs">
				<v-card-title primary-title>
					<div class="headline" v-if="model" data-cy="createModelVue.currentModelLabel">{{model.name}}</div>
					<v-spacer></v-spacer>
					<v-menu
						:close-on-content-click="false"
						:nudge-width="300"
						offset-x
						v-model="createModelMenu">
						<template v-slot:activator="{ on: menu }">
							<v-tooltip bottom>
								<template v-slot:activator="{ on: tooltip }">
									<v-btn
										data-cy="cardMenu.createModelButton"
										right
										icon
										small
										class="small-btn"
										v-on="{ ...tooltip, ...menu }"
									>
										<v-icon>mdi-plus</v-icon>
									</v-btn>
								</template>
								<span>Create Model</span>
							</v-tooltip>
						</template>
						<create-model
							:existingModels="models"
							@save="createModel($event)"
							@cancel="createModelMenu = false"
							></create-model>
					</v-menu>
					<v-menu
						:close-on-content-click="false"
						:nudge-width="300"
						offset-x
						v-model="loadModelsMenu"
					>
						<template v-slot:activator="{ on: menu }">
							<v-tooltip bottom>
								<template v-slot:activator="{ on: tooltip }">
									<v-btn
										data-cy="cardMenu.loadModelButton"
										right
										icon
										small
										class="small-btn"
										v-on="{ ...tooltip, ...menu }"
									>
										<v-icon>open_in_browser</v-icon>
									</v-btn>
								</template>
								<span>Load Model</span>
							</v-tooltip>
						</template>
						<load-model
							:models="models"
							@close="loadModelsMenu = false"
							></load-model>
					</v-menu>
					<v-tooltip bottom>
						<template v-slot:activator="{ on }">
							<v-btn
								data-cy="cardMenu.saveImageButton"
								right
								icon
								small
								class="small-btn"
								v-on="on"
								@click="saveImage"
							>
								<v-icon>image</v-icon>
							</v-btn>
						</template>
						<span>Save Image</span>
					</v-tooltip>
					<v-menu
						:close-on-content-click="false"
						:nudge-width="300"
						offset-x
						v-model="confirmDeleteMenu"
					>
						<template v-slot:activator="{ on: menu }">
							<v-tooltip bottom>
								<template v-slot:activator="{ on: tooltip }">
									<v-btn
										data-cy="cardMenu.deleteModelButton"
										right
										icon
										small
										class="small-btn"
										v-on="{ ...tooltip, ...menu }"
									>
										<v-icon>delete</v-icon>
									</v-btn>
								</template>
								<span>Delete Model</span>
							</v-tooltip>
						</template>
						<confirm
							message="Do you really want to delete this model?"
							confirmText="Delete"
							@confirm="deleteModel"
							@cancel="confirmDeleteMenu = false"></confirm>
					</v-menu>
				</v-card-title>
				<v-card-text>
					<div>
						<v-text-field
							class="search-box"
							clearable
							dark
							full-width
							hide-details
							label="search for entity"
							v-model="searchLabel"
							prepend-icon="search"
							single-line
						></v-text-field>
					</div>
					<v-expansion-panels ref="expansionPanel" v-model="panel" flat fill-height>
						<v-expansion-panel
							lazy
							light
							:ref="'panel_' + item"
							@change="updateNode(item)"
							:key="idx"
							v-for="(item, idx) in nodeLabels" v-show="item.toLowerCase().match((searchLabel || '').toLowerCase())"
						>
							<v-expansion-panel-header>{{ item }}</v-expansion-panel-header>
							<v-expansion-panel-content>
								<entity-card
									v-if="entities[item].type === 'entity'"
									:entity="item"
									:entities="entities"
									:edges="edges"
									:nodes="nodes"
									:edgeIds="edgeIds"
									:activeTab="activeTab"
									@updateModel="onUpdateModel"
									@addProperties="onAddProperties"
									@deleteProperties="onDeleteProperties"
									@saveEdge="onSaveEdge"
									@deleteEdge="onDeleteEdge"
								></entity-card>
								<concept-card
									v-else
									:entity="item"
									:edges="edges"
									:nodes="nodes"
									:edgeIds="edgeIds"
									@updateModel="onUpdateModel"
									@addProperties="onAddProperties"
									@deleteProperties="onDeleteProperties"
									@saveEdge="onSaveEdge"
									@deleteEdge="onDeleteEdge"
								></concept-card>
							</v-expansion-panel-content>
						</v-expansion-panel>
					</v-expansion-panels>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>

				</v-card-actions>
			</v-card>
		</v-flex>
	</v-layout>
</template>

<script>
import CreateModel from '@/components/CreateModel.vue';
import LoadModel from '@/components/LoadModel.vue';
import Confirm from '@/components/Confirm.vue';
import EntityCard from '@/components/ml-modeler/EntityCard.vue';
import ConceptCard from '@/components/ml-modeler/ConceptCard.vue';
import uuidv4 from 'uuid/v4';
import { mapState } from 'vuex'

export default {
	name: 'entity-pick-list',
	props: {
		// all these are passed in from the parent vue component
		nodesCache: {type: Object}, //nodes and edges managed by parent container
		currentNode: {type: String}, //outside changes to the currently selected node
		edgesCache: {type: Object},
		currentEdge: {type: String}, //outside changes to the currently selected edge
	},
	components: {
		CreateModel,
		LoadModel,
		Confirm,
		EntityCard,
		ConceptCard
	},
	data() {
		return {
			createModelMenu: null,
			loadModelsMenu: null,
			confirmDeleteMenu: null,
			panel: null,
			activeTab: null,
			searchLabel: ''
		}
	},
	computed: {
		...mapState({
			model: state => state.model.model,
			models: state => state.model.models
		}),
		nodeLabels() {
			return Object.values(this.nodesCache)
				.map(n => n.entityName).sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()))
		},
		edgeIds() {
			return Object.values(this.edgesCache).map(e => e.id);
		},
		nodes() {
			return Object.values(this.nodesCache);
		},
		entities() {
			return this.nodeLabels.reduce((prev, cur) => {
				let entity = this.nodesCache[cur.toLowerCase()];
				entity.properties = entity.properties
					.map(p => {
						return {
							...p,
							_propId: p._propId || uuidv4(),
						};
					})
					.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
				prev[cur] = entity;
				return prev;
			}, {});
		},
		entity() {
			return this.nodesCache[this.currentNode] || null;
		},
		edge() {
			return this.edgesCache[this.currentEdge] || null;
		},
		edges() {
			return this.nodeLabels.reduce((prev, cur) => {
				prev[cur] = Object.values(this.edgesCache)
					.filter(e => e.from.toLowerCase() === cur.toLowerCase())
					.sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase()));
				return prev;
			}, {})
		},
	},
	watch: {
		panel(newVal) {
			let nodeLabel = this.nodeLabels[newVal]
			if (!nodeLabel) {
				return
			}
			// wait for the panel to finish opening before scrolling it into view
			setTimeout(() => {
				let panelRef = this.$refs['panel_' + nodeLabel]
				if (panelRef && panelRef[0] && panelRef[0].$el) {
					let offset = panelRef[0].$el.offsetTop;
					this.$refs.expansionPanel.$el.scrollTop = offset
				}
			}, 750);
		},
		entity(newVal) {
			//currentNode changed by parent
			if (newVal) {
				this.currentItem = newVal.entityName;
				//turn on the correct button
				this.panel = this.nodeLabels.findIndex(v => v == this.currentItem)
				this.activeTab = 0;
			}
		},
		edge(newVal) {
			if (newVal) {
				this.panel = this.nodeLabels.findIndex(v => v.toLowerCase() == newVal.from)
				this.activeTab = 1;
			}
		}
	},
	methods: {
		updateNode(item) {
			let entity = this.entities[item];
			if (entity) {
				this.$emit('selectedNode', entity);
			}
		},
		createModel(modelName) {
			this.$store.dispatch('model/save', {
				name: modelName,
				edges: {},
				nodes: {}
			})
			this.createModelMenu = false;
		},
		saveImage() {
			this.$emit('doAction', 'saveGraphImage');
		},
		deleteModel() {
			this.$emit('doAction', 'deleteModel')
			this.confirmDeleteMenu = false
		},
		onDeleteEdge(edge) {
			this.$emit('doAction', 'deleteEdge', {edge})
		},
		onSaveEdge({relInfo}) {
			this.$emit ("doAction", 'saveEdge', {
				id: relInfo.id,
				from: relInfo.from,
				label: relInfo.label,
				to: relInfo.to,
				cardinality: relInfo.cardinality,
				keyFrom: relInfo.keyFrom,
				keyTo: relInfo.keyTo
			})
		},
		onUpdateModel() {
			this.$emit ("doAction", 'saveToML')
		},
		onAddProperties({item, propInfo}) {
			let entity = this.entities[item]
			entity.properties.push(propInfo)

			this.$emit ("doAction", 'updateNodes', { nodesCache: this.nodesCache })
			this.$emit ("doAction", 'saveToML')
		},
		onDeleteProperties({item, propName}){
			let entity = this.entities[item]
			entity.properties = entity.properties.filter(p => p.name !== propName)
			this.$emit ("doAction", 'updateNodes', { nodesCache: this.nodesCache })
			this.$emit ("doAction", 'saveToML')
		}
	} //end of methods
} //end of export
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
	background-color:	#E1E1EF;
}

.small-btn {
	width: 30px;
	height: 30px;
}

.abs {
	position: absolute;
	top: 0;
	right: 0;
	left: 0;
	bottom: 0;
	display: flex;
	flex-direction: column;
}

.v-card__text {
	overflow-y: auto;
	display: flex;
	flex-direction: column;
	flex: 1;
}

.relparent {
	position: relative;
}

.v-expansion-panels {
	display: flex;
	justify-content: start;
}
</style>
