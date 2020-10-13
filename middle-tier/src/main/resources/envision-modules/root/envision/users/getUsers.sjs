'use strict';

const seclib = require('/envision/security-lib.xqy');
const config = require('/com.marklogic.hub/config.sjs')
const stagingDB = config.STAGINGDATABASE

const users = seclib.getUsers();

users.map(u => ({
	username: u,
	stagingDocs: xdmp.invokeFunction(() => {
		return fn.count(cts.uriMatch(`/ingest/${u}/*`))
	}, { database: xdmp.database(stagingDB) })
}))
