<template>
	<v-container fluid>
		<v-row v-if="step && flow" id="flow-step">
			<v-flex md12>
				<v-card>
					<v-card-title>
						<h2>{{entityName}} => {{step.name}}</h2>
						<v-spacer></v-spacer>
						<v-btn
							data-cy="flowStep.editButton"
							right
							icon
							small
							class="small-btn"
							@click="editStep">
							<v-icon>settings</v-icon>
						</v-btn>
						<v-menu
							:close-on-content-click="false"
							:nudge-width="300"
							offset-x
							v-model="confirmDeleteMenu">
							<template v-slot:activator="{ on: menu }">
								<v-tooltip bottom>
									<template v-slot:activator="{ on: tooltip }">
										<v-btn
											data-cy="flowStep.deleteButton"
											right
											icon
											small
											class="small-btn"
											v-on="{ ...tooltip, ...menu }"
										>
											<v-icon>delete</v-icon>
										</v-btn>
									</template>
									<span>Delete Step</span>
								</v-tooltip>
							</template>
							<confirm
								message="Do you really want to delete this step?"
								confirmText="Delete"
								:disabled="deleteInProgress"
								@confirm="deleteStep"
								@cancel="confirmDeleteMenu = false"></confirm>
						</v-menu>
					</v-card-title>
					<v-card-text>
						<div id="step-type-mapping-container" v-if="step.stepDefinitionType === 'MAPPING'">
							<mapping-step
								:step="step"
								:flow="flow"
								@saveStep="saveStep"
							></mapping-step>
						</div>
						<div id="step-type-matching-container" v-if="step.stepDefinitionType === 'MATCHING'">
							<matching-step
								:step="step"
								@saveStep="saveStep"
							></matching-step>
						</div>
						<div id="step-type-merging-container" v-if="step.stepDefinitionType === 'MERGING'">
							<merging-step
								:step="step"
								@saveStep="saveStep"
							></merging-step>
						</div>
					</v-card-text>
				</v-card>
			</v-flex>
		</v-row>
		<add-step-dialog
			:showDialog="showEditStep"
			:entityName="entityName"
			:step="step"
			:flowName="flow.name"
			:isEditing="true"
			@closed="showEditStep = false">
		</add-step-dialog>
	</v-container>
</template>

<script>
import { mapActions } from 'vuex'
import Confirm from '@/components/Confirm.vue';
import MappingStep from '@/components/flows/MappingStep'
import MatchingStep from '@/components/flows/MatchingStep'
import MergingStep from '@/components/flows/MergingStep'
import AddStepDialog from '@/components/flows/AddStepDialog'
import flowsApi from '@/api/FlowsApi'

export default {
	name: 'flow-step',
	props: {
		flow: {type: Object},
		step: {type: Object}
	},
	data() {
		return {
			deleteInProgress: false,
			showBody: true,
			confirmDeleteMenu: null,
			showEditStep: false
		}
	},
	components: {
		AddStepDialog,
		MappingStep,
		MatchingStep,
		MergingStep,
		Confirm
	},
	computed: {
		entityName() {
			return this.step && this.step.options && this.step.options.targetEntity
		},
	},
	methods: {
		...mapActions({
			getFlow: 'flows/getFlow'
		}),
		toggleBody() {
			this.showBody = !this.showBody
		},
		editStep() {
			this.showEditStep = true
		},
		saveStep(step) {
			this.$emit('saved', step)
		},
		deleteStep() {
			this.deleteInProgress = true
			flowsApi.deleteStep(this.flow.name, this.step.name)
				.then(() => {
					this.deleteInProgress = false
					this.confirmDeleteMenu = false
					this.$emit('deleted')
				})
		}
	}
}
</script>

<style lang="less" scoped>
@color-mapping: #3F51B5;
@color-mastering: #009688;
@color-ingest: #AD1457;
@color-custom: #F4511E;

.steps-container {
  background: white;
  width: 90%;
  margin: auto;
  margin-top: 30px;
  margin-bottom: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12);
}
header {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0,0,0,.35);
  padding: 8px;
}

.steps-body {
  min-height: 250px;
  background: rgba(238,238,238,.65);
  > p {
    font-size: 24px;
    text-align: center;
  }
}

button {
  height: 35px;
  outline: none;
}

.flow-menu-icon {
  padding: 1%;
  cursor: pointer;
  font-size: 24px;
}

.bar {
  height: 22px;
}
.mapping {
  background: @color-mapping;
}
.matching {
  background: @color-mastering;
}
.merging {
  background: @color-mastering;
}
.mastering {
  background: @color-mastering;
}
.custom {
  background: @color-custom;
}

mat-icon {
  user-select: none;
}
</style>
