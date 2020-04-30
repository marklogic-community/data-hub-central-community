<template>
	<div>
		<v-icon>person</v-icon>{{ profile && profile.fullname || username }}

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

        <v-list-item v-on:click.prevent="adminPage">
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
export default {
  name: 'UserMenu',
  computed: {
    profile() {
      return this.$store.state.auth.profile;
    },
    username() {
      return this.$store.state.auth.username;
    }
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
