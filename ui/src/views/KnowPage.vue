<template>
	<v-container fluid class="KnowPage">
		<v-layout column>
			<v-flex>
				<form class="ml-input ml-search form-inline" role="search" v-on:submit.prevent="searchText">
					<v-text-field
						hide-details
						v-model="qtext"
						placeholder="Search"
						prepend-icon="search"
						single-line
						clearable
						@click:clear="clearSearch"
					></v-text-field>
				</form>
			</v-flex>
			<v-flex md12>
				<v-layout row>
					<v-flex md8 class="graph-container">
						<div v-if="nodes && nodes.length > 0">
							Showing results {{start}} to {{end}} of {{totalResults}}
							<span v-if="currentPage > 1" class="pagination"><a @click="previousPage">&lt;&lt; previous</a></span>
							<span v-if="currentPage < lastPage" class="pagination"><a @click="nextPage">next &gt;&gt;</a></span>
						</div>
						<div v-else>No results found</div>
						<visjs-graph
							class="graph-wrapper"
							:nodes="nodes"
							:edges="edges"
							:options="graphOptions"
							layout="standard"
							@click="onGraphClick"
							@dragStart="onGraphClick"
							@oncontext="graphRightClick"
							ref="graph"
						></visjs-graph>
					</v-flex>
					<v-flex md4 class="right-pane">
						<v-select
							:items="paginations"
							item-text="label"
							item-value="value"
							label="Subjects Per Page"
							:value="subjectsPerPage"
							@change="changeSubjectsPerPage"
							required
						></v-select>
						<v-select
							:items="linkPaginations"
							item-text="label"
							item-value="value"
							label="Links Per Subject"
							:value="linksPerSubject"
							@change="changeLinksPerSubject"
							required
						></v-select>
						<v-select
							:items="linkPaginations"
							item-text="label"
							item-value="value"
							label="Max Related"
							:value="maxRelated"
							@change="changeMaxRelated"
							required
						></v-select>
						<v-select
							:items="databases"
							item-text="label"
							item-value="value"
							label="Database"
							v-model="currentDatabase"
							required
						></v-select>
						<v-select
							:items="sorts"
							item-text="label"
							item-value="value"
							label="Sort"
							v-model="currentSort"
							required
						></v-select>
							<v-select
							:items="dedup"
							item-text="label"
							item-value="value"
							label="Deduplicate Triples"
							v-model="currentDedup"
							required
						></v-select>
						<div class="trip-view" v-if="currentTriple">
							<div v-if="currentTriple.s">{{currentTriple.s.orig}}</div>
							<div v-if="currentTriple.p">{{currentTriple.p.orig}}</div>
							<div v-if="currentTriple.o">
								<img class="trip-preview" v-if="currentTriple.o.orig.match('.jpg')" :src="currentTriple.o.orig">
								<span v-else>{{currentTriple.o.orig}}</span>
							</div>
						</div>
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
				<v-list-item-group>
					<v-list-item
						v-for="item in rightClickItems"
						:key="item.label">
						<v-list-item-title v-if="!item.submenu" @click="contextClick(item)">{{item.label}}</v-list-item-title>
						<v-menu v-if="item.submenu" offset-x right open-on-hover :close-delay="500">
							<template v-slot:activator="{ on }">
								<v-list-item-title v-on="on">{{item.label}} &gt;</v-list-item-title>
							</template>
							<v-list class="submenu">
								<v-list-item
									v-for="subitem in item.submenu"
									:key="subitem.label">
									<v-list-item-title @click="contextClick(subitem)">{{subitem.label}}</v-list-item-title>
								</v-list-item>
							</v-list>
						</v-menu>
					</v-list-item>
				</v-list-item-group>
			</v-list>
		</v-menu>
	</v-container>
</template>

<script>

import VisjsGraph from '@/components/graph/graph.vue';
import { mapState } from 'vuex'

export default {
	name: 'TriplesPage',
	data: function() {
		return {
			paginations: [
				{
					label: '1',
					value: 1
				},
				{
					label: '5',
					value: 5
				},
				{
					label: '10',
					value: 10
				},
				{
					label: '25',
					value: 25
				},
				{
					label: '50',
					value: 50
				},
				{
					label: '100',
					value: 100
				}
			],
			linkPaginations: [
				{
					label: '5',
					value: 5
				},
				{
					label: '10',
					value: 10
				},
				{
					label: '25',
					value: 25
				},
				{
					label: '50',
					value: 50
				},
				{
					label: 'all',
					value: -1
				}
			],
			databases: [
				{
					label: 'Staging',
					value: 'staging'
				},
				{
					label: 'Final',
					value: 'final'
				},
				{
					label: 'Jobs',
					value: 'job'
				}
			],
			sorts: [
				{
					label: 'Default',
					value: 'default'
				},
				{
					label: 'Least Connected First',
					value: 'least-connected'
				},
				{
					label: 'Most Connected First',
					value: 'most-connected'
				}
			],
			dedup: [
				{
					label: 'On',
					value: 'on'
				},
				{
					label: 'Off',
					value: 'off'
				}
			],
			rightClickMenu: null,
			rightClickPos: { x: 0, y: 0 },
			rightClickItems: [],
			currentTriple: null,
			title: 'Explore',
			colors: {},
			currentDatabase: 'final',
			currentSort: 'default',
			currentDedup: 'off',
			qtext: '',
			graphOptions: {
				autoResize: false,
				height: '100%',
				locale: 'en',
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
					smooth: {
						roundness: 0.1 // initial
					}
				},
				physics: {
					enabled: true,
					solver: 'forceAtlas2Based',
					forceAtlas2Based: {
						gravitationalConstant: -200,
						centralGravity: 0.01,
						springLength: 100,
						springConstant: 0.08,
						damping: 0.4,
						avoidOverlap: 0
					},
					maxVelocity: 150, // default 50
					minVelocity: 6, // default 0.1
					stabilization: {
						enabled: true,
						iterations: 1000,
						updateInterval: 100,
						onlyDynamicEdges: false,
						fit: false
					},
					timestep: 0.5,
					adaptiveTimestep: true
				},
				manipulation: {
					enabled: false, // true = use the edit feature
					initiallyActive: false,
				}
			}
		};
	},
	components: {
		VisjsGraph
	},
	computed: {
		...mapState({
			nodesMap: state => state.triples.nodes,
			edgesMap: state => state.triples.edges,
			queryText: state => state.triples.qtext,
			currentPage: state => state.triples.page,
			lastPage: state => Math.ceil(state.triples.total / state.triples.subjectsPerPage),
			start: state => ((state.triples.page - 1) * state.triples.subjectsPerPage) + 1,
			total: state => state.triples.total,
			subjectsPerPage: state => state.triples.subjectsPerPage,
			linksPerSubject: state => state.triples.linksPerSubject,
			maxRelated: state => state.triples.maxRelated
		}),
		totalResults() {
			return Math.max(this.total, this.end)
		},
		end() {
			return this.start + this.subjectsPerPage - 1
		},
		nodes() {
			return Object.values(this.nodesMap)
		},
		edges() {
			return Object.values(this.edgesMap)
		},
	},
	watch: {
		currentDatabase() {
			this.getTriples()
		},
		currentSort() {
			this.getTriples()
		},
		currentDedup() {
			this.getTriples()
		}
	},
	mounted: function() {
		this.getTriples()
	},
	methods: {
		changeSubjectsPerPage(e) {
			this.$store.commit('triples/setSubjectsPerPage', e)
			this.getTriples()
		},
		changeLinksPerSubject(e) {
			this.$store.commit('triples/setLinksPerSubject', e)
			this.getTriples()
		},
		changeMaxRelated(e) {
			this.$store.commit('triples/setMaxRelated', e)
		},
		searchText() {
			this.$store.commit('triples/setPage', 1)
			this.$store.commit('triples/setText', { qtext: this.qtext })
			this.getTriples()
		},
		clearSearch() {
			this.qtext = null
			this.searchText()
		},
		previousPage() {
			this.$store.commit('triples/setPage', this.currentPage - 1)
			this.getTriples()
		},
		nextPage() {
			this.$store.commit('triples/setPage', this.currentPage + 1)
			this.getTriples()
		},
		getTriples() {
			this.currentTriple = null
			this.$store
				.dispatch('triples/browse', {
					database: this.currentDatabase,
					sort: this.currentSort,
					dedup: this.currentDedup
				})
				.then(() => {
					this.searchPending = false;
				});
		},
		onGraphClick(e) {
			let nodeId = e.nodes[0];
			let edgeId = e.edges[0];

			if (nodeId) {
				let node = this.nodesMap[nodeId]
				if (node) {
					let trip = {
						s: null,
						p: null,
						o: null
					}

					let edge = this.edges.find(e => e.to === nodeId)
					if (edge) {
						trip.p = edge

						let s = this.nodesMap[edge.from]
						if (s) {
							trip.s = s
						}
						trip.o = node
					}
					else {
						edge = this.edges.find(e => e.from === nodeId)
						if (edge) {
							trip.p = edge

							let o = this.nodesMap[edge.to]
							if (o) {
								trip.o = o
							}
						}
						trip.s = node

					}

					this.currentTriple = trip
				}
			} else if (edgeId) {
				// let edge = this.edges.find(e => e.id === edgeId);
			} else {
				this.currentTriple = null
			}
		},
		graphRightClick(e) {
			e.event.preventDefault()

			let nodeId = this.$refs.graph.getNodeAt(e.pointer.DOM)
			if (nodeId) {
				let node = this.nodes.find(n => n.id === nodeId)
				if (node) {
					this.rightClickPos = {
						x: e.event.x,
						y: e.event.y
					}

					let predicates = null
					if (node.predicates) {
						predicates = node.predicates.map(p => {
							return {
								label: p,
								action: 'expand',
								node: node,
								predicate: p
							}
						})

						predicates.unshift( {
							label: 'All (Search-Filtered)',
							filterText: true,
							action: 'expand',
							node: node
						})

						predicates.unshift( {
							label: 'All',
							filterText: false,
							action: 'expand',
							node: node
						})
					}

					this.rightClickItems = [
						{
							label: 'Get Related',
							action: 'expand',
							filterText: false,
							node: node,
							submenu: predicates
						},
						{
							label: 'Hide Everything else',
							action: 'hideOthers',
							node: node
						}
					]

					this.$nextTick(() => {
						this.rightClickMenu = true;
					})
				}
			}
		},
		async contextClick(item) {
			this.rightClickMenu = false
			let nodes = {}
			switch(item.action) {
				case 'expand':
					this.expandRelationship(item)
					break
				case 'hideOthers':
					await this.$store.commit('triples/setEdges', {})
					nodes[item.node.id] = item.node
					await this.$store.commit('triples/setNodes', nodes)
					break
			}
		},
		expandRelationship(rel) {
			this.$store.commit('triples/setText', { qtext: this.qtext })
			this.$store
				.dispatch('triples/getRelated', {
					item: rel.node.orig,
					itemId: rel.node.id,
					isIRI: rel.node.isIRI,
					database: this.currentDatabase,
					predicate: rel.predicate || null,
					filterText: rel.filterText
				})
		}
	}
};
</script>

<style lang="less" scoped>
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

.trip-view {
	border: 1px solid #999;
	// border-radius: 5px;
	padding: 10px;
	text-align: center;

	div {
		word-break: break-all;
		padding: 5px;

		&::after {
			border: 1px solid red;
		}
	}
}

.submenu {
	max-height: 250px;
	overflow: scroll;
	&::-webkit-scrollbar {
		-webkit-appearance: none;
		width: 10px;
	}

	&::-webkit-scrollbar-thumb {
		border-radius: 5px;
		background-color: rgba(0,0,0,.5);
		-webkit-box-shadow: 0 0 1px rgba(255,255,255,.5);
	}
}

.trip-preview {
	max-height: 200px;
}

.hideUnlessTesting {
	visibility: hidden;
	position: absolute;
	top: -1000px;
	left: -1000px;
}

.container--fluid {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
}

.layout.column,
.layout.row,
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
</style>


<style lang="less">
	.KnowPage vis-network {
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
	}
</style>
