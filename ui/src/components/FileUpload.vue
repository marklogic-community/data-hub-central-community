<template>
	<div
		class="dropzone"
		:class="{highlight: hovering}"
		@dragover.prevent="dragOver"
		@drop.prevent="onDropFile"
		@dragleave.prevent="dragOut">
		<slot name="default">
		<div class="container">
			<div>Drag & Drop your Data File here</div>

			<div class="or">or...</div>
			<v-btn @click="chooseFile" color="primary">
				Choose a File
			</v-btn>
		</div>
		</slot>
	</div>
</template>

<script>
export default {
  components: {
  },
  computed: {
	},
	data() {
		return {
			hovering: false,
			chooseFileInput: null
		}
	},
	methods: {
		dragOver(event) {
			event.dataTransfer.dropEffect = 'copy'
			this.hovering = true
		},
		dragOut() {
			this.hovering = false
		},
		onDropFile(event) {
			this.hovering = false
			const dt = event.dataTransfer
			if (!dt) {
				return
			}

			if (dt.files.length > 0) {
				this.$emit('upload', dt.files)
			}
		},
		chooseFile() {
			this.chooseFileInput = document.createElement('input');
			this.chooseFileInput.type = 'file'
			this.chooseFileInput.accept = 'text/csv'
			this.chooseFileInput.addEventListener('change', () => {
				this.$emit('upload', this.chooseFileInput.files)
			})
			this.chooseFileInput.click()
		}
	}
};
</script>

<style lang="less" scoped>
.dropzone {
	min-height: 100px;
	min-width: 200px;
	border: 4px dashed black;
	border-radius: 10px;
	display: flex;
	justify-content: center;
	flex-direction: column;
	padding: 20px;
	width: 100%;
	font-weight: bold;
	text-align: center;

	&.highlight {
		border: 4px dashed red;
	}
}

.container > div {
	margin-bottom: 1em;
}
</style>
