'use strict'

const ents = require('/envision/entities.sjs');
const merging = require('/com.marklogic.smart-mastering/merging.xqy')

var uri;

let doc = cts.doc(uri).toObject()
let uris = doc.envelope.headers.merges.map(m => m['document-uri'])

xdmp.invokeFunction(() => {
	declareUpdate();
	merging.rollbackMerge(uri, true, false)
})

let result = null
xdmp.invokeFunction(() => {
	const connectionLimit = 20
	result = ents.getEntities(uris, { connectionLimit })
})

result

