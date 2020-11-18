import axios from 'axios';

export default {
	name: 'UploadApi',
	upload(file, progressHandler = null) {
		let formData = new FormData()
		formData.append('collection', file.name)
		formData.append('file', file)
		return axios.post( '/api/upload',
			formData,
			{
				headers: {
					'Content-Type': 'multipart/form-data'
				},
				onUploadProgress: progressHandler
			}
		)
	}
}
