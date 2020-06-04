'use strict';

var uris;
var flowName;
var stepNumber;
var preview;
var jobId;
var performanceMetrics;

const DataHubSingleton = require("/data-hub/5/datahub-singleton.sjs");

const datahub = DataHubSingleton.instance({
	performanceMetrics: !!performanceMetrics
});
const internalFlowName = 'manual-merge-mastering';
const internalStepNumber = 1;
let refStepNumber = stepNumber || '1';
let flow = datahub.flow.getFlow(flowName);
let stepRef = flow.steps[refStepNumber];
let stepDetails = datahub.flow.step.getStepByNameAndType(stepRef.stepDefinitionName, stepRef.stepDefinitionType);
// build combined options
let flowOptions = flow.options || {};
let stepRefOptions = stepRef.options || {};
let stepDetailsOptions = stepDetails.options || {};
let combinedOptions = Object.assign({}, stepDetailsOptions, flowOptions, stepRefOptions);
let sourceDatabase = combinedOptions.sourceDatabase || datahub.flow.globalContext.sourceDatabase;

combinedOptions.fullOutput = true;
combinedOptions.noWrite = !!preview;
combinedOptions.acceptsBatch = true;
let query = cts.documentQuery(uris.toObject());
let content = datahub.hubUtils.queryToContentDescriptorArray(query, combinedOptions, sourceDatabase);
let results = datahub.flow.runFlow(internalFlowName, jobId, content, combinedOptions, internalStepNumber);
const response = {
	'success': results.errorCount === 0,
	'errors': results.errors,
	'mergedURIs': uris.toObject(),
	'mergedDocument': results.documents.filter((doc) => !!doc.previousUri)[0]
};

response;
