import axios from 'axios';

export default {
	name: 'ExportApi',
	runExports(entityNames){
		return axios.post("/api/export/runExports", {entityNames})
		.then(response => {
			return { response: response.data };
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	}
};
