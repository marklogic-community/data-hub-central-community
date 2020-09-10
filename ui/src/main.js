import Vue from 'vue'
import vuetify from './plugins/vuetify'
import VueHttp from './vue-http'
import Vuelidate from 'vuelidate'
import Moment from 'vue-moment'

import App from './App.vue'
import router from './router'
import store from './store'
import websocket from './plugins/websocket'
import './registerServiceWorker'
import './styles/main.less'

Vue.filter('truncate', function (text, stop, clamp) {
	return text && text.slice(0, stop) + (stop < text.length ? clamp || '...' : '')
})

Vue.use(VueHttp)
Vue.use(Vuelidate)
Vue.use(Moment)
Vue.use(websocket)

Vue.config.productionTip = false

// Configure Vue to ignore custom elements preloaded from index.html
Vue.config.ignoredElements = ['file-dropzone']

new Vue({
  router,
  store,
	vuetify,
	websocket,
  render: h => h(App)
}).$mount('#app')
