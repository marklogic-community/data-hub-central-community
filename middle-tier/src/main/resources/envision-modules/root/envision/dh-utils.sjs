const DataHub = require("/data-hub/5/datahub.sjs")
const datahub = new DataHub()

const jobs = datahub.jobs ? datahub.jobs : require('/data-hub/5/impl/jobs.sjs');
const hubUtils = datahub.hubUtils ? datahub.hubUtils : require('/data-hub/5/impl/hub-utils.sjs');

module.exports = {
	jobs,
	hubUtils
}
