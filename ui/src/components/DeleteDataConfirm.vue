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
						data-cy="upload.deleteButton"
						right
						icon
						small
						class="small-btn"
						v-on="{ ...tooltip, ...menu }"
					>
						<v-icon>delete</v-icon>
					</v-btn>
				</template>
				<span>Delete Data Source</span>
			</v-tooltip>
		</template>
		<confirm
			message="Do you really want to delete this data source?"
			confirmText="Delete"
			:disabled="deleteInProgress"
			@confirm="deleteDatasource"
			@cancel="confirmDeleteMenu = false"></confirm>
	</v-menu>
</template>
<script>
import Confirm from '@/components/Confirm.vue';
import axios from 'axios';

export default {
	components: {
		Confirm
	},
	props: {
		collection: {type: String},
		disabled: {type: Boolean, default: false}
	},
	data() {
		return {
			deleteInProgress: false,
			confirmDeleteMenu: null,
		}
	},
	methods: {
		async deleteDatasource() {
			this.deleteInProgress = true
			try {
				await axios.post("/api/system/deleteCollection", { database: 'staging', collection: this.collection })
				this.$emit('deleted', this.collection)
			}
			catch(error) {
				console.error(error)
			}
			this.deleteInProgress = false
			this.confirmDeleteMenu = false
		},
	}
}
</script>
