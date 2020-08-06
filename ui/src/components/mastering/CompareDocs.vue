<template>
	<v-layout column align-center>
		<v-flex md12>
			<v-layout row justify-center v-if="entityType && showActions">
				<v-card>
					<v-card-text class="text-center">
						<div class="entityInfo">
							<strong>Entity:</strong><span>{{entityType}}</span>
						</div>
						<div class="flowInfo">
							<span class="flowName"><strong>Flow:</strong> {{flowName}}</span>
							<span class="stepName"><strong>Step:</strong> {{stepName}}</span>
						</div>
						<div>
							<v-btn v-if="blocked" data-cy="mastering.unblockButton" @click="unblock">Unblock this Match</v-btn>
							<template v-else>
								<v-btn v-if="merged" data-cy="mastering.unmergeButton" @click="unmerge">UnMerge</v-btn>
								<template v-else>
									<v-btn data-cy="mastering.mergeButton" @click="merge">Merge</v-btn>
									<v-btn data-cy="mastering.blockButton" @click="block">Block this Match</v-btn>
								</template>
							</template>
						</div>

					</v-card-text>
				</v-card>
			</v-layout>

			<v-layout row>
				<v-card v-if="Object.keys(masteringDocs).length > 0">
					<v-card-text>
						<v-simple-table v-if="entityType" dense>
							<thead>
								<tr>
									<th>Property</th>
									<th v-for="(uri, index) in Object.keys(masteringDocs)" :key="uri" :class="`doc${index}`">
										<v-tooltip top>
											<template v-slot:activator="{ on }">
												<span v-on="on">{{uri | truncate(25, '')}}</span>
											</template>
											<span>{{uri}}</span>
										</v-tooltip>
									</th>
									<th class="preview">
										<v-tooltip top>
											<template v-slot:activator="{ on }">
												<strong v-on="on">Merged: {{mergedUri | truncate(25, '')}}</strong>
											</template>
											<span>{{mergedUri}}</span>
										</v-tooltip>

									</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="prop in props" :key="prop" :class="getClass(prop)">
									<td class="propName"><strong>{{prop}}</strong></td>
									<template v-for="(rowProp, index) in rowProps(prop)">
										<td :key="index" :class="`doc${index}`">
											<v-tooltip top>
												<template v-slot:activator="{ on }">
													<span v-on="on">{{rowProp | truncate(50, '')}}</span>
												</template>
												<span>{{rowProp}}</span>
											</v-tooltip>
										</td>
									</template>
									<td class="preview">
										<v-tooltip top>
											<template v-slot:activator="{ on }">
												<span v-on="on">{{mergedProp(prop) | truncate(50, '')}}</span>
											</template>
											<span>{{mergedProp(prop)}}</span>
										</v-tooltip>
									</td>
								</tr>
							</tbody>
						</v-simple-table>
					</v-card-text>
				</v-card>
			</v-layout>

			<v-layout row justify-center v-if="entityType && showActions">
				<v-card>
					<v-card-text class="text-center">
						<v-btn v-if="blocked" data-cy="mastering.unblockButton" @click="unblock">Unblock this Match</v-btn>
						<template v-else>
							<v-btn v-if="merged"  data-cy="mastering.unmergeButton" @click="unmerge">UnMerge</v-btn>
							<template v-else>
								<v-btn data-cy="mastering.mergeButton" @click="merge">Merge</v-btn>
								<v-btn data-cy="mastering.blockButton" @click="block">Block this Match</v-btn>
							</template>
						</template>
					</v-card-text>
				</v-card>
			</v-layout>
		</v-flex>
	</v-layout>
</template>

<script>
import { mapState } from 'vuex'
import * as _ from 'lodash'
export default {
	props: {
		showActions: { type: Boolean, default: true },
		flowName: { type: String },
		stepName: { type: String },
		stepNumber: { type: String },
		uris: { type: Array },
		mergedDoc: { type: Object },
		mergedUri: { type: String },
		merged: { type: Boolean, default: false },
		blocked: { type: Boolean, default: false }
	},
	computed: {
		...mapState({
			allMasteringDocs: state => state.mastering.docs,
			model: state => state.model.model
		}),
		masteringDocs() {
			return this.uris.reduce((item, uri) => {
				const doc = this.allMasteringDocs[uri]
				if (doc) {
					item[uri] = doc
				}
				return item
			}, {})
		},
		entityType() {
			const docs = Object.values(this.masteringDocs)
			if (docs && docs.length > 0) {
				return docs[0].envelope.instance.info.title
			}
			return null
		},
		entity() {
			return (this.entityType && this.model && this.model.nodes) ? this.model.nodes[this.entityType.toLowerCase()] : null
		},
		props() {
			return this.entity ? this.entity.properties.map(p => p.name) : []
		}
	},
	methods: {
		rowProps(prop) {
			return Object.values(this.masteringDocs).map(d => JSON.stringify(d.envelope.instance[this.entityType][prop]))
		},
		mergedProp(prop) {
			return this.mergedDoc ? JSON.stringify(this.mergedDoc.envelope.instance[this.entityType][prop]) : null
		},
		getClass(prop) {
			const props = Object.values(this.masteringDocs).map(d => d.envelope.instance[this.entityType][prop])
			const unique = _.uniq(props)
			if (unique.length === 1) {
				return 'same'
			}
			return 'delta'
		},
		block() {
			this.$store.dispatch('mastering/block', this.uris).then(() => {
				this.$emit('block')
			})
		},
		unblock() {
			this.$store.dispatch('mastering/unblock', this.uris).then(() => {
				this.$emit('unblock')
			})
		},
		merge() {
			this.$store.dispatch('mastering/merge', { uris: this.uris, flowName: this.flowName, stepNumber: this.stepNumber, preview: false }).then(() => {
				this.$emit('merge')
			})
		},
		unmerge() {
			this.$store.dispatch('mastering/unmerge', this.mergedUri).then(() => {
				this.$emit('unmerge')
			})
		}
	},
	mounted: function() {
		this.$store.dispatch('mastering/getDocs', this.uris.slice(0, 6))
	},
	watch: {
		uris() {
			this.$store.dispatch('mastering/getDocs', this.uris.slice(0, 6))
		}
	}
}
</script>
<style lang="less" scoped>
@delta: #EF798A;
@previewBorder: #aaa;
@doc0: #8E9AAF;
@doc1: #CBC0D3;
@doc2: #EFD3D7;
@doc3: #FEEAFA;
@doc4: #DEE2FF;

.theme--light.v-data-table tbody tr.delta:hover:not(.v-data-table__expanded__content) {
	background-color: #ffaaaa;
}

.delta {
	background-color: @delta;
	color: black;
}

button {
	margin-right: 10px;

	&:last-child {
		margin-right: 0px;
	}
}

.same {
	td.preview {
		background-color: #ddffdd;
	}
	td.doc0 {
		background-color: @doc0;
	}
	td.doc1 {
		background-color: @doc1;
	}
}

th.doc0 {
	background-color: @doc0;
}
th.doc1 {
	background-color: @doc1;
}

th {
	font-size: 20px;
	padding: 10px;
}
.theme--light.v-data-table thead tr th {
	color: black;
}
th.preview {
	background-color: #ddffdd;
	border-top: 1px solid @previewBorder;
}

th.preview, td.preview {
	border-left: 1px solid @previewBorder;
	border-right: 1px solid @previewBorder;
}

td.preview:last-child {
	border-bottom: 1px solid @previewBorder;
}

.flowInfo {
	margin-bottom: 10px;
}

.flowName {
	margin-right: 10px;
}

.row {
	margin-bottom: 10px;
}
</style>
