import axios from 'axios';
export default {
	name: 'FlowsApi',
	getFlow(flowId) {
		return axios.get(`/api/flows/${encodeURIComponent(flowId)}`)
		.then(response => response.data)
	},
	getFlows() {
		return axios.get('/api/flows/')
			.then(response => response.data)
	},
	saveFlow(flow) {
		return axios.put(`/api/flows/${encodeURIComponent(flow.name)}`, flow)
	},
	deleteFlow(flowId) {
		return axios.post('/api/flows/delete', { flowId })
	},
	getMapping(mapName) {
		return axios.get(`/api/flows/mappings/${encodeURIComponent(mapName)}`)
			.then(response => response.data)
	},
	saveMapping(mapping) {
		return axios.post(`/api/flows/mappings`, mapping)
			.then(response => response.data)
	},
	validateMapping(mapping, uri) {
		return axios.post(`/api/flows/mappings/validate?uri=${encodeURIComponent(uri)}`, mapping)
			.then(response => response.data)
	},
	getFunctions() {
		return axios.get('/api/flows/mappings/functions')
			.then(response => response.data)
	},
	getSampleDoc(uri, namespaces) {
		return axios.post('/api/flows/mappings/sampleDoc', { uri, namespaces })
			.then(response => response.data)
	},
	previewMapping({ mappingName, mappingVersion, format, uri }) {
		return axios.post('/api/flows/mappings/preview', { mappingName, mappingVersion, format, uri })
			.then(response => response.data)
	},
	getNewStepInfo() {
		return axios.get('/api/flows/newStepInfo')
			.then(response => response.data)
	},
	createStep(flowName, step) {
		return axios.post('/api/flows/steps', { flowName, step })
			.then(response => response.data)
	},
	deleteStep(flowName, stepName) {
		return axios.post('/api/flows/steps/delete', { flowName, stepName })
			.then(response => response.data)
	},
	runSteps(flowName, steps) {
		return axios.post('/api/flows/steps/run', { flowName, steps })
			.then(response => response.data)
	}
}
