<template>
	<v-container fluid>
		<v-layout column align-center>
			<v-layout row>
				<h1>Notifications</h1>
			</v-layout>
			<v-layout row>
				<v-card>
					<v-card-text>
						<v-simple-table>
							<thead>
								<tr>
									<th>
										<v-checkbox
											data-cy="mastering.checkAll"
											v-model="allChecked"
										></v-checkbox>
									</th>
									<th>Labels</th>
									<th>Type</th>
									<th>Matches</th>
									<th>Date</th>
									<th></th>
									<th>
											<v-menu offset-y>
												<template v-slot:activator="{ on }">
													<v-icon data-cy="mastering.actionMenu" v-on="on">more_vert</v-icon>
												</template>
												<v-list>
													<v-list-item :disabled="this.allCheckedNotifications.length === 0" @click="markUnread">
														<v-list-item-title>Mark unread</v-list-item-title>
													</v-list-item>
													<v-list-item :disabled="this.allCheckedNotifications.length === 0" @click="merge">
														<v-list-item-title>Merge</v-list-item-title>
													</v-list-item>
													<v-list-item :disabled="this.allCheckedNotifications.length === 0" @click="unmerge">
														<v-list-item-title>Unmerge</v-list-item-title>
													</v-list-item>
												</v-list>
											</v-menu>
									</th>
								</tr>
							</thead>
							<tbody>
								<template v-for="(notification, index) in notifications">
									<tr
										:key="index"
										:class="notification.meta.status"
									>
										<td><v-checkbox
											v-model="checkedNotifications[index]"
										></v-checkbox></td>
										<td @click="gotoCompare(notification)">{{getLabel(notification)}}</td>
										<td @click="gotoCompare(notification)">{{notification.thresholdLabel}}</td>
										<td @click="gotoCompare(notification)">{{notification.uris.length}}</td>
										<td @click="gotoCompare(notification)">{{notification.meta.dateTime}}</td>
										<td style="width: 58px;" @click="gotoCompare(notification)">
											<v-tooltip top v-if="notification.meta.merged">
												<template v-slot:activator="{ on }">
													<v-icon v-on="on">merge_type</v-icon>
												</template>
												<span>Merged</span>
											</v-tooltip>
											<v-tooltip top v-if="notification.meta.blocked">
												<template v-slot:activator="{ on }">
													<v-icon v-on="on">block</v-icon>
												</template>
												<span>Blocked From Merge</span>
											</v-tooltip>
										</td>
										<td @click="gotoCompare(notification)">
										</td>
									</tr>
								</template>
							</tbody>
						</v-simple-table>
						<v-pagination
							v-if="totalPages > 1"
							v-model="currentPage"
							:length="totalPages"
							circle
							/>
					</v-card-text>
				</v-card>
			</v-layout>
		</v-layout>
	</v-container>
</template>

<script>
import { mapState } from 'vuex'

export default {
	data() {
		return {
			checkedNotifications: {},
			actions: [
				{
					label: 'Mark Unread',
					value: 'markUnread'
				}
			]
		}
	},
	computed: {
		...mapState({
			profile: state => state.auth.profile,
			notifications: state => state.mastering.pagedNotifications,
			page: state => state.mastering.page,
			pageLength: state => state.mastering.pageLength,
			total: state => state.mastering.total
		}),
		checkedIndexes() {
			return Object.keys(this.checkedNotifications).filter(key => this.checkedNotifications[key])
		},
		allCheckedNotifications() {
			return this.checkedIndexes.map(idx => this.notifications[idx])
		},
		allChecked: {
			get() {
				return this.checkedIndexes.length === this.notifications.length
			},
			set(val) {
				this.notifications.forEach((n, index) => {
					this.$set(this.checkedNotifications, index, val)
				})
			}
		},
		currentPage: {
			get() {
				return this.page
			},
			set(val) {
				this.$store.dispatch('mastering/getNotifications', { page: val });
				if (this.$route.query.page != val) {
					this.$router.push({ name: 'root.notifications', query: { page: val} })
				}
			}
		},
		totalPages() {
			console.log('totalPages', this.total, this.pageLength, Math.ceil(this.total / this.pageLength))
			return Math.ceil(this.total / this.pageLength)
		}
	},
	methods: {
		getLabel(notification) {
			let names = []
			for (let key in notification.labels) {
				const ext = notification.labels[key]
				names.push(ext)
			}
			return names.join(', ');
		},
		markUnread() {
			const uris = this.allCheckedNotifications.map(not => not.meta.uri)
			if (uris.length > 0) {
				this.$store.dispatch('mastering/updateNotification', { uris, status: 'unread' })
			}
		},
		async merge() {
			for (let i = 0; i < this.allCheckedNotifications.length; i++) {
				let notification = this.allCheckedNotifications[i]
				await this.$store.dispatch('mastering/merge', { uris: notification.uris, flowName: notification.flowInfo.flowName, stepNumber: notification.flowInfo.stepNumber, preview: false })
			}
			this.$store.dispatch('mastering/getNotifications', { page: this.currentPage });
		},
		async unmerge() {
			for (let i = 0; i < this.allCheckedNotifications.length; i++) {
				let notification = this.allCheckedNotifications[i]
				if (notification.merged.uri) {
					await this.$store.dispatch('mastering/unmerge', notification.merged.uri)
				}
			}
			this.$store.dispatch('mastering/getNotifications', { page: this.currentPage });
		},
		gotoCompare(notification) {
			this.$router.push({
				name: 'root.notifications.compare',
				query: { notification: notification.meta.uri }
			})
		}
	},
	mounted: function() {
		this.currentPage = this.$route.query.page ? parseInt(this.$route.query.page) : 1
	},
	watch: {
		'$route.query.page'(val) {
			this.currentPage = val ? parseInt(val) : 1
		}
	}
}
</script>

<style lang="less" scoped>
tr {
	cursor: pointer;

	&.unread td {
		font-weight: bold;
	}
}
</style>
