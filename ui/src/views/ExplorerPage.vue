<template>
	<v-container fluid class="ExplorerPage">
		<v-layout column>
			<v-layout row class="searchArea">
				<v-flex md8>
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
				<v-flex md4>
					<v-flex d-flex flex-row>
						<v-flex md8 d-flex align-center>
							<nested-menu class="sortMenu" :name="currentSort.preview"  :menuItems="sortOptions" @selected="updateSort" />
						</v-flex>
						<v-flex md4>
							<v-select
								:items="databases"
								data-cy="explore.database"
								item-text="label"
								item-value="value"
								label="Database"
								v-model="currentDatabase"
								:menu-props="{ 'content-class': 'databaseArray'}"
							></v-select>
						</v-flex>
					</v-flex>
				</v-flex>
			</v-layout>
			<v-flex md12>
				<v-layout row class="fullHeight">
					<v-flex :class="['graph-container', (currentNode  && showDetailPane) ? 'md8' : 'md12']">
						<div row>
							<v-tabs v-if="isFinalDb" v-model="tab" @change="updateRoute" hide-slider>
								<v-tab data-cy="tabGraph"><v-icon>bubble_chart</v-icon>Graph</v-tab>
								<v-tab data-cy="tabGrid"><v-icon>reorder</v-icon>Grid</v-tab>
							</v-tabs>
							<div class="pagination-wrapper">
								<span v-if="searchPending">
									Searching...
								</span>
								<span v-if="!searchPending && totalResults > 0">
								Showing results {{pageStart}} to {{pageEnd}} of {{totalResults}}
								</span>
								<span v-if="currentPage > 1" class="pagination"><a @click="previousPage">&lt;&lt; previous</a></span>
								<span v-if="currentPage < lastPage" class="pagination"><a @click="nextPage">next &gt;&gt;</a></span>
							</div>
						</div>

						<v-container class="search-area">
							<v-flex md3 class="scrolly">
								<ml-facets v-if="facets" :facets="facets" :toggle="toggleFacet" :active-facets="activeFacets" :negate="toggleNegatedFacet" :showMore="showMore"></ml-facets>
							</v-flex>
							<v-flex md9 column class="scrolly graph-parent">
								<v-flex class="no-results" md12 v-if="!searchPending && totalResults <= 0">
									<h1><v-icon>fa-frown-o</v-icon> No Results found <v-icon>fa-frown-o</v-icon></h1>
								</v-flex>
								<template v-else>
									<visjs-graph
										v-show="tab === 0 && isFinalDb"
										class="graph-wrapper"
										:nodes="nodes"
										:edges="edges"
										:options="graphOptions"
										layout="standard"
										:events="graphEvents"
										ref="graph"
									>
									</visjs-graph>
									<div v-show="tab === 0 && isFinalDb" class="text-center">
										<v-chip v-for="(item, index) in entitiesArray" :key="index" :color="item.bgColor" :style="{border: '2px dashed', borderColor: item.borderColor}" disabled>{{item.label}}</v-chip>
									</div>
									<ml-results v-show="tab == 1 || !isFinalDb" :results="results" :currentNode="currentNode" :colors="colors" @select="clickedResult"></ml-results>
								</template>
							</v-flex>
						</v-container>

						<ul class="hideUnlessTesting">
							<li v-for="node in nodes" :key="node.id" data-cy="nodeList" v-on:click="selectNode(node)">{{ node.id }}</li>
						</ul>
					</v-flex>
					<v-flex :class="['right-pane', (currentNode && showDetailPane)? 'md4' : 'nowidth']">
						<entity-details
							v-if="currentNode && !currentNode.isConcept"
							:entity="currentNode"
							v-on:expandRelationship="expandRelationship"
							v-on:unmerge="unmerge"
							v-on:hideDetails="showDetailPane = false"
							></entity-details>
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
				<template v-for="item in rightClickItems">
					<v-divider :key="item.label" v-if="item.divider"></v-divider>
					<v-list-item
						v-else
						:key="item.label"
						@click="contextClick(item)">
						<v-list-item-title>{{item.label}}</v-list-item-title>
					</v-list-item>
				</template>
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

import 'vis/dist/vis.css';
import 'ml-visjs-graph/less/ml-visjs-graph.js.less';
import { mapState } from 'vuex'
import _ from 'lodash';
import ColorScheme from 'color-scheme';
import NestedMenu from '@/components/NestedMenu';
import VisjsGraph from 'grove-vue-visjs-graph';
import EntityDetails from '@/components/ml-explorer/EntityDetails';
import Confirm from '@/components/Confirm.vue';
import mlFacets from '@/components/ml-search/ml-facets.vue';
import mlResults from '@/components/ml-search/ml-results.vue';

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
			tabPrivate: 0,
			databases: [
				{
					label: 'Final',
					value: 'final'
				},
				{
					label: 'Staging',
					value: 'staging'
				}
			],
			currentSort: {
				preview: 'Default'
			},
			showSortMenu: false,
			concepts: [],
			searchPending: false,
			rightClickMenu: null,
			confirmUnmergeMenu: null,
			rightClickPos: { x: 0, y: 0 },
			rightClickItems: [],
			currentNode: null,
			showDetailPane: false,
			title: 'Explore',
			entities: {},
			colors: {},
			defaultSortOptions: [
				{
					label: 'Default',
					preview: 'Default',
					value: 'default',
					enabled: true
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
				dragEnd: this.onGraphDrag,
				oncontext: this.graphRightClick
			}
		};
	},
	components: {
		VisjsGraph,
		EntityDetails,
		Confirm,
		NestedMenu,
		mlFacets,
		mlResults
	},
	computed: {
		...mapState({
			edgeMap: state => state.explore.edges,
			nodeMap: state => state.explore.nodes,
			storeEntities: state => state.explore.entities,
			queryText: state => state.explore.qtext,
			currentPage: state => state.explore.page,
			lastPage: state => Math.ceil(state.explore.total / state.explore.pageLength),
			page: state => state.explore.page,
			start: state => ((state.explore.page - 1) * state.explore.pageLength) + 1,
			total: state => state.explore.total,
			pageLength: state => state.explore.pageLength,
			storeSort: state => state.explore.sort,
			model: state => state.model.model,
			activeIndexes: state => state.model.activeIndexes,
			facets: state => state.explore.facets,
			activeFacets: state => state.explore.activeFacets,
			results: state => state.explore.results,
			database: state => state.explore.database
		}),
		isFinalDb() {
			return this.currentDatabase === 'final'
		},
		tab: {
			get() {
				return this.tabPrivate
			},
			set(val) {
				this.tabPrivate = val
				this.currentNode = null
			}
		},
		currentDatabase: {
			get() {
				return this.database
			},
			set(val) {
				if (this.database !== val) {
					this.currentNode = null
					this.$store.commit('explore/clearActiveFacets')
					this.$store.commit('explore/setDatabase', val)
					this.updateRoute()
				}
			}
		},
		routeQuery() {
			let q = {
				tab: this.tab,
			}
			if (this.qtext) {
				q.q = this.qtext
			}
			if (this.currentPage) {
				q.page = this.currentPage
			}
			if (this.currentDatabase) {
				q.db = this.currentDatabase
			}
			return q
		},
		qtext: {
			get() {
				return this.queryText
			},
			set(val) {
				this.$store.commit('explore/setText', { qtext: val })
			}
		},
		pageStart() {
			return parseInt( (this.page - 1) * this.pageLength + 1 );
    },
		pageEnd() {
			return Math.min(this.pageStart + this.pageLength - 1, this.total);
    },
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
		entitySortOptions() {
			if (!(this.model && this.model.nodes)) {
				return []
			}
			const entities = Object.values(this.model.nodes)
				.filter(n => n.type === 'entity')
				.filter(n => n.properties.find(p => p.isElementRangeIndex === true))
			let options = []
			entities.forEach(e => {
				e.properties
					.filter(p => p.isElementRangeIndex === true)
					.forEach(p => {
						const enabled = this.activeIndexes.indexOf(p.name) >= 0
						options.push({
							label: `${e.entityName}.${p.name}`,
							enabled,
							items: ['Ascending', 'Descending'].map(sort => {
								return {
									label: sort,
									preview: `${e.entityName}.${p.name} (${sort})`,
									value: {
										entity: e.entityName,
										property: p.name,
										sortDirection: sort.toLowerCase()
									},
									enabled
								}
							})
						})
					})
			})

			return options
		},
		sortOptions() {
			const items = this.entitySortOptions
			if (items.length > 0) {
				return this.defaultSortOptions.concat([{
					label: 'Advanced',
					enabled: true,
					items
				}])
			}
			return this.defaultSortOptions
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
			return parseInt (this.start + this.nodes.length - 1)
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
			const values = Object.values(this.nodeMap || {})
			if (values.length > 0) {
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
			}
			return []
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
		},
		'$route.query'(val, oldVal) {
			let changed = false
			if (val.page != oldVal.page) {
				changed = true
				this.$store.commit('explore/setPage', val.page ? parseInt(val.page) : 1)
			}

			if (val.q != oldVal.q) {
				changed = true
				this.qtext = val.q || null
			}

			if (val.db != oldVal.db) {
				changed = true
				this.currentDatabase = val.db
			}

			if (changed) {
				this.getEntities()
			}
		},
		'$route.query.tab'(val, oldVal) {
			if (val !== oldVal) {
				this.tab = val ? parseInt(val) : 0

				// need to refresh the graph if changing to tab 0
				if (this.tab === 0) {
					this.$nextTick(() => {
						this.$refs.graph.graph.network.network.redraw()
					})
				}
			}
		}
	},
	mounted: function() {
		this.tab = this.$route.query.tab ? parseInt(this.$route.query.tab) : 0
		this.$store.commit('explore/setPage', this.$route.query.page ? parseInt(this.$route.query.page) : 1)
		this.qtext = this.$route.query.q
		this.currentDatabase = this.$route.query.db || 'final'
		this.handleModelChange();
		this.$store.dispatch('model/getActiveIndexes')
	},
	methods: {
		clickedResult(result) {
			if (this.isFinalDb) {
				this.currentNode = result
				this.showDetailPane = true
			}
			else {
				this.$router.push({ name: 'root.details', query: { uri: result.uri, db: this.currentDatabase } })
			}
		},
		updateRoute () {
			this.$router.push({ name: 'root.explorer', query: this.routeQuery })
		},
		updateSort( val ) {
			this.sort = val.value
			this.currentSort = val
		},
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
		showMore(facet, facetName) {
      if (facet.displayingAll) {
        return;
      }
      this.$store
        .dispatch('explore/showMore', facetName)
        .then(() => {
          this.searchPending = false;
        });
    },
		toggleFacet(facet, type, value) {
      this.searchPending = true;
      this.$store
        .dispatch('explore/toggleFacet', {
          facet,
          type,
          value
        })
        .then(() => {
          this.searchPending = false;
        });
    },
    toggleNegatedFacet(facet, type, value) {
      this.searchPending = true;
      this.$store
        .dispatch('explore/toggleFacet', {
          facet,
          type,
          value,
          negated: true
        })
        .then(() => {
          this.searchPending = false;
        });
    },
		searchText() {
			this.currentNode = null
			this.$store.commit('explore/setText', { qtext: this.qtext })
			this.$store.commit('explore/setPage', 1)
			this.updateRoute()
			this.getEntities()
		},
		clearSearch() {
			this.qtext = null
			this.searchText()
		},
		previousPage() {
			this.$store.commit('explore/setPage', this.currentPage - 1)
			this.updateRoute()
		},
		nextPage() {
			this.$store.commit('explore/setPage', this.currentPage + 1)
			this.updateRoute()
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
		onGraphDrag(e) {
			// only select if dragging. don't
			// allow deselect
			this.onGraphClick(e, true)
		},
		onGraphClick(e, isDrag) {
			let nodeId = e.nodes[0];

			if (nodeId) {
				let node = this.nodeMap[nodeId]
				if (node) {
					this.currentNode = node
					this.showDetailPane = true
				}
				else {
					let concept = this.concepts.find(c => c.id === nodeId)
					if (concept) {
						this.currentNode = concept
						this.showDetailPane = true
					}
				}
			} else if (!isDrag) {
				// only deselect if not dragging
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

					let items = []
					if (!node.isConcept) {
						items = _.sortBy(Object.values(node.edgeCounts), 'label').map(e => {
							return {
								label: `Expand ${e.label} (${e.count})`,
								action: 'expand',
								rel: {
									uri: this.currentNode.id,
									label: e.label
								}
							}
						})
					} else {
						// TODO - change to be like normal entities and show numbers?
						items.push({
							label: 'Expand concept ' + node.label,
							action: 'expandConcept',
							rel: {
								concept: node.label
							}
						})
          }

					if (node.uri && node.uri.match('/com.marklogic.smart-mastering/merged/')) {
						items.push({
							divider: true
						})
						items.push({
							label: 'UnMerge',
							action: 'unmerge',
							node: node
						})
						items.push({
							label: 'Merge History',
							action: 'mergeHistory',
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
				case 'expandConcept':
					this.expandConcept(item.rel)
					break;
				case 'unmerge':
					this.confirmUnmergeMenu = true
					break;
				case 'mergeHistory':
					this.mergeHistory(item.node.uri)
					break;
			}
		},
		expandRelationship({ uri, label }) {
			// TODO: move this to store
			const page = 1
			const pageLength = 10
			this.$store.dispatch('explore/getRelatedEntities', { uri, label, page, pageLength })
		},
		expandConcept({ concept }) {
			// TODO: move this to store
			const page = 1
			const pageLength = 10
			this.$store.dispatch('explore/getEntitiesRelatedToConcept', { concept, page, pageLength })
		},
		mergeHistory(uri) {
			this.$router.push({ name: 'root.explorer.compare', query: { uri } })
		},
		unmerge(uri) {
			this.$store.dispatch('explore/unmerge', uri)
			this.currentNode = null
			this.confirmUnmergeMenu = false
		}
	}
};
</script>

<style lang="less" scoped>

.container {
	display: flex;
	flex: 0 1 auto;
	overflow: hidden;
	height: 100%;
}

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

.nowidth {
	max-width: 0%;
}

.right-pane {
	display: flex;
	flex-direction: column;
	overflow: auto;
	margin-top: 0px;
	position: absolute;
	padding: 0.25em;
	top: 0;
	right: 0;
	bottom: 0;
	width: 100%;

	.md4 {
		padding: 20px;
	}
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

.fullHeight {
	position: relative;
}

.graph-container {
	display: flex;
	flex-direction: column;
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
}

.graph-wrapper {
	display: flex;
	flex: 1;
	border-radius: 4px;
	box-shadow: 0px 3px 1px -2px rgba(0, 0, 0, 0.2), 0px 2px 2px 0px rgba(0, 0, 0, 0.14), 0px 1px 5px 0px rgba(0, 0, 0, 0.12);

}

.relparent {
	position: relative;
}

.searchArea {
	height: auto !important;
}

.v-item-group,
.v-tabs {
	width: auto;
	display: inline-flex;
}

/deep/ .v-slide-group__wrapper {
	flex: 0 0 auto;
}

.pagination-wrapper {
	line-height: 48px;
	float: right;
}

.graph-parent {
	display: flex;
	flex-direction: column;
	padding: 0.25em;
}

.scrolly {
	overflow-y: scroll;
}

/deep/ vis-network {
	padding-top: 0px;
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
}

/deep/ .mlvisjs-graph .vis-network {
	border: none;
}

/deep/ .sortMenu {
	justify-content: start;
	align-items: start;
	text-align: left;
}

.sortMenuContent .v-list-group__header {
	padding: 0 16px 0 0;
}

.v-tabs {
	flex: 0 1 auto;
}

/deep/ .chiclet {
	display: inline-block;
	padding: 5px 10px;
	border-radius: 5px;
	background-color: #ccffcc;
}

.v-chip--disabled {
	opacity: 1;
	margin: 5px 2px;
}

.search-area {
	padding: 0px;
}

.v-tab--active {
	border: 1px solid #ccc;
	border-radius: 10px;
}

.flex {
	transition: all 0.5s;
}

.no-results {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
}
</style>
