/**
 * This custom hook remaps the URIs for envision at a url. it allows 2 mapping steps to work on the same input source
 */
// A custom hook receives the following parameters via DHF. Each can be optionally declared.
var uris; // an array of URIs (may only be one) being processed
var content; // an array of objects for each document being processed
var options; // the options object passed to the step by DHF
var flowName; // the name of the flow being processed
var stepNumber; // the index of the step within the flow being processed; the first step has a step number of 1
var step; // the step definition object

step = step.stepId ? fn.head(cts.search(cts.andQuery([
	cts.collectionQuery("http://marklogic.com/data-hub/steps"),
	cts.jsonPropertyValueQuery("stepId", step.stepId, "case-insensitive")
]))).toObject() : step;

content = content.map(c => {
	c.uri = `/data/${xdmp.getCurrentUser()}/${step.sourceCollection}/${step.targetEntityType}/${sem.uuidString()}.json`.replace(/\s+/g, '_')
	return c;
});
