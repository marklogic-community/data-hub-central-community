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
					:style="radarStyle"
			>
			</div>
		</v-img>
		</transition>
	</v-expand-transition>
	<v-card-actions>
		<label class="menu-label v-label v-label--active theme--light">Minimap</label>
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
		showMiniMapRadar:false,
		imgSrc: '', //'https://cdn.vuetifyjs.com/images/cards/sunshine.jpg',
		graphLayout:{
			position: { x: 0, y: 0 },
			scale: 1.0,
			positionDom: { x: 0, y: 0 }
		},
		radarStyle: {
    left: '0px',
		bottom: '0px',
		height: '300px',
		width: '300px'
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
	handleZoom(e){
			var myEvent = e;
			console.log(myEvent)
	},
	loadGraphLayoutSnap(graph) {
		//load graph's position, scale, etc
		graph.moveTo(this.graphLayout)
	},
	saveGraphLayoutSnap(graph) {
		this.graphLayout={
			position: graph.getViewPosition(),
			scale: graph.getScale(),
			positions: graph.getPositions(),
			positionDom: graph.canvasToDom(graph.getViewPosition())
		}
	},
	drawMinimapImage(graph){
		//copies the graph canvas to the minimap image
		const originalCanvas = graph.network.canvas.frame.canvas
		const minimapImage = document.getElementById('minimapImage')
		//set the minimap image url
		this.miniMapImgSrc = this.minimapImageKey = graph.getGraphImage()  //originalCanvas.toDataURL()
	},
	// Draw minimap Radar
	drawRadar(graph){
		//size of the graph canvas
		const {
			clientWidth,
			clientHeight
		} = graph.network.canvas.frame.canvas

		//load the scale, position the user was viewing
		//we captured it in upDateMiniMap
		//TODO make this an inline function in upDateMinimap
		const scale = this.graphLayout.scale
		const translate = this.graphLayout.position
		const minimapImage = document.getElementById('minimapImage');
		//compute graphics unit
		var ratioX = minimapImage.clientWidth/(clientWidth * scale)
		var ratioY = minimapImage.clientHeight/(clientHeight * scale)

		//move camera in minimap to center
		var minimapCenterPointX = minimapImage.clientWidth * 0.5
		var minimapCenterPointY =  minimapImage.clientHeight * 0.5

		//translate minimap centerpoint to original graph position
		var radarCenterPointX =  minimapCenterPointX + ((translate.x)/scale)
		var radarCenterPointY =  minimapCenterPointY + ((translate.y * -1)/scale)  //flipped coords-- up = < 0

		const minimapRadar = document.getElementById('minimapRadar') //this is for debugging convenience
		this.radarStyle.left = String(radarCenterPointX * 0.5) + 'px'
		this.radarStyle.bottom = String(radarCenterPointY * 0.5) + 'px'

		this.radarStyle.width = String((clientWidth/scale) * ratioX)+ 'px'
		this.radarStyle.height = String((clientHeight/scale) * ratioY)+ 'px'
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
		//restore the state of the graph
		this.loadGraphLayoutSnap(graph)
		//draw the radar
		if( this.showMiniMapRadar ) this.drawRadar(graph);
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


