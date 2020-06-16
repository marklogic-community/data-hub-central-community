import axios from 'axios';
export default {
	name: 'ModelApi',
	async getAllModels() {
		const response = await axios.get('/api/models/')
		const models = response.data;
		models.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
		return models
	},
  view() {
		return axios.get(`/api/models/model.json`)
		.then(response => response.data)
	},
	needsImport() {
		return axios.get(`/api/models/needsImport`)
		.then(response => response.data)
	},
  save(data) {
    return axios.put('/api/models/', data)
    .then(response => {
			return { isError: false, response: response.data };
		})
    .catch(error => {
			console.error('error:', error);
			return { isError: true, error: error };
		});
  },
  deleteModel(data) {
	return axios.post('/api/models/delete', data)
    .then(response => {
		return { isError: false, response: response.data };
	})
    .catch(error => {
		console.error('error:', error);
		return { isError: true, error: error };
	});
  },
  rename(data) {
	return axios.post('/api/models/rename', data)
    .then(response => {
		return { isError: false, response: response.data };
	})
    .catch(error => {
		console.error('error:', error);
		return { isError: true, error: error };
	});

  }
};
