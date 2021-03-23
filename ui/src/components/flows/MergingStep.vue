<template>
	<v-container>
		<v-menu
			:close-on-content-click="false"
			:position-x="deleteOptionMenu.x"
			:position-y="deleteOptionMenu.y"
			absolute
			v-model="confirmDeleteOptionMenu">
			<confirm
				message="Do you really want to delete this option?"
				confirmText="Delete"
				@confirm="deleteOption"
				@cancel="confirmDeleteOptionMenu = false"></confirm>
		</v-menu>
		<v-menu
			:close-on-content-click="false"
			:position-x="deleteStrategyMenu.x"
			:position-y="deleteStrategyMenu.y"
			absolute
			v-model="confirmDeleteStrategyMenu">
			<confirm
				message="Do you really want to delete this Strategy?"
				confirmText="Delete"
				@confirm="deleteStrategy"
				@cancel="confirmDeleteStrategyMenu = false"></confirm>
		</v-menu>
    <v-row>
			<v-flex md12>
				<v-card class="options">
					<v-card-title>
						<span>Merge Options</span>
						<v-spacer></v-spacer>
						<v-btn @click="addOption" data-cy="merging.addOptionBtn">Add</v-btn>
					</v-card-title>
					<v-card-text>
						<v-simple-table id="optionsTable">
							<thead>
								<tr>
									<th>Property to Merge</th>
									<th>Merge Strategy</th>
									<th>Max Values</th>
									<th>Max Sources</th>
									<th>Source Weights</th>
									<th>Length Weight</th>
									<th>Actions</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="(option, index) in expandedOptions" :key="index">
									<td>{{option.entityPropertyPath}}</td>
									<td>{{option.mergeStrategyName}}</td>
									<td>{{option.maxValues}}</td>
									<td>{{option.maxSources}}</td>
									<td>
										<div v-if="option.priorityOrder && option.priorityOrder.sources">
											<div v-for="source in option.priorityOrder.sources" :key="source.name">
												<span class="lbl">{{source.sourceName}}:</span><span class="value">{{source.weight}}</span>
											</div>
										</div>
									</td>
									<td>{{(option.priorityOrder || {}).lengthWeight}}</td>
									<td>
										<v-btn @click="editOption(option, index)" icon small data-cy="mergeStep.editOption"><v-icon small>create</v-icon></v-btn>
										<v-btn @click="showDeleteOption($event, index)" icon small data-cy="mergeStep.deleteOption"><v-icon small>delete</v-icon></v-btn>
									</td>
								</tr>
							</tbody>
						</v-simple-table>
					</v-card-text>
				</v-card>
			</v-flex>
    </v-row>
    <v-row>
			<v-flex md12>
				<v-card>
					<v-card-title>
						<span>Merge Strategies</span>
						<v-spacer></v-spacer>
						<v-btn @click="addStrategy" data-cy="merging.addStrategyBtn">Add</v-btn>
					</v-card-title>
					<v-card-text>
						<v-simple-table id="strategiesTable">
							<thead>
								<tr>
									<th>Strategy Name</th>
									<th>Max Values</th>
									<th>Max Sources</th>
									<th>Source Weights</th>
									<th>Length Weight</th>
									<th>Actions</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="(strategy, index) in strategies" :key="index">
									<td>{{strategy.strategyName}}</td>
									<td>{{strategy.maxValues}}</td>
									<td>{{strategy.maxSources}}</td>
									<td>
										<div v-if="strategy.priorityOrder && strategy.priorityOrder.sources">
											<div v-if="strategy.priorityOrder" v-for="(source, idx) in strategy.priorityOrder.sources" :key="idx">
												<span class="lbl">{{source.sourceName}}:</span><span class="value">{{source.weight}}</span>
											</div>
										</div>
									</td>
									<td>{{ (strategy.priorityOrder || {}).lengthWeight}}</td>
									<td>
										<v-btn @click="editStrategy(strategy, index)" icon small data-cy="merging.editStrategyBtn"><v-icon small>create</v-icon></v-btn>
										<v-tooltip v-if="!strategy.default" top>
											<template v-slot:activator="{ on }">
												<span v-on="on">
													<v-btn
														data-cy="merging.deleteStrategyBtn"
														:disabled="strategyInUse(strategy.strategyName)"
														@click="showDeleteStrategy($event, strategy, index)"
														icon small><v-icon small>delete</v-icon></v-btn>
												</span>
											</template>
											<span>{{strategyInUse(strategy.strategyName) ? 'Can\'t delete a strategy in use' : 'Delete Strategy'}}</span>
										</v-tooltip>
									</td>
								</tr>
							</tbody>
						</v-simple-table>
					</v-card-text>
				</v-card>
			</v-flex>
    </v-row>
		<merge-option-dialog
			:option="currentOption"
			:properties="properties"
			:strategies="strategyNames"
			:showDialog="showAddOption"
			@save="optionAdded"
			@closed="showAddOption = false"
		></merge-option-dialog>
		<merge-strategy-dialog
			:strategy="currentStrategy"
			:strategies="strategyNames"
			:showDialog="showAddStrategy"
			@save="strategyAdded"
			@closed="showAddStrategy = false"
		></merge-strategy-dialog>
	</v-container>
</template>

<script>
import { mapState } from 'vuex'
import MergeOptionDialog from '@/components/flows/MergeOptionDialog'
import MergeStrategyDialog from '@/components/flows/MergeStrategyDialog'
import Confirm from '@/components/Confirm.vue';

export default {
	props: {
		step: {type: Object}
	},
	components: {
		Confirm,
		MergeOptionDialog,
		MergeStrategyDialog
	},
	data() {
		return {
			showAddOption: false,
			confirmDeleteOptionMenu: null,
			deleteOptionMenu: { x: null, y: null },
			currentOption: null,
			currentOptionIndex: -1,

			showAddStrategy: false,
			confirmDeleteStrategyMenu: null,
			deleteStrategyMenu: { x: null, y: null },
			currentStrategy: null,
			currentStrategyIdx: -1,

			options: [],
			strategies: []
		}
	},
	computed: {
		properties() {
			return this.targetEntity ? this.targetEntity.properties.map(p => p.name) : []
		},
		strategyNames() {
			return this.strategies ? this.strategies.filter(s => !s.default).map(s => s.strategyName) : []
		},
		targetEntity() {
			const targetEntityType = String(this.step.targetEntityType || this.step.targetEntity);
			const targetEntityTitle = targetEntityType.substring(targetEntityType.lastIndexOf("/") + 1);
			return this.entities[targetEntityType] || this.entities[targetEntityTitle];
		},
		expandedOptions() {
			return this.options.map(o => ({
				...o,
				...(o.mergeStrategyName ? this.strategies.find(s => s.strategyName === o.mergeStrategyName) : {}),
				raw: o
			}))
		},
		...mapState({
			entities: state => state.flows.entities
		})
	},
	methods: {
		addOption() {
			this.currentOptionIndex = -1
			this.currentOption = null
			this.showAddOption = true
		},
		optionAdded(option) {
			if (this.currentOptionIndex >= 0) {
				this.$set(this.options, this.currentOptionIndex, option)
			}
			else {
				this.options.push(option)
				this.$set(this.options, this.currentOptionIndex, [...this.options])
			}
			this.save()
		},
		editOption(option, index) {
			this.currentOptionIndex = index
			this.currentOption = option
			this.showAddOption = true
		},
		showDeleteOption($e, index) {
			this.currentOptionIndex = index
			this.deleteOptionMenu = {
				x: $e.clientX - 100,
				y: $e.clientY
			}
			this.confirmDeleteOptionMenu = true
		},
		deleteOption() {
			this.confirmDeleteOptionMenu = false
			this.options.splice(this.currentOptionIndex, 1)
			this.save()
		},

		addStrategy() {
			this.currentStrategyIdx = -1
			this.currentStrategy = null
			this.showAddStrategy = true
		},
		strategyAdded(strategy) {
			if (this.currentStrategyIdx >= 0) {
				const oldStrategy = this.strategies[this.currentStrategyIdx]
				if (oldStrategy && oldStrategy.name) {
					for (let i = 0; i < this.options.length; i++) {
						let option = this.options[i]
						if (option.strategy === oldStrategy.name) {
							this.$set(this.options, i, {
								...option,
								strategy: strategy.name
							})
						}
					}
					this.options = this.options.map(option => {
						if (option.strategy === oldStrategy.name) {
							option.strategy = strategy.name
						}
						return option
					})
				}
				this.$set(this.strategies, this.currentStrategyIdx, strategy)

			}
			else {
				this.strategies.push(strategy)
			}
			this.save()
		},
		editStrategy(strategy, index) {
			this.currentStrategy = strategy
			this.currentStrategyIdx = index
			this.showAddStrategy = true
		},
		showDeleteStrategy($e, strategy, idx) {
			this.currentStrategy = strategy
			this.currentStrategyIdx = idx
			this.deleteStrategyMenu = {
				x: $e.clientX - 100,
				y: $e.clientY
			}
			this.confirmDeleteStrategyMenu = true
		},
		deleteStrategy() {
			this.confirmDeleteStrategyMenu = false
			this.strategies.splice(this.currentStrategyIdx, 1)
			this.save()
		},
		strategyInUse(strategy) {
			return !!this.options.find(o => o.strategy === strategy)
		},
		save() {
			let defaultStrategy = {
				sourceWeights: [],
				default: true
			}
			const ds = this.strategies.find(s => s.default)
			if (ds) {
				defaultStrategy = {
					...defaultStrategy,
					...ds
				}
			}

			const mergeRules = this.options
				.concat([defaultStrategy])
			const newStep = {
				...this.step,
				mergeRules,
				mergeStrategies: this.strategies.filter(o => !o.default)
			}
			this.$emit('saveStep', newStep)
		},
		updateValues() {
			this.strategies = (this.step.mergeStrategies || [])
				.map(t => ({...t}))
				.concat([
					{
						strategyName: 'Default',
						default: true,
						maxValues: null,
						maxSources: null,
						priorityOrder: {
							lengthWeight:null,
							sources: []
						},
						...this.step.mergeRules.find(m => m.default)
					}
				])
			this.options = (this.step.mergeRules || []).filter(m => m.entityPropertyPath)
		}
	},
	mounted() {
		this.updateValues()
	},
	watch: {
		'$route.params': 'updateValues',
	}
}
</script>

<style lang="less" scoped>
span.lbl {
	font-weight: bold;
	margin-right: 10px;
	color: #666;
}

.v-card.options {
	margin-bottom: 2em;
}

.capitalize {
	text-transform: capitalize;
}
</style>
