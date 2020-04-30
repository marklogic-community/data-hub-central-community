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
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter,
.fade-leave-active {
  opacity: 0;
}
</style>
