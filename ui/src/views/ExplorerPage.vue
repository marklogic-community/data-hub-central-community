<template>
	<v-container fluid>
		<v-layout column>
			<v-layout row class="searchArea">
				<v-flex md10>
					<form class="ml-input ml-search form-inline" role="search" v-on:submit.prevent="searchText">
						<v-text-field
							hide-details
							v-model="qtext"
							placeholder="Search"
							prepend-icon="search"
							single-line
							clearable
							data-cy="searchInput"
							@click:clear="clearSearch"
						></v-text-field>
					</form>
				</v-flex>
				<v-flex md2>
					<v-select
						v-model="sort"
						:items="sortOptions"
						item-text="label"
						item-value="value"
						label="Sort">
					</v-select>
				</v-flex>
			</v-layout>
			<v-flex md12>
				<v-layout row class="fullHeight">
					<v-flex md8 class="graph-container">
						<div class="pagination-wrapper">
							<span v-if="searchPending">
								Searching...
							</span>
							<span v-if="!searchPending && totalResults > 0">
							Showing results {{start}} to {{end}} of {{totalResults}}
							</span>
							<span v-if="!searchPending && totalResults <= 0">
								No Results found
							</span>
							<span v-if="currentPage > 1" class="pagination"><a @click="previousPage">&lt;&lt; previous</a></span>
							<span v-if="currentPage < lastPage" class="pagination"><a @click="nextPage">next &gt;&gt;</a></span>
						</div>
						<visjs-graph
							class="graph-wrapper"
							:nodes="nodes"
							:edges="edges"
							:options="graphOptions"
							layout="standard"
							:events="graphEvents"
							ref="graph"
						></visjs-graph>
						<ul class="hideUnlessTesting">
							<li v-for="node in nodes" :key="node.id" data-cy="nodeList" v-on:click="selectNode(node)">{{ node.id }}</li>
						</ul>
					</v-flex>
					<v-flex md4 class="right-pane relparent">
						<v-select
							v-model="selectedEntities"
							:items="entitiesArray"
							item-text="label"
							item-value="label"
							multiple
							label="Show Entities"
						>
							<template v-slot:selection="{ item }">
								<v-chip :color="item.bgColor" :style="{border: '2px dashed', borderColor: item.borderColor}">{{item.label}}</v-chip>
							</template>
						</v-select>
						<entity-details
							v-if="currentNode && !currentNode.isConcept"
							:entity="currentNode"
							v-on:expandRelationship="expandRelationship"
							v-on:unmerge="unmerge"></entity-details>
						<v-card
							v-if="currentNode && currentNode.isConcept">
							<v-card-text>
								<h2>{{currentNode.entityName}}:</h2>
								<div>{{currentNode.label}}</div>
							</v-card-text>
						</v-card>
					</v-flex>
				</v-layout>
			</v-flex>
		</v-layout>
		<v-menu
			v-model="rightClickMenu"
			absolute
			:position-x="rightClickPos.x"
			:position-y="rightClickPos.y"
		>
			<v-list>
				<v-list-item
					v-for="item in rightClickItems"
					:key="item.label"
					@click="contextClick(item)">
					<v-list-item-title>{{item.label}}</v-list-item-title>
				</v-list-item>
			</v-list>
		</v-menu>
		<v-menu
			absolute
			:position-x="rightClickPos.x"
			:position-y="rightClickPos.y"
			:close-on-content-click="false"
			:nudge-width="300"
			offset-x
			v-model="confirmUnmergeMenu"
		>
			<confirm
				message="Do you really want to unmerge this entity?"
				confirmText="Unmerge"
				@confirm="unmerge(currentNode.uri)"
				@cancel="confirmUnmergeMenu = false"></confirm>
		</v-menu>
	</v-container>
</template>

<script>

import VisjsGraph from 'grove-vue-visjs-graph';
import 'vis/dist/vis.css';
import 'ml-visjs-graph/less/ml-visjs-graph.js.less';
import crudApi from '@/api/CRUDApi.js';
import EntityDetails from '@/components/ml-explorer/EntityDetails';
import Confirm from '@/components/Confirm.vue';
import { mapState } from 'vuex'
import _ from 'lodash';
import ColorScheme from 'color-scheme';

export default {
	name: 'ExplorePage',
	data: function() {
		var locales = {
			en: {
				edit: 'Edit',
				del: 'Delete selected',
				back: 'Back',
				addNode: 'Add Entity',
				addEdge: 'Add Relationship',
				editNode: 'Edit Entity',
				editEdge: 'Edit Relationship',
				addDescription: 'Click in an empty space to place a new entity.',
				edgeDescription:
					'Click on an entity and drag the relationship to another entity to connect them.',
				editEdgeDescription:
					'Click on the control points and drag them to a entity to connect to it.',
				createEdgeError: 'Cannot link entities to a cluster.',
				deleteClusterError: 'Clusters cannot be deleted.',
				editClusterError: 'Clusters cannot be edited.'
			}
		};

		return {
			concepts: [],
			searchPending: false,
			rightClickMenu: null,
			confirmUnmergeMenu: null,
			rightClickPos: { x: 0, y: 0 },
			rightClickItems: [],
			currentNode: null,
			title: 'Explore',
			entities: {},
			colors: {},
			qtext: '',
			sortOptions: [
				{
					label: 'Default',
					value: 'default'
				},
				{
					label: 'Most Connected First',
					value: 'mostConnected'
				},
				{
					label: 'Least Connected First',
					value: 'leastConnected'
				}
			],
			graphOptions: {
				layout: {
					improvedLayout: true,
				},
				autoResize: false,
				height: '100%',
				locale: 'en',
				locales: locales,
				nodes: {
					shape: 'circle',
					margin: 10,
					font: {
						color: '#000' // MarkLogic teal-accent-311
					},
					borderWidth: 1,
					color: {
						highlight: {
							border: '#999'
						}
					},
					widthConstraint: {
						maximum: 100
					}
				},
				edges: {
					width: 2,
					arrows: {
						to: {
							enabled: true
						},
						middle: {
							enabled: false
						},
						from: {
							enabled: false
						}
					},
					color: {
						color: '#ccc'
					},
					smooth: {
						roundness: 0.1 // initial
					}
				},
				physics: {
					enabled: true,
					stabilization: { enabled: true }
				},

				manipulation: {
					enabled: false, // true = use the edit feature
					initiallyActive: false,
				}
			},
			graphEvents: {
				click: this.onGraphClick,
				dragStart: this.onGraphClick,
				oncontext: this.graphRightClick
			}
		};
	},
	components: {
		VisjsGraph,
		EntityDetails,
		Confirm
	},
	computed: {
		...mapState({
			edgeMap: state => state.explore.edges,
			nodeMap: state => state.explore.nodes,
			storeEntities: state => state.explore.entities,
			queryText: state => state.explore.qtext,
			currentPage: state => state.explore.page,
			lastPage: state => Math.ceil(state.explore.total / state.explore.pageLength),
			start: state => ((state.explore.page - 1) * state.explore.pageLength) + 1,
			total: state => state.explore.total,
			pageLength: state => state.explore.pageLength,
			storeSort: state => state.explore.sort,
			model: state => state.model.model
		}),
		sort: {
			get() {
				return this.storeSort
			},
			set(value) {
				this.$store.commit('explore/setSort', value || 'default')
				this.$store.commit('explore/setPage', 1)
				this.getEntities()
			}
		},
		selectedEntities: {
			get() {
				return this.storeEntities
			},
			set(value) {
				this.$store.commit('explore/setEntities', value || [])
				this.getEntities()
			}
		},
		totalResults() {
			return Math.max(this.total, this.end)
		},
		end() {
			return this.start + this.nodes.length - 1
		},
		entitiesArray() {
			const ents = Object.values(this.entities).map(e => {
				return {
					...e,
					borderColor: this.colors[e.id].border,
					bgColor: this.colors[e.id].background
				}
			}).sort((a, b) => {
				if (a.type === b.type) {
					return (a.id < b.id ) ? -1 : 1
				}
				return (a.type < b.type) ? 1 : -1
			})
			return ents
		},
		nodes() {
			return Object.values(this.nodeMap || {}).map(node => {
				let color = this.colors[node.entityName.toLowerCase()]
				let extras = {}
				if (node.isConcept) {
					extras = {
						shapeProperties: {
							borderDashes: [4,3]
						},
						borderWidth: 3
					}
				}
				return {
					id: node.id,
					label: node.label,
					color: color,
					...extras
				}
			})
		},
		isLoggedIn() {
			return this.$store.state.auth.authenticated;
		},
		edges() {
			return Object.values(this.edgeMap || {})
		}
	},
	watch: {
		model(newValue, oldValue) {
			let oldName = (oldValue && oldValue.name) || null;
			if (newValue && newValue.name !== oldName) {
				this.handleModelChange()
			}
		}
	},
	mounted: function() {
		// work around a bug in visjs where it resizes a lot in firefox
		this.$refs.graph.graph.network.network.canvas._cleanUp()

		this.handleModelChange();
	},
	methods: {
		selectNode( selectedNode ) {
			// called bu Cypress to select a node, as couldn't find how to make it click the graph directly
			this.currentNode = this.nodeMap[selectedNode.id]
		},
		handleModelChange() {
			if (!this.model) {
				return;
			}
			let graph = JSON.parse(JSON.stringify(this.model));

			Object.keys(graph.edges).forEach(key => {
				let edge = graph.edges[key];
				// TODO: only adjust roundness (with +/- 0.1) if you encounter same from/to more than once
				edge.smooth = { roundness: Math.random() - 0.5 };
			});

			this.entities = graph.nodes;
			const scheme = new ColorScheme();
			scheme.from_hue(0)
				.scheme('triade')
				.distance(1)
				.add_complement(false)
				.variation('soft');
			const availableColors = scheme.colors();
			let colors = {};
			let currentColor = 0;
			for (let key in this.entities) {
				let entity = this.entities[key];
				const c = `#${availableColors[currentColor]}`
				if (entity.type === 'entity') {
					colors[entity.id] = {
						background: c,
						highlight: {
							background: c
						},
						border: c,
					};
				}
				else {
					colors[entity.id] = {
						background: '#fff',
						border: '#3cdbc0'
					};
				}
				currentColor = (currentColor + 3) % availableColors.length;
			}
			this.colors = colors;
			let selectedEntities = Object.values(this.entities).sort((a, b) => {
				if (a.type === b.type) {
					return (a.id < b.id ) ? -1 : 1
				}
				return (a.type < b.type) ? 1 : -1
			})
			.map(entity => entity.entityName)
			this.selectedEntities = selectedEntities
		},
		isChecked(entity) {
			let checked = this.storeEntities.indexOf(entity.id) >= 0
			return checked
		},
		toggleEntity(entity) {
			if (this.isChecked(entity)) {
				this.$store.commit('explore/removeEntity', { entity: entity.id })
			} else {
				this.$store.commit('explore/addEntity', { entity: entity.id })
			}
			this.getEntities()
		},
		searchText() {
			this.currentNode = null
			this.$store.commit('explore/setText', { qtext: this.qtext })
			this.$store.commit('explore/setPage', 1)
			this.getEntities()
		},
		clearSearch() {
			this.qtext = null
			this.searchText()
		},
		selectEntity(name) {
			this.getEntities()
		},
		previousPage() {
			this.$store.commit('explore/setPage', this.currentPage - 1)
			this.getEntities()
		},
		nextPage() {
			this.$store.commit('explore/setPage', this.currentPage + 1)
			this.getEntities()
		},
		getEntities() {
			this.searchPending = true
			this.$store
				.dispatch('explore/search')
				.then(() => {
					this.searchPending = false
				}).finally(() => {
					this.searchPending = false
				});
		},
		onGraphClick(e) {
			let nodeId = e.nodes[0];
			let edgeId = e.edges[0];

			if (nodeId) {
				let node = this.nodeMap[nodeId]
				if (node) {
					this.currentNode = node
				}
				else {
					let concept = this.concepts.find(c => c.id === nodeId)
					if (concept) {
						this.currentNode = concept
					}
				}
			} else if (edgeId) {
				let edge = this.edgeMap[edgeId]
			} else {
				this.currentNode = null
			}
		},
		graphRightClick(e) {
			e.event.preventDefault()
			this.rightClickMenu = false

			let network = this.$refs.graph.graph.network.network
			let nodeId = network.getNodeAt(e.pointer.DOM)
			if (nodeId) {
				let node = this.nodeMap[nodeId]
				if (node) {
					this.currentNode = node
					this.rightClickPos = {
						x: e.event.x,
						y: e.event.y
					}

					let items = _.sortBy(Object.values(node.edgeCounts), 'label').map(e => {
						return {
							label: `Expand ${e.label} (${e.count})`,
							action: 'expand',
							rel: {
								uri: this.currentNode.id,
								label: e.label
							}
						}
					})

					if (node.uri && node.uri.match('/com.marklogic.smart-mastering/merged/')) {
						items.push({
							label: 'UnMerge',
							action: 'unmerge',
							node: node
						})
					}
					if (items.length > 0) {
						this.rightClickItems = items

						this.$nextTick(() => {
							this.rightClickMenu = true
						})
					}
				}
			}
		},
		contextClick(item) {
			switch(item.action) {
				case 'expand':
					this.expandRelationship(item.rel)
					break
				case 'unmerge':
					this.confirmUnmergeMenu = true
					break;
			}
		},
		expandRelationship({ uri, label }) {
			// TODO: move this to store
			const page = 1
			const pageLength = 10
			this.$store.dispatch('explore/getRelatedEntities', { uri, label, page, pageLength })
		},
		unmerge(uri) {
			this.$store.dispatch('explore/unmerge', uri)
			this.currentNode = null
			this.confirmUnmergeMenu = false
		}
	}
};
</script>

<style lang="less">
.graph-controls {
	display: none !important;
}

.graph-container {
	padding: 10px;
}

.pagination {
	margin: 0px 5px;
}

table {
	width: 100%;

	th {
		text-align: left;
		font-size: 14px;
		padding: 5px;
	}

	td {
		padding: 2px;
	}
}

.right-pane {
	padding: 20px;
	margin-top: 0px;

	.v-input--selection-controls {
		margin: 0px;
		padding-top: 0px;
	}
}

.ml-search .v-input {
	margin-bottom: 20px;
}

.hideUnlessTesting {
	visibility: hidden;
	position: absolute;
}

.container--fluid {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
}

.layout.column,
.fullHeight,
.mlvisjs-graph {
	height: 100%
}

.graph-container {
	display: flex;
	flex-direction: column;
}

.graph-wrapper {
	display: flex;
	flex: 1;
}

.relparent {
	position: relative;
}

.searchArea {
	height: auto !important;
}
</style>
