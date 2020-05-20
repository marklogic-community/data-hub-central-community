function deploy(path, params) {
	console.log('In osAPI deploy function');
}

import axios from 'axios';

export default {
	name: 'OSApi',
	dhsRunFlows() {
		console.log('In OS API runFlows ');

		return axios
			.get('/api/os/runFlowsWithoutIngestionSteps/')
			.then(response => {
				console.log('Returning ' + response.data);
				return { response: response.data };
			})
			.catch(error => {
				console.error('error:', error);
				return error;
			});
	},
	runIngestSteps() {
		console.log('In OS API runIngestSteps ');

		return axios
			.get('/api/os/runIngestSteps/')
			.then(response => {
				console.log('Returning ' + response.data);
				return { response: response.data };
			})
			.catch(error => {
				console.error('error:', error);
				return error;
			});
	},
	deployToDHS() {
		console.log('In deloyToDHS');

		return axios
			.get('/api/os/deployToDHS/')
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
	},
	setGradleProperties(props) {
		console.log('In OS API setGradleProperties , props=' + props);
		var config = {
			headers: {
				'Content-Length': 0,
				'Content-Type': 'text/plain'
			},
			responseType: 'text'
		};
		return axios
			.post('/api/os/setGradleProps/', props, config)
			.then(response => {
				console.log('Returning ' + response.data);
				return { response: response.data };
			})
			.catch(error => {
				console.error('error:', error);
				return error;
			});
	}
};
