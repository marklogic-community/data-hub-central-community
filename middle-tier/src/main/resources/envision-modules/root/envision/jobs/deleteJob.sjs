'use strict'
var jobId

const DataHub = require("/data-hub/5/datahub.sjs")
const datahub = new DataHub()

datahub.jobs.deleteJob(jobId)

const resp = {}
resp
