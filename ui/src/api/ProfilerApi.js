import axios from 'axios'

export default {
	name: 'ProfilerApi',
	getReports(page, pageLength) {
		return axios.post("/api/data-profiler/reports", {page, pageLength})
			.then(response => {
				return response.data
			})
			.catch(error => {
				console.error('error:', error)
				return error
			})
	},
	getReport(uri) {
		return axios.get(`/api/data-profiler/report?uri=${encodeURIComponent(uri)}`)
			.then(response => {
				return response.data
			})
			.catch(error => {
				console.error('error:', error)
				return error
			})
	},
	deleteReport(uri) {
		return axios.get(`/api/data-profiler/delete-report?uri=${encodeURIComponent(uri)}`)
			.then(response => {
				return response.data
			})
			.catch(error => {
				console.error('error:', error)
				return error
			})
	},
	deleteAllReports() {
		return axios.get('/api/data-profiler/delete-all-reports')
			.then(response => {
				return response.data
			})
			.catch(error => {
				console.error('error:', error)
				return error
			})
	},
	profile({collection, database, sampleSize}) {
		return axios.post("/api/data-profiler/profile", {collection, database, sampleSize})
			.then(response => {
				return response.data
			})
			.catch(error => {
				console.error('error:', error)
				return error
			})
	}
}
