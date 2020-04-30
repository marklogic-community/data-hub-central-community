<template>
	<v-card>
		<v-card-title class="primary--text">Load Model</v-card-title>
		<v-container>
			<v-select
				:items="models"
				v-model="currentModel"
				label="Pick Model..."
				item-text="name"
				return-object
				outlined
			>
				<template slot="item" slot-scope="data">
					<v-list-item-avatar>
						<img v-if="data.item.img" :src="data.item.img" />
					</v-list-item-avatar>
					<v-list-item-content>
						<v-list-item-title v-html="data.item.name" class="primary--text"></v-list-item-title>
					</v-list-item-content>
				</template>
			</v-select>
		</v-container>
		<v-card-actions>
			<v-spacer></v-spacer>
			<v-btn text color="secondary" @click="cancel">Cancel</v-btn>
			<v-btn @click="load" text color="primary">Load</v-btn>
		</v-card-actions>
	</v-card>
</template>

<script>
export default {
	props: {
		models: { type: Array }
	},
	data: () => ({
		currentModel: null
	}),
	methods: {
		load() {
			if (this.currentModel) {
				this.$store.dispatch('model/save', this.currentModel)
				this.$emit('close');
			}
		},
		cancel() {
			this.$emit('close');
		}
	}
}
</script>
