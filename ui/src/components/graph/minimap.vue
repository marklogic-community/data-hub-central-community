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
			<!-- <v-overlay -->
			<div class="minimapRadar"
				absolute
				color="#036358"
				id="minimapRadar"
				v-show="!hideRadar"
			>
			</div>
			<!-- </v-overlay> -->
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
		const {
			clientWidth,
			clientHeight
		} = graph.network.canvas.frame.canvas
		const minimapRadar = document.getElementById('minimapRadar')
		const {
			targetScale
		} = graph.network.view
//		const scale = graph.getScale()
		const scale = this.graphLayout.scale
//		const translate = graph.getViewPosition()
		const translate = this.graphLayout.position
		const minimapImage = document.getElementById('minimapImage');
		var ratio = clientWidth/minimapImage.clientWidth
		minimapRadar.style.transform = `translate(${(translate.x / ratio) *
					targetScale}px, ${(translate.y / ratio) * targetScale}px) scale(${targetScale / scale})`
		minimapRadar.style.width = `${clientWidth / ratio}px`
		minimapRadar.style.height = `${clientHeight / ratio}px`
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
		fitOptions.physics.enabled = false;
		graph.setOptions(fitOptions)
		this.drawMinimapImage(graph);
		this.loadGraphLayoutSnap(graph)
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


