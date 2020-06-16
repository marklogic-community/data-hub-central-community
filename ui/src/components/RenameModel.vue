<template>
	<v-form @submit.prevent="rename">
		<v-card>
			<v-card-title class="primary--text" >Rename Model</v-card-title>
			<v-container>
				<v-text-field
					data-cy="renameModelVue.renameModelNameField"
					color="primary"
					ref="modelName"
					required
					:error="error"
					:error-messages="errorMsg"
					label="Model Name"
					v-model="modelName"
				></v-text-field>
				
			</v-container>
			<v-card-actions>
				<v-spacer></v-spacer>
				<v-btn text color="secondary" @click="cancel">Cancel</v-btn>
				<v-btn type="submit"  text color="primary" data-cy="renameModelVue.renameSubmitButton">Rename</v-btn>
			</v-card-actions>
		</v-card>
	</v-form>
</template>

<script>
export default {
	props: { 
		existingModels: { type: Array }
	},
	data: () => ({
		modelName: null,
		error: false,
		errorMsg: null
	}),
	methods: {
		reset() {
			setTimeout(() => {
				this.$refs.modelName.focus()
			}, 250);
			this.error = false
			this.errorMsg = []
			this.modelName = null
			this.dataType = 'String'
		},
		rename() {
			if (!this.modelName || this.modelName.length === 0) {
				this.error = true
				this.errorMsg = ['A model name is required']
				return
			}
			if (this.existingModels.find(m => m.name.toLowerCase() === this.modelName.toLowerCase())) {
				this.error = true
				this.errorMsg = ['A model of that name already exists']
				return
			}
			this.$emit('rename', this.modelName)
		},
		cancel() {
			this.$emit('cancel')
		}
	}
}
</script>