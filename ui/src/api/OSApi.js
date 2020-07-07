import axios from 'axios';

export default {
	name: 'OSApi',
	runFlows() {
		console.log('In OS API runFlows ');

		return axios
			.get('/api/os/runFlows/')
			.then(response => {
				console.log('Returning ' + response.data);
				return { response: response.data };
			})
			.catch(error => {
				console.error('error:', error);
				return error;
			});
	},
	deployToDH() {
		console.log('In deloyToDHS');

		return axios
			.get('/api/os/deployToDH/')
			.then(response => {
				console.log('Returning ' + response.data);
				return { response: response.data };
			})
			.catch(error => {
				console.error('error:', error);
				return error;
			});
	},
	gradle(command) {
		console.log('In OS API gradle, command=' + command);

		return axios
			.post('/api/os/gradle/', { task: command })
			.then(response => {
				console.log('Returning ' + response.data);
				return { response: response.data };
			})
			.catch(error => {
				console.error('error:', error);
				return error;
			});
	},
	getDHprojectConfig() {
		return axios
			.get('/api/os/getDHprojectConfig/')
			.then(response => {
				console.log('Returning ' + response.data);
				return response.data;
			})
			.catch(error => {
				console.error('Error getting DHS config:', error);
				return error;
			});
	}
};
