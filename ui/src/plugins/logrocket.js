import store from '../store'

const isHosted = process.env.VUE_APP_IS_HOSTED === 'true'
const logRocketID = process.env.VUE_APP_LOGROCKET_ID
const LogRocket = (isHosted && logRocketID) ? require('logrocket') : {
	identify: function() {}
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
