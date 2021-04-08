'use strict'
var jobId

const datahub = require("/envision/dh-utils.sjs")

datahub.jobs.deleteJob(jobId)

const resp = {}
resp
