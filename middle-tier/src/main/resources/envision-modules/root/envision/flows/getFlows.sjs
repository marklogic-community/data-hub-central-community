'use strict';

var flowsToGet;

flowsToGet = flowsToGet.toObject();

// const config = require("/com.marklogic.hub/config.sjs")
// const DataHub = require("/data-hub/5/datahub.sjs");
// const datahub = new DataHub(config);

let flows = fn.collection(['http://marklogic.com/data-hub/flow']).toArray()
	.filter(flow => !xdmp.nodeUri(flow).match('/default-'))
	.map(f => f.toObject())

if (flowsToGet) {
	flows = flows.filter(flow => flowsToGet.indexOf(flow.name) >= 0)
}

flows;
