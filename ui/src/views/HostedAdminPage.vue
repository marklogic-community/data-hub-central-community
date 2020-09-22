<template>
	<v-container>
		<v-layout row>
			<v-flex md12 class="text-center">
				<h1>Manage Users</h1>
			</v-flex>
		</v-layout>
		<v-row justify="center">
			<v-col cols="6" class="data-container">
				<v-simple-table>
					<thead>
						<tr>
							<th>User</th>
							<th>Staging Docs</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="user of users" :key="user.username">
							<td>{{user.username}}</td>
							<td>{{user.stagingDocs}}</td>
							<td>
								<delete-data-confirm
									:deleteInProgress="deleteInProgress"
									tooltip="Delete User"
									message="Do you really want to delete this user?"
									:collection="user.username"
									@deleted="removeUser($event)"/>
							</td>
						</tr>
					</tbody>
				</v-simple-table>
			</v-col>
		</v-row>
	</v-container>
</template>

<script>
import authApi from '../api/AuthApi'
import DeleteDataConfirm from '@/components/DeleteDataConfirm'

export default {
	components: {
		DeleteDataConfirm
	},
	computed: {
		uploading() {
			return this.percentComplete !== null && this.percentComplete < 100
		}
	},
	data() {
		return {
			users: [],
			deleteInProgress: false
		}
	},
	methods: {
		getUsers() {
			authApi.getUsers().then(users => {
				this.users = users
			})
		},
		async removeUser(user) {
			this.deleteInProgress = true
			await authApi.deleteUser(user)
			this.deleteInProgress = false
			this.users = this.users.filter(c => c.username !== user)
		}
	},
	mounted() {
		this.getUsers()
	}
}
</script>

<style lang="less" scoped>
</style>
