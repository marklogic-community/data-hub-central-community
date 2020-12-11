<template>
	<div id="exportContainer">
	<fieldset class="col-sm-9" v-if="entityNames.length > 0">
			<legend>Entities</legend>
			<div v-if="!hasEntities">You don't have any entity instances yet.</div>
			<v-simple-table dense>
				<thead>
					<tr>
						<th>
							<v-checkbox
								data-cy="export.checkAll"
								v-model="allChecked"
								:disabled="!hasEntities"
							></v-checkbox>
						</th>
						<th>Entity Name</th>
						<th>Document Count</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="(entity, index) in entityNames" :key="entity">
						<td class="smallcol">
							<v-checkbox dense
								:data-cy="`export.${entity}`"
								v-model="checkEntities[index]"
								:disabled="(collectionCounts[entity] || 0) === 0"></v-checkbox>
						</td>
						<td >{{entity}}</td>
						<td>{{collectionCounts[entity] || 0}}</td>
					</tr>
				</tbody>
			</v-simple-table>
			<v-btn color="primary" data-cy="export.exportButton" class="right" :disabled="selectedEntities.length <= 0" v-on:click="runExports" aria-label="Export entities.">Export</v-btn>
		</fieldset>
		<div v-else>
			You need to define some Entities on the <router-link :to="{name: 'root.modeler'}">Connect Page</router-link>.
		</div>
		<fieldset class="col-sm-9" v-if="exportJobs.length > 0">
			<legend>Exports</legend>
				<v-simple-table dense>
				<tbody>
					<tr v-for="job in exportJobs" :key="job.id" :data-cy="`export.${job.id}`">
						<td><a :href="downloadLink(job.id)"><v-icon>cloud_download</v-icon> {{job.name}}</a></td>
						<td>{{timeAgo(job.creationDate)}}</td>
						<td>
							<delete-data-confirm
								tooltip="Delete Exported Data"
								message="Do you really want to delete this exported data?"
								:collection="job.id"
								:deleteInProgress="deleteInProgress"
								@deleted="removeData($event)"/>
						</td>
					</tr>
				</tbody>
			</v-simple-table>
		</fieldset>
	</div>
</template>


<script>
import axios from 'axios'
import { mapState, mapActions } from 'vuex'
import flowsApi from '@/api/FlowsApi'
import DeleteDataConfirm from '@/components/DeleteDataConfirm'

export default {
	name:'ExportPage',
	components: {
		DeleteDataConfirm
	},
	data: ()=> ({
		deleteInProgress: false,
		checkEntities: {},
		exportError: '' ,
		datahub: '',
		exportJobs: [],
		showExportStatus:false,
		requestStatus: "green",
		finalData: []
	}),
	computed:{
		...mapState({
			entities: state => Object.values(state.flows.entities)
		}),
		entityNames() {
			return this.entities.map(e => e.info.title)
		},
		entityNamesWithDocs() {
			return this.entityNames.filter(name => (this.collectionCounts[name] || 0) > 0)
		},
		allChecked: {
			get() {
				return this.checkedIndexes.length > 0 && this.checkedIndexes.length === this.entityNamesWithDocs.length
			},
			set(val) {
				this.entityNames.forEach((n, index) => {
					if ((this.collectionCounts[n] || 0) > 0) {
						this.$set(this.checkEntities, index, val)
					}
				})
			}
		},
		checkedIndexes() {
			return Object.keys(this.checkEntities).filter(key => this.checkEntities[key])
		},
		selectedEntities() {
			return this.entityNames.filter((v, idx) => this.checkedIndexes.findIndex(x => x == idx) >= 0)
		},
		collectionCounts() {
			return this.finalData.reduce((p, c) => {
				p[c.collection] = c.count
				return p
			}, {})
		},
		hasEntities() {
			return this.entityNames.reduce((p, c) => {
				return p + (this.collectionCounts[c] || 0)
			}, 0) > 0
		}
	},
	created () {
		this.getEntities()
	},
	methods: {
		...mapActions({
			getEntities: 'flows/getEntities'
		}),
		timeAgo(time) {
			return this.$moment(time).fromNow()
		},
		async runExports() {
			axios.post("/api/export/runExports", this.selectedEntities)
				.then(response => {
					return response.data
				})
				.catch(error => {
					console.error('error:', error)
					return error
				})
		},
		getExports() {
			return axios.get("/api/export/getExports/")
				.then(response => {
					this.exportJobs = response.data.sort((a,b) => this.$moment(b.creationDate).diff(this.$moment(a.creationDate)))
					return response.data
				})
				.catch(error => {
					console.error('error:', error)
					return error
				})
		},
		refreshInfo() {
			flowsApi.getNewStepInfo().then(info => {
				this.finalData = info.collections.final
			})
		},
		downloadLink(exportId) {
			return `/api/export/downloadExport/?exportId=${encodeURIComponent(exportId)}&token=${localStorage.getItem('access_token')}`
		},
		async removeData(exportId) {
			this.deleteInProgress = true
			await axios.get(`/api/export/deleteExport/?exportId=${encodeURIComponent(exportId)}&token=${localStorage.getItem('access_token')}`)
			this.deleteInProgress = false
			this.exportJobs = this.exportJobs.filter(c => c.id !== exportId)
		}
	},
	mounted() {
		this.$ws.subscribe('/topic/status', tick => {
			const msg = tick.body
			if (msg.percentComplete >= 100) {
				this.getExports()
			}
		})
		this.getExports()
		this.refreshInfo()
	}
}

</script>

<style scoped>
	#exportContainer {
		padding-left: 50px;
		padding-right: 50px;
		margin-top: 10px;
	}
	.right {
		float: right;
		margin-left:10px;
	}
	h1 {
		padding-bottom: 30px;
	}
	.adminItem {
		padding: 10px;
		border : 1px solid black;
		border-radius: 5px;
		margin-bottom: 10px;
	}
	fieldset {
		padding: 10px;
		border : 1px solid black;
		border-radius: 5px;
		margin-bottom: 10px;
	}
	fieldset legend {
		font-weight: bold;
	}
	.success, .error{
		border-radius: 3px;
		padding-left: 5px;
	}
	.smallcol {
		width: 100px;
	}
	.code {
		padding-left: 20px;
		font-size: 1.1em;
		color: darkred;
	}
	.clickable-row {
		cursor: pointer;
	}

	a {
		text-decoration: none;
	}
</style>
