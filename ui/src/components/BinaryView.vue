<template>
	<div class="view-binary">
		<div class="bin-data" v-if="uri && type">
			<audio v-if="type.match(/^audio\//)" controls preload="metadata" :src="uri" :title="title">
				<slot id="fallback" name="fallback"></slot>
			</audio>
			<video v-else-if="type.match(/^video\//)" controls preload="metadata" :src="uri" title="" playsinline>
			</video>
			<picture v-else-if="type.match(/^image\//)">
				<img :src="uri" alt="" :title="title">
			</picture>
			<object v-else :data="uri" :type="type" :title="title">
			</object>
		</div>
		<div v-else id="view-binary-loading">
			<slot name="loading">
				Loading... <i class="fa fa-refresh fa-spin"></i>
			</slot>
		</div>
	</div>
</template>

<script>
export default {
	props: {
		src: { type: String },
		type: { type: String },
		title: { type: String }
	},
	computed: {
		uri() {
			return `/api/crud?uri=${encodeURIComponent(this.src)}&token=${localStorage.getItem('access_token')}`
		}
	},
	methods: {
		confirm() {
			this.$emit('confirm')
		},
		cancel() {
			this.$emit('cancel')
		}
	}
}
</script>

<style lang="less" scoped>
.view-binary {
	height: 100%;

	.bin-data {
		height: 100%;
		display: flex;
		flex-direction: column;
	}
}

img {
	width: 100%;
}

object {
	width: 100%;
	height: 100%;
	min-height: 500px;
}

video {
	max-width: 100%;
	max-height: 500px;
}
</style>
