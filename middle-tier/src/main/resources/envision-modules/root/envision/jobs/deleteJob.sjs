'use strict'
var jobId

const DataHub = require("/data-hub/5/datahub.sjs")
const datahub = new DataHub()

console.log('jobId:::', jobId)
datahub.jobs.deleteJob(jobId)

const resp = {}
resp
