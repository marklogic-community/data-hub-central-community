<template>
	<v-dialog
		v-model="dialog"
		persistent
		eager
		max-width="350"
		@keydown.esc="cancel">
		<edit-relationship
			ref="addRel"
			adding="true"
			:nodes="nodes"
			:relationship="relationship"
			:entityLocked="entityLocked"
			:existingRelNames="existingRelNames"
			@save="save"
			@cancel="cancel"
		></edit-relationship>
	</v-dialog>
</template>

<script>
import EditRelationship from '@/components/EditRelationship.vue';

export default {
	props: {
		entityLocked: {type: Boolean}
	},
	data: () => ({
		existingRelNames: null,
		dialog: false,
		resolve: null,
		reject: null,
		relationship: {},
		nodes: null
	}),
	components: {
		EditRelationship
	},
	methods: {
		open(relationship, nodes, existingRelNames) {
			this.relationship = relationship
			this.existingRelNames = existingRelNames || []
			this.nodes = nodes
			this.dialog = true
			this.$refs.addRel.reset()
			return new Promise((resolve, reject) => {
				this.resolve = resolve
				this.reject = reject
			})
		},
		save(e) {
			this.resolve(e)
			this.dialog = false
		},
		cancel() {
			this.resolve(null)
			this.dialog = false
		}
	}
}
</script>
