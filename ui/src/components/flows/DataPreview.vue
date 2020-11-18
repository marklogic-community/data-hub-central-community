<template>
	<v-dialog
		v-model="open"
		width="700">
		<v-card>
			<v-card-title>Mapping Preview<v-spacer></v-spacer><v-icon data-cy="previewDlg.close" @click="close">close</v-icon></v-card-title>
			<v-card-text>
				<pre v-if="preview">{{preview}}</pre>
			</v-card-text>
		</v-card>
	</v-dialog>
</template>

<script>
export default {
	props: {
		preview: {}
	},
	data() {
		return {
			isOpen: null
		}
	},
	computed: {
		open: {
			get() {
				return this.isOpen || this.preview
			},
			set(val) {
				this.isOpen = val
			}
		}
	},
	methods: {
		close() {
			this.open = false
			this.$emit('closed')
		}
	}
}
</script>

<style lang="less" scoped>
pre {
	overflow: scroll;
}
</style>
