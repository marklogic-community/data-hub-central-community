<template>
		<div ref="minimapwrapper" id="minimapWrapper" style="margin: 5px; border: 1px solid #ddd; overflow: hidden; background-color: #FFF; z-index: 9;" class="minimapWrapperIdle">
				<visjs-graph
							:nodes="nodes"
							:edges="edges"
							:options="graphOptions"
							layout="standard"
							ref="minimapgraph"
						>
				<div id="minimapRadar" class="minimapRadar"></div>
				</visjs-graph>
		</div>
</template>

<script>
import { DataSet, DataView } from 'vis-data'
import { Network } from 'vis-network'
const arrayDiff = (arr1, arr2) =>
	arr1.filter(x => arr2.indexOf(x) === -1)

export default {
	name: 'minimap',
	props: {
		network: {},
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
				//'click',
				// 'doubleClick',
				//'oncontext',
				// 'hold',
				// 'release',
				// 'select',
				// 'selectNode',
				// 'selectEdge',
				// 'deselectNode',
				// 'deselectEdge',
				//'dragStart',
				// 'dragging',
				//'dragEnd',
				// 'hoverNode',
				// 'blurNode',
				// 'hoverEdge',
				// 'blurEdge',
				//'zoom',
				// 'showPopup',
				// 'hidePopup',
				// 'startStabilizing',
				// 'stabilizationProgress',
				// 'stabilizationIterationsDone',
				// 'stabilized',
				// 'resize',
				// 'initRedraw',
				// 'beforeDrawing',
				//'afterDrawing',
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
	methods: {
		//****************************support for minimap
getNetwork(){ return this.$parent.network },
setNetworkFromParent( ){this.network = this.getNetwork() },
drawMinimapWrapper(){
  const {
    clientWidth,
    clientHeight
  } = this.getNetwork().body.container;
  const minimapWrapper = document.getElementById('minimapWrapper');
  const width = Math.round(clientWidth / this.ratio);
  const height = Math.round(clientHeight / this.ratio);

  minimapWrapper.style.width = `${width}px`;
  minimapWrapper.style.height = `${height}px`;
},
// Draw minimap Image
//we want to draw the network fully zoomed out in the mimimap
//then radar represents the user's zoomed in port
drawMinimapImage(){
	const originalCanvas = document.getElementsByTagName('canvas')[0]
	const minimapImage = document.getElementById('minimapImage')

  const {
    clientWidth,
    clientHeight
  } = this.getNetwork().body.container

  const tempCanvas = document.createElement('canvas')
  const tempContext = tempCanvas.getContext('2d')

  const width = Math.round((tempCanvas.width = clientWidth / this.ratio))
  const height = Math.round((tempCanvas.height = clientHeight / this.ratio))

  if (tempContext) {
    tempContext.drawImage(originalCanvas, 0, 0, width, height)
		minimapImage.src = tempCanvas.toDataURL()
    minimapImage.width = width
    minimapImage.height = height
  }
},
drawMinimapImage2(){
	const originalCanvas = document.getElementsByTagName('canvas')[0]
	const minimapImage = document.getElementById('minimapImage')

  const {
    clientWidth,
    clientHeight
  } = this.getNetwork().body.container

  const tempCanvas = document.createElement('canvas')
  const tempContext = tempCanvas.getContext('2d')

  const width = Math.round((tempCanvas.width = clientWidth / this.ratio))
  const height = Math.round((tempCanvas.height = clientHeight / this.ratio))

  if (tempContext) {
    tempContext.drawImage(originalCanvas, 0, 0, width, height)
		minimapImage.src = tempCanvas.toDataURL()
    minimapImage.width = width
    minimapImage.height = height
  }
},
// Draw minimap Radar
drawRadar(){
  const {
    clientWidth,
    clientHeight
  } = this.getNetwork().body.container
  const minimapRadar = document.getElementById('minimapRadar')
  const {
    targetScale
  } = this.getNetwork().view
  const scale = this.getNetwork().getScale()
  const translate = this.getNetwork().getViewPosition()
  minimapRadar.style.transform = `translate(${(translate.x / this.ratio) *
        targetScale}px, ${(translate.y / this.ratio) * targetScale}px) scale(${targetScale / scale})`
  minimapRadar.style.width = `${clientWidth / this.ratio}px`
  minimapRadar.style.height = `${clientHeight / this.ratio}px`
},
upDateMinimap1(){
  const {
    clientWidth,
    clientHeight
  } = this.getNetwork().body.container;
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
    this.getNetwork().fit();
  } else {
    this.drawRadar();
  }
},
upDateMinimap(){
	this.initMinimapGraph()
  	this.drawMinimapWrapper();
		//this.drawMinimapImage();
		this.copyDataFromParent();
		this.minimap.fit();
    this.drawRadar();
},
zoomMinimap()
{
	const minimapWrapper = document.getElementById('minimapWrapper');
  minimapWrapper.classList.remove('minimapWrapperIdle');
  minimapWrapper.classList.add('minimapWrapperMove')
},
dragStartMinimap()
{
	const minimapWrapper = document.getElementById('minimapWrapper');
  minimapWrapper.classList.remove('minimapWrapperIdle');
  minimapWrapper.classList.add('minimapWrapperMove');
},
dragEndMinimap()
{
	const minimapWrapper = document.getElementById('minimapWrapper');
  minimapWrapper.classList.remove('minimapWrapperMove');
  minimapWrapper.classList.add('minimapWrapperIdle')
},
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
initMinimapGraph()
{
//	this.copyDataFromParent()
		this.visData.nodes = this.mountVisData('nodes')
		this.visData.edges = this.mountVisData('edges')
		this.minimap = new Network(
			this.$refs.minimapwrapper,
			this.visData,
			this.options
		)

},
copyDataFromParent()
	{
		const parentnetwork = this.getNetwork()
		this.setData(parentnetwork.body.nodes, parentnetwork.body.edges)
	},
	setData(n, e) {
		this.visData.nodes = Array.isArray(n) ? new DataSet(n) : n
		this.visData.edges = Array.isArray(e) ? new DataSet(e) : e
	//	this.minimap.setData(this.visData)
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
		}
},
	created() {

	},

	mounted() {
//		this.initMinimapGraph()
	},
	beforeDestroy() {
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


