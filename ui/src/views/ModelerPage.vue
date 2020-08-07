<template>
	<v-container fluid class="modeler">
		<v-layout column>
			<v-flex md12>
				<v-layout row>
					<v-flex class="graph-container" md8>
						<visjs-graph
							:nodes="nodes"
							:edges="edges"
							:options="graphOptions"
							layout="standard"
							:events="graphEvents"
							ref="graph"
						></visjs-graph>
						<v-btn data-cy="modelerPageVue.addNodeButton" class="hideUnlessTesting" v-on:click="graphAddNode({})">Add Node Test</v-btn>
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
							:currentEdge="currentEdge"
							:edges="edges"
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
					@click="runRightclickAction(item.action, $event)"
				>
					<v-list-item-title>{{item.label}}</v-list-item-title>
				</v-list-item>
			</v-list>
		</v-menu>
	</v-container>
</template>

<script>
import VisjsGraph from 'grove-vue-visjs-graph'
import 'vis/dist/vis.css'
import 'ml-visjs-graph/less/ml-visjs-graph.js.less'
import EntityPickList from '@/components/ml-modeler/EntityPickList'
import AddRelationshipDialog from '@/components/AddRelationshipDialog.vue'
import AddEntityDialog from '@/components/AddEntityDialog.vue'
import { mapState } from 'vuex'

export default {
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
			graphImage: null,
			rightClickMenu: null,
			rightClickItems: [],
			rightClickPos: { x: 0, y: 0 },
			showFullEntityNames: true, // if set to false we show the first 2 chars of each entity name
			currentNodeId: null,
			currentEdge: null,
			selectedNode: 0,
			graphOptions: {
				height: '100%',
				locale: 'en',
				locales: locales,
				autoResize: false,
				nodes: {
					shape: 'circle',
					margin: 10,
					color: {
						border: '#44499c',
						highlight: {
							border: '#44499c'
						}
					},
					font: {
						color: '#44499c'
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
					enabled: true, // true = use the edit feature
					initiallyActive: true,
					addNode: this.graphAddNode,
					addEdge: this.graphAddEdge.bind(this),
					editEdge: false,
					deleteNode: this.graphDeleteNode,
					deleteEdge: this.graphDeleteEdge
				}
			},
			graphEvents: {
				click: this.graphClick,
				oncontext: this.graphRightClick,
				dragStart: this.graphDragStart,
				dragEnd: this.graphDragEnd,
				afterDrawing: this.afterDraphDrawing
			}
		}
	},
	components: {
		VisjsGraph,
		EntityPickList,
		AddRelationshipDialog,
		AddEntityDialog
	},
	computed: {
		currentNode() {
			return this.nodes.find(n => n.id === this.currentNodeId)
		},
		nodes() {
			return this.model ? Object.values(this.model.nodes).map(node => {
				const newNode = { ...node }
				if (newNode.type === 'concept') {
					newNode.shapeProperties = {
						borderDashes: [4,3]
					}
					let color = newNode.color || {}
					color.border = '#3cdbc0'
					color.highlight = color.highlight || {}
					color.highlight.border = '#3cdbc0'
					color.highlight.background = '#c9f5ed'
					newNode.color = color
				}
				return newNode
			}) : []
		},
		edges() {
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
		edgeIds() {
			return this.edges.map(e => e.id)
		},
		...mapState({
			model: state => state.model.model
		}),
	},
	mounted: function() {
		// work around a bug in visjs where it resizes a lot in firefox
		this.$refs.graph.graph.network.network.canvas._cleanUp()
	},
	methods: {
		// called bu Cypress to select a node, as couldn't find how to make it click the graph directly
		selectNode( selectedNode ) {
			this.$refs.graph.graph.network.network.selectNodes([selectedNode.id])
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
			this.$refs.graph.graph.network.network.body.emitter.emit('select', props)
			this.$refs.graph.graph.network.network.body.emitter.emit('click', props)
		},
		// called bu Cypress to select an edge, as couldn't find how to make it click the graph directly
		selectEdge( edge ) {
			this.$refs.graph.graph.network.network.selectEdges([edge.id])
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
			this.$refs.graph.graph.network.network.body.emitter.emit('select', props)
			this.$refs.graph.graph.network.network.body.emitter.emit('click', props)
		},
		updateEntity(entity) {
			console.log('nodes', this.nodes)
			const idx = this.nodes.findIndex(n => n.id === entity.id)
			if (idx >= 0) {
				this.nodes[idx] = entity
			} else {
				this.nodes.push(entity)
			}
			this.doMLSave()
		},
		onSelectedNode(e) {
			//an outside component selected a new node
			this.currentNodeId = e.id
			//use graph api to pan to new node and select
			//the selection api on the graph is really buried...
			this.$refs.graph.graph.network.network.selectionHandler.selectNodes([e.id])

			//NOTE look at TOPG-94 _ need some logic here to determine if we should zoom and pan
			//like if current node is off screen
			var shouldZoomAndPan = false
			if (shouldZoomAndPan) {
				//move to the selected node
				let myOptions = {}
				myOptions.scale = 1 //new view will be 1:1 scale
				myOptions.position = {}
				myOptions.position.x = e.x //position of selected node
				myOptions.position.y = e.y //position of selected node
				myOptions.animation = true //use the default animation
				this.$refs.graph.graph.network.network.moveTo(myOptions)
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
				const idx = this.edges.indexOf(e => e.id === id)
				if (idx >= 0) {
					this.edges[idx] = newEdge
				} else {
					this.edges.push(newEdge)
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
			this.edges.forEach(edge => {
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
			const idx = this.edges.findIndex(e => e.id === edgeId)
			if (idx >= 0) {
				this.edges.splice(idx, 1)
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
		async doMLSave() {
			const model = JSON.parse(JSON.stringify({
				...this.model,
				nodes: this.nodes.reduce((output, entity) => {
					output[entity.id] = entity
					return output
				}, {}),
				edges: this.edges.reduce((output, edge) => {
					output[edge.id] = edge
					return output
				}, {}),
				img: await this.resizedataURL(this.graphImage, 40, 40)
			}))
			await this.$store.dispatch('model/save', model)
		},
		saveGraphImage() {
			if (this.graphImage) {
				let a = document.createElement('a')
				a.setAttribute('download', `${this.model.name.replace(/ /g, '')}.png`)
				a.href = this.graphImage
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
				this.currentEdge = null
			} else if (edgeId) {
				this.currentEdge = edgeId
				this.currentNodeId = null
			} else {
				this.currentNodeId = null
				this.currentEdge = null
			}
		},
		graphRightClick(e) {
			e.event.preventDefault()
			this.rightClickPos = {
				x: e.event.x,
				y: e.event.y,
				canvas: e.pointer.canvas
			}

			let network = this.$refs.graph.graph.network.network
			let node = network.getNodeAt(e.pointer.DOM)
			if (!node) {
				this.rightClickItems = [{
					label: 'Add Entity',
					action: 'addEntity'
				}]
				this.rightClickMenu = true
			}
		},
		runRightclickAction(action) {
			if (action === 'addEntity') {
				let nodeData = {
					id: "89855843-0ae4-40b7-8a58-6d378f040354",
					label: "new",
					x: this.rightClickPos.canvas.x,
					y: this.rightClickPos.canvas.y
				}
				this.graphAddNode(nodeData)
			}
		},
		graphDragStart(e) {
			let nodeId = e.nodes[0];
			if (nodeId) {
				this.currentNodeId = nodeId
				this.currentEdge = null
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
				this.doMLSave()
			}
		},
		afterDraphDrawing(ctx) {
			this.graphImage = ctx.canvas.toDataURL()
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

.graph-container {
	padding: 10px;
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
	/deep/ .mlvisjs-graph,
	.graph-container,
	.graph-container div {
		height: 100%
	}
}

</style>
