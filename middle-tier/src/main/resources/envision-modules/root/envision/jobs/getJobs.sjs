'use strict'
var flowName

const DataHub = require("/data-hub/5/datahub.sjs")
const datahub = new DataHub()

datahub.jobs.getJobDocsByFlow(flowName)
