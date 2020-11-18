import axios from 'axios';
export default {
	name: 'EntitiesApi',
	getEntity(entityName) {
		return axios.get(`/api/entities/${encodeURIComponent(entityName)}`)
		.then(response => response.data)
	},
	getEntities() {
		return axios.get(`/api/entities`)
		.then(response => response.data)
	}
}
