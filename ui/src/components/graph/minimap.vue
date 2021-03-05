<template>

	 <v-card
    class="mx-auto"
    max-width="344"
  >
    <v-expand-transition>
			<v-img
			v-show="showMiniMap"
			id="minimapImage"
      :src="this.imgSrc"
      height="200px"
    	>
			</v-img>
		</v-expand-transition>

    <v-card-actions>
      <v-btn
        light
        text
      >
        Minimap
      </v-btn>

      <v-spacer></v-spacer>

      <v-btn
        icon
        @click="showMiniMap = !showMiniMap"
      >
        <v-icon>{{ showMiniMap ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
      </v-btn>
    </v-card-actions>

  </v-card>
</template>

<script>
const arrayDiff = (arr1, arr2) =>
	arr1.filter(x => arr2.indexOf(x) === -1)

export default {
	name: 'minimap',
	emits: ['saveGraphLayoutSnap', 'loadGraphLayoutSnap'],
	props: {

	},
	data: () => ({
		graph:{},
		graphLayout: {},
		dialog:false,
		show: false,
		showMiniMap:false,
		ratio: 3,
		imgSrc:'https://cdn.vuetifyjs.com/images/cards/sunshine.jpg'
	}),
	computed:{
		imgSrc: {
		get: function () { return this.imgSrc },
		set: function(url) { this.imgSrc = url }
		}
	},
	methods: {
		//****************************support for minimap
getNetwork(){ return /*this.$parent.network*/ this.graph },
loadGraphLayoutSnap(graph) {
	const key = `snap-layout-${this._uid}`
	const item = localStorage.getItem(key)
	var graphLayout = item ? JSON.parse(item) : {
		position: { x: 0, y: 0 },
		scale: 1.0
	}
	graph.moveTo(graphLayout)
},
saveGraphLayoutSnap(graph) {
	const key = `snap-layout-${this._uid}`
	localStorage.setItem(key, JSON.stringify({
		position: graph.getViewPosition(),
		scale: graph.getScale(),
		positions: graph.getPositions()
	}))
},
drawMinimapImage2(graph){
	//we need to create the save/restore graphics logic here in the component
	//const originalCanvas = document.getElementsByTagName('canvas')[0] //this is the graph canvas but we really need a btter way of targeting it
	const originalCanvas = graph.network.canvas.frame.canvas
	const minimapImage = document.getElementById('minimapImage')
	//set the minimap image url
	this.imgSrc = originalCanvas.toDataURL()

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
  this.drawMinimapImage2();
},
upDateMinimap2(){
	this.saveGraphLayoutSnap()
	let fitOptions = this.graph.options;
	fitOptions.physics.enabled = true;
	this.graph.setOptions(fitOptions)
	this.graph.fit(this.nodes);
	fitOptions.physics.enabled = false;
	this.graph.setOptions(fitOptions)
	this.drawMinimapImage2();
	this.loadGraphLayoutSnap()
//  this.drawRadar();
},
upDateMinimap(graph){
	this.saveGraphLayoutSnap(graph)
	let fitOptions = graph.options;
	fitOptions.physics.enabled = true;
	graph.setOptions(fitOptions)
	graph.fit(graph.nodes);
	fitOptions.physics.enabled = false;
	graph.setOptions(fitOptions)
	this.drawMinimapImage2(graph);
	this.loadGraphLayoutSnap(graph)
//  this.drawRadar();
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
	height: 300px;
	width: 300px;
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


