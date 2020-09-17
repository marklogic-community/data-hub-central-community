<template>
	<div class="usermenu">
		{{ profile && profile.fullname || username }}
		<v-badge
			color="red"
			overlap
			:content="notificationCount"
			:value="notificationCount > 0"
		>
			<div>
				<v-icon>person</v-icon>
			</div>
		</v-badge>
		<v-menu bottom left>
			<template v-slot:activator="{ on }">
				<v-btn
					right
					icon
					v-on="on"
				>
					<v-icon>more_vert</v-icon>
				</v-btn>
			</template>

			<v-list>
				<v-list-item :to="{name: 'root.notifications', query: { page: 1 }}">
          <v-list-item-title>
						<v-badge
							color="red"
							inline
							offset-x="10"
							:content="notificationCount"
							:value="notificationCount > 0"
						>
							Notifications
						</v-badge>
					</v-list-item-title>
        </v-list-item>

        <v-list-item v-if="!isHosted" v-on:click.prevent="adminPage">
          <v-list-item-title>Admin</v-list-item-title>
        </v-list-item>

				<v-list-item v-on:click.prevent="logout()">
					<v-list-item-title>Logout</v-list-item-title>
				</v-list-item>

			</v-list>
		</v-menu>
  </div>
</template>

<script>
import { mapState } from 'vuex'

export default {
	name: 'UserMenu',
	data() {
		return {
			isHosted: process.env.VUE_APP_IS_HOSTED === 'true'
		}
	},
  computed: {
		...mapState({
			profile: state => state.auth.profile,
			username: state => state.auth.username,
			notificationCount: state => state.mastering.totalUnread
		})
  },
  methods: {
    logout() {
      this.$store.dispatch('auth/logout').then(() => {
        if (
          this.$route.meta.requiresLogin ||
          this.$route.meta.requiresUpdates
        ) {
          this.$router.push({ name: 'root.login' });
        }
      })
    },
    adminPage() {
      this.$router.push({ name: 'root.admin' });
    }
  }
};
</script>
<style lang="less" scoped>
.usermenu {
	display: flex;
	align-items: center;
}

.v-list-item__title > .v-badge {
	margin-top: 0px;
}
</style>
