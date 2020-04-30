<template>
	<v-app-bar dense dark>
    <img src="@/assets/images/MarkLogic-avatar.svg" height="70%"/>
		<span class="page-title">Envision</span>
		<v-spacer />
		<v-toolbar-items>
      <template v-for="(link, i) in visibleRoutes">
				<v-btn
					:key="i"
					v-if="link.meta.navArea === 'header'"
					:to="{ name: link.name, params: { prev: currentRoute.name } }"
					class="ml-0 hidden-sm-and-down"
					text
				>
					{{ link.meta.label }}
				</v-btn>
			</template>
    </v-toolbar-items>
		<v-spacer />
		<UserMenu/>
	</v-app-bar>
</template>

<script>
import UserMenu from '@/components/UserMenu.vue';

export default {
  name: 'Header',
  components: {
    UserMenu
  },
  computed: {
    currentRoute() {
      return this.$route;
    },
    visibleRoutes() {
      return this.$router.options.routes.filter(function(route) {
        if (this.$store.state.auth.authenticated) {
          return (
            !route.meta.requiresUpdates ||
            !(
              this.$store.state.auth.profile &&
              this.$store.state.auth.profile.disallowUpdates
            )
          );
        } else {
          return !(route.meta.requiresLogin || route.meta.requiresUpdates);
        }
      }, this);
    }
  }
};
</script>

<style lang="less" scoped>
	header {
		max-height: 48px;
	}

	.page-title {
		margin-left: 10px;
	}
</style>
