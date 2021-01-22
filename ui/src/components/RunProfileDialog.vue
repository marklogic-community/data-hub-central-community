<template>
	<v-dialog
		v-model="dialog"
		width="650"
		persistent
		@keydown.esc="cancel">
		<v-form @submit.prevent="save">
			<v-card>
				<v-card-title>Profile Your Data
					<v-spacer/>
					<v-icon @click="cancel">close</v-icon>
				</v-card-title>
				<v-card-text>
					<v-select
						:items="databases"
						item-text="label"
						item-value="value"
						label="Database"
						v-model="currentDatabase"
						:menu-props="{ 'content-class': 'databaseArray'}"
						data-cy="runProfileDialog.database"
						:error-messages="inputErrors('currentDatabase', 'Database')"
						@input="$v.currentDatabase.$touch()"
						@blur="$v.currentDatabase.$touch()"
						required
					></v-select>
					<v-select
						:items="collections"
						label="Data Source"
						v-model="collection"
						item-text="collection"
						item-value="collection"
						data-cy="runProfileDialog.dataSourceField"
						:menu-props="{ 'content-class': 'dataSourceArray'}"
						:error-messages="inputErrors('collection', 'Data Source')"
						@input="$v.collection.$touch()"
						@blur="$v.collection.$touch()"
						required
					>
						<template v-slot:item="{ item, attrs, on }">
							<v-list-item v-on="on" v-bind="attrs">
								<v-list-item-content>
									<v-list-item-title>{{item.collection}}</v-list-item-title>
								</v-list-item-content>
								<v-list-item-action>({{item.count}})</v-list-item-action>
							</v-list-item>
						</template>
					</v-select>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text color="secondary" @click="cancel" data-cy="runProfileDialog.cancelBtn">Cancel</v-btn>
					<v-btn type="submit" text color="primary" data-cy="runProfileDialog.saveBtn">Create Report</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import { required } from 'vuelidate/lib/validators'
import flowsApi from '@/api/FlowsApi'

export default {
	props: {
		showDialog: { type: Boolean }
	},
	computed: {
		collections() {
			return this.currentDatabase === 'staging' ? this.stagingData : this.finalData
		},
	},
	data() {
		return {
			dialog: false,
			resolve: null,
			reject: null,
			databases: [
				{
					label: 'Staging',
					value: 'staging'
				},
				{
					label: 'Final',
					value: 'final'
				}
			],
			stagingData: [],
			finalData: [],
			currentDatabase: 'staging',
			collection: null,
			sampleSize: 1000
		}
	},
	validations: {
		collection: { required },
		currentDatabase: { required }
  },
	methods: {
		refreshInfo() {
			flowsApi.getNewStepInfo().then(info => {
				this.stagingData = info.collections.staging
				this.finalData = info.collections.final
			})
		},
		inputErrors(field, fieldName) {
			const errors = []
			if (!this.$v[field].$dirty) return errors
			this.$v[field].$invalid && this.$v[field].$params.hasOwnProperty('required') && !this.$v[field].required && errors.push(`${fieldName} is required.`)
			return errors
		},
		open() {
			this.refreshInfo()
			this.advancedState = null
			this.$v.$reset()
			this.collection = null
			this.currentDatabase = 'staging'
			this.sampleSize = 1000
			this.dialog = true
			return new Promise((resolve, reject) => {
				this.resolve = resolve
				this.reject = reject
			})
		},
		save() {
			this.$v.$touch()
			if (this.$v.$invalid) {
				return
			}

			this.resolve({collection: this.collection, database: this.currentDatabase, sampleSize: this.sampleSize})
			this.dialog = false
		},
		cancel() {
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
