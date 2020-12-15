<template>
	<v-container fluid class="modeler">
		<v-layout column>
			<v-flex md12 class="fullHeight">
				<v-layout row>
					<v-flex class="graph-container" md8>
						<visjs-graph
							:nodes="nodes"
							:edges="edges"
							:options="graphOptions"
							layout="standard"
							@click="graphClick"
							@oncontext="graphRightClick"
							@dragStart="graphDragStart"
							@dragEnd="saveGraphLayout"
							@zoom="saveGraphLayout"
							@afterDrawing="saveGraphLayout"
							ref="graph"
						>
							<div class="vis-manipulation">
								<v-btn @click="addEntity"><v-icon left small>fa fa-plus-circle</v-icon> Add Entity</v-btn>
								<template v-if="!addEdgeMode">
									<div class="vis-separator-line"></div>
									<v-btn @click="addRelationship"><v-icon left small>fa fa-link</v-icon> Add Relationship</v-btn>
								</template>
								<template v-if="currentNodeId || currentEdgeId">
									<div class="vis-separator-line"></div>
									<v-menu
										:close-on-content-click="false"
										:nudge-width="300"
										offset-x
										v-model="confirmDeleteNode">
										<template v-slot:activator="{ on, attrs }">
											<v-btn data-cy="modeler.deleteSelected" v-bind="attrs" v-on="on"><v-icon left small>fa fa-times</v-icon> Delete Selected</v-btn>
										</template>
										<confirm
											:message="`Do you really want to delete ${currentNode ? currentNode.label : ''}?`"
											confirmText="Delete"
											@confirm="deleteNode"
											@cancel="confirmDeleteNode = false">
										</confirm>
									</v-menu>
								</template>
							</div>
							<div class="vis-status" v-if="addEdgeMode">
								Click on an entity and drag the relationship to another entity to connect them.
								<v-spacer/>
								<v-btn @click="cancelAddRelationship">Cancel</v-btn>
							</div>
						</visjs-graph>
						<v-btn data-cy="modelerPageVue.addNodeButton" class="hideUnlessTesting" v-on:click="addEntity()">Add Node Test</v-btn>
						<ul class="hideUnlessTesting">
							<li v-for="node in nodes" :key="node.id" data-cy="nodeList" v-on:click="selectNode(node)">{{ node.id }}</li>
						</ul>
						<ul class="hideUnlessTesting edges">
							<li v-for="edge in edges" :key="edge.id" data-cy="edgeList" v-on:click="selectEdge(edge)">{{ edge.id }}</li>
						</ul>
					</v-flex>
					<v-flex md4 class="right-pane">
						<entity-pick-list
							:nodes="nodes"
							:entity="currentNode"
							:currentEdge="currentEdgeId"
							:edges="modelEdges"
							@selectedNode="onSelectedNode"
							@deleteModel="deleteModel"
							@saveGraphImage="saveGraphImage"
							@saveEdge="saveEdge"
							@deleteEdge="deleteEdge"
							@updateEntity="updateEntity"
						></entity-pick-list>
					</v-flex>
				</v-layout>
			</v-flex>
		</v-layout>
		<add-entity-dialog ref="addEntity"></add-entity-dialog>
		<add-relationship-dialog
			ref="addRelationship"
			entityLocked></add-relationship-dialog>
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
					@click="runRightclickAction(item.action, item.value)"
				>
					<v-list-item-title>{{item.label}}</v-list-item-title>
				</v-list-item>
			</v-list>
		</v-menu>
	</v-container>
</template>

<script>
import VisjsGraph from '@/components/graph/graph.vue'
import EntityPickList from '@/components/ml-modeler/EntityPickList'
import AddRelationshipDialog from '@/components/AddRelationshipDialog.vue'
import AddEntityDialog from '@/components/AddEntityDialog.vue'
import Confirm from '@/components/Confirm.vue'
import { mapState } from 'vuex'

export default {
	data() {
		var locales = {
			en: {
				edit: 'Edit',
				del: 'Delete selected',
				back: 'Back',
				addNode: 'Add Entity',
				addEdge: 'Add Relationship',
				editNode: 'Edit Entity',
				editEdge: 'Edit Relationship',
				addDescription: 'Click in an empty space to place a new entity or concept.',
				edgeDescription:
					'Click on an entity and drag the relationship to another entity to connect them.',
				editEdgeDescription:
					'Click on the control points and drag them to a entity to connect to it.',
				createEdgeError: 'Cannot link entities to a cluster.',
				deleteClusterError: 'Clusters cannot be deleted.',
				editClusterError: 'Clusters cannot be edited.'
			}
		}

		return {
			addEdgeMode: false,
			confirmDeleteNode: false,
			graphLayout: {},
			rightClickMenu: null,
			rightClickItems: [],
			rightClickPos: { x: 0, y: 0 },
			showFullEntityNames: true, // if set to false we show the first 2 chars of each entity name
			currentNodeId: null,
			currentEdgeId: null,
			selectedNode: 0,
			graphOptions: {
				interaction: {
					navigationButtons: true,
					zoomSpeed: 0.5
				},
				locale: 'en',
				locales: locales,
				autoResize: false,
				nodes: {
					shape: 'circle',
					size: 30,
					borderWidth: 2,
					font: {
						size: 12,
						color: '#44499c'
					},
					margin: 10,
					color: {
						border: '#44499c',
						background: '#fff',
						highlight: {
							border: '#44499c'
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
					// disable physics
					//need to look into enabling and disabling with a flag
					//enable to avoid overlapping edges, disable once graph rendered for display and interaction.
					enabled: false,
					stabilization: { enabled: false }
				},

				manipulation: {
					enabled: false,
					initiallyActive: false,
					addNode: this.graphAddNode,
					addEdge: this.graphAddEdge.bind(this),
					editEdge: false,
					deleteNode: this.graphDeleteNode,
					deleteEdge: this.graphDeleteEdge
				}
			}
		}
	},
	components: {
		Confirm,
		VisjsGraph,
		EntityPickList,
		AddRelationshipDialog,
		AddEntityDialog
	},
	computed: {
		currentNode() {
			return this.nodes.find(n => n.id === this.currentNodeId)
		},
		currentEdge() {
			return this.edges.find(n => n.id === this.currentEdgeId)
		},
		nodes() {
			return this.model ? Object.values(this.model.nodes).map(node => {
				const newNode = { ...node }
				if (newNode.type === 'concept') {
					newNode.shapeProperties = {
						borderDashes: [4,5]
					}
					let color = newNode.color || {}
					color.border = '#3cdbc0'
					color.highlight = color.highlight || {}
					color.highlight.border = '#3cdbc0'
					color.highlight.background = '#c9f5ed'
					newNode.color = color
				}
				if (this.graphLayout) {
					if (this.graphLayout.positions && this.graphLayout.positions[newNode.id]) {
						const pos = this.graphLayout.positions[newNode.id]
						newNode.x = pos.x
						newNode.y = pos.y
					}
				}
				return newNode
			}) : []
		},
		entityNames() {
			return this.model ? Object.values(this.model.nodes).map(node => node.entityName) : []
		},
		embeddedEdges() {
			let edges = []
			if (this.model) {
				Object.values(this.model.nodes).forEach(node => {
					node.properties
						.filter(p => this.entityNames.indexOf(p.type) >= 0)
						.forEach(p => {
							edges.push({
								cardinality: p.isArray ? '1:Many' : '1:1',
								from: node.id,
								id: `${node.id}-${p.name}-${p.type.toLowerCase()}`,
								label: p.name,
								to: p.type.toLowerCase(),
								dashes: [5, 10],
								color: {
									color: '#b0cb2c'
								},
								embedded: true
							})
						})
				})
			}
			return edges
		},
		modelEdges() {
			return this.model ? Object.values(this.model.edges).map(edge => {
				const newEdge = { ...edge }
				if ((this.model.nodes[newEdge.to] && this.model.nodes[newEdge.to].type === 'concept') ||
					(this.model.nodes[newEdge.from] && this.model.nodes[newEdge.from].type === 'concept')) {
					newEdge.dashes = [4,3]
					newEdge.color = newEdge.color || {}
					newEdge.color.color = '#3cdbc0'
				}
				return newEdge
			}) : []
		},
		edges() {
			return this.modelEdges.concat(this.embeddedEdges)
		},
		edgeIds() {
			return this.modelEdges.map(e => e.id)
		},
		...mapState({
			model: state => state.model.model
		}),
	},
	mounted: function() {
		this.loadGraphLayout()
	},
	methods: {
		addEntity() {
			let nodeData = {
				id: "89855843-0ae4-40b7-8a58-6d378f040354",
				label: "new"
			}
			this.graphAddNode(nodeData)
		},
		addRelationship() {
			this.addEdgeMode = true
			this.$refs.graph.addEdgeMode()
		},
		cancelAddRelationship() {
			this.addEdgeMode = false
			this.$refs.graph.disableEditMode()
		},
		deleteNode() {
			this.$refs.graph.deleteSelected()
			this.confirmDeleteNode = false
		},
		// called by Cypress to select a node, as couldn't find how to make it click the graph directly
		selectNode( selectedNode ) {
			this.$refs.graph.network.selectNodes([selectedNode.id])
			const props = {
				"pointer": null,
				"event": null,
				"nodes": [selectedNode.id],
				"edges": [],
				"items": [
					{
						"nodeId": selectedNode.id
					}
				]
			}
			this.$refs.graph.network.body.emitter.emit('select', props)
			this.$refs.graph.network.body.emitter.emit('click', props)
		},
		// called by Cypress to select an edge, as couldn't find how to make it click the graph directly
		selectEdge( edge ) {
			this.$refs.graph.network.selectEdges([edge.id])
			const props = {
				"pointer": null,
				"event": null,
				"nodes": [],
				"edges": [
					edge.id
				],
				"items": [
					{
						"edgeId": edge.id
					}
				]
			}
			this.$refs.graph.network.body.emitter.emit('select', props)
			this.$refs.graph.network.body.emitter.emit('click', props)
		},
		updateEntity(entity) {
			const idx = this.nodes.findIndex(n => n.id === entity.id)
			if (idx >= 0) {
				this.nodes[idx] = entity
			} else {
				this.nodes.push(entity)
			}
			this.doMLSave()
		},
		onSelectedNode(e) {
			this.currentNodeId = e.id
			this.$refs.graph.selectNodes([e.id])

			if (this.$refs.graph.isOffscreen(e.id)) {
				//move to the selected node
				this.$refs.graph.network.focus(e.id, {
					position: {
						x: e.x,
						y: e.y,
					},
					animation: true
				})
			}
		},
		deleteEntity(entityId, save = true) {
			const idx = this.nodes.findIndex(e => e.id === entityId)
			if (idx >= 0) {
				this.nodes.splice(idx, 1)
			}
			if (save) {
				this.doMLSave()
			}
			this.currentNodeId = null
		},
		getEdgeId(edge) {
			return `${edge.from}-${this.getId(edge.label)}-${edge.to}`
		},
		saveEdge(edge) {
			if (edge) {
				if (edge.id) {
					this.deleteEdge(edge, false)
				}
				let roundness = this.getNextRoundness(edge.to, edge.from)
				let id = this.getEdgeId(edge)
				const newEdge = {
					...edge,
					id: id,
					smooth: { roundness: roundness }
				}
				const idx = this.modelEdges.indexOf(e => e.id === id)
				if (idx >= 0) {
					this.modelEdges[idx] = newEdge
				} else {
					this.modelEdges.push(newEdge)
				}
				this.doMLSave()
			}
		},
		getNextRoundness(toNode, fromNode) {
			const defaultRoundness = 0.5 // midpoint
			let roundnessInc = 0.15
			let roundness = defaultRoundness
			let maxRoundness = roundness
			let minRoundness = roundness

			// loop througn all the edges and find the range of roundness's
			this.modelEdges.forEach(edge => {
				if (edge.to == toNode && edge.from == fromNode) {
					if (edge.smooth.roundness > maxRoundness) {
						maxRoundness = edge.smooth.roundness
					}
					if (edge.smooth.roundness < minRoundness) {
						minRoundness = edge.smooth.roundness
					}
				}
			})
			if ( maxRoundness - defaultRoundness > defaultRoundness - minRoundness ) {
				// new roundness should be smaller than min
				roundness = minRoundness - roundnessInc
			} else {
				// new roundness should be larger than max
				roundness = maxRoundness + roundnessInc
			}
			if (roundness < 0 || roundness > 1 ) {
				// guess a random value!
				roundness = Math.random() - 0.5
			}
			return roundness
		},
		deleteEdge(edgeId, save = true) {
			// deletes an individual edge
			const idx = this.modelEdges.findIndex(e => e.id === edgeId)
			if (idx >= 0) {
				this.modelEdges.splice(idx, 1)
			}
			if (save) {
				this.doMLSave()
			}
		},
		getId(str) {
			// get a unique ID - perhaps should use some uuid library?
			return str
				.trim()
				.toLowerCase()
				.replace(/ /g, '_')
		},
		loadGraphLayout() {
			if (!this.model) {
				return
			}

			const key = `layout-${this.model.name.replace(' ', '-')}`
			const item = localStorage.getItem(key)
			this.graphLayout = item ? JSON.parse(item) : {
				position: { x: 0, y: 0 },
				scale: 1.0
			}
			this.$refs.graph.moveTo(this.graphLayout)
		},
		saveGraphLayout() {
			if (!this.model) {
				return
			}
			const key = `layout-${this.model.name.replace(' ', '-')}`
			localStorage.setItem(key, JSON.stringify({
				position: this.$refs.graph.getViewPosition(),
				scale: this.$refs.graph.getScale(),
				positions: this.$refs.graph.getPositions()
			}))
		},
		async doMLSave() {
			const model = JSON.parse(JSON.stringify({
				...this.model,
				nodes: this.nodes.reduce((output, entity) => {
					output[entity.id] = entity
					return output
				}, {}),
				edges: this.modelEdges.reduce((output, edge) => {
					output[edge.id] = edge
					return output
				}, {}),
				img: await this.resizedataURL(this.$refs.graph.getGraphImage(), 40, 40)
			}))
			await this.$store.dispatch('model/save', model)
		},
		saveGraphImage() {
			const graphImage = this.$refs.graph.getGraphImage()
			if (graphImage) {
				let a = document.createElement('a')
				a.setAttribute('download', `${this.model.name.replace(/ /g, '')}.png`)
				a.href = graphImage
				a.innerHTML = 'testing'
				a.style.display = 'none'
				document.body.appendChild(a)
				a.click()
			}
		},
		async deleteModel() {
			await this.$store.dispatch('model/delete', this.model)
		},
		graphAddNode(nodeData, callback) {
			this.$refs.addEntity.open(this.nodes.map(n => n.label.toLowerCase()))
				.then(({type, name, iri, version}) => {
					if (name) {
						var entityLabel
						if (this.showFullEntityNames == true) {
							entityLabel = name
						} else {
							entityLabel = name.substring(0, 2)
						}
						nodeData.entityName = name
						nodeData.label = entityLabel
						if (type === 'concept') {
							nodeData.shapeProperties = {
								borderDashes: [2, 2]
							}
						}
						nodeData.type = type
						nodeData.baseUri = iri
						nodeData.version = version

						const node = {
							...nodeData,
							id: this.getId(nodeData.entityName),
							properties: []
						}
						this.nodes.push(node)
						this.currentNodeId = node.id
						this.doMLSave()
						!!callback && callback(nodeData)

						// select the entity after making it
						setTimeout(() => {
							this.$refs.graph.selectNodes([node.id])
						}, 500)
					}
					else {
						!!callback && callback(null)
					}
				})
		},
		graphAddEdge(nodeData, callback) {
			if (nodeData.from && nodeData.to) {
				this.$refs.addRelationship.open(nodeData, this.nodes, this.edgeIds)
				.then(node => {
					if (node) {
						callback(nodeData)
						this.saveEdge({
							from: nodeData.from,
							label: node.label,
							to: nodeData.to,
							cardinality: node.cardinality,
							keyFrom: node.keyFrom,
							keyTo: node.keyTo
						})
					}
				})
				.finally(() => {
					this.addEdgeMode = false
					this.$refs.graph.disableEditMode()
				})
			}
		},
		graphDeleteNode(nodeData, callback) { // eslint-disable-line no-unused-vars
			nodeData.edges.forEach(item => this.deleteEdge(item, false))
			nodeData.nodes.forEach(item => this.deleteEntity(item, false))
			this.doMLSave()
		},
		graphDeleteEdge(edgeData, callback) {	// eslint-disable-line no-unused-vars
			edgeData.edges.forEach(item => this.deleteEdge(item, false))
			this.doMLSave()
		},
		graphClick(e) {
			let nodeId = e.nodes[0]
			let edgeId = e.edges[0]

			if (nodeId) {
				// not adding edges
				this.currentNodeId = nodeId
				this.currentEdgeId = null
			} else if (edgeId) {
				this.currentEdgeId = edgeId
				this.currentNodeId = null
			} else {
				this.currentNodeId = null
				this.currentEdgeId = null
			}
		},
		graphRightClick(e) {
			e.event.preventDefault()
			this.rightClickPos = {
				x: e.event.x,
				y: e.event.y,
				canvas: e.pointer.canvas
			}

			let nodeId = this.$refs.graph.getNodeAt(e.pointer.DOM)
			if (nodeId) {
				const node = this.nodes.find(n => n.id === nodeId)
				if (node) {
					this.currentNodeId = nodeId
					this.$refs.graph.selectNodes([nodeId])
					this.rightClickItems = [{
						label: `Delete ${node.label}`,
						action: 'deleteEntity',
						value: nodeId
					}]
				}
			}
			else {
				this.rightClickItems = [{
					label: 'Add Entity',
					action: 'addEntity'
				}]
			}
			this.rightClickMenu = true
		},
		runRightclickAction(action, value) {
			if (action === 'addEntity') {
				let nodeData = {
					id: "89855843-0ae4-40b7-8a58-6d378f040354",
					label: "new",
					x: this.rightClickPos.canvas.x,
					y: this.rightClickPos.canvas.y
				}
				this.graphAddNode(nodeData)
			}
			else if (action === 'deleteEntity') {
				const selectedNodes = this.$refs.graph.getSelectedNodes()
				if (selectedNodes[0] === value) {
					this.confirmDeleteNode = true
				}
			}
		},
		graphDragStart(e) {
			let nodeId = e.nodes[0];
			if (nodeId) {
				this.$refs.graph.selectNodes([nodeId])
				this.currentNodeId = nodeId
				this.currentEdgeId = null
			}
		},
		graphDragEnd(e) {
			let nodeId = e.nodes[0]
			if (nodeId) {
				let nodeBeingDragged = this.nodes.find(e => e.id === nodeId)
				if (nodeBeingDragged) {
					nodeBeingDragged['x'] = e.pointer.canvas.x
					nodeBeingDragged['y'] = e.pointer.canvas.y
				}
				this.saveGraphLayout()
			}
		},
		resizedataURL(datas, wantedWidth, wantedHeight) {
			return new Promise(async (resolve) => {
					var img = document.createElement('img')
					img.onload = function() {
							var canvas = document.createElement('canvas')
							var ctx = canvas.getContext('2d')

							canvas.width = wantedWidth
							canvas.height = wantedHeight

							ctx.drawImage(this, 0, 0, wantedWidth, wantedHeight)

							resolve(canvas.toDataURL())
					}
					img.src = datas
			})
		}
	},
	watch: {
		model(val) {
			if (val) {
				this.loadGraphLayout()
			}
		}
	}
}
</script>

<style lang="less" scoped>
.pagination {
	margin: 0px 5px;
}

table {
	width: 100%;
}

.right-pane {
	padding: 20px;
	margin-top: 0px;
}

.ml-search .v-input {
	margin-bottom: 20px;
}

.hideUnlessTesting {
	visibility: hidden;
	position: absolute;
	top: 0px;

	&.edges {
		left: 200px;
	}
}

.container--fluid {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
}

.modeler {
	.layout.column,
	.layout.row,
	.graph {
		height: 100%
	}

	.graph-container {
		padding: 20px;
	}
}

.fullHeight {
	height: 100%;
}

.vis-manipulation {
	border-bottom: 1px solid #999;
	padding: 4px 10px;
	display: flex !important;
	align-items: center;
	box-sizing: content-box;

	.vis-separator-line {
		display: inline-block;
		width: 1px;
		height: 21px;
		background-color: #bdbdbd;
		margin: 0 7px 0 15px;
		height: 100%;
		background-color: #999;
		margin-left: 10px;
		margin-right: 10px;
	}
}

.vis-status {
	padding: 10px;
	color: white;
	background-color: #607d8b;
	display: flex;
}
</style>
