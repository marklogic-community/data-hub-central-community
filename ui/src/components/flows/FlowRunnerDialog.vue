<template>
	<v-dialog
		v-model="open"
		width="650"
		persistent
		@keydown.esc="close">
		<v-form @submit.prevent="runSteps">
			<v-card>
				<v-card-title>Run Steps<v-spacer></v-spacer><v-icon @click="close">close</v-icon></v-card-title>
				<v-card-text>
					<v-row class="fullheight">
						<v-simple-table>
							<thead>
								<tr>
									<th>
										<v-checkbox
											data-cy="mastering.checkAll"
											v-model="allChecked"
										></v-checkbox>
									</th>
									<th>Step</th>
									<th>Description</th>
									<th>Target Entity</th>
									<th>Step Type</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="(step, index) in steps" :key="index">
									<td>
										<v-checkbox dense v-model="checkedSteps[index]"></v-checkbox>
									</td>
									<td>{{step.name}}</td>
									<td>{{step.description}}</td>
									<td>{{step.options.targetEntity}}</td>
									<td>{{step.stepDefinitionType}}</td>
								</tr>
							</tbody>
						</v-simple-table>
					</v-row>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn text color="secondary" @click="close">Cancel</v-btn>
					<v-btn :disabled="checkedIndexes.length == 0" type="submit" text color="primary"><v-icon left>play_arrow</v-icon> Run</v-btn>
				</v-card-actions>
			</v-card>
		</v-form>
	</v-dialog>
</template>

<script>
import flowsApi from '@/api/FlowsApi';

export default {
	props: {
		flow: {type: Object},
		showDialog: { type: Boolean }
	},
	data() {
		return {
			isOpen: null,
			checkedSteps: {},
		}
	},
	computed: {
		steps() {
			if (!this.flow) {
				return []
			}
			let steps = []

			for (let key in this.flow.steps) {
				steps.push({
					...this.flow.steps[key],
					stepOrder: key
				})
			}
			return steps.filter(step => step.stepDefinitionType !== 'INGESTION')
		},

		open: {
			get() {
				return this.isOpen || this.showDialog
			},
			set(val) {
				this.isOpen = val
			}
		},
		checkedIndexes() {
			return Object.keys(this.checkedSteps).filter(key => this.checkedSteps[key])
		},
		allChecked: {
			get() {
				return this.checkedIndexes.length > 0 && this.checkedIndexes.length === this.steps.length
			},
			set(val) {
				this.steps.forEach((n, index) => {
					this.$set(this.checkedSteps, index, val)
				})
			}
		},
		selectedFlows() {
			return this.steps.filter((v, idx) => this.checkedIndexes.findIndex(x => x == idx) >= 0)
		},
		selectedFlowSteps() {
			return this.selectedFlows.map(f => f.stepOrder)
		}
	},
	methods: {
		runSteps() {
			flowsApi.runSteps(this.flow.name, this.selectedFlowSteps)
			this.close()
		},
		close() {
			this.open = false
			this.$emit('closed')
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
