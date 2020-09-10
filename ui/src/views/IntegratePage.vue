<template>
	<v-container fluid>
		<v-layout row justify-center no-gutters>
			<div class="button-wrapper">
				<v-btn @click="addStep">Add Step</v-btn>
				<v-spacer></v-spacer>
				<v-btn @click="showFlowRunner = true">Run Steps</v-btn>
			</div>
		</v-layout>
		<v-layout v-if="steps && steps.length > 0" row>
			<draggable v-bind="dragOptions" v-model="steps" class="flex step-scroller md12" draggable=".step-wrapper">
				<transition-group type="transition" tag="div" :class="['flexxy', { contracted: currentStepName }]" ref="scroller">
					<div :id="step.name" class="step-wrapper" v-for="(step, index) in steps" :key="step.name">
						<div :class="['step', {active: (currentStepName === step.name)}, `elevation-${(currentStepName === step.name) ? 6 : 1}`]" @click="currentStepName = step.name">
							<div :class="['bar', step.stepType]">
								<v-icon color="white" small>drag_indicator</v-icon>
								<v-spacer></v-spacer>
								<span class="steptype">{{step.stepType}}</span>
							</div>
							<div class="step-body">
								<h4>{{step.name}}</h4>
								<div class="meta">
									{{step.dataSource}} => {{step.targetEntity}}
								</div>
							</div>
						</div>
						<div v-if="index < (steps.length - 1)" class="line">&rarr;</div>
					</div>
				</transition-group>
			</draggable>
		</v-layout>
		<v-layout row v-else class="no-steps">
			<v-flex md8>
				<i class="fa fa-level-up" /> Start by adding a step.
			</v-flex>
		</v-layout>

		<v-layout row>
			<v-flex md12>
				<flow-step
					v-if="flow"
					:flow="flow"
					:step="currentStep"
					@saved="saveStep"
					@deleted="stepDeleted"/>
			</v-flex>
		</v-layout>
		<add-step-dialog
			v-if="flow"
			:showDialog="showAddStep"
			:flowName="flow.name"
			@closed="showAddStep = false"
			@saved="stepCreated">
		</add-step-dialog>
		<flow-runner-dialog
			:showDialog="showFlowRunner"
			:flow="flow"
			@closed="showFlowRunner = false">
		</flow-runner-dialog>
	</v-container>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import draggable from 'vuedraggable'
import FlowRunnerDialog from '@/components/flows/FlowRunnerDialog'
import FlowStep from '@/components/flows/FlowStep'
import AddStepDialog from '@/components/flows/AddStepDialog'
import md5 from 'md5';

export default {
	data: function() {
		return {
			showFlowRunner: false,
			showAddStep: false,
			currentStepIdx: null,
			currentStepName: null,
			currentPanel: null,
			dragOptions: {
				animation: 200
			}
		}
	},
	components: {
		FlowRunnerDialog,
		FlowStep,
		AddStepDialog,
		draggable
	},
	computed: {
		currentStep() {
			return (this.flow && this.currentStepName) ?
				Object.values(this.flow.steps).find(s => s.name === this.currentStepName) : null
		},
		sortedSteps() {
			return Object.values(this.entities).map(e => e.info.title)
				.map(e => {
					return {
						name: e,
						steps: this.steps.filter(s => s.targetEntity === e)
					}
				})
		},
		steps: {
			get() {
				return (this.flow && this.flow.steps) ? Object.values(this.flow.steps)
					.filter(step => step.stepDefinitionType !== 'INGESTION')
					.map(step => {
						return {
							name: step.name,
							targetEntity: step.options.targetEntity,
							stepType: step.stepDefinitionType,
							dataSource: step.options.sourceCollection,
							raw: step
						}
					}) : []
			},
			set(steps) {
				const newSteps = steps.reduce((output, step, idx) => {
					output[idx + 1] = step.raw
					return output
				}, {})
				this.saveFlow({
					...this.flow,
					steps: newSteps
				})
			}
		},
		flow() {
			return this.flows[this.flowName]
		},
		routeParams() {
			let p = {}
			if (this.currentStep) {
				p.stepName = this.currentStep.name
			}
			return p
		},
		...mapState({
			flowName: state => md5(state.auth.username),
			flows: state => state.flows.flows,
			entities: state => state.flows.entities
		})
	},
	created () {
		this.getEntities()
		this.getFlow()
	},
	mounted() {
		this.handleRouteParams()
	},
  watch: {
		'$route.params': 'handleRouteParams',
		flow: function() {
			this.handleRouteParams()
		},
		currentStep() {
			this.$nextTick(() => {
				// scroll the current step into view
				const el = this.$el.querySelector(`#${this.currentStepName}`)
				if (el) {
					el.scrollIntoView(false)
				}
			})
			this.updateRoute()
		},
  },
	methods: {
		...mapActions({
			getEntities: 'flows/getEntities',
			getFlow: 'flows/getFlow',
			saveFlow: 'flows/saveFlow'
		}),
		saveStep(step) {
			const newFlow = {
				...this.flow,
				steps: {
					...this.flow.steps,
				}
			}
			newFlow.steps[this.currentStepIdx] = step
			this.saveFlow(newFlow)
		},
		stepDeleted() {
			this.currentStepName = null
			this.getFlow()
		},
		handleRouteParams() {
			if (!this.flow) {
				return
			}
			const params = this.$route.params
			if (params.stepName) {
				const stepIdx = Object.values(this.flow.steps).findIndex(s => s.name === params.stepName)
				this.currentStepIdx = stepIdx + 1
				this.currentStepName = params.stepName
			}
		},
		addStep() {
			this.showAddStep = true
		},
		stepCreated(step) {
			const idx = this.steps.findIndex(s => s.name === step.name)
			if (idx >= 0) {
				this.currentStepIdx = idx
			}
			this.currentStepName = step.name
		},
		getSortedStep(entityName) {
			return this.sortedSteps.find(ss => ss.name === entityName)
		},
		updateRoute() {
			const params = this.$route.params
			if ( params.stepName != this.currentStepName ) {
				this.$router.push({ name: 'root.integrate', params: this.routeParams })
			}
		}
	}
};
</script>

<style lang="less" scoped>
@color-mapping: #3F51B5;
@color-mastering: #009688;

.bar {
	height: 22px;
	border-radius: 4px 4px 0 0;
	display: flex;
	color: white;
	line-height: 22px;
	padding: 0px 5px;

	&.MAPPING {
		background: @color-mapping;
	}
	&.MASTERING {
		background: @color-mastering;
	}
	&.MATCHING {
		background: @color-mastering;
	}
	&.MERGING {
		background: @color-mastering;
	}
}

.step-scroller {
	display: flex;
	overflow-x: auto;
	padding: 1em 0.5em;
}

.step-wrapper {
	display: inline-flex;

	.step {
		cursor: pointer;
		border-radius: 4px;
		display: inline-block;
		width: 200px;
		// height: 160px;
		position: relative;
		background: #fff;
		font-size: 12px;

		&.active {
			border: 1px solid #999;
			border-top: none;
		}

		.step-header {
			padding: 0px 5px;
			display: flex;

			.steptype {
				line-height: 28px;
			}
		}

		.step-body {
			padding: 0.5px 0.5em 0px 0.5em;

			.meta {
				font-size: 10px;
				.body-meta {
					padding-bottom: 0.5em;

					.lbl {
						font-weight: bold;
					}
					.value {
						margin-left: 0.5em;
					}
				}
			}
		}
	}
}

.add-step {
	padding: 2em;
	display: flex;
	justify-content: center;
	flex-direction: column;

	button {
		margin-bottom: 0.5em;
	}
}

.line {
	display: inline-block;
	font-size: 32px;
	color: #696969;
	margin: auto 0;
}

.sortable-chose {
	cursor: move;
}

.flexxy {
	display: flex;
}

.button-wrapper {
	width: 250px;
	display: flex;
}

.no-steps {
	margin-top: 2em;
	text-align: center;
	justify-content: center;
	.fa-level-up {
		transform: scale(-1, 1);
		font-size: 200%;
		color: red;
	}
}

</style>
