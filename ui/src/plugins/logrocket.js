import store from '../store'

const logRocketID = process.env.VUE_APP_LOGROCKET_ID
const LogRocket = logRocketID ? require('logrocket') : {
	identify: function() {},
	init: function() {}
}

export default {
	install(Vue) {
		Vue.prototype.$logRocket = this
		store.$logRocket = this
		LogRocket.init(logRocketID)
	},
	identify(id, userInfo) {
		LogRocket.identify(id, userInfo)
	}
}
