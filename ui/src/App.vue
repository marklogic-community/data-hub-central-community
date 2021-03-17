<template>
	<v-app>
		<Header v-if="isLoggedIn"/>
		<v-main>
          <router-view/>
		</v-main>
    <mlFooter v-if="isLoggedIn"/>
		<progress-listener />
	</v-app>
</template>

<script>
import Header from '@/components/Header.vue';
import mlFooter from '@/components/ml-footer.vue';
import ProgressListener from '@/components/ProgressListener.vue'

export default {
  name: 'app',
  components: {
    Header,
		mlFooter,
		ProgressListener
	},
	computed: {
		isLoggedIn() {
      return this.$store.state.auth.authenticated;
		}
	},
	created() {
    this.$http.interceptors.response.use(undefined, (err) => {
      return new Promise(() => {
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

@font-face {
  font-family: MLCustomFont;
  src: url(./fonts/ML_IconFont.ttf) format('truetype');
  font-family: 'MLCustomFont';
  font-weight: normal;
  font-style: normal;
}

#app,
.v-application--wrap {
	height: 100vh;
	font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

h1, h2, h3, h4{
 font-family:Helvetica Neue;
 font-weight: normal;
 color: #333333
}

h1{
font-size: 22px;
}

h2{
 font-size: 20px;
}

h3{
  font-size: 18px;
 }

h4{
  font-size: 16px;
 }

.v-main {
	overflow: auto;
	flex: 1 1 auto;
}
</style>
