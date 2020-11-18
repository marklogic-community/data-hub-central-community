<template>
	<v-menu
		:close-on-content-click="false"
		:nudge-width="300"
		:disabled="deleteInProgress"
		offset-x
		v-model="confirmDeleteMenu">
		<template v-slot:activator="{ on: menu }">
			<v-tooltip bottom :disabled="disabled">
				<template v-slot:activator="{ on: tooltip }">
					<v-btn
						:disabled="disabled"
						data-cy="deleteDataConfirm.deleteButton"
						right
						icon
						small
						class="small-btn"
						v-on="{ ...tooltip, ...menu }"
					>
						<v-icon>delete</v-icon>
					</v-btn>
				</template>
				<span>{{tooltip}}</span>
			</v-tooltip>
		</template>
		<confirm
			:message="message"
			confirmText="Delete"
			:disabled="deleteInProgress"
			@confirm="deleteDatasource"
			@cancel="confirmDeleteMenu = false"></confirm>
	</v-menu>
</template>
<script>
import Confirm from '@/components/Confirm.vue';

export default {
	components: {
		Confirm
	},
	props: {
		deleteInProgress: {type: Boolean, default: false},
		tooltip: {type: String},
		message: {type: String},
		collection: {type: String},
		disabled: {type: Boolean, default: false}
	},
	data() {
		return {
			confirmDeleteMenu: null,
		}
	},
	methods: {
		async deleteDatasource() {
			try {
				this.$emit('deleted', this.collection)
			}
			catch(error) {
				console.error(error)
			}
			this.confirmDeleteMenu = false
		},
	}
}
</script>
