<template>
	<v-app-bar dense dark>
    <img src="@/assets/images/MarkLogic-avatar.svg" height="70%"/>
		<span class="page-title">Envision</span>
		<a target="_blank" :href="versionLink" class="version">{{version}}</a>
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
		<v-btn v-if="showVideoLinks" color="blue-grey" class="ma-2 white--text" :href="currentRoute.meta.tutorialLink" target="_blank">
			<v-icon
        left
        dark
      >
        fa-youtube
      </v-icon>
			{{currentRoute.meta.tutorialName}}</v-btn>
		<UserMenu/>
	</v-app-bar>
</template>

<script>
import UserMenu from '@/components/UserMenu.vue'
const isTesting = process.env.NODE_ENV === 'test'
const isHosted = process.env.VUE_APP_IS_HOSTED === 'true'
const version = process.env.VUE_APP_ENVISION_VERSION

export default {
  name: 'Header',
  components: {
    UserMenu
  },
  computed: {
		versionLink() {
			if (this.version.startsWith('v.dev-')) {
				return 'https://github.com/marklogic-community/envision'
			}
			return `https://github.com/marklogic-community/envision/releases/tag/v${version}`
		},
		showVideoLinks() {
			return (isTesting || isHosted) && this.currentRoute.meta.tutorialName
		},
    currentRoute() {
      return this.$route
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
          )
        } else {
          return !(route.meta.requiresLogin || route.meta.requiresUpdates)
        }
      }, this)
    }
	},
	data() {
		return {
			version: version ? 'v' + version : `v.dev-${this.$moment().format('l')}`
		}
	}
}
</script>

<style lang="less" scoped>
	header {
		max-height: 48px;
	}

	.page-title {
		margin-left: 10px;
	}

	.version {
		margin-left: 1rem;
		font-size: 12px;
		color: white;
		text-decoration: none;
	}
</style>
