import axios from 'axios'

export default {
	name: 'MasteringApi',

	getHistory(uri) {
		return axios.post(
			'/api/mastering/history',
			{
				uri: uri
			}
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error)
			return error
		})
	},

	getDoc(uri) {
		return axios.get(`/api/mastering/doc?docUri=${uri}`)
			.then(response => {
				return response.data
			})
			.catch(error => {
				console.error('error:', error);
				return error;
			});
	},

	getNotification(uri) {
		return axios.get(
			`/api/mastering/notification?uri=${encodeURIComponent(uri)}`
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},

	getNotifications(page, pageLength, extractions) {
		return axios.post(
			'/api/mastering/notifications/',
			{
				page,
				pageLength,
				extractions
			}
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},

	updateNotification(uris, status) {
		return axios.put(
			'/api/mastering/notifications/',
			{
				uris,
				status
			}
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},

	getBlocks(uris) {
		return axios.post(
			'/api/mastering/blocks/',
			uris
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},

	block(uris) {
		return axios.post(
			'/api/mastering/block/',
			uris
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},

	unBlock(uris) {
		return axios.post(
			'/api/mastering/unblock/',
			uris
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	},

	merge(uris, flowName, stepNumber, preview) {
		return axios.post(
			'/api/mastering/merge',
			{
				uris,
				flowName,
				stepNumber,
				preview
			}
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error)
			return error
		})
	},

	unmerge(uri) {
		return axios.post(
			'/api/mastering/unmerge',
			{
				uri: uri
			}
		)
		.then(response => {
			return response.data
		})
		.catch(error => {
			console.error('error:', error)
			return error
		})
  }
}
