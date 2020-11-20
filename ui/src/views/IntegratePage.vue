<template>
	<v-container fluid class="IntegratePage">
		<v-layout row class="fullHeight">
<<<<<<< HEAD
			<v-flex v-if="!isHosted" :class="['flowsPane', showFlowsPane ? 'md3' : 'nowidth']">
				<div class="manageContents" v-if="showFlowsPane">
					<v-layout row>
						<v-flex class="flows-title" md12>
							<div class="headline">Flows</div>
							<v-spacer/>
							<v-tooltip bottom>
								<template v-slot:activator="{ on }">
									<span v-on="on">
										<v-btn
											color="primary"
											icon
											small
											right
											dark
											data-cy="integrate.addFlowBtn"
											@click="addFlow"
										>
											<v-icon dark>add</v-icon>
										</v-btn>
									</span>
								</template>
								<span>Create Flow</span>
							</v-tooltip>
						</v-flex>
					</v-layout>
					<v-simple-table dense>
						<tbody>
							<tr v-for="flow in flowsArray" :key="flow.name" data-cy="integrate.flowRow">
								<td :class="(flowName === flow.name) ? 'selected-flow': ''" @click="changeFlow(flow.name)">{{flow.name}}</td>
							</tr>
						</tbody>
					</v-simple-table>
				</div>
			</v-flex>
			<v-flex :class="['mainPane', showFlowsPane ? 'md9' : 'md12']">
				<v-layout row class="fullHeight">
					<v-flex :class="['stepsPane', (this.showManageDataPane || this.showRunHistoryPane) ? 'md8': 'md12']">
						<v-layout row justify-center v-if="!isHosted">
							<h3 class="flow-name">{{flowName}}</h3>
							<delete-data-confirm
								:tooltip="`Delete ${flowName}`"
								:message="`Do you really want to delete the ${flowName} flow?`"
								:collection="flowName"
								:deleteInProgress="deleteInProgress"
								@deleted="removeFlow($event)"/>

						</v-layout>
						<v-layout row v-if="!hasSteps" class="no-steps">
							<v-flex md8 :class="nextStepClass">
								<i class="fa" :class="(nextStepClass === '') ? 'fa-level-down':'fa-level-up'" /> {{nextStep}}
							</v-flex>
						</v-layout>
						<v-layout row justify-center no-gutters>
							<div row class="button-wrapper">
								<v-btn data-cy="integrate.addStepBtn" :disabled="!hasData" @click="addStep">Add Step</v-btn>
								<v-spacer></v-spacer>
								<v-btn data-cy="integrate.runStepBtn" :disabled="!hasSteps" @click="showFlowRunner = true">Run Steps</v-btn>
							</div>
						</v-layout>
						<span v-if="!isHosted" class="manageFlowToggle">
							<v-tooltip bottom>
								<template v-slot:activator="{ on }">
									<span v-on="on">
										<v-btn icon @click="showFlowsPane = !showFlowsPane" data-cy="manageData.toggleFlows">
											<v-icon>fa-exchange</v-icon>
										</v-btn>
									</span>
								</template>
								<span>Manage Flows</span>
							</v-tooltip>
						</span>
						<span v-if="!isHosted" class="manageDataToggle">
							<v-tooltip bottom>
								<template v-slot:activator="{ on }">
									<span v-on="on">
										<v-btn icon @click="showManageData" data-cy="manageData.toggle">
											<v-icon>fa-database</v-icon>
										</v-btn>
									</span>
								</template>
								<span>Manage Integrated Data</span>
							</v-tooltip>
						</span>
						<span class="manageHistoryToggle">
							<v-tooltip bottom>
								<template v-slot:activator="{ on }">
									<span v-on="on">
										<v-btn icon @click="showRunHistory()" data-cy="manageData.toggleRunHistory">
											<v-icon>fa-history</v-icon>
										</v-btn>
									</span>
								</template>
								<span>Manage Run History</span>
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
													<span v-if="step.dataSource">{{step.dataSource}} => </span>
													{{step.targetEntity}}
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
						<div class="manageContents" v-if="showManageDataPane">
							<template>
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
													<th>
														<delete-data-confirm
															:tooltip="`Delete All Entity Documents`"
															:message="`Do you really want to delete all Entity documents?`"
															:disabled="!hasFinalData"
															:collection="''"
															:deleteInProgress="deleteInProgress"
															@deleted="removeAllData($event)"/>
														</th>
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
					<v-flex v-if="!isHosted" :class="['runHistoryPane', showRunHistoryPane ? 'md4' : 'nowidth']">
						<div class="runHistoryContents" v-if="showRunHistoryPane">
							<v-layout row>
								<v-flex md12 class="text-center">
									<h1>{{flowName}} Jobs</h1>
								</v-flex>
							</v-layout>
							<div class="text-center" v-if="jobs.length === 0">No Jobs for this Flow</div>
							<v-list dense v-else>
								<v-list-item
									data-cy="jobStatus"
									v-for="job in jobs"
									:key="job.jobId">
									<v-list-item v-if="job.jobStatus === 'finished'">
										<v-list-item-icon>
											<v-icon>fa fa-check</v-icon>
										</v-list-item-icon>

										<v-list-item-content>
											<v-list-item-title>{{timeAgo(job.timeEnded)}} ({{Object.keys(job.stepResponses).length}} Steps)</v-list-item-title>
										</v-list-item-content>
									</v-list-item>
									<v-list-group
										v-else
										no-action
										:prepend-icon="(job.jobStatus === 'finished') ? 'fa-check' : 'fa-exclamation'"
									>
										<template v-slot:activator>
											<v-list-item-title>{{timeAgo(job.timeEnded)}} ({{Object.keys(job.stepResponses).length}} Steps)</v-list-item-title>
										</template>
										<v-list-item
											v-for="(resp, idx) in Object.values(job.stepResponses)"
											:key="idx"
										>
											<v-list-item-icon>
												<v-icon v-if="resp.success">fa fa-check</v-icon>
												<v-icon v-else>fa fa-exclamation</v-icon>
											</v-list-item-icon>
											<v-list-item-content>
												<v-list-item-title>Step {{idx + 1}}</v-list-item-title>
											</v-list-item-content>
											<v-list-item-action v-if="!resp.success">
												<v-btn icon @click="showError($event, resp)">
													<v-icon color="grey lighten-1">mdi-information</v-icon>
												</v-btn>
											</v-list-item-action>
										</v-list-item>
									</v-list-group>
								</v-list-item>
							</v-list>
						</div>
					</v-flex>
				</v-layout>
=======
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
>>>>>>> develop
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
		<add-flow-dialog
			:showDialog="showAddFlow"
			@closed="showAddFlow = false"
			@saved="flowCreated">
		</add-flow-dialog>
		<flow-runner-dialog
			:showDialog="showFlowRunner"
			:flow="flow"
			@closed="showFlowRunner = false">
		</flow-runner-dialog>
		<v-menu
      v-model="showErrorConfirm"
      :position-x="stepErrorX"
      :position-y="stepErrorY"
      absolute
      offset-y
    >
			<confirm
				:message="stepErrorMessage"
				confirmText="Ok"
				@confirm="showErrorConfirm = false"
				:showCancel="false"></confirm>
		</v-menu>
	</v-container>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import draggable from 'vuedraggable'
import FlowRunnerDialog from '@/components/flows/FlowRunnerDialog'
import FlowStep from '@/components/flows/FlowStep'
import AddStepDialog from '@/components/flows/AddStepDialog'
<<<<<<< HEAD
import AddFlowDialog from '@/components/flows/AddFlowDialog'
import DeleteDataConfirm from '@/components/DeleteDataConfirm'
import Confirm from '@/components/Confirm.vue';
import flowsApi from '@/api/FlowsApi'
import jobsApi from '@/api/JobsApi'
import axios from 'axios'

const isHosted = process.env.VUE_APP_IS_HOSTED === 'true'
=======
import DeleteDataConfirm from '@/components/DeleteDataConfirm'
import flowsApi from '@/api/FlowsApi'
import md5 from 'md5'
import axios from 'axios'
>>>>>>> develop

export default {
	data: function() {
		return {
			isHosted: isHosted,
			showFlowRunner: false,
			showAddStep: false,
			showAddFlow: false,
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
<<<<<<< HEAD
			showRunHistoryPane: false,
			showFlowsPane: !isHosted,
			finalData: [],
			deleteInProgress: false,
			flowName: null,
			jobs: [],
			showErrorConfirm: false,
			stepErrorMessage: '',
			stepErrorX: 0,
			stepErrorY: 0
=======
			finalData: [],
			deleteInProgress: false
>>>>>>> develop
		}
	},
	components: {
		FlowRunnerDialog,
		FlowStep,
		AddStepDialog,
<<<<<<< HEAD
		AddFlowDialog,
		DeleteDataConfirm,
		Confirm,
=======
		DeleteDataConfirm,
>>>>>>> develop
		draggable
	},
	computed: {
		hasData() {
			return (this.hasIngestSteps || (this.stepInfo && this.stepInfo.collections.staging.length > 0)) &&
				this.entities && this.entities.length > 0
		},
		hasFinalData() {
			return this.finalData.reduce((p, c) => p + c.count, 0) > 0
		},
		hasSteps() {
			return this.flow && Object.keys(this.flow.steps).length > 0
		},
		hasIngestSteps() {
			return this.flow && Object.values(this.flow.steps).filter(step => step.stepDefinitionType === 'INGESTION').length > 0
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
		flowsArray() {
			return Object.values(this.flows)
				.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
		},
		routeParams() {
			let p = {}
			if (!isHosted && this.flowName) {
				p.flowName = this.flowName
			}
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
			flows: state => state.flows.flows,
			entities: state => Object.values(state.flows.entities)
		})
	},
	created () {
		this.getEntities()

		if (isHosted) {
			this.getFlow()
		}
	},
	mounted() {
		this.handleRouteParams()
		this.refreshInfo()
		this.$ws.subscribe('/topic/status', tick => {
			const msg = tick.body
			if (msg.percentComplete >= 100) {
				this.refreshInfo()
<<<<<<< HEAD
				this.getJobs()
=======
>>>>>>> develop
			}
		})
	},
  watch: {
		'$route.params': 'handleRouteParams',
		flow: function() {
			this.updateRoute()
			this.getJobs()
		},
		flows: function() {
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
		timeAgo(time) {
			return this.$moment(time).fromNow()
		},
		showError(e, step) {
			this.showErrorConfirm = true
			this.stepErrorX = e.clientX
      this.stepErrorY = e.clientY
			this.stepErrorMessage = step.stepOutput.join(' ')
		},
		showManageData() {
			this.showManageDataPane = !this.showManageDataPane
			if (this.showManageDataPane) {
				this.showRunHistoryPane = false
			}
		},
		showRunHistory() {
			this.showRunHistoryPane = !this.showRunHistoryPane
			if (this.showRunHistoryPane) {
				this.showManageDataPane =false
			}
		},
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
			this.getFlow(this.flowName)
		},
		changeFlow(flowName) {
			this.flowName = flowName
			this.currentStepName = null
		},
		handleRouteParams() {
			const params = this.$route.query
			if (params.flowName) {
				this.flowName = params.flowName
			}
			else {
				this.flowName = ((this.flowsArray && this.flowsArray[0]) || {}).name
			}

			this.currentStepName = params.stepName || null
			if (params.stepName) {
				if (this.flow) {
					const stepIdx = Object.values(this.flow.steps).findIndex(s => s.name === params.stepName)
					if (stepIdx >= 0) {
						this.currentStepIdx = stepIdx + 1
					}
					else {
						this.currentStepName = null
						this.updateRoute()
					}
				}
			}
		},
		addStep() {
			this.showAddStep = true
		},
		addFlow() {
			this.showAddFlow = true
		},
		stepCreated(step) {
			const idx = this.steps.findIndex(s => s.name === step.name)
			if (idx >= 0) {
				this.currentStepIdx = idx
			}
			this.currentStepName = step.name
		},
		flowCreated(flow) {
			this.flowName = flow.name
		},
		getSortedStep(entityName) {
			return this.sortedSteps.find(ss => ss.name === entityName)
		},
		updateRoute() {
			const params = this.$route.query
			if ( !isHosted && params.flowName != this.flowName || params.stepName != this.currentStepName ) {
				this.$router.push({ name: 'root.integrate', query: this.routeParams })
			}
		},
<<<<<<< HEAD
		async getStepInfo() {
=======
		refreshInfo() {
>>>>>>> develop
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
<<<<<<< HEAD
		refreshInfo() {
			this.getStepInfo()
			this.$store.dispatch('flows/getFlows')
		},
		removeFlow(flowName) {
			this.$store.dispatch('flows/deleteFlow', flowName)
			console.log('removed!', flowName)
		},
		async removeAllData() {
			this.deleteInProgress = true
			for (let i = 0; i < this.entityNames.length; i++) {
				const collection = this.entityNames[i]
				await axios.post("/api/system/deleteCollection", { database: 'final', collection: collection })
			}
			this.deleteInProgress = false
			this.finalData = []
		},
		async removeData(collection) {
			this.deleteInProgress = true
			await axios.post("/api/system/deleteCollection", { database: 'final', collection: collection })
			await this.getStepInfo()
			this.deleteInProgress = false
			this.finalData = this.finalData.filter(c => c.collection !== collection)
		},
		async removeJob(jobId) {
			await jobsApi.deleteJob(jobId)
			this.jobs = this.jobs.filter(job => job.jobId !== jobId)
		},
		getJobs() {
			jobsApi.getJobs(this.flowName).then(jobs => {
				this.jobs = jobs.map(job => job.job)
					.map(job => ({
						...job,
						jobStatus: job.jobStatus.replace(/[_-]/g, ' ')})
					)
					.sort((a, b) => this.$moment(b.timeEnded).diff(this.$moment(a.timeEnded)))
			})
=======
		async removeData(collection) {
			this.deleteInProgress = true
			await axios.post("/api/system/deleteCollection", { database: 'final', collection: collection })
			this.deleteInProgress = false
			this.finalData = this.finalData.filter(c => c.collection !== collection)
>>>>>>> develop
		}
	}
}
</script>

<style lang="less" scoped>
@color-ingest: #ad1457;
@color-mapping: #3F51B5;
@color-mastering: #009688;
@color-custom: #f4511e;

.bar {
	height: 22px;
	border-radius: 4px 4px 0 0;
	display: flex;
	color: white;
	line-height: 22px;
	padding: 0px 5px;

	&.INGESTION {
		background: @color-ingest;
	}
	&.CUSTOM {
		background: @color-custom;
	}
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
<<<<<<< HEAD

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

	.nowidth {
		border: none;
		padding: 0px;
		margin: 0px;
	}
}

.stepsPane,
.flowsPane {
	position: relative;
}

.flowsPane {
	border-right: 1px solid #ccc;
}

=======

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

>>>>>>> develop
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
<<<<<<< HEAD
	}
}

.runHistoryPane {
	border-left: 1px solid #ccc;
	position: relative;

	/deep/ .v-list-item__action {
		margin: 0;
	}

	.runHistoryContents {
		transition: width 0.5s;

		position: absolute;
		padding: 0.25em;
		top: 0;
		right: 0;
		bottom: 0;
		width: 100%;
=======

		// overflow: hidden;
>>>>>>> develop
	}
}

.md4 .manageContents {
	width: 100%;
}

.nowidth .manageContents {
	width: 0px;
}

<<<<<<< HEAD
.manageFlowToggle {
	position: absolute;
	top: 0px;
	left: 20px;
}

.manageDataToggle {
	position: absolute;
	top: 0px;
	right: 20px;
}

.manageHistoryToggle {
	position: absolute;
	top: 0px;
	right: 50px;
=======
.manageDataToggle {
	position: absolute;
	top: 20px;
	right: 30px;
>>>>>>> develop
}

.flex {
	transition: width 0.5s;
}
<<<<<<< HEAD

.flows-title {
	display: flex;
}

.flowsPane {
	tr {
		cursor: default;
	}

	.selected-flow {
		background-color: #eee;
	}
}

.flow-name {
	margin-bottom: 1rem;
}

.fa-angle-right {
	margin-right: 0.75em;

	&.hidden {
		visibility: hidden;
	}
}

/deep/ .theme--light.v-icon,
/deep/ .v-list-group--active > .v-list-group__header .v-list-item,
/deep/ .v-list-group--active > .v-list-group__header .v-list-item__content,
/deep/ .v-list-group--active > .v-list-group__header .v-list-group__header__prepend-icon .v-icon {
	&.fa-exclamation {
		color: red;
	}

	&.fa-check {
		color: green;
	}
}
// .v-icon.fa-exclamation {
// 	margin-left: 0.75em;
// 	color: red;
// }

// .v-icon.fa-check {
// 	color: green;
// 	margin-left: 0.75em;
// }

/deep/ .v-list-item__icon.v-list-group__header__prepend-icon,
.v-list-item__action:first-child,
.v-list-item__icon:first-child {
	margin-right: 16px;
}
=======
>>>>>>> develop
</style>
