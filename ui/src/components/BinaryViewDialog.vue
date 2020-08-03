<template>
	<v-dialog
		v-model="open"
		width="650"
		persistent
		@keydown.esc="close">
		<v-card>
			<v-card-title>
				<v-tooltip bottom>
					<template v-slot:activator="{ on }">
						<v-btn
							data-cy="binaryViewer.detailsBtn"
							:to="{name: 'root.details', query: { uri }}"
							left icon small class="small-btn" v-on="on">
							<v-icon>list_alt</v-icon>
						</v-btn>
					</template>
					<span>View Full Document</span>
				</v-tooltip>
				{{uri}}
				<v-spacer></v-spacer><v-icon @click="close">close</v-icon></v-card-title>
			<v-card-text>
				<binary-view :src="uri" :type="contentType" :title="fileName"/>
			</v-card-text>
		</v-card>
	</v-dialog>
</template>

<script>
import BinaryView from '@/components/BinaryView.vue';

export default {
	props: {
		uri: { type: String },
		contentType: { type: String },
		fileName: { type: String },
		showDialog: { type: Boolean }
	},
	components: {
		BinaryView
	},
	computed: {
		open: {
			get() {
				return this.isOpen || this.showDialog
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
/deep/ .v-dialog {
	display: flex;
	flex-direction: column;
	overflow: hidden;

	.v-card {
		display: flex;
		height: 100%;
		flex-direction: column;
		flex: 1;
		overflow: hidden;
	}
	.v-card__text {
		overflow-y: auto;
		height: 100%;
	}
}
</style>
