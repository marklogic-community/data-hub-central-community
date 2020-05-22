<template>
	<v-app>
		<Header v-if="isLoggedIn && !needsInstall"/>
		<v-content>
          <router-view/>
		</v-content>
    <ml-footer/>
	</v-app>
</template>

<script>
import BreadCrumbs from '@/components/BreadCrumbs.vue';
import Header from '@/components/Header.vue';
import mlFooter from '@/components/ml-footer.vue';
import { mapState } from 'vuex'

export default {
  name: 'app',
  components: {
    BreadCrumbs,
    Header,
		mlFooter
	},
	computed: {
		isLoggedIn() {
      return this.$store.state.auth.authenticated;
		},
		...mapState({
			needsInstall: state => state.auth.needsInstall
		}),
	},
	created() {
    this.$http.interceptors.response.use(undefined, (err) => {
      return new Promise((resolve, reject) => {
        if (err && err.response && err.response.status === 401 && err.config && !err.config.__isRetryRequest) {
					this.$store.dispatch('auth/logout')
					this.$router.push({ name: 'root.login' });
        }
        throw err;
      });
    });
	}
};
</script>

<style>
.container {
	padding: 12px 20px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter,
.fade-leave-active {
  opacity: 0;
}

.graph-controls {
  display: none !important;
}

.mlvisjs-graph.fontawesome-style .vis-network .vis-manipulation .vis-button .vis-label {
	color: #44499c;
}
.mlvisjs-graph.fontawesome-style .vis-network .vis-manipulation .vis-button .vis-label::before {
	color: #44499c;
}
.mlvisjs-graph.fontawesome-style .vis-network .vis-close::before {
	color: #44499c;
}
.mlvisjs-graph .vis-network {
	border: 1px solid #44499c;
}
.mlvisjs-graph.fontawesome-style .vis-network .vis-manipulation {
	border-bottom: 1px solid #444499c;
}
.mlvisjs-graph.fontawesome-style .vis-network .vis-manipulation .vis-button .vis-label:hover {
	color: #0511e6;
}
.mlvisjs-graph.fontawesome-style .vis-network .vis-manipulation .vis-button .vis-label:hover::before {
	color: #0511e6;
}
</style>
