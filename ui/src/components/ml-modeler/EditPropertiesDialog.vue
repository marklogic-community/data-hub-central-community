<template>
	<v-dialog
		v-model="dialog"
		persistent
		eager
		max-width="600"
		@keydown.esc="close">
		<div class="card-holder">
			<v-card>
				<v-card-title class="primary--text">Edit {{entity.entityName}} Properties<v-spacer></v-spacer><v-icon @click="close">close</v-icon></v-card-title>
				<v-card-text>
					<edit-properties
						:entity="entity"
						:properties="properties"
					></edit-properties>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text @click="close">Close</v-btn>
				</v-card-actions>
			</v-card>
		</div>
	</v-dialog>
</template>

<script>
import EditProperties from '@/components/ml-modeler/EditProperties.vue';

export default {
	data: () => ({
		entity: {},
		properties: [],
		dialog: false,
		resolve: null,
		reject: null
	}),
	components: {
		EditProperties
	},
	methods: {
		open(entity, properties) {
			this.entity = entity || {}
			this.properties = properties || []
			this.dialog = true
			return new Promise((resolve, reject) => {
				this.resolve = resolve
				this.reject = reject
			})
		},
		save(resp) {
			this.resolve(resp)
			this.dialog = false
		},
		close() {
			this.resolve({type: null, name: null})
			this.dialog = false
		}
	}
}
</script>
<style lang="less" scoped>
/deep/ .v-dialog {
	display: flex;
	flex-direction: column;
	overflow: hidden;

	.card-holder {
		height: 100%;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}
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

	.fullheight {
		height: 100%;
	}

	/deep/ .v-data-table__wrapper {
		overflow-y: auto;
	}
	/deep/ .v-data-table {
		display: flex;
		flex-direction: column;
		flex: 1 0 auto;
		height: 100%;
	}
}
</style>
