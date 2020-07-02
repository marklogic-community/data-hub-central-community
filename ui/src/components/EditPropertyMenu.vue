<template>
	<v-menu
		:close-on-content-click="false"
		:nudge-width="425"
		v-model="showMe"
		top
		nudge-top="200"
		offset-x
	>
		<template v-slot:activator="{ on }">
			<span v-on="on"><slot></slot></span>
		</template>
		<edit-property
			:visible="!!showMe"
			:existingProperties="existingProperties"
			:prop="prop"
			:entityName="entityName"
			@save="save(prop, $event)"
			@cancel="cancel()"
		></edit-property>
	</v-menu>
</template>

<script>
import EditProperty from '@/components/EditProperty.vue';

export default {
	name: 'edit-property-menu',
	components: {
		EditProperty,
	},
	props: {
		prop: {type: Object},
		entityName: {type: String},
		existingProperties: {type: Array}
	},
	data() {
		return {
			showMe: false,
		}
	},
	methods: {
		save(oldProp, newProp) {
			this.showMe = false
			this.$emit('save', {oldProp, newProp})
		},
		cancel() {
			this.showMe = false
		}
	}
}
</script>
