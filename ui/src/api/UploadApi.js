import axios from 'axios';

export default {
	name: 'UploadApi',
	upload(collection, files, progressHandler = null) {
		let formData = new FormData()
		formData.append('collection', collection)
		for (let i = 0; i < files.length; i++) {
			const file = files[i]
			formData.append('files', file)
		}
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
