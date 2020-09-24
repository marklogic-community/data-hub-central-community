import axios from 'axios';

export default {
	name: 'ExportApi',
	runExports(){
		return axios.post("/api/export/runExports/")
		.then(response => {
			return { response: response.data };
		})
		.catch(error => {
			console.error('error:', error);
			return error;
		});
	}
};
