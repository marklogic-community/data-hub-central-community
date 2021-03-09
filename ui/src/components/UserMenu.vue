<template>
	<div class="usermenu">
	<div class="vertical"></div>
		<a target="_blank" href="https://marklogic-community.github.io/data-hub-central-community/" class="docsLink">
    	<v-icon>help_outline</v-icon>
   </a>
		<v-list-item :to="{name: 'root.notifications', query: { page: 1 }}" class="notification">
    						<v-badge
    							color="red"
    						  overlap
    							:content="notificationCount"
    							:value="notificationCount > 0"
    						>
   <div>
      	<v-icon>notifications_none</v-icon>
   </div>
    						</v-badge>
            </v-list-item>
		<v-menu bottom left>
			<template v-slot:activator="{ on }">
				<v-btn
					right
					icon
					v-on="on"
          class="userIcon"
				>
					<v-icon>person_outline</v-icon>
				</v-btn>
			</template>

			<v-list>
       <v-list-item class="username">{{ profile && profile.fullname || username }}</v-list-item>
       <div class="horizontal"></div>
        <v-list-item v-if="isHosted && isAdmin" v-on:click.prevent="hostedAdminPage">
          <v-list-item-title>Admin</v-list-item-title>
        </v-list-item>

        <v-list-item v-else-if="!isHosted" v-on:click.prevent="adminPage">
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
			notificationCount: state => state.mastering.totalUnread,
			isAdmin: state => state.auth.authorities.indexOf('ROLE_envisionAdmin') >= 0
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
		hostedAdminPage() {
			this.$router.push({ name: 'root.hostedadmin' });
		},
    adminPage() {
      this.$router.push({ name: 'root.admin' });
    }
	},
	mounted() {
		this.$ws.subscribe('/topic/status', tick => {
			const msg = tick.body
			if (msg.percentComplete >= 100) {
				this.$store.dispatch('mastering/getNotifications', {})
			}
		})
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

.vertical{
   display: inline-block;
   position: relative;
   top: 3px;
   border-left: dotted 1px rgba(255, 255, 255, 0.65);
   height: 24px;
   margin: -6px 8px;
   cursor: default;
}

.horizontal{
  border-top: solid 1px #E5E5E5;
  cursor: pointer;
}

.userIcon{
	height: 0px !important;
  width: 0px !important;
  margin-right: 12px;
}

.username{
  font-weight: 700;
}

.docsLink{
  text-decoration:none;
}

.notification{
  margin-right:10px;
}
</style>
