import axios from 'axios'

export default {
	name: 'MasteringApi',

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
