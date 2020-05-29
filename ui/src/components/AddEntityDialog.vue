<template>
	<v-dialog
		v-model="dialog"
		persistent
		eager
		max-width="340"
		@keydown.esc="cancel">
		<add-entity
			ref="addEntity"
			:existingEntityNames="existingEntityNames"
			@save="save"
			@cancel="cancel"
		></add-entity>
	</v-dialog>
</template>

<script>
import AddEntity from '@/components/AddEntity.vue';

export default {
	data: () => ({
		existingEntityNames: null,
		dialog: false,
		resolve: null,
		reject: null
	}),
	components: {
		AddEntity
	},
	methods: {
		open(existingEntityNames) {
			this.existingEntityNames = existingEntityNames || []
			this.dialog = true
			this.$refs.addEntity.reset()
			return new Promise((resolve, reject) => {
				this.resolve = resolve
				this.reject = reject
			})
		},
		save(resp) {
			this.resolve(resp)
			this.dialog = false
		},
		cancel() {
			this.resolve({type: null, name: null})
			this.dialog = false
		}
	}
}
</script>
