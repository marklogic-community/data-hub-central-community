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
			:position-x="deleteThresholdMenu.x"
			:position-y="deleteThresholdMenu.y"
			absolute
			v-model="confirmDeleteThresholdMenu">
			<confirm
				message="Do you really want to delete this Threshold?"
				confirmText="Delete"
				@confirm="deleteThreshold"
				@cancel="confirmDeleteThresholdMenu = false"></confirm>
		</v-menu>
    <v-row>
			<v-flex md12>
				<v-card class="options">
					<v-card-title>
						<span>Match Options</span>
						<v-spacer></v-spacer>
						<v-btn @click="addOption" data-cy="matching.addOptionBtn">Add</v-btn>
					</v-card-title>
					<v-card-text>
						<v-simple-table>
							<thead>
								<tr>
									<th>Match Ruleset Name</th>
									<th>Weight</th>
									<th>Actions</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="(row, index) in tableData" :key="index">
									<td>{{row.name}}</td>
									<td>{{row.weight}}</td>
									<td>
										<v-btn @click="editOption(row.raw, index)" icon small data-cy="matchStep.editOption"><v-icon small>create</v-icon></v-btn>
										<v-btn @click="showDeleteOption($event, index)" icon small data-cy="matchStep.deleteOption"><v-icon small>delete</v-icon></v-btn>
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
						<span>Match Thresholds</span>
						<v-spacer></v-spacer>
						<v-btn @click="addThreshold" data-cy="matching.addThresholdBtn">Add</v-btn>
					</v-card-title>
					<v-card-text>
						<v-simple-table>
							<thead>
								<tr>
									<th>Threshold Name</th>
									<th>Weight Threshold</th>
									<th>Action</th>
									<th>Actions</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="(threshold, index) in thresholds" :key="index">
									<td>{{threshold.thresholdName}}</td>
									<td>{{threshold.score}}</td>
									<td class="capitalize">{{threshold.action}}</td>
									<td>
										<v-btn @click="editThreshold(threshold, index)" icon small data-cy="matching.editThresholdBtn"><v-icon small>create</v-icon></v-btn>
										<v-btn @click="showDeleteThreshold($event, threshold, index)" icon small data-cy="matching.deleteThresholdBtn"><v-icon small>delete</v-icon></v-btn>
									</td>
								</tr>
							</tbody>
						</v-simple-table>
					</v-card-text>
				</v-card>
			</v-flex>
    </v-row>
		<match-option-dialog
			:option="currentOption"
			:properties="properties"
			:showDialog="showAddOption"
			@save="optionAdded"
			@closed="showAddOption = false"
		></match-option-dialog>
		<match-threshold-dialog
			:threshold="currentThreshold"
			:showDialog="showAddThreshold"
			@save="thresholdAdded"
			@closed="showAddThreshold = false"
		></match-threshold-dialog>
	</v-container>
</template>

<script>
import { mapState } from 'vuex'
import MatchOptionDialog from '@/components/flows/MatchOptionDialog'
import MatchThresholdDialog from '@/components/flows/MatchThresholdDialog'
import Confirm from '@/components/Confirm.vue';

export default {
	name: 'matching-step',
	props: {
		step: {type: Object}
	},
	components: {
		Confirm,
		MatchOptionDialog,
		MatchThresholdDialog
	},
	data() {
		return {
			showAddOption: false,
			confirmDeleteOptionMenu: null,
			deleteOptionMenu: { x: null, y: null },
			currentOption: null,
			currentOptionIndex: -1,

			showAddThreshold: false,
			confirmDeleteThresholdMenu: null,
			deleteThresholdMenu: { x: null, y: null },
			currentThreshold: null,
			currentThresholdIdx: null,

			matchRulesets: [],
			thresholds: []
		}
	},
	computed: {
		properties() {
			return this.targetEntity ? this.targetEntity.properties.map(p => p.name) : []
		},
		tableData() {
			return this.matchRulesets.map((o, idx) => ({
				name: o.name,
				weight: o.weight,
				raw: o,
				index: idx
			}))
		},
		targetEntity() {
			const targetEntityType = String(this.step.targetEntityType || this.step.targetEntity);
			const targetEntityTitle = targetEntityType.substring(targetEntityType.lastIndexOf("/") + 1)
			return this.entities[targetEntityType] || this.entities[targetEntityTitle];
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
				this.$set(this.matchRulesets, this.currentOptionIndex, option)
			}
			else {
				this.matchRulesets.push(option)
				this.$set(this.matchRulesets, this.currentOptionIndex, [...this.matchRulesets])
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
			this.matchRulesets.splice(this.currentOptionIndex, 1)
			this.save()
		},

		addThreshold() {
			this.currentThresholdIdx = -1
			this.currentThreshold = null
			this.showAddThreshold = true
		},
		thresholdAdded(threshold) {
			if (this.currentThresholdIdx >= 0) {
				this.$set(this.thresholds, this.currentThresholdIdx, threshold)
			}
			else {
				this.thresholds.push({
					score: threshold.score,
					action: threshold.action,
					thresholdName: threshold.thresholdName
				})
			}
			this.save()
		},
		editThreshold(threshold, index) {
			this.currentThreshold = threshold
			this.currentThresholdIdx = index
			this.showAddThreshold = true
		},
		showDeleteThreshold($e, threshold, idx) {
			this.currentThreshold = threshold
			this.currentThresholdIdx = idx
			this.deleteThresholdMenu = {
				x: $e.clientX - 100,
				y: $e.clientY
			}
			this.confirmDeleteThresholdMenu = true
		},
		deleteThreshold() {
			this.confirmDeleteThresholdMenu = false
			this.thresholds.splice(this.currentThresholdIdx, 1)
			this.save()
		},
		save() {
			const newStep = {
				...this.step,
				...this.createMatchOptions()
			}
			this.$emit('saveStep', newStep)
		},
		createMatchOptions() {
			let matchRulesets = []
			let getMatchType = this.getMatchType;
			this.matchRulesets.forEach(option => {
				let prefix = option.reduce ? 'Reduce: ' : '';
				let rulesetTitle = option.matchRules.map((rule) => `${rule.entityPropertyPath} - ${getMatchType(rule.matchType)}`).join(', ');
				let matchRuleset = {name: `${prefix}${rulesetTitle}`, weight: option.weight, matchRules: option.matchRules };
				matchRulesets.push(matchRuleset);
			})

			return {
				matchRulesets,
				thresholds: this.thresholds
			}
		},
		getMatchType(algorithmRef) {
			let matchType = 'Exact'
			switch(algorithmRef) {
				case 'doubleMetaphone':
					matchType = 'Double Metaphone'
					break
				case 'Exact':
					matchType = 'Reduce'
					break
				case 'synonym':
					matchType = 'Synonym'
					break
				case 'zip':
					matchType = 'Zip'
					break
				case 'custom':
					matchType = 'Custom'
					break
			}
			return matchType
		},
		updateValues() {
			this.thresholds = [...this.step.thresholds]
			this.matchRulesets = [...this.step.matchRulesets];
		}
	},
	mounted() {
		this.updateValues()
	},
	watch: {
		'$route.params': 'updateValues'
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
