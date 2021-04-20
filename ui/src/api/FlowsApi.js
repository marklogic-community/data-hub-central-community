import axios from 'axios';
export default {
	name: 'FlowsApi',
	getFlow(flowId) {
		return axios.get(`/api/flows/${encodeURIComponent(flowId)}`)
		.then(response => response.data)
		.then((fullFlow) => {
			Object.keys(fullFlow.steps).forEach((stepKey) => {
				let step = fullFlow.steps[stepKey]
				step.stepNumber = stepKey
				if(step.fileLocations) {
					Object.keys(step.fileLocations).forEach((attr) => {
						step[attr] = step.fileLocations[attr];
					});
				}
				if (step.options) {
					Object.assign(step, step.options);
					delete step.options;
				}
				step.targetEntityType = step.targetEntityType || step.targetEntity;
			});
			return fullFlow;
		})
	},
	getFlows() {
		return axios.get('/api/flows/')
			.then(response => {
				let flowDetailPromises = [];
				response.data.forEach((flow) => {
					if (Array.isArray(flow.steps)) {
						let newStepsObj = flow.steps.reduce((stepsObj, step) => {
							stepsObj[step.stepNumber] = step;
							return stepsObj;
						}, {});
						flow.steps = newStepsObj;
					}
					flowDetailPromises.push(this.getFlow(flow.name).then((fullFlow) => flow.steps = fullFlow.steps));
				});
				return Promise.all(flowDetailPromises).then(() => response.data);
			})
	},
	saveFlow(flow) {
		return axios.put(`/api/flows/${encodeURIComponent(flow.name)}`, flow)
	},
	deleteFlow(flowId) {
		return axios.post(`/api/flows/delete?flowId=${encodeURIComponent(flowId)}`)
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
	getCustomStep(stepName) {
		return axios.get(`/api/flows/customSteps/${encodeURIComponent(stepName)}`)
			.then(response => response.data)
	},
	getFunctions() {
		return axios.get('/api/flows/mappings/functions')
			.then(response => response.data)
	},
	getSampleDoc(uri, namespaces) {
		return axios.post('/api/flows/mappings/sampleDoc', { uri, namespaces: namespaces || [] })
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
