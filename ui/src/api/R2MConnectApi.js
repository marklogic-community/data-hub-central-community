import axios from 'axios';

export default {
	name: 'R2MConnectAPI',
	async r2m(source, query, insert) {
		var mlConfig = null
		return axios.post('/api/r2m/runExports', {
					query, source, insert, mlConfig
				})
			.then(response => {
        if (response.status === 200) {
					return response.data;
        } else {
          return { isError: true, error: response.data };
        }
			}
		)
		.catch(error => {
			console.error('error:', error);
			return { isError: true, error: error };
		});
	}
}
