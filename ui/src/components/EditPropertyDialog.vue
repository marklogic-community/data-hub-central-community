<template>
	<v-dialog
		v-model="dialog"
		persistent
		eager
		max-width="600"
		@keydown.esc="close">
		<edit-property
			:visible="true"
			:existingProperties="existingProperties"
			:prop="prop"
			:entityName="entityName"
			ref="editProperty"
			@save="save($event)"
			@cancel="close()"
		></edit-property>
	</v-dialog>
</template>

<script>
import EditProperty from '@/components/EditProperty.vue';

export default {
	data: () => ({
		prop: {},
		entityName: null,
		existingProperties: [],
		dialog: false,
		resolve: null,
		reject: null
	}),
	components: {
		EditProperty
	},
	methods: {
		open({prop, entityName, existingProperties}) {
			this.prop = prop
			this.entityName = entityName
			this.existingProperties = existingProperties
			this.$nextTick(() => {
				this.dialog = true
				this.$refs.editProperty.reset()
			})
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
			this.reject()
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

	form {
		height: 100%;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}
	/deep/ .v-card {
		display: flex;
		height: 100%;
		flex-direction: column;
		flex: 1;
		overflow: hidden;
	}
	/deep/ .v-card__text {
		overflow-y: auto;
		height: 100%;
	}

	/deep/ .fullheight {
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
