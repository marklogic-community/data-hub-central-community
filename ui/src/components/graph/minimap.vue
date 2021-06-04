<template >

	<v-card
    class="mx-auto"
    max-width="344"
  >
	<v-expand-transition>
		<v-img
			v-show="showMiniMap"
			id="minimapImage"
			data-cy="minimapVue.mapImage"
      :src="this.miniMapImgSrc"
			contain
			@click="handleImgClick"
			@load="handleMapImgLoad"
    	>
			<div class="minimapRadar"
					id="minimapRadar"
					v-show="showMiniMapRadar"
					v-bind:style="radarStyle"
			>
			</div>
		</v-img>
	</v-expand-transition>
	<v-card-actions>
		<label class="menu-label v-label v-label--active theme--light">Map</label>
		<v-spacer></v-spacer>
		<v-btn
			icon
			@click="handleDiscloseButtonClick"
			data-cy="minimapVue.openMapButton"
		>
			<v-icon>{{ showMiniMap ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
		</v-btn>
	</v-card-actions>
</v-card>
</template>

<script>
export default {
	name: 'minimap',
	props: {
		graph:{}
	},
	data: () => ({
		showMiniMap:false,
		showMiniMapRadar:true,
		imgSrc: '', //'https://cdn.vuetifyjs.com/images/cards/sunshine.jpg',
		//snapshot of the camera position
		graphLayout:{
			position: { x: 0, y: 0 },
			scale: 1.0
		},
		//keep a snapshot of the graph viewport
		graphCanvas:{
			canvasWidth: 0,
			canvasHeight: 0,
			topLeft: { x: 0, y: 0 },
			bottomRight: { x: 0, y: 0 },
			canvasBounds: { top: 0, left: 0, right: 0, bottom: 0 }
		},
		//keep a snapshot of the last time we checked the full expanse of the graph
		graphFullCanvas:{
			canvasWidth: 0,
			canvasHeight: 0,
			topLeft: { x: 0, y: 0 },
			bottomRight: { x: 0, y: 0 },
			canvasBounds: { top: 0, left: 0, right: 0, bottom: 0 }
		},
		//positioning of the map overlay showing the viewport
		radarStyle: {
			position:'absolute',
			left: '0px',
			bottom: '0px',
			height: '0px',
			width: '0px'
		},
		//animation transition keys
		minimapImageKey:'',
		minimapRadarKey:''
	}),
	computed:{
		miniMapImgSrc: {
		get: function () { return this.imgSrc },
		set: function(url) { this.imgSrc = url }
		}
	},
	methods: {
	getRadarStyle(){
		return this.radarStyle
	},
	handleMapImgLoad(){
	},
	//use the two graph states
	//graphFullCanvas represents the canvas of the full graph in canvas coordinates
	//graphCanvas represents the user's viewport on the graph (camera position, scale) in canvas coordinates
	setRadarSizeFromStoredGraphStates(){
		//draw the radar
		const minimapImage = document.getElementById('minimapImage');
		if(minimapImage.clientWidth > 0 && minimapImage.clientHeight > 0){
			//compare canvas dimensions of captured viewport to full graph canvas
			//update radar styles with new dimensions
			//compute scale of map canvas to zoomed out canvas
			var ratioX = minimapImage.clientWidth/(this.graphFullCanvas.bottomRight.x - this.graphFullCanvas.topLeft.x)
			var ratioY = minimapImage.clientHeight/(this.graphFullCanvas.topLeft.y - this.graphFullCanvas.bottomRight.y)

			this.radarStyle.left = String(Math.round((this.graphCanvas.topLeft.x -  this.graphFullCanvas.topLeft.x ) * ratioX)) + 'px'
			this.radarStyle.bottom = String(Math.round((this.graphCanvas.bottomRight.y - this.graphFullCanvas.bottomRight.y) * ratioY )) + 'px'

			this.radarStyle.width = String(Math.round((this.graphCanvas.bottomRight.x - this.graphCanvas.topLeft.x) * ratioX )) + 'px'
			this.radarStyle.height = String(Math.round((this.graphCanvas.topLeft.y - this.graphCanvas.bottomRight.y) * ratioY )) + 'px'
		}
	},
	handleDiscloseButtonClick(){
		this.showMiniMap = !this.showMiniMap;
		if (this.showMiniMap){
			//initialize minimap if there is no image
			if(this.imgSrc === ''){
				//update the minimap
				this.upDateMinimap(this.graph)
			}
		}
	},
	handleZoom(e){

	},
	handleImgClick(e){
		const{
			offsetX,
			offsetY
		} = e
		const minimapImage = document.getElementById('minimapImage');
		//compute scale of map canvas to zoomed out canvas
		var ratioX = (this.graphFullCanvas.bottomRight.x - this.graphFullCanvas.topLeft.x)/minimapImage.clientWidth
		var ratioY = (this.graphFullCanvas.topLeft.y - this.graphFullCanvas.bottomRight.y)/minimapImage.clientHeight
		//translate to canvas space
		var canvasX = this.graphFullCanvas.topLeft.x + (offsetX * ratioX)
		var canvasY = this.graphFullCanvas.topLeft.y - (offsetY * ratioY)
		//move the graph
		var positions = {
		  position: this.graph.getViewPosition(),
			scale: this.graph.getScale(),
			positions: this.graph.getPositions(),
			animation: true
		}
		positions.position.x = canvasX
		positions.position.y = canvasY
		this.graph.moveTo(positions)
	},
	loadGraphLayoutSnap(graph) {
		//load graph's position, scale, etc
		graph.moveTo(this.graphLayout)
	},
	saveGraphLayoutSnap(graph) {
		const canvasWidth = graph.network.canvas.frame.canvas.clientWidth
		const canvasHeight = graph.network.canvas.frame.canvas.clientHeight
		const topLeft = graph.domToCanvas({ x: 0, y: 0 })
		const bottomRight = graph.domToCanvas({ x: canvasWidth, y: canvasHeight })
		this.graphCanvas={
			canvasWidth: canvasWidth,
			canvasHeight: canvasHeight,
			topLeft: topLeft,
			bottomRight: bottomRight,
			canvasBounds: { top: topLeft.y, left: topLeft.x, right: bottomRight.x, bottom: bottomRight.y }
		}
		this.graphLayout={
			position: graph.getViewPosition(),
			scale: graph.getScale(),
			positions: graph.getPositions(),
		}
	},
	drawMinimapImage(graph){
		//set the minimap image url
		this.miniMapImgSrc = this.minimapImageKey = graph.getGraphImage()  //originalCanvas.toDataURL()
	},
	// Draw minimap Radar
	drawRadar(graph){
		//PRE-REQ: graph should be zoomed at scale of ~1.0
		//TODO: qualify the graph is zoomed out
		//calculate the canvas dimensions for the full graph
		const topLeftZoomed = graph.domToCanvas({ x: 0, y: 0 })
		const canvasWidthZoomed = graph.network.canvas.frame.canvas.clientWidth
		const canvasHeightZoomed = graph.network.canvas.frame.canvas.clientHeight
		const bottomRightZoomed = graph.domToCanvas({ x: canvasWidthZoomed, y: canvasHeightZoomed })

		//store a snapshot of the full expanse of the graph
		this.graphFullCanvas.topLeft = topLeftZoomed;
		this.graphFullCanvas.bottomRight = bottomRightZoomed;

		//handleImgLoad processes the saved graph states
		this.setRadarSizeFromStoredGraphStates()
	},
	upDateMinimap(graph){
		//save user state of the graph
		//this is zoom and position user has set
		this.saveGraphLayoutSnap(graph)
		let fitOptions = graph.options;
		//prep graph for fit call
		fitOptions.physics.enabled = true;
		graph.setOptions(fitOptions)
		//fit draws a zoomed out graph to show all nodes
		graph.fit(graph.nodes);
		//NOTE see below- if you call this here, the graph will not be zoomed out
		//a vis-graph thing?
		//this.drawMinimapImage(graph);

		//reset options
		fitOptions.physics.enabled = false;
		graph.setOptions(fitOptions)

		//draw the minimap image of the zoomed out graph-- NOTE this happens after the setOptions
		//call. If called before the graph is not zoomed out
		this.drawMinimapImage(graph);
		//draw the radar
		//NOTE we still need the graph zoomed out to fit here
		if( this.showMiniMapRadar ) this.drawRadar(graph);

		//restore the state of the graph
		this.loadGraphLayoutSnap(graph)

	}
}
}
</script>

<style lang="less">

.minimapRadar {
	opacity: .2;
  position: absolute;
	background-color: rgba(16, 84, 154, 0.26);
	z-index: 1000;
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
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

</style>


