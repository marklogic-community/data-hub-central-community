const DataHub = require("/data-hub/5/datahub.sjs")
const datahub = new DataHub()

const jobs = datahub.jobs ? datahub.jobs : require('/data-hub/5/impl/jobs.sjs');
const hubUtils = datahub.hubUtils ? datahub.hubUtils : require('/data-hub/5/impl/hub-utils.sjs');

function getFullFlow(flow) {
	// if only flow name is provided, then get the flow object
	if (typeof flow === 'string' || flow instanceof String) {
		flow = fn.head(cts.search(cts.andQuery([cts.collectionQuery("http://marklogic.com/data-hub/flow"),
			cts.jsonPropertyValueQuery("name", flow, "case-insensitive")]))).toObject()
	}
	let stepReferences = Object.values(flow.steps).map(step => step.stepId).filter((stepId) => stepId)
	let fullSteps = stepReferences.length ? cts.search(cts.andQuery([
		cts.collectionQuery("http://marklogic.com/data-hub/steps"),
		cts.jsonPropertyValueQuery("stepId", stepReferences, "case-insensitive")
	])) : [];
	for (let fullStep of fullSteps) {
		const fullStepObj = fullStep.toObject()
		const stepReference = Object.values(flow.steps).find((stepRef) => stepRef.stepId === fullStepObj.stepId)
		Object.assign(stepReference, fullStepObj)
	}
	return flow
}

module.exports = {
	jobs,
	hubUtils,
	getFullFlow,
	stepDefinition: datahub.flow.stepDefinition,
	flow: datahub.flow
}
