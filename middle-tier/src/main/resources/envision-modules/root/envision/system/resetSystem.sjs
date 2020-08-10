'use strict'

const config = require('/com.marklogic.hub/config.sjs')
const finalDB = config.FINALDATABASE
const stagingDB = config.STAGINGDATABASE
const jobDB = config.JOBDATABASE
let arrErrors = []

// code to delete anything in a collection EXCEPT ES/DH collections, or items not in any
// collections (e.g. model.json)
function deleteCollections() {
	declareUpdate();
	for (var col of cts.collections() ) {
		if (col.toString().indexOf("http://marklogic.com/") < 0) {
			xdmp.collectionDelete(col)
		}
	}
}

let arrDatabases = [finalDB, stagingDB]

arrDatabases.forEach(db => {
	try {
		xdmp.invokeFunction(deleteCollections, { "database" : xdmp.database(db) })
	} catch (e) {
		arrErrors.push ("Error clearing items from " + db + ". Error message was '" + e.message + "'." )
	}
})

// delete Jobs and provenance
function deleteJobs() {
	declareUpdate();
	xdmp.collectionDelete("Jobs")
	xdmp.collectionDelete("http://marklogic.com/provenance-services/record")
}

try {
	xdmp.invokeFunction(deleteJobs,{ "database" : xdmp.database(jobDB) })
} catch (e) {
	arrErrors.push ("Error clearing items from data-hub-JOBS. Error message was '" + e.message + "'.")
}

let success = true
let error = null
if (arrErrors.length > 0) {
	success = false
	error = arrErrors.join(', ')
}
const result = {
	success,
	error
}
result
