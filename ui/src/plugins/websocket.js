import SockJS from "sockjs-client"
import Stomp from "webstomp-client"
import store from '../store'

const isTesting = process.env.VUE_APP_IS_TESTING === 'true'

const WebSocket = isTesting ? {
	install(Vue) {
		Vue.prototype.$ws = this
		store.$ws = this
	},
	subscribe() {},
	connect() {
		return new Promise((resolve) => {
			resolve()
		})
	},
	disconnect() {}
} : {
	socket: {},
	subscribeQueue: [],
	install(Vue) {
		Vue.prototype.$ws = this
		store.$ws = this
	},
	subscribe(topic, handler) {
		this.subscribeQueue.push({topic, handler})

		if (this.connected) {
			this._drainSubQ()
		}
	},
	_drainSubQ() {
		this.subscribeQueue.forEach(q => {
			this.stompClient.subscribe(q.topic, tick => {
				tick.body = JSON.parse(tick.body)
				q.handler(tick)
			})
		})
		this.subscribeQueue = []
	},
	connect() {
		return new Promise((resolve, reject) => {
			this.socket = new SockJS('/websocket')
			this.stompClient = Stomp.over(this.socket)
			this.stompClient.connect(
				{},
				() => {
					this.connected = true
					this._drainSubQ()
					resolve()
				},
				error => {
					this.connected = false
					reject(error)
				}
			)
		})
	},
	disconnect() {
		if (this.stompClient) {
			this.stompClient.disconnect()
		}
		this.connected = false
	}
}

export default WebSocket
