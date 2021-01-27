<template>
	<div class="graph _lr-hide">
		<slot></slot>
		<div ref="visualization" class="fullHeight"></div>
		<div id="minimapWrapper" style="position: absolute; bottom:0; margin: 5px; border: 1px solid #ddd; overflow: hidden; background-color: #FFF; z-index: 9;" class="minimapWrapperIdle">
					<img id="minimapImage" class="minimapImage" />
					<div id="minimapRadar" class="minimapRadar"></div>
			</div>
	</div>
</template>

<script>
import { DataSet, DataView } from 'vis-data'
import { Network } from 'vis-network'
const arrayDiff = (arr1, arr2) =>
	arr1.filter(x => arr2.indexOf(x) === -1)

export default {
	name: 'network',
	props: {
		edges: {
			type: [Array, DataSet, DataView],
			default: () => []
		},
		nodes: {
			type: [Array, DataSet, DataView],
			default: () => []
		},
		events: {
			type: Array,
			default: () => [
				'click',
				// 'doubleClick',
				'oncontext',
				// 'hold',
				// 'release',
				// 'select',
				// 'selectNode',
				// 'selectEdge',
				// 'deselectNode',
				// 'deselectEdge',
				'dragStart',
				// 'dragging',
				'dragEnd',
				// 'hoverNode',
				// 'blurNode',
				// 'hoverEdge',
				// 'blurEdge',
				'zoom',
				// 'showPopup',
				// 'hidePopup',
				// 'startStabilizing',
				// 'stabilizationProgress',
				// 'stabilizationIterationsDone',
				// 'stabilized',
				// 'resize',
				// 'initRedraw',
				// 'beforeDrawing',
				'afterDrawing',
				// 'animationFinished',
				// 'configChange'
			]
		},
		options: {
			type: Object,
			default: () => ({})
		}
	},
	data: () => ({
		visData: {
			nodes: [],
			edges: []
		},
		ratio: 5
	}),
	watch: {
		options: {
			deep: true,
			handler(o) {
				this.network.setOptions(o)
			}
		}
	},
	methods: {
		isOffscreen(nodeId) {
			const canvasWidth = this.network.canvas.frame.canvas.clientWidth
			const canvasHeight = this.network.canvas.frame.canvas.clientHeight
			const topLeft = this.domToCanvas({ x: 0, y: 0 })
			const bottomRight = this.domToCanvas({ x: canvasWidth, y: canvasHeight })
			const canvasBounds = { top: topLeft.y, left: topLeft.x, right: bottomRight.x, bottom: bottomRight.y }
			const bbox = this.getBoundingBox(nodeId)
			return (bbox.bottom < canvasBounds.top || bbox.right < canvasBounds.left || bbox.left > canvasBounds.right || bbox.top > canvasBounds.bottom)
		},
		getGraphImage() {
			return this.network.canvas.frame.canvas.toDataURL()
		},
		setData(n, e) {
			this.visData.nodes = Array.isArray(n) ? new DataSet(n) : n
			this.visData.edges = Array.isArray(e) ? new DataSet(e) : e
			this.network.setData(this.visData)
		},
		destroy() {
			this.network.destroy()
		},
		getNode(id) {
			return this.visData.nodes.get(id)
		},
		getEdge(id) {
			return this.visData.edges.get(id)
		},
		setOptions(options) {
			this.network.setOptions(options)
		},
		on(event, callback) {
			this.network.moveTo(event, callback)
		},
		off(event, callback) {
			this.network.moveTo(event, callback)
		},
		once(event, callback) {
			this.network.moveTo(event, callback)
		},
		canvasToDom(p) {
			return this.network.canvasToDOM(p)
		},
		domToCanvas(p) {
			return this.network.DOMtoCanvas(p)
		},
		redraw() {
			this.network.redraw()
		},
		setSize(w, h) {
			this.network.setSize(w, h)
		},
		cluster(options) {
			this.network.cluster(options)
		},
		clusterByConnection(nodeId, options) {
			this.network.clusterByConnection(nodeId, options)
		},
		clusterByHubsize(hubsize, options) {
			this.network.clusterByHubsize(hubsize, options)
		},
		clusterOutliers(options) {
			this.network.clusterOutliers(options)
		},
		findNode(id) {
			return this.network.findNode(id)
		},
		getClusteredEdges(baseEdgeId) {
			return this.network.clustering.getClusteredEdges(baseEdgeId)
		},
		getBaseEdge(clusteredEdgeId) {
			return this.network.clustering.getBaseEdge(clusteredEdgeId)
		},
		getBaseEdges(clusteredEdgeId) {
			return this.network.clustering.getBaseEdges(clusteredEdgeId)
		},
		updateEdge(startEdgeId, options) {
			this.network.clustering.updateEdge(startEdgeId, options)
		},
		updateClusteredNode(clusteredNodeId, options) {
			this.network.clustering.updateClusteredNode(clusteredNodeId, options)
		},
		isCluster(nodeId) {
			return this.network.isCluster(nodeId)
		},
		getNodesInCluster(clusterNodeId) {
			return this.network.getNodesInCluster(clusterNodeId)
		},
		openCluster(nodeId, options) {
			this.network.openCluster(nodeId, options)
		},
		getSeed() {
			return this.network.getSeed()
		},
		enableEditMode() {
			this.network.enableEditMode()
		},
		disableEditMode() {
			this.network.disableEditMode()
		},
		addNodeMode() {
			this.network.addNodeMode()
		},
		editNode() {
			this.network.editNode()
		},
		addEdgeMode() {
			this.network.addEdgeMode()
		},
		editEdgeMode() {
			this.network.editEdgeMode()
		},
		deleteSelected() {
			this.network.deleteSelected()
		},
		getPositions(nodeIds) {
			return this.network.getPositions(nodeIds)
		},
		storePositions() {
			this.network.storePositions()
		},
		moveNode(nodeId, x, y) {
			this.network.moveNode(nodeId, x, y)
		},
		getBoundingBox(nodeId) {
			return this.network.getBoundingBox(nodeId)
		},
		getConnectedNodes(nodeId, direction) {
			return this.network.getConnectedNodes(nodeId, direction)
		},
		getConnectedEdges(nodeId) {
			return this.network.getConnectedEdges(nodeId)
		},
		startSimulation() {
			this.network.startSimulation()
		},
		stopSimulation() {
			this.network.stopSimulation()
		},
		stabilize(iterations) {
			this.network.stabilize(iterations)
		},
		getSelection() {
			return this.network.getSelection()
		},
		getSelectedNodes() {
			return this.network.getSelectedNodes()
		},
		getSelectedEdges() {
			return this.network.getSelectedEdges()
		},
		getNodeAt(p) {
			return this.network.getNodeAt(p)
		},
		getEdgeAt(p) {
			return this.network.getEdgeAt(p)
		},
		selectNodes(nodeIds, highlightEdges) {
			this.network.selectNodes(nodeIds, highlightEdges)
		},
		selectEdges(edgeIds) {
			this.network.selectEdges(edgeIds)
		},
		setSelection(selection, options) {
			this.network.setSelection(selection, options)
		},
		unselectAll() {
			this.network.unselectAll()
		},
		getScale() {
			return this.network.getScale()
		},
		getViewPosition() {
			return this.network.getViewPosition()
		},
		fit(options) {
			this.network.fit(options)
		},
		focus(nodeId, options) {
			this.network.focus(nodeId, options)
		},
		moveTo(options) {
			this.network.moveTo(options)
		},
		releaseNode() {
			this.network.releaseNode()
		},
		getOptionsFromConfigurator() {
			return this.network.getOptionsFromConfigurator()
		},
		mountVisData(propName) {
			let data = this[propName]
			// If data is DataSet or DataView we return early without attaching our own events
			if (!(this[propName] instanceof DataSet || this[propName] instanceof DataView)) {
				data = new DataSet(this[propName])
				// Rethrow all events
				data.on('*', (event, properties, senderId) =>
					this.$emit(`${propName}-${event}`, { event, properties, senderId })
				)
				// We attach deep watcher on the prop to propagate changes in the DataSet
				const callback = value => {
					if (Array.isArray(value)) {
						const newIds = new DataSet(value).getIds()
						const diff = arrayDiff(this.visData[propName].getIds(), newIds)
						this.visData[propName].update(value)
						this.visData[propName].remove(diff)
					}
				}

				this.$watch(propName, callback, {
					deep: true
				})
		}

			// Emitting DataSets back
			this.$emit(`${propName}-mounted`, data)

			return data
		},
//****************************support for minimap
drawMinimapWrapper(){
  const {
    clientWidth,
    clientHeight
  } = this.network.body.container;
  const minimapWrapper = document.getElementById('minimapWrapper');
  const width = Math.round(clientWidth / this.ratio);
  const height = Math.round(clientHeight / this.ratio);

  minimapWrapper.style.width = `${width}px`;
  minimapWrapper.style.height = `${height}px`;
},
// Draw minimap Image
drawMinimapImage(){
	const originalCanvas = document.getElementsByTagName('canvas')[0]
	const minimapImage = document.getElementById('minimapImage')

  const {
    clientWidth,
    clientHeight
  } = this.network.body.container

  const tempCanvas = document.createElement('canvas')
  const tempContext = tempCanvas.getContext('2d')

  const width = Math.round((tempCanvas.width = clientWidth / this.ratio))
  const height = Math.round((tempCanvas.height = clientHeight / this.ratio))

  if (tempContext) {
    tempContext.drawImage(originalCanvas, 0, 0, width, height)
minimapImage.src = tempCanvas.toDataURL()
//	minimapImage.src = this.getGraphImage();
    minimapImage.width = width
    minimapImage.height = height
  }
},

// Draw minimap Radar
drawRadar(){
  const {
    clientWidth,
    clientHeight
  } = this.network.body.container
  const minimapRadar = document.getElementById('minimapRadar')
  const {
    targetScale
  } = this.network.view
  const scale = this.network.getScale()
  const translate = this.network.getViewPosition()
  minimapRadar.style.transform = `translate(${(translate.x / this.ratio) *
        targetScale}px, ${(translate.y / this.ratio) * targetScale}px) scale(${targetScale / scale})`
  minimapRadar.style.width = `${clientWidth / this.ratio}px`
  minimapRadar.style.height = `${clientHeight / this.ratio}px`
},

upDateMinimap(){
  const {
    clientWidth,
    clientHeight
  } = this.network.body.container;
  const width = Math.round(clientWidth / this.ratio);
  const height = Math.round(clientHeight / this.ratio);
  const minimapImage = document.getElementById('minimapImage');
  const minimapWrapper = document.getElementById('minimapWrapper');
  // Initial render
  if (!minimapImage.hasAttribute('src') || minimapImage.src === '') {
    if (!minimapWrapper.style.width || !minimapWrapper.style.height) {
      this.drawMinimapWrapper();
    }
    this.drawMinimapImage();
    this.drawRadar();
  } else if (
    minimapWrapper.style.width !== `${width}px` ||
    minimapWrapper.style.height !== `${height}px`
  ) {
    minimapImage.removeAttribute('src');
    this.drawMinimapWrapper();
    this.network.fit();
  } else {
    this.drawRadar();
  }
}
// Extra settings and cool effects :)
/*
this.network.on('resize', () => {
  network.fit();
})
network.on('dragStart', () => {
  const minimapWrapper = document.getElementById('minimapWrapper');
  minimapWrapper.classList.remove('minimapWrapperIdle');
  minimapWrapper.classList.add('minimapWrapperMove');
})
network.on('dragEnd', () => {
  const minimapWrapper = document.getElementById('minimapWrapper');
  minimapWrapper.classList.remove('minimapWrapperMove');
  minimapWrapper.classList.add('minimapWrapperIdle')
})
network.on('zoom', () => {
  const minimapWrapper = document.getElementById('minimapWrapper');
  minimapWrapper.classList.remove('minimapWrapperIdle');
  minimapWrapper.classList.add('minimapWrapperMove')
})
*/
//************************End minimap support

	},
	created() {
		// This should be a Vue data property, but Vue reactivity kinda bugs Vis.
		// See here for more: https://github.com/almende/vis/issues/2524
		this.network = null
	},
	mounted() {
		this.visData.nodes = this.mountVisData('nodes')
		this.visData.edges = this.mountVisData('edges')
		this.network = new Network(
			this.$refs.visualization,
			this.visData,
			this.options
		)

		this.events.forEach(eventName =>
			this.network.on(eventName, props =>
				this.$emit(eventName, props)
			)
		)
	},
	beforeDestroy() {
		this.network.destroy()
	}
}
</script>

<style lang="less">
.graph {
	border: 1px solid #ccc;
	border-radius: 4px;
	display: flex;
	flex-direction: column;

	.vis-network {
		display: flex;
		height: auto !important;
	}
	.vis-close {
		display: none !important;
	}

	.vis-navigation {
		position: absolute;
		top: 40px;
		margin-left: 10px;

		.vis-button:hover {
			box-shadow: none;
			transform: scale(1.15);
			transition: all 0.15s;

			&:before {
				color: #999;
			}
		}

		.vis-button {
			position: absolute;

			&.vis-up,
			&.vis-down,
			&.vis-left,
			&.vis-right,
			&.vis-zoomIn,
			&.vis-zoomOut,
			&.vis-zoomExtends	{
				background-image: none;
			}

			&:before {
				color: #ccc;
				font-family: FontAwesome;
				font-size: 26px;
				padding: 5px;
			}

			&.vis-up {
				top: 5px;
				left: 30px;

				&:before {
					content: "\f139";
				}
			}

			&.vis-down {
				top: 55px;
				left: 30px;

				&:before {
					content: "\f13a";
				}
			}

			&.vis-left {
				top: 30px;
				left: 0px;

				&:before {
					content: "\f137";
				}
			}

			&.vis-right {
				top: 30px;
				left: 60px;

				&:before {
					content: "\f138";
				}
			}

			&.vis-zoomIn {
				top: 105px;
				left: 45px;

				&:before {
					content: "\f055";
				}
			}

			&.vis-zoomOut {
				top: 105px;
				left: 15px;

				&:before {
					content: "\f056";
				}
			}

			&.vis-zoomExtends {
				top: 35px;
				left: 33px;

				&:before {
					content: "\f0b2";
					font-size: 19px;
				}
			}
		}
	}
}

.fullHeight {
	flex-grow: 1;
	display: flex;
}

.wrapper {
  display: flex;
  height: 400px;
}

#mynetwork {
  flex: 1;
  border: 1px solid #ddd;
}

.minimapRadar {
	opacity: .2;
  position: absolute;
  background-color: rgba(16, 84, 154, 0.26);
}

.minimapImage {
  position: absolute;
}

.minimapWrapperIdle {
	opacity: 1; /*	opacity: 0.2; */
  transition: opacity 0.5s;
}

.minimapWrapperMove {
  opacity: 0.95;
  transition: opacity 0.5s;
}

</style>


