<template>
	<v-card class="detailCard" dark v-if="entity">
		<v-card-title data-cy="entityTitle">
			{{ entity.label }} <span class="subtitle">({{ entity.entityName }})</span>
			<v-spacer></v-spacer>
			<v-tooltip v-if="isMerged" bottom>
				<template v-slot:activator="{ on }">
					<v-btn
						@click="mergeHistory"
						right icon small class="small-btn" v-on="on">
						<v-icon>compare_arrows</v-icon>
					</v-btn>
				</template>
				<span>Merge History</span>
			</v-tooltip>

			<v-menu
				v-if="isMerged"
				:close-on-content-click="false"
				:nudge-width="300"
				offset-x
				v-model="confirmUnmergeMenu"
			>
				<template v-slot:activator="{ on: menu }">
					<v-tooltip bottom>
						<template v-slot:activator="{ on: tooltip }">
							<v-btn
								right
								icon
								small
								class="small-btn"
								v-on="{ ...tooltip, ...menu }"
							>
								<v-icon>merge_type</v-icon>
							</v-btn>
						</template>
						<span>Unmerge</span>
					</v-tooltip>
				</template>
				<confirm
					message="Do you really want to unmerge this entity?"
					confirmText="Unmerge"
					@confirm="unmerge"
					@cancel="confirmUnmergeMenu = false"></confirm>
			</v-menu>

			<v-tooltip bottom>
				<template v-slot:activator="{ on }">
					<v-btn
						data-cy="entity.detailsBtn"
						@click="goDetails"
						right icon small class="small-btn" v-on="on">
						<v-icon>list_alt</v-icon>
					</v-btn>
				</template>
				<span>View Document</span>
			</v-tooltip>
		</v-card-title>
		<v-card-text class="overflow">
			<v-tabs
				v-model="active"
				class="overflow"
				dark
			>
				<v-tab ripple>Properties</v-tab>
				<v-tab ripple>Relationships</v-tab>
				<v-tab ripple>Info</v-tab>
				<v-tab-item>
					<p class="no-rels" v-if="filteredProperties.length == 0">No properties</p>
					<v-simple-table v-else>
						<thead>
							<tr>
								<th>Name</th>
								<th>Type</th>
							</tr>
						</thead>
						<tbody>
							<template v-for="(prop, index) in filteredProperties">
								<tr :key="index">
									<td>{{prop.label}}</td>
									<td>
										<template v-if="prop.value && prop.value.length > 100 && !expandedProperty[prop.label]">
											<span>{{prop.value | truncate(100, '')}}</span>
											<a class="more-less" @click="$set(expandedProperty, prop.label, true)">(more...)</a>
										</template>
										<template v-else>
											<span>{{prop.value}}</span>
											<a class="more-less" v-if="prop.value && prop.value.length > 100" @click="$set(expandedProperty, prop.label, false)">(less...)</a>
										</template>
									</td>
								</tr>
							</template>
						</tbody>
					</v-simple-table>
				</v-tab-item>
				<v-tab-item>
					<v-simple-table v-if="hasRelationships">
						<thead>
							<tr>
								<th>Description</th>
								<th>To entity</th>
								<th>Count</th>
							</tr>
						</thead>
						<tbody>
							<tr v-for="edge in Object.keys(entity.edgeCounts)" :key="edge">
								<td>
									<a @click="expandRelationships(entity.edgeCounts[edge])">{{entity.edgeCounts[edge].label}}</a>
									<!-- <span v-else>{{edge.label}}</span> -->
								</td>
								<td>{{entity.edgeCounts[edge].to}}</td>
								<td>{{entity.edgeCounts[edge].count}}</td>
							</tr>
						</tbody>
					</v-simple-table>
					<p class="no-rels" v-else>No relationships</p>
				</v-tab-item>
				<v-tab-item>
					<v-card flat>
						<v-card-title>History</v-card-title>
						<v-card-text>
							<v-timeline dense>
								<v-timeline-item
									small
									v-for="p in prov"
									:key="p.id"
									class="grey--text darken-4">
									<v-card class="elevation-6">
										<v-card-title @click="expandProv(p.id)" class="headline">
											{{p.entity.type}}
											<span class="time">({{p.wasGeneratedBy.time | moment("from", "now")}})</span>
											<v-icon v-if="expandedProv === p.id">mdi-menu-down</v-icon>
											<v-icon v-else>mdi-menu-left</v-icon>

										</v-card-title>
										<transition appear>
											<v-card-text v-show="expandedProv === p.id">
												<v-simple-table>
													<tbody>
														<tr v-if="p.wasAssociatedWith">
															<td>wasAssociatedWith</td>
															<td>{{p.wasAssociatedWith.agent}}</td>
														</tr>
														<tr v-if="p.wasAttributedTo">
															<td>wasAttributedTo</td>
															<td>{{p.wasAttributedTo.agent}}</td>
														</tr>
														<tr v-if="p.wasDerivedFrom">
															<td>wasDerivedFrom</td>
															<td>{{p.wasDerivedFrom.usedEntity}}</td>
														</tr>
														<tr v-if="p.wasInfluencedBy">
															<td>wasInfluencedBy</td>
															<td>{{p.wasInfluencedBy.influencer}}</td>
														</tr>
													</tbody>
												</v-simple-table>
											</v-card-text>
										</transition>
									</v-card>
								</v-timeline-item>
							</v-timeline>
						</v-card-text>
					</v-card>
					<v-card flat>
						<v-card-title>Metadata</v-card-title>
						<v-card-text>
							<v-simple-table>
								<thead>
									<tr>
										<th>Name</th>
										<th>Type</th>
									</tr>
								</thead>
								<tbody>
									<template v-for="(value, key) in advancedProperties">
										<tr :key="key">
											<td>{{key}}</td>
											<td>{{value}}</td>
										</tr>
									</template>
								</tbody>
							</v-simple-table>
						</v-card-text>
					</v-card>
				</v-tab-item>
			</v-tabs>
		</v-card-text>
	</v-card><!--end of panel-->
</template>

<script>

import Confirm from '@/components/Confirm.vue';
import _ from 'lodash';

export default {
	name: 'entity-details',
	props: {
		entity: {type: Object}
	},
	components: {
		Confirm
	},
	data() {
		return {
			active: null,
			activePanel: 'active',
			activeTab: 1,
			provExpanded: {},
			expandedProv: null,
			confirmUnmergeMenu: null,
			expandedProperty: {}
		}
	},
	computed: {
		isMerged() {
			return this.entity && this.entity.uri && this.entity.uri.match('/com.marklogic.smart-mastering/merged/')
		},
		hasRelationships() {
			return Object.keys(this.entity.edgeCounts || []).length > 0
		},
		prov() {
			return this.entity.prov
				.map(p => {
					return {
						...p,
						expanded: false
					}
				})
				.sort((a, b) => this.$moment(a.entity.time).diff(this.$moment(b.entity.time)))
				.reverse()
		},
		filteredProperties() {
			let props = []
			for (let key in this.entity.entity) {
				props.push({
					label: key,
					value: this.entity.entity[key]
				})
			}
			props.sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase()))
			return props
		},
		advancedProperties() {
			return _.pickBy(this.entity, (v, k) => {
				return (k === 'uri');
			})
		},
		propertiesFromCurrentNode() {
			if (this.entity) {
				return Object.keys(this.entity)
			}
			return []
		}
	},
	methods: {
		unmerge() {
			this.$emit('unmerge', this.entity.uri);
			this.confirmUnmergeMenu = false;
		},
		mergeHistory() {
			this.$router.push({ name: 'root.explorer.compare', query: { uri: this.entity.uri } })
		},
		goDetails() {
			this.$router.push({ name: 'root.details', query: { uri: this.entity.uri, db: 'final' } })
		},
		isPropExpanded(prop) {
			return !!prop.expanded
		},
		expandProv(id) {
			if (this.expandedProv === id) {
				this.expandedProv = null;
			}
			else {
				this.expandedProv = id
			}
		},
		expandRelationships(link) {
			this.$emit('expandRelationship', {
				uri: this.entity.uri,
				label: link.label
			});
		}
	}
} //end of export
</script>

<style lang="less" scoped>
* {
	box-sizing: border-box;
}

.expander {
	color: lightGrey;
	padding-right: 3px;
	float: right;
}

.mlLabel {
	color: #343579;
	font-family:"Helvetica Neue",  "Roboto", "Arial", sans-serif
}

.tabcontent {
	background-color: #FFFFFF;
	padding: 10px;
	border: 1px solid #ccc;
	border-radius: 10px;
	box-shadow: 3px 3px 6px #e1e1e1;
}

.panel {
	text-indent:4px;
	margin: 0px;
	padding: 8px;
	width:100%;
	text-align: left;
	font-family: "Roboto", "Helvetica Neue", "Arial", sans-serif;
}

.active {
	border-width:2px;
	color: #fff;
}

/* STYLING */
.container {
	width: 100%;
	margin: 2px;
	padding: 0px;
	font-family: "Nunito Sans", Arial, Helvetica, sans-serif;
	color: #888;
}

/* Style the tabs */
.tabs {
	overflow: hidden;
	margin-top: 10px;
	margin-left: 10px;
	margin-bottom: -52px; /* hide bottom border */
}

.tabs ul {
	list-style-type: none;
	margin-left: 0px;
}

.tabs a {
	float: left;
	cursor: pointer;
	padding: 6px 24px;
	transition: background-color 0.2s;
	border: 1px solid #ccc;
	border-right: none;
	background-color: #f1f1f1;
	border-radius: 10px 10px 0 0;
	font-weight: bold;
	font-size:small;
	color: #5b6770;
}

.tabs a:last-child {
	border-right: 1px solid #ccc;
}

/* Change background color of tabs on hover */
.tabs a:hover {
	background-color: #aaa;
	color: #fff;
}

	/* Styling for active tab */
.tabs a.active {
	background-color: #fff;
	color: #343579;
	border-bottom: 2px solid #fff;
	cursor: default;
}

th {
	font-weight: bold;
}

.no-rels {
	color: black;
	padding: 10px;
}

.theme--light.v-table thead th {
	color: #666;
	font-weight: bold;
}

.subtitle {
	font-size: 80%;
	margin-left: 10px;
}

.text-right {
	text-align: right;
}

.time {
	font-size: 10px;
	margin-left: 15px;
}

.v-timeline-item__body {
	.v-card {
		max-width: 90%;
	}
}

.v-application .headline {
	font-size: 1.2rem !important;

	.v-icon {
		position: absolute;
		right: 5px;
	}
}

.detailCard {
	display: flex;
	flex: 1 1 auto;
	flex-direction: column;
	overflow: auto;
}

.v-card__text {
	display: flex;
	flex-direction: column;
	flex: 1;
}

.overflow {
	overflow-y: auto;
}

.more-less {
	float: right;
}

.v-input {
	flex: 0 0 auto;
}
</style>
