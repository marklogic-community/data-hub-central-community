import axios from 'axios';

export default {
	name: 'R2MConnectAPI',
	async r2m(tableQuery, sourceConfig, insertConfig, mlConfig) {
		let formData = new FormData()
		formData.append('tableQuery', tableQuery)
		formData.append('sourceConfig', sourceConfig)
		formData.append('insertConfig', insertConfig)
		formData.append('mlConfig', mlConfig)
		try {
			const response = await axios.post('/api/r2m/runExports',
				formData,
				{
					headers: {
						'Content-Type': 'multipart/form-data'
					}
				}
			);
			if (response.status === 200) {
				axios.defaults.headers.common['Authorization'] = response.headers.authorization;
				localStorage.setItem('access_token', response.headers.authorization);
				return response.data;
			} else {
				return { isError: true, error: response.data };
			}
		} catch (error) {
			console.error('error:', error);
			return { isError: true, error: error };
		}
	}
}
