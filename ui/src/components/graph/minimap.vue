<template >

	<v-card
    class="mx-auto"
    max-width="344"
  >
	<v-expand-transition>
		<v-img
			v-show="showMiniMap"
			id="minimapImage"
      :src="this.miniMapImgSrc"
			contain
    	>
			<div class="minimapRadar"
					id="minimapRadar"
					v-show="showMiniMapRadar"
					v-bind:style="radarStyle"
			>
			</div>
		</v-img>
		</transition>
	</v-expand-transition>
	<v-card-actions>
		<label class="menu-label v-label v-label--active theme--light">Map</label>
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
export default {
	name: 'minimap',
	props: {

	},
	data: () => ({
		showMiniMap:false,
		showMiniMapRadar:true,
		imgSrc: '', //'https://cdn.vuetifyjs.com/images/cards/sunshine.jpg',
		graphLayout:{
			position: { x: 0, y: 0 },
			scale: 1.0,
			positionDom: { x: 0, y: 0 }
		},
		graphCanvas:{
			canvasWidth: 0,
			canvasHeight: 0,
			topLeft: { x: 0, y: 0 },
			bottomRight: { x: 0, y: 0 },
			canvasBounds: { top: 0, left: 0, right: 0, bottom: 0 }
		},
		radarStyle: {
			position:'absolute',
			left: '0px',
			bottom: '0px',
			height: '0px',
			width: '0px'
		},
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
	handleZoom(e){
			var myEvent = e;
			//console.log(myEvent)
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
		//calculate current in canvas space
		//load the scale, position the user was viewing
		//we captured it in upDateMiniMap
		//TODO make this an inline function in upDateMinimap
		const scale = this.graphLayout.scale
		const translate = this.graphLayout.position

		//zoomed out dimensions (graph should be zoomed at scale of ~1.0)
		const topLeftZoomed = graph.domToCanvas({ x: 0, y: 0 })
		const canvasWidthZoomed = graph.network.canvas.frame.canvas.clientWidth
		const canvasHeightZoomed = graph.network.canvas.frame.canvas.clientHeight
		const bottomRightZoomed = graph.domToCanvas({ x: canvasWidthZoomed, y: canvasHeightZoomed })

		const minimapImage = document.getElementById('minimapImage');
		//compute scale of map canvas to zoomed out canvas
		var ratioX = minimapImage.clientWidth/(bottomRightZoomed.x - topLeftZoomed.x)
		var ratioY = minimapImage.clientHeight/(topLeftZoomed.y - bottomRightZoomed.y)

		//move camera in minimap to center
		var minimapCenterPointX = minimapImage.clientWidth * 0.5
		var minimapCenterPointY =  minimapImage.clientHeight * 0.5

		//translate minimap centerpoint to original graph position
		var radarCenterPointX =  ((translate.x - this.graphCanvas.topLeft.x) * ratioX)
		var radarCenterPointY =  (((translate.y - this.graphCanvas.bottomRight.y) * -1) * ratioY)  //flipped coords-- up = < 0
		var radarWidth = (this.graphCanvas.canvasWidth/scale) * ratioX
		var radarHeight = (this.graphCanvas.canvasHeight/scale) * ratioY

		this.radarStyle.left = String(Math.round((this.graphCanvas.topLeft.x -  topLeftZoomed.x) * ratioX)) + 'px'
		this.radarStyle.bottom = String(Math.round(this.graphCanvas.bottomRight.y * ratioY * -1)) + 'px'
	this.radarStyle.bottom = '0px'
		//this.radarStyle.right = String(this.graphCanvas.bottomRight.x * ratioX) + 'px'
		//this.radarStyle.top = String(this.graphCanvas.topLeft.y * ratioY * -1) + 'px'

		this.radarStyle.width = String(Math.round((this.graphCanvas.bottomRight.x - this.graphCanvas.topLeft.x) * ratioX )) + 'px'
		this.radarStyle.height = String(Math.round(((this.graphCanvas.topLeft.y - this.graphCanvas.bottomRight.y) * -1 ) * ratioY )) + 'px'
	this.radarStyle.height =  '300px'

	},
	upDateMinimap(graph){
		//save user state of the graph
		this.saveGraphLayoutSnap(graph)
		let fitOptions = graph.options;
		//prep graph for fit call
		fitOptions.physics.enabled = true;
		graph.setOptions(fitOptions)
		//fit draws a zoomed out graph wih all nodes
		graph.fit(graph.nodes);
		//NOTE see below- if you call this here, the graph will not be zoomed out
		//a vis-graph thing?
		//this.drawMinimapImage(graph);

		//reset options
		fitOptions.physics.enabled = false;
		graph.setOptions(fitOptions)

		//capture the image of the zoomed out graph-- NOTE this happens after the setOptions
		//call. If called before the graph is not zoomed out
		this.drawMinimapImage(graph);
		//draw the radar
		if( this.showMiniMapRadar ) this.drawRadar(graph);

		//restore the state of the graph
		this.loadGraphLayoutSnap(graph)

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


