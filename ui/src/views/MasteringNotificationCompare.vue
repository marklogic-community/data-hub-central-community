<template>
	<v-container fluid>
		<v-breadcrumbs :items="breadcrumbs"></v-breadcrumbs>
		<div class="btn-container">
			<v-btn v-if="previous" class="previousButton" @click="goPrevious">Previous</v-btn>
			<v-btn v-if="next" class="nextButton" @click="goNext">Next</v-btn>
		</div>

		<v-snackbar
      v-model="showSnack"
      color="success"
      :timeout="3000"
      :top="true"
    >{{snackMessage}}
		</v-snackbar>
		<compare-docs
			v-if="notification"
			:uris="uris"
			:flowName="flowName"
			:stepNumber="stepNumber"
			:stepName="stepName"
			:showPreview="true"
			:blocked="blocked"
			:merged="merged"
			:mergedDoc="mergedDoc"
			:mergedUri="mergedUri"
			@merge="merge"
			@unmerge="unmerge"
			@block="block"
			@unblock="unblock"
		/>
	</v-container>
</template>

<script>
import CompareDocs from '@/components/mastering/CompareDocs'
import { mapState } from 'vuex'
export default {
	components: {
		CompareDocs
	},
	data() {
		return {
			showSnack: false,
			snackMessage: null,
			breadcrumbs: [
				{
					text: 'Notifications',
					to: { name: 'root.notifications', query: { page: 1 } },
					disabled: false
				},
				{
					text: 'Compare',
					disbled: true
				}
			]
		}
	},
	computed: {
		...mapState({
			notifications: state => state.mastering.allNotifications,
			pagedNotifications: state => state.mastering.pagedNotifications,
			blocks: state => state.mastering.blocks,
			mergedPreview: state => state.mastering.mergedDoc,
			totalUnread: state => state.mastering.totalUnread,
			total: state => state.mastering.total,
			page: state => state.mastering.page,
			pageLength: state => state.mastering.pageLength
		}),
		previous() {
			return this.notificationIndex > 0
		},
		next() {
			return this.notificationIndex < (this.total - 1)
		},
		mergedDoc() {
			return this.mergedPreview || this.notification.merged.doc;
		},
		mergedUri() {
			return this.notification.merged.uri || 'Preview'
		},
		notificationUri() {
			return this.$route.query.notification
		},
		localIndex() {
			return this.pagedNotifications.findIndex(n => n.meta.uri === this.notificationUri)
		},
		notificationIndex() {
			const start = ((this.page - 1) * this.pageLength) + this.localIndex
			return start
		},
		notification() {
			return this.notifications[this.notificationUri]
		},
		flowName() {
			return this.notification ? this.notification.flowInfo.flowName : null
		},
		stepNumber() {
			return this.notification ? this.notification.flowInfo.stepNumber : null
		},
		stepName() {
			return this.notification ? this.notification.flowInfo.stepName : null
		},
		uris() {
			return this.notification ? this.notification.uris : []
		},
		blocked() {
			if (this.blocks && this.uris && this.uris.length > 0) {
				const b = this.blocks[this.uris[0]]
				if (b) {
					const incs = b.includes(this.uris[1])
					return incs
				}
			}
			return false
		},
		merged() {
			return this.notification ? this.notification.meta.merged : false
		}
	},
	methods: {
		merge() {
			this.showAlert('Merge Complete')
		},
		unmerge() {
			this.showAlert('Unmerge Complete')
		},
		block() {
			this.showAlert('Blocked')
		},
		unblock() {
			this.showAlert('Unblocked')
		},
		showAlert(msg) {
			this.snackMessage = msg
			this.showSnack = true
			setTimeout(() => {
				this.showSnack = false
				this.snackMessage = null
			}, 2000)
		},
		goPrevious() {
			const nextLocalIdx = this.localIndex - 1
			if (nextLocalIdx < 0) {
				this.$store.dispatch('mastering/getNotifications', { page: this.page - 1 }).then(() => {
					const notification = this.pagedNotifications[this.pagedNotifications.length - 1]
					this.$router.push({name: 'root.notifications.compare', query: { notification: notification.meta.uri }})
				})
			}
			else {
				const prev = this.pagedNotifications[nextLocalIdx]
				this.$router.push({name: 'root.notifications.compare', query: { notification: prev.meta.uri }})
			}
		},
		goNext() {
			const nextLocalIdx = this.localIndex + 1
			if (nextLocalIdx >= this.pagedNotifications.length) {
				this.$store.dispatch('mastering/getNotifications', { page: this.page + 1 }).then(() => {
					const notification = this.pagedNotifications[0]
					this.$router.push({name: 'root.notifications.compare', query: { notification: notification.meta.uri }})
				})
			}
			else {
				const next = this.pagedNotifications[nextLocalIdx]
				this.$router.push({name: 'root.notifications.compare', query: { notification: next.meta.uri }})
			}
		},
		getNotification() {
			this.$store.dispatch('mastering/getNotification', this.notificationUri).then(() => {
				if (this.notification.meta.status === 'unread') {
					this.$store.dispatch('mastering/updateNotification', { uris: [this.notificationUri], status: 'read' })
				}
				if (!this.merged.doc) {
					this.$store.dispatch('mastering/merge', { uris: this.uris, flowName: this.flowName, stepNumber: this.stepNumber, preview: true})
				}
			})
		}
	},
	mounted: function() {
		this.getNotification()
	},
	watch: {
		'$route.query.notification'() {
			this.getNotification()
		}
	}
}
</script>
<style lang="less" scoped>
.previousButton {
	float: left
}

.nextButton {
	float: right;
}
.btn-container {
	overflow: hidden;
}
</style>
