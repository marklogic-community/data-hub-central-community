import store from '../store'

const isTesting = process.env.VUE_APP_IS_TESTING === 'true'
const isHosted = process.env.VUE_APP_IS_HOSTED === 'true'
const logRocketID = process.env.VUE_APP_LOGROCKET_ID
const LogRocket = (logRocketID && !isTesting) ? require('logrocket') : {
	identify: function() {},
	init: function() {}
}

export default {
	install(Vue) {
		Vue.prototype.$logRocket = this
		store.$logRocket = this
		LogRocket.init(logRocketID, {
			network: {
				requestSanitizer: request => {
					request.headers['Authorization'] = null;
					if (!isHosted) {
						request.body = null
					}
					else if (request.url.toLowerCase().indexOf('login') !== -1) {
						request.body = null;
					}

					// otherwise log the request normally
					return request;
				},
				responseSanitizer: () => {
					return null;
				},
			},
		})
	},
	identify(id, userInfo) {
		LogRocket.identify(id, userInfo)
	}
}
