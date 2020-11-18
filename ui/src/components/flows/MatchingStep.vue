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
									<th>Property To Match</th>
									<th>Match Type</th>
									<th>Weight</th>
									<th>Other</th>
									<th>Actions</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="(row, index) in tableData" :key="index">
									<td>{{row.propertyName}}</td>
									<td>{{row.matchType}}</td>
									<td>{{row.weight}}</td>
									<td>
										<template v-if="row.zip5match9">
											<div>
												<span class="lbl">5-Matches-9 Boost:</span><span class="value">{{row.zip5match9}}</span>
											</div>
											<div>
												<span class="lbl">9-Matches-5 Boost:</span><span class="value">{{row.zip9match5}}</span>
											</div>
										</template>
									</td>
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
									<td>{{threshold.label}}</td>
									<td>{{threshold.above}}</td>
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

			options: [],
			thresholds: []
		}
	},
	computed: {
		properties() {
			return this.targetEntity ? this.targetEntity.properties.map(p => p.name) : []
		},
		tableData() {
			return this.options.map((o, idx) => ({
				propertyName: o.propertyName || o.propertiesReduce.join(', '),
				matchType: this.getMatchType(o.algorithmRef),
				weight: o.weight,
				raw: o,
				index: idx,
				zip5match9: o.zip5match9,
				zip9match5: o.zip9match5
			}))
		},
		targetEntity() {
			return this.entities[this.step.options.targetEntity]
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
					above: threshold.above,
					action: threshold.action,
					label: threshold.label
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
				options: {
					...this.step.options,
					matchOptions: this.createMatchOptions()
				}
			}
			this.$emit('saveStep', newStep)
		},
		createMatchOptions() {
			let scoring = {
				add: [],
				expand: [],
				reduce: []
			}
			let props = {}

			this.options.forEach(option => {
				if (option.propertyName) {
					props[option.propertyName] = option.propertyName
				}
				else if (option.propertiesReduce) {
					option.propertiesReduce.forEach(p => props[p] = p)
				}

				if (!option.algorithmRef || option.algorithmRef === 'exact') {
					scoring.add.push({
						propertyName: option.propertyName,
						weight: option.weight
					})
				}
				else if (option.algorithmRef === 'standard-reduction') {
					scoring.reduce.push({
						algorithmRef: option.algorithmRef,
						weight: option.weight,
						allMatch: {
							property: option.propertiesReduce
						}
					})
				}
				else if (option.algorithmRef === 'zip-match') {
					scoring.expand.push({
						propertyName: option.propertyName,
						algorithmRef: option.algorithmRef,
						zip: [
							{ origin: 5, weight: option.zip5match9 },
							{ origin: 9, weight: option.zip9match5 }
						]
					})
				}
			})

			return {
				...this.matchOptions,
				scoring,
				propertyDefs: {
					properties: Object.values(props).map(p => ({
						localname: p,
						name: p
					}))
				},
				thresholds: {
					threshold: this.thresholds
				}
			}
		},
		getMatchType(algorithmRef) {
			let matchType = 'Exact'
			switch(algorithmRef) {
				case 'double-metaphone':
					matchType = 'Double Metaphone'
					break
				case 'standard-reduction':
					matchType = 'Reduce'
					break
				case 'zip-match':
					matchType = 'Zip'
					break
			}
			return matchType
		},
		updateValues() {
			this.thresholds = this.step.options.matchOptions.thresholds.threshold.map(t => ({...t}))
			this.options = Object.values(this.step.options.matchOptions.scoring).reduce((output, s) => {
				return output.concat(s.map(ss => {
					const obj = {
						algorithmRef: ss.algorithmRef,
						propertyName: ss.propertyName,
						propertiesReduce: (ss.allMatch && ss.allMatch.property),
						weight: ss.weight,
						zip5match9: ss.zip5match9,
						zip9match5: ss.zip9match5
					}
					if (ss.zip) {
						Object.values(ss.zip).forEach(z => {
							if (z.origin === 5) {
								obj.zip5match9 = z.weight
							}
							else if (z.origin === 9) {
								obj.zip9match5 = z.weight
							}
						})
					}
					return obj
				}))
			}, [])
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
