import axios from 'axios';

export default {
  name: 'CRUDApi',
  metadata(uri, db) {
		return axios.get(`/api/crud/metadata?uri=${encodeURIComponent(uri)}&database=${db}`)
      .then(response => response.data)
			.catch(error => {
				console.error('error:', error);
				return error;
			})
	},
	doc(uri, db='final') {
		return axios.get(`/api/crud?uri=${encodeURIComponent(uri)}&database=${db}`)
      .then(response => {
				return response.data
			})
			.catch(error => {
				console.error('error:', error);
				return error;
			})
	},
	binaryDoc(uri, db='final') {
		return axios({
			method: 'get',
			url: `/api/crud?uri=${encodeURIComponent(uri)}&database=${db}`,
			responseType: 'arraybuffer'
		})
	}
};
