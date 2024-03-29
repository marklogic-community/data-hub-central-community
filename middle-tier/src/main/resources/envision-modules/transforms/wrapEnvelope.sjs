'use strict';
const DataHubSingleton = require("/data-hub/5/datahub-singleton.sjs");
const datahub = DataHubSingleton.instance();

function transform(context, params, content) {
  const flowName = 'default-ingestion';
  let flow = datahub.flow.getFlow(flowName);
  if (!flow) {
    datahub.debug.log({message: params, type: 'error'});
    fn.error(null, "RESTAPI-SRVEXERR", Sequence.from([404, "Not Found","The specified flow " + flowName + " is missing."]));
  }

  const step = '1';
  let stepObj = flow.steps[step];
  if(!stepObj) {
    datahub.debug.log({message: params, type: 'error'});
    fn.error(null, "RESTAPI-SRVEXERR", Sequence.from([404, "Not Found", "The specified step "+ step + " is missing in  " + flowName]));
  }
  if(! stepObj.stepDefinitionType.toLowerCase() === "ingestion"){
    datahub.debug.log({message: params, type: 'error'});
    fn.error(null, "RESTAPI-SRVEXERR", Sequence.from([400, "Invalid Step Type", "The specified step "+ step + " is not an ingestion step"]));
  }

	let jobId = params["job-id"];
	let outputFormat = 'json';
	// XQUERY: "xqy",
  // JAVASCRIPT: "sjs",
  // XML: "xml",
  // JSON: "json",
  // BINARY: "binary",
	// TEXT: 'text',
	const format = context.outputType.toLowerCase();
	if (format.endsWith('json')) {
		outputFormat = 'json';
	}
	else if (format.endsWith('xml')) {
		outputFormat = 'xml';
	}
	else {
		outputFormat = 'binary';
	}

  const options = {
		outputFormat: outputFormat
	};

  options.writeStepOutput = false;
  options.fullOutput = true;
  options.noBatchWrite = true;

  let newContent = {};
  newContent.uri=context.uri;
  newContent.value=content;

  let flowContent = [];
  flowContent.push(newContent);

  let flowResponse = datahub.flow.runFlow(flowName, jobId, flowContent, options, step);
    if (flowResponse.errors && flowResponse.errors.length) {
      datahub.debug.log(flowResponse.errors[0]);
      fn.error(null, flowResponse.errors[0].message, flowResponse.errors[0].stack);
    }
    let documents = flowResponse.documents;
    if (documents && documents.length) {
    	Object.assign(context, documents[0].context);
    }
    let docs = [];
    for (let doc of documents) {
      delete doc.context;
      if (doc.type && doc.type === 'error' && doc.message) {
        datahub.debug.log(doc);
        fn.error(null, "RESTAPI-SRVEXERR", Sequence.from([500, "Flow Error", doc.message]));
      } else if (!doc.value) {
        datahub.debug.log({message: params, type: 'error'});
        fn.error(null, "RESTAPI-SRVEXERR", Sequence.from([404, "Null Content", "The content was null in the flow " + flowName + " for " + doc.uri + "."]));
      }
      else {
        docs.push(doc.value);
      }
    }
   return Sequence.from(docs);
}

exports.transform = transform;
