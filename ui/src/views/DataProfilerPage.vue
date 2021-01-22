<template>
	<v-container fluid class="DataProfilerPage">
		<v-layout row class="fullHeight">
			<v-flex md12>
				<v-btn @click="profile">Create Profile</v-btn>
				<v-data-table
					v-if="reports"
					:headers="reportHeaders"
					:items="reportItems"
					:items-per-page="10"
					class="elevation-1"
				>
					<template v-slot:header.actions>
						<delete-data-confirm
							tooltip="Delete All Reports"
							message="Do you really want to delete all Reports?"
							:disabled="(deleteInProgress || reports.length <= 0)"
							:collection="''"
							:deleteInProgress="deleteInProgress"
							@deleted="removeAllReports($event)"/>
					</template>
					<template v-slot:item.name="{ item }">
						<router-link :to="{ name: 'root.profiles.profile', query: { uri: encodeURIComponent(item.uri) }}">{{item.name}}</router-link>
					</template>

					<template v-slot:item.actions="{ item }">
						<delete-data-confirm
							tooltip="Delete Report"
							message="Do you really want to delete this report?"
							:collection="item.uri"
							:disabled="deleteInProgress"
							:deleteInProgress="deleteInProgress"
							@deleted="deleteReport($event)"/>
					</template>
				</v-data-table>
			</v-flex>
		</v-layout>
		<run-profile-dialog ref="runProfileDlg" />
	</v-container>
</template>

<script>
import profilerApi from '@/api/ProfilerApi'

import RunProfileDialog from '@/components/RunProfileDialog.vue'
import DeleteDataConfirm from '@/components/DeleteDataConfirm'

export default {
	components: {
		RunProfileDialog,
		DeleteDataConfirm
	},
	computed: {
		reportHeaders() {
			return [
				{ text: 'Data Source', value: 'name' },
				{ text: 'Sample Size', value: 'total' },
				{ text: 'Created', value: 'created' },
				{ text: '', value: 'actions' }
			]
		},
		reportItems() {
			return this.reports.map(r => (
				{
					...r,
					created: this.$moment(r.created).fromNow()
				}
			))
		},
	},
	data() {
		return {
			reports: [],
			deleteInProgress: false
		}
	},
	methods: {
		getReports() {
			profilerApi.getReports(this.currentPage, this.pageLength).then(resp => {
				this.reports = resp.reports
			})
		},
		removeAllReports() {
			profilerApi.deleteAllReports().then(() => {
				this.reports = []
			})
		},
		deleteReport(uri) {
			profilerApi.deleteReport(uri).then(() => {
				this.reports = this.reports.filter(r => r.uri !== uri)
			})
		},
		profile() {
			this.$refs.runProfileDlg.open().then(({collection, database, sampleSize}) => {
				if (collection) {
					profilerApi.profile({collection, database, sampleSize}).then(() => {})
				}
			})
		}
	},
	mounted() {
		this.$ws.subscribe('/topic/status', tick => {
			const msg = tick.body
			if (msg.percentComplete >= 100) {
				this.getReports()
			}
		})
		this.getReports()
	}
}
</script>

<style lang="less" scoped>
.fullHeight {
	height: 100%;
	position: relative;
}

.DataProfilerPage {
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
</style>
