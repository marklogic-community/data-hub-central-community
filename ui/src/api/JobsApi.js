import axios from 'axios';
export default {
	name: 'JobsApi',
	getJobs(flowName) {
		return axios.get(`/api/jobs?flowName=${encodeURIComponent(flowName)}`)
			.then(response => response.data)
	},
	deleteJob(jobId) {
		return axios.post(`/api/jobs/delete?jobId=${encodeURIComponent(jobId)}`)
			.then(response => response.data)
	}
}
