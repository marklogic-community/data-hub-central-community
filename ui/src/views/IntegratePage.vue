<template>
	<v-container fluid class="IntegratePage">
		<v-layout row class="fullHeight">
			<v-flex :class="['stepsPane', showManageDataPane ? 'md8' : 'md12']">
				<v-layout row v-if="!hasSteps" class="no-steps">
					<v-flex md8 :class="nextStepClass">
							<i class="fa" :class="(nextStepClass === '') ? 'fa-level-down':'fa-level-up'" /> {{nextStep}}
					</v-flex>
				</v-layout>
				<v-layout row justify-center no-gutters>
					<div class="button-wrapper">
						<v-btn data-cy="integrate.addStepBtn" :disabled="!hasData" @click="addStep">Add Step</v-btn>
						<v-spacer></v-spacer>
						<v-btn data-cy="integrate.runStepBtn" :disabled="!hasSteps" @click="showFlowRunner = true">Run Steps</v-btn>
					</div>
				</v-layout>
				<span class="manageDataToggle">
					<v-tooltip bottom>
						<template v-slot:activator="{ on }">
							<span v-on="on">
								<v-btn icon @click="showManageDataPane = !showManageDataPane" data-cy="manageData.toggle">
									<v-icon>fa-database</v-icon>
								</v-btn>
							</span>
						</template>
						<span>Manage Integrated Data</span>
					</v-tooltip>
				</span>
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
				<v-layout row>
					<v-flex md12>
						<flow-step
							v-if="flow"
							:flow="flow"
							:step="currentStep"
							:stepInfo="stepInfo"
							@saved="saveStep"
							@deleted="stepDeleted"/>
					</v-flex>
				</v-layout>
			</v-flex>
			<v-flex :class="['manageDataPane', showManageDataPane ? 'md4' : 'nowidth']">
				<div class="manageContents">
					<template v-if="showManageDataPane">
						<v-layout row>
							<v-flex md12 class="text-center">
								<h1>Manage My Data</h1>
							</v-flex>
						</v-layout>
						<v-row justify="center">
							<v-col cols="10" class="data-container">
								<v-simple-table>
									<thead>
										<tr>
											<th>Entity Name</th>
											<th>Count</th>
											<th></th>
										</tr>
									</thead>
									<tbody data-cy="manageData.table">
										<tr v-for="entity in entityNames" :key="entity" :data-cy="`manageData.${entity}`">
											<td>{{entity}}</td>
											<td>{{collectionCounts[entity] || 0}}</td>
											<td>
												<delete-data-confirm
													:tooltip="`Delete ${entity} Documents`"
													:message="`Do you really want to delete all ${entity} documents?`"
													:disabled="(collectionCounts[entity] || 0) === 0"
													:collection="entity"
													:deleteInProgress="deleteInProgress"
													@deleted="removeData($event)"/>
											</td>
										</tr>
									</tbody>
								</v-simple-table>
							</v-col>
						</v-row>
					</template>
				</div>
			</v-flex>
		</v-layout>
		<add-step-dialog
			v-if="flow"
			:showDialog="showAddStep"
			:flowName="flow.name"
			:stepInfo="stepInfo"
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
import DeleteDataConfirm from '@/components/DeleteDataConfirm'
import flowsApi from '@/api/FlowsApi'
import md5 from 'md5'
import axios from 'axios'

export default {
	data: function() {
		return {
			showFlowRunner: false,
			showAddStep: false,
			currentStepIdx: null,
			currentStepName: null,
			currentPanel: null,
			stepInfo: null,
			dragOptions: {
				animation: 200
			},
			nextStep: 'Start by adding a step.',
			nextStepClass: '',
			showManageDataPane: true,
			finalData: [],
			deleteInProgress: false
		}
	},
	components: {
		FlowRunnerDialog,
		FlowStep,
		AddStepDialog,
		DeleteDataConfirm,
		draggable
	},
	computed: {
		hasData() {
			return this.stepInfo && this.stepInfo.collections.staging.length > 0 &&
				this.entities && this.entities.length > 0
		},
		hasSteps() {
			return this.flow && Object.keys(this.flow.steps).length > 0
		},
		currentStep() {
			return (this.flow && this.currentStepName) ?
				Object.values(this.flow.steps).find(s => s.name === this.currentStepName) : null
		},
		sortedSteps() {
			return this.entities.map(e => e.info.title)
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
		entityNames() {
			return this.entities.map(e => e.info.title).sort()
		},
		collectionCounts() {
			return this.finalData.reduce((p, c) => {
				p[c.collection] = c.count
				return p
			}, {})
		},
		...mapState({
			flowName: state => md5(state.auth.username),
			flows: state => state.flows.flows,
			entities: state => Object.values(state.flows.entities)
		})
	},
	created () {
		this.getEntities()
		this.getFlow()
	},
	mounted() {
		this.handleRouteParams()
		this.refreshInfo()
		this.$ws.subscribe('/topic/status', tick => {
			const msg = tick.body
			if (msg.percentComplete >= 100) {
				this.refreshInfo()
			}
		})
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
		},
		refreshInfo() {
			flowsApi.getNewStepInfo().then(info => {
				this.finalData = info.collections.final
				this.stepInfo = info
				if (!this.stepInfo.collections.staging || this.stepInfo.collections.staging.length <= 0) {
					this.nextStep = 'Start by Uploading data.'
					this.nextStepClass = 'upload'
				}
				else if (!this.entities || this.entities.length <= 0) {
					this.nextStep = 'Start by creating a data model'
					this.nextStepClass = 'connect'
				}
			})
		},
		async removeData(collection) {
			this.deleteInProgress = true
			await axios.post("/api/system/deleteCollection", { database: 'final', collection: collection })
			this.deleteInProgress = false
			this.finalData = this.finalData.filter(c => c.collection !== collection)
		}
	}
}
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
	margin-bottom: 2em;
	text-align: center;
	justify-content: center;
	.fa-level-up,
	.fa-level-down {
		transform: scale(-1, 1);
		font-size: 200%;
		color: red;
	}

	.fa-level-down {
		transform: scale(-1, 1) translateY(15px)
	}
}

.upload {
	transform: translateX(-150px);
}

.connect {
	transform: translateX(-20px);
}
.fullHeight {
	height: 100%;
	position: relative;
}

.IntegratePage {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 0px;

	.flex {
		padding: 12px 20px;
	}
}

.stepsPane {
	position: relative;
}

.manageDataPane {
	border-left: 1px solid #ccc;
	position: relative;

	.manageContents {
		transition: width 0.5s;

		position: absolute;
		padding: 0.25em;
		top: 0;
		right: 0;
		bottom: 0;
		width: 100%;

		// overflow: hidden;
	}
}

.md4 .manageContents {
	width: 100%;
}

.nowidth .manageContents {
	width: 0px;
}

.manageDataToggle {
	position: absolute;
	top: 20px;
	right: 30px;
}

.flex {
	transition: width 0.5s;
}
</style>
