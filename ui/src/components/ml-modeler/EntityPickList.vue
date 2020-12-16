<template>
	<v-layout fill-height>
		<v-flex xs12 fill-height class="relparent">
			<v-card dark class="white--text abs">
				<v-card-title primary-title>
					<div
						v-if="model"
						class="headline"
						data-cy="createModelVue.currentModelLabel">{{model.name}}</div>
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
										v-on="{ ...tooltip, ...menu }">
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
						v-model="renameModelMenu">
						<template v-slot:activator="{ on: menu }">
							<v-tooltip bottom>
								<template v-slot:activator="{ on: tooltip }">
									<v-btn
										data-cy="cardMenu.renameModelButton"
										:disabled="!model"
										right
										icon
										small
										class="small-btn"
										v-on="{ ...tooltip, ...menu }">
										<v-icon>mdi-rename-box</v-icon>
									</v-btn>
								</template>
								<span>Rename Model</span>
							</v-tooltip>
						</template>
						<rename-model
							:existingModels="models"
							@rename="renameModel"
							@cancel="renameModelMenu = false"
							></rename-model>
					</v-menu>

					<v-menu
						:close-on-content-click="false"
						:nudge-width="300"
						offset-x
						v-model="loadModelsMenu">
						<template v-slot:activator="{ on: menu }">
							<v-tooltip bottom>
								<template v-slot:activator="{ on: tooltip }">
									<v-btn
										data-cy="cardMenu.loadModelButton"
										right
										icon
										small
										class="small-btn"
										v-on="{ ...tooltip, ...menu }">
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
								:disabled="!model"
								right
								icon
								small
								class="small-btn"
								v-on="on"
								@click="saveImage">
								<v-icon>image</v-icon>
							</v-btn>
						</template>
						<span>Save Image</span>
					</v-tooltip>
					<v-menu
						:close-on-content-click="false"
						:nudge-width="300"
						offset-x
						v-model="confirmDeleteMenu">
						<template v-slot:activator="{ on: menu }">
							<v-tooltip bottom>
								<template v-slot:activator="{ on: tooltip }">
									<v-btn
										data-cy="cardMenu.deleteModelButton"
										:disabled="!model"
										right
										icon
										small
										class="small-btn"
										v-on="{ ...tooltip, ...menu }">
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
					<div v-if="model">
						<v-text-field
							data-cy="modeler.entityFilter"
							class="search-box"
							clearable
							dark
							full-width
							hide-details
							label="search for entity"
							v-model="searchLabel"
							prepend-icon="search"
							single-line></v-text-field>
					</div>
					<h2 class="text-center" v-else>Please Create or Load a Model to begin</h2>
					<v-expansion-panels ref="expansionPanel" v-model="currentPanel" flat fill-height>
						<v-expansion-panel
							lazy
							light
							@change="updateNode(entity)"
							:ref="'panel_' + entity.id"
							:key="idx"
							:data-cy="`panel-${entity.id}`"
							v-for="(entity, idx) in sortedEntities"
							v-show="entity.label.toLowerCase().match((searchLabel || '').toLowerCase())">
							<v-expansion-panel-header>{{ entity.label }}</v-expansion-panel-header>
							<v-expansion-panel-content>
								<entity-card
									v-if="entity.type === 'entity'"
									:entity="entity"
									:entities="entities"
									:edges="entityEdges(entity)"
									:nodes="nodes"
									:edgeIds="edgeIds"
									:activeTab="activeTab"
									@updated="onUpdateModel"
									@deleteProperties="onDeleteProperties"
									@saveEdge="onSaveEdge"
									@deleteEdge="onDeleteEdge"></entity-card>
								<concept-card
									v-else
									:entity="entity"
									:edges="entityEdges(entity)"
									:nodes="nodes"
									:edgeIds="edgeIds"
									@updated="onUpdateModel"
									@deleteProperties="onDeleteProperties"
									@saveEdge="onSaveEdge"
									@deleteEdge="onDeleteEdge"
								></concept-card>
							</v-expansion-panel-content>
						</v-expansion-panel>
					</v-expansion-panels>
				</v-card-text>
			</v-card>
		</v-flex>
	</v-layout>
</template>

<script>
import CreateModel from '@/components/CreateModel.vue'
import RenameModel from '@/components/RenameModel.vue'
import LoadModel from '@/components/LoadModel.vue'
import Confirm from '@/components/Confirm.vue'
import EntityCard from '@/components/ml-modeler/EntityCard.vue'
import ConceptCard from '@/components/ml-modeler/ConceptCard.vue'
import { mapState } from 'vuex'

export default {
	name: 'entity-pick-list',
	props: {
		nodes: { type: Array },
		edges: {type: Array},
		entity: {type: Object},
		currentEdge: {type: String}
	},
	components: {
		CreateModel,
		RenameModel,
		LoadModel,
		Confirm,
		EntityCard,
		ConceptCard
	},
	data() {
		return {
			createModelMenu: null,
			renameModelMenu: null,
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
		currentPanel: {
			get() {
				return this.panel
			},
			set(val) {
				this.panel = val
				if (this.panel) {
					const entity = this.sortedEntities[val]
					if (!entity) {
						return
					}
					setTimeout(() => {
						let panelRef = this.$refs['panel_' + entity.id]
						if (panelRef && panelRef[0] && panelRef[0].$el) {
							let offset = panelRef[0].$el.offsetTop
							this.$refs.expansionPanel.$el.scrollTop = offset
						}
					}, 750)
				}
			}
		},
		edgeIds() {
			return this.edges.map(e => e.id)
		},
		sortedEntities() {
			return this.nodes.slice()
				.sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase()))
		},
		entities() {
			return this.nodes.reduce((output, entity) => {
				output[entity.entityName] = entity
				return output
			}, {})
		},
		edge() {
			return this.currentEdge ? this.edges.find(e => e.id === this.currentEdge): null
		},
	},
	watch: {
		entity(newVal) {
			if (newVal) {
				this.currentItem = newVal.entityName
				this.currentPanel = this.sortedEntities.findIndex(v => v.label == this.currentItem)
				this.activeTab = 0
			}
		},
		edge(newVal) {
			if (newVal) {
				this.currentPanel = this.sortedEntities.findIndex(v => v.label.toLowerCase() == newVal.from)
				this.activeTab = 1
			}
		}
	},
	methods: {
		entityEdges(entity) {
			return this.edges.filter(e => e.from.toLowerCase() === entity.id.toLowerCase())
		},
		updateNode(entity) {
			this.$emit('selectedNode', entity)
		},
		createModel(modelName) {
			this.$store.dispatch('model/save', {
				name: modelName,
				edges: {},
				nodes: {}
			})
			this.createModelMenu = false
		},
		renameModel(newModelName) {
			this.$store.dispatch('model/rename', {
				originalname: this.model.name,
				newname: newModelName,
				model: this.model
			})
			this.renameModelMenu = false
		},
		saveImage() {
			this.$emit('saveGraphImage')
		},
		deleteModel() {
			this.$emit('deleteModel')
			this.confirmDeleteMenu = false
		},
		onDeleteEdge(edge) {
			this.$emit('deleteEdge', edge)
		},
		onSaveEdge({relInfo}) {
			this.$emit ('saveEdge', {
				id: relInfo.id,
				from: relInfo.from,
				label: relInfo.label,
				to: relInfo.to,
				cardinality: relInfo.cardinality,
				keyFrom: relInfo.keyFrom,
				keyTo: relInfo.keyTo
			})
		},
		onUpdateModel(entity) {
			this.$set(this.entities, entity.entityName, entity)
			this.$emit ('updateEntity', entity)
		},
		onDeleteProperties({entity, propName}){
			entity.properties = entity.properties.filter(p => p.name !== propName)
			this.$emit ('updateEntity', entity)
		}
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
	justify-content: flex-start;
}
</style>
