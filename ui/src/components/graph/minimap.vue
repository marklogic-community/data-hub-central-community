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
			contain
    	>
			<div class="minimapRadar"
					id="minimapRadar"
					v-show="showMiniMap"
					:style="radarStyle"
			>
			</div>
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
export default {
	name: 'minimap',
	props: {

	},
	data: () => ({
		showMiniMap:false,
		hideRadar:false,
		imgSrc:'https://cdn.vuetifyjs.com/images/cards/sunshine.jpg',
		graphLayout:{
			position: { x: 0, y: 0 },
			scale: 1.0
		},
		radarStyle: {
    left: '0px',
		bottom: '0px',
		height: '300px',
		width: '300px'
  	}
	}),
	computed:{
		imgSrc: {
		get: function () { return this.imgSrc },
		set: function(url) { this.imgSrc = url }
		}
	},
	methods: {

	loadGraphLayoutSnap(graph) {
		//load graph's position, scale, etc
		graph.moveTo(this.graphLayout)
	},
	saveGraphLayoutSnap(graph) {
		this.graphLayout={
			position: graph.getViewPosition(),
			scale: graph.getScale(),
			positions: graph.getPositions()
		}
	},
	drawMinimapImage(graph){
		//copies the graph canvas to the minimap image
		const originalCanvas = graph.network.canvas.frame.canvas
		const minimapImage = document.getElementById('minimapImage')
		//set the minimap image url
		this.imgSrc = originalCanvas.toDataURL()
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
		//compute graphics unit ratio
		var ratioX = minimapImage.clientWidth/(clientWidth * scale)
		var ratioY = minimapImage.clientHeight/(clientHeight * scale)
		var minimapCenterPointX = minimapImage.clientWidth * 0.5
		var minimapCenterPointY =  minimapImage.clientHeight * 0.5
		var radarCenterPointX =  minimapCenterPointX + (translate.x * scale * ratioX)
		var radarCenterPointY =  minimapCenterPointY + (translate.y * scale * ratioY)
		//translate div to map coordinates
		const minimapRadar = document.getElementById('minimapRadar') //this is for debugging convenience
		this.radarStyle.left = String(radarCenterPointX - ((clientWidth  * ratioX) * 0.5)) + 'px'
		this.radarStyle.bottom = String(radarCenterPointY - ((clientHeight  * ratioY) * 0.5)) + 'px'

		this.radarStyle.width = String(clientWidth  * ratioX)+ 'px'
		this.radarStyle.height = String(clientHeight * ratioY)+ 'px'

		// this.radarStyle.left = '0px'
		// this.radarStyle.bottom = '0px'

		// this.radarStyle.width = '100px'
		// this.radarStyle.height = '100px'

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
		this.drawRadar(graph);
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


